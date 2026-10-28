package com.cxy1ng.mapper;

import com.cxy1ng.entity.Plan;

import java.util.List;

public interface PlanMapper {

    int insert(Plan plan);

    int deleteById(Integer id);

    int updateById(Plan plan);

    Plan selectById(Integer id);

    List<Plan> selectAll(Plan plan);

}
