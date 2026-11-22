package com.cxy1ng.service.parser;

import cn.hutool.core.util.StrUtil;
import com.cxy1ng.dto.bill.BillImportItemDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * PayPal 跨境流水策略解析器
 * 适配 PayPal 英文与多语言导出的 CSV / XLSX 流水，自动提取外币、交易号与原币信息
 */
@Component
public class PayPalBillParser extends AbstractBillParser {

    @Override
    public boolean supports(String channel) {
        if (channel == null) return false;
        String c = channel.trim().toLowerCase();
        return c.equals("paypal") || c.equals("pp") || c.contains("贝宝");
    }

    @Override
    protected void postProcessItem(BillImportItemDTO item, List<Object> row, Map<String, Integer> colIdxMap) {
        // 1. 支付方式缺省为 PayPal
        String payment = item.getPayment();
        if (StrUtil.isBlank(payment) || "系统导入".equals(payment) || "/".equals(payment)) {
            item.setPayment("PayPal");
        }

        // 2. 跨境/外币自动标记
        item.setIsForeign(true);

        // 3. 状态收敛（适配英文状态：Completed, Refunded, Cancelled, Denied, Pending）
        String status = item.getStatus();
        if (status != null) {
            String lower = status.toLowerCase();
            if (lower.contains("refund") || lower.contains("退款")) {
                item.setStatus("已退款");
            } else if (lower.contains("completed") || lower.contains("cleared") || lower.contains("成功")) {
                item.setStatus("交易成功");
            } else if (lower.contains("cancel") || lower.contains("denied") || lower.contains("failed") || lower.contains("关闭")) {
                item.setStatus("交易关闭");
            } else if (lower.contains("pending")) {
                item.setStatus("待处理");
            }
        }
    }
}
