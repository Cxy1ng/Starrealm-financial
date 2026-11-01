package com.cxy1ng.common.config;

import cn.hutool.core.util.ObjectUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.cxy1ng.common.Constants;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.entity.Admin;
import com.cxy1ng.exception.CustomException;
import com.cxy1ng.service.AdminService;
import com.cxy1ng.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    @Resource
    private AdminService adminService;

    @Resource
    UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || jakarta.servlet.DispatcherType.ASYNC.equals(request.getDispatcherType())) {
            return true;
        }
        String token = request.getHeader(Constants.TOKEN);
        if (ObjectUtil.isEmpty(token)) {
            token = request.getParameter(Constants.TOKEN);
        }
        if (ObjectUtil.isEmpty(token) && request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("sr_auth_token".equals(cookie.getName()) && ObjectUtil.isNotEmpty(cookie.getValue())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        if (ObjectUtil.isEmpty(token)) {
            throw new CustomException(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        Account account = null;
        try {
            String userInfo = JWT.decode(token).getAudience().get(0);
            String userId = userInfo.split("-")[0];
            int perm = Integer.parseInt(userInfo.split("-")[1]);
            String userRole = userInfo.split("-")[2];
            PermEnum role = PermEnum.getRoleByPerm(perm);
            if (userRole.equals(String.valueOf(role))) {
                if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
                    Admin adm = adminService.selectById(Integer.valueOf(userId));
                    if (adm != null && Integer.valueOf(1).equals(adm.getBanned())) {
                        throw new CustomException(401, "您的管理员账号已被超级管理员封禁强制下线");
                    }
                    account = adm;
                } else if (role.equals(PermEnum.USER)) {
                    com.cxy1ng.entity.User u = userService.selectById(Integer.valueOf(userId));
                    if (u != null && Integer.valueOf(1).equals(u.getAccountBanned())) {
                        String reason = cn.hutool.core.util.StrUtil.isNotBlank(u.getAccountBanReason())
                                ? u.getAccountBanReason()
                                : "因存钱日记、存钱计划或个人简介内容违规，账号已被封禁";
                        throw new CustomException(401, "您的账号已被封禁强制下线：" + reason);
                    }
                    account = u;
                }
            }
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        if (ObjectUtil.isNull(account)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        try {
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(account.getPwd())).build();
            jwtVerifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }

        // 强制重置初始密码安全硬拦截（防前端 DOM/F12 绕过）：
        // 若账号仍处于强制重置初始密码状态，除修改密码、退出登录及查询自身账户信息外，阻断全部系统业务接口
        if (Integer.valueOf(1).equals(account.getNeedChangePwd())) {
            String uri = request.getRequestURI();
            boolean isAllowed = uri.endsWith("/updatePwd")
                    || uri.endsWith("/logout")
                    || (account.getId() != null && uri.contains("/user/selectById/" + account.getId()))
                    || (account.getId() != null && uri.contains("/admin/selectById/" + account.getId()));
            if (!isAllowed) {
                throw new CustomException(403, "账户初始密码尚未修改，请先重置密码后方可使用系统功能");
            }
        }
        return true;
    }
}
