package com.campusbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusbook.common.BizException;
import com.campusbook.common.PageResult;
import com.campusbook.dto.OrderCreateReq;
import com.campusbook.entity.Book;
import com.campusbook.entity.BookOrder;
import com.campusbook.entity.Review;
import com.campusbook.entity.User;
import com.campusbook.mapper.BookMapper;
import com.campusbook.mapper.BookOrderMapper;
import com.campusbook.mapper.ReviewMapper;
import com.campusbook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final SecureRandom RND = new SecureRandom();
    private static final String[] STATUS_TEXT = {"待确认", "已预约", "已完成", "已取消", "爽约"};

    private final BookOrderMapper orderMapper;
    private final BookMapper bookMapper;
    private final NotificationService notifyService;
    private final UserMapper userMapper;
    private final ReviewMapper reviewMapper;

    @Transactional
    public BookOrder create(Long buyerId, OrderCreateReq req) {
        Book b = bookMapper.selectById(req.getBookId());
        if (b == null) throw new BizException(404, "书籍不存在");
        if (b.getStatus() == null || b.getStatus() != 1) throw new BizException("该书已售出或已下架");
        if (Objects.equals(b.getSellerId(), buyerId)) throw new BizException("不能购买自己发布的书籍");

        // 先做一次快速判断（省掉大多数无谓的更新）
        Long active = orderMapper.selectCount(new LambdaQueryWrapper<BookOrder>()
                .eq(BookOrder::getBookId, req.getBookId())
                .in(BookOrder::getStatus, 0, 1));
        if (active != null && active > 0) throw new BizException("该书已被其他同学预订");

        // 关键：用带条件的 UPDATE 原子地把书锁住。
        // 两个买家同时下单时，上面的 count 都会读到 0，
        // 只有真正把 status 从 1 改成 2 的那一个才能拿到行数 1。
        int claimed = bookMapper.update(null, new UpdateWrapper<Book>()
                .eq("id", b.getId())
                .eq("status", 1)
                .set("status", 2));
        if (claimed == 0) throw new BizException("该书已被其他同学预订");

        BookOrder o = new BookOrder();
        o.setOrderNo(genOrderNo());
        o.setBookId(b.getId());
        o.setBuyerId(buyerId);
        o.setSellerId(b.getSellerId());
        o.setPrice(b.getPrice());
        o.setStatus(0);
        o.setPickupPlace(req.getPickupPlace());
        o.setPickupTime(req.getPickupTime());
        o.setRemark(req.getRemark());
        orderMapper.insert(o);

        log.info("创建订单 {} bookId={} buyer={}", o.getOrderNo(), b.getId(), buyerId);

        notifyService.push(o.getSellerId(), "order", "有新订单待确认",
                "有同学预约了《" + b.getTitle() + "》，请尽快确认交接时间",
                "order", o.getId());
        return fill(o);
    }

    @Transactional
    public BookOrder confirm(Long userId, Long orderId) {
        BookOrder o = mustGet(orderId);
        if (!Objects.equals(o.getSellerId(), userId)) throw new BizException(403, "只有卖家可以确认");
        if (o.getStatus() != 0) throw new BizException("当前状态不可确认");
        o.setStatus(1);
        // 记下确认时刻，订单时间轴要靠它渲染"卖家确认"节点
        o.setConfirmedAt(LocalDateTime.now());
        orderMapper.updateById(o);

        notifyService.push(o.getBuyerId(), "order", "卖家已确认预约",
                "订单 " + o.getOrderNo() + " 已确认，请按约定时间地点交接",
                "order", o.getId());
        return fill(o);
    }

    @Transactional
    public BookOrder complete(Long userId, Long orderId) {
        BookOrder o = mustGet(orderId);
        if (!Objects.equals(o.getBuyerId(), userId) && !Objects.equals(o.getSellerId(), userId))
            throw new BizException(403, "无权操作该订单");
        if (o.getStatus() != 1) throw new BizException("请先由卖家确认预约");
        o.setStatus(2);
        o.setFinishedAt(LocalDateTime.now());
        // 兼容老数据：没走过 confirm 的订单直接完成时补一个确认时间
        if (o.getConfirmedAt() == null) o.setConfirmedAt(o.getFinishedAt());
        orderMapper.updateById(o);
        addCredit(o.getBuyerId(), 1);
        addCredit(o.getSellerId(), 2);

        notifyService.push(o.getBuyerId(), "order", "交易已完成",
                "订单 " + o.getOrderNo() + " 已完成，别忘了给卖家一个评价", "order", o.getId());
        notifyService.push(o.getSellerId(), "order", "交易已完成",
                "订单 " + o.getOrderNo() + " 已完成，信用分 +2", "order", o.getId());
        return fill(o);
    }

    @Transactional
    public BookOrder cancel(Long userId, Long orderId) {
        BookOrder o = mustGet(orderId);
        if (!Objects.equals(o.getBuyerId(), userId) && !Objects.equals(o.getSellerId(), userId))
            throw new BizException(403, "无权操作该订单");
        if (o.getStatus() == 2) throw new BizException("已完成的订单不能取消");
        if (o.getStatus() == 3) throw new BizException("订单已取消");
        o.setStatus(3);
        orderMapper.updateById(o);

        Book b = bookMapper.selectById(o.getBookId());
        if (b != null && (b.getStatus() == null || b.getStatus() == 2)) {
            b.setStatus(1);
            bookMapper.updateById(b);
        }
        if (o.getStatus() != null && o.getStatus() >= 0) addCredit(userId, -2);

        // 通知另一方，避免对方还在等
        Long otherId = Objects.equals(userId, o.getBuyerId()) ? o.getSellerId() : o.getBuyerId();
        notifyService.push(otherId, "order", "订单已被取消",
                "订单 " + o.getOrderNo() + " 已取消，书籍重新上架", "order", o.getId());
        return fill(o);
    }

    public PageResult<BookOrder> list(Long userId, String role, Integer status, int pageNo, int size) {
        Page<BookOrder> page = new Page<>(Math.max(1, pageNo), Math.max(1, Math.min(size, 50)));
        Long buyer = "buyer".equals(role) ? userId : null;
        Long seller = "seller".equals(role) ? userId : null;
        IPage<BookOrder> r = orderMapper.pageOrders(page, buyer, seller, status);
        r.getRecords().forEach(this::fillStatusAndReview);
        return PageResult.of(r.getTotal(), pageNo, size, r.getRecords());
    }

    public PageResult<BookOrder> listAll(Integer status, int pageNo, int size) {
        Page<BookOrder> page = new Page<>(Math.max(1, pageNo), Math.max(1, Math.min(size, 50)));
        IPage<BookOrder> r = orderMapper.pageOrders(page, null, null, status);
        r.getRecords().forEach(this::fillStatusAndReview);
        return PageResult.of(r.getTotal(), pageNo, size, r.getRecords());
    }

    public BookOrder get(Long id) {
        BookOrder o = orderMapper.selectById(id);
        if (o == null) throw new BizException(404, "订单不存在");
        return fill(o);
    }

    private BookOrder mustGet(Long id) {
        BookOrder o = orderMapper.selectById(id);
        if (o == null) throw new BizException(404, "订单不存在");
        return o;
    }

    private void addCredit(Long uid, int delta) {
        if (uid == null) return;
        User u = userMapper.selectById(uid);
        if (u == null) return;
        int s = (u.getCreditScore() == null ? 100 : u.getCreditScore()) + delta;
        u.setCreditScore(Math.max(0, Math.min(100, s)));
        userMapper.updateById(u);
    }

    private void fillStatusAndReview(BookOrder o) {
        o.setStatusText(STATUS_TEXT[Math.min(o.getStatus() == null ? 0 : o.getStatus(), 4)]);

        // 待确认的订单 24 小时后会被定时任务取消，这里算好剩余时间给前端倒计时
        if (o.getStatus() != null && o.getStatus() == 0 && o.getCreatedAt() != null) {
            LocalDateTime deadline = o.getCreatedAt().plusHours(24);
            o.setExpireAt(deadline);
            o.setRemainHours(java.time.Duration.between(LocalDateTime.now(), deadline).toHours());
        }
        Review rv = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, o.getId()).last("limit 1"));
        o.setReviewed(rv != null);
        if (rv != null) {
            o.setReviewId(rv.getId());
            o.setReviewRating(rv.getRating());
            o.setReviewContent(rv.getContent());
            o.setReviewImages(rv.getImages());
        }
    }

    private BookOrder fill(BookOrder o) {
        Book b = bookMapper.selectById(o.getBookId());
        if (b != null) {
            o.setBookTitle(b.getTitle());
            o.setBookCover(b.getCoverImg());
        }
        User buyer = userMapper.selectById(o.getBuyerId());
        User seller = userMapper.selectById(o.getSellerId());
        if (buyer != null) o.setBuyerNickname(buyer.getNickname());
        if (seller != null) o.setSellerNickname(seller.getNickname());
        fillStatusAndReview(o);
        return o;
    }

    /**
     * 订单号：CB + 下单日期 + 5 位随机数。
     * 不把时分秒拼进去，一是和种子数据格式保持一致（否则表格里长短不一），
     * 二是演示时顾客看到一长串数字也不好看。5 位随机数在同一天内足够区分。
     */
    private String genOrderNo() {
        return "CB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%05d", RND.nextInt(100000));
    }
}
