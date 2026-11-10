package com.mobila.shop;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdOrderByIdDesc(Long productId);

    List<Review> findByProductIdAndUserId(Long productId, Long userId);

    @Query("select avg(r.stars) from Review r where r.productId = :pid")
    Double avgStars(@Param("pid") Long pid);
}
