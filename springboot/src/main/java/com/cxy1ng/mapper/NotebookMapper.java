package com.cxy1ng.mapper;

import com.cxy1ng.entity.Notebook;

import java.util.List;

public interface NotebookMapper {

    int insert(Notebook notebook);

    int deleteById(Integer id);

    int updateById(Notebook notebook);

    Notebook selectById(Integer id);

    List<Notebook> selectAll(Notebook notebook);

}
