-- ============================================================
-- v3：个人资料、聊天、注册
-- 在已有库上增量执行
-- ============================================================
USE campus_book;

-- 1) 用户资料扩展
--    实名信息（real_name / real_phone）只在管理端展示，
--    学生端接口会置空，避免泄露
ALTER TABLE sys_user
  ADD COLUMN gender      VARCHAR(8)   NULL COMMENT '男/女/保密',
  ADD COLUMN signature   VARCHAR(120) NULL COMMENT '个性签名',
  ADD COLUMN real_name   VARCHAR(32)  NULL COMMENT '真实姓名，仅管理端可见',
  ADD COLUMN real_phone  VARCHAR(20)  NULL COMMENT '实名手机号，仅管理端可见';

-- 2) 聊天消息
CREATE TABLE IF NOT EXISTS chat_message (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  conversation_key VARCHAR(64)  NOT NULL COMMENT '两个用户按 id 升序拼接，保证唯一',
  from_user_id     BIGINT       NOT NULL,
  to_user_id       BIGINT       NOT NULL,
  book_id          BIGINT       NULL COMMENT '围绕哪本书聊的，可为空',
  content          VARCHAR(1000) NOT NULL,
  type             VARCHAR(10)  NOT NULL DEFAULT 'text' COMMENT 'text/image',
  read_flag        TINYINT      NOT NULL DEFAULT 0,
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_conv (conversation_key, created_at),
  KEY idx_to_unread (to_user_id, read_flag)
) ENGINE=InnoDB COMMENT='买卖双方聊天消息';

-- 3) 给演示账号补一些资料，界面不至于空着
UPDATE sys_user SET gender = '男', signature = '计科在读，喜欢攒教材', real_name = '示例同学', real_phone = phone
 WHERE id % 7 = 0 AND role = 'student';
UPDATE sys_user SET gender = '女', signature = '考研党，出闲置换资料'
 WHERE id % 7 = 3 AND role = 'student';
