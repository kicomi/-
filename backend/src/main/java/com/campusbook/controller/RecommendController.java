package com.campusbook.controller;

import com.campusbook.common.R;
import com.campusbook.common.UserContext;
import com.campusbook.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "推荐")
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @Operation(summary = "个性化推荐（ItemCF + 内容匹配融合）")
    @GetMapping("/for-user")
    public R<Map<String, Object>> forUser(@RequestParam(defaultValue = "10") int limit,
                                          @RequestParam(defaultValue = "home") String scene) {
        return R.ok(recommendService.forUser(UserContext.requireUserId(), limit, scene));
    }
}
