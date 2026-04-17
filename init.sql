CREATE TABLE inventory_item (
    id BIGSERIAL PRIMARY KEY,

    source_item_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
-- price can be up to 10 digits, with 2 decimal
    price NUMERIC(10, 2) NOT NULL DEFAULT 0,
    quantity INTEGER NOT NULL DEFAULT 0,

    category_id BIGINT,
    category_name VARCHAR(255),

    last_synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

-- no 2 rows in our database can have the same source id value, prevents duplicates
    CONSTRAINT uq_inventory_item_source_id UNIQUE (source_item_id),

-- make sure price and quantity are never negative
    CONSTRAINT chk_inventory_item_quantity_nonnegative CHECK (quantity >= 0),
    CONSTRAINT chk_inventory_item_price_nonnegative CHECK (price >= 0)
);

-- keeps an efficient index of values we care about searching - feel free to add
CREATE INDEX idx_inventory_item_name ON inventory_item (name);
CREATE INDEX idx_inventory_item_active ON inventory_item (is_active);
CREATE INDEX idx_inventory_item_category_id ON inventory_item (category_id);

-- sync log for updating database
CREATE TABLE inventory_sync_log (
    id BIGSERIAL PRIMARY KEY,
    sync_started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sync_finished_at TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    records_processed INTEGER NOT NULL DEFAULT 0,
    records_inserted INTEGER NOT NULL DEFAULT 0,
    records_updated INTEGER NOT NULL DEFAULT 0,
    records_deactivated INTEGER NOT NULL DEFAULT 0,
    error_message TEXT
);
