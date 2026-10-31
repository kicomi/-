package com.campusbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 调用 Python 算法服务（回归定价 / 协同过滤）。
 * 服务不可用时由调用方走降级策略，保证答辩演示不中断。
 */
@Slf4j
@Component
public class AlgoClient {

    private final RestTemplate restTemplate;
    /** 重训要跑完整的训练流程，几十秒都可能，必须单独给一个长超时的客户端 */
    private final RestTemplate longRestTemplate;
    private final String baseUrl;

    public AlgoClient(@Value("${app.algorithm.base-url}") String baseUrl,
                      @Value("${app.algorithm.timeout-ms:1500}") int timeout,
                      @Value("${app.algorithm.retrain-timeout-ms:180000}") int retrainTimeout) {
        this.baseUrl = baseUrl;

        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(timeout);
        f.setReadTimeout(timeout);
        this.restTemplate = new RestTemplate(f);

        SimpleClientHttpRequestFactory f2 = new SimpleClientHttpRequestFactory();
        f2.setConnectTimeout(timeout);
        f2.setReadTimeout(retrainTimeout);
        this.longRestTemplate = new RestTemplate(f2);
    }

    public boolean isAlive() {
        try {
            restTemplate.getForObject(baseUrl + "/health", Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(baseUrl + path, entity, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String path) {
        return restTemplate.getForObject(baseUrl + path, Map.class);
    }

    /** 让算法服务用最新数据重训并热替换模型 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> reload() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> r = longRestTemplate.postForObject(
                    baseUrl + "/reload", new HttpEntity<>(Map.of(), headers), Map.class);
            if (r == null) r = new java.util.HashMap<>();
            r.putIfAbsent("status", "reloaded");
            return r;
        } catch (Exception e) {
            Map<String, Object> err = new java.util.HashMap<>();
            err.put("status", "failed");
            err.put("message", e.getMessage());
            return err;
        }
    }

    public String toJson(Object o) {
        try {
            return new ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}
