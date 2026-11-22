package com.cxy1ng.service;

import cn.hutool.core.util.StrUtil;
import com.cxy1ng.dto.bill.BillImportItemDTO;
import com.cxy1ng.entity.Bill;
import com.cxy1ng.mapper.BillMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 跨渠道及同渠道多维滑动窗口排重引擎
 * 实现：
 * 1. 同渠道全局官方单号幂等（批次内去重 + 历史入库单号比对）
 * 2. 跨渠道多维滑动窗口排重（120秒时间窗口 + 金额严格相等 + 借贷收支同向 + 卡号尾号与第三方签名交叉指纹比对）
 * 3. PayPal / 外币跨境多币种排重（时间容差窗口 + PAYPAL* 摘要特征提取）
 */
@Service
public class BillDeduplicationEngine {

    private static final Logger log = LoggerFactory.getLogger(BillDeduplicationEngine.class);

    @Resource
    private BillMapper billMapper;

    /**
     * 默认滑动时间窗口（秒）
     */
    private long slidingWindowSeconds = 120;

    private static final Pattern CARD_TAIL_PATTERN = Pattern.compile(".*?(\\d{4})\\)?$");
    private static final Pattern CARD_PAREN_PATTERN = Pattern.compile(".*?\\((\\d{4})\\).*?");
    private static final Pattern PAYPAL_SIGNATURE_PATTERN = Pattern.compile("(?i)PAYPAL\\s*\\*?\\s*([A-Za-z0-9_]+)");

    private static final List<String> THIRD_PARTY_SIGNATURES = Arrays.asList(
            "财付通", "微信支付", "微信", "支付宝", "快捷支付", "网联", "网银在线"
    );

    private static final List<String> BANK_KEYWORDS = Arrays.asList(
            "招商银行", "工商银行", "建设银行", "中国银行", "农业银行", "交通银行",
            "浦发银行", "民生银行", "中信银行", "光大银行", "广发银行", "平安银行",
            "兴业银行", "邮储银行", "银行卡", "储蓄卡", "信用卡", "借记卡"
    );

    public BillDeduplicationEngine() {
    }

    public BillDeduplicationEngine(BillMapper billMapper) {
        this.billMapper = billMapper;
    }

    public long getSlidingWindowSeconds() {
        return slidingWindowSeconds;
    }

    public void setSlidingWindowSeconds(long slidingWindowSeconds) {
        this.slidingWindowSeconds = slidingWindowSeconds;
    }

    public BillMapper getBillMapper() {
        return billMapper;
    }

    public void setBillMapper(BillMapper billMapper) {
        this.billMapper = billMapper;
    }

    /**
     * 对导入明细列表执行完整排重分析，直接在每个 DTO 上标注 isConflict、conflictReason 与 conflictWithId
     *
     * @param items   待导入条目列表
     * @param userId  操作用户 ID
     * @param channel 当前导入渠道
     */
    public void detectDuplicates(List<BillImportItemDTO> items, Integer userId, String channel) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<Bill> historyBills = Collections.emptyList();
        if (billMapper != null && userId != null) {
            try {
                Bill query = new Bill();
                query.setUserId(userId);
                List<Bill> dbList = billMapper.selectAll(query);
                if (dbList != null) {
                    historyBills = dbList;
                }
            } catch (Exception e) {
                log.warn("Failed to load historical bills for user {}: {}", userId, e.getMessage());
            }
        }

