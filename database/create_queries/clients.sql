CREATE TABLE if not exists 'clients' (
'client_id' INTEGER PRIMARY KEY AUTOINCREMENT,
 'login' TEXT NOT NULL,
 'first_name' TEXT NOT NULL,
 'last_name' TEXT NOT NULL,
 'account_pass_hash' INTEGER NOT NULL
 );