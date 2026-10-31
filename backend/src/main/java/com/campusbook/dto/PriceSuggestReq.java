package com.campusbook.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriceSuggestReq {
    private Long bookId;
    private BigDecimal originalPrice;
    private Integer conditionLevel = 3;
    private Integer wearLevel = 3;
    private Integer noteFlag = 0;
    private Integer publishYear;
    private Integer listingCount = 1;
    private Integer soldCount30d = 0;
    private BigDecimal sellerHistory;
    private Long categoryId;
}
