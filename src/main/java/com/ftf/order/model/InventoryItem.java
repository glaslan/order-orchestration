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

    // inventory team string-matches by name, so name is the natural key for sync
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "parent_category_id")
    private Long parentCategoryId;

    @Column(name = "parent_category_name")
    private String parentCategoryName;

    @Column(name = "category_level")
    private Integer categoryLevel;

    @Column(name = "last_stock_date")
    private LocalDateTime lastStockDate;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    // can disable items for sale
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
