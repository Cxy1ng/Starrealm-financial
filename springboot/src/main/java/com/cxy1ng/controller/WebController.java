package com.cxy1ng.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.cxy1ng.common.Result;
import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.service.AdminService;
import com.cxy1ng.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

@RestController
public class WebController {

    private static final String AUTH_COOKIE_NAME = "sr_auth_token";

    @Resource
    private UserService userService;

    @Resource
    private AdminService adminService;

    @GetMapping("/hello")
    public Result hello() {
        return Result.success("访问成功");
    }

    @PostMapping("/login")
    public Result login(@RequestBody Account account, HttpServletResponse response) {
        if (ObjectUtil.isEmpty(account.getUsername()) || ObjectUtil.isEmpty(account.getPwd()) || ObjectUtil.isEmpty(account.getPerm())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        boolean rememberMe = Boolean.TRUE.equals(account.getRememberMe());
        PermEnum role = PermEnum.getRoleByPerm(account.getPerm());
        if (role.equals(PermEnum.USER)) {
            account.setRole(String.valueOf(role));
            account = userService.login(account);
        } else if (role.equals(PermEnum.ADMIN)) {
            account.setRole(String.valueOf(role));
            account = adminService.login(account);
        } else {
            return Result.error(ResultCodeEnum.PARAM_ERROR);
        }
        if (account != null && StrUtil.isNotBlank(account.getToken()) && response != null) {
            Cookie authCookie = new Cookie(AUTH_COOKIE_NAME, account.getToken());
            authCookie.setHttpOnly(true);
            authCookie.setPath("/");
            // 仅勾选“7天内自动登录”时持久化 7 天；否则设为会话级 Cookie（关闭浏览器即清理）
            authCookie.setMaxAge(rememberMe ? 7 * 24 * 60 * 60 : -1);
            response.addCookie(authCookie);
        }
        return Result.success(account);
    }

    @PostMapping("/logout")
    public Result logout(HttpServletResponse response) {
        if (response != null) {
            Cookie clearCookie = new Cookie(AUTH_COOKIE_NAME, "");
            clearCookie.setHttpOnly(true);
            clearCookie.setPath("/");
            clearCookie.setMaxAge(0);
            response.addCookie(clearCookie);
        }
        return Result.success();
    }

    @PostMapping("/register")
    public Result register(@RequestBody Account account) {
        if (StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getPwd()) || ObjectUtil.isEmpty(account.getPerm())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        PermEnum role = PermEnum.getRoleByPerm(account.getPerm());
        if (role.equals(PermEnum.USER)) {
            userService.register(account);
        } else {
            return Result.error(ResultCodeEnum.PARAM_ERROR);
        }
        return Result.success();
    }

    @PutMapping({"/password", "/updatePwd"})
    public Result updatePwd(@RequestBody Account account) {
        PermEnum role = PermEnum.getRoleByPerm(account.getPerm());
        if (StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getPwd()) || ObjectUtil.isEmpty(account.getPerm())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (role.equals(PermEnum.USER)) {
            userService.updatePwd(account);
        } else if (role.equals(PermEnum.ADMIN)) {
            adminService.updatePwd(account);
        } else if (role.equals(PermEnum.SUPER_ADMIN)) {
            adminService.updatePwd(account);
        } else {
            return Result.error(ResultCodeEnum.PARAM_ERROR);
        }
        return Result.success();
    }
}
