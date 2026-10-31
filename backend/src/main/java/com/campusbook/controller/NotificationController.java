package com.campusbook.controller;

import com.campusbook.common.PageResult;
import com.campusbook.common.R;
import com.campusbook.common.UserContext;
import com.campusbook.entity.Notification;
import com.campusbook.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "通知")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "通知列表")
    @GetMapping
    public R<PageResult<Notification>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return R.ok(notificationService.page(UserContext.requireUserId(), page, size));
    }

    @Operation(summary = "未读数")
    @GetMapping("/unread")
    public R<Map<String, Object>> unread() {
        return R.ok(Map.of("count", notificationService.unreadCount(UserContext.requireUserId())));
    }

    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        notificationService.markRead(UserContext.requireUserId(), id);
        return R.ok(null);
    }

    @Operation(summary = "全部已读")
    @PutMapping("/read-all")
    public R<Void> readAll() {
        notificationService.markAllRead(UserContext.requireUserId());
        return R.ok(null);
    }
}
