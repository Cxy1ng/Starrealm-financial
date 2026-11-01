package com.cxy1ng.entity;

public class Account {
    private Integer id;
    private String username;
    private String pwd;
    private String newPwd;
    private Integer perm;
    private String avatar;
    private String token;
    private String role;
    private String apiKey;
    private Boolean hasApiKey;
    private Integer needChangePwd;

    public Integer getNeedChangePwd() {
        return needChangePwd;
    }

    public void setNeedChangePwd(Integer needChangePwd) {
        this.needChangePwd = needChangePwd;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Boolean getHasApiKey() {
        return hasApiKey;
    }

    public void setHasApiKey(Boolean hasApiKey) {
        this.hasApiKey = hasApiKey;
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

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public Integer getPerm() {
        return perm;
    }

    public void setPerm(Integer perm) {
        this.perm = perm;
    }

    public String getNewPwd() {
        return newPwd;
    }

    public void setNewPwd(String newPwd) {
        this.newPwd = newPwd;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
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
