package com.cxy1ng.service;

import cn.hutool.core.date.DateUtil;
import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.entity.Notebook;
import com.cxy1ng.exception.CustomException;
import com.cxy1ng.mapper.NotebookMapper;
import com.cxy1ng.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotebookService {

    @Resource
    private NotebookMapper notebookMapper;

    public void add(Notebook notebook) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.USER)) {
            notebook.setUserId(currentUser.getId());
            notebook.setDate(DateUtil.today());
            notebookMapper.insert(notebook);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public void deleteById(Integer id) {
        Notebook nb = notebookMapper.selectById(id);
        if (nb != null) {
            UserService.cleanupHtmlImages(nb.getContent());
        }
        notebookMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            notebookMapper.deleteById(id);
        }
    }

    public void updateById(Notebook notebook) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.USER)) {
            notebookMapper.updateById(notebook);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public Notebook selectById(Integer id) {
        return notebookMapper.selectById(id);
    }

    public List<Notebook> selectAll(Notebook notebook) {
        return notebookMapper.selectAll(notebook);
    }

    public PageInfo<Notebook> selectPage(Notebook notebook, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.USER)) {
            notebook.setUserId(currentUser.getId());
        }
        PageHelper.startPage(pageNum, pageSize);
        List<Notebook> list = notebookMapper.selectAll(notebook);
        return PageInfo.of(list);
    }

}
