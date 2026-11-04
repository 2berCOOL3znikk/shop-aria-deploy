package com.aria.entity;
import jakarta.persistence.*;

/** Reserved codes cannot be supplied manually; the sequence survives rollback and restart. */
@Entity @Table(name="product_sku_allocations")
public class ProductSkuAllocation {
    @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="product_sku_sequence")
    @SequenceGenerator(name="product_sku_sequence",sequenceName="aria_product_sku_seq",allocationSize=1)
    public Long id;
}
