package com.campusbook.controller;

import com.campusbook.common.PageResult;
import com.campusbook.common.R;
import com.campusbook.common.UserContext;
import com.campusbook.dto.AuditReq;
import com.campusbook.entity.Banner;
import com.campusbook.entity.Book;
import com.campusbook.entity.BookOrder;
import com.campusbook.entity.User;
import com.campusbook.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "管理端")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BookService bookService;
    private final UserService userService;
    private final OrderService orderService;
    private final StatsService statsService;
    private final AlgoClient algoClient;
    private final BannerService bannerService;
    private final AiService aiService;
    private final AdminUserService adminUserService;

    @Operation(summary = "重新训练算法模型（用最新数据）")
    @PostMapping("/algo/retrain")
    public R<Map<String, Object>> retrain() {
        UserContext.requireAdmin();
        long start = System.currentTimeMillis();
        Map<String, Object> r = algoClient.reload();
        r.put("costMs", System.currentTimeMillis() - start);
        return R.ok(r);
    }

    @Operation(summary = "调整用户信用分")
    @PutMapping("/users/{id}/credit")
    public R<User> adjustCredit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UserContext.requireAdmin();
        Integer score = body.get("creditScore") == null ? null : Integer.valueOf(String.valueOf(body.get("creditScore")));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        return R.ok(userService.adjustCredit(id, score, reason));
    }

    @Operation(summary = "用户详情：资料 + 发布 + 订单 + 评价")
    @GetMapping("/users/{id}/detail")
    public R<Map<String, Object>> userDetail(@PathVariable Long id) {
        UserContext.requireAdmin();
        return R.ok(adminUserService.detail(id));
    }

    @Operation(summary = "重置用户昵称")
    @PutMapping("/users/{id}/nickname")
    public R<User> resetNickname(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UserContext.requireAdmin();
        String nickname = body.get("nickname") == null ? null : String.valueOf(body.get("nickname"));
        return R.ok(userService.resetNickname(id, nickname));
    }

    @Operation(summary = "书籍审核列表")
    @GetMapping("/books")
    public R<PageResult<Book>> books(@RequestParam(required = false) Integer status,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        UserContext.requireAdmin();
        return R.ok(bookService.pageForAdmin(status, keyword, page, size));
    }

    @Operation(summary = "审核书籍")
    @PostMapping("/books/{id}/audit")
    public R<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditReq req) {
        UserContext.requireAdmin();
        bookService.audit(UserContext.requireUserId(), id, req.getAction(), req.getReason());
        return R.ok();
    }

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public R<PageResult<User>> users(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String sort,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        UserContext.requireAdmin();
        var p = userService.page(page, size, keyword, sort);
        return R.ok(PageResult.of(p.getTotal(), page, size, p.getRecords()));
    }

    @Operation(summary = "启用/禁用用户")
    @PostMapping("/users/{id}/status")
    public R<Void> userStatus(@PathVariable Long id, @RequestParam Integer status) {
        UserContext.requireAdmin();
        userService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "订单管理")
    @GetMapping("/orders")
    public R<PageResult<BookOrder>> orders(@RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        UserContext.requireAdmin();
        return R.ok(orderService.listAll(status, page, size));
    }

    @Operation(summary = "数据总览")
    @GetMapping("/stats/overview")
    public R<Map<String, Object>> overview() {
        UserContext.requireAdmin();
        return R.ok(statsService.overview());
    }

    @Operation(summary = "图表数据")
    @GetMapping("/stats/charts")
    public R<Map<String, Object>> charts() {
        UserContext.requireAdmin();
        return R.ok(statsService.charts());
    }

    @Operation(summary = "算法服务状态")
    @GetMapping("/algo-status")
    public R<Map<String, Object>> algoStatus() {
        UserContext.requireAdmin();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("alive", algoClient.isAlive());
        return R.ok(m);
    }

    // ---------------------------------------------------------- 封禁 / 禁言

    @Operation(summary = "封禁用户，days<=0 为永久")
    @PostMapping("/users/{id}/ban")
    public R<User> ban(@PathVariable Long id,
                       @RequestParam(defaultValue = "0") int days,
                       @RequestParam(required = false) String reason) {
        UserContext.requireAdmin();
        return R.ok(userService.ban(id, days, reason));
    }

    @Operation(summary = "解除封禁")
    @PostMapping("/users/{id}/unban")
    public R<User> unban(@PathVariable Long id) {
        UserContext.requireAdmin();
        return R.ok(userService.unban(id));
    }

    @Operation(summary = "禁言用户，days<=0 为永久")
    @PostMapping("/users/{id}/mute")
    public R<User> mute(@PathVariable Long id,
                        @RequestParam(defaultValue = "0") int days,
                        @RequestParam(required = false) String reason) {
        UserContext.requireAdmin();
        return R.ok(userService.mute(id, days, reason));
    }

    @Operation(summary = "解除禁言")
    @PostMapping("/users/{id}/unmute")
    public R<User> unmute(@PathVariable Long id) {
        UserContext.requireAdmin();
        return R.ok(userService.unmute(id));
    }

    // ---------------------------------------------------------- 广告位

    @Operation(summary = "广告列表")
    @GetMapping("/banners")
    public R<java.util.List<Banner>> banners(@RequestParam(required = false) String position) {
        UserContext.requireAdmin();
        return R.ok(bannerService.list(position));
    }

    @Operation(summary = "新增或修改广告")
    @PostMapping("/banners")
    public R<Banner> saveBanner(@RequestBody Banner banner) {
        UserContext.requireAdmin();
        return R.ok(bannerService.save(banner));
    }

    @Operation(summary = "启用/暂停广告")
    @PostMapping("/banners/{id}/status")
    public R<Void> bannerStatus(@PathVariable Long id, @RequestParam Integer status) {
        UserContext.requireAdmin();
        bannerService.toggle(id, status);
        return R.ok();
    }

    @Operation(summary = "删除广告")
    @DeleteMapping("/banners/{id}")
    public R<Void> deleteBanner(@PathVariable Long id) {
        UserContext.requireAdmin();
        bannerService.delete(id);
        return R.ok();
    }

    // ---------------------------------------------------------- 运营助手

    @Operation(summary = "AI 助手对话")
    @PostMapping("/ai/chat")
    public R<Map<String, Object>> aiChat(@RequestBody Map<String, String> body) {
        UserContext.requireAdmin();
        return R.ok(aiService.chat(body.get("message")));
    }

    @Operation(summary = "AI 助手推荐问题")
    @GetMapping("/ai/quick-questions")
    public R<java.util.List<String>> quickQuestions() {
        UserContext.requireAdmin();
        return R.ok(aiService.quickQuestions());
    }
}
