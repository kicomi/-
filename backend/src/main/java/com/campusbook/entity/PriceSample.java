package com.campusbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("price_sample")
public class PriceSample {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Long categoryId;
    private BigDecimal originalPrice;
    private Integer conditionLevel;
    private Integer wearLevel;
    private Integer noteFlag;
    private Integer publishYear;
    private Integer listingCount;
    @TableField("sold_count_30d")
    private Integer soldCount30d;
    private BigDecimal sellerHistory;
    private BigDecimal soldPrice;
    private LocalDateTime soldAt;
}
