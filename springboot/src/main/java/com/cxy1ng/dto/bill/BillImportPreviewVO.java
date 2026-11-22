package com.cxy1ng.dto.bill;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 账单导入预览结果 VO（与 BillImportPrecheckVO 保持完全兼容）
 */
public class BillImportPreviewVO extends BillImportPrecheckVO implements Serializable {
    private static final long serialVersionUID = 1L;

    public BillImportPreviewVO() {
        super();
    }

    public BillImportPreviewVO(Integer totalCount, Integer autoCategorizedCount, Integer uncertainCount,
                               Integer conflictCount, BigDecimal totalAmount, List<BillImportItemDTO> items) {
        super(totalCount, autoCategorizedCount, uncertainCount, conflictCount, totalAmount, items);
    }
}
