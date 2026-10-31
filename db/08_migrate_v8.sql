USE campus_book;
ALTER TABLE review
  ADD COLUMN images VARCHAR(1000) NULL COMMENT '评价图片，JSON 数组字符串';
