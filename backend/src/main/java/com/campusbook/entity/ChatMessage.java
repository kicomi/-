package com.campusbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 买卖双方聊天消息。
 * conversationKey 由两个用户 id 升序拼接，保证同一对用户只有一条会话线。
 */
@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationKey;
    private Long fromUserId;
    private Long toUserId;
    private Long bookId;
    private String content;
    private String type;
    private Integer readFlag;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String fromNickname;
    @TableField(exist = false)
    private String fromAvatar;
    @TableField(exist = false)
    private String peerNickname;
    @TableField(exist = false)
    private String peerAvatar;
    @TableField(exist = false)
    private String bookTitle;
}
