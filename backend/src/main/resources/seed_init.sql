-- Seed initial data for Finapse
-- No user is seeded: accounts are created through /api/auth/register.
-- A pre-auth "Local User" row from an older install is claimed automatically
-- by the first account that registers, so existing data stays reachable.

INSERT IGNORE INTO categories (id, name, display_name, created_at, updated_at) VALUES
('00000000-0000-0000-0000-000000000011', 'FOOD_DINING',       'Food & Dining',      NOW(), NOW()),
('00000000-0000-0000-0000-000000000012', 'GROCERIES',         'Groceries',          NOW(), NOW()),
('00000000-0000-0000-0000-000000000013', 'SHOPPING',          'Shopping',           NOW(), NOW()),
('00000000-0000-0000-0000-000000000014', 'TRANSPORTATION',    'Transportation',     NOW(), NOW()),
('00000000-0000-0000-0000-000000000015', 'BILLS_UTILITIES',   'Bills & Utilities',  NOW(), NOW()),
('00000000-0000-0000-0000-000000000016', 'ENTERTAINMENT',     'Entertainment',      NOW(), NOW()),
('00000000-0000-0000-0000-000000000017', 'HEALTHCARE',        'Healthcare',         NOW(), NOW()),
('00000000-0000-0000-0000-000000000018', 'TRAVEL',            'Travel',             NOW(), NOW()),
('00000000-0000-0000-0000-000000000019', 'EDUCATION',         'Education',          NOW(), NOW()),
('00000000-0000-0000-0000-000000000020', 'SUBSCRIPTIONS',     'Subscriptions',      NOW(), NOW()),
('00000000-0000-0000-0000-000000000021', 'OTHER',             'Other',              NOW(), NOW());
