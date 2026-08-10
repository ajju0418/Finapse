-- =============================================================
-- FINAPSE — Seed Data
-- =============================================================

USE finapse;

-- -------------------------------------------------------------
-- Default user (single-user MVP)
-- -------------------------------------------------------------
INSERT INTO users (id, name, created_at, updated_at) VALUES
('00000000-0000-0000-0000-000000000001', 'Local User', NOW(), NOW());

-- -------------------------------------------------------------
-- Default categories
-- -------------------------------------------------------------
INSERT INTO categories (id, name, display_name, created_at, updated_at) VALUES
('cat-00000000-0000-0000-0000-000000000001', 'FOOD_DINING',       'Food & Dining',      NOW(), NOW()),
('cat-00000000-0000-0000-0000-000000000002', 'GROCERIES',         'Groceries',          NOW(), NOW()),
('cat-00000000-0000-0000-0000-000000000003', 'SHOPPING',          'Shopping',           NOW(), NOW()),
('cat-00000000-0000-0000-0000-000000000004', 'TRANSPORTATION',    'Transportation',     NOW(), NOW()),
('cat-00000000-0000-0000-0000-000000000005', 'BILLS_UTILITIES',   'Bills & Utilities',  NOW(), NOW()),
('cat-00000000-0000-0000-0000-000000000006', 'ENTERTAINMENT',     'Entertainment',      NOW(), NOW()),
('cat-00000000-0000-0000-0000-000000000007', 'HEALTHCARE',        'Healthcare',         NOW(), NOW()),
('cat-00000000-0000-0000-0000-000000000008', 'TRAVEL',            'Travel',             NOW(), NOW()),
('cat-00000000-0000-0000-0000-000000000009', 'EDUCATION',         'Education',          NOW(), NOW()),
('cat-00000000-0000-0000-0000-000000000010', 'SUBSCRIPTIONS',     'Subscriptions',      NOW(), NOW()),
('cat-00000000-0000-0000-0000-000000000011', 'OTHER',             'Other',              NOW(), NOW());
