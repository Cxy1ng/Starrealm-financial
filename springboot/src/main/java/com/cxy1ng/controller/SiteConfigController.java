package com.cxy1ng.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cxy1ng.common.Result;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.service.SiteConfigService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
public class SiteConfigController {

    @Resource
    private SiteConfigService siteConfigService;

    /**
     * 管理员读取全部网站公共配置（脱敏返回）
     */
    @GetMapping("/siteConfig/selectAll")
    public Result selectAll() {
        return Result.success(siteConfigService.selectAllMasked());
    }

    /**
     * 管理员加密保存网站公共配置
     */
    @PutMapping("/siteConfig/saveBatch")
    public Result saveBatch(@RequestBody Map<String, String> payload) {
        siteConfigService.saveBatch(payload);
        return Result.success();
    }

    /**
     * 实时汇率查询接口（仅查询单价汇率，前端本地计算具体金额）
     * 调用示例：curl -H "Authorization: Bearer YOUR_KEY" "https://api.finexly.com/v1/rate?from=USD&to=EUR"
     */
    @GetMapping("/exchange/rate")
    public Result getExchangeRate(@RequestParam String from, @RequestParam String to) {
        String base = from.trim().toUpperCase();
        String target = to.trim().toUpperCase();

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("from", base);
        resultData.put("to", target);

        if (base.equals(target)) {
            resultData.put("rate", 1.0);
            resultData.put("updateTime", DateUtil.now());
            return Result.success(resultData);
        }

        String finexlyKey = siteConfigService.getPlainValue("finexly_api_key", null);
        if (StrUtil.isBlank(finexlyKey)) {
            return Result.error(ResultCodeEnum.PARAM_ERROR.code,
                    "网站尚未配置 Finexly 汇率 API Key，请联系管理员前往【平台配置 -> 网站公共配置】填写密钥");
        }

        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl("https://api.finexly.com")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + finexlyKey)
                    .build();

            String rawJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/rate")
                            .queryParam("from", base)
                            .queryParam("to", target)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            JSONObject json = JSONUtil.parseObj(rawJson);
            Double rate = extractRateFromResponse(json, target);
            if (rate == null || rate <= 0) {
                return Result.error(ResultCodeEnum.SYSTEM_ERROR.code, "汇率解析失败，上游响应: " + rawJson);
            }

            String updatedAt = json.getStr("updated_at",
                    json.getStr("timestamp",
                            json.getStr("date", DateUtil.now())));

            resultData.put("rate", rate);
            resultData.put("updateTime", updatedAt);
            return Result.success(resultData);
        } catch (Exception e) {
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.code, "Finexly 汇率接口调用失败：" + e.getMessage());
        }
    }

    /**
     * 兼容提取 Finexly /v1/rate 返回结构中的汇率数值
     */
    private Double extractRateFromResponse(JSONObject json, String target) {
        if (json == null) return null;
        if (json.containsKey("rate") && json.getDouble("rate") != null) {
            return json.getDouble("rate");
        }
        if (json.containsKey("result") && json.getDouble("result") != null) {
            return json.getDouble("result");
        }
        if (json.containsKey("data")) {
            Object dataObj = json.get("data");
            if (dataObj instanceof Number num) {
                return num.doubleValue();
            }
            JSONObject dataJson = json.getJSONObject("data");
            if (dataJson != null) {
                if (dataJson.getDouble("rate") != null) return dataJson.getDouble("rate");
                if (dataJson.getDouble(target) != null) return dataJson.getDouble(target);
            }
        }
        if (json.containsKey("rates")) {
            JSONObject ratesJson = json.getJSONObject("rates");
            if (ratesJson != null && ratesJson.getDouble(target) != null) {
                return ratesJson.getDouble(target);
            }
        }
        return null;
    }
}
