package com.campusbook.controller;

import com.campusbook.common.R;
import com.campusbook.common.UserContext;
import com.campusbook.dto.BookQuery;
import com.campusbook.entity.Book;
import com.campusbook.service.BannerService;
import com.campusbook.service.BookService;
import com.campusbook.service.DictService;
import com.campusbook.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.campusbook.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "公开接口")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final DictService dictService;
    private final BookService bookService;
    private final RecommendService recommendService;
    private final BannerService bannerService;
    private final SellerService sellerService;

    @GetMapping("/dict")
    public R<Map<String, Object>> dict() {
        return R.ok(dictService.all());
    }

    @GetMapping("/majors")
    public R<?> majors() { return R.ok(dictService.majors()); }

    @GetMapping("/courses")
    public R<?> courses(@RequestParam(required = false) Long majorId) {
        return R.ok(dictService.courses(majorId));
    }

    @GetMapping("/categories")
    public R<?> categories() { return R.ok(dictService.categories()); }

    @GetMapping("/books")
    public R<?> books(BookQuery query) {
        return R.ok(bookService.search(query, null));
    }

    @GetMapping("/books/{id}")
    public R<Book> book(@PathVariable Long id) {
        return R.ok(bookService.detail(id, null));
    }

    @GetMapping("/home")
    public R<Map<String, Object>> home() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hot", bookService.search(hotQuery(), null).getList());
        m.put("latest", bookService.search(latestQuery(), null).getList());
        m.put("categories", dictService.categories());
        return R.ok(m);
    }

    @Operation(summary = "小程序广告位（仅返回投放中且在时间窗内的）")
    @GetMapping("/banners")
    public R<?> banners(@RequestParam(defaultValue = "home_banner") String position,
                        @RequestParam(defaultValue = "5") int limit) {
        return R.ok(bannerService.active(position, limit));
    }

    @Operation(summary = "广告曝光计数")
    @PostMapping("/banners/{id}/view")
    public R<Void> bannerView(@PathVariable Long id) {
        bannerService.addView(id);
        return R.ok();
    }

    @Operation(summary = "搜索热词（按在售书籍的课程聚合）")
    @GetMapping("/hot-keywords")
    public R<List<Map<String, Object>>> hotKeywords(@RequestParam(defaultValue = "8") int limit) {
        return R.ok(bookService.hotKeywords(limit));
    }

    @Operation(summary = "卖家画像：成交数、好评率、最近评价")
    @GetMapping("/sellers/{sellerId}")
    public R<Map<String, Object>> seller(@PathVariable Long sellerId) {
        return R.ok(sellerService.profile(sellerId));
    }

    @GetMapping("/similar/{bookId}")
    public R<List<Book>> similar(@PathVariable Long bookId,
                                 @RequestParam(defaultValue = "6") int limit) {
        return R.ok(recommendService.similarBooks(bookId, limit));
    }

    private BookQuery hotQuery() {
        BookQuery q = new BookQuery();
        q.setPage(1); q.setSize(10); q.setSort("hot"); q.setStatus(1);
        return q;
    }

    private BookQuery latestQuery() {
        BookQuery q = new BookQuery();
        q.setPage(1); q.setSize(10); q.setSort("newest"); q.setStatus(1);
        return q;
    }
}