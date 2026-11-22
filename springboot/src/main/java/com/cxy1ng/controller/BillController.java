package com.cxy1ng.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.cxy1ng.common.Result;
import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.dto.bill.BillImportConfirmDTO;
import com.cxy1ng.dto.bill.BillImportPrecheckVO;
import com.cxy1ng.entity.Account;
import com.cxy1ng.entity.Bill;
import com.cxy1ng.service.BillImportService;
import com.cxy1ng.service.BillService;
import com.cxy1ng.utils.TokenUtils;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping({"/bills", "/bill"})
public class BillController {

    @Resource
    private BillService billService;

    @Resource
    private BillImportService billImportService;

    @PostMapping({"", "/insert"})
    public Result insert(@RequestBody Bill bill) {
        billService.insert(bill);
        return Result.success();
    }

    @DeleteMapping({"/{id}", "/delete/{id}"})
    public Result delete(@PathVariable Integer id) {
        billService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping({"", "/batch", "/delete/batch"})
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        billService.deleteBatch(ids);
        return Result.success();
    }

    @PutMapping({"", "/{id}", "/update"})
    public Result update(@PathVariable(required = false) Integer id, @RequestBody Bill bill) {
        if (id != null) {
            bill.setId(id);
        }
        billService.update(bill);
        return Result.success();
    }

    @GetMapping({"/{id}", "/selectById/{id}"})
    public Result selectById(@PathVariable Integer id) {
        Bill bill = billService.selectById(id);
        return Result.success(bill);
    }

    @GetMapping({"/all", "/selectAll"})
    public Result selectAll(Bill bill) {
        List<Bill> list = billService.selectAll(bill);
        return Result.success(list);
    }

    @GetMapping({"", "/selectPage"})
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

