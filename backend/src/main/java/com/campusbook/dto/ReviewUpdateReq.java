package com.campusbook.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** 修改评价：不同于新建，不需要 orderId */
@Data
public class ReviewUpdateReq {
    @Min(1) @Max(5)
    private Integer rating;
    private String content;
    private String images;
}
