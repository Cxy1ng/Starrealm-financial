package com.cxy1ng.dto.bill;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 账单导入预检结果 VO (Precheck Result)
 */
public class BillImportPrecheckVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 账单总笔数
     */
    private Integer totalCount;

    /**
     * 自动精准分类笔数
     */
    private Integer autoCategorizedCount;

    /**
     * 待确认/未分类笔数
     */
    private Integer uncertainCount;

    /**
     * 疑似重复笔数
     */
    private Integer conflictCount;

    /**
     * 账单总金额合计
     */
    private BigDecimal totalAmount;

    /**
     * 导入渠道
     */
    private String channel;

    /**
     * 原始文件名
     */
    private String filename;

    /**
     * 解析与预检后的明细记录列表
     */
    private List<BillImportItemDTO> items;

    /**
     * 是否触发了渠道智能自动纠偏
     */
    private Boolean isChannelCorrected = false;

    /**
     * 用户前端最初选中的渠道
     */
    private String originalChannel;

    /**
     * 纠偏提示文本
     */
    private String correctionNotice;

    public BillImportPrecheckVO() {
        this.totalCount = 0;
        this.autoCategorizedCount = 0;
        this.uncertainCount = 0;
        this.conflictCount = 0;
        this.totalAmount = BigDecimal.ZERO;
        this.items = new ArrayList<>();
    }

    public BillImportPrecheckVO(Integer totalCount, Integer autoCategorizedCount, Integer uncertainCount,
                                Integer conflictCount, BigDecimal totalAmount, List<BillImportItemDTO> items) {
        this.totalCount = totalCount;
        this.autoCategorizedCount = autoCategorizedCount;
        this.uncertainCount = uncertainCount;
        this.conflictCount = conflictCount;
        this.totalAmount = totalAmount;
        this.items = items;
    }

    public BillImportPrecheckVO(Integer totalCount, Integer autoCategorizedCount, Integer uncertainCount,
                                BigDecimal totalAmount, List<BillImportItemDTO> items) {
        this(totalCount, autoCategorizedCount, uncertainCount, 0, totalAmount, items);
    }

    /**
     * 便捷静态构造：从行列表中聚合统计指标
     */
    public static BillImportPrecheckVO of(List<BillImportItemDTO> items) {
        BillImportPrecheckVO vo = new BillImportPrecheckVO();
        if (items == null || items.isEmpty()) {
            return vo;
        }

        vo.setItems(items);
        vo.setTotalCount(items.size());

        int autoCount = 0;
        int uncertCount = 0;
        int confCount = 0;
        BigDecimal sumAmount = BigDecimal.ZERO;

        for (BillImportItemDTO item : items) {
            if (Boolean.TRUE.equals(item.getIsUncertain()) || "待确认".equals(item.getCategory())) {
                uncertCount++;
            } else {
                autoCount++;
            }
            if (Boolean.TRUE.equals(item.getIsConflict())) {
                confCount++;
            }
            if (item.getAmount() != null) {
                sumAmount = sumAmount.add(item.getAmount());
            }
        }

        vo.setAutoCategorizedCount(autoCount);
        vo.setUncertainCount(uncertCount);
        vo.setConflictCount(confCount);
        vo.setTotalAmount(sumAmount);
        return vo;
    }

    /**
     * 便捷静态构造：从行列表中聚合统计指标并关联渠道与文件名
     */
    public static BillImportPrecheckVO of(List<BillImportItemDTO> items, String channel, String filename) {
        BillImportPrecheckVO vo = of(items);
        vo.setChannel(channel);
        vo.setFilename(filename);
        return vo;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getAutoCategorizedCount() {
        return autoCategorizedCount;
    }

    public void setAutoCategorizedCount(Integer autoCategorizedCount) {
        this.autoCategorizedCount = autoCategorizedCount;
    }

    public Integer getUncertainCount() {
        return uncertainCount;
    }

    public void setUncertainCount(Integer uncertainCount) {
        this.uncertainCount = uncertainCount;
    }

    public Integer getConflictCount() {
        return conflictCount;
    }

    public void setConflictCount(Integer conflictCount) {
        this.conflictCount = conflictCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<BillImportItemDTO> getItems() {
        return items;
    }

    public void setItems(List<BillImportItemDTO> items) {
        this.items = items;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Boolean getIsChannelCorrected() {
        return isChannelCorrected;
    }

    public void setIsChannelCorrected(Boolean channelCorrected) {
        isChannelCorrected = channelCorrected;
    }

    public String getOriginalChannel() {
        return originalChannel;
    }

    public void setOriginalChannel(String originalChannel) {
        this.originalChannel = originalChannel;
    }

    public String getCorrectionNotice() {
        return correctionNotice;
    }

    public void setCorrectionNotice(String correctionNotice) {
        this.correctionNotice = correctionNotice;
    }

    @Override
    public String toString() {
        return "BillImportPrecheckVO{" +
                "totalCount=" + totalCount +
                ", autoCategorizedCount=" + autoCategorizedCount +
                ", uncertainCount=" + uncertainCount +
                ", conflictCount=" + conflictCount +
                ", totalAmount=" + totalAmount +
                ", channel='" + channel + '\'' +
                ", filename='" + filename + '\'' +
                ", itemsSize=" + (items != null ? items.size() : 0) +
                '}';
    }
}
