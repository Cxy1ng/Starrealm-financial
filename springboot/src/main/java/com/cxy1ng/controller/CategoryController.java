package com.cxy1ng.controller;

import com.cxy1ng.common.Result;
import com.cxy1ng.entity.Category;
import com.cxy1ng.service.CategoryService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/categories", "/category"})
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @PostMapping({"", "/insert"})
    public Result insert(@RequestBody Category category) {
        categoryService.insert(category);
        return Result.success();
    }

    @DeleteMapping({"/{id}", "/delete/{id}"})
    public Result deleteById(@PathVariable Integer id) {
        categoryService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping({"", "/batch", "/delete/batch"})
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        categoryService.deleteBatch(ids);
        return Result.success();
    }

    @PutMapping({"", "/{id}", "/update"})
    public Result update(@PathVariable(required = false) Integer id, @RequestBody Category category) {
        if (id != null) {
            category.setId(id);
        }
        categoryService.update(category);
        return Result.success();
    }

    @GetMapping({"/{id}", "/selectById/{id}"})
    public Result selectById(@PathVariable Integer id) {
        Category category = categoryService.selectById(id);
        return Result.success(category);
    }

    @GetMapping({"/all", "/selectAll"})
    public Result selectAll(Category category) {
        List<Category> list = categoryService.selectAll(category);
        return Result.success(list);
    }

    @GetMapping({"", "/selectPage"})
    public Result selectPage(Category category,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Category> page = categoryService.selectPage(category, pageNum, pageSize);
        return Result.success(page);
    }

}
