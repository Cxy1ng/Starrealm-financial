package com.cxy1ng.service.parser;

import cn.hutool.core.util.StrUtil;
import com.cxy1ng.dto.bill.BillImportItemDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 支付宝多格式账单策略解析器
 * 支持支付宝官方导出的 CSV / XLSX 账单，自动过滤说明横幅与末尾统计汇总
 */
@Component
public class AlipayBillParser extends AbstractBillParser {

    @Override
    public boolean supports(String channel) {
        if (channel == null) return false;
        String c = channel.trim().toLowerCase();
        return c.equals("alipay") || c.equals("ali") || c.equals("zfb") || c.contains("支付宝");
    }

    @Override
    protected boolean isSummaryOrNoiseRow(List<Object> row, int rowIndex) {
        if (super.isSummaryOrNoiseRow(row, rowIndex)) {
            return true;
        }
        String first = cellToStr(row, 0, "");
        if (first.startsWith("----------------") || first.startsWith("================")) {
            return true;
        }
        if (first.contains("支付宝交易记录明细列表") || first.contains("账号:") || first.contains("起始日期:")) {
            return true;
        }
        if (first.contains("共") && (first.contains("笔记录") || first.contains("笔交易"))) {
            return true;
        }
        if (row.size() <= 4 && (first.contains("已收入") || first.contains("已支出") || first.contains("导出时间"))) {
            return true;
        }
        return false;
    }

    @Override
    protected void postProcessItem(BillImportItemDTO item, List<Object> row, Map<String, Integer> colIdxMap) {
        // 1. 归一化支付方式
        String payment = item.getPayment();
        if (StrUtil.isBlank(payment) || "系统导入".equals(payment) || "/".equals(payment)) {
            item.setPayment("支付宝");
        }

        // 2. 状态语义收敛
        String status = item.getStatus();
        if (status != null) {
            if (status.contains("退款")) {
                item.setStatus("已退款");
            } else if (status.contains("成功") || status.contains("已完成")) {
                item.setStatus("交易成功");
            } else if (status.contains("关闭") || status.contains("失败") || status.contains("未付款")) {
                item.setStatus("交易关闭");
            }
        }

        // 3. 收款方缺省与清洗
        if ("未知商户".equals(item.getPayee()) && StrUtil.isNotBlank(item.getDetail())) {
            item.setPayee(cleanMerchantName(item.getDetail()));
        }
    }
}
