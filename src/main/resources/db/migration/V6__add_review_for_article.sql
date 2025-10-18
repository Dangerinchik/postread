begin;
ALTER TABLE articles ADD COLUMN review_for_article_id INTEGER;
ALTER TABLE articles ADD CONSTRAINT fk_articles_review_for
    FOREIGN KEY (review_for_article_id) REFERENCES articles(id) ON DELETE SET NULL;

CREATE INDEX idx_articles_review_for ON articles(review_for_article_id);
end;