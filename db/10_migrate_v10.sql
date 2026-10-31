USE campus_book;

-- 高信用用户的书籍标记为优先，首页与审核队列都排前面
ALTER TABLE book ADD COLUMN priority TINYINT NOT NULL DEFAULT 0 COMMENT '1=高信用卖家，优先展示';
CREATE INDEX idx_book_priority ON book (priority);
