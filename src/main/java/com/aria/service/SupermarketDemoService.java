package com.aria.service;

import com.aria.config.OrganizationBoundary;
import com.aria.dto.ControlRequests.*;
import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.service.integrations.PosAdapter.*;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

/** Explicit, atomic, repeat-safe teaching import. Never runs in an ordinary company. */
@Service
public class SupermarketDemoService {
 private static final String MARKER="DEMO_SUPERMARKET_V1_READY";
 private final SupermarketDemoCatalogue catalogue;private final DemoCatalogueImportService base;
 private final ControlService control;private final InventoryService inventory;private final PurchaseService purchases;private final PosService pos;private final SmartPricingService pricing;
 private final ProductRepository products;private final ProductDetailRepository details;private final EntityManager em;private final AuditService audit;private final OrganizationBoundary boundary;
 @Value("${aria.demo-supermarket.enabled:false}") private boolean enabled;
 public SupermarketDemoService(SupermarketDemoCatalogue catalogue,DemoCatalogueImportService base,ControlService control,InventoryService inventory,PurchaseService purchases,PosService pos,SmartPricingService pricing,ProductRepository products,ProductDetailRepository details,EntityManager em,AuditService audit,OrganizationBoundary boundary){this.catalogue=catalogue;this.base=base;this.control=control;this.inventory=inventory;this.purchases=purchases;this.pos=pos;this.pricing=pricing;this.products=products;this.details=details;this.em=em;this.audit=audit;this.boundary=boundary;}
 private boolean eligible(Organization org){return enabled&&"demo".equals(boundary.key())&&org!=null&&org.demonstration;}
 private boolean prepared(){return em.createQuery("select count(e) from AuditEvent e where e.action=:marker",Long.class).setParameter("marker",MARKER).getSingleResult()>0;}
 @Transactional(readOnly=true) public Map<String,Object> status(){return Map.of("available",eligible(em.find(Organization.class,1)),"prepared",prepared(),"teachingProducts",catalogue.items().size()+16,"demonstration",true,"note","Отдельный учебный супермаркет: цены, партии, продажи, возвраты и поставщики синтетические. Повтор не восстанавливает проданный запас и не меняет разрешения ARIA.");}
 @Transactional public Map<String,Object> populate(boolean confirmed){
  if(!confirmed)throw new IllegalArgumentException("Подтвердите создание учебного магазина");
  var org=em.find(Organization.class,1,LockModeType.PESSIMISTIC_WRITE);
  if(!eligible(org))throw new ResponseStatusException(HttpStatus.CONFLICT,"Учебные данные разрешены только в отдельной демонстрационной компании");
  if(prepared())return Map.of("alreadyPrepared",true,"demonstration",true,"note","Учебный магазин уже подготовлен. Продажи, остатки и ваши изменения сохранены без повторного импорта.");
  // Detect collisions before creating anything. The transaction also protects partial failures.
  for(var item:catalogue.items())if(products.findBySkuIgnoreCase(item.code()).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"Учебный артикул уже занят. Существующие записи не перезаписаны.");
  base.populate(true);var today=LocalDate.now();var byItem=new LinkedHashMap<SupermarketDemoCatalogue.Item,Product>();var vendors=new LinkedHashMap<String,Supplier>();var quotes=new HashMap<Long,Long>();var currentLots=new HashMap<Long,List<InventoryLot>>();
  for(var item:catalogue.items()){
   var supplier=vendors.computeIfAbsent(item.category(),category->control.saveSupplier(null,new SupplierInput("Учебный поставщик · "+category,null,"Синтетический поставщик супермаркета ARIA. Контакт и реальные обязательства отсутствуют.",true)));
   var product=control.saveProduct(null,new ProductInput(item.code(),item.name(),item.category(),item.price(),null,"Учебная карточка без конкретного бренда. Цена, упаковка, остатки и история созданы для демонстрации ARIA; сведения производителя не подтверждены.",item.image(),"DEMO-"+item.code(),item.unit(),5,item.minimumPrice()));
   var detail=details.findByProductId(product.getId()).orElseThrow();detail.setDemonstration(true);detail.setCatalogueReference("supermarket-v1:"+item.code());detail.setSize(item.size());details.saveAndFlush(detail);byItem.put(item,product);
   var quote=purchases.quote(product.getId(),supplier.getId(),item.cost(),1,2,today.minusDays(7),today.plusDays(60),true);quotes.put(product.getId(),quote.id);
   // Historical stock is consumed by the actual POS service below. It is not added to today's balance twice.
   int historical=item.weeklySales()*4;if(historical>0){var history=inventory.save(null,new LotInput(product.getId(),supplier.getId(),"DEMO-HISTORY-V1",today.minusDays(35),null,item.expiryDays()==null?null:today.plusDays(1),historical,item.cost()));inventory.handling(history.getId(),history.handlingRevision,item.storage(),"CONFIRMED",true,"Учебная проверка исторической партии; не сведения реального магазина");}
   var purchase=purchases.create(quote.id,item.quantity(),"Учебная поставка. Реального заказа поставщику и оплаты нет");purchases.approve(purchase.id,"Учебное разрешение приёмки");purchase.createdAt=today.minusDays(5).atTime(9,0);purchase.approvedAt=purchase.createdAt.plusHours(1);
   int first=item.profile().equals("EXPIRING")||item.profile().equals("SOCIAL")?Math.max(1,item.quantity()*2/3):item.quantity();var batches=new ArrayList<InventoryLot>();
   if(item.profile().equals("EXPIRED"))batches.add(historicalExpired(product,supplier,purchase,item,today));
   else{var receipt=purchases.receive(purchase.id,"demo-supermarket-v1-"+item.code()+"-a",first,"DEMO-STOCK-V1-A",today.minusDays(4),null,item.expiryDays()==null?null:today.plusDays(Math.max(1,item.expiryDays())));batches.add(em.find(InventoryLot.class,receipt.lotId));}
   if(first<item.quantity()){var later=purchases.receive(purchase.id,"demo-supermarket-v1-"+item.code()+"-b",item.quantity()-first,"DEMO-STOCK-V1-B",today.minusDays(2),null,today.plusDays(Math.max(14,item.expiryDays()+14)));batches.add(em.find(InventoryLot.class,later.lotId));}
   for(var batch:batches)inventory.handling(batch.getId(),batch.handlingRevision,item.storage(),item.profile().equals("DAMAGED")?"REJECTED":item.profile().equals("SAFETY_REVIEW")?"UNKNOWN":"CONFIRMED",true,"Учебный сценарий "+item.profile()+". Фактических проверок коммерческого товара не заявляется.");
   currentLots.put(product.getId(),batches);
  }
  int saleReceipts=0;
  for(int day=0;day<28;day++){
   var lines=new ArrayList<Line>();for(var entry:byItem.entrySet()){int rate=entry.getKey().weeklySales(),quantity=(rate*(day+1))/7-(rate*day)/7;if(quantity>0)lines.add(new Line(entry.getValue().getId(),quantity,entry.getKey().price()));}
   if(lines.isEmpty())continue;var view=(Map<?,?>)pos.record(new Sale("demo-supermarket-v1-day-"+day,lines),"ARIA educational history");var receipt=(PosReceipt)view.get("receipt");var time=today.minusDays(28-day).atTime(12+day%5,10);receipt.createdAt=time;var order=em.find(Order.class,receipt.orderId);order.setCreatedAt(time);order.setShippedAt(time);order.setDeliveryAddress("УЧЕБНАЯ ПРОДАЖА · синтетическая история супермаркета, без оплаты и фискализации");saleReceipts++;
  }
  // A real domain return records quarantine and never makes returned units sellable.
  var returnProduct=byItem.entrySet().stream().filter(e->e.getKey().profile().equals("NON_PERISHABLE")).findFirst().orElseThrow();var returned=(Map<?,?>)pos.record(new Sale("demo-supermarket-v1-return-sale",List.of(new Line(returnProduct.getValue().getId(),1,returnProduct.getKey().price()))),"ARIA educational return");var returnReceipt=(PosReceipt)returned.get("receipt");pos.returnToQuarantine(returnReceipt.id,"Учебный возврат: товар в карантине, реальной выплаты нет");
  var returnedOrder=em.find(Order.class,returnReceipt.orderId);returnedOrder.setCreatedAt(today.minusDays(2).atTime(14,0));returnedOrder.setShippedAt(today.minusDays(2).atTime(14,0));returnReceipt.createdAt=returnedOrder.getCreatedAt();returnReceipt.returnedAt=today.minusDays(1).atTime(10,0);
  int draft=0;
  for(var entry:byItem.entrySet()){
   var item=entry.getKey();var product=entry.getValue();
   if(item.profile().equals("EXPIRED"))inventory.synchronize(product);
   if(item.profile().equals("LOW_STOCK")){var purchase=purchases.create(quotes.get(product.getId()),24,"Учебный черновик пополнения, без отправки поставщику");if(draft++%2==0)purchases.approve(purchase.id,"Учебное разрешение будущей приёмки");}
  }
  em.flush();int recommendations=0;for(var entry:byItem.entrySet())if(Set.of("EXPIRING","SOCIAL","PRICE_LOCK","SAFETY_REVIEW").contains(entry.getKey().profile())&&inventory.available(entry.getValue())>0){pricing.analyzeForManager(entry.getValue().getId());recommendations++;}
  audit.record(MARKER,"demo-supermarket:v1","64 учебных карточки + справочный каталог; "+saleReceipts+" учебных продаж через POS, 1 возврат в карантин. Разрешения AI и реальные компании не изменялись.");
  return Map.of("created",64,"referenceCatalogue",16,"suppliers",vendors.size(),"salesReceipts",saleReceipts,"returns",1,"recommendations",recommendations,"demonstration",true,"note","Учебный супермаркет подготовлен. Источник рекомендаций — автоматический Java-расчёт; ответы нейросети не имитировались.");
 }
 /** Historical teaching import only; ordinary receiving still rejects expired stock. */
 private InventoryLot historicalExpired(Product product,Supplier supplier,PurchaseRequest purchase,SupermarketDemoCatalogue.Item item,LocalDate today){
  var lot=new InventoryLot();lot.setProduct(product);lot.setSupplier(supplier);lot.setLotNumber("DEMO-HISTORICAL-EXPIRED-V1");lot.setReceivedAt(today.minusDays(4));lot.setExpiryDate(today.minusDays(1));lot.setQuantity(item.quantity());lot.setInitialQuantity(item.quantity());lot.setUnitCost(item.cost());em.persist(lot);em.flush();
  inventory.movement(lot,item.quantity(),0,"DEMO_HISTORICAL_RECEIPT","Учебная историческая партия: приёмка до истечения срока, не текущая приёмка просроченного товара");inventory.synchronize(product);
  var receipt=new PurchaseReceipt();receipt.requestId=purchase.id;receipt.requestKey="demo-supermarket-v1-"+item.code()+"-a";receipt.quantity=item.quantity();receipt.lotId=lot.getId();receipt.fingerprint=PurchaseService.fingerprint(purchase.id+"|"+item.quantity()+"|"+lot.getLotNumber()+"|"+lot.getReceivedAt()+"|null|"+lot.getExpiryDate());em.persist(receipt);purchase.receivedQuantity=item.quantity();purchase.status="RECEIVED";purchase.completedAt=today.minusDays(4).atTime(11,0);
  audit.record("DEMO_HISTORICAL_EXPIRY","product:"+product.getId(),"Учебная партия с истёкшим сроком. Исключена из продажи и социальных предложений.");return lot;
 }
}
