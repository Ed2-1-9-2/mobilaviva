package com.mobila.shop;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishRepository extends JpaRepository<WishItem, Long> {
    List<WishItem> findByUserIdOrderByIdDesc(Long userId);

    List<WishItem> findByUserIdAndProductId(Long userId, Long productId);
}
