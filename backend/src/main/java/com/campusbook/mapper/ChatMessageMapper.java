package com.campusbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusbook.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 会话列表：每个对端只保留最新一条，同时统计未读数。
     * 校园场景消息量很小，直接 GROUP BY 就够，不用上缓存。
     */
    @Select("""
            SELECT t.peer_id     AS peerId,
                   t.last_id     AS lastId,
                   m.content     AS lastContent,
                   m.created_at  AS lastTime,
                   t.unread      AS unread
              FROM (
                SELECT CASE WHEN from_user_id = #{uid} THEN to_user_id ELSE from_user_id END AS peer_id,
                       MAX(id) AS last_id,
                       SUM(CASE WHEN to_user_id = #{uid} AND read_flag = 0 THEN 1 ELSE 0 END) AS unread
                  FROM chat_message
                 WHERE from_user_id = #{uid} OR to_user_id = #{uid}
                 GROUP BY peer_id
              ) t
              JOIN chat_message m ON m.id = t.last_id
             ORDER BY t.last_id DESC
            """)
    List<Map<String, Object>> selectConversations(@Param("uid") Long uid);

    @Select("SELECT COUNT(*) FROM chat_message WHERE to_user_id = #{uid} AND read_flag = 0")
    long countUnread(@Param("uid") Long uid);
}
