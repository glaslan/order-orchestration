package com.ftf.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ftf.order.model.OrderManifest;

public interface OrderManifestRepository extends JpaRepository<OrderManifest, Long> {
    List<OrderManifest> findByCustomerId(String customerId);
}
