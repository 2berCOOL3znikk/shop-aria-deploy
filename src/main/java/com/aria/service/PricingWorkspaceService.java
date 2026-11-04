package com.aria.service;

import com.aria.entity.PricingDecision;
import com.aria.entity.Product;
import com.aria.repository.PricingDecisionRepository;
import com.aria.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** Read model for an owner's current decisions. Pricing/apply constraints stay in the existing services. */
@Service
public class PricingWorkspaceService {
    public enum View {ALL,ATTENTION,AUTOMATIC,PENDING,UNCHANGED,ERROR}
    private final PricingDecisionRepository decisions;
    private final ProductRepository products;
    private final SmartPricingService smart;
    private final PricingService pricing;
    private final InventoryService inventory;
    private final EntityManager em;
    private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
    private record LiveLabel(String search,java.math.BigDecimal price,int quantity) {}
    public PricingWorkspaceService(PricingDecisionRepository d,ProductRepository p,SmartPricingService s,PricingService price,InventoryService i,EntityManager em){decisions=d;products=p;smart=s;pricing=price;inventory=i;this.em=em;}

    static Set<View> categories(PricingDecision d,Map<?,?> report,boolean automaticallyApplied){
        var tags=EnumSet.of(View.ALL);
        if(!(report.get("facts") instanceof Map<?,?> facts)||!(facts.get("currentPercent") instanceof Number current)||!(report.get("suggestedPercent") instanceof Number suggested)){
            tags.add(View.ERROR);tags.add(View.ATTENTION);return tags;
        }
        int chosen="AI".equals(d.source)&&d.selectedPercent!=null?d.selectedPercent:suggested.intValue();
        String action=String.valueOf(report.get("action"));
        boolean pending="PENDING".equals(d.status),blocked=Set.of("BLOCKED","REVIEW").contains(action);
        boolean pricingStrategy=Set.of("PRICE","KEEP").contains(d.strategy==null?"PRICE":d.strategy);
        boolean unchanged=!blocked&&pricingStrategy&&chosen==current.intValue();
        if(unchanged)tags.add(View.UNCHANGED);
        if(pending&&!blocked&&pricingStrategy&&!unchanged)tags.add(View.PENDING);
        if(pending&&(!unchanged||d.aiLastSkippedAt!=null))tags.add(View.ATTENTION);
        if(automaticallyApplied&&"APPLIED".equals(d.status))tags.add(View.AUTOMATIC);
        // A completed external attempt that retained ENGINE is an actual fallback.
        // A request skipped for lack of consent is not a failed model response.
        if("ENGINE".equals(d.source)&&d.aiCompletedAt!=null){tags.add(View.ERROR);tags.add(View.ATTENTION);}
        return tags;
    }
    private Map<?,?> report(PricingDecision d){try{return json.readValue(d.reportJson,Map.class);}catch(RuntimeException invalid){return Map.of();}}

