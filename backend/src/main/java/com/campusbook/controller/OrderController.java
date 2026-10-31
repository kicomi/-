package com.campusbook.controller;

import com.campusbook.common.PageResult;
import com.campusbook.common.R;
import com.campusbook.common.UserContext;
import com.campusbook.dto.OrderCreateReq;
import com.campusbook.entity.BookOrder;
import com.campusbook.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "下单（预约自提）")
    @PostMapping
    public R<BookOrder> create(@Valid @RequestBody OrderCreateReq req) {
        return R.ok(orderService.create(UserContext.requireUserId(), req));
    }

    @Operation(summary = "卖家确认预约")
    @PutMapping("/{id}/confirm")
    public R<BookOrder> confirm(@PathVariable Long id) {
        return R.ok(orderService.confirm(UserContext.requireUserId(), id));
    }

    @Operation(summary = "完成交易")
    @PutMapping("/{id}/complete")
    public R<BookOrder> complete(@PathVariable Long id) {
        return R.ok(orderService.complete(UserContext.requireUserId(), id));
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public R<BookOrder> cancel(@PathVariable Long id) {
        return R.ok(orderService.cancel(UserContext.requireUserId(), id));
    }

    @Operation(summary = "我的订单")
    @GetMapping
    public R<PageResult<BookOrder>> list(@RequestParam(defaultValue = "buyer") String role,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return R.ok(orderService.list(UserContext.requireUserId(), role, status, page, size));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<BookOrder> detail(@PathVariable Long id) {
        return R.ok(orderService.get(id));
    }
}
