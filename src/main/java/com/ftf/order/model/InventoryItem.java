package com.ftf.order.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//Java Persistence API lets us work with database rows as if they were Java Objects
//can genereate queries for us automatically with some setup
//must match init.sql, changes must be made to both

@Entity
@Table(name = "inventory_item")
@Getter
@Setter
@NoArgsConstructor
public class InventoryItem {

    // tells JPA this is OUR primary key, postgres handles creating it
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //remove source ID, they dont need it - they string match to remove items from inventory
    //when we send a sale to inv, they will return success/failure for the update
    //we will control the choice to pull database again

    // inventory team's item id
    @Column(name = "source_item_id", nullable = false, unique = true)
    private Long sourceItemId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    // can disable items for sale
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
