package com.aria.service;

import com.aria.entity.*;
import com.aria.enums.ProductStatus;
import com.aria.repository.InventoryLotRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

/** Read-only native lot workflow. Product-level social reserves are never assigned to a guessed lot. */
@Service @Transactional(readOnly=true)
public class ProductFlowService {
 public enum View {ALL,AVAILABLE,REVIEW,BLOCKED,DISCOUNTED,SOCIAL,CLOSED}
 private final InventoryLotRepository lots;private final PolicyService policies;private final InventoryService inventory;private final EntityManager em;private final PricingService pricing;
 public ProductFlowService(InventoryLotRepository l,PolicyService p,InventoryService i,EntityManager e,PricingService pricing){lots=l;policies=p;inventory=i;em=e;this.pricing=pricing;}
 private Map<String,PricingPolicy> rules(){var result=new HashMap<String,PricingPolicy>();policies.list().forEach(p->result.put(p.getCategory(),p));return result;}
 private int days(Product p,Map<String,PricingPolicy> rules){return rules.getOrDefault(PolicyService.categoryKey(p.getCategory()),rules.getOrDefault("*",new PricingPolicy())).getCriticalDays();}
 private View state(InventoryLot l,Map<String,PricingPolicy> rules){var p=l.getProduct();if(p.getArchived()||!l.getActive()||l.getQuantity()==0)return View.CLOSED;if(!inventory.usable(l))return View.BLOCKED;if(Set.of(ProductStatus.SOCIAL,ProductStatus.RESERVED).contains(p.getStatus()))return View.SOCIAL;if(p.getDiscountPercent()!=null&&p.getDiscountPercent()>0)return View.DISCOUNTED;if(l.getExpiryDate()!=null&&!l.getExpiryDate().isAfter(LocalDate.now().plusDays(days(p,rules))))return View.REVIEW;return View.AVAILABLE;}
 private Specification<InventoryLot> specification(String search,View view,Map<String,PricingPolicy> rules){return (lot,query,cb)->{
  var p=lot.join("product");var open=cb.and(cb.isFalse(p.get("archived")),cb.isTrue(lot.get("active")),cb.greaterThan(lot.get("quantity"),0));
  var bad=cb.or(cb.equal(lot.get("handlingStatus"),"REJECTED"),cb.lessThan(lot.<LocalDate>get("expiryDate"),LocalDate.now()));var usable=cb.and(open,cb.not(cb.equal(lot.get("handlingStatus"),"REJECTED")),cb.or(cb.isNull(lot.get("expiryDate")),cb.greaterThanOrEqualTo(lot.get("expiryDate"),LocalDate.now())));
  var social=p.get("status").in(ProductStatus.SOCIAL,ProductStatus.RESERVED);var discount=cb.greaterThan(cb.coalesce(p.<Integer>get("discountPercent"),0),0);var expiry=new ArrayList<Predicate>();var category=cb.lower(cb.trim(p.<String>get("category")));var specific=rules.keySet().stream().filter(k->!k.equals("*")).toList();
  for(var key:specific)expiry.add(cb.and(cb.equal(category,key),cb.lessThanOrEqualTo(lot.<LocalDate>get("expiryDate"),LocalDate.now().plusDays(rules.get(key).getCriticalDays()))));
  expiry.add(cb.and(specific.isEmpty()?cb.conjunction():cb.not(category.in(specific)),cb.lessThanOrEqualTo(lot.<LocalDate>get("expiryDate"),LocalDate.now().plusDays(rules.getOrDefault("*",new PricingPolicy()).getCriticalDays()))));var review=cb.or(expiry.toArray(Predicate[]::new));
  Predicate selected=switch(view){case ALL->cb.conjunction();case CLOSED->cb.not(open);case BLOCKED->cb.and(open,bad);case SOCIAL->cb.and(usable,social);case DISCOUNTED->cb.and(usable,cb.not(social),discount);case REVIEW->cb.and(usable,cb.not(social),cb.not(discount),review);case AVAILABLE->cb.and(usable,cb.not(social),cb.not(discount),cb.or(cb.isNull(lot.get("expiryDate")),cb.not(review)));};
  if(search.isBlank())return selected;String pattern="%"+search.trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
  return cb.and(selected,cb.or(cb.like(cb.lower(p.get("name")),pattern,'\\'),cb.like(cb.lower(p.get("sku")),pattern,'\\'),cb.like(cb.lower(lot.get("lotNumber")),pattern,'\\')));
 };}
 public Map<String,Object> list(String search,String selected,int page,int size){
  View view;try{view=View.valueOf(selected);}catch(Exception bad){throw new IllegalArgumentException("Неизвестный этап товара");}if(search==null||search.length()>200||page<0||page>1000000||size<1||size>100)throw new IllegalArgumentException("Проверьте поиск и страницу пути товара");
  var rules=rules();var result=lots.findAll(specification(search,view,rules),PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"receivedAt","id")));var counts=new LinkedHashMap<String,Long>();for(var item:View.values())counts.put(item.name(),lots.count(specification(search,item,rules)));
  return Map.of("content",result.getContent().stream().map(l->row(l,rules)).toList(),"counts",counts,"page",page,"totalPages",result.getTotalPages(),"totalElements",result.getTotalElements(),"scope","NATIVE_LOTS","asOf",LocalDateTime.now());
 }
 private java.math.BigDecimal livePrice(Product p){try{return pricing.price(p,p.getDiscountPercent()==null?0:p.getDiscountPercent());}catch(IllegalArgumentException missing){return null;}}
 private Map<String,Object> row(InventoryLot lot,Map<String,PricingPolicy> rules){var p=lot.getProduct();var value=new LinkedHashMap<String,Object>();value.put("id",lot.getId());value.put("productId",p.getId());value.put("name",p.getName());value.put("sku",p.getSku());value.put("lotNumber",lot.getLotNumber());value.put("quantity",lot.getQuantity());value.put("initialQuantity",lot.getInitialQuantity());value.put("unit",p.getUnit());value.put("receivedAt",lot.getReceivedAt());value.put("expiryDate",lot.getExpiryDate());value.put("handlingStatus",lot.handlingStatus);value.put("stage",state(lot,rules));value.put("currentPrice",livePrice(p));value.put("discountPercent",p.getDiscountPercent());value.put("archived",p.getArchived());return value;}
 public Map<String,Object> detail(Long id){var lot=lots.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Партия не найдена"));var value=new LinkedHashMap<>(row(lot,rules()));value.put("manufacturedAt",lot.getManufacturedAt());value.put("storageRequirements",lot.storageRequirements);value.put("handlingCheckedAt",lot.handlingCheckedAt);value.put("unitCost",lot.getUnitCost());
  value.put("movements",em.createQuery("select m from InventoryMovement m where m.lotId=:id order by m.createdAt desc,m.id desc",InventoryMovement.class).setParameter("id",id).setMaxResults(30).getResultList().stream().map(m->Map.of("id",m.id,"type",m.type,"quantityDelta",m.quantityDelta,"at",m.createdAt,"reason",m.reason)).toList());
  value.put("orders",em.createQuery("select a.order.id,a.order.status,a.quantity,a.order.shippedAt from OrderAllocation a where a.lot.id=:id order by a.id desc",Object[].class).setParameter("id",id).setMaxResults(10).getResultList().stream().map(r->{var v=new LinkedHashMap<String,Object>();v.put("id",r[0]);v.put("status",r[1]);v.put("quantity",r[2]);v.put("shippedAt",r[3]);return v;}).toList());
  value.put("priceHistory",em.createQuery("select p from PriceChange p where p.productId=:id order by p.changedAt desc,p.id desc",PriceChange.class).setParameter("id",lot.getProduct().getId()).setMaxResults(5).getResultList().stream().map(p->Map.of("at",p.changedAt,"oldPercent",p.oldPercent,"newPercent",p.newPercent,"reason",p.reason)).toList());
  value.put("social",em.createQuery("select o from SocialOffer o where o.product.id=:id order by o.id desc",SocialOffer.class).setParameter("id",lot.getProduct().getId()).setMaxResults(10).getResultList().stream().map(o->{var v=new LinkedHashMap<String,Object>();v.put("id",o.getId());v.put("status",o.getStatus());v.put("reserved",o.getQuantityClaimed());v.put("transferredAt",o.getTransferredAt());return v;}).toList());
  value.put("decisions",em.createQuery("select d from PricingDecision d where d.productId=:id order by d.createdAt desc,d.id desc",PricingDecision.class).setParameter("id",lot.getProduct().getId()).setMaxResults(3).getResultList().stream().map(d->{var v=new LinkedHashMap<String,Object>();v.put("id",d.id);v.put("source",d.source);v.put("strategy",d.strategy);v.put("status",d.status);v.put("createdAt",d.createdAt);v.put("aiCompletedAt",d.aiCompletedAt);v.put("reviewAt",d.reviewAt);return v;}).toList());return value;
 }
}
