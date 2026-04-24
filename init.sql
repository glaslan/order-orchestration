CREATE TABLE inventory_item (
    id BIGSERIAL PRIMARY KEY,

-- inventory team string-matches by product name, so name is our natural key
    name VARCHAR(255) NOT NULL,
-- price can be up to 10 digits, with 2 decimal
    price NUMERIC(10, 2) NOT NULL DEFAULT 0,
    quantity INTEGER NOT NULL DEFAULT 0,

    category_id BIGINT,
    category_name VARCHAR(255),
    parent_category_id BIGINT,
    parent_category_name VARCHAR(255),
    category_level INTEGER,

    last_stock_date TIMESTAMP,
    last_synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

-- no 2 rows can share the same product name, matches inventory team's uniqueness assumption
    CONSTRAINT uq_inventory_item_name UNIQUE (name),

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

-- Insert placeholder products into inventory_item
INSERT INTO inventory_item (name, price, quantity, category_id, category_name, is_active)
VALUES
    -- Original 5
    ('Apples', 5.00, 50, 1, 'Produce', TRUE),
    ('Milk', 2.00, 20, 2, 'Dairy', TRUE),
    ('Bread', 3.00, 15, 3, 'Bakery', TRUE),
    ('Bananas', 0.99, 100, 1, 'Produce', TRUE),
    ('Eggs', 4.50, 30, 2, 'Dairy', TRUE),
    -- Produce (Vegetables)
    ('Carrots', 1.50, 60, 1, 'Produce', TRUE),
    ('Broccoli', 2.75, 40, 1, 'Produce', TRUE),
    ('Spinach', 3.00, 25, 1, 'Produce', TRUE),
    ('Potatoes (5lb)', 4.99, 80, 1, 'Produce', TRUE),
    ('Onions', 1.20, 90, 1, 'Produce', TRUE),
    ('Garlic', 0.50, 150, 1, 'Produce', TRUE),
    ('Bell Peppers', 1.50, 45, 1, 'Produce', TRUE),
    ('Cucumbers', 0.80, 55, 1, 'Produce', TRUE),
    ('Zucchini', 1.25, 35, 1, 'Produce', TRUE),
    ('Kale', 2.50, 20, 1, 'Produce', TRUE),
    ('Sweet Potatoes', 1.90, 40, 1, 'Produce', TRUE),
    ('Corn (Ear)', 0.75, 200, 1, 'Produce', TRUE),
    ('Cabbage', 1.80, 30, 1, 'Produce', TRUE),
    ('Asparagus', 4.00, 15, 1, 'Produce', TRUE),
    ('Brussels Sprouts', 3.50, 25, 1, 'Produce', TRUE),
    ('Cauliflower', 3.25, 20, 1, 'Produce', TRUE),
    ('Celery', 1.75, 30, 1, 'Produce', TRUE),
    ('Eggplant', 2.00, 18, 1, 'Produce', TRUE),
    ('Mushrooms', 3.50, 22, 1, 'Produce', TRUE),
    ('Radishes', 1.00, 40, 1, 'Produce', TRUE),
    ('Beets', 1.50, 30, 1, 'Produce', TRUE),
    ('Green Beans', 2.99, 45, 1, 'Produce', TRUE),
    ('Leeks', 2.50, 15, 1, 'Produce', TRUE),
    ('Pumpkins', 6.00, 20, 1, 'Produce', TRUE),
    ('Butternut Squash', 2.50, 25, 1, 'Produce', TRUE),
    -- Produce (Fruits)
    ('Strawberries', 4.50, 30, 1, 'Produce', TRUE),
    ('Blueberries', 5.00, 25, 1, 'Produce', TRUE),
    ('Raspberries', 5.50, 15, 1, 'Produce', TRUE),
    ('Blackberries', 5.50, 12, 1, 'Produce', TRUE),
    ('Peaches', 1.50, 40, 1, 'Produce', TRUE),
    ('Pears', 1.25, 35, 1, 'Produce', TRUE),
    ('Plums', 0.99, 40, 1, 'Produce', TRUE),
    ('Cherries', 6.00, 20, 1, 'Produce', TRUE),
    ('Grapes (Red)', 3.99, 30, 1, 'Produce', TRUE),
    ('Grapes (Green)', 3.99, 30, 1, 'Produce', TRUE),
    ('Oranges', 0.80, 100, 1, 'Produce', TRUE),
    ('Lemons', 0.60, 80, 1, 'Produce', TRUE),
    ('Limes', 0.50, 80, 1, 'Produce', TRUE),
    ('Grapefruit', 1.25, 25, 1, 'Produce', TRUE),
    ('Melon (Cantaloupe)', 3.50, 15, 1, 'Produce', TRUE),
    ('Watermelon', 7.00, 10, 1, 'Produce', TRUE),
    ('Honeydew', 4.00, 12, 1, 'Produce', TRUE),
    ('Apricots', 1.00, 30, 1, 'Produce', TRUE),
    ('Cranberries', 3.50, 20, 1, 'Produce', TRUE),
    ('Figs', 2.00, 15, 1, 'Produce', TRUE),
    -- Dairy & Eggs
    ('Butter', 4.00, 40, 2, 'Dairy', TRUE),
    ('Cheddar Cheese', 5.50, 30, 2, 'Dairy', TRUE),
    ('Mozzarella', 6.00, 25, 2, 'Dairy', TRUE),
    ('Yogurt (Plain)', 3.50, 20, 2, 'Dairy', TRUE),
    ('Yogurt (Greek)', 4.50, 15, 2, 'Dairy', TRUE),
    ('Cottage Cheese', 3.00, 18, 2, 'Dairy', TRUE),
    ('Sour Cream', 2.50, 22, 2, 'Dairy', TRUE),
    ('Heavy Cream', 3.50, 15, 2, 'Dairy', TRUE),
    ('Goat Cheese', 7.00, 12, 2, 'Dairy', TRUE),
    ('Swiss Cheese', 5.75, 20, 2, 'Dairy', TRUE),
    ('Feta Cheese', 6.50, 15, 2, 'Dairy', TRUE),
    ('Quail Eggs', 8.00, 10, 2, 'Dairy', TRUE),
    ('Duck Eggs', 9.00, 8, 2, 'Dairy', TRUE),
    ('Chocolate Milk', 3.00, 15, 2, 'Dairy', TRUE),
    ('Whipped Cream', 3.25, 12, 2, 'Dairy', TRUE),
    -- Bakery
    ('Sourdough Loaf', 6.00, 10, 3, 'Bakery', TRUE),
    ('Bagels (6pk)', 5.00, 15, 3, 'Bakery', TRUE),
    ('Baguette', 2.50, 20, 3, 'Bakery', TRUE),
    ('Rye Bread', 4.50, 12, 3, 'Bakery', TRUE),
    ('Dinner Rolls', 4.00, 18, 3, 'Bakery', TRUE),
    ('Croissants (4pk)', 7.00, 10, 3, 'Bakery', TRUE),
    ('Blueberry Muffin', 2.50, 24, 3, 'Bakery', TRUE),
    ('Apple Pie', 12.00, 5, 3, 'Bakery', TRUE),
    ('Cinnamon Rolls', 3.50, 15, 3, 'Bakery', TRUE),
    ('Whole Wheat Bread', 3.50, 20, 3, 'Bakery', TRUE),
    -- Farm Pantry & Meat (Larder)
    ('Honey (Raw)', 8.00, 30, 4, 'Pantry', TRUE),
    ('Maple Syrup', 12.00, 20, 4, 'Pantry', TRUE),
    ('Strawberry Jam', 4.50, 25, 4, 'Pantry', TRUE),
    ('Pickles', 3.50, 30, 4, 'Pantry', TRUE),
    ('Apple Cider', 5.00, 15, 4, 'Pantry', TRUE),
    ('Bacon (1lb)', 8.00, 20, 5, 'Meat', TRUE),
    ('Pork Sausage', 6.50, 25, 5, 'Meat', TRUE),
    ('Ground Beef (1lb)', 7.00, 40, 5, 'Meat', TRUE),
    ('Chicken Breast', 9.00, 30, 5, 'Meat', TRUE),
    ('Chicken Thighs', 6.00, 35, 5, 'Meat', TRUE),
    ('Whole Chicken', 15.00, 10, 5, 'Meat', TRUE),
    ('Lamb Chops', 18.00, 8, 5, 'Meat', TRUE),
    ('Beef Ribeye', 22.00, 6, 5, 'Meat', TRUE),
    ('Ham (Sliced)', 5.50, 20, 5, 'Meat', TRUE),
    ('Turkey Breast', 10.00, 15, 5, 'Meat', TRUE),
    -- Grains & Flour
    ('All-Purpose Flour', 4.00, 50, 4, 'Pantry', TRUE),
    ('Cornmeal', 3.00, 30, 4, 'Pantry', TRUE),
    ('Oats (Rolled)', 3.50, 40, 4, 'Pantry', TRUE),
    ('Brown Rice', 4.50, 35, 4, 'Pantry', TRUE),
    ('White Rice', 3.00, 60, 4, 'Pantry', TRUE),
    ('Quinoa', 6.00, 20, 4, 'Pantry', TRUE),
    ('Popcorn Kernels', 2.50, 30, 4, 'Pantry', TRUE),
    ('Sunflower Seeds', 3.00, 25, 4, 'Pantry', TRUE),
    ('Walnuts', 7.00, 15, 4, 'Pantry', TRUE),
    ('Almonds', 8.00, 15, 4, 'Pantry', TRUE),
    -- More Produce to hit 120
    ('Arugula', 3.50, 20, 1, 'Produce', TRUE),
    ('Bok Choy', 2.00, 25, 1, 'Produce', TRUE),
    ('Swiss Chard', 3.00, 15, 1, 'Produce', TRUE),
    ('Collard Greens', 2.50, 30, 1, 'Produce', TRUE),
    ('Parsley', 1.25, 50, 1, 'Produce', TRUE),
    ('Cilantro', 1.25, 50, 1, 'Produce', TRUE),
    ('Basil', 2.50, 20, 1, 'Produce', TRUE),
    ('Mint', 2.00, 20, 1, 'Produce', TRUE),
    ('Rosemary', 2.50, 15, 1, 'Produce', TRUE),
    ('Thyme', 2.50, 15, 1, 'Produce', TRUE),
    ('Dill', 2.00, 20, 1, 'Produce', TRUE),
    ('Chives', 1.50, 30, 1, 'Produce', TRUE),
    ('Ginger', 3.00, 20, 1, 'Produce', TRUE),
    ('Turmeric', 4.00, 10, 1, 'Produce', TRUE),
    ('Horseradish', 3.50, 10, 1, 'Produce', TRUE),
    ('Okra', 3.00, 25, 1, 'Produce', TRUE),
    ('Parsnips', 2.00, 30, 1, 'Produce', TRUE),
    ('Rhubarb', 4.00, 12, 1, 'Produce', TRUE),
    ('Shallots', 3.00, 40, 1, 'Produce', TRUE),
    ('Turnips', 1.50, 35, 1, 'Produce', TRUE)
ON CONFLICT (name) DO NOTHING;


