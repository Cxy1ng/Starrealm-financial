package com.cxy1ng.utils;

import org.springframework.web.client.RestTemplate;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.math.BigDecimal;

public class CurrencyUtil {

    private static final RestTemplate restTemplate = new RestTemplate();

    public static BigDecimal convertToCNY(String fromCurrency, BigDecimal amount) {
        String url = String.format(
                "https://v2.xxapi.cn/api/exchange?from=%s&to=CNY&amount=%s",
                fromCurrency,
                amount.toPlainString()
        );

        String response = restTemplate.getForObject(url, String.class);

        JSONObject json = JSONUtil.parseObj(response);
        if (json.getInt("code") != 200) {
            throw new RuntimeException("汇率转换失败：" + json.getStr("msg"));
        }

        return json.getJSONObject("data").getBigDecimal("result");
    }
}
