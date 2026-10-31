package com.campusbook.controller;

import com.campusbook.common.R;
import com.campusbook.common.UserContext;
import com.campusbook.dto.ReviewReq;
import com.campusbook.entity.Review;
import com.campusbook.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.campusbook.common.PageResult;
import java.util.List;
import java.util.Map;

@Tag(name = "评价")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public R<Review> create(@Valid @RequestBody ReviewReq req) {
        return R.ok(reviewService.create(UserContext.requireUserId(), req));
    }

    @PutMapping("/{id}")
    public R<Review> update(@PathVariable Long id,
                            @Valid @RequestBody com.campusbook.dto.ReviewUpdateReq req) {
        return R.ok(reviewService.update(UserContext.requireUserId(), id, req));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        reviewService.delete(UserContext.requireUserId(), id);
        return R.ok(null);
    }

    @GetMapping("/order/{orderId}")
    public R<Review> byOrder(@PathVariable Long orderId) {
        return R.ok(reviewService.byOrder(UserContext.requireUserId(), orderId));
    }

    @GetMapping("/book/{bookId}/page")
    public R<PageResult<Review>> byBookPage(@PathVariable Long bookId,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return R.ok(reviewService.pageByBook(bookId, page, size));
    }

    @GetMapping("/book/{bookId}/summary")
    public R<Map<String, Object>> summary(@PathVariable Long bookId) {
        return R.ok(reviewService.summary(bookId));
    }

    @GetMapping("/book/{bookId}")
    public R<List<Review>> byBook(@PathVariable Long bookId) {
        return R.ok(reviewService.listByBook(bookId));
    }

    @GetMapping("/user/{userId}")
    public R<List<Review>> byUser(@PathVariable Long userId) {
        return R.ok(reviewService.listByUser(userId));
    }
}
