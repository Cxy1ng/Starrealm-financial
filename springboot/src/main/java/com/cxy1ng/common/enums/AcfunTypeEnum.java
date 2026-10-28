package com.cxy1ng.common.enums;

public enum AcfunTypeEnum {
    PAY("支出"),
    INCOME("收入");

    private String value;

    public String getValue() {
        return value;
    }

    AcfunTypeEnum(String value) {
        this.value = value;
    }
}
