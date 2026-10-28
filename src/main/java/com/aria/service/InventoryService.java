package com.aria.service;

import com.aria.dto.ControlRequests.LotInput;
import com.aria.entity.*;
import com.aria.enums.ProductStatus;
import com.aria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Product row is the transaction lock; lots are consumed FEFO under that lock. */
@Service
@Transactional
public class InventoryService {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private final ProductRepository products;
    private final InventoryLotRepository lots;
    private final SupplierRepository suppliers;
    private final AuditService audit;
    private final PricingService pricing;
    public InventoryService(ProductRepository products, InventoryLotRepository lots, SupplierRepository suppliers, AuditService audit, PricingService pricing) {
        this.products=products; this.lots=lots; this.suppliers=suppliers; this.audit=audit;
        this.pricing=pricing;
    }
    public Product lock(Long id) {
        Product p=products.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Товар не найден"));
        entityManager.refresh(p);return p;
    }
    public void ensureLegacyLot(Product p) {
        if (!lots.findByProductId(p.getId()).isEmpty() || p.getQuantityOnHand()==null || p.getQuantityOnHand()==0) return;
        InventoryLot lot=new InventoryLot(); lot.setProduct(p); lot.setLotNumber("INITIAL-"+p.getId());
        lot.setReceivedAt(p.getReceivedAt()); lot.setExpiryDate(p.getExpiryDate());
        lot.setQuantity(p.getQuantityOnHand()); lot.setInitialQuantity(p.getQuantityOnHand());
        lot.setUnitCost(p.getUnitCost()==null?BigDecimal.ZERO:p.getUnitCost()); lots.save(lot);
    }
    public boolean usable(InventoryLot lot) {
        return lot.getActive() && lot.getQuantity()>0 && (lot.getExpiryDate()==null || !lot.getExpiryDate().isBefore(LocalDate.now()));
    }
    @Transactional(readOnly=true)
    public int available(Product p) {
        return available(p,lots.findByProductId(p.getId()));
    }
    public int available(Product p,List<InventoryLot> rows) {
        if (rows.isEmpty()) return p.getExpiryDate()!=null && p.getExpiryDate().isBefore(LocalDate.now()) ? 0 : Math.max(0,p.getQuantityOnHand()==null?0:p.getQuantityOnHand());
        return Math.toIntExact(rows.stream().filter(this::usable).mapToLong(InventoryLot::getQuantity).sum());
    }
    public void synchronize(Product p) {
        List<InventoryLot> rows=lots.findByProductId(p.getId());
        if (rows.isEmpty()) return;
        List<InventoryLot> available=rows.stream().filter(this::usable).toList();
        long quantity=available.stream().mapToLong(InventoryLot::getQuantity).sum();
        if(quantity>Integer.MAX_VALUE)throw new IllegalArgumentException("Слишком большой общий остаток товара");
        p.setQuantityOnHand((int)quantity);
        p.setExpiryDate(available.stream().map(InventoryLot::getExpiryDate).filter(Objects::nonNull).min(LocalDate::compareTo).orElse(null));
        // Use the highest remaining purchase cost to avoid pricing below any available lot's cost.
        available.stream().map(InventoryLot::getUnitCost).max(BigDecimal::compareTo).ifPresent(p::setUnitCost);
        products.save(p);
    }
    public List<InventoryLot> list() { return lots.findAll(); }
    public void initializeExisting() {
        for(Product item:products.findAll()) {Product p=lock(item.getId());ensureLegacyLot(p);synchronize(p);}
    }
    public InventoryLot save(Long id, LotInput input) {
        Product p=lock(input.productId());
        if (p.getStatus()==ProductStatus.SOCIAL || p.getStatus()==ProductStatus.RESERVED || p.getStatus()==ProductStatus.TRANSFERRED || p.getStatus()==ProductStatus.WRITTEN_OFF)
            throw new IllegalStateException("Поступление недоступно: товар архивирован, списан или участвует в социальной передаче");
        if (input.expiryDate()!=null && (input.expiryDate().isBefore(input.receivedAt()) || input.expiryDate().isBefore(LocalDate.now())))
            throw new IllegalArgumentException("Нельзя принимать просроченный товар или срок раньше даты приёмки");
        if(input.expiryDate()==null && p.getCategory()!=null && p.getCategory().toLowerCase(java.util.Locale.ROOT).contains("продукт")) throw new IllegalArgumentException("Для продуктов укажите срок годности партии");
        if (input.manufacturedAt()!=null && input.manufacturedAt().isAfter(input.receivedAt()))
            throw new IllegalArgumentException("Дата производства не может быть позже даты приёмки");
        ensureLegacyLot(p);
        InventoryLot lot=id==null?new InventoryLot():lots.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Партия не найдена"));
        if (id!=null && !lot.getProduct().getId().equals(p.getId())) throw new IllegalArgumentException("Нельзя менять товар существующей партии");
        if (id!=null && (!lot.getActive() || !lot.getQuantity().equals(lot.getInitialQuantity()))) throw new IllegalStateException("После движения остатка редактирование партии запрещено. Оформите новое поступление или списание");
        if ((id==null || !lot.getLotNumber().equalsIgnoreCase(input.lotNumber().trim())) && lots.existsByProductIdAndLotNumber(p.getId(),input.lotNumber().trim())) throw new IllegalStateException("Номер партии уже существует для этого товара");
        lot.setProduct(p); lot.setLotNumber(input.lotNumber().trim()); lot.setReceivedAt(input.receivedAt());
        lot.setManufacturedAt(input.manufacturedAt()); lot.setExpiryDate(input.expiryDate());
        lot.setSupplier(input.supplierId()==null?null:suppliers.findById(input.supplierId()).filter(Supplier::getActive).orElseThrow(()->new IllegalArgumentException("Выберите действующего поставщика")));
        lot.setInitialQuantity(input.quantity()); lot.setQuantity(input.quantity()); lot.setUnitCost(input.unitCost());
        lots.saveAndFlush(lot); synchronize(p);
        if(p.getDiscountPercent()!=null && p.getDiscountPercent()>0) pricing.validate(p,p.getDiscountPercent());
        audit.record(id==null?"LOT_RECEIVED":"LOT_UPDATED","lot:"+lot.getId(),lot.getLotNumber()+" · "+input.quantity()+" ед."); return lot;
    }
    public record Consumption(InventoryLot lot,int quantity) {}
    public List<Consumption> consume(Product p,int quantity,String operation) {
        ensureLegacyLot(p);
        if (quantity<1 || quantity>available(p)) throw new IllegalStateException("Недостаточно пригодного товара «"+p.getName()+"». Обновите корзину");
        List<InventoryLot> rows=new ArrayList<>(lots.findByProductId(p.getId()).stream().filter(this::usable).toList());
        rows.sort(Comparator.comparing(InventoryLot::getExpiryDate,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(InventoryLot::getReceivedAt).thenComparing(InventoryLot::getId));
        int remaining=quantity;
        List<Consumption> consumed=new ArrayList<>();
        for (InventoryLot lot:rows) { int used=Math.min(remaining,lot.getQuantity()); if(used==0) continue; lot.setQuantity(lot.getQuantity()-used);remaining-=used;
            consumed.add(new Consumption(lot,used)); audit.record(operation,"lot:"+lot.getId(),"Товар "+p.getSku()+" · списано "+used+" ед."); }
        lots.saveAllAndFlush(rows); synchronize(p);
        return consumed;
    }
    public void restore(InventoryLot lot,int quantity) {
        Product p=lock(lot.getProduct().getId());entityManager.refresh(lot);lot.setQuantity(Math.addExact(lot.getQuantity(),quantity));
        lots.saveAndFlush(lot);synchronize(p);audit.record("ORDER_STOCK_RETURNED","lot:"+lot.getId(),"Возвращено "+quantity+" ед. Пригодность и архив проверяются отдельно");
    }
    public void writeOff(Long id,String reason) {
        InventoryLot lot=lots.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Партия не найдена"));
        Product p=lock(lot.getProduct().getId());
        entityManager.refresh(lot);
        if (p.getStatus()==ProductStatus.SOCIAL || p.getStatus()==ProductStatus.RESERVED) throw new IllegalStateException("Сначала завершите или отмените социальный резерв");
        if (!lot.getActive() || lot.getQuantity()==0) throw new IllegalStateException("Партия уже закрыта или израсходована");
        audit.record("LOT_WRITTEN_OFF","lot:"+id,"Количество: "+lot.getQuantity()+". Причина: "+reason);
        lot.setQuantity(0);lot.setActive(false);lots.saveAndFlush(lot);synchronize(p);
    }
    public void writeOffAll(Product p,String reason) {
        ensureLegacyLot(p);
        for (InventoryLot lot:lots.findByProductId(p.getId())) {lot.setQuantity(0);lot.setActive(false);}
        p.setQuantityOnHand(0);audit.record("PRODUCT_WRITTEN_OFF","product:"+p.getId(),reason);
    }
}
