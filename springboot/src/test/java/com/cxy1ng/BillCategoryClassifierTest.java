package com.cxy1ng;

import com.cxy1ng.dto.bill.BillImportConfirmDTO;
import com.cxy1ng.dto.bill.BillImportItemDTO;
import com.cxy1ng.dto.bill.BillImportPrecheckVO;
import com.cxy1ng.service.BillCategoryClassifier;
import com.cxy1ng.service.BillCategoryClassifier.ClassificationResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 账单智能分类引擎与 DTO/VO 验证测试
 */
class BillCategoryClassifierTest {

    private static BillCategoryClassifier classifier;

    @BeforeAll
    static void setUp() {
        classifier = new BillCategoryClassifier();
    }

    @Test
    @DisplayName("1. 知名品牌商户精准识别 (餐饮美食/交通出行等，isUncertain = false)")
    void testKnownBrandsCategorization() {
        // 瑞幸咖啡 -> 餐饮美食
        ClassificationResult luckin = classifier.classify("瑞幸咖啡（科技园店）", "生椰拿铁 1杯", "支出");
        assertEquals("餐饮美食", luckin.getCategory());
        assertFalse(luckin.isUncertain(), "知名品牌瑞幸咖啡应准确识别，isUncertain应为false");

        // 喜茶 -> 餐饮美食
        ClassificationResult heytea = classifier.classify("喜茶HEYTEA", "多肉葡萄", "支出");
        assertEquals("餐饮美食", heytea.getCategory());
        assertFalse(heytea.isUncertain());

        // 美团外卖 -> 餐饮美食
        ClassificationResult meituan = classifier.classify("美团外卖", "美团订单-米线套餐", "支出");
        assertEquals("餐饮美食", meituan.getCategory());
        assertFalse(meituan.isUncertain());

        // 深圳地铁 -> 交通出行
        ClassificationResult szMetro = classifier.classify("深圳地铁", "地铁乘车码扣费", "支出");
        assertEquals("交通出行", szMetro.getCategory());
        assertFalse(szMetro.isUncertain());

        // 滴滴出行 -> 交通出行
        ClassificationResult didi = classifier.classify("滴滴出行", "快车行程单", "支出");
        assertEquals("交通出行", didi.getCategory());
        assertFalse(didi.isUncertain());

        // 国家电网 -> 生活缴费
        ClassificationResult grid = classifier.classify("国家电网公司", "居民生活用电电费", "支出");
        assertEquals("生活缴费", grid.getCategory());
        assertFalse(grid.isUncertain());

        // Steam -> 文化休闲
        ClassificationResult steam = classifier.classify("Steam Games", "Purchase transaction", "支出");
        assertEquals("文化休闲", steam.getCategory());
        assertFalse(steam.isUncertain());

        // Apple Store -> 数码科技
        ClassificationResult apple = classifier.classify("Apple Store", "iCloud+ 50GB 空间订阅", "支出");
        assertEquals("数码科技", apple.getCategory());
        assertFalse(apple.isUncertain());

        // 盒马鲜生 -> 日常购物
        ClassificationResult hema = classifier.classify("盒马鲜生超市", "生鲜日用品采购", "支出");
        assertEquals("日常购物", hema.getCategory());
        assertFalse(hema.isUncertain());

        // 老百姓大药房 -> 医疗健康
        ClassificationResult pharmacy = classifier.classify("老百姓大药房", "感冒灵颗粒", "支出");
        assertEquals("医疗健康", pharmacy.getCategory());
        assertFalse(pharmacy.isUncertain());
    }

