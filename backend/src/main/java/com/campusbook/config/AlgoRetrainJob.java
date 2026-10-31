package com.campusbook.config;

import com.campusbook.service.AlgoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 算法在线学习：每天凌晨用最新产生的订单/浏览数据重训一次模型。
 * 校园二手书的成交是低频事件，每天一次足够，也不会给算法服务造成压力。
 * 管理员也可以在后台手动触发（/api/admin/algo/retrain）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlgoRetrainJob {

    private final AlgoClient algoClient;

    @Scheduled(cron = "0 30 3 * * ?")
    public void dailyRetrain() {
        log.info("开始每日模型重训 {}", LocalDateTime.now());
        var r = algoClient.reload();
        log.info("模型重训完成: {}", r.get("status"));
    }
}
