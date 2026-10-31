package com.campusbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbook.common.BizException;
import com.campusbook.entity.Book;
import com.campusbook.entity.BookOrder;
import com.campusbook.entity.Report;
import com.campusbook.entity.Review;
import com.campusbook.entity.User;
import com.campusbook.mapper.BookMapper;
import com.campusbook.mapper.BookOrderMapper;
import com.campusbook.mapper.ReportMapper;
import com.campusbook.mapper.ReviewMapper;
import com.campusbook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 管理端「用户详情」。
 * 列表只能看到昵称和状态，管理员要判断一个用户是否可信，
 * 得看他发过什么书、成交过什么、被举报过几次。
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final BookOrderMapper orderMapper;
    private final ReviewMapper reviewMapper;
    private final ReportMapper reportMapper;
    private final UserService userService;

    public Map<String, Object> detail(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BizException(404, "用户不存在");
        userService.fillMajor(u);

        Map<String, Object> out = new LinkedHashMap<>();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", u.getId());
        info.put("nickname", u.getNickname());
        info.put("avatar", u.getAvatar());
        info.put("studentNo", u.getStudentNo());
        info.put("majorName", u.getMajorName());
        info.put("grade", u.getGrade());
        info.put("gender", u.getGender());
        info.put("signature", u.getSignature());
        info.put("phone", u.getPhone());
        info.put("creditScore", u.getCreditScore());
        info.put("role", u.getRole());
        info.put("status", u.getStatus());
        info.put("banUntil", u.getBanUntil());
        info.put("banReason", u.getBanReason());
        info.put("muteStatus", u.getMuteStatus());
        info.put("muteUntil", u.getMuteUntil());
        info.put("muteReason", u.getMuteReason());
        info.put("createdAt", u.getCreatedAt());
        // 实名信息只在这里给管理端
        info.put("realName", u.getRealName());
        info.put("realPhone", u.getRealPhone());
        out.put("user", info);

        long bookCount = bookMapper.selectCount(new LambdaQueryWrapper<Book>().eq(Book::getSellerId, userId));
        long soldCount = orderMapper.selectCount(new LambdaQueryWrapper<BookOrder>()
                .eq(BookOrder::getSellerId, userId).eq(BookOrder::getStatus, 2));
        long buyCount = orderMapper.selectCount(new LambdaQueryWrapper<BookOrder>()
                .eq(BookOrder::getBuyerId, userId).eq(BookOrder::getStatus, 2));
        // 被举报次数 = 作为用户被举报成立 + 其发布的书籍被举报成立
        long reportAsUser = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getTargetType, "user")
                .eq(Report::getTargetId, userId)
                .eq(Report::getStatus, 1));
        long reportAsSeller = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getTargetType, "book")
                .eq(Report::getStatus, 1)
                .inSql(Report::getTargetId, "SELECT id FROM book WHERE seller_id = " + userId));
        long reportAgainst = reportAsUser + reportAsSeller;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("bookCount", bookCount);
        stats.put("soldCount", soldCount);
        stats.put("buyCount", buyCount);
        stats.put("reviewCount", reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getToUserId, userId)));
        stats.put("reportAgainst", reportAgainst);
        out.put("stats", stats);

        // 最近发布的 8 本
        List<Book> books = bookMapper.selectList(new LambdaQueryWrapper<Book>()
                .eq(Book::getSellerId, userId).orderByDesc(Book::getId).last("limit 8"));
        List<Map<String, Object>> bookRows = new ArrayList<>();
        for (Book b : books) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("title", b.getTitle());
            m.put("price", b.getPrice());
            m.put("status", b.getStatus());
            m.put("coverImg", b.getCoverImg());
            m.put("createdAt", b.getCreatedAt());
            bookRows.add(m);
        }
        out.put("books", bookRows);

        // 最近 8 笔订单（买卖都算）
        List<BookOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<BookOrder>()
                .and(w -> w.eq(BookOrder::getBuyerId, userId).or().eq(BookOrder::getSellerId, userId))
                .orderByDesc(BookOrder::getId).last("limit 8"));
        List<Map<String, Object>> orderRows = new ArrayList<>();
        for (BookOrder o : orders) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("price", o.getPrice());
            m.put("status", o.getStatus());
            m.put("role", Objects.equals(o.getBuyerId(), userId) ? "buyer" : "seller");
            m.put("createdAt", o.getCreatedAt());
            Book b = bookMapper.selectById(o.getBookId());
            m.put("bookTitle", b == null ? "书籍已删除" : b.getTitle());
            orderRows.add(m);
        }
        out.put("orders", orderRows);

        // 收到的评价
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getToUserId, userId).orderByDesc(Review::getId).last("limit 5"));
        List<Map<String, Object>> reviewRows = new ArrayList<>();
        for (Review r : reviews) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rating", r.getRating());
            m.put("content", r.getContent());
            m.put("createdAt", r.getCreatedAt());
            Book b = bookMapper.selectById(r.getBookId());
            m.put("bookTitle", b == null ? "书籍已删除" : b.getTitle());
            reviewRows.add(m);
        }
        out.put("reviews", reviewRows);

        return out;
    }
}