    @Test
    @DisplayName("2. 未识别商户优雅降级为【待确认】(isUncertain = true) 并支持安全兜底")
    void testUnknownMerchantsFallback() {
        // 真实脱敏未识别商户 1: 龙江好日子
        ClassificationResult r1 = classifier.classify("龙江好日子东京城店", "商品消费", "支出");
        assertEquals("待确认", r1.getCategory());
        assertTrue(r1.isUncertain(), "未命中任何品牌或正则的商户必须标记为待确认");
        assertEquals("其他支出", r1.getFallbackCategory(), "支出交易默认兜底为其他支出");
        assertEquals("其他支出", r1.getEffectiveCategory(true), "强制兜底生效时返回其他支出");
        assertEquals("待确认", r1.getEffectiveCategory(false), "不强制兜底时维持待确认");

        // 真实脱敏未识别商户 2: 牡丹江启澜商贸
        ClassificationResult r2 = classifier.classify("牡丹江启澜商贸有限责任公司", "预付账款", "支出");
        assertEquals("待确认", r2.getCategory());
        assertTrue(r2.isUncertain());
        assertEquals("其他支出", r2.getEffectiveCategory(true));

        // 真实脱敏未识别商户 3: 凯荣电气焊
        ClassificationResult r3 = classifier.classify("凯荣电气焊配件门市", "五金加工", "支出");
        assertEquals("待确认", r3.getCategory());
        assertTrue(r3.isUncertain());
        assertEquals("其他支出", r3.getEffectiveCategory(true));

        // 极简便捷调用接口测试
        String directCategory = classifier.classifyToCategoryName("牡丹江启澜商贸有限责任公司", "预付账款", "支出", true);
        assertEquals("其他支出", directCategory);
        String uncertainCategory = classifier.classifyToCategoryName("牡丹江启澜商贸有限责任公司", "预付账款", "支出", false);
        assertEquals("待确认", uncertainCategory);
    }

    @Test
    @DisplayName("3. 收入与支出规则路由、收支隔离与退款/工资等优先匹配")
    void testIncomeVsExpenseClassification() {
        // 退款返还 (优先级 100)
        ClassificationResult refund = classifier.classify("天猫超市退款", "订单取消退款入账", "收入");
        assertEquals("退款返还", refund.getCategory());
        assertFalse(refund.isUncertain());
        assertEquals("收入", refund.getDirection());

        // 工资薪金 (优先级 95)
        ClassificationResult salary = classifier.classify("星界科技有限公司", "2026年10月代发工资", "收入");
        assertEquals("工资薪金", salary.getCategory());
        assertFalse(salary.isUncertain());

        // 红包转账 (收入)
        ClassificationResult redPacket = classifier.classify("微信支付", "收到红包", "收入");
        assertEquals("红包转账", redPacket.getCategory());
        assertFalse(redPacket.isUncertain());

        // 投资理财
        ClassificationResult invest = classifier.classify("蚂蚁财富", "余额宝理财收益入账", "收入");
        assertEquals("投资理财", invest.getCategory());
        assertFalse(invest.isUncertain());

        // 二手闲置
        ClassificationResult secondhand = classifier.classify("闲鱼", "闲鱼二手出售款项", "收入");
        assertEquals("二手闲置", secondhand.getCategory());
        assertFalse(secondhand.isUncertain());

        // 未知收入商户优雅降级为【待确认】并兜底为【其他收入】
        ClassificationResult unknownIncome = classifier.classify("某未知公司海外汇款", "款项存入", "收入");
        assertEquals("待确认", unknownIncome.getCategory());
        assertTrue(unknownIncome.isUncertain());
        assertEquals("其他收入", unknownIncome.getFallbackCategory());
        assertEquals("其他收入", unknownIncome.getEffectiveCategory(true));
        assertEquals("待确认", unknownIncome.getEffectiveCategory(false));
    }

    @Test
    @DisplayName("4. 收支分类集合隔离与动态联动验证")
    void testCategorySetIsolation() {
        List<String> expenseCategories = classifier.getExpenseCategories();
        List<String> incomeCategories = classifier.getIncomeCategories();

        // 支出分类包含 9 项且包含 "其他支出"，不应包含纯收入分类
        assertEquals(9, expenseCategories.size());
        assertTrue(expenseCategories.contains("其他支出"));
        assertTrue(expenseCategories.contains("餐饮美食"));
        assertFalse(expenseCategories.contains("工资薪金"));
        assertFalse(expenseCategories.contains("退款返还"));
        assertFalse(expenseCategories.contains("其他收入"));

        // 收入分类包含 8 项且包含 "其他收入"，不应包含纯支出分类
        assertEquals(8, incomeCategories.size());
        assertTrue(incomeCategories.contains("其他收入"));
        assertTrue(incomeCategories.contains("工资薪金"));
        assertFalse(incomeCategories.contains("餐饮美食"));
        assertFalse(incomeCategories.contains("生活缴费"));
        assertFalse(incomeCategories.contains("其他支出"));

        // 动态获取接口联动
        assertEquals(expenseCategories, classifier.getAvailableCategories("支出"));
        assertEquals(incomeCategories, classifier.getAvailableCategories("收入"));
    }

