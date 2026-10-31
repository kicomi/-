-- ============================================================
--  校园二手书供需匹配与智能定价系统  数据库结构
--  MySQL 8.0 / utf8mb4
-- ============================================================
DROP DATABASE IF EXISTS campus_book;
CREATE DATABASE campus_book DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE campus_book;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1. 基础字典
-- ------------------------------------------------------------
CREATE TABLE major (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(64)  NOT NULL COMMENT '专业名称',
  college     VARCHAR(64)  NOT NULL COMMENT '所属学院',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_major_name (name)
) ENGINE=InnoDB COMMENT='专业';

CREATE TABLE course (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(128) NOT NULL COMMENT '课程名称',
  major_id    BIGINT       NULL,
  semester    VARCHAR(16)  NULL COMMENT '开课学期',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_major (major_id)
) ENGINE=InnoDB COMMENT='课程';

CREATE TABLE category (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  name        VARCHAR(64) NOT NULL,
  parent_id   BIGINT      NOT NULL DEFAULT 0,
  sort        INT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='图书分类';

CREATE TABLE tag (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(64)  NOT NULL,
  type        VARCHAR(16)  NOT NULL DEFAULT 'keyword' COMMENT 'keyword/course/major',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tag_name (name)
) ENGINE=InnoDB COMMENT='标签体系';

-- ------------------------------------------------------------
-- 2. 用户
-- ------------------------------------------------------------
CREATE TABLE sys_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  openid        VARCHAR(64)  NULL COMMENT '微信 openid',
  student_no    VARCHAR(32)  NULL COMMENT '学号',
  nickname      VARCHAR(64)  NOT NULL DEFAULT '同学',
  avatar        VARCHAR(255) NULL,
  phone         VARCHAR(20)  NULL,
  major_id      BIGINT       NULL,
  grade         VARCHAR(16)  NULL COMMENT '年级 如 2023',
  role          VARCHAR(16)  NOT NULL DEFAULT 'student' COMMENT 'student/admin',
  password      VARCHAR(100) NULL COMMENT '管理员密码(bcrypt)',
  credit_score  INT          NOT NULL DEFAULT 100,
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_openid (openid),
  KEY idx_user_major (major_id)
) ENGINE=InnoDB COMMENT='用户(学生/管理员)';

-- ------------------------------------------------------------
-- 3. 书籍
-- ------------------------------------------------------------
CREATE TABLE book (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  seller_id        BIGINT        NOT NULL,
  title            VARCHAR(200)  NOT NULL,
  author           VARCHAR(100)  NULL,
  isbn             VARCHAR(32)   NULL,
  publisher        VARCHAR(100)  NULL,
  publish_year     INT           NULL,
  edition          VARCHAR(32)   NULL,
  category_id      BIGINT        NULL,
  course_id        BIGINT        NULL,
  major_id         BIGINT        NULL,
  original_price   DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '原价',
  condition_level  TINYINT       NOT NULL DEFAULT 3 COMMENT '品相 1-5',
  wear_level       TINYINT       NOT NULL DEFAULT 3 COMMENT '新旧 1-5',
  note_flag        TINYINT       NOT NULL DEFAULT 0 COMMENT '是否含笔记划线',
  cover_img        VARCHAR(255)  NULL,
  images           TEXT          NULL COMMENT 'JSON 数组',
  description      VARCHAR(1000) NULL,
  price            DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '卖家定价',
  suggested_price  DECIMAL(10,2) NULL COMMENT '模型建议价',
  price_low        DECIMAL(10,2) NULL COMMENT '价格区间下界',
  price_high       DECIMAL(10,2) NULL COMMENT '价格区间上界',
  listing_count    INT           NOT NULL DEFAULT 1 COMMENT '同款在售数量',
  sold_count_30d   INT           NOT NULL DEFAULT 0 COMMENT '近30天同款成交数',
  status           TINYINT       NOT NULL DEFAULT 1 COMMENT '0待审核 1在售 2已售 3已下架 4已驳回',
  reject_reason    VARCHAR(255)  NULL,
  view_count       INT           NOT NULL DEFAULT 0,
  favorite_count   INT           NOT NULL DEFAULT 0,
  created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_book_seller (seller_id),
  KEY idx_book_status (status),
  KEY idx_book_course (course_id),
  KEY idx_book_major (major_id),
  KEY idx_book_category (category_id),
  KEY idx_book_price (price),
  KEY idx_book_created (created_at),
  FULLTEXT KEY ft_book_title (title) WITH PARSER ngram
) ENGINE=InnoDB COMMENT='二手书';

