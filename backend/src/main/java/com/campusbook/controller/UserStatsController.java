package com.campusbook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbook.common.R;
import com.campusbook.common.UserContext;
import com.campusbook.entity.Book;
import com.campusbook.mapper.BookMapper;
import com.campusbook.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "个人统计")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserStatsController {

    private final BookMapper bookMapper;
    private final BookService bookService;

    /**
     * 个人中心的数据条。
     * 一次查完，避免首页轮询好几个接口。
     */
    @Operation(summary = "我的统计：在售/已售/收藏/被浏览")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        Long uid = UserContext.requireUserId();

        List<Book> mine = bookMapper.selectList(new LambdaQueryWrapper<Book>()
                .eq(Book::getSellerId, uid));
        long selling = mine.stream().filter(b -> b.getStatus() != null && b.getStatus() == 1).count();
        long sold = mine.stream().filter(b -> b.getStatus() != null && b.getStatus() == 2).count();
        long views = mine.stream().mapToLong(b -> b.getViewCount() == null ? 0 : b.getViewCount()).sum();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("selling", selling);
        out.put("sold", sold);
        out.put("fav", bookService.myFavorites(uid).size());
        out.put("views", views);
        return R.ok(out);
    }
}
