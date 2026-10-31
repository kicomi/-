package com.campusbook.controller;

import com.campusbook.common.BizException;
import com.campusbook.common.PageResult;
import com.campusbook.common.R;
import com.campusbook.common.UserContext;
import com.campusbook.dto.BookPublishReq;
import com.campusbook.dto.BookQuery;
import com.campusbook.entity.Book;
import com.campusbook.service.BookService;
import com.campusbook.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "书籍")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final RecommendService recommendService;

    @Operation(summary = "检索书籍（支持课程/专业/价格/品相/关键词）")
    @GetMapping
    public R<PageResult<Book>> list(BookQuery query) {
        return R.ok(bookService.search(query, UserContext.getUserId()));
    }

    @Operation(summary = "书籍详情")
    @GetMapping("/{id}")
    public R<Book> detail(@PathVariable Long id) {
        return R.ok(bookService.detail(id, UserContext.getUserId()));
    }

    @Operation(summary = "发布闲置")
    @PostMapping
    public R<Book> publish(@Valid @RequestBody BookPublishReq req) {
        return R.ok(bookService.publish(UserContext.requireUserId(), req));
    }

    @Operation(summary = "编辑我发布的书籍")
    @PutMapping("/{id}")
    public R<Book> update(@PathVariable Long id, @Valid @RequestBody BookPublishReq req) {
        return R.ok(bookService.updateBook(UserContext.requireUserId(), id, req));
    }

    @Operation(summary = "下架")
    @PostMapping("/{id}/offline")
    public R<Void> offline(@PathVariable Long id) {
        bookService.offline(UserContext.requireUserId(), id);
        return R.ok();
    }

    @Operation(summary = "收藏")
    @PostMapping("/{id}/favorite")
    public R<Boolean> favorite(@PathVariable Long id) {
        return R.ok(bookService.toggleFavorite(UserContext.requireUserId(), id, true));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{id}/favorite")
    public R<Boolean> unfavorite(@PathVariable Long id) {
        return R.ok(bookService.toggleFavorite(UserContext.requireUserId(), id, false));
    }

    @Operation(summary = "我发布的书籍")
    @GetMapping("/my")
    public R<List<Book>> my(@RequestParam(defaultValue = "selling") String type) {
        return R.ok(bookService.myBooks(UserContext.requireUserId(), type));
    }

    @Operation(summary = "我最近看过的书")
    @GetMapping("/my/history")
    public R<List<Book>> history(@RequestParam(defaultValue = "12") int limit) {
        return R.ok(bookService.browseHistory(UserContext.requireUserId(), limit));
    }

    @Operation(summary = "我的收藏")
    @GetMapping("/my/favorites")
    public R<List<Book>> myFavorites() {
        return R.ok(bookService.myFavorites(UserContext.requireUserId()));
    }

    @Operation(summary = "相似书籍（协同过滤）")
    @GetMapping("/{id}/similar")
    public R<List<Book>> similar(@PathVariable Long id,
                                 @RequestParam(defaultValue = "6") int limit) {
        return R.ok(recommendService.similarBooks(id, limit));
    }

    @Operation(summary = "可能想买的人（卖家视角）")
    @GetMapping("/potential-buyers")
    public R<List<Long>> potentialBuyers() {
        return R.ok(recommendService.potentialBuyers(UserContext.requireUserId(), 5));
    }
}
