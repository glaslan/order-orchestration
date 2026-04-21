package com.ftf.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ftf.order.model.InventorySyncLog;

//inherits from JPARepository, gives us database operations our of the box
//includes:
// save(entity) — insert or update
// findById(id) — look up by primary key
// findAll() — get all rows
// deleteById(id) — remove a row
// count() - count rows
//and more, autogen by Spring and JPA

public interface InventorySyncLogRepository extends JpaRepository<InventorySyncLog, Long> {

    // allows us to search for syncs by type (use for debugging failed syncs)
    List<InventorySyncLog> findByStatus(String status);
}
