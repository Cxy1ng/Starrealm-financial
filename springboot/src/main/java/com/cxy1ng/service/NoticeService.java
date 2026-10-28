package com.cxy1ng.service;

import cn.hutool.core.date.DateUtil;
import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.entity.Notice;
import com.cxy1ng.exception.CustomException;
import com.cxy1ng.mapper.NoticeMapper;
import com.cxy1ng.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeService {

    @Resource
    private NoticeMapper noticeMapper;

    public void insert(Notice notice) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            notice.setTime(DateUtil.now());
            notice.setUser(currentUser.getUsername());
            noticeMapper.insert(notice);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public void deleteById(Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            noticeMapper.deleteById(id);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public void deleteBatch(List<Integer> ids) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            for (Integer id : ids) {
                noticeMapper.deleteById(id);
            }
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public void update(Notice notice) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            noticeMapper.update(notice);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public Notice selectById(Integer id) {
        return noticeMapper.selectById(id);
    }

    public List<Notice> selectAll(Notice notice) {
        return noticeMapper.selectAll(notice);
    }

    public PageInfo<Notice> selectPage(Notice notice, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            PageHelper.startPage(pageNum, pageSize);
            List<Notice> list = noticeMapper.selectAll(notice);
            return PageInfo.of(list);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }
}
