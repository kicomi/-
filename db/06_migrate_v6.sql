USE campus_book;

-- 举报/投诉：买卖纠纷、违规书籍的入口
CREATE TABLE IF NOT EXISTS report (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  reporter_id BIGINT       NOT NULL COMMENT '举报人',
  target_type VARCHAR(16)  NOT NULL COMMENT 'book / user',
  target_id   BIGINT       NOT NULL,
  reason      VARCHAR(32)  NOT NULL COMMENT '举报原因',
  detail      VARCHAR(500) NULL COMMENT '补充说明',
  status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 待处理 1 已处理 2 已驳回',
  handle_note VARCHAR(200) NULL,
  handler_id  BIGINT       NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  handled_at  DATETIME     NULL,
  PRIMARY KEY (id),
  KEY idx_status (status, id),
  KEY idx_target (target_type, target_id)
) ENGINE=InnoDB COMMENT='举报记录';
