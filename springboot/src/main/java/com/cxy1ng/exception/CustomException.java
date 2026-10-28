package com.cxy1ng.exception;

import com.cxy1ng.common.enums.ResultCodeEnum;

public class CustomException extends RuntimeException {
    private int code;
    private String msg;

    public CustomException(ResultCodeEnum resultCodeEnum) {
        this.code = resultCodeEnum.code;
        this.msg = resultCodeEnum.msg;
    }

    public CustomException(ResultCodeEnum resultCodeEnum, String appendMsg) {
        super(resultCodeEnum.msg + "：" + appendMsg);
        this.code = resultCodeEnum.code;
    }

    public CustomException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
