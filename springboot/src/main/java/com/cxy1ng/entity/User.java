package com.cxy1ng.entity;

import java.io.Serializable;
import java.sql.Date;

public class User extends Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String username;
    private String nickname;
    private String sex;
    private String pwd;
    private String avatar;
    private String phone;
    private String email;
    private Date birth;
    private String descriptiontext;
    private Integer perm;
    private String token;
    private String role;
    private String apiKey;
    private Integer banned;
    private String banReason;
    private String banTime;
    private Integer accountBanned;
    private String accountBanReason;
    private String accountBanTime;
    private Integer aiBanned;
    private Integer aiBanLevel;
    private Integer aiBanCount;
    private String aiBanReason;
    private String aiBanTime;
    private String aiBanDuration;
    private String aiBanExpireTime;
    private Integer violationCount24h;
    private String violationLogs;
    private Integer needChangePwd; // 1-初始默认密码需重置

    public Integer getNeedChangePwd() {
        return needChangePwd;
    }

    public void setNeedChangePwd(Integer needChangePwd) {
        this.needChangePwd = needChangePwd;
    }

    public Integer getAccountBanned() {
        return accountBanned;
    }

    public void setAccountBanned(Integer accountBanned) {
        this.accountBanned = accountBanned;
    }

    public String getAccountBanReason() {
        return accountBanReason;
    }

    public void setAccountBanReason(String accountBanReason) {
        this.accountBanReason = accountBanReason;
    }

    public String getAccountBanTime() {
        return accountBanTime;
    }

    public void setAccountBanTime(String accountBanTime) {
        this.accountBanTime = accountBanTime;
    }

    public Integer getAiBanned() {
        return aiBanned;
    }

    public void setAiBanned(Integer aiBanned) {
        this.aiBanned = aiBanned;
    }

    public Integer getAiBanLevel() {
        return aiBanLevel;
    }

    public void setAiBanLevel(Integer aiBanLevel) {
        this.aiBanLevel = aiBanLevel;
    }

    public Integer getAiBanCount() {
        return aiBanCount;
    }

    public void setAiBanCount(Integer aiBanCount) {
        this.aiBanCount = aiBanCount;
    }

    public String getAiBanReason() {
        return aiBanReason;
    }

    public void setAiBanReason(String aiBanReason) {
        this.aiBanReason = aiBanReason;
    }

    public String getAiBanTime() {
        return aiBanTime;
    }

    public void setAiBanTime(String aiBanTime) {
        this.aiBanTime = aiBanTime;
    }

    public String getAiBanDuration() {
        return aiBanDuration;
    }

    public void setAiBanDuration(String aiBanDuration) {
        this.aiBanDuration = aiBanDuration;
    }

    public String getAiBanExpireTime() {
        return aiBanExpireTime;
    }

    public void setAiBanExpireTime(String aiBanExpireTime) {
        this.aiBanExpireTime = aiBanExpireTime;
    }

    public Integer getViolationCount24h() {
        return violationCount24h;
    }

    public void setViolationCount24h(Integer violationCount24h) {
        this.violationCount24h = violationCount24h;
    }

    public String getViolationLogs() {
        return violationLogs;
    }

    public void setViolationLogs(String violationLogs) {
        this.violationLogs = violationLogs;
    }

    public Integer getBanned() {
        return banned;
    }

    public void setBanned(Integer banned) {
        this.banned = banned;
    }

    public String getBanReason() {
        return banReason;
    }

    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }

    public String getBanTime() {
        return banTime;
    }

    public void setBanTime(String banTime) {
        this.banTime = banTime;
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

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getBirth() {
        return birth;
    }

    public void setBirth(Date birth) {
        this.birth = birth;
    }

    public String getDescriptiontext() {
        return descriptiontext;
    }

    public void setDescriptiontext(String descriptiontext) {
        this.descriptiontext = descriptiontext;
    }

    public Integer getPerm() {
        return perm;
    }

    public void setPerm(Integer perm) {
        this.perm = perm;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
