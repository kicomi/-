package com.campusbook.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewReq {
    @NotNull
    private Long orderId;
    @Min(1) @Max(5)
    private Integer rating = 5;
    private String content;
    private String tags;
    /** 图片地址数组的 JSON 字符串，最多 3 张 */
    private String images;
}
