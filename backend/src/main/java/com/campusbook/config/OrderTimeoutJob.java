package com.campusbook.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.campusbook.entity.Book;
import com.campusbook.entity.BookOrder;
import com.campusbook.mapper.BookMapper;
import com.campusbook.mapper.BookOrderMapper;
import com.campusbook.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消。
 * 买家下单后卖家一直不确认，订单会永远挂着、书也被锁死，
 * 其他同学想买都买不了。这里每小时扫一次，超过 24 小时未确认的自动释放。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private static final int TIMEOUT_HOURS = 24;

    private final BookOrderMapper orderMapper;
    private final BookMapper bookMapper;
    private final NotificationService notifyService;

    @Scheduled(cron = "0 7 * * * ?")
    @Transactional
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(TIMEOUT_HOURS);
        List<BookOrder> stale = orderMapper.selectList(new LambdaQueryWrapper<BookOrder>()
                .eq(BookOrder::getStatus, 0)
                .lt(BookOrder::getCreatedAt, deadline));

        if (stale.isEmpty()) return;
        log.info("自动取消超时订单 {} 笔", stale.size());

        for (BookOrder o : stale) {
            o.setStatus(3);
            orderMapper.updateById(o);

            // 释放书籍
            bookMapper.update(null, new UpdateWrapper<Book>()
                    .eq("id", o.getBookId()).eq("status", 2).set("status", 1));

            notifyService.push(o.getSellerId(), "order", "预约已超时取消",
                    "订单 " + o.getOrderNo() + " 超过 24 小时未确认，系统已自动取消，书籍重新上架",
                    "order", o.getId());
            notifyService.push(o.getBuyerId(), "order", "预约已超时取消",
                    "订单 " + o.getOrderNo() + " 卖家超过 24 小时未确认，已自动取消，可以看看别的书",
                    "order", o.getId());
        }
    }
}
