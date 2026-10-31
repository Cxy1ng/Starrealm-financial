package com.cxy1ng.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.cxy1ng.common.Constants;
import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.entity.User;
import com.cxy1ng.exception.CustomException;
import com.cxy1ng.mapper.UserMapper;
import com.cxy1ng.utils.ApiKeyCryptoUtil;
import com.cxy1ng.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.apache.poi.util.StringUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private SiteConfigService siteConfigService;

    private void populateBanInfo(User user) {
        if (user == null || user.getId() == null) return;
        String raw = siteConfigService.getPlainValue("user_ban_" + user.getId(), "");
        if (StrUtil.isNotBlank(raw)) {
            cn.hutool.json.JSONObject obj = cn.hutool.json.JSONUtil.parseObj(raw);
            user.setBanned(obj.getInt("banned", 0));
            user.setBanReason(obj.getStr("banReason", ""));
            user.setBanTime(obj.getStr("banTime", ""));
        } else {
            user.setBanned(0);
        }
    }

    public void updateBanStatus(User req) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (!PermEnum.ADMIN.equals(role) && !PermEnum.SUPER_ADMIN.equals(role)) {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
        if (req == null || req.getId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        int banned = (req.getBanned() != null && req.getBanned() == 1) ? 1 : 0;
        String key = "user_ban_" + req.getId();
        java.util.Map<String, String> payload = new java.util.HashMap<>();
        if (banned == 1) {
            cn.hutool.json.JSONObject obj = new cn.hutool.json.JSONObject();
            obj.set("banned", 1);
            obj.set("banReason", StrUtil.isNotBlank(req.getBanReason()) ? req.getBanReason().trim() : "违反《星界用户服务协议》第 4.2 条（AI 提示词注入或越狱攻击）");
            obj.set("banTime", DateUtil.now());
            payload.put(key, obj.toString());
        } else {
            payload.put(key, "");
        }
        siteConfigService.saveBatch(payload);
    }

    public List<User> selectAll(User user) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            List<User> list = userMapper.selectAll(user);
            list.forEach(this::populateBanInfo);
            return list;
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public User selectById(Integer id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setHasApiKey(StrUtil.isNotBlank(user.getApiKey()));
            populateBanInfo(user);
        }
        return user;
    }

    public PageInfo<User> selectPage(User user, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            PageHelper.startPage(pageNum, pageSize);
            List<User> list = userMapper.selectAll(user);
            list.forEach(this::populateBanInfo);
            return PageInfo.of(list);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public void insert(User user) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            User dbUser = userMapper.selectByUsername(user.getUsername());
            if (ObjectUtil.isNotNull(dbUser)) {
                throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
            }
            if (ObjectUtil.isEmpty(user.getUsername())) {
                user.setUsername(user.getUsername());
            }
            if (ObjectUtil.isEmpty(user.getNickname())) {
                user.setNickname(user.getUsername());
            }
            if (ObjectUtil.isEmpty(user.getPwd())) {
                user.setPwd(Constants.USER_DEFAULT_PASSWORD);
            }
            user.setPerm(PermEnum.USER.getMinPerm());
            user.setRole(String.valueOf(PermEnum.getRoleByPerm(user.getPerm())));
            userMapper.insert(user);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public void update(User user) {
        if ("__CLEAR__".equals(user.getApiKey())) {
            user.setApiKey("");
        } else if (StrUtil.isNotBlank(user.getApiKey())) {
            // 如果包含脱敏星号则不覆盖原数据库值
            if (user.getApiKey().contains("****")) {
                user.setApiKey(null);
            } else {
                user.setApiKey(ApiKeyCryptoUtil.encrypt(user.getApiKey()));
            }
        } else {
            user.setApiKey(null);
        }
        userMapper.update(user);
    }

    public void deleteById(Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            userMapper.deleteById(id);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public void deleteBatch(List<Integer> ids) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            for (Integer id : ids) {
                this.deleteById(id);
            }
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public Account login(Account account) {
        User dbUser = userMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbUser)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!account.getPwd().equals(dbUser.getPwd())) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        populateBanInfo(dbUser);
        if (Integer.valueOf(1).equals(dbUser.getBanned())) {
            String reason = StrUtil.isNotBlank(dbUser.getBanReason()) ? dbUser.getBanReason() : "违反《星界用户服务协议》相关条款";
            throw new CustomException(ResultCodeEnum.USER_BANNED_ERROR.code, "账号已被封禁，原因：" + reason + "。如有疑问请联系系统管理员。");
        }
        if (StrUtil.isBlank(dbUser.getRole())) {
            dbUser.setRole(String.valueOf(PermEnum.getRoleByPerm(dbUser.getPerm())));
        }
        String tokenData = dbUser.getId() + "-" + dbUser.getPerm() + "-" + dbUser.getRole();
        String token = TokenUtils.createToken(tokenData, dbUser.getPwd());
        dbUser.setToken(token);
        dbUser.setHasApiKey(StrUtil.isNotBlank(dbUser.getApiKey()));
        dbUser.setApiKey(ApiKeyCryptoUtil.mask(dbUser.getApiKey()));
        return dbUser;
    }

    public void register(Account account) {
        User dbUser = userMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNotNull(dbUser)) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        User user = new User();
        user.setUsername(account.getUsername());
        user.setNickname(StrUtil.isNotBlank(account.getUsername()) ? account.getUsername() : "新用户");
        user.setPwd(account.getPwd());
        user.setPerm(PermEnum.USER.getMinPerm());
        user.setRole(String.valueOf(PermEnum.USER));
        userMapper.insert(user);
    }

    public void updatePwd(Account account) {
        User dbUser = this.selectById(account.getId());
        if (ObjectUtil.isNull(dbUser)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!account.getPwd().equals(dbUser.getPwd())) {
            throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        dbUser.setPwd(account.getNewPwd());
        this.update(dbUser);
    }
}
