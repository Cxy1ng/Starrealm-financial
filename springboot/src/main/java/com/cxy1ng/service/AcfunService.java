package com.cxy1ng.service;

import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.entity.Acfun;
import com.cxy1ng.exception.CustomException;
import com.cxy1ng.mapper.AcfunMapper;
import com.cxy1ng.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class AcfunService {

    @Resource
    private AcfunMapper acfunMapper;

    public void insert(Acfun acfun) {
        acfunMapper.insert(acfun);
    }

    public void deleteById(Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            acfunMapper.deleteById(id);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public void deleteBatch(List<Integer> ids) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            for (Integer id : ids) {
                acfunMapper.deleteById(id);
            }
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public void update(Acfun acfun) {
        acfunMapper.update(acfun);
    }

    public Acfun selectById(Integer id) {
        return acfunMapper.selectById(id);
    }

    public List<Acfun> selectAll(Acfun acfun) {
        return acfunMapper.selectAll(acfun);
    }

    public PageInfo<Acfun> selectPage(Acfun acfun, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Acfun> list = acfunMapper.selectAll(acfun);
        return PageInfo.of(list);
    }

    public Acfun selectByUserId(Integer userId) {
        return acfunMapper.selectByUserId(userId);
    }
}