CREATE TABLE book_tag (
  book_id  BIGINT NOT NULL,
  tag_id   BIGINT NOT NULL,
  PRIMARY KEY (book_id, tag_id)
) ENGINE=InnoDB COMMENT='书籍标签关联';

CREATE TABLE favorite (
  id         BIGINT   NOT NULL AUTO_INCREMENT,
  user_id    BIGINT   NOT NULL,
  book_id    BIGINT   NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fav_user_book (user_id, book_id),
  KEY idx_fav_book (book_id)
) ENGINE=InnoDB COMMENT='收藏';

CREATE TABLE browse_log (
  id         BIGINT   NOT NULL AUTO_INCREMENT,
  user_id    BIGINT   NOT NULL,
  book_id    BIGINT   NOT NULL,
  duration   INT      NOT NULL DEFAULT 0 COMMENT '停留秒数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_browse_user (user_id),
  KEY idx_browse_book (book_id),
  KEY idx_browse_time (created_at)
) ENGINE=InnoDB COMMENT='浏览记录(隐式反馈)';

-- ------------------------------------------------------------
-- 4. 交易
-- ------------------------------------------------------------
CREATE TABLE book_order (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  order_no     VARCHAR(32)   NOT NULL,
  book_id      BIGINT        NOT NULL,
  buyer_id     BIGINT        NOT NULL,
  seller_id    BIGINT        NOT NULL,
  price        DECIMAL(10,2) NOT NULL,
  status       TINYINT       NOT NULL DEFAULT 0 COMMENT '0待确认 1已预约 2已完成 3已取消 4爽约',
  pickup_place VARCHAR(128)  NULL,
  pickup_time  DATETIME      NULL,
  remark       VARCHAR(255)  NULL,
  created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  finished_at  DATETIME      NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_buyer (buyer_id),
  KEY idx_order_seller (seller_id),
  KEY idx_order_book (book_id),
  KEY idx_order_status (status)
) ENGINE=InnoDB COMMENT='订单';

CREATE TABLE review (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  order_id     BIGINT       NOT NULL,
  book_id      BIGINT       NOT NULL,
  from_user_id BIGINT       NOT NULL,
  to_user_id   BIGINT       NOT NULL,
  rating       TINYINT      NOT NULL DEFAULT 5,
  content      VARCHAR(500) NULL,
  tags         VARCHAR(255) NULL,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_review_order (order_id),
  KEY idx_review_to (to_user_id)
) ENGINE=InnoDB COMMENT='评价';

CREATE TABLE audit_log (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  book_id    BIGINT       NOT NULL,
  admin_id   BIGINT       NOT NULL,
  action     VARCHAR(16)  NOT NULL COMMENT 'approve/reject/offline',
  reason     VARCHAR(255) NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_book (book_id)
) ENGINE=InnoDB COMMENT='审核日志';

-- ------------------------------------------------------------
-- 5. 算法相关
-- ------------------------------------------------------------
CREATE TABLE price_sample (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  title            VARCHAR(200)  NULL,
  category_id      BIGINT        NULL,
  original_price   DECIMAL(10,2) NOT NULL,
  condition_level  TINYINT       NOT NULL,
  wear_level       TINYINT       NOT NULL,
  note_flag        TINYINT       NOT NULL DEFAULT 0,
  publish_year     INT           NOT NULL,
  listing_count    INT           NOT NULL DEFAULT 0 COMMENT '同款在售数量',
  sold_count_30d   INT           NOT NULL DEFAULT 0 COMMENT '近30天成交数',
  seller_history   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '卖家历史成交均价',
  sold_price       DECIMAL(10,2) NOT NULL COMMENT '成交价(目标变量)',
  sold_at          DATETIME      NULL,
  PRIMARY KEY (id),
  KEY idx_sample_cat (category_id)
) ENGINE=InnoDB COMMENT='定价模型训练样本(模拟+采集)';

CREATE TABLE price_record (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  book_id         BIGINT        NULL,
  features        JSON          NULL,
  suggested_price DECIMAL(10,2) NOT NULL,
  price_low       DECIMAL(10,2) NULL,
  price_high      DECIMAL(10,2) NULL,
  model_version   VARCHAR(32)   NULL,
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_price_record_book (book_id)
) ENGINE=InnoDB COMMENT='定价调用留痕';

SET FOREIGN_KEY_CHECKS = 1;
