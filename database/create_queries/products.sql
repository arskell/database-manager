CREATE TABLE if not exists 'products' (
'product_id' INTEGER PRIMARY KEY AUTOINCREMENT, 
'name' TEXT NOT NULL,
'description' TEXT NOT NULL,
'price_per_unit' FLOAT(24) NOT NULL,
'weight' FLOAT(24) NOT NULL,
'picture_path' TEXT
);