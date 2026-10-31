package com.campusbook.service;

import com.campusbook.entity.Book;
import com.campusbook.mapper.BookMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 供需匹配：ItemCF 协同过滤（Python 服务）为主，内容匹配 + 热门为兜底。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    private final AlgoClient algoClient;
    private final BookMapper bookMapper;
    private final BookService bookService;
    private final MatchService matchService;

    public Map<String, Object> forUser(Long userId, int limit, String scene) {
        int n = Math.max(1, Math.min(limit, 50));
        List<Book> result;
        String model;
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("userId", userId);
            body.put("n", n);
            body.put("scene", scene == null ? "home" : scene);
            Map<String, Object> r = algoClient.post("/recommend", body);
            List<Long> ids = extractIds(r);
            if (ids.isEmpty()) throw new IllegalStateException("返回推荐为空");
            List<Book> books = bookMapper.selectByIdsWithMeta(ids);
            bookService.enrichList(books, userId);
            Map<Long, Integer> order = new HashMap<>();
            for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
            books.sort(Comparator.comparingInt(b -> order.getOrDefault(b.getId(), 999)));
            Map<Long, Integer> scoreMap = extractScores(r);
            books.forEach(b -> {
                Integer s = scoreMap.get(b.getId());
                b.setMatchScore(s == null ? null : Math.min(99, s));
                b.setMatchReason(str(r.get("reason"), "为你推荐"));
            });
            // 同一本教材可能有多个卖家在挂，按书名去重后再返回，避免推荐位重复
            result = MatchService.diversify(books, n);
            model = str(r.get("model"), "ItemCF");
        } catch (Exception e) {
            log.warn("推荐服务不可用，降级为内容匹配: {}", e.getMessage());
            List<Book> hot = bookMapper.selectHot(1, null, null, null, 120);
            result = matchService.contentTopN(userId, hot, n);
            bookService.enrichList(result, userId);
            model = "ContentFallback";
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("model", model);
        out.put("items", result);
        return out;
    }

    public List<Book> similarBooks(Long bookId, int limit) {
        Book current = bookMapper.selectById(bookId);
        if (current == null) return List.of();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("bookId", bookId);
            body.put("n", limit);
            Map<String, Object> r = algoClient.post("/similar", body);
            List<Long> ids = extractIds(r);
            if (!ids.isEmpty()) {
                List<Book> books = bookMapper.selectByIdsWithMeta(ids);
                bookService.enrichList(books, null);
                return books;
            }
        } catch (Exception e) {
            log.warn("相似图书服务不可用，降级为同类热门: {}", e.getMessage());
        }
        return bookService.similar(bookId, limit);
    }

    /** "可能想买的人"：卖家侧的书籍-潜在买家匹配（演示用简化版） */
    public List<Long> potentialBuyers(Long sellerId, int limit) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sellerId", sellerId);
            body.put("n", limit);
            Map<String, Object> r = algoClient.post("/potential-buyers", body);
            Object ids = r.get("userIds");
            if (ids instanceof List<?> l) {
                List<Long> out = new ArrayList<>();
                l.forEach(x -> {
                    if (x instanceof Number num) out.add(num.longValue());
                });
                return out;
            }
        } catch (Exception e) {
            log.debug("潜在买家服务不可用: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractIds(Map<String, Object> r) {
        List<Long> out = new ArrayList<>();
        if (r == null) return out;
        Object items = r.get("items");
        if (items instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    Object id = m.get("bookId");
                    if (id instanceof Number n) out.add(n.longValue());
                } else if (o instanceof Number n) {
                    out.add(n.longValue());
                }
            }
        }
        Object ids = r.get("bookIds");
        if (out.isEmpty() && ids instanceof List<?> list) {
            list.forEach(x -> {
                if (x instanceof Number n) out.add(n.longValue());
            });
        }
        return out;
    }

    private Map<Long, Integer> extractScores(Map<String, Object> r) {
        Map<Long, Integer> m = new HashMap<>();
        if (r == null) return m;
        Object items = r.get("items");
        if (items instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> mm) {
                    Object id = mm.get("bookId");
                    Object sc = mm.get("score");
                    if (id instanceof Number n && sc instanceof Number s) {
                        m.put(n.longValue(), (int) Math.round(s.doubleValue() * 100));
                    }
                }
            }
        }
        return m;
    }

    private String str(Object o, String def) { return o == null ? def : String.valueOf(o); }
}
