package com.ftf.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ftf.order.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderManifestId(Long orderManifestId);
}