    @Transactional(readOnly=true)
    public Map<String,Object> list(String selected,String query,int page,int size){
        View filter;
        try{filter=View.valueOf(selected);}catch(Exception invalid){throw new IllegalArgumentException("Неизвестный фильтр решений");}
        if(page<0||page>1000000||size<1||size>100||query==null||query.length()>200)throw new IllegalArgumentException("Проверьте фильтр и страницу решений");
        String search=query.trim().toLowerCase(Locale.ROOT).replace('ё','е');
        var labels=new HashMap<Long,LiveLabel>();
        for(Object[] p:em.createQuery("select p.id,p.name,p.sku,p.category,p.unitPrice,p.discountPercent,p.quantityOnHand from Product p where p.archived=false",Object[].class).getResultList()){
            var priceOnly=new Product();priceOnly.setUnitPrice((java.math.BigDecimal)p[4]);
            var price=livePrice(priceOnly,p[5] instanceof Number percent?percent.intValue():0);
            labels.put((Long)p[0],new LiveLabel((p[1]+" "+p[2]+" "+p[3]).toLowerCase(Locale.ROOT).replace('ё','е'),price,((Number)p[6]).intValue()));
        }
        var autoResources=new HashSet<>(em.createQuery("select a.resource from AuditEvent a where a.action='SMART_AUTOPILOT_APPLIED'",String.class).getResultList());
        var counts=new LinkedHashMap<String,Long>();for(var v:View.values())counts.put(v.name(),0L);
        var chosen=new ArrayList<PricingDecision>();long[] total={0};long offset=(long)page*size;
        // Stream and detach instead of keeping every historical report in a persistence context.
        try(var stream=decisions.streamCurrentWorkspace()){
            stream.forEach(d->{
                var r=report(d);
                var label=labels.get(d.productId);
                if(!search.isBlank()&&(label==null||!label.search().contains(search))){em.detach(d);return;}
                var tags=categories(d,r,autoResources.contains("decision:"+d.id));
                var facts=r.get("facts") instanceof Map<?,?> f?f:Map.of();
                if("PENDING".equals(d.status)&&(d.reviewAt.isBefore(LocalDateTime.now())||label==null||!decimalEqual(label.price(),facts.get("currentPrice"))||!Objects.equals(label.quantity(),facts.get("quantity") instanceof Number n?n.intValue():null))){tags.add(View.ATTENTION);tags.remove(View.PENDING);tags.remove(View.UNCHANGED);}
                if(d.explanationJson!=null)try{json.readValue(d.explanationJson,Map.class);}catch(RuntimeException invalid){tags.add(View.ERROR);tags.add(View.ATTENTION);}
                for(var v:tags)counts.compute(v.name(),(key,value)->value+1);
                if(tags.contains(filter)){long index=total[0]++;if(index>=offset&&index<offset+size)chosen.add(d);}
                em.detach(d);
            });
        }
        var live=new HashMap<Long,Product>();products.findAllById(chosen.stream().map(d->d.productId).distinct().toList()).forEach(p->live.put(p.getId(),p));
        var content=chosen.stream().map(d->enrich(d,live.get(d.productId))).toList();
        return Map.of("content",content,"page",page,"totalPages",(int)((total[0]+size-1)/size),"totalElements",total[0],"counts",counts,"view",filter.name(),"asOf",LocalDateTime.now(),"scope","LATEST_PER_ACTIVE_PRODUCT");
    }
    private Map<String,Object> enrich(PricingDecision d,Product p){
        Map<String,Object> row;
        try{row=smart.view(d);}catch(RuntimeException invalid){row=new LinkedHashMap<>();row.put("id",d.id);row.put("productId",d.productId);row.put("status",d.status);row.put("source",d.source);row.put("report",null);row.put("readError",true);}
        if(p!=null){
            var live=new LinkedHashMap<String,Object>();live.put("name",p.getName());live.put("sku",p.getSku());live.put("category",p.getCategory());live.put("price",livePrice(p,p.getDiscountPercent()==null?0:p.getDiscountPercent()));live.put("discountPercent",p.getDiscountPercent());live.put("quantity",inventory.available(p));live.put("imageUrl",p.getImageUrl());live.put("archived",p.getArchived());row.put("live",live);
            var report=report(d);var facts=report.get("facts") instanceof Map<?,?> f?f:Map.of();
            boolean stale="PENDING".equals(d.status)&&(d.reviewAt.isBefore(LocalDateTime.now())||!decimalEqual(live.get("price"),facts.get("currentPrice"))||!Objects.equals(((Number)live.get("quantity")).intValue(),facts.get("quantity") instanceof Number n?n.intValue():null));
            row.put("stale",stale);
        }
        return row;
    }
    private java.math.BigDecimal livePrice(Product p,int percent){try{return pricing.price(p,percent);}catch(IllegalArgumentException invalid){return null;}}
    private boolean decimalEqual(Object a,Object b){try{return a!=null&&b!=null&&new java.math.BigDecimal(a.toString()).compareTo(new java.math.BigDecimal(b.toString()))==0;}catch(NumberFormatException invalid){return false;}}
}
