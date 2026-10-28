package com.aria.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;

@Entity
@Table(name="inventory_lots",uniqueConstraints=@UniqueConstraint(name="uk_product_lot_number",columnNames={"product_id","lot_number"}))
public class InventoryLot {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(nullable=false)
    private Product product;
    @ManyToOne(fetch=FetchType.EAGER)
    private Supplier supplier;
    @Column(nullable=false,length=80)
    private String lotNumber;
    @Column(nullable=false)
    private LocalDate receivedAt;

    private LocalDate manufacturedAt;

    private LocalDate expiryDate;
    @Column(nullable=false)
    private Integer initialQuantity = 0;
    @Column(nullable=false)
    private Integer quantity = 0;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal unitCost = BigDecimal.ZERO;
    @Column(nullable=false)
    private boolean active = true;
    public InventoryLot() {}
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Product getProduct() { return product; }
    public void setProduct(Product value) { product = value; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier value) { supplier = value; }
    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String value) { lotNumber = value; }
    public LocalDate getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDate value) { receivedAt = value; }
    public LocalDate getManufacturedAt() { return manufacturedAt; }
    public void setManufacturedAt(LocalDate value) { manufacturedAt = value; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate value) { expiryDate = value; }
    public Integer getInitialQuantity() { return initialQuantity; }
    public void setInitialQuantity(Integer value) { initialQuantity = value; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer value) { quantity = value; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal value) { unitCost = value; }
    public boolean getActive() { return active; }
    public void setActive(boolean value) { active = value; }
}
