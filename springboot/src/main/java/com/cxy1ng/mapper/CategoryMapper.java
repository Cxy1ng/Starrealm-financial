package com.cxy1ng.mapper;

import com.cxy1ng.entity.Category;
import java.util.List;

public interface CategoryMapper {

    int insert(Category category);

    int deleteById(Integer id);

    int update(Category category);

    Category selectById(Integer id);

    List<Category> selectAll(Category category);

}
