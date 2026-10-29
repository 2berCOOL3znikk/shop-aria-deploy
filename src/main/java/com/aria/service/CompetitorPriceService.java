package com.aria.service;
import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.service.pricing.CompetitorCsv;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.*;
import java.math.*;
import java.util.*;

@Service @Transactional
public class CompetitorPriceService {
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.context.ApplicationEventPublisher events;
    private final CompetitorSourceRepository sources;private final CompetitorQuoteRepository quotes;
    private final ProductRepository products;private final InventoryService inventory;private final PricingService pricing;private final AuditService audit;
    public CompetitorPriceService(CompetitorSourceRepository sources,CompetitorQuoteRepository quotes,ProductRepository products,InventoryService inventory,PricingService pricing,AuditService audit){this.sources=sources;this.quotes=quotes;this.products=products;this.inventory=inventory;this.pricing=pricing;this.audit=audit;}
    public CompetitorSource saveSource(Long id,String name,String url,boolean active){
        URI uri=publicUrl(url);var s=id==null?new CompetitorSource():sources.findById(id).orElseThrow(()->new IllegalArgumentException("Источник не найден"));
        s.name=name.trim();s.baseUrl="https://"+uri.getHost().toLowerCase(Locale.ROOT)+(uri.getPort()==-1?"":":"+uri.getPort());s.active=active;sources.saveAndFlush(s);
        audit.record("COMPETITOR_SOURCE_SAVED","source:"+s.id,s.name+" · разрешённый ручной источник · активен "+active);return s;
    }
    @Transactional(readOnly=true) public List<CompetitorSource> sources(){return sources.findAll();}
    public Map<String,Object> importCsv(Long sourceId,String csv,boolean identityConfirmed){
        if(!identityConfirmed)throw new IllegalArgumentException("Подтвердите совпадение упаковки и характеристик товара");
        var source=sources.findById(sourceId).filter(s->s.active).orElseThrow(()->new IllegalArgumentException("Выберите активный разрешённый источник"));
        int imported=0,duplicates=0,line=1;
        for(var row:CompetitorCsv.read(csv)){
            line++;try{
                if(row.size()!=7)throw new IllegalArgumentException("Требуется 7 столбцов");
                var p=products.findBySkuIgnoreCase(row.get(0)).orElseThrow(()->new IllegalArgumentException("SKU не найден"));
                p=inventory.lock(p.getId());if(p.getArchived())throw new IllegalArgumentException("Товар архивирован");
                String barcode=row.get(1),unit=row.get(2);
                if(!validGtin(barcode)||!barcode.equals(p.getBarcode())||!unit.equalsIgnoreCase(p.getUnit()))throw new IllegalArgumentException("Должны совпадать корректный GTIN/EAN и единица упаковки в карточке товара");
                BigDecimal price=new BigDecimal(row.get(3));if(price.scale()>2||price.signum()<=0||price.compareTo(new BigDecimal("9999999999.99"))>0)throw new IllegalArgumentException("Недопустимая цена");
                if(!"KZT".equals(row.get(4)))throw new IllegalArgumentException("Поддерживается только KZT без выдуманного курса");
                Instant observed=OffsetDateTime.parse(row.get(5)).toInstant();if(observed.isAfter(Instant.now())||observed.isBefore(Instant.now().minus(Duration.ofDays(30))))throw new IllegalArgumentException("Дата с часовым поясом: не в будущем и не старше 30 дней");
                publicUrl(row.get(6));
                if(!sameOrigin(row.get(6),source.baseUrl))throw new IllegalArgumentException("URL должен принадлежать разрешённому источнику");
                var old=quotes.findBySourceIdAndProductIdAndObservedAt(sourceId,p.getId(),observed);
                if(old.isPresent()){var q=old.get();if(q.price.compareTo(price)!=0||!q.barcode.equals(barcode)||!q.unit.equalsIgnoreCase(unit)||!q.url.equals(row.get(6)))throw new IllegalArgumentException("Запись с этой датой уже есть с другими данными");duplicates++;continue;}
                var q=new CompetitorQuote();q.sourceId=sourceId;q.productId=p.getId();q.barcode=barcode;q.unit=unit;q.price=price;q.observedAt=observed;q.url=row.get(6);quotes.saveAndFlush(q);events.publishEvent(new StoreChanged(p.getId(),"COMPETITOR_QUOTE"));imported++;
            }catch(IllegalArgumentException|java.time.DateTimeException e){throw new IllegalArgumentException("Строка "+line+": "+(e instanceof NumberFormatException?"Неверный формат цены":e instanceof java.time.DateTimeException?"Неверная дата с часовым поясом":e.getMessage()));}
        }
        audit.record("COMPETITOR_CSV_IMPORTED","source:"+sourceId,"Добавлено "+imported+", повторов "+duplicates);return Map.of("imported",imported,"duplicates",duplicates);
    }
    private URI publicUrl(String value){
        try{var uri=URI.create(value);if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getFragment()!=null||value.length()>1000)throw new IllegalArgumentException();return uri;}
        catch(RuntimeException e){throw new IllegalArgumentException("Нужна публичная HTTPS-ссылка без пароля и фрагмента");}
    }
    public static boolean validGtin(String value){
        if(value==null||!value.matches("(?:\\d{8}|\\d{12}|\\d{13}|\\d{14})"))return false;int sum=0,weight=3;
        for(int i=value.length()-2;i>=0;i--){sum+=(value.charAt(i)-'0')*weight;weight=weight==3?1:3;}return (10-sum%10)%10==value.charAt(value.length()-1)-'0';
    }
    @Transactional(readOnly=true) public Map<String,Object> context(Product p){
        var approved=new HashMap<Long,CompetitorSource>();sources.findAll().stream().filter(s->s.active).forEach(s->approved.put(s.id,s));
        var latest=new LinkedHashMap<Long,CompetitorQuote>();var since=Instant.now().minus(Duration.ofDays(7));
        for(var q:quotes.findByProductIdOrderByObservedAtDescIdDesc(p.getId()))if(approved.containsKey(q.sourceId)&&sameOrigin(q.url,approved.get(q.sourceId).baseUrl)&&"KZT".equals(q.currency)&&q.price.signum()>0&&!q.observedAt.isBefore(since)&&q.barcode.equals(p.getBarcode())&&q.unit.equalsIgnoreCase(p.getUnit()))latest.putIfAbsent(q.sourceId,q);
        if(latest.isEmpty())return Map.of("status","NO_VERIFIED_PRICES","note","Нет свежих сопоставимых импортированных цен. Автосбор не подключён.");
        var values=latest.values().stream().map(q->q.price).sorted().toList();int n=values.size();var median=n%2==1?values.get(n/2):values.get(n/2-1).add(values.get(n/2)).divide(BigDecimal.valueOf(2),2,RoundingMode.HALF_UP);
        var current=pricing.price(p,p.getDiscountPercent());var delta=current.subtract(median).multiply(BigDecimal.valueOf(100)).divide(median,2,RoundingMode.HALF_UP);
        var rows=latest.values().stream().map(q->Map.of("source",approved.get(q.sourceId).name,"price",q.price,"currency",q.currency,"observedAt",q.observedAt,"url",q.url,"match","EAN_EXACT_OPERATOR_CONFIRMED")).toList();
        return Map.of("status","AVAILABLE","medianPrice",median,"differencePercent",delta,"position",delta.compareTo(new BigDecimal("2"))>0?"HIGHER":delta.compareTo(new BigDecimal("-2"))<0?"LOWER":"SIMILAR","offers",rows,"note","Ручной импорт, GTIN и упаковку подтвердил сотрудник. Свежесть до 7 дней. Медиана не доказывает спрос или выгодность изменения цены.");
    }
    @Transactional(readOnly=true) public Map<String,Object> product(Long id){var p=products.findById(id).orElseThrow(()->new IllegalArgumentException("Товар не найден"));return Map.of("productId",id,"sku",p.getSku(),"comparison",context(p),"history",quotes.findByProductIdOrderByObservedAtDescIdDesc(id));}
    private boolean sameOrigin(String a,String b){try{var first=URI.create(a);var second=URI.create(b);return "https".equalsIgnoreCase(first.getScheme())&&first.getHost()!=null&&first.getHost().equalsIgnoreCase(second.getHost())&&(first.getPort()==-1?443:first.getPort())==(second.getPort()==-1?443:second.getPort());}catch(RuntimeException e){return false;}}
}
