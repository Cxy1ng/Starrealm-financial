package com.cxy1ng.controller;

import com.cxy1ng.common.Result;
import com.cxy1ng.entity.Admin;
import com.cxy1ng.service.AdminService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminService adminService;

    @GetMapping("/selectAll")
    public Result selectAll(Admin admin) {
        List<Admin> list = adminService.selectAll(admin);
        return Result.success(list);
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Admin admin = adminService.selectById(id);
        if (admin != null) {
            admin.setHasApiKey(cn.hutool.core.util.StrUtil.isNotBlank(admin.getApiKey()));
            admin.setApiKey(null);
        }
        return Result.success(admin);
    }

    @GetMapping("/selectPage")
    public Result selectPage(Admin admin,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Admin> pageInfo =  adminService.selectPage(admin, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @PostMapping("/insert")
    public Result insert(@RequestBody Admin admin) {
        adminService.insert(admin);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody Admin admin) {
        adminService.update(admin);
        return Result.success();
    }

    @PutMapping("/ban")
    public Result ban(@RequestBody Admin admin) {
        adminService.updateBanStatus(admin);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        adminService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        adminService.deleteBatch(ids);
        return Result.success();
    }
}
