package com.aria.entity;
import jakarta.persistence.*;
@Entity
@Table(name="order_allocations")
public class OrderAllocation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(nullable=false) private Order order;
    @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(nullable=false) private InventoryLot lot;
    @Column(nullable=false) private int quantity;
    public OrderAllocation() {}
    public OrderAllocation(Order order,InventoryLot lot,int quantity){this.order=order;this.lot=lot;this.quantity=quantity;}
    public Long getId(){return id;}
    public InventoryLot getLot(){return lot;}
    public int getQuantity(){return quantity;}
}
