package com.campusbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.campusbook.common.BizException;
import com.campusbook.entity.Book;
import com.campusbook.entity.ChatMessage;
import com.campusbook.entity.User;
import com.campusbook.mapper.BookMapper;
import com.campusbook.mapper.ChatMessageMapper;
import com.campusbook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageMapper chatMapper;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;

    /** 两个用户 id 升序拼接，保证 A 找 B 与 B 找 A 是同一条会话 */
    private String conversationKey(Long a, Long b) {
        return Math.min(a, b) + "_" + Math.max(a, b);
    }

    @Transactional
    public ChatMessage send(Long fromId, Long toId, Long bookId, String content, String type) {
        if (toId == null) throw new BizException("缺少接收人");
        if (Objects.equals(fromId, toId)) throw new BizException("不能给自己发消息");
        if (content == null || content.trim().isEmpty()) throw new BizException("消息不能为空");
        if (content.length() > 1000) throw new BizException("消息太长了");

        User sender = userMapper.selectById(fromId);
        if (sender == null) throw new BizException(404, "用户不存在");
        // 禁言在发消息这一刻校验，管理端解禁后立即恢复
        if (sender.getMuteStatus() != null && sender.getMuteStatus() == 1) {
            if (sender.getMuteUntil() == null || sender.getMuteUntil().isAfter(LocalDateTime.now())) {
                throw new BizException(403, "你已被禁言，暂时不能发送消息");
            }
        }
        if (userMapper.selectById(toId) == null) throw new BizException(404, "对方不存在");

        ChatMessage m = new ChatMessage();
        m.setConversationKey(conversationKey(fromId, toId));
        m.setFromUserId(fromId);
        m.setToUserId(toId);
        m.setBookId(bookId);
        m.setContent(content.trim());
        m.setType(type == null ? "text" : type);
        m.setReadFlag(0);
        chatMapper.insert(m);
        return m;
    }

    /** 拉取与某人的历史消息，顺带把对方发给我的标记为已读 */
    @Transactional
    public List<ChatMessage> history(Long meId, Long peerId, Long bookId) {
        String key = conversationKey(meId, peerId);

        LambdaQueryWrapper<ChatMessage> w = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationKey, key)
                .orderByAsc(ChatMessage::getId);
        if (bookId != null) w.eq(ChatMessage::getBookId, bookId);

        List<ChatMessage> list = chatMapper.selectList(w);
        if (list.size() > 300) list = list.subList(list.size() - 300, list.size());

        chatMapper.update(null, new UpdateWrapper<ChatMessage>()
                .eq("conversation_key", key)
                .eq("to_user_id", meId)
                .eq("read_flag", 0)
                .set("read_flag", 1));

        // 补一下书名，聊天页顶部要显示"正在聊哪本书"
        Map<Long, String> bookTitles = new HashMap<>();
        for (ChatMessage m : list) {
            if (m.getBookId() != null && !bookTitles.containsKey(m.getBookId())) {
                Book b = bookMapper.selectById(m.getBookId());
                bookTitles.put(m.getBookId(), b == null ? null : b.getTitle());
            }
            m.setBookTitle(m.getBookId() == null ? null : bookTitles.get(m.getBookId()));
        }
        return list;
    }

    /** 会话列表：对端昵称、头像、最后一条消息、未读数 */
    public List<Map<String, Object>> conversations(Long meId) {
        List<Map<String, Object>> rows = chatMapper.selectConversations(meId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long peerId = row.get("peerId") == null ? null : ((Number) row.get("peerId")).longValue();
            if (peerId == null) continue;
            User peer = userMapper.selectById(peerId);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("peerId", peerId);
            m.put("peerNickname", peer == null ? "已注销用户" : peer.getNickname());
            m.put("peerAvatar", peer == null ? "" : peer.getAvatar());
            m.put("lastContent", row.get("lastContent"));
            m.put("lastTime", row.get("lastTime"));
            m.put("unread", row.get("unread"));
            out.add(m);
        }
        return out;
    }

    public long unreadCount(Long uid) {
        return chatMapper.countUnread(uid);
    }
}
