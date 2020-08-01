CREATE TABLE if not exists 'suppliers' (
'id' INTEGER PRIMARY KEY AUTOINCREMENT, 
'product_id' INTEGER NOT NULL,
'supplier_name' TEXT NOT NULL,
'weight' FLOAT(24) NOT NULL,
'price_per_unit' FLOAT(24) NOT NULL,
'date' datetime default current_timestamp NOT NULL
);