package com.campusbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusbook.common.BizException;
import com.campusbook.common.PageResult;
import com.campusbook.entity.Book;
import com.campusbook.entity.Report;
import com.campusbook.entity.User;
import com.campusbook.mapper.BookMapper;
import com.campusbook.mapper.ReportMapper;
import com.campusbook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final com.campusbook.mapper.BookOrderMapper orderMapper;
    private final com.campusbook.mapper.ChatMessageMapper chatMapper;
    private final com.campusbook.mapper.ReviewMapper reviewMapper;
    private final NotificationService notifyService;

    private static final Map<Integer, String> STATUS_TEXT = Map.of(0, "待处理", 1, "已处理", 2, "已驳回");

    /** 同一用户对同一对象只允许一条待处理的举报，避免刷屏 */
    @Transactional
    public Report create(Long reporterId, String targetType, Long targetId, String reason, String detail) {
        if (targetId == null) throw new BizException("缺少举报对象");
        if (reason == null || reason.isBlank()) throw new BizException("请选择举报原因");
        if (!"book".equals(targetType) && !"user".equals(targetType) && !"order".equals(targetType)) {
            throw new BizException("举报类型不正确");
        }
        if ("user".equals(targetType) && targetId.equals(reporterId)) {
            throw new BizException("不能举报自己");
        }

        long dup = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, reporterId)
                .eq(Report::getTargetType, targetType)
                .eq(Report::getTargetId, targetId)
                .eq(Report::getStatus, 0));
        if (dup > 0) throw new BizException("你已经举报过了，管理员正在处理");

        Report r = new Report();
        r.setReporterId(reporterId);
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        r.setReason(reason);
        r.setDetail(detail);
        r.setStatus(0);
        reportMapper.insert(r);
        return r;
    }

    public PageResult<Report> page(Integer status, int page, int size) {
        Page<Report> p = new Page<>(page, size);
        LambdaQueryWrapper<Report> w = new LambdaQueryWrapper<>();
        if (status != null) w.eq(Report::getStatus, status);
        w.orderByAsc(Report::getStatus).orderByDesc(Report::getId);

        IPage<Report> result = reportMapper.selectPage(p, w);
        fill(result.getRecords());
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    private void fill(List<Report> list) {
        if (list.isEmpty()) return;

        Set<Long> userIds = list.stream().map(Report::getReporterId).collect(Collectors.toSet());
        Map<Long, String> names = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getNickname() == null ? "同学" : u.getNickname()));

        Set<Long> bookIds = list.stream()
                .filter(r -> "book".equals(r.getTargetType()))
                .map(Report::getTargetId)
                .collect(Collectors.toSet());
        Map<Long, String> titles = new HashMap<>();
        if (!bookIds.isEmpty()) {
            for (Book b : bookMapper.selectBatchIds(bookIds)) titles.put(b.getId(), b.getTitle());
        }

        for (Report r : list) {
            r.setReporterNickname(names.getOrDefault(r.getReporterId(), "同学"));
            r.setStatusText(STATUS_TEXT.getOrDefault(r.getStatus(), "未知"));
            if ("order".equals(r.getTargetType())) {
                var o = orderMapper.selectById(r.getTargetId());
                r.setTargetTitle(o == null ? "订单已删除" : "订单 " + o.getOrderNo());
            } else if ("book".equals(r.getTargetType())) {
                r.setTargetTitle(titles.getOrDefault(r.getTargetId(), "书籍已删除"));
            } else {
                User u = userMapper.selectById(r.getTargetId());
                r.setTargetTitle(u == null ? "用户已注销" : u.getNickname());
            }
        }
    }

    /**
     * 举报详情。管理员光看"原因 + 说明"没法判断真假，
     * 这里把能核实的证据一起捞出来：这本书长什么样、举报人到底买没买过、
     * 双方各自的历史举报记录。
     */
    public Map<String, Object> detail(Long id) {
        Report r = reportMapper.selectById(id);
        if (r == null) throw new BizException(404, "举报不存在");

        User reporter = userMapper.selectById(r.getReporterId());
        Map<String, Object> out = new LinkedHashMap<>();

        Map<String, Object> rep = new LinkedHashMap<>();
        rep.put("id", r.getId());
        rep.put("targetType", r.getTargetType());
        rep.put("reason", r.getReason());
        rep.put("detail", r.getDetail());
        rep.put("status", r.getStatus());
        rep.put("statusText", STATUS_TEXT.getOrDefault(r.getStatus(), "未知"));
        rep.put("handleNote", r.getHandleNote());
        rep.put("createdAt", r.getCreatedAt());
        rep.put("handledAt", r.getHandledAt());

        Map<String, Object> reporterInfo = new LinkedHashMap<>();
        reporterInfo.put("id", r.getReporterId());
        reporterInfo.put("nickname", reporter == null ? "已注销" : reporter.getNickname());
        reporterInfo.put("creditScore", reporter == null ? null : reporter.getCreditScore());
        reporterInfo.put("reportCount", reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, r.getReporterId())));
        reporterInfo.put("validCount", reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, r.getReporterId())
                .eq(Report::getStatus, 1)));
        rep.put("reporter", reporterInfo);
        out.put("report", rep);

        if ("book".equals(r.getTargetType())) {
            Book b = bookMapper.selectById(r.getTargetId());
            if (b != null) {
                Map<String, Object> book = new LinkedHashMap<>();
                book.put("id", b.getId());
                book.put("title", b.getTitle());
                book.put("author", b.getAuthor());
                book.put("coverImg", b.getCoverImg());
                book.put("images", b.getImages());
                book.put("price", b.getPrice());
                book.put("originalPrice", b.getOriginalPrice());
                book.put("conditionLevel", b.getConditionLevel());
                book.put("wearLevel", b.getWearLevel());
                book.put("noteFlag", b.getNoteFlag());
                book.put("description", b.getDescription());
                book.put("status", b.getStatus());
                book.put("createdAt", b.getCreatedAt());
                book.put("sellerId", b.getSellerId());
                out.put("book", book);

                User seller = userMapper.selectById(b.getSellerId());
                Map<String, Object> sellerInfo = new LinkedHashMap<>();
                sellerInfo.put("id", b.getSellerId());
                sellerInfo.put("nickname", seller == null ? "已注销" : seller.getNickname());
                sellerInfo.put("creditScore", seller == null ? null : seller.getCreditScore());
                sellerInfo.put("soldCount", orderMapper.selectCount(new LambdaQueryWrapper<com.campusbook.entity.BookOrder>()
                        .eq(com.campusbook.entity.BookOrder::getSellerId, b.getSellerId())
                        .eq(com.campusbook.entity.BookOrder::getStatus, 2)));
                sellerInfo.put("reportedCount", reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                        .eq(Report::getTargetType, "book")
                        .eq(Report::getStatus, 1)
                        .inSql(Report::getTargetId, "SELECT id FROM book WHERE seller_id = " + b.getSellerId())));
                out.put("seller", sellerInfo);

                // 最关键的一条证据：举报人到底有没有真的买过这本书
                com.campusbook.entity.BookOrder order = orderMapper.selectOne(
                        new LambdaQueryWrapper<com.campusbook.entity.BookOrder>()
                                .eq(com.campusbook.entity.BookOrder::getBookId, b.getId())
                                .eq(com.campusbook.entity.BookOrder::getBuyerId, r.getReporterId())
                                .orderByDesc(com.campusbook.entity.BookOrder::getId)
                                .last("limit 1"));

                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("hasOrder", order != null);
                evidence.put("orderStatus", order == null ? null : order.getStatus());
                evidence.put("orderTime", order == null ? null : order.getCreatedAt());
                evidence.put("chatted", chatMapper.selectCount(new LambdaQueryWrapper<com.campusbook.entity.ChatMessage>()
                        .and(w -> w.eq(com.campusbook.entity.ChatMessage::getFromUserId, r.getReporterId())
                                .eq(com.campusbook.entity.ChatMessage::getToUserId, b.getSellerId()))
                        .or(w -> w.eq(com.campusbook.entity.ChatMessage::getFromUserId, b.getSellerId())
                                .eq(com.campusbook.entity.ChatMessage::getToUserId, r.getReporterId()))) > 0);
                out.put("evidence", evidence);

                List<Map<String, Object>> reviews = new ArrayList<>();
                List<com.campusbook.entity.Review> rs2 = reviewMapper.selectList(
                        new LambdaQueryWrapper<com.campusbook.entity.Review>()
                                .eq(com.campusbook.entity.Review::getBookId, b.getId())
                                .orderByDesc(com.campusbook.entity.Review::getId)
                                .last("limit 3"));
                for (com.campusbook.entity.Review rv : rs2) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("rating", rv.getRating());
                    m.put("content", rv.getContent());
                    reviews.add(m);
                }
                out.put("reviews", reviews);
            }
        }
        return out;
    }

    /** 管理员处理：通过（1）或驳回（2），可附处理说明 */
    @Transactional
    public Report handle(Long adminId, Long id, Integer status, String note) {
        Report r = reportMapper.selectById(id);
        if (r == null) throw new BizException(404, "举报不存在");
        if (status == null || (status != 1 && status != 2)) throw new BizException("处理结果不正确");

        r.setStatus(status);
        r.setHandleNote(note);
        r.setHandlerId(adminId);
        r.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(r);

        // 举报成立时把书下架，形成闭环
        // 把处理结果告诉举报人，形成闭环
        notifyService.push(r.getReporterId(), "report", "举报已处理",
                "你举报的《" + (reportTargetTitle(r)) + "》已" + (status == 1 ? "核实处理" : "驳回")
                        + (note == null || note.isBlank() ? "" : "：" + note),
                "book", r.getTargetId());

        if (status == 1) {
            applyPenalty(r);
        }
        fill(List.of(r));
        return r;
    }

    /** 举报成立时的处罚：书籍下架 + 卖家扣信用分，并通知当事人 */
    private void applyPenalty(Report r) {
        if ("book".equals(r.getTargetType())) {
            Book b = bookMapper.selectById(r.getTargetId());
            if (b == null) return;

            if (b.getStatus() != null && b.getStatus() != 3) {
                b.setStatus(3);
                b.setPriority(0);
                bookMapper.updateById(b);
            }
            deductCredit(b.getSellerId(), "你发布的《" + b.getTitle() + "》因举报成立被下架",
                    penaltyOf(r.getReason()));
        } else if ("user".equals(r.getTargetType())) {
            deductCredit(r.getTargetId(), "你的账号因举报成立被扣分", penaltyOf(r.getReason()));
        }
    }

    /**
     * 按举报原因分级扣分。诈骗最重、价格异常最轻，
     * 用固定值一刀切会让"标错价"和"骗钱"受同样的处罚。
     */
    private static final java.util.Map<String, Integer> PENALTY_BY_REASON = java.util.Map.of(
            "涉嫌诈骗", 20,
            "内容违规", 15,
            "疑似虚假信息", 10,
            "交易纠纷", 8,
            "书况与描述不符", 5,
            "价格异常", 3
    );
    private static final int DEFAULT_PENALTY = 5;

    /** 处理前先算出这条举报会扣多少分，管理端弹窗要用 */
    public int penaltyOf(String reason) {
        return PENALTY_BY_REASON.getOrDefault(reason, DEFAULT_PENALTY);
    }

    private void deductCredit(Long userId, String reason, int penalty) {
        if (userId == null) return;
        User u = userMapper.selectById(userId);
        if (u == null || "admin".equals(u.getRole())) return;

        int old = u.getCreditScore() == null ? 100 : u.getCreditScore();
        int now = Math.max(0, old - penalty);
        if (now == old) return;

        u.setCreditScore(now);
        userMapper.updateById(u);

        notifyService.push(userId, "system", "信用分已扣减",
                reason + "，信用分 " + old + " → " + now + "（-" + penalty + "）。"
                        + "请在发布时如实描述书况，多次违规会被限制发布。",
                null, null);
    }

    private String reportTargetTitle(Report r) {
        if ("order".equals(r.getTargetType())) {
            var o = orderMapper.selectById(r.getTargetId());
            return o == null ? "该订单" : "订单 " + o.getOrderNo();
        }
        if ("book".equals(r.getTargetType())) {
            Book b = bookMapper.selectById(r.getTargetId());
            return b == null ? "该书籍" : b.getTitle();
        }
        User u = userMapper.selectById(r.getTargetId());
        return u == null ? "该用户" : u.getNickname();
    }

    public long pendingCount() {
        return reportMapper.selectCount(new LambdaQueryWrapper<Report>().eq(Report::getStatus, 0));
    }
}
