DROP TABLE IF EXISTS user;

CREATE TABLE device (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  device_token TEXT NOT NULL,
  encryption_key TEXT NOT NULL,
  device_name TEXT NOT NULL
);