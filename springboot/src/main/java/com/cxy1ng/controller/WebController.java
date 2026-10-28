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
import org.springframework.web.bind.annotation.*;

@RestController
public class WebController {

    @Resource
    private UserService userService;

    @Resource
    private AdminService adminService;

    @GetMapping("/hello")
    public Result hello() {
        return Result.success("访问成功");
    }

    @PostMapping("/login")
    public Result login(@RequestBody Account account) {
        if (ObjectUtil.isEmpty(account.getUsername()) || ObjectUtil.isEmpty(account.getPwd()) || ObjectUtil.isEmpty(account.getPerm())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
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
        return Result.success(account);
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

    @PutMapping("/updatePwd")
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
