package com.campusbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusbook.common.BizException;
import com.campusbook.common.PageResult;
import com.campusbook.entity.Notification;
import com.campusbook.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    /**
     * 发一条通知。通知失败不应该影响主流程（下单、处理举报等），
     * 所以这里把异常吞掉只记日志。
     */
    public void push(Long userId, String type, String title, String content, String linkType, Long linkId) {
        if (userId == null) return;
        try {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setType(type);
            n.setTitle(title);
            n.setContent(content);
            n.setLinkType(linkType);
            n.setLinkId(linkId);
            n.setReadFlag(0);
            notificationMapper.insert(n);
        } catch (Exception e) {
            log.warn("通知发送失败 userId={} title={}", userId, title, e);
        }
    }

    public PageResult<Notification> page(Long userId, int page, int size) {
        Page<Notification> p = new Page<>(page, size);
        IPage<Notification> r = notificationMapper.selectPage(p,
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getId));
        return PageResult.of(r.getTotal(), page, size, r.getRecords());
    }

    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadFlag, 0));
    }

    public void markRead(Long userId, Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null) throw new BizException(404, "通知不存在");
        if (!n.getUserId().equals(userId)) throw new BizException(403, "无权操作");
        if (n.getReadFlag() != null && n.getReadFlag() == 1) return;
        notificationMapper.update(null, new UpdateWrapper<Notification>()
                .eq("id", id).set("read_flag", 1));
    }

    public void markAllRead(Long userId) {
        notificationMapper.update(null, new UpdateWrapper<Notification>()
                .eq("user_id", userId).eq("read_flag", 0).set("read_flag", 1));
    }
}
