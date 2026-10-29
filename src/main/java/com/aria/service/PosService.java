package com.aria.service;
import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.service.integrations.PosAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
@Service
public class PosService implements PosAdapter {
 private final PosReceiptRepository receipts;private final OrderRepository orders;private final OrderAllocationRepository allocations;
 private final InventoryService inventory;private final PricingService pricing;private final AuditService audit;
 public PosService(PosReceiptRepository r,OrderRepository o,OrderAllocationRepository a,InventoryService i,PricingService p,AuditService log){receipts=r;orders=o;allocations=a;inventory=i;pricing=p;audit=log;}
 private String fingerprint(Sale sale){return PurchaseService.fingerprint(sale.lines().stream().sorted(Comparator.comparing(Line::productId)).map(l->l.productId()+":"+l.quantity()+":"+l.expectedPrice().stripTrailingZeros().toPlainString()).collect(java.util.stream.Collectors.joining("|")));}
 @Transactional public Object record(Sale sale,String operator){
  var old=receipts.findByRequestKey(sale.requestKey());if(old.isPresent())return replay(sale,operator);
  if(sale.lines().stream().map(Line::productId).distinct().count()!=sale.lines().size())throw new IllegalArgumentException("Объедините повторяющиеся товары в одну строку");
  var locked=new HashMap<Long,Product>();for(var line:sale.lines().stream().sorted(Comparator.comparing(Line::productId)).toList())locked.put(line.productId(),inventory.lock(line.productId()));
  // A concurrent retry may have waited for these locks while the first receipt committed.
  if(receipts.findByRequestKey(sale.requestKey()).isPresent())return replay(sale,operator);
  var order=new Order();order.setStatus("DELIVERED");order.setShippedAt(LocalDateTime.now());order.setOwnerEmail("pos:"+PurchaseService.fingerprint(operator).substring(0,40));order.setDeliveryAddress("Продажа в магазине · внутренний POS, без эквайринга");BigDecimal total=BigDecimal.ZERO;List<InventoryService.Consumption> consumed=new ArrayList<>();
  for(var line:sale.lines().stream().sorted(Comparator.comparing(Line::productId)).toList()){
   var p=locked.get(line.productId());inventory.synchronize(p);if(p.getArchived()||!Set.of(com.aria.enums.ProductStatus.ACTIVE,com.aria.enums.ProductStatus.DISCOUNTED).contains(p.getStatus()))throw new IllegalStateException("Товар недоступен для продажи: "+p.getName());
   pricing.validate(p,p.getDiscountPercent());var price=pricing.price(p,p.getDiscountPercent());if(price.compareTo(line.expectedPrice())!=0)throw new IllegalStateException("Цена изменилась: "+p.getName()+". Обновите POS.");
   total=total.add(price.multiply(BigDecimal.valueOf(line.quantity())));if(total.compareTo(new BigDecimal("9999999999.99"))>0)throw new IllegalArgumentException("Сумма превышает предел учёта");order.getItems().add(new OrderItem(p,line.quantity(),price));consumed.addAll(inventory.consume(p,line.quantity(),"POS_SALE_STOCK"));
  }
  order.setTotalAmount(total);orders.saveAndFlush(order);for(var c:consumed)allocations.save(new OrderAllocation(order,c.lot(),c.quantity()));
  var receipt=new PosReceipt();receipt.requestKey=sale.requestKey();receipt.fingerprint=fingerprint(sale);receipt.orderId=order.getId();receipt.cashier=operator;receipts.saveAndFlush(receipt);audit.record("POS_SALE_RECORDED","receipt:"+receipt.id,"Заказ "+order.getId()+" · "+total+" ₸ · внутренний нефискальный учёт; оплата не проводится");return view(receipt);
 }
 @Transactional(readOnly=true) public Object replay(Sale sale,String operator){var r=receipts.findByRequestKey(sale.requestKey()).orElseThrow(()->new IllegalStateException("Продажа ещё не завершена. Повторите запрос с тем же ключом позже"));if(!r.cashier.equals(operator)||!r.fingerprint.equals(fingerprint(sale)))throw new IllegalStateException("Ключ уже использован для другого состава или оператора");return view(r);}
 @Transactional public Object returnToQuarantine(Long id,String reason){var r=receipts.lockById(id).orElseThrow(()->new IllegalArgumentException("Чек не найден"));if("RETURN_QUARANTINE".equals(r.status))return view(r);if(!"SALE".equals(r.status))throw new IllegalStateException("Возврат недоступен");r.status="RETURN_QUARANTINE";r.returnedAt=LocalDateTime.now();r.returnReason=reason;
  // Returned goods are not sellable before physical assessment. Correct net analytics, retain the original sale.
  var order=orders.findLockedById(r.orderId).orElseThrow();order.setStatus("RETURNED");for(var allocation:allocations.findByOrderId(order.getId()))inventory.movement(allocation.getLot(),0,allocation.getQuantity(),"RETURN_QUARANTINE","POS #"+id+": "+reason);audit.record("POS_RETURN_QUARANTINE","receipt:"+id,reason+" · количество не возвращено в продажу; деньги не возвращаются автоматически");return view(r);}
 private Map<String,Object> view(PosReceipt r){return Map.of("receipt",r,"order",com.aria.dto.AdminViews.OrderRow.from(orders.findById(r.orderId).orElseThrow()),"fiscal",false,"paymentProcessed",false,"note","Внутренний чек. Эквайринг и фискализация не подключены. Возврат регистрируется в карантине; пригодный остаток сам не увеличивается.");}
 @Transactional(readOnly=true) public List<Map<String,Object>> list(){return receipts.findTop100ByOrderByCreatedAtDescIdDesc().stream().map(this::view).toList();}
}
