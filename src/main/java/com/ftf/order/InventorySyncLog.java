package com.ftf.order;

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

@Entity
@Table(name = "inventory_sync_log")
@Getter
@Setter
@NoArgsConstructor
public class InventorySyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sync_started_at", nullable = false)
    private LocalDateTime syncStartedAt;

    @Column(name = "sync_finished_at")
    private LocalDateTime syncFinishedAt;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "records_processed", nullable = false)
    private int recordsProcessed;

    @Column(name = "records_inserted", nullable = false)
    private int recordsInserted;

    @Column(name = "records_updated", nullable = false)
    private int recordsUpdated;

    @Column(name = "records_deactivated", nullable = false)
    private int recordsDeactivated;

    @Column(name = "error_message")
    private String errorMessage;
}
