package com.cxy1ng.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

public class Bill implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Date time;
    private String category;
    private String payee;
    private String detail;
    private String type;
    private BigDecimal amount;
    private String payment;
    private String status;
    private String comment;
    private Integer userId;
    private String userName;
    private String start;
    private String end;
    private Integer percent;
    private String ids;
    private String[] idsArr;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPayee() {
        return payee;
    }

    public void setPayee(String payee) {
        this.payee = payee;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public Integer getPercent() {
        return percent;
    }

    public void setPercent(Integer percent) {
        this.percent = percent;
    }

    public String getIds() {
        return ids;
    }

    public void setIds(String ids) {
        this.ids = ids;
    }

    public String[] getIdsArr() {
        return idsArr;
    }

    public void setIdsArr(String[] idsArr) {
        this.idsArr = idsArr;
    }

    @Override
    public String toString() {
        return "Bill{" +
                "id=" + id +
                ", time='" + time + '\'' +
                ", category='" + category + '\'' +
                ", payee='" + payee + '\'' +
                ", detail='" + detail + '\'' +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", payment='" + payment + '\'' +
                ", status='" + status + '\'' +
                ", comment='" + comment + '\'' +
                ", userId=" + userId +
                ", userName='" + userName + '\'' +
                ", start='" + start + '\'' +
                ", end='" + end + '\'' +
                ", percent=" + percent +
                ", ids='" + ids + '\'' +
                ", idsArr=" + Arrays.toString(idsArr) +
                '}';
    }
}
