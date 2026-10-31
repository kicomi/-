package com.campusbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbook.entity.Book;
import com.campusbook.entity.BrowseLog;
import com.campusbook.entity.Favorite;
import com.campusbook.entity.User;
import com.campusbook.mapper.BookMapper;
import com.campusbook.mapper.BrowseLogMapper;
import com.campusbook.mapper.FavoriteMapper;
import com.campusbook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 内容匹配（冷启动兜底 + 匹配度计算）。
 * 用户画像 = 浏览/收藏过的书籍在 分类/课程/专业 上的加权偏好。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final BrowseLogMapper browseLogMapper;
    private final FavoriteMapper favoriteMapper;
    private final BookMapper bookMapper;
    private final UserMapper userMapper;

    /** 用户画像：categoryId -> weight, courseId -> weight, majorId -> weight */
    public Map<String, Map<Long, Double>> profile(Long userId) {
        Map<Long, Double> cat = new HashMap<>();
        Map<Long, Double> course = new HashMap<>();
        Map<Long, Double> major = new HashMap<>();

        List<BrowseLog> logs = browseLogMapper.selectList(new LambdaQueryWrapper<BrowseLog>()
                .eq(BrowseLog::getUserId, userId).orderByDesc(BrowseLog::getId).last("limit 200"));
        List<Favorite> favs = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId));

        Set<Long> ids = new HashSet<>();
        logs.forEach(l -> ids.add(l.getBookId()));
        favs.forEach(f -> ids.add(f.getBookId()));
        if (ids.isEmpty()) return Map.of();

        Map<Long, Book> bookMap = new HashMap<>();
        bookMapper.selectBatchIds(ids).forEach(b -> bookMap.put(b.getId(), b));

        // 浏览权重 1，收藏权重 3
        for (BrowseLog l : logs) {
            Book b = bookMap.get(l.getBookId());
            if (b == null) continue;
            add(cat, b.getCategoryId(), 1.0);
            add(course, b.getCourseId(), 1.0);
            add(major, b.getMajorId(), 0.5);
        }
        for (Favorite f : favs) {
            Book b = bookMap.get(f.getBookId());
            if (b == null) continue;
            add(cat, b.getCategoryId(), 3.0);
            add(course, b.getCourseId(), 3.0);
            add(major, b.getMajorId(), 1.5);
        }

        // 用户自身专业作为弱偏好
        User u = userMapper.selectById(userId);
        if (u != null && u.getMajorId() != null) add(major, u.getMajorId(), 2.0);

        Map<String, Map<Long, Double>> p = new HashMap<>();
        p.put("category", cat);
        p.put("course", course);
        p.put("major", major);
        return p;
    }

    private void add(Map<Long, Double> m, Long k, double v) {
        if (k == null) return;
        m.merge(k, v, Double::sum);
    }

    /** 批量打分，返回 0-99 的匹配度 */
    public Map<Long, Integer> scoreBooks(Long userId, List<Book> books) {
        Map<Long, Integer> result = new HashMap<>();
        if (books == null || books.isEmpty()) return result;
        Map<String, Map<Long, Double>> p = profile(userId);
        if (p.isEmpty()) return result;

        Map<Long, Double> cat = p.getOrDefault("category", Map.of());
        Map<Long, Double> course = p.getOrDefault("course", Map.of());
        Map<Long, Double> major = p.getOrDefault("major", Map.of());

        double maxCat = cat.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double maxCourse = course.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double maxMajor = major.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        int maxView = books.stream().mapToInt(b -> b.getViewCount() == null ? 0 : b.getViewCount()).max().orElse(1);

        for (Book b : books) {
            double s = 0;
            if (b.getCategoryId() != null && cat.containsKey(b.getCategoryId()))
                s += 0.45 * cat.get(b.getCategoryId()) / maxCat;
            if (b.getCourseId() != null && course.containsKey(b.getCourseId()))
                s += 0.35 * course.get(b.getCourseId()) / maxCourse;
            if (b.getMajorId() != null && major.containsKey(b.getMajorId()))
                s += 0.15 * major.get(b.getMajorId()) / maxMajor;
            s += 0.05 * (b.getViewCount() == null ? 0 : b.getViewCount()) / Math.max(1, maxView);
            result.put(b.getId(), (int) Math.round(Math.min(0.99, s) * 100));
        }
        return result;
    }

    public String reason(Long userId, Book b) {
        Map<String, Map<Long, Double>> p = profile(userId);
        if (p.isEmpty()) return "热门推荐";
        if (b.getCourseId() != null && p.getOrDefault("course", Map.of()).containsKey(b.getCourseId()))
            return "同课程教材";
        if (b.getCategoryId() != null && p.getOrDefault("category", Map.of()).containsKey(b.getCategoryId()))
            return "你可能感兴趣的分类";
        if (b.getMajorId() != null && p.getOrDefault("major", Map.of()).containsKey(b.getMajorId()))
            return "同专业在用";
        return "热门推荐";
    }

    /** 基于内容匹配的候选排序（无协同过滤时的兜底） */
    public List<Book> contentTopN(Long userId, List<Book> candidates, int n) {
        Map<Long, Integer> scores = scoreBooks(userId, candidates);
        List<Book> sorted = new ArrayList<>(candidates);
        sorted.sort((a, b) -> Integer.compare(
                scores.getOrDefault(b.getId(), 0), scores.getOrDefault(a.getId(), 0)));
        for (Book b : sorted) {
            Integer s = scores.get(b.getId());
            if (s != null) { b.setMatchScore(s); b.setMatchReason(reason(userId, b)); }
        }
        return diversify(sorted, n);
    }

    /**
     * 多样性重排：同一本书可能有多个卖家在挂，若直接按分数取前 N，
     * 推荐位会被同一本教材的多个副本占满。这里按书名去重，只保留分数最高的那一本。
     * 检索结果不做此处理——买家在搜索结果里本来就想比价。
     */
    public static List<Book> diversify(List<Book> sorted, int n) {
        List<Book> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Book b : sorted) {
            if (out.size() >= n) break;
            String key = (b.getTitle() == null || b.getTitle().isBlank())
                    ? "#" + b.getId()
                    : b.getTitle().trim();
            if (!seen.add(key)) continue;
            out.add(b);
        }
        return out;
    }
}
