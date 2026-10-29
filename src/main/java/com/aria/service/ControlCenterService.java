package com.aria.service;
import com.aria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @Transactional(readOnly=true)
public class ControlCenterService {
 private final OrderRepository orders;private final InventoryLotRepository lots;private final InventoryLedgerService ledger;
 private final PricingDecisionRepository decisions;private final SmartPricingService smart;private final ManagerActionRepository actions;
 private final ProductRepository products;
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
 public ControlCenterService(OrderRepository o,InventoryLotRepository l,InventoryLedgerService i,PricingDecisionRepository d,SmartPricingService s,ManagerActionRepository a,ProductRepository p){orders=o;lots=l;ledger=i;decisions=d;smart=s;actions=a;products=p;}
 public Map<String,Object> center(){
  var revenue=orders.recordedRevenue(LocalDate.now().atStartOfDay()).getFirst();var held=lots.heldValue().getFirst();
  var recent=decisions.findAllByOrderByCreatedAtDescIdDesc(org.springframework.data.domain.PageRequest.of(0,8)).getContent();
  var cards=new ArrayList<Map<String,Object>>();var outcomes=new ArrayList<Map<String,Object>>();var seen=new HashSet<Long>();
  for(var d:recent){var report=json.readValue(d.reportJson,Map.class);var facts=(Map<?,?>)report.get("facts");var card=new LinkedHashMap<String,Object>();card.put("id",d.id);card.put("productId",d.productId);card.put("name",facts.get("name"));card.put("status",d.status);card.put("source",d.source);card.put("strategy",d.strategy);card.put("selectedPercent",d.selectedPercent==null?report.get("suggestedPercent"):d.selectedPercent);card.put("reason",report.get("reason"));card.put("confidence",report.get("confidence"));card.put("reviewAt",d.reviewAt);card.put("planning",report.get("planning"));card.put("explanation",d.explanationJson==null?null:json.readValue(d.explanationJson,Map.class));cards.add(card);
   if(seen.add(d.productId)){var history=smart.history(d.productId);history.stream().limit(1).forEach(outcomes::add);}
  }
  var loss=ledger.report(null,0,1);var insights=new LinkedHashMap<String,Object>();insights.put("shippedRevenueToday",revenue[0]);insights.put("shippedOrdersToday",revenue[1]);insights.put("heldPurchaseValue",held[0]);insights.put("heldUnits",held[1]);insights.put("writtenOffUnits30",loss.get("writtenOffUnits30"));insights.put("writtenOffCost30",loss.get("recordedWriteOffCost30"));insights.put("unknownWriteOffCostUnits30",loss.get("unknownCostUnits30"));insights.put("note","Выручка подтверждённых отгрузок, не чистая прибыль. Стоимость запаса — зарегистрированная закупка, включая ещё не списанный риск. Налоги, полный учёт расходов и причинный эффект скидок здесь не вычисляются.");
  return Map.of("asOf",LocalDateTime.now(),"insights",insights,"decisions",cards,"outcomes",outcomes.stream().limit(5).toList(),"openSignals",actions.countByStatus("OPEN"),"unresolvedSignals",actions.countByStatus("OPEN")+actions.countByStatus("ACKNOWLEDGED"));
 }
}
