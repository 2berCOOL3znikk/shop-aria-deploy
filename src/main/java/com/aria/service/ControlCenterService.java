package com.aria.service;
import com.aria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @Transactional(readOnly=true)
public class ControlCenterService {
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
 private final OrderRepository orders;private final InventoryLotRepository lots;private final InventoryLedgerService ledger;
 private final PricingDecisionRepository decisions;private final SmartPricingService smart;private final ManagerActionRepository actions;
 private final ProductRepository products;
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
 public ControlCenterService(OrderRepository o,InventoryLotRepository l,InventoryLedgerService i,PricingDecisionRepository d,SmartPricingService s,ManagerActionRepository a,ProductRepository p){orders=o;lots=l;ledger=i;decisions=d;smart=s;actions=a;products=p;}
 public Map<String,Object> center(){
  var revenue=orders.recordedRevenue(LocalDate.now().atStartOfDay()).getFirst();var held=lots.heldValue().getFirst();
  var recent=decisions.findRecentActivity(org.springframework.data.domain.PageRequest.of(0,8));
  var cards=new ArrayList<Map<String,Object>>();var outcomes=new ArrayList<Map<String,Object>>();var seen=new HashSet<Long>();
  for(var d:recent){var report=json.readValue(d.reportJson,Map.class);var facts=(Map<?,?>)report.get("facts");var card=new LinkedHashMap<String,Object>();card.put("id",d.id);card.put("productId",d.productId);card.put("name",facts.get("name"));card.put("status",d.status);card.put("source",d.source);card.put("strategy",d.strategy);card.put("selectedPercent",d.selectedPercent==null?report.get("suggestedPercent"):d.selectedPercent);card.put("reason",report.get("reason"));card.put("confidence",report.get("confidence"));card.put("reviewAt",d.reviewAt);card.put("planning",report.get("planning"));card.put("explanation",d.explanationJson==null?null:json.readValue(d.explanationJson,Map.class));cards.add(card);
   if(seen.add(d.productId)){var history=smart.history(d.productId);history.stream().limit(1).forEach(outcomes::add);}
  }
  var loss=ledger.report(null,0,1);var insights=new LinkedHashMap<String,Object>();insights.put("shippedRevenueToday",revenue[0]);insights.put("shippedOrdersToday",revenue[1]);insights.put("heldPurchaseValue",held[0]);insights.put("heldUnits",held[1]);insights.put("writtenOffUnits30",loss.get("writtenOffUnits30"));insights.put("writtenOffCost30",loss.get("recordedWriteOffCost30"));insights.put("unknownWriteOffCostUnits30",loss.get("unknownCostUnits30"));insights.put("note","Выручка подтверждённых отгрузок, не чистая прибыль. Стоимость запаса — зарегистрированная закупка, включая ещё не списанный риск. Налоги, полный учёт расходов и причинный эффект скидок здесь не вычисляются.");
  var external=em.createQuery("select d from StorePricingDecision d where not exists(select n.id from StorePricingDecision n where n.storeId=d.storeId and n.externalId=d.externalId and n.id>d.id) order by d.id desc",com.aria.entity.StorePricingDecision.class).setMaxResults(20).getResultList().stream().map(d->{var s=em.find(com.aria.entity.ConnectedStore.class,d.storeId);var v=new LinkedHashMap<String,Object>();v.put("id",d.id);v.put("storeId",d.storeId);v.put("storeName",s.name);v.put("testConnection",s.testConnection);v.put("archived",s.archived);v.put("name",d.productName);v.put("source",d.source);v.put("status",d.status);v.put("strategy",d.strategy);v.put("selectedPercent",d.selectedPercent);v.put("reason",d.reason);v.put("currency",d.currency);v.put("currentPrice",d.currentPrice);v.put("suggestedPrice",d.suggestedPrice);v.put("aiCompletedAt",d.aiCompletedAt);v.put("priceChangeId",d.priceChangeId);v.put("explanation",d.explanationJson==null?null:json.readValue(d.explanationJson,Map.class));return v;}).toList();
  var changes=em.createQuery("select c from StorePriceChange c order by c.id desc",com.aria.entity.StorePriceChange.class).setMaxResults(10).getResultList();
  return Map.of("asOf",LocalDateTime.now(),"insights",insights,"decisions",cards,"outcomes",outcomes.stream().limit(5).toList(),"openSignals",actions.countByStatus("OPEN")+ConnectedPricingService.attentionCount(em),"unresolvedSignals",actions.countByStatus("OPEN")+actions.countByStatus("ACKNOWLEDGED")+ConnectedPricingService.attentionCount(em),"connectedDecisions",external,"connectedChanges",changes);
 }
}
