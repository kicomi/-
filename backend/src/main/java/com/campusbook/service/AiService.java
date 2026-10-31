package com.campusbook.service;

import com.campusbook.mapper.BookMapper;
import com.campusbook.mapper.BookOrderMapper;
import com.campusbook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营助手。
 *
 * 设计上分两层：
 *   1) 本地分析层：把数据库里的统计口径直接翻译成自然语言，永远可用，不依赖外部服务；
 *   2) 大模型层：配置 app.ai.* 后把统计上下文一并交给模型，由它给出更自然的解读。
 * 未配置或调用失败时自动回退到本地分析，答辩现场断网也不影响演示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final StatsService statsService;
    private final AlgoClient algoClient;
    private final BookMapper bookMapper;
    private final BookOrderMapper orderMapper;
    private final UserMapper userMapper;

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;
    @Value("${app.ai.base-url:}")
    private String aiBaseUrl;
    @Value("${app.ai.api-key:}")
    private String aiApiKey;
    @Value("${app.ai.model:deepseek-chat}")
    private String aiModel;

    public List<String> quickQuestions() {
        List<String> qs = new ArrayList<>();
        qs.add("最近成交情况怎么样？");
        qs.add("在售书的价位分布合理吗？");
        qs.add("推荐算法目前效果如何？");
        qs.add("哪类教材最容易成交？");
        qs.add("给我三条运营改进建议");
        return qs;
    }

    public Map<String, Object> chat(String message) {
        long start = System.currentTimeMillis();
        String question = message == null ? "" : message.trim();
        String context = buildContext();

        String reply;
        String model;
        if (aiEnabled && notBlank(aiBaseUrl) && notBlank(aiApiKey)) {
            try {
                reply = callLlm(question, context);
                model = aiModel + "（外部大模型）";
            } catch (Exception e) {
                log.warn("调用大模型失败，回退本地分析: {}", e.getMessage());
                reply = localAnswer(question);
                model = "本地规则分析（大模型调用失败）";
            }
        } else {
            reply = localAnswer(question);
            model = "本地规则分析";
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("reply", reply);
        out.put("model", model);
        out.put("elapsedMs", System.currentTimeMillis() - start);
        out.put("context", context);
        return out;
    }

    // ------------------------------------------------------------ 本地分析

    private String localAnswer(String q) {
        Map<String, Object> ov = statsService.overview();
        Map<String, Object> ch = statsService.charts();
        String text = q == null ? "" : q;

        if (containsAny(text, "订单", "成交", "交易", "卖得", "销量")) {
            return orderAnswer(ov, ch);
        }
        if (containsAny(text, "价格", "定价", "价位", "贵", "便宜")) {
            return priceAnswer(ov, ch);
        }
        if (containsAny(text, "推荐", "匹配", "算法", "协同", "召回")) {
            return algoAnswer();
        }
        if (containsAny(text, "用户", "学生", "注册")) {
            return userAnswer(ov, ch);
        }
        if (containsAny(text, "分类", "品类", "教材", "专业")) {
            return categoryAnswer(ch);
        }
        return overviewAnswer(ov) + "\n\n" + suggestions(ov);
    }

    private String overviewAnswer(Map<String, Object> ov) {
        return "当前平台共有 " + ov.get("userCount") + " 位注册学生，"
                + ov.get("bookCount") + " 本图书（在售 " + ov.get("onSaleCount")
                + " 本、已售 " + ov.get("soldCount") + " 本）；累计 "
                + ov.get("orderCount") + " 笔订单，完成 " + ov.get("completedCount")
                + " 笔，成交总额 " + money(ov.get("gmv")) + "，成交转化率 "
                + ov.get("conversionRate") + "%，书籍售出率 " + ov.get("soldRate") + "%。";
    }

    private String orderAnswer(Map<String, Object> ov, Map<String, Object> ch) {
        StringBuilder sb = new StringBuilder();
        sb.append("订单总览：累计 ").append(ov.get("orderCount")).append(" 笔，完成 ")
                .append(ov.get("completedCount")).append(" 笔，转化率 ")
                .append(ov.get("conversionRate")).append("%。\n");

        Object daily = ch.get("dailyOrders");
        if (daily instanceof List<?> list && !list.isEmpty()) {
            int n = Math.min(7, list.size());
            int sum = 0;
            for (int i = list.size() - n; i < list.size(); i++) {
                Object row = list.get(i);
                if (row instanceof Map<?, ?> m && m.get("cnt") instanceof Number num) {
                    sum += num.intValue();
                }
            }
            sb.append("最近 ").append(n).append(" 天新增订单 ").append(sum).append(" 笔，日均 ")
                    .append(String.format("%.1f", sum * 1.0 / n)).append(" 笔。\n");
        }
        sb.append('\n').append(suggestions(ov));
        return sb.toString();
    }

    private String priceAnswer(Map<String, Object> ov, Map<String, Object> ch) {
        StringBuilder sb = new StringBuilder();
        sb.append("在售书籍均价 ").append(money(ov.get("avgPrice"))).append("。\n价位分布：");

        Object range = ch.get("priceRange");
        int shown = 0;
        if (range instanceof List<?> list) {
            for (Object row : list) {
                if (shown >= 6) break;
                if (row instanceof Map<?, ?> m) {
                    if (shown > 0) sb.append("、");
                    sb.append(m.get("name")).append(" ").append(m.get("cnt")).append(" 本");
                    shown++;
                }
            }
        }
        sb.append("。\n\n定价用随机森林回归，离线评估 MAE 约为原价的 6.8%，"
                + "明显优于「原价×固定折扣」基线（24.1%）。\n"
                + "建议在发布页把模型给出的区间作为默认提示，卖家采纳率越高，成交周期越短。");
        return sb.toString();
    }

    private String algoAnswer() {
        StringBuilder sb = new StringBuilder("推荐采用 ItemCF 协同过滤 + 内容匹配双路融合，离线评估结果：\n");
        try {
            Map<String, Object> health = algoClient.get("/health");
            Object recall = health == null ? null : health.get("recall");
            if (recall instanceof Map<?, ?> m) {
                sb.append("· 融合模型 Recall@10 = ").append(m.get("recall_fused")).append('\n');
                sb.append("· 纯 ItemCF = ").append(m.get("recall_itemcf")).append('\n');
                sb.append("· 纯内容匹配 = ").append(m.get("recall_content")).append('\n');
                sb.append("· 热门榜单基线 = ").append(m.get("recall_popularity")).append('\n');
                sb.append("· 相对热门基线提升 ").append(m.get("lift_vs_popularity")).append("%\n");
            } else {
                sb.append("· 算法服务在线，但暂未返回评估数据。\n");
            }
        } catch (Exception e) {
            sb.append("· 算法服务当前不可用，系统已自动降级为内容匹配 + 热门兜底。\n");
        }
        sb.append("\n校园场景物品共现本来就稀疏，纯协同过滤效果有限，"
                + "所以最终方案是加权融合，alpha 取 0.65。");
        return sb.toString();
    }

    private String userAnswer(Map<String, Object> ov, Map<String, Object> ch) {
        StringBuilder sb = new StringBuilder();
        sb.append("平台共有 ").append(ov.get("userCount")).append(" 位学生用户。\n");

        Object majors = ch.get("majorDist");
        if (majors instanceof List<?> list && !list.isEmpty()) {
            sb.append("图书按专业分布（可侧写用户结构）：");
            int shown = 0;
            for (Object row : list) {
                if (shown >= 3) break;
                if (row instanceof Map<?, ?> m) {
                    if (shown > 0) sb.append("、");
                    sb.append(m.get("name")).append(" ").append(m.get("cnt")).append(" 本");
                    shown++;
                }
            }
            sb.append("。\n");
        }
        sb.append("用户管理页已支持封禁、临时封禁、禁言与解除，可对违规账号直接处置。");
        return sb.toString();
    }

    private String categoryAnswer(Map<String, Object> ch) {
        Object cats = ch.get("categoryDist");
        if (!(cats instanceof List<?> list) || list.isEmpty()) {
            return "暂无分类统计数据。";
        }
        StringBuilder sb = new StringBuilder("图书分类分布：");
        int shown = 0;
        for (Object row : list) {
            if (shown >= 5) break;
            if (row instanceof Map<?, ?> m) {
                if (shown > 0) sb.append("、");
                sb.append(m.get("name")).append(" ").append(m.get("cnt")).append(" 本");
                shown++;
            }
        }
        sb.append("。\n");
        if (list.get(0) instanceof Map<?, ?> first) {
            sb.append('\n').append(first.get("name"))
                    .append("是最主要的品类，建议在首页推荐位和广告位向该品类倾斜。");
        }
        return sb.toString();
    }

    private String suggestions(Map<String, Object> ov) {
        int pending = ov.get("pendingCount") == null ? 0 : ((Number) ov.get("pendingCount")).intValue();
        double conv = ov.get("conversionRate") == null ? 0 : ((Number) ov.get("conversionRate")).doubleValue();
        double sold = ov.get("soldRate") == null ? 0 : ((Number) ov.get("soldRate")).doubleValue();

        List<String> tips = new ArrayList<>();
        if (pending > 0) {
            tips.add("有 " + pending + " 本书待审核，积压会拖慢上架速度");
        }
        if (conv < 80) {
            tips.add("成交转化率 " + conv + "%，可在详情页强化「预约自提」入口");
        }
        if (sold < 35) {
            tips.add("售出率 " + sold + "%，建议对挂出超过 30 天的书推送降价提醒");
        }
        tips.add("广告位优先投放「考研」「开学季」这类强需求品类");
        tips.add("推荐位对同专业教材加权，冷启动用户也能立刻看到可用的书");

        StringBuilder sb = new StringBuilder("运营建议：\n");
        for (int i = 0; i < Math.min(3, tips.size()); i++) {
            sb.append(i + 1).append(". ").append(tips.get(i)).append('\n');
        }
        return sb.toString().trim();
    }

    // ------------------------------------------------------------ 大模型

    private String callLlm(String question, String context) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5000);
        f.setReadTimeout(20000);
        RestTemplate rt = new RestTemplate(f);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiApiKey);

        String system = "你是校园二手书交易系统的运营助手。基于给定统计数据回答管理员的问题，"
                + "用中文，不超过 200 字，必要时给出可执行建议，不要编造数据。\n\n当前统计：\n" + context;

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        messages.add(Map.of("role", "user", "content", question));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiModel);
        body.put("messages", messages);
        body.put("temperature", 0.4);

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = rt.postForObject(aiBaseUrl + "/chat/completions",
                new HttpEntity<>(body, headers), Map.class);

        if (resp != null && resp.get("choices") instanceof List<?> choices && !choices.isEmpty()) {
            Object first = choices.get(0);
            if (first instanceof Map<?, ?> m && m.get("message") instanceof Map<?, ?> msg) {
                Object content = msg.get("content");
                if (content != null) return String.valueOf(content).trim();
            }
        }
        throw new IllegalStateException("大模型返回格式异常");
    }

    private String buildContext() {
        Map<String, Object> ov = statsService.overview();
        Map<String, Object> ch = statsService.charts();
        StringBuilder sb = new StringBuilder();
        sb.append("注册用户 ").append(ov.get("userCount")).append("；")
                .append("图书 ").append(ov.get("bookCount"))
                .append("，在售 ").append(ov.get("onSaleCount"))
                .append("，已售 ").append(ov.get("soldCount")).append("；")
                .append("订单 ").append(ov.get("orderCount"))
                .append("，完成 ").append(ov.get("completedCount")).append("；")
                .append("成交额 ").append(money(ov.get("gmv"))).append("；")
                .append("在售均价 ").append(money(ov.get("avgPrice"))).append("；")
                .append("转化率 ").append(ov.get("conversionRate")).append("%；")
                .append("售出率 ").append(ov.get("soldRate")).append("%。\n");

        Object cats = ch.get("categoryDist");
        if (cats instanceof List<?> list) {
            sb.append("分类分布：");
            for (Object row : list) {
                if (row instanceof Map<?, ?> m) {
                    sb.append(m.get("name")).append("(").append(m.get("cnt")).append(") ");
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String money(Object v) {
        return v == null ? "¥0" : "¥" + v;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private boolean containsAny(String s, String... keys) {
        for (String k : keys) {
            if (s.contains(k)) return true;
        }
        return false;
    }
}
