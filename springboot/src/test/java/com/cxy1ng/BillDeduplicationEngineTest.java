package com.cxy1ng;

import com.cxy1ng.dto.bill.BillImportConfirmDTO;
import com.cxy1ng.dto.bill.BillImportItemDTO;
import com.cxy1ng.dto.bill.BillImportPrecheckVO;
import com.cxy1ng.entity.Bill;
import com.cxy1ng.service.BillDeduplicationEngine;
import com.cxy1ng.service.BillImportService;
import com.cxy1ng.service.BillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("跨渠道及同渠道多维滑动窗口排重引擎实测")
public class BillDeduplicationEngineTest {

    private BillDeduplicationEngine deduplicationEngine;

    @BeforeEach
    void setUp() {
        deduplicationEngine = new BillDeduplicationEngine();
        deduplicationEngine.setSlidingWindowSeconds(120);
    }

    @Test
    @DisplayName("测试同渠道批次内相同 txId 幂等去重")
    void testBatchSelfDeduplication() {
        Date now = new Date();
        BillImportItemDTO item1 = new BillImportItemDTO();
        item1.setTxId("TX10001");
        item1.setAmount(new BigDecimal("50.00"));
        item1.setType("支出");
        item1.setTime(now);
        item1.setPayee("喜茶");

        BillImportItemDTO item2 = new BillImportItemDTO();
        item2.setTxId("TX10001"); // 重复单号
        item2.setAmount(new BigDecimal("50.00"));
        item2.setType("支出");
        item2.setTime(now);
        item2.setPayee("喜茶");

        List<BillImportItemDTO> items = Arrays.asList(item1, item2);
        deduplicationEngine.detectDuplicates(items, Collections.emptyList(), "wechat");

        assertFalse(item1.getIsConflict());
        assertTrue(item2.getIsConflict());
        assertEquals("同批次包含重复官方交易单号", item2.getConflictReason());

        BillImportPrecheckVO precheck = BillImportPrecheckVO.of(items, "wechat", "batch.xlsx");
        assertEquals(2, precheck.getTotalCount());
        assertEquals(1, precheck.getConflictCount());
    }

    @Test
    @DisplayName("测试与历史账单同渠道官方单号重复比对")
    void testHistoryTxIdDeduplication() {
        Bill hist = new Bill();
        hist.setId(999);
        hist.setComment("单号:WX202605018888");
        hist.setAmount(new BigDecimal("120.00"));
        hist.setType("支出");
        hist.setTime(new Date());

        BillImportItemDTO item = new BillImportItemDTO();
        item.setTxId("WX202605018888");
        item.setAmount(new BigDecimal("120.00"));
        item.setType("支出");
        item.setTime(new Date());

        List<BillImportItemDTO> items = Collections.singletonList(item);
        deduplicationEngine.detectDuplicates(items, Collections.singletonList(hist), "wechat");

        assertTrue(item.getIsConflict());
        assertEquals("同渠道官方单号重复", item.getConflictReason());
        assertEquals(999, item.getConflictWithId());
    }

    @Test
    @DisplayName("测试跨渠道绑卡扣款 120 秒滑动窗口与卡号尾号指纹交叉比对")
    void testCrossChannelSlidingWindowDeduplication() {
        Date baseTime = new Date(1714560000000L); // 2026-05-01 12:00:00

        // 历史已入账的微信支付流水（使用招商银行 8899 卡支付）
        Bill wechatHist = new Bill();
        wechatHist.setId(777);
        wechatHist.setTime(baseTime);
        wechatHist.setAmount(new BigDecimal("35.00"));
        wechatHist.setType("支出");
        wechatHist.setPayee("美团外卖");
        wechatHist.setPayment("招商银行储蓄卡(8899)");

        // 待导入的银行流水（发生于 45 秒后，在 120 秒时间窗口内）
        Date bankTime = new Date(baseTime.getTime() + 45 * 1000L);
        BillImportItemDTO bankItem = new BillImportItemDTO();
        bankItem.setTime(bankTime);
        bankItem.setAmount(new BigDecimal("35.00"));
        bankItem.setType("支出");
        bankItem.setPayee("招商银行");
        bankItem.setDetail("财付通-美团外卖快捷支付");
        bankItem.setPayment("银行卡(8899)");

        List<BillImportItemDTO> items = Collections.singletonList(bankItem);
        deduplicationEngine.detectDuplicates(items, Collections.singletonList(wechatHist), "bank");

        assertTrue(bankItem.getIsConflict());
        assertEquals("识别到跨渠道绑卡重复流水", bankItem.getConflictReason());
        assertEquals(777, bankItem.getConflictWithId());
    }

