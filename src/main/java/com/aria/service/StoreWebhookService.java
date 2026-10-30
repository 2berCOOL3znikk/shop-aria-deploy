package com.aria.service;
import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.integration.IntegrationVault;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.nio.charset.StandardCharsets;
@Service @Transactional
public class StoreWebhookService {
 private final StoreWebhookReceiverRepository receivers;private final StoreWebhookReceiptRepository receipts;private final ConnectedStoreRepository stores;private final ExternalIntegrationRepository integrations;private final IntegrationVault vault;private final StoreSyncQueue queue;private final AuditService audit;
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
 public StoreWebhookService(StoreWebhookReceiverRepository r,StoreWebhookReceiptRepository d,ConnectedStoreRepository s,ExternalIntegrationRepository i,IntegrationVault v,StoreSyncQueue q,AuditService a){receivers=r;receipts=d;stores=s;integrations=i;vault=v;queue=q;audit=a;}
 public Map<String,Object> settings(Long id){var store=stores.findById(id).orElseThrow(()->new IllegalArgumentException("Магазин не найден"));var receiver=receivers.findById(id).orElse(null);var result=new LinkedHashMap<String,Object>();result.put("version",store.version);result.put("enabled",receiver!=null&&receiver.enabled);result.put("productsPath",receiver!=null&&receiver.enabled?path(receiver,"products"):null);result.put("ordersPath",receiver!=null&&receiver.enabled?path(receiver,"orders"):null);result.put("lastAcceptedAt",receiver==null?null:receiver.lastAcceptedAt);result.put("received",receipts.countByStoreId(id));result.put("events",receipts.findTop20ByStoreIdOrderByIdDesc(id).stream().map(e->{var event=new LinkedHashMap<String,Object>();event.put("id",e.id);event.put("resource",e.resource);event.put("taskId",e.taskId);event.put("status",e.status);event.put("receivedAt",e.receivedAt);return event;}).toList());return result;}
 public Object enable(Long id,long version){var store=stores.lockById(id).orElseThrow(()->new IllegalArgumentException("Магазин не найден"));fresh(store,version);em.lock(store,jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT);if(!store.platform.equals("WOOCOMMERCE")||store.status.equals("DISCONNECTED"))throw new IllegalArgumentException("Настройте подключение WooCommerce");if(integrations.findByStoreId(id).map(i->i.encryptedCredentials==null).orElse(true))throw new IllegalArgumentException("Сначала сохраните ключи чтения магазина");
  String secret=random();var receiver=receivers.lockByStoreId(id).orElseGet(StoreWebhookReceiver::new);receiver.storeId=id;receiver.routeToken=random();receiver.encryptedSecret=vault.encrypt(secret);receiver.enabled=true;receiver.lastAcceptedAt=null;receivers.saveAndFlush(receiver);audit.record("STORE_WEBHOOK_ENABLED","store:"+id,"Новые адреса и подпись. Предыдущие адреса отозваны");var result=settings(id);result.put("secret",secret);return result;
 }
 public Object disable(Long id,long version){var store=stores.lockById(id).orElseThrow(()->new IllegalArgumentException("Магазин не найден"));fresh(store,version);em.lock(store,jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT);revoke(id);em.flush();audit.record("STORE_WEBHOOK_DISABLED","store:"+id,"Новые уведомления не принимаются; уже принятые задания сохранены");return settings(id);}
 public void revoke(Long id){stores.lockById(id).orElseThrow();receivers.lockByStoreId(id).ifPresent(r->{r.enabled=false;r.encryptedSecret=null;});}
 public boolean receive(String token,String resource,String signature,String contentType,byte[] body){
  if(!Set.of("products","orders").contains(resource)||!token.matches("[A-Za-z0-9_-]{43}"))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  var receiver=receivers.lockByToken(token).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(!receiver.enabled||receiver.encryptedSecret==null)throw new ResponseStatusException(HttpStatus.GONE);
  // WooCommerce's activation probe is not an authenticated event and must never enqueue work.
  if(contentType!=null&&contentType.toLowerCase(Locale.ROOT).startsWith("application/x-www-form-urlencoded")&&body.length<128&&new String(body,StandardCharsets.US_ASCII).matches("webhook_id=[1-9][0-9]*"))return false;
  verify(vault.decrypt(receiver.encryptedSecret),signature,body);
  try{Object parsed=json.readValue(body,Object.class);if(!(parsed instanceof Map<?,?> map)||!Objects.toString(map.get("id"),"").matches("[1-9][0-9]{0,18}"))throw new IllegalArgumentException();}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Неподдерживаемое уведомление");}
  String hash;try{hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(body));}catch(Exception e){throw new IllegalStateException(e);}
  if(receipts.existsByStoreIdAndGenerationAndResourceAndBodyHash(receiver.storeId,token,resource,hash))return false;
  var receipt=new StoreWebhookReceipt();receipt.storeId=receiver.storeId;receipt.generation=token;receipt.resource=resource;receipt.bodyHash=hash;receipt.status="PENDING";receipt.bodyBytes=body.length;receipt.receivedAt=java.time.LocalDateTime.now();receipts.saveAndFlush(receipt);receiver.lastAcceptedAt=receipt.receivedAt;return true;
 }
 public boolean hasPending(){return receipts.existsByStatus("PENDING");}
 public boolean dispatch(){
  var first=receipts.findFirstByStatusOrderByIdAsc("PENDING");if(first.isEmpty())return false;
  var store=stores.lockById(first.get().storeId).orElseThrow();var receiver=receivers.lockByStoreId(store.id).orElse(null);var batch=receipts.findTop100ByStoreIdAndStatusOrderByIdAsc(store.id,"PENDING");var tasks=new HashMap<String,Long>();
  for(var event:batch){if(receiver==null||!receiver.enabled||!Objects.equals(receiver.routeToken,event.generation)||store.status.equals("DISCONNECTED")){event.status="CANCELLED";continue;}
   Long taskId=tasks.get(event.resource);if(taskId==null){taskId=queue.enqueue(store.id,store.version,event.resource.equals("products")?"SYNC":"ORDERS").id;tasks.put(event.resource,taskId);}event.taskId=taskId;event.status="QUEUED";
  }
  return !batch.isEmpty();
 }
 private void verify(String secret,String signature,byte[] body){try{if(signature==null||signature.length()>100)throw new IllegalArgumentException();var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));if(!java.security.MessageDigest.isEqual(mac.doFinal(body),Base64.getDecoder().decode(signature)))throw new IllegalArgumentException();}catch(Exception e){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Подпись уведомления не подтверждена");}}
 private String random(){byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
 private String path(StoreWebhookReceiver r,String resource){return "/api/webhooks/woocommerce/"+r.routeToken+"/"+resource;}
 private void fresh(ConnectedStore s,long version){if(s.version!=version)throw new ResponseStatusException(HttpStatus.CONFLICT,"Данные изменились. Обновите магазины");}
}
