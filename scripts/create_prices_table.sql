CREATE TABLE IF NOT EXISTS prices (
  item_id TEXT NOT NULL,
  item_name TEXT,
  tier INTEGER,
  enchantment INTEGER,
  city TEXT NOT NULL,
  quality INTEGER NOT NULL,
  sell_price_min INTEGER,
  sell_price_min_date TEXT,
  buy_price_max INTEGER,
  buy_price_max_date TEXT,
  retrieved_at TEXT NOT NULL,
  PRIMARY KEY (item_id, city, quality, retrieved_at)
);
