-- Расширения для PostgreSQL
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gin;

-- Триграммные индексы (только PostgreSQL)
CREATE INDEX IF NOT EXISTS idx_items_name_trgm ON items USING gin (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_items_description_trgm ON items USING gin (description gin_trgm_ops);

-- Комбинированный триграммный индекс для поиска
CREATE INDEX IF NOT EXISTS idx_items_search_combined ON items USING gin (
    name gin_trgm_ops,
    description gin_trgm_ops
) WHERE is_available = true;

-- Настройки autovacuum для таблиц с частыми обновлениями
ALTER TABLE bookings SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_vacuum_threshold = 1000,
    autovacuum_analyze_scale_factor = 0.02,
    autovacuum_analyze_threshold = 500
);

ALTER TABLE items SET (
    autovacuum_vacuum_scale_factor = 0.1,
    autovacuum_vacuum_threshold = 500
);