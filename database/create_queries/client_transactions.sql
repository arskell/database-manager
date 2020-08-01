CREATE TABLE if not exists 'client_transactions' (
'id' INTEGER PRIMARY KEY AUTOINCREMENT, 
'client_id' INTEGER NOT NULL,
'product_id' INTEGER NOT NULL,
'weight' FLOAT(24) NOT NULL,
'price_per_unit' FLOAT(24) NOT NULL,
'date' datetime default current_timestamp NOT NULL,
'transaction_id' INTEGER NOT NULL
);