package com.cxy1ng.mapper;

import com.cxy1ng.entity.Notice;

import java.util.List;

public interface NoticeMapper {

    int insert(Notice notice);

    int deleteById(Integer id);

    int update(Notice notice);

    Notice selectById(Integer id);

    List<Notice> selectAll(Notice notice);
}
