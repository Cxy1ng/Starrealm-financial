package com.cxy1ng.mapper;

import com.cxy1ng.entity.Bill;
import java.util.List;

public interface BillMapper {

    int insert(Bill bill);

    int deleteById(Integer id);

    int update(Bill bill);

    Bill selectById(Integer id);

    List<Bill> selectAll(Bill bill);

    List<String> selectCategoryByType(String type);
}
