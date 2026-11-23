package com.cxy1ng.service.parser;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.cxy1ng.dto.bill.BillImportItemDTO;
import com.cxy1ng.service.BillCategoryClassifier;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 账单多渠道解析器抽象基类
 * 封装通用文件加载（Excel/CSV）、多语言表头探测、金额清洗、日期自适应解析及分类回填模板流程
 */
public abstract class AbstractBillParser implements ChannelBillParser {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Resource
    protected BillCategoryClassifier classifier;

    // 多语言表头识别正则（支持简中、繁体、英文、中英双语，不区分大小写）
    protected static final Pattern PATTERN_TIME = Pattern.compile(
            "(?i).*(交易时间|付款时间|创建时间|记账日期|交易日期|时间|日期|Posting\\s*Date|Transaction\\s*Date|Payment\\s*Date|Date|Time).*"
    );
    protected static final Pattern PATTERN_DATE_ONLY = Pattern.compile(
            "(?i).*(交易日期|记账日|记账日期|清算日|起息日|Date).*"
    );
    protected static final Pattern PATTERN_TIME_ONLY = Pattern.compile(
            "(?i).*(交易时间|创建时间|付款时间|记账时间|Time|Timestamp).*"
    );
    protected static final Pattern PATTERN_PAYEE = Pattern.compile(
            "(?i).*(交易对方|对方名称|收款方|对方账号|商户|商户名称|收款人|对方|Payee|Merchant|Counterparty|Description|To|Name).*"
    );
    protected static final Pattern PATTERN_DETAIL = Pattern.compile(
            "(?i).*(商品说明|交易说明|商品名称|商品|摘要|附言|用途|Item\\s*Title|Product|Details|Summary|Memo).*"
    );
    protected static final Pattern PATTERN_TYPE = Pattern.compile(
            "(?i).*(收/支|收支类型|收支|账单类型|借贷标志|借/贷|Direction|Type|Dr/Cr|Credit/Debit).*"
    );
    protected static final Pattern PATTERN_AMOUNT = Pattern.compile(
            "(?i).*(金额\\(元\\)|金额（元）|交易金额|发生额|金额|Gross|Net|Amount|Total).*"
    );
    protected static final Pattern PATTERN_PAYMENT = Pattern.compile(
            "(?i).*(支付方式|收/付款方式|收付款方式|资金渠道|Payment\\s*Method|Method|Account).*"
    );
    protected static final Pattern PATTERN_STATUS = Pattern.compile(
            "(?i).*(当前状态|交易状态|状态|Status).*"
    );
    protected static final Pattern PATTERN_CATEGORY = Pattern.compile(
            "(?i).*(交易类型|交易分类|分类|类型|Category).*"
    );
    protected static final Pattern PATTERN_TX_ID = Pattern.compile(
            "(?i).*(交易单号|交易号|微信支付交易单号|支付宝交易号|订单号|流水号|业务流水号|Transaction\\s*ID|Order\\s*ID|Txn\\s*ID|Reference).*"
    );
    protected static final Pattern PATTERN_COMMENT = Pattern.compile(
            "(?i).*(备注|附言|Comment|Note|Remarks).*"
    );

    public BillCategoryClassifier getClassifier() {
        return classifier;
    }

    public void setClassifier(BillCategoryClassifier classifier) {
        this.classifier = classifier;
    }

    @Override
    public List<BillImportItemDTO> parse(InputStream inputStream, String filename, Integer userId) {
        if (inputStream == null) {
            return Collections.emptyList();
        }

        byte[] fileBytes;
        try {
            fileBytes = IoUtil.readBytes(inputStream);
        } catch (Exception e) {
            log.error("Failed to read file bytes: {}", filename, e);
            return Collections.emptyList();
        }

        if (fileBytes == null || fileBytes.length == 0) {
            return Collections.emptyList();
        }

        // 1. 将文件转换为二维矩阵
        List<List<Object>> allRows = loadRows(fileBytes, filename);
        if (allRows == null || allRows.isEmpty()) {
            log.warn("Parsed empty rows from file: {}", filename);
            return Collections.emptyList();
        }

        // 2. 扫描定位表头行并建立列索引映射
        Map<String, Integer> colIdxMap = new HashMap<>();
        int headerRowIndex = findHeaderRow(allRows, colIdxMap);
        if (headerRowIndex == -1) {
            log.warn("Failed to detect valid table header in file: {}", filename);
            return Collections.emptyList();
        }

        // 3. 逐行解析数据行并组装 DTO
        List<BillImportItemDTO> itemList = new ArrayList<>();
        for (int r = headerRowIndex + 1; r < allRows.size(); r++) {
            List<Object> row = allRows.get(r);
            if (row == null || row.isEmpty() || isSummaryOrNoiseRow(row, r)) {
                continue;
            }

            try {
                BillImportItemDTO item = parseRow(row, colIdxMap, userId, r);
                if (item != null && item.getAmount() != null) {
                    // 后置钩子增强
                    postProcessItem(item, row, colIdxMap);

                    // 4. 调用分类引擎回填 category 与 isUncertain
                    if (classifier != null) {
                        classifier.classify(item);
                    }
                    itemList.add(item);
                }
            } catch (Exception e) {
                log.debug("Skip unparsable row {}: {}", r, e.getMessage());
            }
        }

        return itemList;
    }

