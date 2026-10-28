package com.aria.service;
import com.aria.entity.Product;
import com.aria.enums.ProductStatus;
import com.aria.repository.ProductDetailRepository;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class PublicProductView {
    private final InventoryService inventory;private final PricingService pricing;private final ProductDetailRepository details;
    public PublicProductView(InventoryService inventory,PricingService pricing,ProductDetailRepository details){this.inventory=inventory;this.pricing=pricing;this.details=details;}
    public Map<String,Object> summary(Product p){
        Map<String,Object> row=new LinkedHashMap<>();int qty=inventory.available(p);
        boolean sellable=!p.getArchived()&&qty>0&&(p.getStatus()==ProductStatus.ACTIVE||p.getStatus()==ProductStatus.DISCOUNTED);
        row.put("id",p.getId());row.put("sku",p.getSku());row.put("name",p.getName());row.put("category",p.getCategory());row.put("unit",p.getUnit());
        row.put("imageUrl",p.getImageUrl()==null?"/images/product-generic.svg":p.getImageUrl());row.put("unitPrice",p.getUnitPrice());row.put("discountPercent",p.getDiscountPercent());
        row.put("displayPrice",p.getUnitPrice()==null||p.getUnitPrice().signum()<=0?null:pricing.price(p,p.getDiscountPercent()==null?0:p.getDiscountPercent()));row.put("expiryDate",p.getExpiryDate());row.put("quantityOnHand",sellable?qty:0);row.put("status",sellable?p.getStatus().name():"UNAVAILABLE");
        int sales=p.getSalesLast30Days()==null?0:p.getSalesLast30Days();row.put("popularityRank",sales>=50?3:sales>=20?2:sales>0?1:0);
        details.findByProductId(p.getId()).ifPresent(d->{row.put("brand",d.getBrand());row.put("description",d.getDescriptionLong());row.put("size",d.getSize());});
        return row;
    }
}
