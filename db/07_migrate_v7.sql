USE campus_book;

-- 站内通知：订单状态、举报结果、审核结果
CREATE TABLE IF NOT EXISTS notification (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  user_id    BIGINT       NOT NULL COMMENT '接收人',
  type       VARCHAR(16)  NOT NULL COMMENT 'order/report/book/system',
  title      VARCHAR(64)  NOT NULL,
  content    VARCHAR(300) NULL,
  link_type  VARCHAR(16)  NULL COMMENT 'order/book/chat',
  link_id    BIGINT       NULL,
  read_flag  TINYINT      NOT NULL DEFAULT 0,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user (user_id, read_flag, id)
) ENGINE=InnoDB COMMENT='站内通知';
