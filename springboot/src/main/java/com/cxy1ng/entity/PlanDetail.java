package com.cxy1ng.entity;

import java.io.Serializable;
import java.math.BigDecimal;

public class PlanDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private BigDecimal money;
    private BigDecimal sum;
    private String date;
    private Integer planId;

    public BigDecimal getSum() {
        return sum;
    }

    public void setSum(BigDecimal sum) {
        this.sum = sum;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

}