    @Test
    @DisplayName("5. BillImportItemDTO 实体直接归类回填")
    void testItemDtoClassification() {
        BillImportItemDTO itemKnown = new BillImportItemDTO();
        itemKnown.setPayee("瑞幸咖啡（科技园店）");
        itemKnown.setDetail("生椰拿铁 1杯");
        itemKnown.setType("支出");
        itemKnown.setAmount(new BigDecimal("18.50"));

        classifier.classify(itemKnown);
        assertEquals("餐饮美食", itemKnown.getCategory());
        assertFalse(itemKnown.getIsUncertain());

        BillImportItemDTO itemUnknown = new BillImportItemDTO();
        itemUnknown.setPayee("龙江好日子东京城店");
        itemUnknown.setDetail("商品消费");
        itemUnknown.setType("支出");
        itemUnknown.setAmount(new BigDecimal("35.00"));

        classifier.classify(itemUnknown);
        assertEquals("待确认", itemUnknown.getCategory());
        assertTrue(itemUnknown.getIsUncertain());
    }

    @Test
    @DisplayName("6. BillImportPrecheckVO 聚合统计与指标计算")
    void testPrecheckVoAggregation() {
        List<BillImportItemDTO> items = new ArrayList<>();

        // 已自动识别行
        BillImportItemDTO item1 = new BillImportItemDTO(new Date(), "喜茶", "多肉葡萄", "喜茶饮品", "支出",
                new BigDecimal("21.00"), "微信零钱", "支付成功", "餐饮美食", false, "TX1001", false, "21.00");
        items.add(item1);

        BillImportItemDTO item2 = new BillImportItemDTO(new Date(), "深圳地铁", "深圳地铁乘车码", "地铁扣款", "支出",
                new BigDecimal("5.00"), "微信零钱", "支付成功", "交通出行", false, "TX1002", false, "5.00");
        items.add(item2);

        // 待确认行 (isUncertain = true)
        BillImportItemDTO item3 = new BillImportItemDTO(new Date(), "龙江好日子东京城店", "龙江好日子", "食品杂货", "支出",
                new BigDecimal("50.00"), "微信零钱", "支付成功", "待确认", true, "TX1003", false, "50.00");
        items.add(item3);

        // 待确认行 (category = "待确认")
        BillImportItemDTO item4 = new BillImportItemDTO(new Date(), "牡丹江启澜商贸有限责任公司", "启澜商贸", "五金", "支出",
                new BigDecimal("124.00"), "微信零钱", "支付成功", "待确认", false, "TX1004", false, "124.00");
        items.add(item4);

        BillImportPrecheckVO vo = BillImportPrecheckVO.of(items);

        assertEquals(4, vo.getTotalCount(), "总笔数应为 4");
        assertEquals(2, vo.getAutoCategorizedCount(), "自动分类准确笔数应为 2");
        assertEquals(2, vo.getUncertainCount(), "待确认笔数应为 2");
        assertEquals(new BigDecimal("200.00"), vo.getTotalAmount(), "总金额合计应为 200.00");
        assertEquals(items, vo.getItems());
    }

    @Test
    @DisplayName("7. BillImportConfirmDTO 结构与构造完整性")
    void testConfirmDto() {
        List<BillImportItemDTO> items = new ArrayList<>();
        BillImportItemDTO item = new BillImportItemDTO();
        item.setTxId("TX999");
        item.setAmount(new BigDecimal("99.00"));
        items.add(item);

        BillImportConfirmDTO confirmDTO = new BillImportConfirmDTO(items, 1001, "WECHAT", "SKIP");
        assertEquals(1, confirmDTO.getItems().size());
        assertEquals(1001, confirmDTO.getUserId());
        assertEquals("WECHAT", confirmDTO.getChannel());
        assertEquals("SKIP", confirmDTO.getDuplicateAction());
        assertTrue(confirmDTO.toString().contains("itemsCount=1"));
    }
}
