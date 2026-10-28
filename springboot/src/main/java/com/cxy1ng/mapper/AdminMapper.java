package com.cxy1ng.mapper;

import com.cxy1ng.entity.Admin;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AdminMapper {

    List<Admin> selectAll(Admin admin);

    Admin selectById(Integer id);

    int insert(Admin admin);

    int update(Admin admin);

    int deleteById(Integer id);

    @Select("select * from admin where username = #{username}")
    Admin selectByUsername(String Username);
}
