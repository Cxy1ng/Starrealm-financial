package com.cxy1ng.mapper;

import com.cxy1ng.entity.Acfun;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AcfunMapper {

    int insert(Acfun acfun);

    int deleteById(Integer id);

    int update(Acfun acfun);

    Acfun selectById(Integer id);

    List<Acfun> selectAll(Acfun acfun);

    @Select("select * from acfun where user_id = #{userId}")
    Acfun selectByUserId(Integer userId);
}
