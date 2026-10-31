package com.campusbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("book")
public class Book {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sellerId;
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private Integer publishYear;
    private String edition;
    private Long categoryId;
    private Long courseId;
    private Long majorId;
    private BigDecimal originalPrice;
    private Integer conditionLevel;
    private Integer wearLevel;
    private Integer noteFlag;
    private String coverImg;
    private String images;
    private String description;
    private BigDecimal price;
    private BigDecimal suggestedPrice;
    private BigDecimal priceLow;
    private BigDecimal priceHigh;
    private Integer listingCount;
    /** 列名带数字，MyBatis-Plus 的驼峰转换会得到 sold_count30d，这里显式指定 */
    @TableField("sold_count_30d")
    private Integer soldCount30d;
    private Integer status;
    private String rejectReason;
    /** 1 = 高信用卖家发布，首页与审核队列优先 */
    private Integer priority;
    private Integer viewCount;
    private Integer favoriteCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ---- 非数据库字段 ----
    @TableField(exist = false)
    private String sellerNickname;
    @TableField(exist = false)
    private String sellerAvatar;
    @TableField(exist = false)
    private String courseName;
    @TableField(exist = false)
    private String majorName;
    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private List<String> tagNames;
    @TableField(exist = false)
    private Boolean favorited;
    @TableField(exist = false)
    private Integer matchScore;
    @TableField(exist = false)
    private String matchReason;
}
