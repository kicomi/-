package com.campusbook.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class PriceSuggestResp {
    private BigDecimal suggestedPrice;
    private BigDecimal priceLow;
    private BigDecimal priceHigh;
    private String model;
    private String modelVersion;
    private Double confidence;
    /** 与"原价打折"基线的对比，答辩展示用 */
    private BigDecimal baselinePrice;
    private Map<String, Double> contributions;
    private List<String> tips;
    private boolean degraded;
}
