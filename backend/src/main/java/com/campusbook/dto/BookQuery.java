package com.campusbook.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookQuery {
    private Integer page = 1;
    private Integer size = 10;
    private String keyword;
    private Long categoryId;
    private Long majorId;
    private Long courseId;
    private Long sellerId;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private Integer conditionMin;
    private Integer noteFlag;
    /** newest | priceAsc | priceDesc | hot | priceSaving */
    private String sort = "newest";
    private Integer status = 1;
}
