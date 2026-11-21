package com.cxy1ng.controller;

import com.cxy1ng.common.Result;
import com.cxy1ng.entity.Acfun;
import com.cxy1ng.service.AcfunService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/acfun", "/acfun-videos"})
public class AcfunController {

    @Resource
    private AcfunService acfunService;

    @PostMapping({"", "/insert"})
    public Result insert(@RequestBody Acfun acfun) {
        acfunService.insert(acfun);
        return Result.success();
    }

    @DeleteMapping({"/{id}", "/delete/{id}"})
    public Result deleteById(@PathVariable Integer id) {
        acfunService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping({"", "/batch", "/delete/batch"})
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        acfunService.deleteBatch(ids);
        return Result.success();
    }

    @PutMapping({"", "/{id}", "/update"})
    public Result update(@PathVariable(required = false) Integer id, @RequestBody Acfun acfun) {
        if (id != null) {
            acfun.setId(id);
        }
        acfunService.update(acfun);
        return Result.success();
    }

    @GetMapping({"/{id}", "/selectById/{id}"})
    public Result selectById(@PathVariable Integer id) {
        Acfun acfun = acfunService.selectById(id);
        return Result.success(acfun);
    }

    @GetMapping({"/all", "/selectAll"})
    public Result selectAll(Acfun acfun) {
        List<Acfun> list = acfunService.selectAll(acfun);
        return Result.success(list);
    }

    @GetMapping({"", "/selectPage"})
    public Result selectPage(Acfun acfun,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Acfun> page = acfunService.selectPage(acfun, pageNum, pageSize);
        return Result.success(page);
    }
}
