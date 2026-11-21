package com.cxy1ng.controller;

import com.cxy1ng.common.Result;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.entity.User;
import com.cxy1ng.service.UserService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/users", "/user"})
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping({"/all", "/selectAll"})
    public Result selectAll(User user) {
        List<User> list = userService.selectAll(user);
        return Result.success(list);
    }

    @GetMapping({"/{id}", "/selectById/{id}"})
    public Result selectById(@PathVariable Integer id) {
        User user = userService.selectById(id);
        if (user != null) {
            user.setHasApiKey(cn.hutool.core.util.StrUtil.isNotBlank(user.getApiKey()));
            user.setApiKey(null);
        }
        return Result.success(user);
    }

    @GetMapping({"", "/selectPage"})
    public Result selectPage(User user,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<User> pageInfo =  userService.selectPage(user, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @PostMapping("/deregister")
    public Result deregister(@RequestBody Map<String, String> payload) {
        String pwd = payload != null ? payload.get("pwd") : null;
        if (cn.hutool.core.util.StrUtil.isBlank(pwd)) {
            return Result.error(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        userService.deregister(pwd.trim());
        return Result.success();
    }

    @PostMapping({"", "/insert"})
    public Result insert(@RequestBody User user) {
        String defaultPwd = userService.insert(user);
        return Result.success(defaultPwd);
    }

    @PutMapping({"", "/{id}", "/update"})
    public Result update(@PathVariable(required = false) Integer id, @RequestBody User user) {
        if (id != null) {
            user.setId(id);
        }
        userService.update(user);
        return Result.success();
    }

    @PutMapping("/ban")
    public Result updateBanStatus(@RequestBody User user) {
        userService.updateBanStatus(user);
        return Result.success();
    }

    @PutMapping({"/ai-ban", "/aiBan"})
    public Result auditAiBan(@RequestBody java.util.Map<String, Object> payload) {
        userService.auditAiBan(payload);
        return Result.success();
    }

    @DeleteMapping({"/{id}", "/delete/{id}"})
    public Result deleteById(@PathVariable Integer id) {
        userService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping({"", "/batch", "/delete/batch"})
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        userService.deleteBatch(ids);
        return Result.success();
    }
}
