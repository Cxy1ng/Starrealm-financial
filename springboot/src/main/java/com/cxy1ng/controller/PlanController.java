package com.cxy1ng.controller;

import com.cxy1ng.common.Result;
import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.entity.Plan;
import com.cxy1ng.service.PlanService;
import com.cxy1ng.utils.TokenUtils;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/plans", "/plan"})
public class PlanController {

    @Resource
    private PlanService planService;

    @PostMapping({"", "/add", "/insert"})
    public Result add(@RequestBody Plan plan) {
        planService.add(plan);
        return Result.success();
    }

    @DeleteMapping({"/{id}", "/delete/{id}"})
    public Result deleteById(@PathVariable Integer id) {
        planService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping({"", "/batch", "/delete/batch"})
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        planService.deleteBatch(ids);
        return Result.success();
    }

    @PutMapping({"", "/{id}", "/update"})
    public Result updateById(@PathVariable(required = false) Integer id, @RequestBody Plan plan) {
        if (id != null) {
            plan.setId(id);
        }
        planService.updateById(plan);
        return Result.success();
    }

    @GetMapping({"/{id}", "/selectById/{id}"})
    public Result selectById(@PathVariable Integer id) {
        Plan plan = planService.selectById(id);
        return Result.success(plan);
    }

    @GetMapping({"/all", "/selectAll"})
    public Result selectAll(Plan plan) {
        List<Plan> list = planService.selectAll(plan);
        return Result.success(list);
    }

    @GetMapping({"", "/selectPage"})
    public Result selectPage(Plan plan,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.USER)) {
            plan.setUserId(currentUser.getId());
        }
        PageInfo<Plan> page = planService.selectPage(plan, pageNum, pageSize);
        return Result.success(page);
    }

}
