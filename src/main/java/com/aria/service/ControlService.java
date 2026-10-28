package com.aria.service;

import com.aria.dto.ControlRequests.*;
import com.aria.entity.*;
import com.aria.enums.ProductStatus;
import com.aria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class ControlService {
    private final ProductRepository products;
    private final ProductDetailRepository details;
    private final SupplierRepository suppliers;
    private final InventoryLotRepository lots;
    private final InventoryService inventory;
    private final PricingService pricing;
    private final AuditService audit;
    public ControlService(ProductRepository products,ProductDetailRepository details,SupplierRepository suppliers,InventoryLotRepository lots,InventoryService inventory,PricingService pricing,AuditService audit){
        this.products=products;this.details=details;this.suppliers=suppliers;this.lots=lots;this.inventory=inventory;this.pricing=pricing;this.audit=audit;
    }
    public Product saveProduct(Long id,ProductInput input){
        Product p=id==null?new Product():inventory.lock(id);
        if ((id==null || !input.sku().trim().equalsIgnoreCase(p.getSku())) && products.existsBySkuIgnoreCase(input.sku().trim())) throw new IllegalStateException("SKU уже занят другим товаром");
        if(id!=null && p.getArchived()) throw new IllegalStateException("Товар архивирован");
        if(input.imageUrl()!=null && !input.imageUrl().isBlank() && !input.imageUrl().startsWith("/images/") && !input.imageUrl().matches("/api/products/\\d+/photo") && !input.imageUrl().matches("https://[^\\s]+")) throw new IllegalArgumentException("Фото: загруженное изображение, путь /images/… или ссылка https://…");
        p.setSku(input.sku().trim().toUpperCase(java.util.Locale.ROOT));p.setName(input.name().trim());p.setCategory(input.category().trim());p.setUnitPrice(input.unitPrice());
        p.setBarcode(input.barcode());p.setUnit(input.unit().trim());p.setLowStockThreshold(input.lowStockThreshold());
        p.setImageUrl(input.imageUrl()==null||input.imageUrl().isBlank()?"/images/product-generic.svg":input.imageUrl().trim());
        if(id==null){p.setReceivedAt(LocalDate.now());p.setUnitCost(java.math.BigDecimal.ZERO);}
        if(p.getDiscountPercent()!=null && p.getDiscountPercent()>0) pricing.validate(p,p.getDiscountPercent());
        products.saveAndFlush(p);
        ProductDetail detail=details.findByProductId(p.getId()).orElseGet(ProductDetail::new);detail.setProduct(p);detail.setBrand(input.brand());detail.setDescriptionLong(input.description());details.save(detail);
        audit.record(id==null?"PRODUCT_CREATED":"PRODUCT_UPDATED","product:"+p.getId(),p.getSku()+" · "+p.getName()+" · цена "+p.getUnitPrice()+" ₸");return p;
    }
    public void archiveProduct(Long id){
        Product p=inventory.lock(id);
        if(p.getStatus()==ProductStatus.SOCIAL || p.getStatus()==ProductStatus.RESERVED) throw new IllegalStateException("Сначала завершите социальную передачу");
        inventory.ensureLegacyLot(p);
        if(lots.findByProductId(id).stream().anyMatch(l->l.getQuantity()>0)) throw new IllegalStateException("Перед архивированием продайте или спишите все партии товара");
        p.setArchived(true);audit.record("PRODUCT_ARCHIVED","product:"+id,p.getSku());
    }
    public Supplier saveSupplier(Long id,SupplierInput input){
        Supplier s=id==null?new Supplier():suppliers.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Поставщик не найден"));
        s.setName(input.name().trim());s.setContact(input.contact());s.setNotes(input.notes());s.setActive(input.active());suppliers.save(s);
        audit.record(id==null?"SUPPLIER_CREATED":"SUPPLIER_UPDATED","supplier:"+s.getId(),s.getName());return s;
    }
    public void deleteSupplier(Long id){
        Supplier s=suppliers.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Поставщик не найден"));
        if(lots.existsBySupplierId(id)) throw new IllegalStateException("Поставщик связан с партиями. Снимите флажок «Активен», чтобы сохранить историю");
        suppliers.delete(s);audit.record("SUPPLIER_DELETED","supplier:"+id,s.getName());
    }
    public void applyDiscount(Long id,DiscountInput input){
        Product p=inventory.lock(id);inventory.ensureLegacyLot(p);inventory.synchronize(p);
        if(input.expectedPrice()!=null && pricing.price(p,input.percent()).compareTo(input.expectedPrice())!=0)
            throw new IllegalStateException("Цена товара изменилась после расчёта. Обновите сценарий и подтвердите скидку заново");
        if(input.percent()!=0 || !(p.getStatus()==ProductStatus.ACTIVE||p.getStatus()==ProductStatus.DISCOUNTED||p.getStatus()==ProductStatus.CANDIDATE))pricing.validate(p,input.percent());
        if(p.getArchived()) throw new IllegalStateException("Товар архивирован");
        p.setDiscountPercent(input.percent());p.setStatus(input.percent()==0?ProductStatus.ACTIVE:ProductStatus.DISCOUNTED);
        audit.record("DISCOUNT_CHANGED","product:"+id,input.percent()+"% · цена "+pricing.price(p,input.percent())+" ₸ · "+input.reason());
    }
}
