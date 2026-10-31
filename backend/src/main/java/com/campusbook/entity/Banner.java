package com.campusbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小程序广告位。管理端投放，学生端首页轮播读取。
 */
@Data
@TableName("banner")
public class Banner {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    /** book / category / search / url / none */
    private String linkType;
    private String linkValue;
    /** home_banner / category_banner / detail_banner */
    private String position;
    private Integer sort;
    /** 1 投放中 0 已暂停 */
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer viewCount;
    private Integer clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