        detectDuplicates(items, historyBills, channel);
    }

    /**
     * 纯内存排重比对接口（支持无数据库环境的单元测试与轻量检测）
     *
     * @param items        待导入条目
     * @param historyBills 用户已入库的历史账单列表
     * @param channel      渠道标识
     */
    public void detectDuplicates(List<BillImportItemDTO> items, List<Bill> historyBills, String channel) {
        if (items == null || items.isEmpty()) {
            return;
        }
        if (historyBills == null) {
            historyBills = Collections.emptyList();
        }

        // ==========================================
        // A1. 同渠道批次内自我去重（检测同批上传相同 txId）
        // ==========================================
        Set<String> seenBatchTxIds = new HashSet<>();
        for (BillImportItemDTO item : items) {
            if (Boolean.TRUE.equals(item.getIsConflict())) continue;
            String txId = item.getTxId();
            if (StrUtil.isNotBlank(txId)) {
                if (seenBatchTxIds.contains(txId)) {
                    item.setIsConflict(true);
                    item.setConflictReason("同批次包含重复官方交易单号");
                } else {
                    seenBatchTxIds.add(txId);
                }
            }
        }

        // ==========================================
        // A2. 同渠道官方单号与数据库历史记录比对
        // ==========================================
        for (BillImportItemDTO item : items) {
            if (Boolean.TRUE.equals(item.getIsConflict())) continue;
            String txId = item.getTxId();
            if (StrUtil.isBlank(txId)) continue;

            for (Bill hist : historyBills) {
                if (isTxIdMatch(txId, hist)) {
                    item.setIsConflict(true);
                    item.setConflictReason("同渠道官方单号重复");
                    item.setConflictWithId(hist.getId());
                    break;
                }
            }
        }

        // ==========================================
        // B. 跨渠道多维滑动窗口排重（比对历史账单）
        // ==========================================
        for (BillImportItemDTO item : items) {
            if (Boolean.TRUE.equals(item.getIsConflict())) continue;

            for (Bill hist : historyBills) {
                // 1. 常规跨渠道绑卡排重（如 微信/支付宝 vs 银行流水）
                if (isCrossChannelDuplicate(item, hist)) {
                    item.setIsConflict(true);
                    item.setConflictReason("识别到跨渠道绑卡重复流水");
                    item.setConflictWithId(hist.getId());
                    break;
                }

                // 2. PayPal / 外币多币种排重
                if (isPayPalCrossDuplicate(item, hist)) {
                    item.setIsConflict(true);
                    item.setConflictReason("识别到跨渠道绑卡重复流水");
                    item.setConflictWithId(hist.getId());
                    break;
                }
            }
        }

        // ==========================================
        // C. 同批次内跨渠道滑动窗口排重（处理混合渠道导入）
        // ==========================================
        for (int i = 0; i < items.size(); i++) {
            BillImportItemDTO item1 = items.get(i);
            if (Boolean.TRUE.equals(item1.getIsConflict())) continue;

            for (int j = i + 1; j < items.size(); j++) {
                BillImportItemDTO item2 = items.get(j);
                if (Boolean.TRUE.equals(item2.getIsConflict())) continue;

                if (isCrossChannelItemsDuplicate(item1, item2)) {
                    item2.setIsConflict(true);
                    item2.setConflictReason("识别到跨渠道绑卡重复流水");
                }
            }
        }
    }

    /**
     * 判断历史单号是否匹配
     */
    protected boolean isTxIdMatch(String txId, Bill hist) {
        if (hist == null || StrUtil.isBlank(txId)) return false;
        String comment = hist.getComment();
        if (comment != null && comment.contains(txId)) {
            return true;
        }
        String detail = hist.getDetail();
        if (detail != null && detail.contains(txId)) {
            return true;
        }
        return false;
    }

    /**
     * 判断单条导入条目与历史记录是否满足跨渠道滑动窗口排重
     */
    protected boolean isCrossChannelDuplicate(BillImportItemDTO item, Bill hist) {
        if (item == null || hist == null) return false;
        if (item.getAmount() == null || hist.getAmount() == null) return false;

        // 1. 金额必须严格相等
        if (item.getAmount().compareTo(hist.getAmount()) != 0) {
            return false;
        }

        // 2. 收支方向必须同向
        String t1 = item.getType();
        String t2 = hist.getType();
        if (t1 != null && t2 != null && !t1.equals(t2)) {
            return false;
        }

        // 3. 时间在滑动窗口内
        Date d1 = item.getTime();
        Date d2 = hist.getTime();
        if (d1 == null || d2 == null) {
            return false;
        }
        long diffSec = Math.abs(d1.getTime() - d2.getTime()) / 1000;
        if (diffSec > slidingWindowSeconds) {
            return false;
        }

        // 4. 交叉指纹验证
        return matchCrossChannelFingerprint(
                item.getPayment(), item.getPayee(), item.getDetail(),
                hist.getPayment(), hist.getPayee(), hist.getDetail()
        );
    }

    /**
     * 判断两个导入条目之间是否满足跨渠道滑动窗口排重
     */
    protected boolean isCrossChannelItemsDuplicate(BillImportItemDTO item1, BillImportItemDTO item2) {
        if (item1 == null || item2 == null) return false;
        if (item1.getAmount() == null || item2.getAmount() == null) return false;

        if (item1.getAmount().compareTo(item2.getAmount()) != 0) {
            return false;
        }

        String t1 = item1.getType();
        String t2 = item2.getType();
        if (t1 != null && t2 != null && !t1.equals(t2)) {
            return false;
        }

        Date d1 = item1.getTime();
        Date d2 = item2.getTime();
        if (d1 == null || d2 == null) return false;

        long diffSec = Math.abs(d1.getTime() - d2.getTime()) / 1000;
        if (diffSec > slidingWindowSeconds) return false;

        return matchCrossChannelFingerprint(
                item1.getPayment(), item1.getPayee(), item1.getDetail(),
                item2.getPayment(), item2.getPayee(), item2.getDetail()
        );
    }

    /**
     * 交叉指纹比对：
     * 1. 卡号尾号匹配（如 "招商银行储蓄卡(8899)" 与银行卡 "(8899)"）
     * 2. 第三方支付签名（"财付通"、"微信支付"、"支付宝"、"快捷支付"）与银行渠道关联
     * 3. 商户名称交叉匹配（如银行摘要中出现第三方商户名）
     */
    protected boolean matchCrossChannelFingerprint(String payment1, String payee1, String detail1,
                                                 String payment2, String payee2, String detail2) {
        String p1 = StrUtil.nullToEmpty(payment1);
        String p2 = StrUtil.nullToEmpty(payment2);
        String text1 = (payment1 + " " + payee1 + " " + detail1).toLowerCase();
        String text2 = (payment2 + " " + payee2 + " " + detail2).toLowerCase();

        // 1. 提取银行卡尾号比对
        String tail1 = extractCardTail(payment1);
        String tail2 = extractCardTail(payment2);
        if (tail1 == null) tail1 = extractCardTail(text1);
        if (tail2 == null) tail2 = extractCardTail(text2);

        if (tail1 != null && tail2 != null && tail1.equals(tail2)) {
            return true;
        }

        // 2. 一方为银行卡渠道，另一方包含第三方支付签名（如 财付通/微信/支付宝/快捷支付）
        boolean text1HasThirdParty = hasThirdPartySignature(text1);
        boolean text2HasThirdParty = hasThirdPartySignature(text2);
        boolean text1HasBank = hasBankKeyword(text1);
        boolean text2HasBank = hasBankKeyword(text2);

        if ((text1HasThirdParty && text2HasBank) || (text2HasThirdParty && text1HasBank)) {
            return true;
        }

        // 3. 商户名跨字段包含（如 银行明细出现第三方商户名）
        String py1 = StrUtil.nullToEmpty(payee1).trim();
        String py2 = StrUtil.nullToEmpty(payee2).trim();
        if (py1.length() >= 2 && text2.contains(py1.toLowerCase())) {
            return true;
        }
        if (py2.length() >= 2 && text1.contains(py2.toLowerCase())) {
            return true;
        }

        return false;
    }

    /**
     * PayPal 跨币种 / 信用卡跨渠道排重判断
     */
    protected boolean isPayPalCrossDuplicate(BillImportItemDTO item, Bill hist) {
        if (item == null || hist == null) return false;
        if (item.getAmount() == null || hist.getAmount() == null) return false;

        // 金额必须相等
        if (item.getAmount().compareTo(hist.getAmount()) != 0) {
            return false;
        }

        // 时间滑动窗口放宽至 24 小时（兼容国际信用卡记账与时区差异）
        Date d1 = item.getTime();
        Date d2 = hist.getTime();
        if (d1 == null || d2 == null) return false;
        long diffSec = Math.abs(d1.getTime() - d2.getTime()) / 1000;
        if (diffSec > 86400) {
            return false;
        }

        String combinedHist = (hist.getPayee() + " " + hist.getDetail() + " " + hist.getPayment()).toUpperCase();
        String combinedItem = (item.getPayee() + " " + item.getDetail() + " " + item.getPayment()).toUpperCase();

        // 探测 PAYPAL* 商户前缀
        Matcher m = PAYPAL_SIGNATURE_PATTERN.matcher(combinedHist);
        if (m.find()) {
            String ppMerchant = m.group(1).toUpperCase();
            if (combinedItem.contains(ppMerchant)) {
                return true;
            }
        }

        Matcher m2 = PAYPAL_SIGNATURE_PATTERN.matcher(combinedItem);
        if (m2.find()) {
            String ppMerchant = m2.group(1).toUpperCase();
            if (combinedHist.contains(ppMerchant)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 提取卡号后 4 位
     */
    protected String extractCardTail(String str) {
        if (StrUtil.isBlank(str)) return null;
        Matcher m1 = CARD_PAREN_PATTERN.matcher(str);
        if (m1.find()) {
            return m1.group(1);
        }
        Matcher m2 = CARD_TAIL_PATTERN.matcher(str);
        if (m2.find()) {
            return m2.group(1);
        }
        return null;
    }

    protected boolean hasThirdPartySignature(String text) {
        if (StrUtil.isBlank(text)) return false;
        for (String sig : THIRD_PARTY_SIGNATURES) {
            if (text.contains(sig.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasBankKeyword(String text) {
        if (StrUtil.isBlank(text)) return false;
        for (String kw : BANK_KEYWORDS) {
            if (text.contains(kw.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
