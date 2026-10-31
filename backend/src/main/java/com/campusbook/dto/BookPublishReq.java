package com.campusbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookPublishReq {
    @NotBlank(message = "书名不能为空")
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private Integer publishYear;
    private String edition;
    private Long categoryId;
    private Long courseId;
    private Long majorId;
    @NotNull(message = "原价不能为空")
    private BigDecimal originalPrice;
    private Integer conditionLevel = 3;
    private Integer wearLevel = 3;
    private Integer noteFlag = 0;
    private String coverImg;
    private String images;
    private String description;
    @NotNull(message = "售价不能为空")
    private BigDecimal price;
    private BigDecimal suggestedPrice;
    private BigDecimal priceLow;
    private BigDecimal priceHigh;
    private Integer listingCount = 1;
    private Integer soldCount30d = 0;
}
