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
 private final ProductRepository products;private final OrderRepository orders;private final InventoryService inventory;private final InventoryLotRepository lots;
 public DemandAnalyticsService(ProductRepository p,OrderRepository o,InventoryService i,InventoryLotRepository l){products=p;orders=o;inventory=i;lots=l;}
 @Transactional(readOnly=true) public Map<String,Object> product(Long id){return forProduct(products.findById(id).orElseThrow(()->new IllegalArgumentException("Товар не найден")));}
 public Map<String,Object> forProduct(Product p){
  LocalDate today=LocalDate.now(),start=today.minusDays(89);if(p.getReceivedAt().isAfter(start))start=p.getReceivedAt();
  int days=(int)Math.max(1,ChronoUnit.DAYS.between(start,today)+1);double[] series=new double[days];BigDecimal revenue=BigDecimal.ZERO;
  var rows=orders.pricingSales(p.getId(),start.atStartOfDay(),LocalDateTime.now());for(var row:rows){int day=(int)ChronoUnit.DAYS.between(start,((LocalDateTime)row[0]).toLocalDate());if(day>=0&&day<days){int qty=((Number)row[1]).intValue();series[day]+=qty;revenue=revenue.add(((BigDecimal)row[2]).multiply(BigDecimal.valueOf(qty)));}}
  var forecast=DemandForecast.fit(series);int stock=inventory.available(p),target=Math.max(p.getLowStockThreshold(),forecast.daily().isEmpty()?0:(int)Math.min(1000000,Math.ceil(forecast.units14())));
  double recent=Arrays.stream(series,Math.max(0,days-7),days).sum(),previous=days>=14?Arrays.stream(series,days-14,days-7).sum():0;
  Double change=days>=14&&previous>=10?Math.round((recent-previous)/previous*10000d)/100d:null;
  long expired=0;BigDecimal lost=BigDecimal.ZERO;for(var l:lots.findByProductId(p.getId()))if(l.getActive()&&l.getQuantity()>0&&l.getExpiryDate()!=null&&l.getExpiryDate().isBefore(today)){expired+=l.getQuantity();lost=lost.add(l.getUnitCost().multiply(BigDecimal.valueOf(l.getQuantity())));}
  List<Map<String,Object>> history=new ArrayList<>();for(int i=0;i<days;i++)history.add(Map.of("date",start.plusDays(i),"units",series[i]));
  var out=new LinkedHashMap<String,Object>();out.put("productId",p.getId());out.put("name",p.getName());out.put("sku",p.getSku());out.put("stock",stock);out.put("forecast",forecast);out.put("history",history);out.put("reorderUnits",Math.max(0,target-stock));out.put("reorderBasis",forecast.daily().isEmpty()?"Только заданный владельцем минимальный остаток":"Максимум минимального остатка и оценочных отгрузок за 14 дней; срок поставки здесь не известен");out.put("units7",recent);out.put("previousUnits7",previous);out.put("changePercent",change);out.put("anomaly",change!=null&&change<=-50?"SALES_DROP":change!=null&&change>=100?"SALES_SURGE":"NONE");out.put("recordedRevenue",revenue);out.put("expiredUnits",expired);out.put("expiredCost",lost);out.put("note","Выручка подтверждённых отгрузок. Себестоимость просроченного остатка — риск, не оформленное списание. Налоги, доставка, история потерь и непроданный спрос не восстановлены.");return out;
 }
}
