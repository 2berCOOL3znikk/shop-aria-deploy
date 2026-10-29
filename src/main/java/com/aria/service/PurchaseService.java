package com.aria.service;
import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.dto.ControlRequests.LotInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;
import java.math.*;
@Service @Transactional
public class PurchaseService {
 private final SupplierQuoteRepository quotes;private final PurchaseRequestRepository requests;private final PurchaseReceiptRepository receipts;
 private final SupplierRepository suppliers;private final InventoryService inventory;private final AiManagerStateRepository manager;private final AuditService audit;
 public PurchaseService(SupplierQuoteRepository q,PurchaseRequestRepository p,PurchaseReceiptRepository r,SupplierRepository s,InventoryService i,AiManagerStateRepository m,AuditService a){quotes=q;requests=p;receipts=r;suppliers=s;inventory=i;manager=m;audit=a;}
 public SupplierQuote quote(Long product,Long supplier,BigDecimal cost,int minimum,int days,LocalDate at,LocalDate until,boolean verified){
  var p=inventory.lock(product);if(p.getArchived())throw new IllegalStateException("Товар архивирован");suppliers.findById(supplier).filter(Supplier::getActive).orElseThrow(()->new IllegalArgumentException("Поставщик неактивен или не найден"));
  if(until.isBefore(LocalDate.now())||until.isBefore(at)||until.isAfter(at.plusDays(365)))throw new IllegalArgumentException("Проверьте срок действия предложения");
  var q=quotes.findByProductIdAndSupplierId(product,supplier).orElseGet(SupplierQuote::new);q.productId=product;q.supplierId=supplier;q.unitCost=cost;q.minimumQuantity=minimum;q.leadDays=days;q.quotedOn=at;q.validUntil=until;q.verified=verified;q.updatedAt=LocalDateTime.now();quotes.save(q);audit.record("SUPPLIER_QUOTE_SAVED","product:"+product,"Поставщик "+supplier+" · цена "+cost+" ₸ · подтверждено "+verified);return q;
 }
 private SupplierQuote validQuote(Long id){var q=quotes.findById(id).orElseThrow(()->new IllegalArgumentException("Предложение не найдено"));if(!q.verified||q.validUntil.isBefore(LocalDate.now()))throw new IllegalStateException("Предложение не подтверждено или срок истёк");suppliers.findById(q.supplierId).filter(Supplier::getActive).orElseThrow(()->new IllegalStateException("Поставщик неактивен"));return q;}
 private static final List<String> OPEN=List.of("DRAFT","APPROVED","PART_RECEIVED");
 private PurchaseRequest create(SupplierQuote q,int quantity,String reason,boolean automatic){
  var p=inventory.lock(q.productId);if(p.getArchived()||Set.of(com.aria.enums.ProductStatus.RESERVED,com.aria.enums.ProductStatus.SOCIAL,com.aria.enums.ProductStatus.TRANSFERRED,com.aria.enums.ProductStatus.WRITTEN_OFF).contains(p.getStatus()))throw new IllegalStateException("Товар недоступен для закупки");
  var old=requests.findFirstByProductIdAndStatusInOrderByCreatedAtDesc(p.getId(),OPEN);if(old.isPresent())return old.get();
  int qty=Math.max(quantity,q.minimumQuantity);if(qty>1000000)throw new IllegalArgumentException("Слишком большое количество");var total=q.unitCost.multiply(BigDecimal.valueOf(qty));if(total.compareTo(new BigDecimal("9999999999.99"))>0)throw new IllegalArgumentException("Сумма заказа превышает предел учёта");
  var r=new PurchaseRequest();r.productId=p.getId();r.supplierId=q.supplierId;r.quoteId=q.id;r.quantity=qty;r.unitCost=q.unitCost;r.estimatedTotal=total;r.productName=p.getName();r.supplierName=suppliers.findById(q.supplierId).orElseThrow().getName();r.reason=reason;r.managerCreated=automatic;requests.saveAndFlush(r);audit.record("PURCHASE_DRAFT_CREATED","purchase:"+r.id,qty+" ед. · "+total+" ₸ · только внутренний черновик");return r;
 }
 public PurchaseRequest create(Long quoteId,int quantity,String reason){var q=validQuote(quoteId);return create(q,quantity,reason,false);}
 public Map<String,Object> prepare(String lease,Long productId,int needed){
  var state=manager.lockState().orElseThrow();if(!Objects.equals(state.lease,lease)||"OFF".equals(state.mode)||!state.purchaseDrafts||state.leaseUntil==null||state.leaseUntil.isBefore(LocalDateTime.now()))return Map.of("status","NOT_ALLOWED");
  inventory.lock(productId);var old=requests.findFirstByProductIdAndStatusInOrderByCreatedAtDesc(productId,OPEN);if(old.isPresent())return Map.of("status","EXISTS","id",old.get().id);
  var candidates=quotes.findByProductId(productId).stream().filter(q->q.verified&&!q.validUntil.isBefore(LocalDate.now())&&suppliers.findById(q.supplierId).map(Supplier::getActive).orElse(false)).sorted(Comparator.comparing((SupplierQuote q)->q.leadDays>14).thenComparing(q->q.unitCost).thenComparing(q->q.id)).toList();
  if(candidates.isEmpty())return Map.of("status","NO_VERIFIED_QUOTE");var q=candidates.getFirst();var total=q.unitCost.multiply(BigDecimal.valueOf(Math.max(needed,q.minimumQuantity)));
  if(state.dailyDraftBudget.signum()<=0||requests.budgetUsed(LocalDate.now().atStartOfDay()).add(total).compareTo(state.dailyDraftBudget)>0)return Map.of("status","DRAFT_BUDGET");
  var r=create(q,needed,"ARIA Manager: минимум владельца / оценка отгрузок. Срок поставки "+q.leadDays+" дн. требует проверки. Без отправки поставщику.",true);return Map.of("status","DRAFT","id",r.id);
 }
 public PurchaseRequest approve(Long id,String reason){var p=requests.lockById(id).orElseThrow(()->new IllegalArgumentException("Закупка не найдена"));if(!"DRAFT".equals(p.status))throw new IllegalStateException("Подтвердить можно только черновик");var product=inventory.lock(p.productId);var quote=validQuote(p.quoteId);if(quote.unitCost.compareTo(p.unitCost)!=0||p.quantity<quote.minimumQuantity)throw new IllegalStateException("Условия поставщика изменились. Отмените черновик и создайте новый по актуальному предложению");if(product.getArchived())throw new IllegalStateException("Товар архивирован");p.status="APPROVED";p.approvedAt=LocalDateTime.now();audit.record("PURCHASE_APPROVED","purchase:"+id,reason+" · внутреннее разрешение, внешнего заказа и оплаты нет");return p;}
 public PurchaseRequest cancel(Long id,String reason){var p=requests.lockById(id).orElseThrow(()->new IllegalArgumentException("Закупка не найдена"));if(!Set.of("DRAFT","APPROVED","PART_RECEIVED").contains(p.status))throw new IllegalStateException("Заказ уже завершён");p.status="CANCELLED";p.completedAt=LocalDateTime.now();audit.record("PURCHASE_CANCELLED","purchase:"+id,reason+" · принятые партии сохранены");return p;}
 public PurchaseReceipt receive(Long id,String key,int quantity,String lotNumber,LocalDate received,LocalDate made,LocalDate expiry){
  var p=requests.lockById(id).orElseThrow(()->new IllegalArgumentException("Закупка не найдена"));String fp=fingerprint(id+"|"+quantity+"|"+lotNumber+"|"+received+"|"+made+"|"+expiry);var old=receipts.findByRequestKey(key);
  if(old.isPresent()){if(!old.get().requestId.equals(id)||!old.get().fingerprint.equals(fp))throw new IllegalStateException("Ключ повторного запроса относится к другой приёмке");return old.get();}
  if(!Set.of("APPROVED","PART_RECEIVED").contains(p.status)||quantity>p.quantity-p.receivedQuantity)throw new IllegalStateException("Приёмка превышает остаток подтверждённой закупки");
  var lot=inventory.save(null,new LotInput(p.productId,p.supplierId,lotNumber,received,made,expiry,quantity,p.unitCost));
  var r=new PurchaseReceipt();r.requestId=id;r.requestKey=key;r.fingerprint=fp;r.lotId=lot.getId();r.quantity=quantity;receipts.saveAndFlush(r);p.receivedQuantity+=quantity;p.status=p.receivedQuantity==p.quantity?"RECEIVED":"PART_RECEIVED";if("RECEIVED".equals(p.status))p.completedAt=LocalDateTime.now();audit.record("PURCHASE_RECEIVED","purchase:"+id,quantity+" ед. · партия "+lot.getId());return r;
 }
 public static String fingerprint(String value){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 @Transactional(readOnly=true) public Map<String,Object> list(){return Map.of("quotes",quotes.findAll(),"requests",requests.findTop100ByOrderByCreatedAtDescIdDesc());}
}
