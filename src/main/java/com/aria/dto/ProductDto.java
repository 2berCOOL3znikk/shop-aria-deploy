// dto/ProductDto.java
package com.aria.dto;

import java.math.BigDecimal;
import java.time.LocalDate;


public record ProductDto(
        Long id,
        String sku,
        String name,
        String category,
        Integer quantityOnHand,
        LocalDate receivedAt,
        Integer salesLast30Days,
        BigDecimal unitCost,
        BigDecimal unitPrice,
        LocalDate expiryDate,
        String status,        // enum как строка для клиента
        String ariaReason     // только если статус CANDIDATE/RESERVED
)

{}
