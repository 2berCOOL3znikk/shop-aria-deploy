package com.aria.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TransferLotDto(
        Long id,
        String partner,
        LocalDateTime createdAt,
        String state,
        String note,
        List<ProductDto> items
) {}