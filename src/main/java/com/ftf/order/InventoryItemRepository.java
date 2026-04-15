package com.ftf.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

// interface definition for JPA 
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    // generates query for finding inventory item by source id, optional may return
    // value or not
    Optional<InventoryItem> findBySourceItemId(Long sourceItemId);

    // pulls all active items only
    List<InventoryItem> findByActiveTrue();
}
