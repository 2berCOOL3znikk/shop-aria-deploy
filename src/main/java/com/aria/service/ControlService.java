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
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.context.ApplicationEventPublisher events;
    private final ProductRepository products;
    private final ProductDetailRepository details;
    private final SupplierRepository suppliers;
    private final InventoryLotRepository lots;
    private final InventoryService inventory;
    private final PricingService pricing;
    private final AuditService audit;
    private final PriceHistoryService priceHistory;
    private final ProductSkuAllocationRepository skuAllocations;
    public ControlService(ProductRepository products,ProductDetailRepository details,SupplierRepository suppliers,InventoryLotRepository lots,InventoryService inventory,PricingService pricing,AuditService audit,PriceHistoryService priceHistory,ProductSkuAllocationRepository skuAllocations){
        this.products=products;this.details=details;this.suppliers=suppliers;this.lots=lots;this.inventory=inventory;this.pricing=pricing;this.audit=audit;
        this.priceHistory=priceHistory;
        this.skuAllocations=skuAllocations;
    }
    public Product saveProduct(Long id,ProductInput input){
        Product p=id==null?new Product():inventory.lock(id);
        var oldPrice=p.getUnitPrice();int oldPercent=p.getDiscountPercent()==null?0:p.getDiscountPercent();
        p.setMinimumPrice(input.minimumPrice());
        if(input.minimumPrice()!=null && input.unitPrice().compareTo(input.minimumPrice())<0)throw new IllegalArgumentException("Базовая цена ниже минимальной");
        String sku=input.sku()==null||input.sku().isBlank()?(id==null?allocateSku():p.getSku()):input.sku().trim().toUpperCase(java.util.Locale.ROOT);
        if(input.sku()!=null&&!input.sku().isBlank()&&sku.startsWith("ARIA-AUTO-")&&(id==null||!sku.equalsIgnoreCase(p.getSku())))throw new IllegalArgumentException("Этот диапазон кодов выделяется автоматически. Оставьте код пустым или задайте свой артикул.");
        if ((id==null || !sku.equalsIgnoreCase(p.getSku())) && products.existsBySkuIgnoreCase(sku)) throw new IllegalStateException("SKU уже занят другим товаром");
        if(id!=null && p.getArchived()) throw new IllegalStateException("Товар архивирован");
        if(input.imageUrl()!=null && !input.imageUrl().isBlank() && !input.imageUrl().startsWith("/images/") && !input.imageUrl().matches("/api/products/\\d+/photo") && !input.imageUrl().matches("https://[^\\s]+")) throw new IllegalArgumentException("Фото: загруженное изображение, путь /images/… или ссылка https://…");
        p.setSku(sku);p.setName(input.name().trim());p.setCategory(input.category().trim());p.setUnitPrice(input.unitPrice());
        p.setBarcode(input.barcode());p.setUnit(input.unit().trim());p.setLowStockThreshold(input.lowStockThreshold());
        p.setImageUrl(input.imageUrl()==null||input.imageUrl().isBlank()?"/images/product-generic.svg":input.imageUrl().trim());
        if(id==null){p.setReceivedAt(LocalDate.now());p.setUnitCost(java.math.BigDecimal.ZERO);}
        if(p.getDiscountPercent()!=null && p.getDiscountPercent()>0) pricing.validate(p,p.getDiscountPercent());
        products.saveAndFlush(p);
        if(oldPrice!=null)priceHistory.record(p,oldPrice,oldPercent,oldPercent,"Изменение карточки товара");
        ProductDetail detail=details.findByProductId(p.getId()).orElseGet(ProductDetail::new);detail.setProduct(p);detail.setBrand(input.brand());detail.setDescriptionLong(input.description());details.save(detail);
        events.publishEvent(new StoreChanged(p.getId(),"PRODUCT_CHANGED"));audit.record(id==null?"PRODUCT_CREATED":"PRODUCT_UPDATED","product:"+p.getId(),p.getSku()+" · "+p.getName()+" · цена "+p.getUnitPrice()+" ₸");return p;
    }
    public Map<String,Object> productAttributes(Long id){var p=inventory.lock(id);return attributesView(p,details.findByProductId(id).orElse(null));}
    public Map<String,Object> saveAttributes(Long id,ProductAttributesInput input){
        if(!input.confirmed())throw new IllegalArgumentException("Подтвердите проверку характеристик");
        var p=inventory.lock(id);if(p.getArchived())throw new IllegalStateException("Товар архивирован");
        var d=details.findByProductId(id).orElse(null);
        if(!Objects.equals(attributesRevision(p,d),input.revision()))throw new IllegalStateException("Характеристики изменились. Откройте форму заново и проверьте значения.");
        var color=blankToNull(input.color());var size=blankToNull(input.size());
        if(Objects.equals(color,d==null?null:d.getColor())&&Objects.equals(size,d==null?null:d.getSize())&&Objects.equals(decimal(input.weightKg()),decimal(d==null?null:d.getWeightKg())))return attributesView(p,d);
        if(d!=null&&!Objects.equals(size,d.getSize())&&d.getCatalogueReference()!=null&&p.getImageUrl()!=null&&p.getImageUrl().startsWith("/images/demo/"))p.setImageUrl("/images/product-placeholder.svg");
        if(d==null){d=new ProductDetail();d.setProduct(p);}
        d.setColor(color);d.setSize(size);d.setWeightKg(input.weightKg());details.saveAndFlush(d);
        // Owner input is never promoted to manufacturer verification or an AI-generated fact.
        events.publishEvent(new StoreChanged(id,"PRODUCT_CHANGED"));audit.record("PRODUCT_ATTRIBUTES_UPDATED","product:"+id,"Сведения магазина; подтверждены владельцем · "+input.reason().trim());
        return attributesView(p,d);
    }
    private String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}
    private String decimal(java.math.BigDecimal value){return value==null?null:value.stripTrailingZeros().toPlainString();}
    private Map<String,Object> attributesView(Product p,ProductDetail d){
        var out=new LinkedHashMap<String,Object>();out.put("productId",p.getId());out.put("name",p.getName());out.put("category",p.getCategory());out.put("revision",attributesRevision(p,d));
        out.put("color",d==null?null:d.getColor());out.put("size",d==null?null:d.getSize());out.put("weightKg",d==null?null:d.getWeightKg());return out;
    }
    private String attributesRevision(Product p,ProductDetail d){
        try{
            var bytes=new java.io.ByteArrayOutputStream();var values=new java.io.DataOutputStream(bytes);
            for(String value:new String[]{p.getCategory(),d==null?null:d.getColor(),d==null?null:d.getSize(),decimal(d==null?null:d.getWeightKg())}){
                if(value==null){values.writeInt(-1);}else{var encoded=value.getBytes(java.nio.charset.StandardCharsets.UTF_8);values.writeInt(encoded.length);values.write(encoded);}
            }
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        }catch(java.io.IOException|java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException("Не удалось проверить версию характеристик",impossible);}
    }
    private String allocateSku(){
        // Old manually named products are preserved; sequence and reserved prefix prevent new writers colliding.
        for(int attempt=0;attempt<1000;attempt++){String code="ARIA-AUTO-"+skuAllocations.saveAndFlush(new ProductSkuAllocation()).id;if(!products.existsBySkuIgnoreCase(code))return code;}
        throw new IllegalStateException("Не удалось выделить код товара. Обратитесь к администратору.");
    }
    public void archiveProduct(Long id){
        Product p=inventory.lock(id);
        if(p.getStatus()==ProductStatus.SOCIAL || p.getStatus()==ProductStatus.RESERVED) throw new IllegalStateException("Сначала завершите социальную передачу");
        inventory.ensureLegacyLot(p);
        if(lots.findByProductId(id).stream().anyMatch(l->l.getQuantity()>0)) throw new IllegalStateException("Перед архивированием продайте или спишите все партии товара");
        p.setArchived(true);events.publishEvent(new StoreChanged(id,"PRODUCT_ARCHIVED"));audit.record("PRODUCT_ARCHIVED","product:"+id,p.getSku());
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
        priceHistory.record(p,p.getUnitPrice(),p.getDiscountPercent()==null?0:p.getDiscountPercent(),input.percent(),input.reason());
        p.setDiscountPercent(input.percent());p.setStatus(input.percent()==0?ProductStatus.ACTIVE:ProductStatus.DISCOUNTED);
        events.publishEvent(new StoreChanged(id,"PRICE_CHANGED"));audit.record("DISCOUNT_CHANGED","product:"+id,input.percent()+"% · цена "+pricing.price(p,input.percent())+" ₸ · "+input.reason());
    }
}
