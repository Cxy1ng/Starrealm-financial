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

    @Resource
    private SiteConfigService siteConfigService;

    private void fillAdminBanStatus(Admin a) {
        if (a == null || a.getId() == null) return;
        String val = siteConfigService.getPlainValue("admin_ban_" + a.getId(), "0");
        if (cn.hutool.core.util.StrUtil.isNotBlank(val) && "1".equals(val.trim())) {
            a.setBanned(1);
        } else {
            a.setBanned(0);
        }
        String needPwdVal = siteConfigService.getPlainValue("admin_need_change_pwd_" + a.getId(), "0");
        if (cn.hutool.core.util.StrUtil.isNotBlank(needPwdVal) && "1".equals(needPwdVal.trim())) {
            a.setNeedChangePwd(1);
        } else {
            a.setNeedChangePwd(0);
        }
    }

    public List<Admin> selectAll(Admin admin) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            List<Admin> list = adminMapper.selectAll(admin);
            if (list != null) {
                list.forEach(this::fillAdminBanStatus);
            }
            return list;
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public Admin selectById(Integer id) {
        Admin a = adminMapper.selectById(id);
        fillAdminBanStatus(a);
        return a;
    }

    public PageInfo<Admin> selectPage(Admin admin, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            PageHelper.startPage(pageNum, pageSize);
            List<Admin> list = adminMapper.selectAll(admin);
            if (list != null) {
                list.forEach(this::fillAdminBanStatus);
            }
            return PageInfo.of(list);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public String insert(Admin admin) {
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
            // 自动生成符合强密码规则的随机临时密码（8-20位，含大写、小写、数字、特殊符号）
            String tempPwd = com.cxy1ng.utils.PasswordValidator.generateStrongTempPassword();
            admin.setPwd(tempPwd);
            if (admin.getPerm() == null) {
                admin.setPerm(PermEnum.ADMIN.getMinPerm());
            }
            adminMapper.insert(admin);
            Admin created = adminMapper.selectByUsername(admin.getUsername());
            if (created != null && created.getId() != null) {
                siteConfigService.putSystemValue("admin_need_change_pwd_" + created.getId(), "1", "管理员需首次登录重置密码", currentUser != null ? currentUser.getUsername() : "system");
            }
            return tempPwd;
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

    public void updateBanStatus(Admin req) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (!PermEnum.SUPER_ADMIN.equals(role)) {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
        if (req == null || req.getId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        Admin target = adminMapper.selectById(req.getId());
        if (target == null) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (target.getPerm() != null && target.getPerm() >= 15) {
            throw new CustomException(400, "不可封禁超级管理员账号");
        }
        String key = "admin_ban_" + req.getId();
        int banned = (req.getBanned() != null && req.getBanned() == 1) ? 1 : 0;
        if (banned == 1) {
            siteConfigService.putSystemValue(key, "1", "管理员账号封禁状态", currentUser != null ? currentUser.getUsername() : "system");
        } else {
            siteConfigService.putSystemValue(key, "0", "管理员账号封禁状态", currentUser != null ? currentUser.getUsername() : "system");
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
        fillAdminBanStatus(dbAdmin);
        if (Integer.valueOf(1).equals(dbAdmin.getBanned())) {
            throw new CustomException(401, "您的管理员账号已被超级管理员封禁，无法登录系统后台");
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
        if (!com.cxy1ng.utils.PasswordValidator.isValid(account.getNewPwd())) {
            throw new CustomException(400, "新密码必须为8-20位，且同时包含大写字母、小写字母、数字和至少一个特殊符号");
        }
        dbAdmin.setPwd(account.getNewPwd());
        adminMapper.update(dbAdmin);
        // 清除临时密码重置标记
        siteConfigService.putSystemValue("admin_need_change_pwd_" + dbAdmin.getId(), "0", "管理员初始临时密码已重置", account.getUsername() != null ? account.getUsername() : "system");
    }
}
