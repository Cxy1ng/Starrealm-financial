package com.cxy1ng.controller;

import com.cxy1ng.common.Result;
import com.cxy1ng.entity.Acfun;
import com.cxy1ng.service.AcfunService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/acfun")
public class AcfunController {

    @Resource
    private AcfunService acfunService;

    @PostMapping("/insert")
    public Result insert(@RequestBody Acfun acfun) {
        acfunService.insert(acfun);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        acfunService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        acfunService.deleteBatch(ids);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody Acfun acfun) {
        acfunService.update(acfun);
        return Result.success();
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Acfun acfun = acfunService.selectById(id);
        return Result.success(acfun);
    }

    @GetMapping("/selectAll")
    public Result selectAll(Acfun acfun) {
        List<Acfun> list = acfunService.selectAll(acfun);
        return Result.success(list);
    }

    @GetMapping("/selectPage")
    public Result selectPage(Acfun acfun,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Acfun> page = acfunService.selectPage(acfun, pageNum, pageSize);
        return Result.success(page);
    }
}
