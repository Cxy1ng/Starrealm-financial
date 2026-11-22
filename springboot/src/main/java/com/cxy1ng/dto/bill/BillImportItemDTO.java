package com.cxy1ng.dto.bill;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 单笔账单导入解析与预检行 DTO
 */
public class BillImportItemDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date time;

    private String payee;

    private String rawPayee;

    private String detail;

    private String type;

    private BigDecimal amount;

    private String payment;

    private String status;

    private String category;

    @JsonProperty("isUncertain")
    private Boolean isUncertain;

    private String txId;

    @JsonProperty("isForeign")
    private Boolean isForeign;

    private String rawAmount;

    @JsonProperty("isConflict")
    private Boolean isConflict = false;

    private String conflictReason;

    private Integer conflictWithId;

    public BillImportItemDTO() {
    }

    public BillImportItemDTO(Date time, String payee, String rawPayee, String detail, String type,
                             BigDecimal amount, String payment, String status, String category,
                             Boolean isUncertain, String txId, Boolean isForeign, String rawAmount) {
        this.time = time;
        this.payee = payee;
        this.rawPayee = rawPayee;
        this.detail = detail;
        this.type = type;
        this.amount = amount;
        this.payment = payment;
        this.status = status;
        this.category = category;
        this.isUncertain = isUncertain;
        this.txId = txId;
        this.isForeign = isForeign;
        this.rawAmount = rawAmount;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getPayee() {
        return payee;
    }

    public void setPayee(String payee) {
        this.payee = payee;
    }

    public String getRawPayee() {
        return rawPayee;
    }

    public void setRawPayee(String rawPayee) {
        this.rawPayee = rawPayee;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPayment() {
        return payment;
    }

    public void setPayment(String payment) {
        this.payment = payment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getIsUncertain() {
        return isUncertain;
    }

    public void setIsUncertain(Boolean uncertain) {
        isUncertain = uncertain;
    }

    public Boolean isUncertain() {
        return isUncertain;
    }

    public void setUncertain(Boolean uncertain) {
        isUncertain = uncertain;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public Boolean getIsForeign() {
        return isForeign;
    }

    public void setIsForeign(Boolean foreign) {
        isForeign = foreign;
    }

    public Boolean isForeign() {
        return isForeign;
    }

    public void setForeign(Boolean foreign) {
        isForeign = foreign;
    }

    public String getRawAmount() {
        return rawAmount;
    }

    public void setRawAmount(String rawAmount) {
        this.rawAmount = rawAmount;
    }

    public Boolean getIsConflict() {
        return isConflict;
    }

    public void setIsConflict(Boolean conflict) {
        isConflict = conflict;
    }

    public Boolean isConflict() {
        return isConflict;
    }

    public void setConflict(Boolean conflict) {
        isConflict = conflict;
    }

    public String getConflictReason() {
        return conflictReason;
    }

    public void setConflictReason(String conflictReason) {
        this.conflictReason = conflictReason;
    }

    public Integer getConflictWithId() {
        return conflictWithId;
    }

    public void setConflictWithId(Integer conflictWithId) {
        this.conflictWithId = conflictWithId;
    }

    @Override
    public String toString() {
        return "BillImportItemDTO{" +
                "time=" + time +
                ", payee='" + payee + '\'' +
                ", rawPayee='" + rawPayee + '\'' +
                ", detail='" + detail + '\'' +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", payment='" + payment + '\'' +
                ", status='" + status + '\'' +
                ", category='" + category + '\'' +
                ", isUncertain=" + isUncertain +
                ", txId='" + txId + '\'' +
                ", isForeign=" + isForeign +
                ", rawAmount='" + rawAmount + '\'' +
                ", isConflict=" + isConflict +
                ", conflictReason='" + conflictReason + '\'' +
                ", conflictWithId=" + conflictWithId +
                '}';
    }
}
