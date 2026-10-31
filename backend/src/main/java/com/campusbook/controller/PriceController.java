package com.campusbook.controller;

import com.campusbook.common.R;
import com.campusbook.dto.PriceSuggestReq;
import com.campusbook.dto.PriceSuggestResp;
import com.campusbook.service.PriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "智能定价")
@RestController
@RequestMapping("/api/price")
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    @Operation(summary = "根据图书特征给出建议售价与价格区间")
    @PostMapping("/suggest")
    public R<PriceSuggestResp> suggest(@RequestBody PriceSuggestReq req) {
        return R.ok(priceService.suggest(req));
    }
}
