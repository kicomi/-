USE campus_book;
ALTER TABLE sys_user
  ADD COLUMN custom_major VARCHAR(64) NULL COMMENT '手动输入的专业名，优先于 major_id 展示';
