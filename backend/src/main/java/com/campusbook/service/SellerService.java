package com.campusbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbook.entity.Book;
import com.campusbook.entity.BookOrder;
import com.campusbook.entity.Review;
import com.campusbook.entity.User;
import com.campusbook.mapper.BookMapper;
import com.campusbook.mapper.BookOrderMapper;
import com.campusbook.mapper.ReviewMapper;
import com.campusbook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 卖家画像。详情页要让买家判断"这个人靠不靠谱"，
 * 光有昵称和专业不够，得有成交量和评价。
 */
@Service
@RequiredArgsConstructor
public class SellerService {

    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final BookOrderMapper orderMapper;
    private final ReviewMapper reviewMapper;
    private final UserService userService;

    public Map<String, Object> profile(Long sellerId) {
        User u = userMapper.selectById(sellerId);
        Map<String, Object> out = new LinkedHashMap<>();
        if (u == null) {
            out.put("nickname", "已注销用户");
            return out;
        }

        userService.fillMajor(u);

        long soldCount = orderMapper.selectCount(new LambdaQueryWrapper<BookOrder>()
                .eq(BookOrder::getSellerId, sellerId)
                .eq(BookOrder::getStatus, 2));

        long onSaleCount = bookMapper.selectCount(new LambdaQueryWrapper<Book>()
                .eq(Book::getSellerId, sellerId)
                .eq(Book::getStatus, 1));

        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getToUserId, sellerId)
                .orderByDesc(Review::getId));

        int reviewCount = reviews.size();
        double ratingAvg = 0;
        int goodCount = 0;
        if (reviewCount > 0) {
            int sum = 0;
            for (Review r : reviews) {
                int rating = r.getRating() == null ? 5 : r.getRating();
                sum += rating;
                if (rating >= 4) goodCount++;
            }
            ratingAvg = Math.round(sum * 10.0 / reviewCount) / 10.0;
        }

        out.put("userId", sellerId);
        out.put("nickname", u.getNickname());
        out.put("avatar", u.getAvatar());
        out.put("majorName", u.getMajorName());
        out.put("grade", u.getGrade());
        out.put("gender", u.getGender());
        out.put("creditScore", u.getCreditScore());
        out.put("soldCount", soldCount);
        out.put("onSaleCount", onSaleCount);
        out.put("reviewCount", reviewCount);
        out.put("ratingAvg", reviewCount == 0 ? 5.0 : ratingAvg);
        // 没有评价时按 100% 展示，避免新卖家显示成 0%
        out.put("goodRate", reviewCount == 0 ? 100 : (int) Math.round(goodCount * 100.0 / reviewCount));
        out.put("recentReviews", recentReviews(reviews, 3));
        return out;
    }

    /** 最近几条评价，附上被评价的那本书的书名 */
    private List<Map<String, Object>> recentReviews(List<Review> reviews, int limit) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (reviews.isEmpty()) return out;

        Set<Long> bookIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (Review r : reviews) {
            if (r.getBookId() != null) bookIds.add(r.getBookId());
            if (r.getFromUserId() != null) userIds.add(r.getFromUserId());
        }

        Map<Long, String> bookTitles = new HashMap<>();
        if (!bookIds.isEmpty()) {
            for (Book b : bookMapper.selectBatchIds(bookIds)) bookTitles.put(b.getId(), b.getTitle());
        }
        Map<Long, String> names = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(userIds)) names.put(u.getId(), u.getNickname());
        }

        for (Review r : reviews.subList(0, Math.min(limit, reviews.size()))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rating", r.getRating());
            m.put("content", r.getContent());
            m.put("images", r.getImages());
            m.put("fromNickname", names.getOrDefault(r.getFromUserId(), "同学"));
            m.put("bookTitle", bookTitles.get(r.getBookId()));
            m.put("createdAt", r.getCreatedAt());
            out.add(m);
        }
        return out;
    }
}
