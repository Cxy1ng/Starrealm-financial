package com.cxy1ng.entity;

import java.io.Serializable;

public class Admin extends Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String username;
    private String nickname;
    private String pwd;
    private String avatar;
    private String email;
    private String phone;
    private Integer perm;
    private String role;
    private String apiKey;
    private Integer banned; // 0-正常 1-封禁
    private Integer needChangePwd; // 1-临时密码需重置
    private String tempPassword; // 仅在超级管理员创建成功时回传展示一次

    public Integer getNeedChangePwd() {
        return needChangePwd;
    }

    public void setNeedChangePwd(Integer needChangePwd) {
        this.needChangePwd = needChangePwd;
    }

    public String getTempPassword() {
        return tempPassword;
    }

    public void setTempPassword(String tempPassword) {
        this.tempPassword = tempPassword;
    }

    public Integer getBanned() {
        return banned;
    }

    public void setBanned(Integer banned) {
        this.banned = banned;
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }

    @Override
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getPerm() {
        return perm;
    }

    public void setPerm(Integer perm) {
        this.perm = perm;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
