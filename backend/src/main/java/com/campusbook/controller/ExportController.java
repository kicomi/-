package com.campusbook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbook.common.ExcelUtil;
import com.campusbook.common.UserContext;
import com.campusbook.entity.Book;
import com.campusbook.entity.BookOrder;
import com.campusbook.entity.User;
import com.campusbook.mapper.BookMapper;
import com.campusbook.mapper.BookOrderMapper;
import com.campusbook.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "数据导出")
@RestController
@RequestMapping("/api/admin/export")
@RequiredArgsConstructor
public class ExportController {

    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final BookOrderMapper orderMapper;
    private final com.campusbook.service.UserService userService;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Operation(summary = "导出用户")
    @GetMapping("/users")
    public void users(@RequestParam(required = false) Integer status,
                      HttpServletResponse resp) throws Exception {
        UserContext.requireAdmin();

        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        if (status != null) w.eq(User::getStatus, status);
        w.orderByDesc(User::getId);
        List<User> list = userMapper.selectList(w);

        String[] head = {"ID", "昵称", "学号", "专业", "年级", "信用分", "状态", "禁言", "注册时间", "真实姓名"};
        List<String[]> rows = new ArrayList<>();
        for (User u : list) {
            userService.fillMajor(u);
            rows.add(new String[]{
                    str(u.getId()),
                    u.getNickname(),
                    u.getStudentNo(),
                    u.getMajorName(),
                    u.getGrade(),
                    str(u.getCreditScore()),
                    u.getStatus() != null && u.getStatus() == 1 ? "正常" : "封禁",
                    u.getMuteStatus() != null && u.getMuteStatus() == 1 ? "禁言中" : "正常",
                    u.getCreatedAt() == null ? "" : u.getCreatedAt().format(TS),
                    u.getRealName()
            });
        }
        write(resp, "用户列表", head, rows);
    }

    @Operation(summary = "导出订单")
    @GetMapping("/orders")
    public void orders(@RequestParam(required = false) Integer status,
                       HttpServletResponse resp) throws Exception {
        UserContext.requireAdmin();

        LambdaQueryWrapper<BookOrder> w = new LambdaQueryWrapper<>();
        if (status != null) w.eq(BookOrder::getStatus, status);
        w.orderByDesc(BookOrder::getId);
        List<BookOrder> list = orderMapper.selectList(w);

        Map<Long, String> titles = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        String[] STATUS = {"待确认", "已确认", "已完成", "已取消"};

        String[] head = {"订单号", "书籍", "金额", "状态", "买家", "卖家", "交接地点", "下单时间", "完成时间"};
        List<String[]> rows = new ArrayList<>();
        for (BookOrder o : list) {
            String title = titles.computeIfAbsent(o.getBookId(), id -> {
                Book b = bookMapper.selectById(id);
                return b == null ? "书籍已删除" : b.getTitle();
            });
            String buyer = names.computeIfAbsent(o.getBuyerId(), id -> {
                User u = userMapper.selectById(id);
                return u == null ? "已注销" : u.getNickname();
            });
            String seller = names.computeIfAbsent(o.getSellerId(), id -> {
                User u = userMapper.selectById(id);
                return u == null ? "已注销" : u.getNickname();
            });
            int s = o.getStatus() == null ? 0 : Math.min(o.getStatus(), 3);
            rows.add(new String[]{
                    o.getOrderNo(), title, str(o.getPrice()), STATUS[s],
                    buyer, seller, o.getPickupPlace(),
                    o.getCreatedAt() == null ? "" : o.getCreatedAt().format(TS),
                    o.getFinishedAt() == null ? "" : o.getFinishedAt().format(TS)
            });
        }
        write(resp, "订单列表", head, rows);
    }

    private void write(HttpServletResponse resp, String sheet, String[] head, List<String[]> rows)
            throws Exception {
        byte[] data = ExcelUtil.build(sheet, head, rows);
        String fileName = sheet + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        resp.setContentLength(data.length);
        resp.getOutputStream().write(data);
        resp.getOutputStream().flush();
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
