package com.cxy1ng.service;

import cn.hutool.core.date.DateUtil;
import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.common.enums.PlanStatusEnum;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.entity.Plan;
import com.cxy1ng.exception.CustomException;
import com.cxy1ng.mapper.PlanMapper;
import com.cxy1ng.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
public class PlanService {

    @Resource
    private PlanMapper planMapper;

    @Resource
    PlanDetailService planDetailService;

    public void add(Plan plan) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.USER)) {
            plan.setUserId(currentUser.getId());
        }
        Date start = DateUtil.parseDate(plan.getStart());
        Date todayBegin = DateUtil.beginOfDay(new Date());
        if (start.before(todayBegin)) {
            throw new CustomException(ResultCodeEnum.DATE_START_ERROR);
        }
        if (!start.before(DateUtil.parseDate(plan.getEnd()))) {
            throw new CustomException(ResultCodeEnum.DATE_ERROR);
        }
        plan.setDate(DateUtil.today());
        planMapper.insert(plan);
    }

    public void deleteById(Integer id) {
        planMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            planMapper.deleteById(id);
        }
    }

    public void updateById(Plan plan) {
        planMapper.updateById(plan);
    }

    public Plan selectById(Integer id) {
        Plan plan = planMapper.selectById(id);;
        this.setPlan(plan);
        return plan;
    }

    public List<Plan> selectAll(Plan plan) {
        return planMapper.selectAll(plan);
    }

    public PageInfo<Plan> selectPage(Plan plan, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Plan> list = planMapper.selectAll(plan);
        PageInfo<Plan> pageInfo = PageInfo.of(list);
        for (Plan p : pageInfo.getList()) {
            this.setPlan(p);
        }
        return pageInfo;
    }

    public boolean isPlanExpired(Plan p) {
        if (p == null || p.getEnd() == null || p.getEnd().isBlank()) {
            return false;
        }
        Date todayBegin = DateUtil.beginOfDay(new Date());
        Date endDate = DateUtil.beginOfDay(DateUtil.parseDate(p.getEnd()));
        return endDate.before(todayBegin);
    }

    public void setPlan(Plan p) {
        if (p == null) {
            return;
        }
        BigDecimal sum = planDetailService.getFirstSum(p.getId());
        if (p.getMoney() != null && p.getMoney().compareTo(BigDecimal.ZERO) > 0) {
            int percent = sum.multiply(BigDecimal.valueOf(100))
                    .divide(p.getMoney(), 0, RoundingMode.HALF_UP)
                    .intValue();
            p.setPercent(Math.max(0, percent));
        } else {
            p.setPercent(0);
        }
        Date todayBegin = DateUtil.beginOfDay(new Date());
        Date startDate = DateUtil.beginOfDay(DateUtil.parseDate(p.getStart()));
        Date endDate = DateUtil.beginOfDay(DateUtil.parseDate(p.getEnd()));

        // 已经结束过期的计划状态始终为「已结束」，停止存钱；未过期且金额达标时为「已完成」（仍允许继续多存）
        if (endDate.before(todayBegin)) {
            p.setStatus(PlanStatusEnum.END.getValue());
        } else if (sum.compareTo(p.getMoney()) >= 0) {
            p.setStatus(PlanStatusEnum.DONE.getValue());
        } else if (startDate.after(todayBegin)) {
            p.setStatus(PlanStatusEnum.NO_READY.getValue());
        } else {
            p.setStatus(PlanStatusEnum.IN_PROGRESS.getValue());
        }
    }
}
