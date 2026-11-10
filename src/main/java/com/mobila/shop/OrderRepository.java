package com.mobila.shop;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    Optional<CustomerOrder> findByStripeSessionId(String stripeSessionId);
    List<CustomerOrder> findByUserIdOrderByIdDesc(Long userId);
    List<CustomerOrder> findAllByOrderByIdDesc();

    /** Codul promoțional a fost deja folosit de acest cont într-o comandă neanulată. */
    boolean existsByUserIdAndCouponCodeAndStatusNot(Long userId, String couponCode, String status);

    /** „Clienții au cumpărat și…”: produsele aflate în aceleași comenzi (neanulate, plătite sau la livrare) cu produsul dat, cele mai frecvente primele. */
    @Query("select i2.productId, count(i2) from OrderItem i1, OrderItem i2 where i1.productId = :pid and i2.order = i1.order and i2.productId <> :pid "
         + "and i1.order.status <> 'Anulată' and i1.order.status <> 'Așteaptă plata' group by i2.productId order by count(i2) desc")
    List<Object[]> alsoBought(@Param("pid") Long pid);
}
