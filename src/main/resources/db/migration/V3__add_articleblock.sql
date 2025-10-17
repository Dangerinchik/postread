-- Создание таблицы для блоков статьи
CREATE TABLE article_blocks (
id BIGSERIAL PRIMARY KEY,
article_id BIGINT NOT NULL,
block_type VARCHAR(20) NOT NULL CHECK (block_type IN ('text', 'media', 'code')),
content TEXT,
block_order INTEGER NOT NULL,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

FOREIGN KEY (article_id)
REFERENCES articles(id)
ON DELETE CASCADE,

UNIQUE (article_id, block_order)
);

CREATE INDEX idx_article_blocks_article_id ON article_blocks(article_id);
CREATE INDEX idx_article_blocks_order ON article_blocks(block_order);
CREATE INDEX idx_article_blocks_type ON article_blocks(block_type);