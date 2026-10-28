package com.cxy1ng.service;

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

    public List<User> selectAll(User user) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            return userMapper.selectAll(user);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public User selectById(Integer id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setHasApiKey(StrUtil.isNotBlank(user.getApiKey()));
        }
        return user;
    }

    public PageInfo<User> selectPage(User user, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            PageHelper.startPage(pageNum, pageSize);
            List<User> list = userMapper.selectAll(user);
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
        Account dbUser = userMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbUser)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!account.getPwd().equals(dbUser.getPwd())) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
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
