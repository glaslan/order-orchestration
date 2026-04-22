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

-- Insert placeholder products into inventory_item
INSERT INTO inventory_item (source_item_id, name, price, quantity, category_id, category_name, is_active)
VALUES
    -- Original 5
    (101, 'Apples', 5.00, 50, 1, 'Produce', TRUE),
    (102, 'Milk', 2.00, 20, 2, 'Dairy', TRUE),
    (103, 'Bread', 3.00, 15, 3, 'Bakery', TRUE),
    (104, 'Bananas', 0.99, 100, 1, 'Produce', TRUE),
    (105, 'Eggs', 4.50, 30, 2, 'Dairy', TRUE),
    -- Produce (Vegetables)
    (106, 'Carrots', 1.50, 60, 1, 'Produce', TRUE),
    (107, 'Broccoli', 2.75, 40, 1, 'Produce', TRUE),
    (108, 'Spinach', 3.00, 25, 1, 'Produce', TRUE),
    (109, 'Potatoes (5lb)', 4.99, 80, 1, 'Produce', TRUE),
    (110, 'Onions', 1.20, 90, 1, 'Produce', TRUE),
    (111, 'Garlic', 0.50, 150, 1, 'Produce', TRUE),
    (112, 'Bell Peppers', 1.50, 45, 1, 'Produce', TRUE),
    (113, 'Cucumbers', 0.80, 55, 1, 'Produce', TRUE),
    (114, 'Zucchini', 1.25, 35, 1, 'Produce', TRUE),
    (115, 'Kale', 2.50, 20, 1, 'Produce', TRUE),
    (116, 'Sweet Potatoes', 1.90, 40, 1, 'Produce', TRUE),
    (117, 'Corn (Ear)', 0.75, 200, 1, 'Produce', TRUE),
    (118, 'Cabbage', 1.80, 30, 1, 'Produce', TRUE),
    (119, 'Asparagus', 4.00, 15, 1, 'Produce', TRUE),
    (120, 'Brussels Sprouts', 3.50, 25, 1, 'Produce', TRUE),
    (121, 'Cauliflower', 3.25, 20, 1, 'Produce', TRUE),
    (122, 'Celery', 1.75, 30, 1, 'Produce', TRUE),
    (123, 'Eggplant', 2.00, 18, 1, 'Produce', TRUE),
    (124, 'Mushrooms', 3.50, 22, 1, 'Produce', TRUE),
    (125, 'Radishes', 1.00, 40, 1, 'Produce', TRUE),
    (126, 'Beets', 1.50, 30, 1, 'Produce', TRUE),
    (127, 'Green Beans', 2.99, 45, 1, 'Produce', TRUE),
    (128, 'Leeks', 2.50, 15, 1, 'Produce', TRUE),
    (129, 'Pumpkins', 6.00, 20, 1, 'Produce', TRUE),
    (130, 'Butternut Squash', 2.50, 25, 1, 'Produce', TRUE),
    -- Produce (Fruits)
    (131, 'Strawberries', 4.50, 30, 1, 'Produce', TRUE),
    (132, 'Blueberries', 5.00, 25, 1, 'Produce', TRUE),
    (133, 'Raspberries', 5.50, 15, 1, 'Produce', TRUE),
    (134, 'Blackberries', 5.50, 12, 1, 'Produce', TRUE),
    (135, 'Peaches', 1.50, 40, 1, 'Produce', TRUE),
    (136, 'Pears', 1.25, 35, 1, 'Produce', TRUE),
    (137, 'Plums', 0.99, 40, 1, 'Produce', TRUE),
    (138, 'Cherries', 6.00, 20, 1, 'Produce', TRUE),
    (139, 'Grapes (Red)', 3.99, 30, 1, 'Produce', TRUE),
    (140, 'Grapes (Green)', 3.99, 30, 1, 'Produce', TRUE),
    (141, 'Oranges', 0.80, 100, 1, 'Produce', TRUE),
    (142, 'Lemons', 0.60, 80, 1, 'Produce', TRUE),
    (143, 'Limes', 0.50, 80, 1, 'Produce', TRUE),
    (144, 'Grapefruit', 1.25, 25, 1, 'Produce', TRUE),
    (145, 'Melon (Cantaloupe)', 3.50, 15, 1, 'Produce', TRUE),
    (146, 'Watermelon', 7.00, 10, 1, 'Produce', TRUE),
    (147, 'Honeydew', 4.00, 12, 1, 'Produce', TRUE),
    (148, 'Apricots', 1.00, 30, 1, 'Produce', TRUE),
    (149, 'Cranberries', 3.50, 20, 1, 'Produce', TRUE),
    (150, 'Figs', 2.00, 15, 1, 'Produce', TRUE),
    -- Dairy & Eggs
    (151, 'Butter', 4.00, 40, 2, 'Dairy', TRUE),
    (152, 'Cheddar Cheese', 5.50, 30, 2, 'Dairy', TRUE),
    (153, 'Mozzarella', 6.00, 25, 2, 'Dairy', TRUE),
    (154, 'Yogurt (Plain)', 3.50, 20, 2, 'Dairy', TRUE),
    (155, 'Yogurt (Greek)', 4.50, 15, 2, 'Dairy', TRUE),
    (156, 'Cottage Cheese', 3.00, 18, 2, 'Dairy', TRUE),
    (157, 'Sour Cream', 2.50, 22, 2, 'Dairy', TRUE),
    (158, 'Heavy Cream', 3.50, 15, 2, 'Dairy', TRUE),
    (159, 'Goat Cheese', 7.00, 12, 2, 'Dairy', TRUE),
    (160, 'Swiss Cheese', 5.75, 20, 2, 'Dairy', TRUE),
    (161, 'Feta Cheese', 6.50, 15, 2, 'Dairy', TRUE),
    (162, 'Quail Eggs', 8.00, 10, 2, 'Dairy', TRUE),
    (163, 'Duck Eggs', 9.00, 8, 2, 'Dairy', TRUE),
    (164, 'Chocolate Milk', 3.00, 15, 2, 'Dairy', TRUE),
    (165, 'Whipped Cream', 3.25, 12, 2, 'Dairy', TRUE),
    -- Bakery
    (166, 'Sourdough Loaf', 6.00, 10, 3, 'Bakery', TRUE),
    (167, 'Bagels (6pk)', 5.00, 15, 3, 'Bakery', TRUE),
    (168, 'Baguette', 2.50, 20, 3, 'Bakery', TRUE),
    (169, 'Rye Bread', 4.50, 12, 3, 'Bakery', TRUE),
    (170, 'Dinner Rolls', 4.00, 18, 3, 'Bakery', TRUE),
    (171, 'Croissants (4pk)', 7.00, 10, 3, 'Bakery', TRUE),
    (172, 'Blueberry Muffin', 2.50, 24, 3, 'Bakery', TRUE),
    (173, 'Apple Pie', 12.00, 5, 3, 'Bakery', TRUE),
    (174, 'Cinnamon Rolls', 3.50, 15, 3, 'Bakery', TRUE),
    (175, 'Whole Wheat Bread', 3.50, 20, 3, 'Bakery', TRUE),
    -- Farm Pantry & Meat (Larder)
    (176, 'Honey (Raw)', 8.00, 30, 4, 'Pantry', TRUE),
    (177, 'Maple Syrup', 12.00, 20, 4, 'Pantry', TRUE),
    (178, 'Strawberry Jam', 4.50, 25, 4, 'Pantry', TRUE),
    (179, 'Pickles', 3.50, 30, 4, 'Pantry', TRUE),
    (180, 'Apple Cider', 5.00, 15, 4, 'Pantry', TRUE),
    (181, 'Bacon (1lb)', 8.00, 20, 5, 'Meat', TRUE),
    (182, 'Pork Sausage', 6.50, 25, 5, 'Meat', TRUE),
    (183, 'Ground Beef (1lb)', 7.00, 40, 5, 'Meat', TRUE),
    (184, 'Chicken Breast', 9.00, 30, 5, 'Meat', TRUE),
    (185, 'Chicken Thighs', 6.00, 35, 5, 'Meat', TRUE),
    (186, 'Whole Chicken', 15.00, 10, 5, 'Meat', TRUE),
    (187, 'Lamb Chops', 18.00, 8, 5, 'Meat', TRUE),
    (188, 'Beef Ribeye', 22.00, 6, 5, 'Meat', TRUE),
    (189, 'Ham (Sliced)', 5.50, 20, 5, 'Meat', TRUE),
    (190, 'Turkey Breast', 10.00, 15, 5, 'Meat', TRUE),
    -- Grains & Flour
    (191, 'All-Purpose Flour', 4.00, 50, 4, 'Pantry', TRUE),
    (192, 'Cornmeal', 3.00, 30, 4, 'Pantry', TRUE),
    (193, 'Oats (Rolled)', 3.50, 40, 4, 'Pantry', TRUE),
    (194, 'Brown Rice', 4.50, 35, 4, 'Pantry', TRUE),
    (195, 'White Rice', 3.00, 60, 4, 'Pantry', TRUE),
    (196, 'Quinoa', 6.00, 20, 4, 'Pantry', TRUE),
    (197, 'Popcorn Kernels', 2.50, 30, 4, 'Pantry', TRUE),
    (198, 'Sunflower Seeds', 3.00, 25, 4, 'Pantry', TRUE),
    (199, 'Walnuts', 7.00, 15, 4, 'Pantry', TRUE),
    (200, 'Almonds', 8.00, 15, 4, 'Pantry', TRUE),
    -- More Produce to hit 120
    (201, 'Arugula', 3.50, 20, 1, 'Produce', TRUE),
    (202, 'Bok Choy', 2.00, 25, 1, 'Produce', TRUE),
    (203, 'Swiss Chard', 3.00, 15, 1, 'Produce', TRUE),
    (204, 'Collard Greens', 2.50, 30, 1, 'Produce', TRUE),
    (205, 'Parsley', 1.25, 50, 1, 'Produce', TRUE),
    (206, 'Cilantro', 1.25, 50, 1, 'Produce', TRUE),
    (207, 'Basil', 2.50, 20, 1, 'Produce', TRUE),
    (208, 'Mint', 2.00, 20, 1, 'Produce', TRUE),
    (209, 'Rosemary', 2.50, 15, 1, 'Produce', TRUE),
    (210, 'Thyme', 2.50, 15, 1, 'Produce', TRUE),
    (211, 'Dill', 2.00, 20, 1, 'Produce', TRUE),
    (212, 'Chives', 1.50, 30, 1, 'Produce', TRUE),
    (213, 'Ginger', 3.00, 20, 1, 'Produce', TRUE),
    (214, 'Turmeric', 4.00, 10, 1, 'Produce', TRUE),
    (215, 'Horseradish', 3.50, 10, 1, 'Produce', TRUE),
    (216, 'Okra', 3.00, 25, 1, 'Produce', TRUE),
    (217, 'Parsnips', 2.00, 30, 1, 'Produce', TRUE),
    (218, 'Rhubarb', 4.00, 12, 1, 'Produce', TRUE),
    (219, 'Shallots', 3.00, 40, 1, 'Produce', TRUE),
    (220, 'Turnips', 1.50, 35, 1, 'Produce', TRUE)
ON CONFLICT (source_item_id) DO NOTHING;


