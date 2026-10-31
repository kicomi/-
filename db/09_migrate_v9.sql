USE campus_book;

-- 拼音检索：校园里「gdsx」查「高等数学」很常见
ALTER TABLE book ADD COLUMN title_pinyin VARCHAR(200) NULL COMMENT '书名全拼+首字母，小写';
CREATE INDEX idx_book_pinyin ON book (title_pinyin);
