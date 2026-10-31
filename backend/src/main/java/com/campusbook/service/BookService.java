package com.campusbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusbook.common.BizException;
import com.campusbook.common.PageResult;
import com.campusbook.dto.BookPublishReq;
import com.campusbook.dto.BookQuery;
import com.campusbook.entity.*;
import com.campusbook.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookMapper bookMapper;
    private final TagMapper tagMapper;
    private final FavoriteMapper favoriteMapper;
    private final BrowseLogMapper browseLogMapper;
    private final AuditLogMapper auditLogMapper;
    private final MatchService matchService;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;

    // ------------------------------------------------------------ 检索

    public PageResult<Book> search(BookQuery q, Long currentUserId) {
        int pageNo = q.getPage() == null || q.getPage() < 1 ? 1 : q.getPage();
        int size = q.getSize() == null || q.getSize() < 1 ? 10 : Math.min(q.getSize(), 50);
        Page<Book> page = new Page<>(pageNo, size);

        IPage<Book> r = bookMapper.searchPage(page,
                q.getStatus(), q.getKeyword(), q.getCategoryId(), q.getMajorId(),
                q.getCourseId(), q.getSellerId(), null,
                q.getPriceMin(), q.getPriceMax(), q.getConditionMin(), q.getNoteFlag(),
                q.getSort());

        List<Book> list = r.getRecords();
        enrichList(list, currentUserId);

        // 关键词/分类检索时计算"匹配度"，用于图 4 的匹配度排序展示
        if (currentUserId != null) {
            Map<Long, Integer> scores = matchService.scoreBooks(currentUserId, list);
            list.forEach(b -> {
                Integer s = scores.get(b.getId());
                if (s != null) {
                    b.setMatchScore(s);
                    b.setMatchReason(matchService.reason(currentUserId, b));
                }
            });
        }
        return PageResult.of(r.getTotal(), pageNo, size, list);
    }

    public PageResult<Book> pageForAdmin(Integer status, String keyword, int pageNo, int size) {
        Page<Book> page = new Page<>(pageNo, size);
        IPage<Book> r = bookMapper.searchPage(page, status, keyword, null, null, null, null, null,
                null, null, null, null, "newest");
        enrichList(r.getRecords(), null);
        return PageResult.of(r.getTotal(), pageNo, size, r.getRecords());
    }

    // ------------------------------------------------------------ 详情

    @Transactional
    public Book detail(Long id, Long currentUserId) {
        Book b = bookMapper.selectById(id);
        if (b == null) throw new BizException(404, "书籍不存在");
        bookMapper.update(null, new UpdateWrapper<Book>()
                .eq("id", id).setSql("view_count = view_count + 1"));
        b.setViewCount((b.getViewCount() == null ? 0 : b.getViewCount()) + 1);

        // selectById 不会走联表，卖家/课程/专业这些展示字段要单独补齐，
        // 否则详情页卖家一片空白
        List<Book> withMeta = bookMapper.selectByIdsWithMeta(List.of(id));
        if (!withMeta.isEmpty()) {
            Book m = withMeta.get(0);
            b.setSellerNickname(m.getSellerNickname());
            b.setSellerAvatar(m.getSellerAvatar());
            b.setCourseName(m.getCourseName());
            b.setMajorName(m.getMajorName());
            b.setCategoryName(m.getCategoryName());
            b.setStatus(m.getStatus());
            b.setListingCount(m.getListingCount());
            b.setSoldCount30d(m.getSoldCount30d());
        }
        enrichList(List.of(b), currentUserId);

        if (currentUserId != null) {
            BrowseLog log = new BrowseLog();
            log.setUserId(currentUserId);
            log.setBookId(id);
            log.setDuration(30);
            browseLogMapper.insert(log);
        }
        return b;
    }

    /**
     * 最近浏览：同一本书只保留最后一次，按时间倒序。
     * 浏览记录本来就在写（推荐在用），顺手给用户一个入口。
     */
    public List<Book> browseHistory(Long userId, int limit) {
        List<BrowseLog> logs = browseLogMapper.selectList(new LambdaQueryWrapper<BrowseLog>()
                .eq(BrowseLog::getUserId, userId)
                .orderByDesc(BrowseLog::getId)
                .last("limit " + Math.min(limit * 4, 80)));

        List<Long> ids = new ArrayList<>();
        for (BrowseLog l : logs) {
            if (!ids.contains(l.getBookId())) ids.add(l.getBookId());
            if (ids.size() >= limit) break;
        }
        if (ids.isEmpty()) return List.of();

        List<Book> list = bookMapper.selectByIdsWithMeta(ids);
        Map<Long, Book> map = list.stream().collect(Collectors.toMap(Book::getId, b -> b));
        List<Book> ordered = new ArrayList<>();
        for (Long id : ids) {
            Book b = map.get(id);
            if (b != null) ordered.add(b);
        }
        enrichList(ordered, userId);
        return ordered;
    }

    /**
     * 搜索热词：按在售书籍的课程聚合，取前 8。
     * 校园场景下"课程名"比通用热搜词更有意义，学生就是按课找书。
     */
    public List<Map<String, Object>> hotKeywords(int limit) {
        List<Book> books = bookMapper.selectList(new LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, 1)
                .isNotNull(Book::getCourseId)
                .select(Book::getCourseId));

        Map<Long, Long> counts = books.stream()
                .collect(Collectors.groupingBy(Book::getCourseId, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("keyword", courseName(e.getKey()));
                    m.put("count", e.getValue());
                    return m;
                })
                .filter(m -> m.get("keyword") != null)
                .collect(Collectors.toList());
    }

    private String courseName(Long courseId) {
        if (courseId == null) return null;
        Course c = courseMapper.selectById(courseId);
        return c == null ? null : c.getName();
    }

    public List<Book> similar(Long bookId, int limit) {
        Book b = bookMapper.selectById(bookId);
        if (b == null) return List.of();
        List<Book> sameCourse = bookMapper.selectHot(1, null, null, bookId, limit);
        if (b.getCategoryId() != null) {
            sameCourse = bookMapper.selectHot(1, b.getCategoryId(), null, bookId, limit);
        }
        enrichList(sameCourse, null);
        return sameCourse;
    }

    // ------------------------------------------------------------ 发布 / 编辑

    @Transactional
    /**
     * 发布前把明显不合理的数据挡掉。
     * 之前售价 2000、原价 50 也能发出来，二手价比新书贵 40 倍，
     * 这种数据一旦进了库，定价模型和推荐都会被带偏。
     */
    private void validatePublish(BookPublishReq req) {
        if (req.getTitle() == null || req.getTitle().isBlank()) throw new BizException("请填写书名");

        java.math.BigDecimal orig = req.getOriginalPrice();
        java.math.BigDecimal price = req.getPrice();

        if (orig == null || orig.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BizException("原价必须大于 0");
        }
        if (orig.compareTo(new java.math.BigDecimal("10000")) > 0) {
            throw new BizException("原价看起来不太合理，请确认");
        }
        if (price == null || price.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BizException("售价必须大于 0");
        }
        if (price.compareTo(orig) > 0) {
            throw new BizException("二手售价不能高于原价");
        }
        if (req.getConditionLevel() != null && (req.getConditionLevel() < 1 || req.getConditionLevel() > 5)) {
            throw new BizException("品相等级只能是 1-5");
        }
        if (req.getWearLevel() != null && (req.getWearLevel() < 1 || req.getWearLevel() > 5)) {
            throw new BizException("新旧程度只能是 1-5");
        }
        if (req.getPublishYear() != null && (req.getPublishYear() < 1950 || req.getPublishYear() > 2100)) {
            throw new BizException("出版年份不合理");
        }
        if (req.getDescription() != null && req.getDescription().length() > 500) {
            throw new BizException("描述不能超过 500 字");
        }
    }

    /** 高信用阈值：>=90 走"先上架 + 首页优先"，低于此线进待审核队列 */
    public static final int TRUSTED_SCORE = 90;
    /** 低于这个分数禁止发布 */
    public static final int LOW_CREDIT_BLOCK = 60;

    public Book publish(Long sellerId, BookPublishReq req) {
        validatePublish(req);
        Book b = new Book();
        copyReq(b, req);
        b.setSellerId(sellerId);

        // 信用分决定待遇：高分卖家免审核并优先展示，低分卖家先审后上
        User seller = userMapper.selectById(sellerId);
        int credit = seller == null || seller.getCreditScore() == null ? 100 : seller.getCreditScore();

        // 信用过低直接不让发，和信用分说明页写的一致
        if (credit < LOW_CREDIT_BLOCK) {
            throw new BizException("信用分低于 " + LOW_CREDIT_BLOCK + " 分，暂时无法发布新书，请先完成交易积累信用");
        }
        boolean trusted = credit >= TRUSTED_SCORE;
        b.setStatus(trusted ? 1 : 0);
        b.setPriority(trusted ? 1 : 0);
        b.setRejectReason(null);
        b.setViewCount(0);
        b.setFavoriteCount(0);
        bookMapper.insert(b);
        return b;
    }

    @Transactional
    public Book updateBook(Long userId, Long bookId, BookPublishReq req) {
        Book b = bookMapper.selectById(bookId);
        if (b == null) throw new BizException(404, "书籍不存在");
        if (!Objects.equals(b.getSellerId(), userId)) throw new BizException(403, "只能编辑自己发布的书籍");
        validatePublish(req);
        copyReq(b, req);
        bookMapper.updateById(b);
        return b;
    }

    private void copyReq(Book b, BookPublishReq req) {
        b.setTitle(req.getTitle());
        b.setAuthor(req.getAuthor());
        b.setIsbn(req.getIsbn());
        b.setPublisher(req.getPublisher());
        b.setPublishYear(req.getPublishYear());
        b.setEdition(req.getEdition());
        b.setCategoryId(req.getCategoryId());
        b.setCourseId(req.getCourseId());
        b.setMajorId(req.getMajorId());
        b.setOriginalPrice(req.getOriginalPrice());
        b.setConditionLevel(req.getConditionLevel());
        b.setWearLevel(req.getWearLevel());
        b.setNoteFlag(req.getNoteFlag());
        b.setCoverImg(req.getCoverImg());
        b.setImages(req.getImages());
        b.setDescription(req.getDescription());
        b.setPrice(req.getPrice());
        b.setSuggestedPrice(req.getSuggestedPrice());
        b.setPriceLow(req.getPriceLow());
        b.setPriceHigh(req.getPriceHigh());
        if (req.getListingCount() != null) b.setListingCount(req.getListingCount());
        if (req.getSoldCount30d() != null) b.setSoldCount30d(req.getSoldCount30d());
    }

    @Transactional
    public void offline(Long userId, Long bookId) {
        Book b = bookMapper.selectById(bookId);
        if (b == null) throw new BizException(404, "书籍不存在");
        boolean owner = Objects.equals(b.getSellerId(), userId);
        if (!owner) throw new BizException(403, "无权下架他人书籍");
        b.setStatus(3);
        bookMapper.updateById(b);
    }

    // ------------------------------------------------------------ 收藏

    @Transactional
    public boolean toggleFavorite(Long userId, Long bookId, boolean add) {
        Book b = bookMapper.selectById(bookId);
        if (b == null) throw new BizException(404, "书籍不存在");
        Favorite exist = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).eq(Favorite::getBookId, bookId).last("limit 1"));
        if (add && exist == null) {
            Favorite f = new Favorite();
            f.setUserId(userId);
            f.setBookId(bookId);
            favoriteMapper.insert(f);
            bookMapper.update(null, new UpdateWrapper<Book>()
                    .eq("id", bookId).setSql("favorite_count = favorite_count + 1"));
            return true;
        }
        if (!add && exist != null) {
            favoriteMapper.deleteById(exist.getId());
            bookMapper.update(null, new UpdateWrapper<Book>()
                    .eq("id", bookId).setSql("favorite_count = GREATEST(favorite_count - 1, 0)"));
            return false;
        }
        return exist != null;
    }

    public List<Book> myBooks(Long sellerId, String type) {
        LambdaQueryWrapper<Book> w = new LambdaQueryWrapper<Book>().eq(Book::getSellerId, sellerId);
        if ("selling".equals(type)) w.eq(Book::getStatus, 1);
        else if ("sold".equals(type)) w.eq(Book::getStatus, 2);
        else if ("offline".equals(type)) w.in(Book::getStatus, 3, 4);
        w.orderByDesc(Book::getId);
        List<Book> list = bookMapper.selectList(w);
        enrichList(list, sellerId);
        return list;
    }

    public List<Book> myFavorites(Long userId) {
        List<Favorite> favs = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).orderByDesc(Favorite::getId));
        if (favs.isEmpty()) return List.of();
        List<Long> ids = favs.stream().map(Favorite::getBookId).toList();
        List<Book> books = bookMapper.selectByIdsWithMeta(ids);
        enrichList(books, userId);
        Map<Long, Object> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
        books.sort(Comparator.comparingInt(b -> (int) order.getOrDefault(b.getId(), 999)));
        return books;
    }

    // ------------------------------------------------------------ 审核

    @Transactional
    public void audit(Long adminId, Long bookId, String action, String reason) {
        Book b = bookMapper.selectById(bookId);
        if (b == null) throw new BizException(404, "书籍不存在");
        switch (action) {
            case "approve" -> { b.setStatus(1); b.setRejectReason(null); }
            case "reject"  -> { b.setStatus(4); b.setRejectReason(reason); }
            case "offline" -> { b.setStatus(3); }
            default -> throw new BizException("未知审核动作: " + action);
        }
        bookMapper.updateById(b);
        AuditLog log = new AuditLog();
        log.setBookId(bookId);
        log.setAdminId(adminId);
        log.setAction(action);
        log.setReason(reason);
        auditLogMapper.insert(log);
    }

    // ------------------------------------------------------------ 组装

    public void enrichList(List<Book> books, Long currentUserId) {
        if (books == null || books.isEmpty()) return;
        List<Long> ids = books.stream().map(Book::getId).toList();

        try {
            Map<Long, List<String>> tagMap = new HashMap<>();
            for (Map<String, Object> row : tagMapper.selectNamesByBooks(ids)) {
                Object bid = row.get("bookId");
                Object name = row.get("name");
                if (bid == null || name == null) continue;
                tagMap.computeIfAbsent(((Number) bid).longValue(), k -> new ArrayList<>())
                        .add(String.valueOf(name));
            }
            books.forEach(b -> b.setTagNames(tagMap.getOrDefault(b.getId(), List.of())));
        } catch (Exception e) {
            log.warn("加载标签失败: {}", e.getMessage());
        }

        if (currentUserId != null) {
            List<Favorite> favs = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, currentUserId).in(Favorite::getBookId, ids));
            Set<Long> favIds = favs.stream().map(Favorite::getBookId).collect(Collectors.toSet());
            books.forEach(b -> b.setFavorited(favIds.contains(b.getId())));
        } else {
            books.forEach(b -> b.setFavorited(false));
        }
    }
}
