package com.aria.repository;

import com.aria.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Order> {
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(o.totalAmount),0),count(o) from Order o where o.shippedAt>=:since and o.status in ('SHIPPED','DELIVERED')")
    List<Object[]> recordedRevenue(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);
    @org.springframework.data.jpa.repository.Query("select o.shippedAt, i.quantity, i.pricePerUnit from Order o join o.items i where i.product.id=:productId and o.shippedAt>=:since and o.shippedAt<=:until and o.status in ('SHIPPED','DELIVERED') order by o.shippedAt,o.id,i.id")
    List<Object[]> pricingSales(@org.springframework.data.repository.query.Param("productId") Long productId,@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since,@org.springframework.data.repository.query.Param("until") java.time.LocalDateTime until);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Order o where o.id=:id")
    java.util.Optional<Order> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);

    @org.springframework.data.jpa.repository.Query("select i.product.id, sum(i.quantity) from Order o join o.items i where o.shippedAt >= :since and o.status in ('SHIPPED','DELIVERED') group by i.product.id")
    List<Object[]> shippedUnitsSince(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);
}