    /**
     * 文件内容加载为二维行列表（自动适配 Excel 多 Sheet 与 CSV 多编码）
     */
    protected List<List<Object>> loadRows(byte[] bytes, String filename) {
        String lowerName = filename != null ? filename.toLowerCase() : "";

        if (lowerName.endsWith(".csv") || lowerName.endsWith(".txt")) {
            return loadCsvRows(bytes);
        }

        try {
            return loadExcelRows(bytes);
        } catch (Exception excelErr) {
            log.debug("Excel loading failed for {}, trying CSV fallback: {}", filename, excelErr.getMessage());
            try {
                return loadCsvRows(bytes);
            } catch (Exception csvErr) {
                log.error("Failed to parse file as Excel or CSV: {}", filename, csvErr);
                return Collections.emptyList();
            }
        }
    }

    /**
     * Excel 文件解析：遍历所有 Sheet 提取全部非空行
     */
    protected List<List<Object>> loadExcelRows(byte[] bytes) throws Exception {
        List<List<Object>> rows = new ArrayList<>();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            Workbook workbook = WorkbookFactory.create(bais);
            int sheetCount = workbook.getNumberOfSheets();
            for (int s = 0; s < sheetCount; s++) {
                try {
                    ExcelReader reader = new ExcelReader(workbook, s);
                    List<List<Object>> sheetRows = reader.read();
                    if (sheetRows != null && !sheetRows.isEmpty()) {
                        rows.addAll(sheetRows);
                    }
                } catch (Exception e) {
                    log.debug("Failed to read sheet index {}: {}", s, e.getMessage());
                }
            }
        }
        return rows;
    }

    /**
     * CSV 文件解析：智能探测 UTF-8 / GBK / ANSI 编码并处理引号与换行
     */
    protected List<List<Object>> loadCsvRows(byte[] bytes) {
        Charset charset = detectCsvCharset(bytes);
        CsvReader csvReader = CsvUtil.getReader();
        CsvData csvData = csvReader.read(new InputStreamReader(new ByteArrayInputStream(bytes), charset));
        List<List<Object>> rows = new ArrayList<>();
        if (csvData != null && csvData.getRows() != null) {
            for (CsvRow csvRow : csvData.getRows()) {
                rows.add(new ArrayList<>(csvRow.getRawList()));
            }
        }
        return rows;
    }

    /**
     * 自动探测 CSV 编码（支持带 BOM UTF-8、普通 UTF-8、GBK 兜底）
     */
    protected Charset detectCsvCharset(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return StandardCharsets.UTF_8;
        }
        // 检查 UTF-8 BOM
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }

        // 尝试使用 UTF-8 解码探测
        try {
            java.nio.charset.CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            decoder.onMalformedInput(java.nio.charset.CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT);
            decoder.decode(java.nio.ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (Exception ignored) {
            // 含有非 UTF-8 字节，回退到中文 Windows 默认的 GBK / GB18030
            try {
                return Charset.forName("GBK");
            } catch (Exception e) {
                return StandardCharsets.UTF_8;
            }
        }
    }

    /**
     * 自动扫描定位真实表头行（跳过前置元数据或统计说明区）
     */
    protected int findHeaderRow(List<List<Object>> allRows, Map<String, Integer> colIdxMap) {
        int limit = Math.min(allRows.size(), 60);
        for (int i = 0; i < limit; i++) {
            List<Object> row = allRows.get(i);
            if (row == null || row.isEmpty()) continue;

            Map<String, Integer> candidateMap = new HashMap<>();
            for (int c = 0; c < row.size(); c++) {
                Object cell = row.get(c);
                if (cell == null) continue;
                String header = cleanHeaderCell(cell.toString());
                if (header.isEmpty()) continue;

                if (PATTERN_DATE_ONLY.matcher(header).matches()) {
                    candidateMap.putIfAbsent("date", c);
                }
                if (PATTERN_TIME_ONLY.matcher(header).matches()) {
                    candidateMap.putIfAbsent("time", c);
                }

                if (PATTERN_TIME.matcher(header).matches()) {
                    candidateMap.putIfAbsent("time", c);
                } else if (PATTERN_AMOUNT.matcher(header).matches()) {
                    candidateMap.putIfAbsent("amount", c);
                } else if (PATTERN_PAYEE.matcher(header).matches()) {
                    candidateMap.putIfAbsent("payee", c);
                } else if (PATTERN_DETAIL.matcher(header).matches()) {
                    candidateMap.putIfAbsent("detail", c);
                } else if (PATTERN_TYPE.matcher(header).matches()) {
                    candidateMap.putIfAbsent("type", c);
                } else if (PATTERN_CATEGORY.matcher(header).matches()) {
                    candidateMap.putIfAbsent("category", c);
                } else if (PATTERN_PAYMENT.matcher(header).matches()) {
                    candidateMap.putIfAbsent("payment", c);
                } else if (PATTERN_STATUS.matcher(header).matches()) {
                    candidateMap.putIfAbsent("status", c);
                } else if (PATTERN_TX_ID.matcher(header).matches()) {
                    candidateMap.putIfAbsent("txId", c);
                } else if (PATTERN_COMMENT.matcher(header).matches()) {
                    candidateMap.putIfAbsent("comment", c);
                }
            }

            // 只要命中了金额列，并且同时命中了时间/对方/收支/分类中任意一列，即判定为真实表头
            if (candidateMap.containsKey("amount") && (candidateMap.containsKey("time") || candidateMap.containsKey("payee")
                    || candidateMap.containsKey("type") || candidateMap.containsKey("category"))) {
                colIdxMap.putAll(candidateMap);
                return i;
            }
        }
        return -1;
    }

    protected String cleanHeaderCell(String text) {
        if (text == null) return "";
        return text.replace("\uFEFF", "")
                .replace("\u200B", "")
                .replace("　", "")
                .trim();
    }

    /**
     * 判断是否为汇总统计行、分隔线或噪音行（如支付宝尾部汇总、分隔横线等）
     */
    protected boolean isSummaryOrNoiseRow(List<Object> row, int rowIndex) {
        if (row == null || row.isEmpty()) return true;
        String first = cellToStr(row, 0, "");
        if (first.startsWith("---") || first.startsWith("===")) {
            return true;
        }
        if (first.contains("共") && (first.contains("笔记录") || first.contains("笔交易"))) {
            return true;
        }
        if (row.size() <= 4 && (first.contains("导出时间") || first.contains("已收入") || first.contains("已支出"))) {
            return true;
        }
        return false;
    }

    /**
     * 单行核心字段解析器
     */
    protected BillImportItemDTO parseRow(List<Object> row, Map<String, Integer> colIdxMap, Integer userId, int rowIndex) {
        Object rawAmountObj = getCell(row, colIdxMap.get("amount"));
        if (rawAmountObj == null) return null;

        String rawAmountStr = rawAmountObj.toString().trim();
        if (StrUtil.isBlank(rawAmountStr)) return null;

        // 金额与币种识别（检查金额串或全行任一单元格如币种列 USD、EUR 等）
        boolean isForeign = detectForeignCurrency(rawAmountStr);
        if (!isForeign) {
            for (Object cell : row) {
                if (cell != null && detectForeignCurrency(cell.toString())) {
                    isForeign = true;
                    break;
                }
            }
        }
        BigDecimal amount = cleanAmount(rawAmountStr);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        // 交易时间自适应解析（支持日期列与时间列分立或合并）
        Integer dateCol = colIdxMap.get("date");
        Integer timeCol = colIdxMap.get("time");
        Object dateObj = getCell(row, dateCol);
        Object timeObj = getCell(row, timeCol);

        Date time;
        if (dateObj != null && timeObj != null && !dateCol.equals(timeCol)) {
            String dateStr = dateObj.toString().trim();
            String timeStr = timeObj.toString().trim();
            time = parseDateTime(dateStr + " " + timeStr);
        } else {
            time = parseDateTime(timeObj != null ? timeObj : dateObj);
        }

        // 文本字段清洗
        String rawPayee = cellToStr(row, colIdxMap.get("payee"), "未知商户");
        String cleanPayee = cleanMerchantName(rawPayee);
        String detail = cellToStr(row, colIdxMap.get("detail"), "");
        if ("/".equals(detail.trim()) || "-".equals(detail.trim()) || ".".equals(detail.trim())) {
            detail = "";
        } else {
            // 清理商品说明中结尾过长的冗余单号后缀（如 微信中的 _1002100260517...）
            int under = detail.indexOf('_');
            if (under > 0 && under < detail.length() - 5) {
                detail = detail.substring(0, under).trim();
            }
        }

        // 若商户名无效或为 "/"，且交易说明不为空，智能从交易说明中提取商户名
        if (isInvalidMerchant(cleanPayee) && StrUtil.isNotBlank(detail)) {
            MerchantExtracted extracted = extractMerchantFromDetail(detail);
            if (extracted != null) {
                cleanPayee = extracted.merchant;
                detail = extracted.subDetail;
            } else {
                cleanPayee = detail.trim();
                detail = "";
            }
        }
        String rawType = cellToStr(row, colIdxMap.get("type"), "");
        String rawCategory = cellToStr(row, colIdxMap.get("category"), "");
        String status = cellToStr(row, colIdxMap.get("status"), "交易成功");
        String payment = cellToStr(row, colIdxMap.get("payment"), "系统导入");
        String txId = cellToStr(row, colIdxMap.get("txId"), null);

        // 收支方向归一化
        String normalizedType = normalizeDirection(rawType, rawAmountStr, cleanPayee, detail, rawCategory);

        BillImportItemDTO item = new BillImportItemDTO();
        item.setTime(time);
        item.setPayee(cleanPayee);
        if (StrUtil.isNotBlank(rawPayee) && !rawPayee.trim().equals(cleanPayee.trim())) {
            item.setRawPayee(rawPayee.trim());
        } else {
            item.setRawPayee(null);
        }
        item.setDetail(detail);
        item.setType(normalizedType);
        item.setAmount(amount.abs());
        item.setPayment(cleanValue(payment, "系统导入"));
        item.setStatus(cleanValue(status, "交易成功"));
        item.setCategory(StrUtil.isNotBlank(rawCategory) ? rawCategory : "待确认");
        item.setIsUncertain(true);
        item.setTxId(cleanValue(txId, null));
        item.setIsForeign(isForeign);
        item.setRawAmount(rawAmountStr);

        return item;
    }

    /**
     * 子类增强钩子（用于渠道专有单号、状态或支付方式修正）
     */
    protected void postProcessItem(BillImportItemDTO item, List<Object> row, Map<String, Integer> colIdxMap) {
        // 默认空实现，由子类重写扩展
    }

    /**
     * 金额清洗：剔除货币符号、千分位逗号，提取纯数字
     */
    protected BigDecimal cleanAmount(String rawAmountStr) {
        if (StrUtil.isBlank(rawAmountStr)) return null;
        // 去除空格、人民币符号、美元符号、分号、制表符等
        String s = rawAmountStr.replace("¥", "")
                .replace("￥", "")
                .replace("$", "")
                .replace("HK$", "")
                .replace("€", "")
                .replace("元", "")
                .replace(",", "")
                .replace("　", "")
                .trim();

        // 兼容括号负数，如 (120.00) -> -120.00
        if (s.startsWith("(") && s.endsWith(")")) {
            s = "-" + s.substring(1, s.length() - 1).trim();
        }

        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            // 正则提取第一段合法的浮点数字
            Matcher m = Pattern.compile("[-+]?\\d+(\\.\\d+)?").matcher(s);
            if (m.find()) {
                try {
                    return new BigDecimal(m.group());
                } catch (Exception ignored) {
                }
            }
            return null;
        }
    }

    /**
     * 判断是否包含外币标志
     */
    protected boolean detectForeignCurrency(String text) {
        if (StrUtil.isBlank(text)) return false;
        String t = text.toUpperCase();
        return t.contains("$") || t.contains("USD") || t.contains("HKD") || t.contains("HK$")
                || t.contains("EUR") || t.contains("€") || t.contains("JPY") || t.contains("GBP");
    }

    /**
     * 时间日期自适应解析（支持 Date、Excel 数值日期及常见文本格式）
     */
    protected Date parseDateTime(Object timeObj) {
        if (timeObj == null) {
            return new Date();
        }
        if (timeObj instanceof Date) {
            return (Date) timeObj;
        }
        if (timeObj instanceof Number) {
            double d = ((Number) timeObj).doubleValue();
            if (d > 20000 && d < 75000) {
                try {
                    return org.apache.poi.ss.usermodel.DateUtil.getJavaDate(d);
                } catch (Exception ignored) {
                }
            }
        }

        String str = timeObj.toString().replace("\uFEFF", "").trim();
        if (StrUtil.isBlank(str)) {
            return new Date();
        }

        // 字符串型 Excel OA Date 浮点数支持（如微信 Excel 导出的 46159.749444444446）
        try {
            if (cn.hutool.core.util.NumberUtil.isNumber(str)) {
                double d = Double.parseDouble(str);
                if (d > 20000 && d < 75000) {
                    return org.apache.poi.ss.usermodel.DateUtil.getJavaDate(d);
                }
            }
        } catch (Exception ignored) {
        }

        try {
            return DateUtil.parse(str);
        } catch (Exception ignored) {
        }

        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm",
                "yyyy-MM-dd", "yyyy/MM/dd", "dd/MM/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm:ss",
                "dd/MM/yyyy", "MM/dd/yyyy", "yyyyMMddHHmmss", "yyyyMMdd"
        };
        for (String p : patterns) {
            try {
                return DateUtil.parse(str, p);
            } catch (Exception ignored) {
            }
        }

        return new Date();
    }

    /**
     * 收支方向标准化推导
     */
    protected String normalizeDirection(String rawType, String rawAmountStr, String payee, String detail, String rawCategory) {
        if (StrUtil.isNotBlank(rawType)) {
            String t = rawType.trim();
            if (t.contains("支") || t.contains("借") || t.equalsIgnoreCase("Debit") || t.equalsIgnoreCase("PAY") || t.equalsIgnoreCase("Payment")) {
                return "支出";
            }
            if ((t.contains("收") && !t.contains("不计")) || t.contains("贷") || t.equalsIgnoreCase("Credit") || t.equalsIgnoreCase("Income")) {
                return "收入";
            }
        }

        // 若金额为负数且无明确收支列，在部分网银或 PayPal 中负数代表支出，正数代表收入
        if (StrUtil.isNotBlank(rawAmountStr) && (rawAmountStr.contains("-") || (rawAmountStr.startsWith("(") && rawAmountStr.endsWith(")")))) {
            return "支出";
        }

        // 语义推断
        String combined = (payee + " " + detail + " " + rawCategory);
        if (combined.contains("退款") || combined.contains("转入") || combined.contains("收款") || combined.contains("收到") || combined.contains("工资") || combined.contains("薪资")) {
            return "收入";
        }
        return "支出";
    }

    private static final Pattern DETAIL_DELIMITER_PATTERN = Pattern.compile("^([\\w\\u4e00-\\u9fa5]+?)(出售|代发|充值|退款|快捷支付|支付|App)?[-_—:|\\s]+(.*)$");

    protected boolean isInvalidMerchant(String payee) {
        if (StrUtil.isBlank(payee)) return true;
        String p = payee.trim();
        return "/".equals(p) || "-".equals(p) || ".".equals(p) || "未知商户".equals(p) || "未知".equals(p) || "其他".equals(p);
    }

    protected MerchantExtracted extractMerchantFromDetail(String detail) {
        if (StrUtil.isBlank(detail)) return null;
        String d = detail.trim();
        Matcher m = DETAIL_DELIMITER_PATTERN.matcher(d);
        if (m.find()) {
            String brand = m.group(1).trim();
            String action = m.group(2) != null ? m.group(2).trim() : "";
            String rest = m.group(3) != null ? m.group(3).trim() : "";
            if (brand.length() >= 2) {
                String sub = StrUtil.isNotBlank(action) ? (action + " " + rest).trim() : rest;
                return new MerchantExtracted(brand, sub);
            }
        }
        return null;
    }

    protected static class MerchantExtracted {
        public final String merchant;
        public final String subDetail;
        public MerchantExtracted(String merchant, String subDetail) {
            this.merchant = merchant;
            this.subDetail = subDetail != null ? subDetail.trim() : "";
        }
    }

    protected String cleanMerchantName(String payee) {
        if (isInvalidMerchant(payee)) {
            return "未知商户";
        }
        String p = payee.trim();
        // 清除结尾长单号后缀，如 "商户名_100201..."
        int under = p.indexOf('_');
        if (under > 0 && under < p.length() - 5) {
            p = p.substring(0, under).trim();
        }
        return p;
    }

    protected String cleanValue(String val, String defaultVal) {
        if (val == null || "/".equals(val.trim()) || StrUtil.isBlank(val)) {
            return defaultVal;
        }
        return val.trim();
    }

    protected Object getCell(List<Object> row, Integer idx) {
        if (idx == null || idx < 0 || idx >= row.size()) return null;
        return row.get(idx);
    }

    protected String cellToStr(List<Object> row, Integer idx, String defaultVal) {
        Object val = getCell(row, idx);
        if (val == null) return defaultVal;
        String s = val.toString().replace("\uFEFF", "").trim();
        return s.isEmpty() ? defaultVal : s;
    }
}
