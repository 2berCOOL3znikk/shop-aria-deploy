package com.aria.dto;
import com.aria.entity.*;
import com.aria.enums.ProductStatus;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
public final class AdminViews {
    private AdminViews(){}
    public record ProductRow(Long id,String sku,String name,String category,String unit,String barcode,
        BigDecimal unitPrice,BigDecimal unitCost,int quantityOnHand,Integer lowStockThreshold,
        Integer discountPercent,ProductStatus status,boolean archived,String imageUrl,LocalDate expiryDate){}
    public record OrderLine(String productName,String productSku,int quantity,BigDecimal pricePerUnit){}
    public record OrderRow(Long id,LocalDateTime createdAt,LocalDateTime shippedAt,String ownerEmail,
        String deliveryAddress,String status,BigDecimal totalAmount,List<OrderLine> items){
        public static OrderRow from(Order o){return new OrderRow(o.getId(),o.getCreatedAt(),o.getShippedAt(),o.getOwnerEmail(),o.getDeliveryAddress(),o.getStatus(),o.getTotalAmount(),o.getItems().stream()
            .map(i->new OrderLine(i.getProductName()!=null?i.getProductName():i.getProduct().getName(),i.getProductSku()!=null?i.getProductSku():i.getProduct().getSku(),i.getQuantity(),i.getPricePerUnit())).toList());}
    }
    public record PageResult<T>(List<T> content,int number,int size,long totalElements,int totalPages){
        public static <T> PageResult<T> from(org.springframework.data.domain.Page<T> p){return new PageResult<>(p.getContent(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());}
    }
}
