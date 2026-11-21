package com.cxy1ng.controller;

import com.cxy1ng.common.Result;
import com.cxy1ng.entity.PlanDetail;
import com.cxy1ng.service.PlanDetailService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/plan-details", "/planDetail"})
public class PlanDetailController {

    @Resource
    private PlanDetailService planDetailService;

    @PostMapping({"", "/add", "/insert"})
    public Result add(@RequestBody PlanDetail planDetail) {
        planDetailService.add(planDetail);
        return Result.success();
    }

    @DeleteMapping({"/{id}", "/delete/{id}"})
    public Result deleteById(@PathVariable Integer id) {
        planDetailService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping({"", "/batch", "/delete/batch"})
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        planDetailService.deleteBatch(ids);
        return Result.success();
    }

    @PutMapping({"", "/{id}", "/update"})
    public Result updateById(@PathVariable(required = false) Integer id, @RequestBody PlanDetail planDetail) {
        if (id != null) {
            planDetail.setId(id);
        }
        planDetailService.updateById(planDetail);
        return Result.success();
    }

    @GetMapping({"/{id}", "/selectById/{id}"})
    public Result selectById(@PathVariable Integer id) {
        PlanDetail planDetail = planDetailService.selectById(id);
        return Result.success(planDetail);
    }

    @GetMapping({"/all", "/selectAll"})
    public Result selectAll(PlanDetail planDetail) {
        List<PlanDetail> list = planDetailService.selectAll(planDetail);
        return Result.success(list);
    }

    @GetMapping({"", "/selectPage"})
    public Result selectPage(PlanDetail planDetail,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<PlanDetail> page = planDetailService.selectPage(planDetail, pageNum, pageSize);
        return Result.success(page);
    }

}
