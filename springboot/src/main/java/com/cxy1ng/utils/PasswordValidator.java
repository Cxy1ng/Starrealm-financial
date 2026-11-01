package com.cxy1ng.utils;

import java.security.SecureRandom;
import java.util.regex.Pattern;

public class PasswordValidator {

    /**
     * 强密码正则：8-20位，必须包含大写字母、小写字母、数字和至少一个特殊符号
     */
    private static final String PWD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?`~])[A-Za-z\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?`~]{8,20}$";
    private static final Pattern PWD_PATTERN = Pattern.compile(PWD_REGEX);

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()_+-=";
    private static final String ALL_CHARS = LOWER + UPPER + DIGITS + SYMBOLS;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static boolean isValid(String password) {
        if (password == null) return false;
        return PWD_PATTERN.matcher(password).matches();
    }

    /**
     * 生成符合 8-20 位、包含大小写、数字与特殊符号的强随机临时密码（默认 12 位）
     */
    public static String generateStrongTempPassword() {
        int length = 12;
        char[] pwd = new char[length];
        // 保证各类字符至少出现一个
        pwd[0] = LOWER.charAt(RANDOM.nextInt(LOWER.length()));
        pwd[1] = UPPER.charAt(RANDOM.nextInt(UPPER.length()));
        pwd[2] = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
        pwd[3] = SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length()));
        for (int i = 4; i < length; i++) {
            pwd[i] = ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length()));
        }
        // 洗牌 Fisher-Yates
        for (int i = length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = pwd[i];
            pwd[i] = pwd[j];
            pwd[j] = temp;
        }
        return new String(pwd);
    }
}
