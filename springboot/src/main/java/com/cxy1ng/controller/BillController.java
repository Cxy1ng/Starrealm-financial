package com.cxy1ng.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.cxy1ng.common.Result;
import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.entity.Bill;
import com.cxy1ng.service.BillService;
import com.cxy1ng.utils.TokenUtils;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/bill")
public class BillController {

    @Resource
    private BillService billService;

    @PostMapping("/insert")
    public Result insert(@RequestBody Bill bill) {
        billService.insert(bill);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        billService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        billService.deleteBatch(ids);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody Bill bill) {
        billService.update(bill);
        return Result.success();
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Bill bill = billService.selectById(id);
        return Result.success(bill);
    }

    @GetMapping("/selectAll")
    public Result selectAll(Bill bill) {
        List<Bill> list = billService.selectAll(bill);
        return Result.success(list);
    }

    @GetMapping("/selectPage")
    public Result selectPage(Bill bill,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Bill> page = billService.selectPage(bill, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/export")
    public void export(Bill bill,  HttpServletResponse response) throws IOException {
        Account currentUser = TokenUtils.getCurrentUser();

        if (currentUser == null || currentUser.getId() == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"未登录或登录过期");
            return;
        }

        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.USER)) {
            bill.setUserId(currentUser.getId());
            bill.setUserName(currentUser.getUsername());
        }

        String ids = bill.getIds();
        if (StrUtil.isNotBlank(ids)) {
            bill.setIdsArr(ids.split(","));
        }
        List<Bill> billList = billService.selectAll(bill);
        ExcelWriter excelWriter = ExcelUtil.getWriter(true);

        excelWriter.addHeaderAlias("time", "交易时间");
        excelWriter.addHeaderAlias("category", "交易分类");
        excelWriter.addHeaderAlias("payee", "收款方");
        excelWriter.addHeaderAlias("detail", "交易说明");
        excelWriter.addHeaderAlias("type", "收/支");
        excelWriter.addHeaderAlias("amount", "金额");
        excelWriter.addHeaderAlias("payment", "收/付款方式");
        excelWriter.addHeaderAlias("status", "交易状态");
        excelWriter.addHeaderAlias("comment", "备注");
//        excelWriter.addHeaderAlias("userName", "用户名称");
        excelWriter.setOnlyAlias(true);

        excelWriter.write(billList);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("账单数据", StandardCharsets.UTF_8) + ".xlsx");
        ServletOutputStream os = response.getOutputStream();
        excelWriter.flush(os, true);
        excelWriter.close();
        os.close();
    }

    @GetMapping("/count")
    public Result count(String type) {
        List<Bill> list = billService.count(type);
        return Result.success(list);
    }

    @PostMapping("/import")
    public Result importData(MultipartFile file) throws Exception {
        InputStream inputStream = file.getInputStream();
        ExcelReader excelReader = ExcelUtil.getReader(inputStream);

        Map<String, String> headerAlias = new HashMap<>() {{
            put("交易时间", "time");
            put("交易类型", "category");
            put("交易分类", "category");
            put("交易对方", "payee");
            put("对方账号", "payee");
            put("商品", "detail");
            put("商品说明", "detail");
            put("收/支", "type");
            put("金额", "amount");
            put("金额(元)", "amount");
            put("支付方式", "payment");
            put("收/付款方式", "payment");
            put("当前状态", "status");
            put("交易状态", "status");
            put("备注", "comment");
        }};
        excelReader.setHeaderAlias(headerAlias);
        Account currentUser = TokenUtils.getCurrentUser();
        Integer userId = currentUser.getId();

        List<Map<String, Object>> rawList = excelReader.readAll();
        List<Bill> billList = new ArrayList<>();
        for (Map<String, Object> row : rawList) {
            Bill bill = new Bill();

            bill.setTime((DateTime) row.get("time"));
            bill.setCategory(row.get("category").toString());
            bill.setPayee(row.get("payee").toString());
            bill.setDetail(row.get("detail").toString());
            bill.setType(row.get("type").toString());

            Object rawAmount = row.get("amount");
            if (rawAmount != null) {
                String amountStr = rawAmount.toString().replaceAll("[^\\d.-]", "");
                bill.setAmount(new BigDecimal(amountStr));
            }

            bill.setPayment(row.get("payment").toString());
            bill.setStatus(row.get("status").toString());
            bill.setComment(row.get("comment").toString());
            bill.setUserId(userId);

            billList.add(bill);
        }
        for (Bill bill : billList) {
            billService.insert(bill);
        }
        return Result.success();
    }
}
