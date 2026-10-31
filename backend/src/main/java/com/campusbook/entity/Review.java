package com.campusbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long bookId;
    private Long fromUserId;
    private Long toUserId;
    private Integer rating;
    private String content;
    private String tags;
    /** 评价图片，JSON 数组字符串 */
    private String images;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String fromNickname;
}
