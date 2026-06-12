-- Seed product catalogue. Prices are in cents.
MERGE INTO products (sku, name, description, price, stock_quantity) KEY (sku) VALUES
    ('SKU-EGGS-006',    'PnP Free Range Eggs (6 pack)',         'Large free range eggs, 6 per pack',     3999, 200),
    ('SKU-MILK-2L',     'Clover Full Cream Milk (2L)',          'Fresh full cream milk, 2 litres',       2999, 150),
    ('SKU-BREAD-700',   'Albany Superior White Bread (700g)',   'Sliced white bread, 700 grams',         1999, 300),
    ('SKU-KOO-BEANS',   'Koo Baked Beans in Tomato Sauce (410g)', 'Classic baked beans in tomato sauce', 1499, 500),
    ('SKU-TASTIC-2KG',  'Tastic Rice (2kg)',                    'Long grain parboiled rice, 2kg bag',    4999, 180),
    ('SKU-OIL-750ML',   'PnP Sunflower Oil (750ml)',            'Refined sunflower cooking oil',         2499, 220),
    ('SKU-OROS-ORG-2L', 'Oros Orange Squash (2L)',              'Orange flavoured squash concentrate',   3499, 120),
    ('SKU-FLOUR-2.5KG', 'Sasko Cake Flour (2.5kg)',             'All-purpose cake flour',                4299,  90),
    ('SKU-BULL-300G',   'Bull Brand Corned Meat (300g)',        'Canned corned beef, 300 grams',         2799, 160),
    ('SKU-GOUDA-200G',  'PnP Cheese Gouda Sliced (200g)',       'Sliced gouda cheese, 200 grams',        4499,  75);