    @Test
    @DisplayName("测试跨渠道时间窗口超限（超过 120 秒）不判定为冲突")
    void testCrossChannelSlidingWindowExceeded() {
        Date baseTime = new Date(1714560000000L);

        Bill wechatHist = new Bill();
        wechatHist.setId(777);
        wechatHist.setTime(baseTime);
        wechatHist.setAmount(new BigDecimal("35.00"));
        wechatHist.setType("支出");
        wechatHist.setPayment("招商银行储蓄卡(8899)");

        // 时间间隔 300 秒（超出了 120 秒窗口）
        Date bankTime = new Date(baseTime.getTime() + 300 * 1000L);
        BillImportItemDTO bankItem = new BillImportItemDTO();
        bankItem.setTime(bankTime);
        bankItem.setAmount(new BigDecimal("35.00"));
        bankItem.setType("支出");
        bankItem.setPayment("银行卡(8899)");

        List<BillImportItemDTO> items = Collections.singletonList(bankItem);
        deduplicationEngine.detectDuplicates(items, Collections.singletonList(wechatHist), "bank");

        assertFalse(bankItem.getIsConflict());
        assertNull(bankItem.getConflictReason());
    }

    @Test
    @DisplayName("测试 PayPal 与信用卡跨境摘要特征排重")
    void testPayPalCrossBorderDeduplication() {
        Date now = new Date();

        Bill cardHist = new Bill();
        cardHist.setId(666);
        cardHist.setTime(now);
        cardHist.setAmount(new BigDecimal("49.99"));
        cardHist.setType("支出");
        cardHist.setDetail("PAYPAL *STEAM GAMES 消费");
        cardHist.setPayment("招行全币种信用卡");

        BillImportItemDTO ppItem = new BillImportItemDTO();
        ppItem.setTime(now);
        ppItem.setAmount(new BigDecimal("49.99"));
        ppItem.setType("支出");
        ppItem.setPayee("Steam Games");
        ppItem.setPayment("PayPal");

        List<BillImportItemDTO> items = Collections.singletonList(ppItem);
        deduplicationEngine.detectDuplicates(items, Collections.singletonList(cardHist), "paypal");

        assertTrue(ppItem.getIsConflict());
        assertEquals("识别到跨渠道绑卡重复流水", ppItem.getConflictReason());
        assertEquals(666, ppItem.getConflictWithId());
    }

    @Test
    @DisplayName("测试 BillImportService confirm 自动排重跳过与全部保留策略")
    void testImportServiceConfirmDuplicateActions() {
        BillService mockBillService = Mockito.mock(BillService.class);
        BillImportService importService = new BillImportService();
        importService.setBillService(mockBillService);

        BillImportItemDTO normalItem = new BillImportItemDTO();
        normalItem.setPayee("麦当劳");
        normalItem.setAmount(new BigDecimal("25.00"));
        normalItem.setType("支出");
        normalItem.setIsConflict(false);

        BillImportItemDTO duplicateItem = new BillImportItemDTO();
        duplicateItem.setPayee("麦当劳");
        duplicateItem.setAmount(new BigDecimal("25.00"));
        duplicateItem.setType("支出");
        duplicateItem.setIsConflict(true);
        duplicateItem.setConflictReason("同渠道官方单号重复");

        List<BillImportItemDTO> items = Arrays.asList(normalItem, duplicateItem);

        // 1. 测试默认/auto_merge：跳过冲突项，仅入库 1 笔
        when(mockBillService.insertBatch(anyList(), eq(1))).thenReturn(1);
        BillImportConfirmDTO confirmDTO = new BillImportConfirmDTO(items, 1, "wechat", "auto_merge");
        int inserted = importService.confirm(confirmDTO);
        assertEquals(1, inserted);

        // 2. 测试 keep_all：全部保留入库 2 笔
        when(mockBillService.insertBatch(anyList(), eq(1))).thenReturn(2);
        confirmDTO.setDuplicateAction("keep_all");
        inserted = importService.confirm(confirmDTO);
        assertEquals(2, inserted);
    }
}
