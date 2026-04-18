package com.ftf.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCustomerId(String customerId);
    Optional<CartItem> findByCustomerIdAndInventoryItemId(String customerId, Long inventoryItemId);
    void deleteByCustomerId(String customerId);
}
