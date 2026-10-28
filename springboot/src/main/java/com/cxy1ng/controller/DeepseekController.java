package com.cxy1ng.controller;

import cn.hutool.core.util.StrUtil;
import com.cxy1ng.common.Result;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.exception.CustomException;
import com.cxy1ng.service.SiteConfigService;
import com.cxy1ng.utils.ApiKeyCryptoUtil;
import com.cxy1ng.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import cn.hutool.json.JSONUtil;

@RestController
@RequestMapping("/deepseek")
public class DeepseekController {

    @Value("${deepseek.api-key:}")
    private String fallbackApiKey;

    @Resource
    private SiteConfigService siteConfigService;

    /**
     * 优先读取当前登录用户的个人加密 API Key；若未配置，则回退读取管理员在【网站配置】中设置的公共备用 Key
     */
    private String resolvePlainApiKey() {
        Account currentUser = TokenUtils.getCurrentUser();
        String plainApiKey = null;
        if (currentUser != null && StrUtil.isNotBlank(currentUser.getApiKey())) {
            plainApiKey = ApiKeyCryptoUtil.decrypt(currentUser.getApiKey());
        }
        if (StrUtil.isBlank(plainApiKey)) {
            plainApiKey = siteConfigService.getPlainValue("deepseek_public_api_key", null);
        }
        if (StrUtil.isBlank(plainApiKey) && StrUtil.isNotBlank(fallbackApiKey)) {
            plainApiKey = fallbackApiKey.trim();
        }
        return plainApiKey;
    }

    private String resolveBaseUrl() {
        String configured = siteConfigService.getPlainValue("deepseek_base_url", "https://api.deepseek.com");
        if (StrUtil.isBlank(configured)) {
            return "https://api.deepseek.com";
        }
        String trimmed = configured.trim().replaceAll("/+$", "");
        // 若用户填写了带 /v1 的地址，统一规范化，避免拼出 /v1/v1
        if (trimmed.endsWith("/v1")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed;
    }

    /**
     * 检测当前 DeepSeek API 连通性状态
     * 使用与 OpenAI/DeepSeek 完全兼容的 GET /v1/models 端点
     */
    @GetMapping("/status")
    public Result checkStatus() {
        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("model", "Deepseek V4.1 Flash");
        statusMap.put("build", "1017");

        String plainApiKey = resolvePlainApiKey();
        if (StrUtil.isBlank(plainApiKey)) {
            statusMap.put("connected", false);
            statusMap.put("state", "unconfigured");
            statusMap.put("message", "未配置 API 密钥");
            return Result.success(statusMap);
        }

        String baseUrl = resolveBaseUrl();
        long start = System.currentTimeMillis();
        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + plainApiKey)
                    .build();

            webClient.get()
                    .uri("/v1/models")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(8))
                    .block();

            long latency = System.currentTimeMillis() - start;
            statusMap.put("connected", true);
            statusMap.put("state", "online");
            statusMap.put("latencyMs", latency);
            statusMap.put("message", "已连接 · " + latency + "ms");
            return Result.success(statusMap);
        } catch (Exception e) {
            statusMap.put("connected", false);
            statusMap.put("state", "error");
            statusMap.put("message", "密钥无效或网络不可达");
            return Result.success(statusMap);
        }
    }

    /**
     * 流式对话接口 (使用标准 SseEmitter，在主请求线程先解析完用户 Token 与 API Key，避免 Spring MVC 异步重派发丢失 RequestContext 或触发二次拦截)
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> payload) {
        String plainApiKey = resolvePlainApiKey();
        if (StrUtil.isBlank(plainApiKey)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR.code, "尚未配置 DeepSeek API Key，请前往【个人设置】或【网站配置】填写密钥");
        }
        String baseUrl = resolveBaseUrl();

        Object rawMessages = payload.get("messages");
        List<?> messages;
        if (rawMessages instanceof List<?> list && !list.isEmpty()) {
            messages = list;
        } else {
            messages = List.of(
                    Map.of("role", "system", "content", "你是智汇账本的专属智能理财与汇率客服助手（Deepseek V4.1 Flash 1017），回答简洁、专业、亲切，支持 Markdown 格式。"),
                    Map.of("role", "user", "content", String.valueOf(payload.getOrDefault("prompt", "")))
            );
        }

        Map<String, Object> requestBody = Map.of(
                "model", "deepseek-chat",
                "messages", messages,
                "stream", true,
                "temperature", 0.8,
                "max_tokens", 1536
        );

        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> {
            HttpURLConnection conn = null;
            try {
                URI uri = URI.create(baseUrl + "/v1/chat/completions");
                conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(90_000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + plainApiKey);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "text/event-stream");

                byte[] jsonBytes = JSONUtil.toJsonStr(requestBody).getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBytes);
                    os.flush();
                }

                int status = conn.getResponseCode();
                if (status != 200) {
                    try (BufferedReader errReader = new BufferedReader(new InputStreamReader(
                            conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder errSb = new StringBuilder();
                        String line;
                        while ((line = errReader.readLine()) != null) {
                            errSb.append(line);
                        }
                        Map<String, Object> fakeDelta = Map.of(
                                "choices", List.of(Map.of("delta", Map.of("content", "**DeepSeek 接口返回错误 (HTTP " + status + ")**：" + errSb)))
                        );
                        emitter.send(SseEmitter.event().data(JSONUtil.toJsonStr(fakeDelta)));
                    }
                    emitter.send(SseEmitter.event().data("[DONE]"));
                    emitter.complete();
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty()) continue;
                        if (trimmed.startsWith("data:")) {
                            String dataPart = trimmed.substring(5).trim();
                            emitter.send(SseEmitter.event().data(dataPart));
                            if ("[DONE]".equals(dataPart)) {
                                break;
                            }
                        }
                    }
                }
                emitter.complete();
            } catch (Exception ex) {
                try {
                    Map<String, Object> errChunk = Map.of(
                            "choices", List.of(Map.of("delta", Map.of("content", "\n\n**连接异常**：" + ex.getMessage())))
                    );
                    emitter.send(SseEmitter.event().data(JSONUtil.toJsonStr(errChunk)));
                    emitter.complete();
                } catch (Exception ignored) {
                    emitter.completeWithError(ex);
                }
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });

        return emitter;
    }

    @PostMapping("/analyze")
    public String analyze(@RequestBody Map<String, Object> payload) {
        String plainApiKey = resolvePlainApiKey();
        if (StrUtil.isBlank(plainApiKey)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR.code, "尚未配置 DeepSeek API Key，请先前往【个人设置】页面填写并加密保存您的 API 密钥");
        }
        String baseUrl = resolveBaseUrl();

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + plainApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(
                        Map.of("role", "user", "content", String.valueOf(payload.getOrDefault("prompt", "")))
                ),
                "temperature", 1.0,
                "max_tokens", 1024
        );

        return webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
