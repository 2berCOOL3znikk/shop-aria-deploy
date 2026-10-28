package com.aria.service;
import com.aria.dto.AdminViews.*;
import com.aria.entity.*;
import com.aria.entity.Order;
import com.aria.repository.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true)
public class AdminReadService {
    private static final Map<String,String> ACTIONS=Map.ofEntries(
        Map.entry("SMART_PRICING_RECOMMENDED","Рекомендация Smart Pricing"),Map.entry("SMART_PRICING_APPLIED","Решение Smart Pricing применено"),Map.entry("SMART_PRICING_REJECTED","Рекомендация отклонена"),Map.entry("SMART_PRICING_ROLLED_BACK","Откат цены"),
        Map.entry("SMART_AUTOPILOT_APPLIED","Автопилот изменил цену"),Map.entry("SMART_AUTOPILOT_SKIPPED","Автопилот пропустил изменение"),Map.entry("SMART_AUTOPILOT_CONFIGURED","Настройки автопилота"),Map.entry("SMART_AUTOPILOT_STOPPED","Автопилот экстренно выключен"),
        Map.entry("PRODUCT_CREATED","Создан товар"),Map.entry("PRODUCT_UPDATED","Изменён товар"),Map.entry("PRODUCT_ARCHIVED","Товар архивирован"),Map.entry("PRODUCT_PHOTO_UPDATED","Обновлено фото"),
        Map.entry("LOT_RECEIVED","Принята партия"),Map.entry("LOT_UPDATED","Исправлена партия"),Map.entry("LOT_WRITTEN_OFF","Списана партия"),Map.entry("PRODUCT_WRITTEN_OFF","Товар списан"),
        Map.entry("DISCOUNT_CHANGED","Изменена скидка"),Map.entry("ORDER_CREATED","Создан заказ"),Map.entry("ORDER_STOCK","Отгрузка заказа"),Map.entry("ORDER_STOCK_RETURNED","Возврат товара из заказа"),Map.entry("ORDER_STATUS_CHANGED","Изменён статус заказа"),
        Map.entry("SOCIAL_PUBLISHED","Публикация на соцполке"),Map.entry("SOCIAL_RESERVED","Бронь фонда"),Map.entry("SOCIAL_TRANSFERRED","Передача подтверждена"),Map.entry("SOCIAL_CANCELLED","Отмена передачи"),
        Map.entry("SUPPLIER_CREATED","Добавлен поставщик"),Map.entry("SUPPLIER_UPDATED","Изменён поставщик"),Map.entry("SUPPLIER_DELETED","Удалён поставщик"),Map.entry("PRICING_POLICY_CHANGED","Изменены правила скидок"),Map.entry("PRICING_POLICY_RESET","Сброшено правило категории"));
    private final ProductRepository products;private final InventoryLotRepository lots;private final OrderRepository orders;
    private final AuditEventRepository audit;private final InventoryService inventory;private final PolicyService policies;
    public AdminReadService(ProductRepository products,InventoryLotRepository lots,OrderRepository orders,AuditEventRepository audit,InventoryService inventory,PolicyService policies){
        this.products=products;this.lots=lots;this.orders=orders;this.audit=audit;this.inventory=inventory;this.policies=policies;
    }
    public List<ProductRow> productRows(){
        return productRows(lots.findAll());
    }
    private List<ProductRow> productRows(List<InventoryLot> stock){
        var grouped=stock.stream().collect(Collectors.groupingBy(l->l.getProduct().getId()));
        return products.findAll().stream().map(p->{
            var rows=grouped.getOrDefault(p.getId(),List.of());var usable=rows.stream().filter(inventory::usable).toList();
            var expiry=rows.isEmpty()?p.getExpiryDate():usable.stream().map(InventoryLot::getExpiryDate).filter(Objects::nonNull).min(LocalDate::compareTo).orElse(null);
            var cost=usable.stream().map(InventoryLot::getUnitCost).max(BigDecimal::compareTo).orElse(p.getUnitCost());
            return new ProductRow(p.getId(),p.getSku(),p.getName(),p.getCategory(),p.getUnit(),p.getBarcode(),p.getUnitPrice(),cost,inventory.available(p,rows),p.getLowStockThreshold(),p.getDiscountPercent(),p.getStatus(),p.getArchived(),p.getImageUrl(),expiry);
        }).sorted(Comparator.comparing(ProductRow::id).reversed()).toList();
    }
    public Map<String,Object> overview(){
        var stock=lots.findAll();var rows=productRows(stock).stream().filter(p->!p.archived()).toList();var today=LocalDate.now();
        var rules=policies.list().stream().collect(Collectors.toMap(PricingPolicy::getCategory,p->p));var common=rules.getOrDefault("*",new PricingPolicy());
        var expired=stock.stream().filter(l->l.getActive()&&l.getQuantity()>0&&l.getExpiryDate()!=null&&l.getExpiryDate().isBefore(today)).toList();
        long urgent=stock.stream().filter(inventory::usable).filter(l->l.getExpiryDate()!=null&&!l.getExpiryDate().isAfter(today.plusDays(rules.getOrDefault(PolicyService.categoryKey(l.getProduct().getCategory()),common).getCriticalDays()))).count();
        long low=rows.stream().filter(p->p.quantityOnHand()<=(p.lowStockThreshold()==null?5:p.lowStockThreshold())).count();
        Map<String,Object> result=new LinkedHashMap<>();result.put("products",rows.size());result.put("expiredLots",expired.size());result.put("urgentLots",urgent);result.put("lowStock",low);
        result.put("pendingOrders",orders.count((root,query,cb)->cb.equal(root.get("status"),"PENDING")));
        result.put("availableUnits",rows.stream().mapToLong(ProductRow::quantityOnHand).sum());result.put("expiredUnits",expired.stream().mapToLong(InventoryLot::getQuantity).sum());
        result.put("stockCost",stock.stream().filter(inventory::usable).map(l->l.getUnitCost().multiply(BigDecimal.valueOf(l.getQuantity()))).reduce(BigDecimal.ZERO,BigDecimal::add));
        result.put("briefing",List.of("Просроченных партий: "+expired.size()+". Они исключены из доступного остатка.","Партий с критическим сроком: "+urgent+". Проверьте возможность скидки или передачи.","Товаров на минимуме или ниже: "+low+". Проверьте потребность в пополнении."));return result;
    }
    public PageResult<OrderRow> orderPage(String q,String state,int page,int size,String sort){
        validate(q,page,size);if(!state.isBlank()&&!Set.of("PENDING","SHIPPED","DELIVERED","CANCELLED").contains(state))throw new IllegalArgumentException("Неизвестный статус заказа");
        Sort ordering=switch(sort){case "newest"->Sort.by(Sort.Direction.DESC,"createdAt","id");case "oldest"->Sort.by(Sort.Direction.ASC,"createdAt","id");case "amount"->Sort.by(Sort.Direction.DESC,"totalAmount","id");default->throw new IllegalArgumentException("Неизвестная сортировка");};String pattern=pattern(q);
        Specification<Order> spec=(root,query,cb)->{
            List<Predicate> terms=new ArrayList<>();if(!state.isBlank())terms.add(cb.equal(root.get("status"),state));
            if(!q.isBlank()){
                Subquery<Long> items=query.subquery(Long.class);Root<Order> order=items.from(Order.class);var item=order.join("items");
                Expression<String> name=cb.coalesce(item.<String>get("productName"),item.get("product").get("name"));Expression<String> sku=cb.coalesce(item.<String>get("productSku"),item.get("product").get("sku"));
                items.select(order.get("id")).where(cb.or(cb.like(cb.lower(name),pattern,'\\'),cb.like(cb.lower(sku),pattern,'\\')));
                List<Predicate> matches=new ArrayList<>(List.of(cb.like(cb.lower(root.get("ownerEmail")),pattern,'\\'),cb.like(cb.lower(root.get("deliveryAddress")),pattern,'\\'),root.get("id").in(items)));
                try{matches.add(cb.equal(root.get("id"),Long.parseLong(q.trim())));}catch(NumberFormatException ignored){}terms.add(cb.or(matches.toArray(Predicate[]::new)));
            }return cb.and(terms.toArray(Predicate[]::new));
        };return PageResult.from(orders.findAll(spec,PageRequest.of(page,size,ordering)).map(OrderRow::from));
    }
    public PageResult<AuditEvent> auditPage(String q,int page,int size){
        validate(q,page,size);String pattern=pattern(q);
        var aliases=ACTIONS.entrySet().stream().filter(e->e.getValue().toLowerCase(Locale.ROOT).contains(q.trim().toLowerCase(Locale.ROOT))).map(Map.Entry::getKey).toList();
        Specification<AuditEvent> spec=(root,query,cb)->q.isBlank()?cb.conjunction():cb.or(cb.like(cb.lower(root.get("actor")),pattern,'\\'),cb.like(cb.lower(root.get("action")),pattern,'\\'),cb.like(cb.lower(root.get("resource")),pattern,'\\'),cb.like(cb.lower(root.get("details")),pattern,'\\'),aliases.isEmpty()?cb.disjunction():root.get("action").in(aliases));
        return PageResult.from(audit.findAll(spec,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt","id"))));
    }
    private void validate(String q,int page,int size){if(q.length()>200||page<0||page>1000000||size<1||size>100)throw new IllegalArgumentException("Поиск — до 200 символов, страница ≥ 0, размер — от 1 до 100");}
    private String pattern(String q){return "%"+q.trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";}
}
