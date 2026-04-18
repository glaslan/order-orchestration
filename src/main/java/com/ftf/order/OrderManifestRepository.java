package com.ftf.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderManifestRepository extends JpaRepository<OrderManifest, Long> {
    List<OrderManifest> findByCustomerId(String customerId);
}
