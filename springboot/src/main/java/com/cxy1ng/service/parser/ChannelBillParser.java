package com.cxy1ng.service.parser;

import com.cxy1ng.dto.bill.BillImportItemDTO;

import java.io.InputStream;
import java.util.List;

/**
 * 渠道账单解析器策略接口
 * 用于支持不同支付渠道（微信、支付宝、银行流水、PayPal 等）多格式流水文件的解析与标准化
 */
public interface ChannelBillParser {

    /**
     * 解析上传的流水文件并返回标准化预览行 DTO 列表
     *
     * @param inputStream 上传文件的输入流
     * @param filename    原始文件名（含后缀，如 .xlsx、.csv 等）
     * @param userId      当前操作用户的 ID
     * @return 标准化的账单导入行列表
     */
    List<BillImportItemDTO> parse(InputStream inputStream, String filename, Integer userId);

    /**
     * 当前解析器是否支持指定的渠道
     *
     * @param channel 渠道标识（如 WECHAT、ALIPAY、BANK、PAYPAL 等）
     * @return true 若支持该渠道
     */
    boolean supports(String channel);
}
