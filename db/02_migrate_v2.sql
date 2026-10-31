-- ============================================================
--  管理端功能扩展：用户封禁/禁言、广告位
--  在已有库上增量执行，不重建数据
-- ============================================================
USE campus_book;

-- 1) 用户封禁与禁言
ALTER TABLE sys_user
  ADD COLUMN ban_until   DATETIME     NULL COMMENT '临时封禁到期时间，NULL 表示永久封禁',
  ADD COLUMN ban_reason  VARCHAR(255) NULL COMMENT '封禁原因',
  ADD COLUMN mute_status TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 1 禁言',
  ADD COLUMN mute_until  DATETIME     NULL COMMENT '禁言到期时间，NULL 表示永久禁言',
  ADD COLUMN mute_reason VARCHAR(255) NULL COMMENT '禁言原因';

-- 2) 小程序广告位
CREATE TABLE IF NOT EXISTS banner (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  title       VARCHAR(100) NOT NULL COMMENT '广告标题',
  subtitle    VARCHAR(200) NULL COMMENT '副标题/推广语',
  image_url   VARCHAR(500) NULL COMMENT '图片地址',
  link_type   VARCHAR(20)  NOT NULL DEFAULT 'book' COMMENT 'book/category/search/url/none',
  link_value  VARCHAR(255) NULL COMMENT '跳转目标，如书籍ID、分类ID、搜索词、外链',
  position    VARCHAR(30)  NOT NULL DEFAULT 'home_banner' COMMENT '投放位置',
  sort        INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 投放中 0 已暂停',
  start_time  DATETIME     NULL COMMENT '投放开始时间',
  end_time    DATETIME     NULL COMMENT '投放结束时间',
  view_count  INT          NOT NULL DEFAULT 0,
  click_count INT          NOT NULL DEFAULT 0,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_banner_pos (position, status, sort)
) ENGINE=InnoDB COMMENT='小程序广告位';

-- 3) 演示广告数据（首页轮播 / 活动位）
INSERT INTO banner (title, subtitle, image_url, link_type, link_value, position, sort, status, view_count, click_count)
VALUES
  ('开学季教材专场', '同专业二手教材一站集齐', '', 'category', '1', 'home_banner', 1, 1, 1280, 96),
  ('考研书籍专区',   '真题、词汇、政治一网打尽', '', 'search',  '考研', 'home_banner', 2, 1, 864, 73),
  ('智能定价上线',   '拍照描述书况，自动给出参考价', '', 'url', '/pages/price/index', 'home_banner', 3, 1, 642, 51),
  ('同校同专业优先展示', '优先匹配你们班的同学', '', 'none', '', 'home_banner', 4, 0, 320, 12);
