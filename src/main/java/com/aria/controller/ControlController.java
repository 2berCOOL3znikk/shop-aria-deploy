package com.aria.controller;

import com.aria.dto.ControlRequests.*;
import com.aria.entity.*;
import com.aria.repository.*;
import com.aria.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin/control")
public class ControlController {
    private final ControlService control; private final InventoryService inventory; private final PricingService pricing;
    private final ProductRepository products; private final ProductDetailRepository details; private final SupplierRepository suppliers;
    private final AuditEventRepository events; private final OrderRepository orders;
    private final SocialService social;
    private final SocialOfferRepository offers;
    private final OrderService orderService;
    private final IntelligenceService intelligence;
    public ControlController(ControlService control,InventoryService inventory,PricingService pricing,ProductRepository products,ProductDetailRepository details,SupplierRepository suppliers,AuditEventRepository events,OrderRepository orders,SocialService social,SocialOfferRepository offers,OrderService orderService,IntelligenceService intelligence){
        this.control=control;this.inventory=inventory;this.pricing=pricing;this.products=products;this.details=details;this.suppliers=suppliers;this.events=events;this.orders=orders;
        this.social=social;
        this.offers=offers;
        this.orderService=orderService;
        this.intelligence=intelligence;
    }
    @GetMapping("/products") public List<Product> products(){return products.findAll();}
    @GetMapping("/products/{id}") public Map<String,Object> product(@PathVariable Long id){var result=new HashMap<String,Object>();result.put("product",inventory.lock(id));result.put("details",details.findByProductId(id).orElse(null));return result;}
    @PostMapping("/products") public Product create(@Valid @RequestBody ProductInput input){return control.saveProduct(null,input);}
    @PutMapping("/products/{id}") public Product update(@PathVariable Long id,@Valid @RequestBody ProductInput input){return control.saveProduct(id,input);}
    @DeleteMapping("/products/{id}") public Map<String,String> archive(@PathVariable Long id){control.archiveProduct(id);return Map.of("message","Товар архивирован, история сохранена");}
    @GetMapping("/suppliers") public List<Supplier> suppliers(){return suppliers.findAll();}
    @GetMapping("/suppliers/{id}") public Supplier supplier(@PathVariable Long id){return suppliers.findById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Поставщик не найден"));}
    @PostMapping("/suppliers") public Supplier createSupplier(@Valid @RequestBody SupplierInput input){return control.saveSupplier(null,input);}
    @PutMapping("/suppliers/{id}") public Supplier updateSupplier(@PathVariable Long id,@Valid @RequestBody SupplierInput input){return control.saveSupplier(id,input);}
    @DeleteMapping("/suppliers/{id}") public Map<String,String> deleteSupplier(@PathVariable Long id){control.deleteSupplier(id);return Map.of("message","Поставщик удалён");}
    @GetMapping("/lots") public List<InventoryLot> lots(){return inventory.list();}
    @GetMapping("/lots/{id}") public InventoryLot lot(@PathVariable Long id){return inventory.list().stream().filter(l->l.getId().equals(id)).findFirst().orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Партия не найдена"));}
    @PostMapping("/lots") public InventoryLot createLot(@Valid @RequestBody LotInput input){return inventory.save(null,input);}
    @PutMapping("/lots/{id}") public InventoryLot updateLot(@PathVariable Long id,@Valid @RequestBody LotInput input){return inventory.save(id,input);}
    @DeleteMapping("/lots/{id}") public Map<String,String> deleteLot(@PathVariable Long id,@Valid @RequestBody ReasonInput input){inventory.writeOff(id,input.reason());return Map.of("message","Партия списана с записью причины");}
    @GetMapping("/pricing/{id}") public Map<String,Object> preview(@PathVariable Long id,@RequestParam(defaultValue="0") int percent){Product p=inventory.lock(id);inventory.synchronize(p);return Map.of("current",pricing.preview(p,percent),"recommendation",pricing.recommendation(p));}
    @PutMapping("/pricing/{id}") public Map<String,String> discount(@PathVariable Long id,@Valid @RequestBody DiscountInput input){control.applyDiscount(id,input);return Map.of("message","Скидка применена");}
    @GetMapping("/audit") public List<AuditEvent> audit(){return events.findTop100ByOrderByCreatedAtDescIdDesc();}
    @GetMapping("/orders") public List<Order> orders(){return orders.findAllByOrderByCreatedAtDesc();}
    @PutMapping("/orders/{id}/status") public Order orderStatus(@PathVariable Long id,@Valid @RequestBody OrderStateInput input){return orderService.changeStatus(id,input.status(),input.reason());}
    @GetMapping("/social") public List<SocialOffer> social(){return offers.findAll();}
    @PostMapping("/social/{id}/confirm") public SocialOffer confirmSocial(@PathVariable Long id,@Valid @RequestBody ReasonInput input){return social.finish(id,false,input.reason());}
    @PostMapping("/social/{id}/cancel") public SocialOffer cancelSocial(@PathVariable Long id,@Valid @RequestBody ReasonInput input){return social.finish(id,true,input.reason());}
    @GetMapping("/overview") @Transactional(readOnly=true)
    public Map<String,Object> overview(){
        var all=products.findAll().stream().filter(p->!p.getArchived()).toList();var lots=inventory.list();var today=LocalDate.now();
        long expired=lots.stream().filter(l->l.getActive()&&l.getQuantity()>0&&l.getExpiryDate()!=null&&l.getExpiryDate().isBefore(today)).count();
        long soon=lots.stream().filter(l->inventory.usable(l)&&l.getExpiryDate()!=null&&!l.getExpiryDate().isAfter(today.plusDays(7))).count();
        long low=all.stream().filter(p->inventory.available(p)<=(p.getLowStockThreshold()==null?5:p.getLowStockThreshold())).count();
        return Map.of("products",all.size(),"expiredLots",expired,"urgentLots",soon,"lowStock",low,"pendingOrders",orders.findAll().stream().filter(o->"PENDING".equals(o.getStatus())).count(),"briefing",List.of("Проверьте "+expired+" просроченных партий: они исключены из доступного остатка.","У "+soon+" партий срок истекает в течение 7 дней. Рассчитайте скидку или согласуйте передачу.","У "+low+" товаров остаток ниже установленного порога. Подготовьте поступление."));
    }
    @PostMapping("/intelligence/briefing") public Map<String,Object> briefing(){return intelligence.explain(overview());}
}
