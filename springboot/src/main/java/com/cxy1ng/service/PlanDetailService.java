package com.cxy1ng.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.entity.Plan;
import com.cxy1ng.entity.PlanDetail;
import com.cxy1ng.exception.CustomException;
import com.cxy1ng.mapper.PlanDetailMapper;
import com.cxy1ng.mapper.PlanMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class PlanDetailService {

    @Resource
    private PlanDetailMapper planDetailMapper;

    @Resource
    private PlanMapper planMapper;

    private void assertPlanNotExpired(Integer planId) {
        if (planId == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        Plan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        if (plan.getEnd() != null && !plan.getEnd().isBlank()) {
            Date todayBegin = DateUtil.beginOfDay(new Date());
            Date endDate = DateUtil.beginOfDay(DateUtil.parseDate(plan.getEnd()));
            if (endDate.before(todayBegin)) {
                throw new CustomException(ResultCodeEnum.PLAN_EXPIRED_ERROR);
            }
        }
    }

    public void add(PlanDetail planDetail) {
        assertPlanNotExpired(planDetail.getPlanId());
        BigDecimal sum = this.getFirstSum(planDetail.getPlanId());

        planDetail.setSum(sum.add(planDetail.getMoney()));
        planDetail.setDate(DateUtil.today());
        planDetailMapper.insert(planDetail);
    }

    public BigDecimal getFirstSum(Integer planId) {
        PlanDetail param = new PlanDetail();
        param.setPlanId(planId);
        List<PlanDetail> planDetailList = planDetailMapper.selectAll(param);
        BigDecimal sum = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(planDetailList)) {
            for (PlanDetail detail : planDetailList) {
                if (detail.getMoney() != null) {
                    sum = sum.add(detail.getMoney());
                }
            }
        }
        return sum;
    }

    private void recalculatePlanSums(Integer planId) {
        if (planId == null) return;
        PlanDetail param = new PlanDetail();
        param.setPlanId(planId);
        List<PlanDetail> list = planDetailMapper.selectAll(param);
        if (CollUtil.isEmpty(list)) return;
        // selectAll 默认按 id desc 排序，倒序从最早一笔向最新一笔重算累计 sum
        BigDecimal running = BigDecimal.ZERO;
        for (int i = list.size() - 1; i >= 0; i--) {
            PlanDetail d = list.get(i);
            if (d.getMoney() != null) {
                running = running.add(d.getMoney());
            }
            d.setSum(running);
            planDetailMapper.updateById(d);
        }
    }

    public void deleteById(Integer id) {
        PlanDetail detail = planDetailMapper.selectById(id);
        Integer planId = detail != null ? detail.getPlanId() : null;
        if (planId != null) {
            assertPlanNotExpired(planId);
        }
        planDetailMapper.deleteById(id);
        if (planId != null) {
            recalculatePlanSums(planId);
        }
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    public void updateById(PlanDetail planDetail) {
        planDetailMapper.updateById(planDetail);
    }

    public PlanDetail selectById(Integer id) {
        return planDetailMapper.selectById(id);
    }

    public List<PlanDetail> selectAll(PlanDetail planDetail) {
        return planDetailMapper.selectAll(planDetail);
    }

    public PageInfo<PlanDetail> selectPage(PlanDetail planDetail, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<PlanDetail> list = planDetailMapper.selectAll(planDetail);
        return  PageInfo.of(list);
    }

}
