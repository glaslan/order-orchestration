CREATE TABLE inventory_snapshot (
                                    id BIGSERIAL PRIMARY KEY,

                                    source_inventory_id BIGINT NOT NULL,
                                    source_product_id BIGINT NOT NULL,
                                    product_name VARCHAR(255) NOT NULL,
                                    quantity_on_hand INTEGER NOT NULL DEFAULT 0,
                                    unit_of_measure VARCHAR(50) NOT NULL,

                                    source_vendor_id BIGINT,
                                    source_category_id BIGINT,

                                    last_restocked_date TIMESTAMP,
                                    source_updated_at TIMESTAMP,
                                    last_synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                    CONSTRAINT uq_inventory_snapshot_inventory_id UNIQUE (source_inventory_id),
                                    CONSTRAINT uq_inventory_snapshot_product_id UNIQUE (source_product_id),
                                    CONSTRAINT chk_inventory_snapshot_quantity_nonnegative CHECK (quantity_on_hand >= 0)
);

CREATE INDEX idx_inventory_snapshot_product_name
    ON inventory_snapshot (product_name);

CREATE INDEX idx_inventory_snapshot_active
    ON inventory_snapshot (is_active);

CREATE INDEX idx_inventory_snapshot_vendor_id
    ON inventory_snapshot (source_vendor_id);

CREATE INDEX idx_inventory_snapshot_category_id
    ON inventory_snapshot (source_category_id);

CREATE INDEX idx_inventory_snapshot_source_updated_at
    ON inventory_snapshot (source_updated_at);

CREATE TABLE inventory_sync_log (
                                    sync_id BIGSERIAL PRIMARY KEY,
                                    sync_started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    sync_finished_at TIMESTAMP,
                                    status VARCHAR(30) NOT NULL,
                                    records_processed INTEGER NOT NULL DEFAULT 0,
                                    records_inserted INTEGER NOT NULL DEFAULT 0,
                                    records_updated INTEGER NOT NULL DEFAULT 0,
                                    records_deactivated INTEGER NOT NULL DEFAULT 0,
                                    error_message TEXT
);