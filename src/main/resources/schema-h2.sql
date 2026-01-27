-- Индекс для поиска по email
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- обычные индексы для полнотекстового поиска
CREATE INDEX IF NOT EXISTS idx_items_name_search ON items(name);
CREATE INDEX IF NOT EXISTS idx_items_description_search ON items(description);

-- Комбинированный индекс для поиска
CREATE INDEX IF NOT EXISTS idx_items_search_h2 ON items(name, description) WHERE is_available = true;