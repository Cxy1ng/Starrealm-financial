package com.cxy1ng.controller;

import com.cxy1ng.common.Result;
import com.cxy1ng.entity.Notebook;
import com.cxy1ng.service.NotebookService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notebook")
public class NotebookController {

    @Resource
    private NotebookService notebookService;

    @PostMapping("/add")
    public Result add(@RequestBody Notebook notebook) {
        notebookService.add(notebook);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        notebookService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        notebookService.deleteBatch(ids);
        return Result.success();
    }

    @PutMapping("/update")
    public Result updateById(@RequestBody Notebook notebook) {
        notebookService.updateById(notebook);
        return Result.success();
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Notebook notebook = notebookService.selectById(id);
        return Result.success(notebook);
    }

    @GetMapping("/selectAll")
    public Result selectAll(Notebook notebook) {
        List<Notebook> list = notebookService.selectAll(notebook);
        return Result.success(list);
    }

    @GetMapping("/selectPage")
    public Result selectPage(Notebook notebook,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Notebook> page = notebookService.selectPage(notebook, pageNum, pageSize);
        return Result.success(page);
    }

}
