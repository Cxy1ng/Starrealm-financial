package com.cxy1ng.service.parser;

import cn.hutool.core.util.StrUtil;
import com.cxy1ng.dto.bill.BillImportItemDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 微信支付多格式账单策略解析器
 * 适配微信官方导出的 .xlsx 与 .csv 流水，自动跳过前置元数据区域，定位表头并提取微信交易单号
 */
@Component
public class WeChatBillParser extends AbstractBillParser {

    @Override
    public boolean supports(String channel) {
        if (channel == null) return false;
        String c = channel.trim().toLowerCase();
        return c.equals("wechat") || c.equals("weixin") || c.equals("wx") || c.contains("微信");
    }

    @Override
    protected void postProcessItem(BillImportItemDTO item, List<Object> row, Map<String, Integer> colIdxMap) {
        // 1. 归一化支付方式
        String payment = item.getPayment();
        if (StrUtil.isBlank(payment) || "系统导入".equals(payment) || "/".equals(payment)) {
            item.setPayment("微信支付");
        }

        // 2. 状态语义收敛（消除金额污染，如 已全额退款(¥39.00) -> 已退款）
        String status = item.getStatus();
        if (status != null) {
            if (status.contains("退款")) {
                item.setStatus("已退款");
            } else if (status.contains("成功") || status.contains("已存入零钱") || status.contains("支付成功")) {
                item.setStatus("交易成功");
            } else if (status.contains("关闭") || status.contains("撤销") || status.contains("失败")) {
                item.setStatus("交易关闭");
            }
        }

        // 3. 收款方缺省与清洗
        if ("未知商户".equals(item.getPayee()) && StrUtil.isNotBlank(item.getDetail())) {
            item.setPayee(cleanMerchantName(item.getDetail()));
        }

        // 4. 单号提取与补充
        if (StrUtil.isBlank(item.getTxId())) {
            // 尝试读取备用单号列（商户单号）
            Integer txCol = colIdxMap.get("txId");
            if (txCol != null && txCol + 1 < row.size()) {
                String subId = cellToStr(row, txCol + 1, null);
                if (StrUtil.isNotBlank(subId) && !"/".equals(subId)) {
                    item.setTxId(subId.trim());
                }
            }
        }
    }
}
