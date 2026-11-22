package com.cxy1ng.service.parser;

import cn.hutool.core.util.StrUtil;
import com.cxy1ng.dto.bill.BillImportItemDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 银行流水账单策略解析器
 * 适配各商业银行（招行、工行、建行、中行等）借记卡与信用卡通用对账单，兼容借/贷与多币种标记
 */
@Component
public class BankBillParser extends AbstractBillParser {

    @Override
    public boolean supports(String channel) {
        if (channel == null) return false;
        String c = channel.trim().toLowerCase();
        return c.equals("bank") || c.equals("unionpay") || c.equals("card")
                || c.equals("cmb") || c.equals("icbc") || c.equals("boc") || c.equals("ccb")
                || c.contains("银行") || c.contains("银联");
    }

    @Override
    protected void postProcessItem(BillImportItemDTO item, List<Object> row, Map<String, Integer> colIdxMap) {
        // 1. 支付方式默认为银行卡
        String payment = item.getPayment();
        if (StrUtil.isBlank(payment) || "系统导入".equals(payment) || "/".equals(payment)) {
            item.setPayment("银行卡");
        }

        // 2. 状态收敛
        if (StrUtil.isBlank(item.getStatus()) || "/".equals(item.getStatus())) {
            item.setStatus("交易成功");
        }

        // 3. 银行对账单多币种探测（若明细或金额包含外币标记）
        if (!Boolean.TRUE.equals(item.getIsForeign())) {
            String combined = (item.getDetail() + " " + item.getRawAmount() + " " + item.getPayee());
            if (detectForeignCurrency(combined)) {
                item.setIsForeign(true);
            }
        }
    }
}
