package com.campusbook.controller;

import com.campusbook.common.PageResult;
import com.campusbook.common.R;
import com.campusbook.common.UserContext;
import com.campusbook.entity.Report;
import com.campusbook.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "举报")
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "提交举报")
    @PostMapping("/api/reports")
    public R<Report> create(@RequestBody Map<String, Object> body) {
        Long uid = UserContext.requireUserId();
        String type = body.get("targetType") == null ? "book" : String.valueOf(body.get("targetType"));
        Long targetId = body.get("targetId") == null ? null : Long.valueOf(String.valueOf(body.get("targetId")));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        String detail = body.get("detail") == null ? null : String.valueOf(body.get("detail"));
        return R.ok(reportService.create(uid, type, targetId, reason, detail));
    }

    @Operation(summary = "举报列表")
    @GetMapping("/api/admin/reports")
    public R<PageResult<Report>> page(@RequestParam(required = false) Integer status,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return R.ok(reportService.page(status, page, size));
    }

    @Operation(summary = "举报详情（含核实证据）")
    @GetMapping("/api/admin/reports/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(reportService.detail(id));
    }

    @Operation(summary = "处理举报")
    @PutMapping("/api/admin/reports/{id}")
    public R<Report> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = body.get("status") == null ? null : Integer.valueOf(String.valueOf(body.get("status")));
        String note = body.get("note") == null ? null : String.valueOf(body.get("note"));
        return R.ok(reportService.handle(UserContext.requireUserId(), id, status, note));
    }

    @Operation(summary = "待处理举报数")
    @GetMapping("/api/admin/reports/pending")
    public R<Map<String, Object>> pending() {
        return R.ok(Map.of("count", reportService.pendingCount()));
    }
}
