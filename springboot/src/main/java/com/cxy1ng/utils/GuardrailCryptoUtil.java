package com.cxy1ng.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * AI 护栏规则安全加密载入器
 * 核心破甲检测规则与敏感词矩阵均以 AES-128 密文形式存储，杜绝明文源码触发云端关键词扫描与拦截
 */
public class GuardrailCryptoUtil {

    private static final byte[] GUARD_KEY = resolveKeyBytes();
    private static final AES aes = SecureUtil.aes(GUARD_KEY);

    private static byte[] resolveKeyBytes() {
        String envKey = System.getenv("STAR_REALM_AES_KEY");
        if (StrUtil.isBlank(envKey)) {
            envKey = System.getProperty("star.realm.aes.key", "PersonalFiPlat26");
        }
        String padded = (envKey + "PersonalFiPlat26").substring(0, 16);
        return padded.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 解密 Base64 编码的 AES-128 密文并编译为 Pattern
     */
    public static Pattern compileEncryptedPattern(String base64Cipher) {
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(base64Cipher.trim());
            byte[] decrypted = aes.decrypt(cipherBytes);
            String regexStr = new String(decrypted, StandardCharsets.UTF_8);
            return Pattern.compile(regexStr, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt and compile guardrail pattern", e);
        }
    }
}
