package com.cxy1ng.mapper;

import com.cxy1ng.entity.PlanDetail;

import java.util.List;

public interface PlanDetailMapper {

    int insert(PlanDetail planDetail);

    int deleteById(Integer id);

    int updateById(PlanDetail planDetail);

    PlanDetail selectById(Integer id);

    List<PlanDetail> selectAll(PlanDetail planDetail);

}
