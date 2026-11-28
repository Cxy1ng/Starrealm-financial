package com.cxy1ng;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.cxy1ng.utils.TokenUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 签发时长（默认 12 小时 vs 勾选 7 天免登录）单元测试
 */
class TokenUtilsTest {

    @Test
    @DisplayName("默认及未勾选记住我签发 12 小时有效期，勾选 7 天免登录签发 168 小时有效期")
    void testCreateTokenExpirationByRememberMe() {
        String payload = "1-1-USER";
        String secret = "Pwd@123456";
        long now = System.currentTimeMillis();

        String defaultToken = TokenUtils.createToken(payload, secret);
        String sessionToken = TokenUtils.createToken(payload, secret, false);
        String rememberToken = TokenUtils.createToken(payload, secret, true);

        DecodedJWT defaultJwt = JWT.require(Algorithm.HMAC256(secret)).build().verify(defaultToken);
        DecodedJWT sessionJwt = JWT.require(Algorithm.HMAC256(secret)).build().verify(sessionToken);
        DecodedJWT rememberJwt = JWT.require(Algorithm.HMAC256(secret)).build().verify(rememberToken);

        long twelveHoursMs = 12L * 3600 * 1000;
        long sevenDaysMs = 7L * 24 * 3600 * 1000;

        assertExpirationNear(defaultJwt.getExpiresAt(), now + twelveHoursMs);
        assertExpirationNear(sessionJwt.getExpiresAt(), now + twelveHoursMs);
        assertExpirationNear(rememberJwt.getExpiresAt(), now + sevenDaysMs);
    }

    private void assertExpirationNear(Date expiresAt, long expectedMs) {
        assertNotNull(expiresAt);
        long diff = Math.abs(expiresAt.getTime() - expectedMs);
        assertTrue(diff < 10_000L, "过期时间偏差过大: " + diff + "ms");
    }
}
