package com.cxy1ng.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.cxy1ng.common.Constants;
import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.service.AdminService;
import com.cxy1ng.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

@Component
public class TokenUtils {

    private static final Logger log = LoggerFactory.getLogger(TokenUtils.class);

    private static AdminService staticAdminService;
    private static UserService staticUserService;

    @Resource
    AdminService adminService;

    @Resource
    UserService userService;

    private static final int DEFAULT_TOKEN_EXPIRE_HOURS = 12;
    private static final int REMEMBER_ME_TOKEN_EXPIRE_HOURS = 24 * 7;

    @PostConstruct
    public void setUserService() {
        staticAdminService = adminService;
        staticUserService = userService;
    }

    public static String createToken(String data, String sign) {
        return createToken(data, sign, DEFAULT_TOKEN_EXPIRE_HOURS);
    }

    /**
     * 按是否勾选“7天内自动登录”签发对应有效期的 JWT（未勾选 12 小时，勾选 7 天）
     */
    public static String createToken(String data, String sign, boolean rememberMe) {
        return createToken(data, sign, rememberMe ? REMEMBER_ME_TOKEN_EXPIRE_HOURS : DEFAULT_TOKEN_EXPIRE_HOURS);
    }

    public static String createToken(String data, String sign, int expireHours) {
        int hours = expireHours > 0 ? expireHours : DEFAULT_TOKEN_EXPIRE_HOURS;
        return JWT.create().withAudience(data)
                .withExpiresAt(DateUtil.offsetHour(new Date(), hours))
                .sign(Algorithm.HMAC256(sign));
    }

    public static Account getCurrentUser() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String token = request.getHeader(Constants.TOKEN);
            if (ObjectUtil.isEmpty(token)) {
                token = request.getParameter(Constants.TOKEN);
            }
            if (ObjectUtil.isNotEmpty(token)) {
                String userInfo = JWT.decode(token).getAudience().get(0);
                String userId = userInfo.split("-")[0];
                int perm = Integer.parseInt(userInfo.split("-")[1]);
                String userRole = userInfo.split("-")[2];
                PermEnum role = PermEnum.getRoleByPerm(perm);
                if (userRole.equals(String.valueOf(role))) {
                    if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
                        return staticAdminService.selectById(Integer.valueOf(userId));
                    } else if (role.equals(PermEnum.USER)) {
                        return staticUserService.selectById(Integer.valueOf(userId));
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取当前用户信息出错", e);
        }
        return new Account();
    }
}
