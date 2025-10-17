-- Удаляем старую колонку article_text (если существует)
ALTER TABLE articles DROP COLUMN IF EXISTS article_text;

-- Добавляем колонку для хранения количества блоков (опционально)
ALTER TABLE articles ADD COLUMN IF NOT EXISTS blocks_count INTEGER DEFAULT 0;

-- Обновляем существующие записи (если нужно)
UPDATE articles SET blocks_count = 0 WHERE blocks_count IS NULL;