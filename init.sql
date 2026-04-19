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

-- persistent cart: one row per customer+item, updated in place as quantity changes
CREATE TABLE cart_item (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    inventory_item_id BIGINT NOT NULL REFERENCES inventory_item(id),
    quantity INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cart_quantity_positive CHECK (quantity > 0),
    CONSTRAINT uq_cart_customer_item UNIQUE (customer_id, inventory_item_id)
);

CREATE INDEX idx_cart_customer ON cart_item (customer_id);

-- one row per submitted order
CREATE TABLE order_manifest (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    customer_name VARCHAR(255),
    customer_email VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    pickup BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_manifest_customer ON order_manifest (customer_id);

-- snapshot of items at time of order; price is locked in so a price change doesn't affect old orders
CREATE TABLE order_item (
    id BIGSERIAL PRIMARY KEY,
    order_manifest_id BIGINT NOT NULL REFERENCES order_manifest(id),
    inventory_item_id BIGINT NOT NULL REFERENCES inventory_item(id),
    product_name VARCHAR(255) NOT NULL,
    price NUMERIC(10,2) NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT chk_order_item_quantity_positive CHECK (quantity > 0)
);

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
