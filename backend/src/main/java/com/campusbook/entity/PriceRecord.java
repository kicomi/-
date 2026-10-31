package com.campusbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("price_record")
public class PriceRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bookId;
    private String features;
    private BigDecimal suggestedPrice;
    private BigDecimal priceLow;
    private BigDecimal priceHigh;
    private String modelVersion;
    private LocalDateTime createdAt;
}
