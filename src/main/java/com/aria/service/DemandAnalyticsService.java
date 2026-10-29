package com.aria.service;
import com.aria.repository.*;
import com.aria.entity.Product;
import com.aria.service.analytics.DemandForecast;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.math.*;
@Service
public class DemandAnalyticsService {
 @org.springframework.beans.factory.annotation.Autowired private SupplierQuoteRepository quotes;
 @org.springframework.beans.factory.annotation.Autowired private SupplierRepository suppliers;
 private final ProductRepository products;private final OrderRepository orders;private final InventoryService inventory;private final InventoryLotRepository lots;
 public DemandAnalyticsService(ProductRepository p,OrderRepository o,InventoryService i,InventoryLotRepository l){products=p;orders=o;inventory=i;lots=l;}
 @Transactional(readOnly=true) public Map<String,Object> product(Long id){return forProduct(products.findById(id).orElseThrow(()->new IllegalArgumentException("Товар не найден")));}
 public Map<String,Object> forProduct(Product p){
  LocalDate today=LocalDate.now(),start=today.minusDays(89);var first=lots.findByProductId(p.getId()).stream().map(l->l.getReceivedAt()).min(LocalDate::compareTo).orElse(p.getReceivedAt());if(p.getReceivedAt().isBefore(first))first=p.getReceivedAt();if(first.isAfter(start))start=first;
  int days=(int)Math.max(1,ChronoUnit.DAYS.between(start,today)+1);double[] series=new double[days];BigDecimal revenue=BigDecimal.ZERO;
  var rows=orders.pricingSales(p.getId(),start.atStartOfDay(),LocalDateTime.now());for(var row:rows){int day=(int)ChronoUnit.DAYS.between(start,((LocalDateTime)row[0]).toLocalDate());if(day>=0&&day<days){int qty=((Number)row[1]).intValue();series[day]+=qty;revenue=revenue.add(((BigDecimal)row[2]).multiply(BigDecimal.valueOf(qty)));}}
  int stock=inventory.available(p);double rate=Arrays.stream(series,Math.max(0,days-7),days).average().orElse(0);Double cover=rate>0?Math.round(stock/rate*100d)/100d:null;
  var verified=quotes.findByProductId(p.getId()).stream().filter(q->q.verified&&!q.validUntil.isBefore(today)&&suppliers.findById(q.supplierId).map(x->x.getActive()).orElse(false)).toList();
  Integer lead=verified.stream().mapToInt(q->q.leadDays).min().stream().boxed().findFirst().orElse(null);
  int supported=Math.max(1,(int)Math.floor(days/(1+1.96/Math.sqrt(Math.max(1,Arrays.stream(series).sum())))));
  int requested=lead==null?(cover==null?supported:Math.max(1,(int)Math.min(365,Math.ceil(cover)))):Math.max(1,lead);
  int horizon=Math.min(supported,requested);var forecast=DemandForecast.fit(series,horizon);
  int target=Math.max(p.getLowStockThreshold(),lead==null||forecast.daily().isEmpty()?0:(int)Math.min(1000000,Math.ceil(forecast.expectedUnits())));

  double recent=Arrays.stream(series,Math.max(0,days-7),days).sum(),previous=days>=14?Arrays.stream(series,days-14,days-7).sum():0;
  Double change=days>=14&&previous>=10?Math.round((recent-previous)/previous*10000d)/100d:null;
  long rejected=0,unconfirmed=0;for(var lot:lots.findByProductId(p.getId()))if(lot.getActive()&&lot.getQuantity()>0){if("REJECTED".equals(lot.handlingStatus))rejected+=lot.getQuantity();else if(lot.storageRequirements!=null&&!lot.storageRequirements.isBlank()&&!"CONFIRMED".equals(lot.handlingStatus))unconfirmed+=lot.getQuantity();}
  long expired=0;BigDecimal lost=BigDecimal.ZERO;for(var l:lots.findByProductId(p.getId()))if(l.getActive()&&l.getQuantity()>0&&l.getExpiryDate()!=null&&l.getExpiryDate().isBefore(today)){expired+=l.getQuantity();lost=lost.add(l.getUnitCost().multiply(BigDecimal.valueOf(l.getQuantity())));}
  List<Map<String,Object>> history=new ArrayList<>();for(int i=0;i<days;i++)history.add(Map.of("date",start.plusDays(i),"units",series[i]));
  var out=new LinkedHashMap<String,Object>();out.put("productId",p.getId());out.put("name",p.getName());out.put("sku",p.getSku());out.put("stock",stock);out.put("forecast",forecast);out.put("history",history);out.put("daysOfCoverEstimate",cover);out.put("verifiedLeadDays",lead);out.put("supplierOptions",verified.stream().map(q->Map.of("quoteId",q.id,"supplierId",q.supplierId,"unitCost",q.unitCost,"minimumQuantity",q.minimumQuantity,"leadDays",q.leadDays,"validUntil",q.validUntil)).toList());out.put("horizonLimitation",requested>supported?"История не поддерживает весь требуемый горизонт; риск дефицита за его пределами не оценён":"Горизонт выбран из покрытия запасом / подтверждённого срока поставки и ограничен историей");out.put("reorderUnits",Math.max(0,target-stock));out.put("reorderBasis",lead==null||forecast.daily().isEmpty()?"Минимум владельца; проверенный срок поставки или история отсутствуют":"Минимум владельца и оценка отгрузок за "+horizon+" дн. при подтверждённом сроке поставки "+lead+" дн.; если срок длиннее горизонта, оценка неполная");out.put("units7",recent);out.put("previousUnits7",previous);out.put("changePercent",change);out.put("anomaly",change!=null&&change<=-50?"SALES_DROP":change!=null&&change>=100?"SALES_SURGE":"NONE");out.put("recordedRevenue",revenue);out.put("handlingRejectedUnits",rejected);out.put("handlingUnconfirmedUnits",unconfirmed);out.put("expiredUnits",expired);out.put("expiredCost",lost);out.put("note","Выручка подтверждённых отгрузок. Себестоимость просроченного остатка — риск, не оформленное списание. Налоги, доставка, история потерь и непроданный спрос не восстановлены.");return out;
 }
}
