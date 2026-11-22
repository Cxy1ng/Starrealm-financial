package com.cxy1ng.service;

import cn.hutool.core.util.StrUtil;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.dto.bill.BillImportConfirmDTO;
import com.cxy1ng.dto.bill.BillImportItemDTO;
import com.cxy1ng.dto.bill.BillImportPrecheckVO;
import com.cxy1ng.entity.Account;
import com.cxy1ng.entity.Bill;
import com.cxy1ng.exception.CustomException;
import com.cxy1ng.service.parser.ChannelBillParser;
import com.cxy1ng.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 账单智能导入与解析编排服务
 * 统一协调多渠道策略解析器（微信、支付宝、银行卡、PayPal），提供预检指标聚合与批量入账
 */
@Service
public class BillImportService {

    private static final Logger log = LoggerFactory.getLogger(BillImportService.class);

    @Resource
    private List<ChannelBillParser> parsers;

    @Resource
    private BillService billService;

    @Resource
    private BillDeduplicationEngine deduplicationEngine;

    /**
     * 上传流水文件执行预检解析，返回统计指标与明细行
     *
     * @param file    上传的账单文件（XLSX、CSV 等）
     * @param channel 渠道标识（wechat, alipay, bank, paypal 等）
     * @param userId  操作用户 ID
     * @return 预检统计及解析后条目封装
     */
    public BillImportPrecheckVO precheck(MultipartFile file, String channel, Integer userId) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }

        String filename = file.getOriginalFilename();
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            log.error("Failed to read bytes from uploaded file: {}", filename, e);
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        // 1. 内容指纹快速嗅探与双向校验
        String sniffedChannel = sniffChannel(fileBytes, filename);
        String finalChannel = channel;
        boolean isCorrected = false;
        String notice = null;

        if (StrUtil.isNotBlank(sniffedChannel)) {
            if (StrUtil.isNotBlank(channel) && !channel.equalsIgnoreCase(sniffedChannel)) {
                // 用户选定的渠道与文件内容基因指纹确凿冲突 -> 触发智能纠偏
                log.info("【账单导入智能纠偏】用户选择渠道为 [{}]，但文件内容指纹判定为 [{}]，自动切换策略解析器", channel, sniffedChannel);
                finalChannel = sniffedChannel;
                isCorrected = true;
                notice = "系统检测到您上传的文件实际为【" + getChannelLabel(sniffedChannel) + "】账单，已自动智能纠偏并选用专属解析器！";
            } else if (StrUtil.isBlank(channel)) {
                finalChannel = sniffedChannel;
            }
        }

        ChannelBillParser parser = resolveParser(finalChannel, filename);
        if (parser == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        List<BillImportItemDTO> items;
        try (InputStream is = new java.io.ByteArrayInputStream(fileBytes)) {
            items = parser.parse(is, filename, userId);
        } catch (Exception e) {
            log.error("Failed to parse bill file: {}", filename, e);
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        if (items == null) {
            items = Collections.emptyList();
        }

        // 调用跨渠道及同渠道多维滑动窗口排重引擎
        if (deduplicationEngine != null && !items.isEmpty()) {
            deduplicationEngine.detectDuplicates(items, userId, finalChannel);
        }

        BillImportPrecheckVO vo = BillImportPrecheckVO.of(items, finalChannel, filename);
        vo.setIsChannelCorrected(isCorrected);
        vo.setOriginalChannel(channel);
        vo.setCorrectionNotice(notice);
        return vo;
    }

    /**
     * 确认导入账单提交落库
     *
     * @param confirmDTO 用户确认后的账单条目与入库选项
     * @return 成功入库记录数
     */
    @Transactional
    public int confirm(BillImportConfirmDTO confirmDTO) {
        if (confirmDTO == null || confirmDTO.getItems() == null || confirmDTO.getItems().isEmpty()) {
            return 0;
        }

        Integer userId = confirmDTO.getUserId();
        if (userId == null) {
            try {
                Account currentUser = TokenUtils.getCurrentUser();
                if (currentUser != null) {
                    userId = currentUser.getId();
                }
            } catch (Exception ignored) {
            }
        }

        String action = confirmDTO.getDuplicateAction();
        boolean skipDuplicates = StrUtil.isBlank(action)
                || "auto_merge".equalsIgnoreCase(action)
                || "SKIP".equalsIgnoreCase(action)
                || "skip_duplicates".equalsIgnoreCase(action);

        List<Bill> billList = new ArrayList<>();
        for (BillImportItemDTO item : confirmDTO.getItems()) {
            if (item == null) continue;

            // 根据排重策略跳过冲突流水
            if (skipDuplicates && Boolean.TRUE.equals(item.getIsConflict())) {
                log.info("跳过疑似重复流水: payee={}, amount={}, conflictReason={}",
                        item.getPayee(), item.getAmount(), item.getConflictReason());
                continue;
            }

            Bill b = new Bill();
            b.setTime(item.getTime() != null ? item.getTime() : new Date());
            b.setPayee(StrUtil.isNotBlank(item.getPayee()) ? item.getPayee() : "未知商户");
            b.setDetail(item.getDetail() != null ? item.getDetail() : "");
            b.setType(StrUtil.isNotBlank(item.getType()) ? item.getType() : "支出");
            b.setAmount(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
            b.setPayment(StrUtil.isNotBlank(item.getPayment()) ? item.getPayment() : "导入账单");
            b.setStatus(StrUtil.isNotBlank(item.getStatus()) ? item.getStatus() : "交易成功");

            // 分类受控保护：若仍为“待确认”或未指定，安全降级至收支对应兜底类别
            String category = item.getCategory();
            if (StrUtil.isBlank(category) || "待确认".equals(category)) {
                category = "支出".equals(b.getType()) ? "其他支出" : "其他收入";
            }
            b.setCategory(category);

            // 交易单号注入备注以便追溯
            String comment = "";
            if (StrUtil.isNotBlank(item.getTxId())) {
                comment = "单号:" + item.getTxId();
            }
            b.setComment(comment);
            b.setUserId(userId);

            billList.add(b);
        }

        int count = billService.insertBatch(billList, userId);
        if (userId != null) {
            billService.syncUserAcfun(userId);
        }
        return count;
    }

    /**
     * 根据渠道标识或文件名动态解析匹配的策略解析器
     */
    public ChannelBillParser resolveParser(String channel, String filename) {
        if (parsers == null || parsers.isEmpty()) {
            return null;
        }

        // 1. 优先按指定 channel 匹配
        if (StrUtil.isNotBlank(channel)) {
            for (ChannelBillParser p : parsers) {
                if (p.supports(channel)) {
                    return p;
                }
            }
        }

        // 2. 按文件名特征探测
        if (StrUtil.isNotBlank(filename)) {
            String lower = filename.toLowerCase();
            for (ChannelBillParser p : parsers) {
                if (lower.contains("wechat") || lower.contains("微信")) {
                    if (p.supports("wechat")) return p;
                } else if (lower.contains("alipay") || lower.contains("支付宝")) {
                    if (p.supports("alipay")) return p;
                } else if (lower.contains("paypal") || lower.contains("贝宝")) {
                    if (p.supports("paypal")) return p;
                } else if (lower.contains("bank") || lower.contains("银行") || lower.contains("银联") || lower.contains("card")) {
                    if (p.supports("bank")) return p;
                }
            }
        }

        // 3. 兜底返回首个解析器（通常为 WeChatBillParser）
        return parsers.get(0);
    }

    public List<ChannelBillParser> getParsers() {
        return parsers;
    }

    public void setParsers(List<ChannelBillParser> parsers) {
        this.parsers = parsers;
    }

    public BillService getBillService() {
        return billService;
    }

    public void setBillService(BillService billService) {
        this.billService = billService;
    }

    public BillDeduplicationEngine getDeduplicationEngine() {
        return deduplicationEngine;
    }

    public void setDeduplicationEngine(BillDeduplicationEngine deduplicationEngine) {
        this.deduplicationEngine = deduplicationEngine;
    }

    /**
     * 文件内容指纹快速嗅探器（支持 CSV / XLSX 核心元数据与列指纹探测）
     */
    public String sniffChannel(byte[] bytes, String filename) {
        if (bytes == null || bytes.length == 0) return null;

        String headText = "";
        try {
            int len = Math.min(bytes.length, 8192);
            String utf8 = new String(bytes, 0, len, java.nio.charset.StandardCharsets.UTF_8);
            String gbk = new String(bytes, 0, len, java.nio.charset.Charset.forName("GBK"));
            headText = utf8 + " " + gbk;
        } catch (Exception ignored) {
        }

        // 如果是 XLSX 容器文件，提取 sharedStrings 或 sheet1 XML 片段
        if (headText.startsWith("PK") && bytes.length > 100) {
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if ("xl/sharedStrings.xml".equals(entry.getName()) || entry.getName().endsWith("sheet1.xml")) {
                        byte[] buf = new byte[8192];
                        int r = zis.read(buf);
                        if (r > 0) {
                            headText += " " + new String(buf, 0, r, java.nio.charset.StandardCharsets.UTF_8);
                        }
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 1. 支付宝专属指纹
        if (headText.contains("支付宝交易记录") || headText.contains("支付宝交易号") || headText.contains("商家订单号")
                || headText.contains("资金状态") || headText.contains("alipay.com")) {
            return "alipay";
        }

        // 2. 微信支付专属指纹
        if (headText.contains("微信支付账单") || headText.contains("微信支付交易单号") || headText.contains("商户单号")
                || headText.contains("微信昵称") || headText.contains("零钱通存取")) {
            return "wechat";
        }

        // 3. PayPal 跨境专属指纹
        if (headText.contains("PayPal") || (headText.contains("Transaction ID") && headText.contains("Gross"))) {
            return "paypal";
        }

        // 4. 商业银行流水专属指纹
        if (headText.contains("借贷标志") || headText.contains("联机余额") || headText.contains("招商银行")
                || headText.contains("工商银行") || headText.contains("建设银行") || headText.contains("中国银行")
                || headText.contains("农业银行") || headText.contains("交通银行") || headText.contains("一卡通")
                || headText.contains("卡号/账号")) {
            return "bank";
        }

        return null;
    }

    public String getChannelLabel(String ch) {
        if ("wechat".equalsIgnoreCase(ch)) return "微信支付";
        if ("alipay".equalsIgnoreCase(ch)) return "支付宝";
        if ("bank".equalsIgnoreCase(ch)) return "银行卡流水";
        if ("paypal".equalsIgnoreCase(ch)) return "PayPal";
        return ch != null ? ch : "所选渠道";
    }
}
