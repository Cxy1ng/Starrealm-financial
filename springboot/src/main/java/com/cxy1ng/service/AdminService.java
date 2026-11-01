package com.cxy1ng.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.cxy1ng.common.Constants;
import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.entity.Admin;
import com.cxy1ng.exception.CustomException;
import com.cxy1ng.mapper.AdminMapper;
import com.cxy1ng.utils.ApiKeyCryptoUtil;
import com.cxy1ng.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    @Resource
    private AdminMapper adminMapper;

    public List<Admin> selectAll(Admin admin) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            return adminMapper.selectAll(admin);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public Admin selectById(Integer id) {
        return adminMapper.selectById(id);
    }

    public PageInfo<Admin> selectPage(Admin admin, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            PageHelper.startPage(pageNum, pageSize);
            List<Admin> list = adminMapper.selectAll(admin);
            return PageInfo.of(list);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public void insert(Admin admin) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.SUPER_ADMIN)) {
            Admin dbAdmin = adminMapper.selectByUsername(admin.getUsername());
            if (ObjectUtil.isNotNull(dbAdmin)) {
                throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
            }
            if (ObjectUtil.isNotNull(admin.getUsername())) {
                admin.setUsername(admin.getUsername());
            }
            if (StrUtil.isBlank(admin.getNickname())) {
                admin.setNickname(admin.getUsername());
            }
            if (ObjectUtil.isEmpty(admin.getPwd())) {
                admin.setPwd(Constants.ADMIN_DEFAULT_PASSWORD);
            }
            admin.setPerm(PermEnum.ADMIN.getMinPerm());
            adminMapper.insert(admin);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }

    }

    public void update(Admin admin) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum currentRole = PermEnum.getRoleByPerm(currentUser.getPerm());
        
        // 如果当前操作者是普通管理员(ADMIN，perm 10-14)，且目标更新的是超级管理员(SUPER_ADMIN，perm 15)或另一个管理员但权限高于等于自己，则拒绝
        if (currentRole.equals(PermEnum.ADMIN)) {
            if (admin.getPerm() != null && admin.getPerm() >= 15) {
                throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
            }
            Admin targetDb = adminMapper.selectById(admin.getId());
            if (targetDb != null && targetDb.getPerm() >= 15) {
                throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
            }
        }

        if (currentUser.getPerm() >= admin.getPerm() || (currentRole.equals(PermEnum.ADMIN) && admin.getPerm() < 15)) {
            if ("__CLEAR__".equals(admin.getApiKey())) {
                admin.setApiKey("");
            } else if (StrUtil.isNotBlank(admin.getApiKey())) {
                if (admin.getApiKey().contains("****")) {
                    admin.setApiKey(null);
                } else {
                    admin.setApiKey(ApiKeyCryptoUtil.encrypt(admin.getApiKey()));
                }
            } else {
                admin.setApiKey(null);
            }
            adminMapper.update(admin);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }

    }

    public void deleteById(Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.SUPER_ADMIN)) {
            adminMapper.deleteById(id);
        } else if (role.equals(PermEnum.ADMIN)) {
            Admin target = adminMapper.selectById(id);
            if (target != null && target.getPerm() >= 15) {
                throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
            }
            adminMapper.deleteById(id);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public void deleteBatch(List<Integer> ids) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.SUPER_ADMIN)) {
            for (Integer id : ids) {
                this.deleteById(id);
            }
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }

    }

    public Admin login(Account account) {
        Admin dbAdmin = adminMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbAdmin)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!account.getPwd().equals(dbAdmin.getPwd())) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        if (StrUtil.isBlank(dbAdmin.getRole())) {
            dbAdmin.setRole(String.valueOf(PermEnum.getRoleByPerm(dbAdmin.getPerm())));
        }
        String tokenData = dbAdmin.getId() + "-" + dbAdmin.getPerm() + "-" + dbAdmin.getRole();
        String token = TokenUtils.createToken(tokenData, dbAdmin.getPwd());
        dbAdmin.setToken(token);
        dbAdmin.setHasApiKey(StrUtil.isNotBlank(dbAdmin.getApiKey()));
        dbAdmin.setApiKey(ApiKeyCryptoUtil.mask(dbAdmin.getApiKey()));
        return dbAdmin;
    }

    public void updatePwd(Account account) {
        Admin dbAdmin = adminMapper.selectByUsername(account.getUsername());
        if(ObjectUtil.isNull(dbAdmin)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!account.getPwd().equals(dbAdmin.getPwd())) {
            throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        dbAdmin.setPwd(account.getNewPwd());
        adminMapper.update(dbAdmin);
    }
}
