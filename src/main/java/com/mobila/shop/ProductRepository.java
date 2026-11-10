package com.mobila.shop;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Query("select p.category, count(p) from Product p group by p.category order by p.category")
    List<Object[]> categoryCounts();

    @Query("select p.brand, count(p) from Product p group by p.brand order by p.brand")
    List<Object[]> brandCounts();

    @Query("select distinct p.category from Product p order by p.category")
    List<String> categories();

    /** Blochează rândul produsului până la sfârșitul tranzacției (previne vânzarea peste stoc). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> lockById(@Param("id") Long id);
}
