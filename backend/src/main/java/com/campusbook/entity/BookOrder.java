package com.campusbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("book_order")
public class BookOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long bookId;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal price;
    private Integer status;
    private String pickupPlace;
    private LocalDateTime pickupTime;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 卖家确认预约的时刻，时间轴上单独一个节点 */
    private LocalDateTime confirmedAt;
    private LocalDateTime finishedAt;

    @TableField(exist = false)
    private String bookTitle;
    @TableField(exist = false)
    private String bookCover;
    @TableField(exist = false)
    private String buyerNickname;
    @TableField(exist = false)
    private String sellerNickname;
    @TableField(exist = false)
    private Boolean reviewed;

    /** 已评价时带上评价内容，方便订单页直接展开查看 */
    @TableField(exist = false)
    private Long reviewId;
    @TableField(exist = false)
    private Integer reviewRating;
    @TableField(exist = false)
    private String reviewContent;
    @TableField(exist = false)
    private String reviewImages;
    @TableField(exist = false)
    private String statusText;

    /** 待确认订单的自动取消时间，前端用它显示倒计时 */
    @TableField(exist = false)
    private LocalDateTime expireAt;
    /** 剩余小时数，负数表示已过期 */
    @TableField(exist = false)
    private Long remainHours;
}
