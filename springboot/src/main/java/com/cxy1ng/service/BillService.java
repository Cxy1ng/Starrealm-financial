package com.cxy1ng.service;

import cn.hutool.core.date.DateUtil;
import com.cxy1ng.common.enums.AcfunTypeEnum;
import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.entity.Acfun;
import com.cxy1ng.entity.Bill;
import com.cxy1ng.exception.CustomException;
import com.cxy1ng.mapper.BillMapper;
import com.cxy1ng.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class BillService {

    @Resource
    private BillMapper billMapper;
    @Resource
    AcfunService acfunService;

    /**
     * 重新计算并同步指定用户的账户收支汇总（确保新增、修改、删除账单后余额强一致）
     */
    public void syncUserAcfun(Integer userId) {
        if (userId == null) {
            return;
        }
        Bill query = new Bill();
        query.setUserId(userId);
        List<Bill> userBills = billMapper.selectAll(query);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalPay = BigDecimal.ZERO;
        for (Bill b : userBills) {
            if (b.getAmount() == null) continue;
            if (AcfunTypeEnum.PAY.getValue().equals(b.getType())) {
                totalPay = totalPay.add(b.getAmount());
            } else {
                totalIncome = totalIncome.add(b.getAmount());
            }
        }
        BigDecimal balance = totalIncome.subtract(totalPay);

        Acfun acfun = acfunService.selectByUserId(userId);
        if (acfun == null) {
            acfun = new Acfun();
            acfun.setUserId(userId);
            acfun.setIncome(totalIncome);
            acfun.setPay(totalPay);
            acfun.setBalance(balance);
            acfunService.insert(acfun);
        } else {
            acfun.setIncome(totalIncome);
            acfun.setPay(totalPay);
            acfun.setBalance(balance);
            acfunService.update(acfun);
        }
    }

    @Transactional
    public void insert(Bill bill) {
        if (bill.getTime() == null) {
            bill.setTime(DateUtil.date());
        }
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.USER)) {
            bill.setUserId(currentUser.getId());
        }
        billMapper.insert(bill);
        syncUserAcfun(bill.getUserId());
    }

    @Transactional
    public void deleteById(Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        Bill existing = billMapper.selectById(id);
        if (existing == null) {
            return;
        }
        if (role.equals(PermEnum.USER) && !Objects.equals(existing.getUserId(), currentUser.getId())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
        billMapper.deleteById(id);
        syncUserAcfun(existing.getUserId());
    }

    @Transactional
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            this.deleteById(id);
        }
    }

    @Transactional
    public void update(Bill bill) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        Bill existing = billMapper.selectById(bill.getId());
        if (existing == null) {
            return;
        }
        if (role.equals(PermEnum.USER) && !Objects.equals(existing.getUserId(), currentUser.getId())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
        billMapper.update(bill);
        syncUserAcfun(existing.getUserId());
    }

    public Bill selectById(Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        Bill bill = billMapper.selectById(id);
        if (role.equals(PermEnum.USER) && bill != null && !Objects.equals(bill.getUserId(), currentUser.getId())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
        return bill;
    }

    public List<Bill> selectAll(Bill bill) {
        return billMapper.selectAll(bill);
    }

    public PageInfo<Bill> selectPage(Bill bill, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.USER)) {
            bill.setUserId(currentUser.getId());
        }
        PageHelper.startPage(pageNum, pageSize);
        List<Bill> list = billMapper.selectAll(bill);
        return PageInfo.of(list);
    }

    public List<Bill> count(String type) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.USER)) {
            Bill bill = new Bill();
            bill.setUserId(currentUser.getId());
            bill.setType(type);
            List<Bill> billList = billMapper.selectAll(bill);
            BigDecimal sum = billList.stream()
                    .map(Bill::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO);
            List<String> categoryList = billList.stream()
                    .map(Bill::getCategory)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            List<Bill> list = new ArrayList<>();
            for (String category : categoryList) {
                Bill b = new Bill();
                b.setCategory(category);
                BigDecimal categorySum = billList.stream()
                        .filter(bi -> category.equals(bi.getCategory()) && bi.getAmount() != null)
                        .map(Bill::getAmount)
                        .reduce(BigDecimal::add)
                        .orElse(BigDecimal.ZERO);
                b.setAmount(categorySum);
                if (sum.compareTo(BigDecimal.ZERO) > 0) {
                    b.setPercent(categorySum.divide(sum, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).intValue());
                } else {
                    b.setPercent(0);
                }
                list.add(b);
            }
            return list;
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }
}
