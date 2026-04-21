package com.ftf.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ftf.order.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCustomerId(String customerId);
    Optional<CartItem> findByCustomerIdAndInventoryItemId(String customerId, Long inventoryItemId);
    void deleteByCustomerId(String customerId);
}
