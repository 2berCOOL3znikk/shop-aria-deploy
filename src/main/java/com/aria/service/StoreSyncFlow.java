package com.aria.service;

import com.aria.entity.*;
import com.aria.integration.*;
import com.aria.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Short database phases around bounded, read-only external HTTP. No transaction spans the network. */
@Service
public class StoreSyncFlow {
    private final TransactionTemplate transaction;
    private final ConnectedStoreRepository stores;
    private final ExternalIntegrationRepository integrations;
    private final StoreSnapshotRepository snapshots;
    private final StoreOrderSnapshotRepository orders;
    private final StoreSyncRunRepository runs;
    private final StoreSyncQueue queue;
    private final IntegrationVault vault;
    private final WooCommerceReader reader;
    private final WooCatalogImporter catalog;
    private final WooOrderImporter orderImporter;
    private final AuditService audit;
    @PersistenceContext private EntityManager em;
    private final ConcurrentHashMap<Long,Object> reading=new ConcurrentHashMap<>();
    private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
    private record Input(Long id,long version,String platform,String address,String encrypted,String operation,String snapshot,StoreSyncQueue.Claim claim){
        @Override public String toString(){return "StoreRead["+id+"/"+operation+"]";}
    }
    private record Result(WooCatalogImporter.Catalog catalog,List<WooOrderImporter.Item> orders){}
    private static final class Obsolete extends RuntimeException {}
    public StoreSyncFlow(PlatformTransactionManager manager,ConnectedStoreRepository stores,ExternalIntegrationRepository integrations,
            StoreSnapshotRepository snapshots,StoreOrderSnapshotRepository orders,StoreSyncRunRepository runs,StoreSyncQueue queue,
            IntegrationVault vault,WooCommerceReader reader,WooCatalogImporter catalog,WooOrderImporter orderImporter,AuditService audit){
        transaction=new TransactionTemplate(manager);transaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.stores=stores;this.integrations=integrations;this.snapshots=snapshots;this.orders=orders;this.runs=runs;this.queue=queue;this.vault=vault;this.reader=reader;this.catalog=catalog;this.orderImporter=orderImporter;this.audit=audit;
    }
    public boolean inProgress(Long id){return reading.containsKey(id);}
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public void synchronize(Long id,long version,String operation,StoreSyncQueue.Claim claim){
        Object marker=new Object();if(reading.putIfAbsent(id,marker)!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Обновление уже выполняется. Дождитесь завершения чтения магазина.");
        try{
            var input=transaction.execute(tx->prepare(id,version,operation,claim));Result result=null;String error=null;
            try{result=read(input);}catch(IllegalArgumentException|IllegalStateException e){error=Objects.toString(e.getMessage(),"Не удалось прочитать магазин. Предыдущие данные сохранены");}
            final Result received=result;final String failure=error;
            try{transaction.executeWithoutResult(tx->finish(input,received,failure));}
            catch(Obsolete cancelled){
                transaction.executeWithoutResult(tx->{em.clear();var s=stores.lockById(id).orElse(null);if(s!=null)saveRun(id,operation,"STALE",0,0,"Чтение завершено после изменения подключения или данных. Ответ не применён.");audit.record("STORE_SYNC_DISCARDED","store:"+id,"Поздний ответ не применён; актуальные данные и разрешения сохранены");});
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Подключение или данные изменились во время чтения. Поздний ответ не применён.");
            }
        }finally{reading.remove(id,marker);}
    }
    private Input prepare(Long id,long version,String operation,StoreSyncQueue.Claim claim){
        em.clear();var s=stores.lockById(id).orElseThrow(()->new IllegalArgumentException("Магазин не найден"));
        if(s.version!=version)throw new ResponseStatusException(HttpStatus.CONFLICT,"Данные изменились. Обновите список магазинов");
        if(s.archived)throw new IllegalStateException("Подключение в архиве. Сначала восстановите его");
        if(s.status.equals("DISCONNECTED"))throw new IllegalArgumentException("Магазин отключён. Настройте его заново");
        if(!Set.of("CHECK","SYNC","ORDERS").contains(operation)||operation.equals("ORDERS")&&!s.platform.equals("WOOCOMMERCE"))throw new IllegalArgumentException("Для чтения заказов настройте подключение WooCommerce");
        if(claim!=null)queue.requireActive(claim);
        String encrypted=integrations.findByStoreId(id).map(i->i.encryptedCredentials).orElse(null);
        return new Input(id,s.version,s.platform,s.baseUrl,encrypted,operation,operation.equals("SYNC")&&s.platform.equals("WOOCOMMERCE")?snapshotStamp(id):"",claim);
    }
    private Result read(Input input){
        if(!input.platform.equals("WOOCOMMERCE"))return new Result(null,null);
        if(input.encrypted==null)throw new IllegalArgumentException("Сначала сохраните ключи чтения магазина");
        var credentials=vault.decrypt(input.encrypted).split("\n",-1);
        if(credentials.length!=2)throw new IllegalStateException("Настройте доступы магазина заново");
        if(input.operation.equals("CHECK")){reader.get(input.address,"products?per_page=100&page=1",credentials[0],credentials[1]);return new Result(null,null);}
        if(input.operation.equals("ORDERS"))return new Result(null,orderImporter.read(input.address,credentials[0],credentials[1]));
        return new Result(catalog.read(input.address,credentials[0],credentials[1]),null);
    }
    private void finish(Input input,Result received,String failure){
        em.clear();var s=stores.lockById(input.id).orElseThrow(Obsolete::new);
        String encrypted=integrations.findByStoreId(input.id).map(i->i.encryptedCredentials).orElse(null);
        if(s.archived||s.status.equals("DISCONNECTED")||s.version!=input.version||!s.platform.equals(input.platform)||!Objects.equals(s.baseUrl,input.address)||!Objects.equals(encrypted,input.encrypted))throw new Obsolete();
        if(input.claim!=null&&!queue.isActive(input.claim))throw new Obsolete();
        if(!input.snapshot.isEmpty()&&!input.snapshot.equals(snapshotStamp(input.id)))throw new Obsolete();
        String operation=input.operation;int imported=0,skipped=0;String note;
        if(failure!=null){
            String message=failure.length()>500?failure.substring(0,500):failure;
            if(operation.equals("ORDERS"))s.lastOrdersError=message;else{s.status="ERROR";s.lastError=message;}
            saveRun(input.id,operation,"ERROR",0,0,message);audit.record(operation.equals("ORDERS")?"STORE_ORDERS_SYNC_FAILED":"STORE_SYNC_FAILED","store:"+input.id,message);
        }else{
            var observed=LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            if(operation.equals("ORDERS")){
                for(var item:received.orders){var row=orders.findByStoreIdAndExternalId(input.id,item.externalId()).orElseGet(StoreOrderSnapshot::new);
                    if(row.modifiedAtUtc!=null&&row.modifiedAtUtc.isAfter(item.modifiedAtUtc())){skipped++;continue;}
                    row.storeId=input.id;row.externalId=item.externalId();row.status=item.status();row.currency=item.currency();row.total=item.total();row.refundedTotal=item.refundedTotal();row.createdAtUtc=item.createdAtUtc();row.modifiedAtUtc=item.modifiedAtUtc();row.completedAtUtc=item.completedAtUtc();row.observedAt=observed;
                    row.linesJson=json.writeValueAsString(item.lines().stream().map(line->{var value=new LinkedHashMap<String,Object>();value.put("productId",line.productId());value.put("quantity",line.quantity());value.put("total",line.total().toPlainString());return value;}).toList());orders.save(row);imported++;
                }
                s.lastOrdersSync=observed;s.lastOrdersError=null;note="Снимки заказов без контактов. Локальные заказы и остатки не изменялись. Устаревших записей пропущено: "+skipped;
            }else{
                if(input.platform.equals("WOOCOMMERCE")){
                    if(operation.equals("SYNC")){
                        var previous=snapshots.findByStoreIdOrderByNameAsc(input.id);var ids=new HashMap<String,Long>();previous.forEach(p->ids.put(p.externalId,p.id));var incoming=new HashSet<String>();var rows=new ArrayList<StoreSnapshot>();
                        for(var p:received.catalog.items()){var row=new StoreSnapshot();row.id=ids.get(p.externalId());row.storeId=input.id;row.externalId=p.externalId();row.parentExternalId=p.parentId();row.name=p.name();row.sku=p.sku();row.price=p.price();row.availableQuantity=p.quantity();row.observedAt=observed;row.currency=received.catalog.currency();incoming.add(row.externalId);rows.add(row);}
                        snapshots.deleteAll(previous.stream().filter(p->!incoming.contains(p.externalId)).toList());snapshots.saveAll(rows);s.lastSync=observed;s.catalogCurrency=received.catalog.currency();s.currencyConfirmedAt=observed;imported=rows.size();skipped=received.catalog.skipped();
                    }
                    integrations.findByStoreId(input.id).orElseThrow(Obsolete::new).status="READ_VERIFIED";
                }else s.lastSync=observed;
                s.status="CONNECTED";s.lastError=null;note=input.platform.equals("ARIA_DEMO")?"Проверен существующий демо-каталог; новые товары не импортировались":"Только чтение. Пропущено неподдерживаемых типов: "+skipped;
            }
            saveRun(input.id,operation,"SUCCESS",imported,skipped,note);
            audit.record(operation.equals("ORDERS")?"STORE_ORDERS_SYNCED":operation.equals("CHECK")?"STORE_ACCESS_CHECKED":"STORE_SYNCED","store:"+input.id,operation.equals("ORDERS")?note:"Только чтение. Внешние цены и склад не менялись");
        }
        em.flush();if(input.claim!=null&&!queue.isActive(input.claim))throw new Obsolete();
    }
    private void saveRun(Long id,String operation,String status,int imported,int skipped,String note){var run=new StoreSyncRun();run.storeId=id;run.operation=operation;run.status=status;run.imported=imported;run.skipped=skipped;run.note=note.length()>500?note.substring(0,500):note;run.completedAt=LocalDateTime.now();runs.save(run);}
    private String snapshotStamp(Long id){
        try{var bytes=new java.io.ByteArrayOutputStream();var data=new java.io.DataOutputStream(bytes);
            for(var row:snapshots.findByStoreIdOrderByNameAsc(id).stream().sorted(Comparator.comparing(p->p.externalId)).toList())for(Object value:new Object[]{row.id,row.externalId,row.parentExternalId,row.name,row.sku,row.price==null?null:row.price.stripTrailingZeros().toPlainString(),row.availableQuantity,row.currency,row.observedAt}){
                if(value==null)data.writeInt(-1);else{var encoded=value.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);data.writeInt(encoded.length);data.write(encoded);}
            }
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        }catch(java.io.IOException|java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException("Не удалось проверить версию каталога",impossible);}
    }
}
