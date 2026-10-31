package com.campusbook.service;

import com.campusbook.dto.PriceSuggestReq;
import com.campusbook.dto.PriceSuggestResp;
import com.campusbook.entity.PriceRecord;
import com.campusbook.mapper.PriceRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能定价：优先调用 Python 回归模型服务；服务不可用时降级为可解释的规则估算，
 * 保证答辩现场不会因为算法服务没启动而报错。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {

    private final AlgoClient algoClient;
    private final PriceRecordMapper priceRecordMapper;

    public PriceSuggestResp suggest(PriceSuggestReq req) {
        PriceSuggestResp resp;
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("originalPrice", req.getOriginalPrice() == null ? 0 : req.getOriginalPrice().doubleValue());
            body.put("conditionLevel", nz(req.getConditionLevel(), 3));
            body.put("wearLevel", nz(req.getWearLevel(), 3));
            body.put("noteFlag", nz(req.getNoteFlag(), 0));
            body.put("publishYear", req.getPublishYear() == null ? Year.now().getValue() - 3 : req.getPublishYear());
            body.put("listingCount", nz(req.getListingCount(), 1));
            body.put("soldCount30d", nz(req.getSoldCount30d(), 0));
            body.put("sellerHistory", req.getSellerHistory() == null
                    ? (req.getOriginalPrice() == null ? 0 : req.getOriginalPrice().doubleValue() * 0.4)
                    : req.getSellerHistory().doubleValue());
            body.put("categoryId", req.getCategoryId() == null ? 1 : req.getCategoryId());

            Map<String, Object> r = algoClient.post("/predict", body);
            if (r == null || r.get("suggested_price") == null) throw new IllegalStateException("算法服务返回为空");

            resp = new PriceSuggestResp();
            resp.setSuggestedPrice(dec(r.get("suggested_price")));
            resp.setPriceLow(dec(r.get("price_low")));
            resp.setPriceHigh(dec(r.get("price_high")));
            resp.setModel(str(r.get("model"), "RandomForestRegressor"));
            resp.setModelVersion(str(r.get("model_version"), "v1"));
            resp.setConfidence(r.get("confidence") == null ? null : ((Number) r.get("confidence")).doubleValue());
            resp.setDegraded(false);
            Object contrib = r.get("contributions");
            if (contrib instanceof Map<?, ?> m) {
                Map<String, Double> cm = new HashMap<>();
                m.forEach((k, v) -> {
                    if (v instanceof Number n) cm.put(String.valueOf(k), n.doubleValue());
                });
                resp.setContributions(cm);
            }
        } catch (Exception e) {
            log.warn("算法服务不可用，使用本地降级估算: {}", e.getMessage());
            resp = fallback(req);
        }

        resp.setBaselinePrice(baseline(req.getOriginalPrice()));
        resp.setTips(buildTips(req, resp));

        saveRecord(req, resp);
        return resp;
    }

    /** 降级：与训练数据同构的可解释折扣模型 */
    private PriceSuggestResp fallback(PriceSuggestReq req) {
        double orig = req.getOriginalPrice() == null ? 0 : req.getOriginalPrice().doubleValue();
        int cond = nz(req.getConditionLevel(), 3);
        int wear = nz(req.getWearLevel(), 3);
        int note = nz(req.getNoteFlag(), 0);
        int listing = nz(req.getListingCount(), 1);
        int sold30 = nz(req.getSoldCount30d(), 0);
        int year = req.getPublishYear() == null ? Year.now().getValue() - 3 : req.getPublishYear();
        double hist = req.getSellerHistory() == null ? orig * 0.4 : req.getSellerHistory().doubleValue();

        double d = 0.34 + 0.055 * cond + 0.030 * wear + (note == 1 ? 0.045 : 0)
                - 0.006 * Math.min(listing, 15) + 0.009 * Math.min(sold30, 15)
                + 0.08 * (orig == 0 ? 0 : hist / orig - 0.42)
                - 0.0018 * Math.max(0, Year.now().getValue() - year);
        long cat = req.getCategoryId() == null ? 0 : req.getCategoryId();
        if (cat == 8) d += 0.05;
        if (cat == 5) d -= 0.06;
        d = Math.max(0.10, Math.min(0.85, d));

        double p = orig * d;
        PriceSuggestResp resp = new PriceSuggestResp();
        resp.setSuggestedPrice(round(p));
        resp.setPriceLow(round(p * 0.88));
        resp.setPriceHigh(round(p * 1.12));
        resp.setModel("RuleFallback");
        resp.setModelVersion("fallback-v1");
        resp.setDegraded(true);
        Map<String, Double> c = new LinkedHashMap<>();
        c.put("品相等级", 0.055 * cond);
        c.put("新旧程度", 0.030 * wear);
        c.put("笔记划线", note == 1 ? 0.045 : 0.0);
        c.put("同款在售量", -0.006 * Math.min(listing, 15));
        c.put("近30天成交", 0.009 * Math.min(sold30, 15));
        c.put("出版年份", -0.0018 * Math.max(0, Year.now().getValue() - year));
        resp.setContributions(c);
        return resp;
    }

    private BigDecimal baseline(BigDecimal original) {
        if (original == null) return BigDecimal.ZERO;
        return original.multiply(new BigDecimal("0.4")).setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> buildTips(PriceSuggestReq req, PriceSuggestResp resp) {
        java.util.ArrayList<String> tips = new java.util.ArrayList<>();
        int cond = nz(req.getConditionLevel(), 3);
        if (cond >= 4) tips.add("品相较好，可按建议价上限挂出");
        if (cond <= 2) tips.add("品相一般，建议接近区间下界以提高成交率");
        if (nz(req.getListingCount(), 1) >= 5) tips.add("同款在售较多，适当降价能更快出手");
        if (nz(req.getSoldCount30d(), 0) >= 5) tips.add("近30天成交活跃，需求旺盛，可适度上浮");
        if (resp.getSuggestedPrice() != null && resp.getPriceHigh() != null
                && resp.getSuggestedPrice().compareTo(resp.getPriceHigh()) > 0) {
            tips.add("当前挂价高于模型区间上界，可能拉长成交周期");
        }
        if (tips.isEmpty()) tips.add("按建议价挂出成交概率最高");
        return tips;
    }

    private void saveRecord(PriceSuggestReq req, PriceSuggestResp resp) {
        try {
            PriceRecord pr = new PriceRecord();
            pr.setBookId(req.getBookId());
            pr.setFeatures(algoClient.toJson(req));
            pr.setSuggestedPrice(resp.getSuggestedPrice());
            pr.setPriceLow(resp.getPriceLow());
            pr.setPriceHigh(resp.getPriceHigh());
            pr.setModelVersion(resp.getModelVersion());
            pr.setCreatedAt(LocalDateTime.now());
            priceRecordMapper.insert(pr);
        } catch (Exception e) {
            log.warn("定价记录保存失败: {}", e.getMessage());
        }
    }

    private static int nz(Integer v, int def) { return v == null ? def : v; }

    private static BigDecimal dec(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof Number n) return new BigDecimal(n.toString()).setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal(String.valueOf(o)).setScale(2, RoundingMode.HALF_UP);
    }

    private static String str(Object o, String def) { return o == null ? def : String.valueOf(o); }

    private static BigDecimal round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
