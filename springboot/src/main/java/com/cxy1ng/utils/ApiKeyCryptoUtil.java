package com.cxy1ng.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.StandardCharsets;

/**
 * DeepSeek API Key 对称加密/解密工具类 (AES)
 */
public class ApiKeyCryptoUtil {

    // 16字节 (128位) 密钥：优先读取环境变量 STAR_REALM_AES_KEY（开源部署时可通过环境变量注入私有密钥）
    private static final byte[] SECRET_KEY = resolveAesKeyBytes();
    private static final String PREFIX = "ENC(";
    private static final String SUFFIX = ")";

    private static final AES aes = SecureUtil.aes(SECRET_KEY);

    private static byte[] resolveAesKeyBytes() {
        String envKey = System.getenv("STAR_REALM_AES_KEY");
        if (StrUtil.isBlank(envKey)) {
            envKey = System.getProperty("star.realm.aes.key", "PersonalFiPlat26");
        }
        String padded = (envKey + "PersonalFiPlat26").substring(0, 16);
        return padded.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 加密明文 API Key
     */
    public static String encrypt(String plainKey) {
        if (StrUtil.isBlank(plainKey)) {
            return null;
        }
        String trimmed = plainKey.trim();
        // 如果已经是加密格式或脱敏展示格式，不再重复加密
        if (trimmed.startsWith(PREFIX) && trimmed.endsWith(SUFFIX)) {
            return trimmed;
        }
        return PREFIX + aes.encryptHex(trimmed) + SUFFIX;
    }

    /**
     * 解密存储的密文 API Key
     */
    public static String decrypt(String storedKey) {
        if (StrUtil.isBlank(storedKey)) {
            return null;
        }
        String trimmed = storedKey.trim();
        if (trimmed.startsWith(PREFIX) && trimmed.endsWith(SUFFIX)) {
            String hex = trimmed.substring(PREFIX.length(), trimmed.length() - SUFFIX.length());
            try {
                return aes.decryptStr(hex, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return null;
            }
        }
        return trimmed;
    }

    /**
     * 生成前端脱敏展示的字符串，例如 sk-6ea1****8623
     */
    public static String mask(String storedKey) {
        String plain = decrypt(storedKey);
        if (StrUtil.isBlank(plain)) {
            return "";
        }
        if (plain.length() <= 8) {
            return "****";
        }
        return plain.substring(0, 6) + "****" + plain.substring(plain.length() - 4);
    }
}
