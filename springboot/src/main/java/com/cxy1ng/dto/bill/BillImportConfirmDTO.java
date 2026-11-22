package com.cxy1ng.dto.bill;

import java.io.Serializable;
import java.util.List;

/**
 * 账单确认入库提交 DTO
 */
public class BillImportConfirmDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 经用户在预检/分类向导确认后的账单列表
     */
    private List<BillImportItemDTO> items;

    /**
     * 归属用户 ID（若为空则从当前登录 Token 中解析）
     */
    private Integer userId;

    /**
     * 流水来源渠道（WECHAT、ALIPAY、BANK、PAYPAL 等）
     */
    private String channel;

    /**
     * 排重处理策略：SKIP（自动跳过重复流水）、KEEP_ALL（全部保留入库）
     */
    private String duplicateAction;

    public BillImportConfirmDTO() {
    }

    public BillImportConfirmDTO(List<BillImportItemDTO> items) {
        this.items = items;
    }

    public BillImportConfirmDTO(List<BillImportItemDTO> items, Integer userId, String channel, String duplicateAction) {
        this.items = items;
        this.userId = userId;
        this.channel = channel;
        this.duplicateAction = duplicateAction;
    }

    public List<BillImportItemDTO> getItems() {
        return items;
    }

    public void setItems(List<BillImportItemDTO> items) {
        this.items = items;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getDuplicateAction() {
        return duplicateAction;
    }

    public void setDuplicateAction(String duplicateAction) {
        this.duplicateAction = duplicateAction;
    }

    @Override
    public String toString() {
        return "BillImportConfirmDTO{" +
                "itemsCount=" + (items != null ? items.size() : 0) +
                ", userId=" + userId +
                ", channel='" + channel + '\'' +
                ", duplicateAction='" + duplicateAction + '\'' +
                '}';
    }
}
