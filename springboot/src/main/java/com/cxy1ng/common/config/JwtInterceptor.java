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
                    account = adminService.selectById(Integer.valueOf(userId));
                } else if (role.equals(PermEnum.USER)) {
                    com.cxy1ng.entity.User u = userService.selectById(Integer.valueOf(userId));
                    if (u != null && Integer.valueOf(1).equals(u.getBanned())) {
                        String reason = cn.hutool.core.util.StrUtil.isNotBlank(u.getBanReason()) ? u.getBanReason() : "违反《星界用户服务协议》相关条款";
                        throw new CustomException(ResultCodeEnum.USER_BANNED_ERROR.code, "账号已被封禁，原因：" + reason);
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
        return true;
    }
}
