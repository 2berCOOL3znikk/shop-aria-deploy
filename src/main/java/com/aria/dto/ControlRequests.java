package com.aria.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public final class ControlRequests {
 private ControlRequests() {}
 public record ProductInput(@NotBlank @Size(max=64) String sku,@NotBlank @Size(max=500) String name,
  @NotBlank @Size(max=120) String category,@NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal unitPrice,
  @Size(max=100) String brand,@Size(max=10000) String description,@Size(max=500) String imageUrl,
  @Size(max=64) String barcode,@NotBlank @Size(max=20) String unit,@Min(0) @Max(1000000) int lowStockThreshold) {}
 public record SupplierInput(@NotBlank @Size(max=160) String name,@Size(max=160) String contact,@Size(max=1000) String notes,boolean active) {}
 public record LotInput(@NotNull Long productId,Long supplierId,@NotBlank @Size(max=80) String lotNumber,
  @NotNull @PastOrPresent LocalDate receivedAt,@PastOrPresent LocalDate manufacturedAt,LocalDate expiryDate,
  @Min(1) @Max(1000000) int quantity,@NotNull @DecimalMin("0.00") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal unitCost) {}
 public record DiscountInput(@Min(0) @Max(90) int percent,@NotBlank @Size(max=1000) String reason) {}
 public record ReasonInput(@NotBlank @Size(max=1000) String reason) {}
 public record OrderStateInput(@NotBlank @Pattern(regexp="SHIPPED|DELIVERED|CANCELLED") String status,@NotBlank @Size(max=1000) String reason) {}
 public record PolicyInput(@NotBlank @Size(max=120) String category,
  @NotNull @Min(1) @Max(3650) Integer warningDays,@NotNull @Min(0) @Max(3650) Integer criticalDays,@NotNull @Min(0) @Max(3650) Integer clearanceDays,
  @NotNull @Min(0) @Max(90) Integer warningPercent,@NotNull @Min(0) @Max(90) Integer criticalPercent,@NotNull @Min(0) @Max(90) Integer clearancePercent,
  @NotNull @Min(1) @Max(3650) Integer slowStockDays,@NotNull @Min(0) @Max(90) Integer slowStockPercent,@NotNull @Min(0) @Max(90) Integer minimumMarginPercent) {}
}
