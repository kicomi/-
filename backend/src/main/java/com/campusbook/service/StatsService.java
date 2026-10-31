package com.campusbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbook.entity.User;
import com.campusbook.mapper.BookMapper;
import com.campusbook.mapper.BookOrderMapper;
import com.campusbook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final BookMapper bookMapper;
    private final BookOrderMapper orderMapper;
    private final UserMapper userMapper;

    public Map<String, Object> overview() {
        long users = userMapper.selectCount(new LambdaQueryWrapper<User>().ne(User::getRole, "admin"));
        long books = bookMapper.selectCount(null);
        long onSale = bookMapper.countByStatus(1);
        long sold = bookMapper.countByStatus(2);
        long pending = bookMapper.countByStatus(0);
        long orders = orderMapper.countAll();
        long completed = orderMapper.countByStatus(2);
        BigDecimal gmv = orderMapper.sumCompletedAmount();
        BigDecimal avgPrice = bookMapper.avgPriceByStatus(1);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userCount", users);
        m.put("bookCount", books);
        m.put("onSaleCount", onSale);
        m.put("soldCount", sold);
        m.put("pendingCount", pending);
        m.put("orderCount", orders);
        m.put("completedCount", completed);
        m.put("gmv", gmv == null ? BigDecimal.ZERO : gmv);
        m.put("avgPrice", avgPrice == null ? BigDecimal.ZERO
                : avgPrice.setScale(2, RoundingMode.HALF_UP));
        m.put("conversionRate", orders == 0 ? 0.0
                : BigDecimal.valueOf(completed * 100.0 / orders).setScale(1, RoundingMode.HALF_UP).doubleValue());
        m.put("soldRate", books == 0 ? 0.0
                : BigDecimal.valueOf(sold * 100.0 / books).setScale(1, RoundingMode.HALF_UP).doubleValue());
        return m;
    }

    public Map<String, Object> charts() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categoryDist", bookMapper.statByCategory());
        m.put("majorDist", bookMapper.statByMajor());
        m.put("priceRange", bookMapper.statByPriceRange());
        m.put("dailyBooks", bookMapper.statDailyNewBooks());
        m.put("dailyOrders", orderMapper.statDailyOrders());
        return m;
    }
}
