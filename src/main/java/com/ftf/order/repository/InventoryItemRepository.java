package com.ftf.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ftf.order.model.InventoryItem;

// interface definition for JPA
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    // inventory team keys items by name, so we do too for sync lookups
    Optional<InventoryItem> findByName(String name);

    // pulls all active items only
    List<InventoryItem> findByActiveTrue();
}