    /**
     * 账单导入第一阶段：预检解析（不落库，返回多维指标与智能分类待确认条目）
     */
    @PostMapping({"/import/precheck", "/precheck"})
    public Result precheck(@RequestParam("file") MultipartFile file,
                           @RequestParam(value = "channel", required = false) String channel) {
        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择要导入的账单文件");
        }
        Account currentUser = TokenUtils.getCurrentUser();
        Integer userId = currentUser != null ? currentUser.getId() : null;
        if (userId == null) {
            return Result.error(401, "登录状态已失效，请重新登录");
        }
        BillImportPrecheckVO precheckVO = billImportService.precheck(file, channel, userId);
        return Result.success(precheckVO);
    }

    /**
     * 账单导入第二阶段：确认提交入库
     */
    @PostMapping({"/import/confirm", "/confirm"})
    public Result confirm(@RequestBody BillImportConfirmDTO confirmDTO) {
        if (confirmDTO == null || confirmDTO.getItems() == null || confirmDTO.getItems().isEmpty()) {
            return Result.error(400, "待入库记录为空");
        }
        Account currentUser = TokenUtils.getCurrentUser();
        Integer userId = currentUser != null ? currentUser.getId() : null;
        if (userId == null) {
            return Result.error(401, "登录状态已失效，请重新登录");
        }
        confirmDTO.setUserId(userId);
        int count = billImportService.confirm(confirmDTO);
        return Result.success(count);
    }

    @PostMapping("/import")
    public Result importData(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择要导入的账单文件");
        }
        Account currentUser = TokenUtils.getCurrentUser();
        Integer userId = currentUser != null ? currentUser.getId() : null;
        if (userId == null) {
            return Result.error(401, "登录状态已失效，请重新登录");
        }

        byte[] fileBytes = file.getBytes();
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        // 1. 将 Excel (.xlsx/.xls) 或 CSV 统一解析为二维字符串/对象矩阵 allRows
        List<List<Object>> allRows = new ArrayList<>();
        boolean parsedAsExcel = false;
        if (!originalFilename.endsWith(".csv")) {
            try {
                ExcelReader reader = ExcelUtil.getReader(new ByteArrayInputStream(fileBytes));
                // 遍历所有 Sheet，找到包含账单数据的 Sheet（防止某些导出文件首个 Sheet 为空或说明页）
                int sheetCount = reader.getSheetCount();
                for (int s = 0; s < sheetCount; s++) {
                    ExcelReader sheetReader = ExcelUtil.getReader(new ByteArrayInputStream(fileBytes), s);
                    List<List<Object>> rows = sheetReader.read();
                    if (rows != null && !rows.isEmpty()) {
                        allRows.addAll(rows);
                    }
                }
                parsedAsExcel = !allRows.isEmpty();
            } catch (Exception ignored) {
                parsedAsExcel = false;
            }
        }

        // 若不是标准 Excel 或以 .csv 导出，则自动按 UTF-8 / GBK 解析 CSV
        if (!parsedAsExcel) {
            String contentUtf8 = new String(fileBytes, StandardCharsets.UTF_8);
            Charset csvCharset = (contentUtf8.contains("交易时间") || contentUtf8.contains("金额") || contentUtf8.contains("收/支"))
                    ? StandardCharsets.UTF_8
                    : Charset.forName("GBK");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileBytes), csvCharset))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    String[] cols = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    List<Object> row = new ArrayList<>(cols.length);
                    for (String c : cols) {
                        String clean = c.trim();
                        if (clean.startsWith("\"") && clean.endsWith("\"") && clean.length() >= 2) {
                            clean = clean.substring(1, clean.length() - 1).trim();
                        }
                        row.add(clean);
                    }
                    allRows.add(row);
                }
            }
        }

        if (allRows.isEmpty()) {
            return Result.error(400, "账单文件内容为空，未解析到任何数据");
        }

        // 2. 自动扫描定位真实表头行（兼容微信/支付宝前 16~25 行的统计说明区，以及本系统首行表头）
        int headerRowIndex = -1;
        Map<String, Integer> colIdxMap = new HashMap<>();

        for (int i = 0; i < Math.min(allRows.size(), 60); i++) {
            List<Object> row = allRows.get(i);
            if (row == null || row.isEmpty()) continue;

            Map<String, Integer> candidateMap = new HashMap<>();
            for (int c = 0; c < row.size(); c++) {
                Object cell = row.get(c);
                if (cell == null) continue;
                // 去除零宽字符、BOM、首尾空格与全角空格
                String header = cell.toString()
                        .replace("\uFEFF", "")
                        .replace("\u200B", "")
                        .replace("　", "")
                        .trim();
                if (header.isEmpty()) continue;

                if (header.contains("交易时间") || header.equals("时间") || header.contains("付款时间") || header.contains("创建时间")) {
                    candidateMap.putIfAbsent("time", c);
                } else if (header.contains("交易类型") || header.contains("交易分类") || header.equals("分类") || header.equals("类型")) {
                    candidateMap.putIfAbsent("category", c);
                } else if (header.contains("交易对方") || header.contains("收款方") || header.contains("对方账号") || header.contains("商户")) {
                    candidateMap.putIfAbsent("payee", c);
                } else if (header.equals("商品") || header.contains("商品说明") || header.contains("交易说明") || header.contains("商品名称")) {
                    candidateMap.putIfAbsent("detail", c);
                } else if (header.contains("收/支") || header.contains("收支") || header.contains("账单类型")) {
                    candidateMap.putIfAbsent("type", c);
                } else if (header.startsWith("金额") || header.contains("金额(元)") || header.contains("金额（元）")) {
                    candidateMap.putIfAbsent("amount", c);
                } else if (header.contains("支付方式") || header.contains("收/付款方式") || header.contains("收付款方式")) {
                    candidateMap.putIfAbsent("payment", c);
                } else if (header.contains("当前状态") || header.contains("交易状态") || header.equals("状态")) {
                    candidateMap.putIfAbsent("status", c);
                } else if (header.contains("备注")) {
                    candidateMap.putIfAbsent("comment", c);
                }
            }

            // 只要同时命中了“金额”列与（“时间”或“收/支”或“分类”）列，即判定为真实表头行
            if (candidateMap.containsKey("amount") && (candidateMap.containsKey("time") || candidateMap.containsKey("type") || candidateMap.containsKey("category"))) {
                headerRowIndex = i;
                colIdxMap = candidateMap;
                break;
            }
        }

        if (headerRowIndex == -1) {
            return Result.error(400, "未识别到有效的账单表头（需包含交易时间、收/支、金额等列）");
        }

        // 3. 逐行解析表头下方的数据行
        List<Bill> billList = new ArrayList<>();
        for (int r = headerRowIndex + 1; r < allRows.size(); r++) {
            List<Object> row = allRows.get(r);
            if (row == null || row.isEmpty()) continue;

            Object rawAmount = getCell(row, colIdxMap.get("amount"));
            if (rawAmount == null) continue;
            // 清洗 ¥、￥、元、逗号、空格等符号
            String amountStr = rawAmount.toString().replaceAll("[^\\d.-]", "").trim();
            if (StrUtil.isBlank(amountStr)) continue;
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr).abs();
            } catch (Exception e) {
                continue;
            }
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            String rawCategory = cellToStr(row, colIdxMap.get("category"), "其他");
            String rawDetail = cellToStr(row, colIdxMap.get("detail"), "");
            String rawStatus = cellToStr(row, colIdxMap.get("status"), "交易成功");
            String rawType = cellToStr(row, colIdxMap.get("type"), "");

            // 归一化“收/支”类型（兼容微信账单中的“支出”、“收入”、“/”、“不计收支”等）
            String normalizedType;
            if (rawType.contains("支")) {
                normalizedType = "支出";
            } else if (rawType.contains("收") && !rawType.contains("不计")) {
                normalizedType = "收入";
            } else {
                // 若微信账单中收/支列为 "/" 或 "不计收支"，根据交易类型/说明智能推断：含转入/退款/红包/收款归为收入，否则归为支出
                String combined = rawCategory + " " + rawDetail + " " + rawStatus;
                if (combined.contains("转入") || combined.contains("退款") || combined.contains("收款") || combined.contains("收到")) {
                    normalizedType = "收入";
                } else {
                    normalizedType = "支出";
                }
            }

            Bill bill = new Bill();
            Object rawTime = getCell(row, colIdxMap.get("time"));
            if (rawTime instanceof Date) {
                bill.setTime((Date) rawTime);
            } else if (rawTime != null && StrUtil.isNotBlank(rawTime.toString())) {
                try {
                    bill.setTime(DateUtil.parse(rawTime.toString().trim()));
                } catch (Exception e) {
                    bill.setTime(new Date());
                }
            } else {
                bill.setTime(new Date());
            }

            bill.setCategory(StrUtil.isNotBlank(rawCategory) ? rawCategory : "其他");
            bill.setPayee(cellToStr(row, colIdxMap.get("payee"), "未知商户"));
            bill.setDetail(rawDetail);
            bill.setType(normalizedType);
            bill.setAmount(amount);
            String paymentStr = cellToStr(row, colIdxMap.get("payment"), "微信支付");
            bill.setPayment("/".equals(paymentStr) || StrUtil.isBlank(paymentStr) ? "微信支付" : paymentStr);
            bill.setStatus("/".equals(rawStatus) || StrUtil.isBlank(rawStatus) ? "交易成功" : rawStatus);
            String commentStr = cellToStr(row, colIdxMap.get("comment"), "");
            bill.setComment("/".equals(commentStr) ? "" : commentStr);
            bill.setUserId(userId);

            billList.add(bill);
        }

        if (billList.isEmpty()) {
            return Result.error(400, "未在文件中解析到有效的收支记录，请检查账单内容");
        }

        int importedCount = billService.insertBatch(billList, userId);
        return Result.success(importedCount);
    }

    private Object getCell(List<Object> row, Integer idx) {
        if (idx == null || idx < 0 || idx >= row.size()) return null;
        return row.get(idx);
    }

    private String cellToStr(List<Object> row, Integer idx, String defaultVal) {
        Object val = getCell(row, idx);
        if (val == null) return defaultVal;
        String s = val.toString().replace("\uFEFF", "").trim();
        return s.isEmpty() ? defaultVal : s;
    }
}
