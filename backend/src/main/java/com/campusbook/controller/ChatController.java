package com.campusbook.controller;

import com.campusbook.common.R;
import com.campusbook.common.UserContext;
import com.campusbook.entity.ChatMessage;
import com.campusbook.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "聊天")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public R<ChatMessage> send(@RequestBody Map<String, Object> body) {
        Long uid = UserContext.requireUserId();
        Long to = body.get("toUserId") == null ? null : Long.valueOf(String.valueOf(body.get("toUserId")));
        Long bookId = body.get("bookId") == null ? null : Long.valueOf(String.valueOf(body.get("bookId")));
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        String type = body.get("type") == null ? null : String.valueOf(body.get("type"));
        return R.ok(chatService.send(uid, to, bookId, content, type));
    }

    @Operation(summary = "历史消息（同时标记已读）")
    @GetMapping("/history")
    public R<List<ChatMessage>> history(@RequestParam Long peerId,
                                        @RequestParam(required = false) Long bookId) {
        return R.ok(chatService.history(UserContext.requireUserId(), peerId, bookId));
    }

    @Operation(summary = "会话列表")
    @GetMapping("/conversations")
    public R<List<Map<String, Object>>> conversations() {
        return R.ok(chatService.conversations(UserContext.requireUserId()));
    }

    @Operation(summary = "未读数")
    @GetMapping("/unread")
    public R<Map<String, Object>> unread() {
        return R.ok(Map.of("count", chatService.unreadCount(UserContext.requireUserId())));
    }
}
