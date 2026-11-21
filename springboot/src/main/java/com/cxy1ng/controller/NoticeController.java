package com.cxy1ng.controller;

import com.cxy1ng.common.Result;
import com.cxy1ng.entity.Notice;
import com.cxy1ng.service.NoticeService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/notices", "/notice"})
public class NoticeController {

    @Resource
    private NoticeService noticeService;

    @PostMapping({"", "/insert"})
    public Result insert(@RequestBody Notice notice) {
        noticeService.insert(notice);
        return Result.success();
    }

    @DeleteMapping({"/{id}", "/delete/{id}"})
    public Result delete(@PathVariable Integer id) {
        noticeService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping({"", "/batch", "/delete/batch"})
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        noticeService.deleteBatch(ids);
        return Result.success();
    }

    @PutMapping({"", "/{id}", "/update"})
    public Result update(@PathVariable(required = false) Integer id, @RequestBody Notice notice) {
        if (id != null) {
            notice.setId(id);
        }
        noticeService.update(notice);
        return Result.success();
    }

    @GetMapping({"/{id}", "/selectById/{id}"})
    public Result selectById(@PathVariable Integer id) {
        Notice notice = noticeService.selectById(id);
        return Result.success(notice);
    }

    @GetMapping({"/all", "/selectAll"})
    public Result selectAll(Notice notice) {
        List<Notice> list = noticeService.selectAll(notice);
        return Result.success(list);
    }

    @GetMapping({"", "/selectPage"})
    public Result selectPage(Notice notice,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Notice> pageInfo = noticeService.selectPage(notice, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
