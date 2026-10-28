package com.cxy1ng.mapper;

import com.cxy1ng.entity.User;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper {

    List<User> selectAll(User user);

    User selectById(Integer id);

    int insert(User user);

    int update(User user);

    int deleteById(Integer id);

    @Select("select * from user where username = #{username}")
    User selectByUsername(String username);
}
