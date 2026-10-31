USE campus_book;

-- 订单确认时刻。原来只有 created_at / updated_at / finished_at，
-- 已完成订单的 updated_at 会被完成动作覆盖，时间轴上"确认"节点就没时间可显示
ALTER TABLE book_order
  ADD COLUMN confirmed_at DATETIME NULL COMMENT '卖家确认预约的时间';

-- 回填历史数据：已完成或已确认的订单，按创建后 5~40 分钟估一个确认时刻
UPDATE book_order
   SET confirmed_at = DATE_ADD(created_at, INTERVAL (5 + (id * 7) % 35) MINUTE)
 WHERE status IN (1, 2) AND confirmed_at IS NULL;
