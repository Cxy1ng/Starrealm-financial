package com.cxy1ng;

import com.cxy1ng.dto.bill.BillImportConfirmDTO;
import com.cxy1ng.dto.bill.BillImportItemDTO;
import com.cxy1ng.dto.bill.BillImportPrecheckVO;
import com.cxy1ng.entity.Bill;
import com.cxy1ng.service.BillCategoryClassifier;
import com.cxy1ng.service.BillImportService;
import com.cxy1ng.service.BillService;
import com.cxy1ng.service.parser.AlipayBillParser;
import com.cxy1ng.service.parser.BankBillParser;
import com.cxy1ng.service.parser.PayPalBillParser;
import com.cxy1ng.service.parser.WeChatBillParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("多渠道流水解析器与导入服务综合实测")
public class ChannelBillParserTest {

    private BillCategoryClassifier classifier;
    private WeChatBillParser weChatParser;
    private AlipayBillParser alipayParser;
    private BankBillParser bankParser;
    private PayPalBillParser paypalParser;
    private BillImportService importService;

    @BeforeEach
    void setUp() {
        classifier = new BillCategoryClassifier();
        classifier.init();

        weChatParser = new WeChatBillParser();
        weChatParser.setClassifier(classifier);

        alipayParser = new AlipayBillParser();
        alipayParser.setClassifier(classifier);

        bankParser = new BankBillParser();
        bankParser.setClassifier(classifier);

        paypalParser = new PayPalBillParser();
        paypalParser.setClassifier(classifier);

        importService = new BillImportService();
        importService.setParsers(Arrays.asList(weChatParser, alipayParser, bankParser, paypalParser));
    }

    @Test
    @DisplayName("测试微信真实流水 XLSX 文件全量解析与指标聚合")
    void testWeChatRealExcelParsing() throws Exception {
        File file = new File("../temp-examples/微信支付账单流水文件(20260319-20260517)_20260930131508.xlsx");
        if (!file.exists()) {
            file = new File("temp-examples/微信支付账单流水文件(20260319-20260517)_20260930131508.xlsx");
        }
        if (!file.exists()) {
            file = new File("PersonalFiPlat/temp-examples/微信支付账单流水文件(20260319-20260517)_20260930131508.xlsx");
        }
        if (!file.exists()) {
            file = new File("d:/GitRepo/PersonalFiPlat/temp-examples/微信支付账单流水文件(20260319-20260517)_20260930131508.xlsx");
        }

        assertTrue(file.exists(), "微信真实流水样本文件应存在于指定路径");

        List<BillImportItemDTO> items;
        try (FileInputStream fis = new FileInputStream(file)) {
            items = weChatParser.parse(fis, file.getName(), 1);
        }

        assertNotNull(items);
        assertFalse(items.isEmpty());
        assertEquals(199, items.size(), "微信样本账单应精确解析出 199 笔有效流水记录");

        BillImportPrecheckVO precheck = BillImportPrecheckVO.of(items, "wechat", file.getName());
        assertEquals(199, precheck.getTotalCount());
        assertTrue(precheck.getAutoCategorizedCount() > 180, "自动精准归类笔数应达到 90% 以上");
        assertTrue(precheck.getTotalAmount().compareTo(BigDecimal.ZERO) > 0);

        // 打印前 5 个收款方以便观察真实样本商户
        for (int i = 0; i < Math.min(5, items.size()); i++) {
            System.out.println("WeChat sample item " + i + ": payee=" + items.get(i).getPayee()
                    + ", category=" + items.get(i).getCategory() + ", amount=" + items.get(i).getAmount());
        }
    }

    @Test
    @DisplayName("测试支付宝 CSV 格式解析及首尾噪声过滤")
    void testAlipayCsvParsing() {
        String alipayCsv = "------------------------------------------------------------------------------------\n"
                + "支付宝交易记录明细列表\n"
                + "账号: 13800000000\n"
                + "起始日期: 2026-05-01\n"
                + "------------------------------------------------------------------------------------\n"
                + "交易号,商家订单号,交易创建时间,付款时间,最近修改时间,交易来源地,类型,交易对方,商品名称,金额（元）,收/支,交易状态,服务费（元）,成功退款（元）,备注,资金状态\n"
                + "2026050122001111,ORDER101,2026-05-01 12:00:00,2026-05-01 12:00:05,2026-05-01 12:00:05,其他,即时到账,美团外卖平台,麻辣烫套餐,28.50,支出,交易成功,0.00,0.00,,已支出\n"
                + "2026050122002222,ORDER102,2026-05-01 14:30:00,2026-05-01 14:30:05,2026-05-01 14:30:05,其他,即时到账,中国石化销售加油站,加油费,300.00,支出,交易成功,0.00,0.00,,已支出\n"
                + "2026050122003333,ORDER103,2026-05-01 18:20:00,2026-05-01 18:20:05,2026-05-01 18:20:05,其他,退款,美团平台商户,退还差价,10.00,收入,已退款,0.00,0.00,,已退款\n"
                + "2026050122004444,ORDER104,2026-05-01 20:10:00,2026-05-01 20:10:05,2026-05-01 20:10:05,其他,即时到账,牡丹江启澜商贸有限责任公司,杂项商品,150.00,支出,交易成功,0.00,0.00,,已支出\n"
                + "------------------------------------------------------------------------------------\n"
                + "共 4 笔记录\n"
                + "已收入: 1 笔 10.00 元\n"
                + "已支出: 3 笔 378.50 元\n";

        ByteArrayInputStream bais = new ByteArrayInputStream(alipayCsv.getBytes(StandardCharsets.UTF_8));
        List<BillImportItemDTO> items = alipayParser.parse(bais, "alipay_record.csv", 1);

        assertNotNull(items);
        assertEquals(4, items.size());

        assertEquals("餐饮美食", items.get(0).getCategory());
        assertFalse(items.get(0).getIsUncertain());

        assertEquals("交通出行", items.get(1).getCategory());
        assertFalse(items.get(1).getIsUncertain());

        assertEquals("退款返还", items.get(2).getCategory());
        assertFalse(items.get(2).getIsUncertain());

        assertEquals("待确认", items.get(3).getCategory());
        assertTrue(items.get(3).getIsUncertain());
    }

    @Test
    @DisplayName("测试银行对账单借贷标志与外币识别")
    void testBankBillParsing() {
        String bankCsv = "交易日期,摘要,对方户名,借贷标志,金额,币种,交易流水号\n"
                + "2026-05-02 09:15:00,网上快捷支付,国家电网上海市电力公司,借,150.00,CNY,BK20260502001\n"
                + "2026-05-02 10:00:00,跨行代发,腾讯科技（深圳）有限公司代发工资,贷,18000.00,CNY,BK20260502002\n"
                + "2026-05-02 15:45:00,境外消费,Apple Store Regent St,借,99.00,USD,BK20260502003\n";

        ByteArrayInputStream bais = new ByteArrayInputStream(bankCsv.getBytes(StandardCharsets.UTF_8));
        List<BillImportItemDTO> items = bankParser.parse(bais, "bank_statement.csv", 1);

        assertNotNull(items);
        assertEquals(3, items.size());

        // 电费 -> 生活缴费 支出
        assertEquals("支出", items.get(0).getType());
        assertEquals("生活缴费", items.get(0).getCategory());

        // 工资 -> 工资薪金 收入
        assertEquals("收入", items.get(1).getType());
        assertEquals("工资薪金", items.get(1).getCategory());

        // 境外消费 -> 数码科技 外币
        assertEquals("支出", items.get(2).getType());
        assertEquals("数码科技", items.get(2).getCategory());
        assertTrue(items.get(2).getIsForeign());
    }

    @Test
    @DisplayName("测试 PayPal 英文账单及跨境属性解析")
    void testPayPalBillParsing() {
        String paypalCsv = "Date,Time,TimeZone,Name,Type,Status,Currency,Gross,Fee,Net,From Email Address,To Email Address,Transaction ID\n"
                + "05/03/2026,08:30:15,PST,Steam Games,Payment,Completed,USD,-49.99,0.00,-49.99,user@test.com,store@valvesoftware.com,PAYPAL998811\n"
                + "05/03/2026,11:20:00,PST,GitHub Inc,Subscription,Completed,USD,-10.00,0.00,-10.00,user@test.com,billing@github.com,PAYPAL998822\n";

        ByteArrayInputStream bais = new ByteArrayInputStream(paypalCsv.getBytes(StandardCharsets.UTF_8));
        List<BillImportItemDTO> items = paypalParser.parse(bais, "paypal_export.csv", 1);

        assertNotNull(items);
        assertEquals(2, items.size());

        // Steam -> 文化休闲
        assertEquals("支出", items.get(0).getType());
        assertEquals("文化休闲", items.get(0).getCategory());
        assertTrue(items.get(0).getIsForeign());
        assertEquals(new BigDecimal("49.99"), items.get(0).getAmount());

        // GitHub -> 数码科技
        assertEquals("支出", items.get(1).getType());
        assertEquals("数码科技", items.get(1).getCategory());
        assertTrue(items.get(1).getIsForeign());
        assertEquals(new BigDecimal("10.00"), items.get(1).getAmount());
    }

    @Test
    @DisplayName("测试真实模拟示例文件解析与时分秒保留")
    void testSampleFilesParsing() throws Exception {
        File alipayFile = new File("d:/GitRepo/PersonalFiPlat/temp-examples/alipay_test_statement.csv");
        if (alipayFile.exists()) {
            try (FileInputStream fis = new FileInputStream(alipayFile)) {
                List<BillImportItemDTO> items = alipayParser.parse(fis, alipayFile.getName(), 1);
                assertNotNull(items);
                assertEquals(20, items.size());
            }
        }

        File bankFile = new File("d:/GitRepo/PersonalFiPlat/temp-examples/cmb_bank_statement.csv");
        if (bankFile.exists()) {
            try (FileInputStream fis = new FileInputStream(bankFile)) {
                List<BillImportItemDTO> items = bankParser.parse(fis, bankFile.getName(), 1);
                assertNotNull(items);
                assertEquals(10, items.size());
                assertNotNull(items.get(0).getTime());
                String tStr = cn.hutool.core.date.DateUtil.format(items.get(0).getTime(), "yyyy-MM-dd HH:mm:ss");
                assertEquals("2026-11-02 08:30:00", tStr);
            }
        }
    }

    @Test
    @DisplayName("测试用户选错渠道时的内容指纹嗅探与智能纠偏")
    void testChannelAutoCorrection() throws Exception {
        String alipayContent = "------------------------------------------------------------------------------------\n"
                + "支付宝交易记录明细列表\n"
                + "账号: test@alipay.com\n"
                + "------------------------------------------------------------------------------------\n"
                + "交易号,商家订单号,交易创建时间,类型,交易对方,商品名称,金额（元）,收/支,交易状态\n"
                + "2026110122001001,ORDER01,2026-11-01 12:00:00,即时到账,美团外卖,快餐,25.00,支出,交易成功\n";

        MockMultipartFile mockFile = new MockMultipartFile("file", "unknown_bill.csv", "text/csv", alipayContent.getBytes(StandardCharsets.UTF_8));

        // 用户选的是 wechat，但文件内容是支付宝
        BillImportPrecheckVO vo = importService.precheck(mockFile, "wechat", 1);
        assertNotNull(vo);
        assertTrue(vo.getIsChannelCorrected());
        assertEquals("alipay", vo.getChannel());
        assertEquals("wechat", vo.getOriginalChannel());
        assertNotNull(vo.getCorrectionNotice());
        assertEquals(1, vo.getTotalCount());
    }

    @Test
    @DisplayName("测试解析器策略动态路由与名称探测")
    void testResolveParser() {
        // 显式渠道参数匹配
        assertEquals(weChatParser, importService.resolveParser("wechat", null));
        assertEquals(alipayParser, importService.resolveParser("alipay", null));
        assertEquals(bankParser, importService.resolveParser("bank", null));
        assertEquals(paypalParser, importService.resolveParser("paypal", null));

        // 文件名智能推断
        assertEquals(weChatParser, importService.resolveParser(null, "微信支付账单流水.xlsx"));
        assertEquals(alipayParser, importService.resolveParser(null, "alipay_record_2026.csv"));
        assertEquals(bankParser, importService.resolveParser(null, "招商银行对账单.xlsx"));
        assertEquals(paypalParser, importService.resolveParser(null, "paypal_export.csv"));

        // 未知渠道与文件名兜底
        assertEquals(weChatParser, importService.resolveParser("unknown_channel", "unknown_file.bin"));
    }

    @Test
    @DisplayName("测试 BillImportService 预检流程与 MockMultipartFile 适配")
    void testImportServicePrecheck() {
        String alipayCsv = "交易号,商家订单号,交易创建时间,付款时间,最近修改时间,交易来源地,类型,交易对方,商品名称,金额（元）,收/支,交易状态,服务费（元）,成功退款（元）,备注,资金状态\n"
                + "2026050122001111,ORDER101,2026-05-01 12:00:00,2026-05-01 12:00:05,2026-05-01 12:00:05,其他,即时到账,美团外卖,快餐,32.00,支出,交易成功,0.00,0.00,,已支出\n";

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "alipay_record.csv", "text/csv", alipayCsv.getBytes(StandardCharsets.UTF_8));

        BillImportPrecheckVO precheck = importService.precheck(multipartFile, "alipay", 1);
        assertNotNull(precheck);
        assertEquals(1, precheck.getTotalCount());
        assertEquals("alipay", precheck.getChannel());
        assertEquals("alipay_record.csv", precheck.getFilename());
        assertEquals("餐饮美食", precheck.getItems().get(0).getCategory());
        assertFalse(precheck.getItems().get(0).getIsUncertain());
    }

    @Test
    @DisplayName("测试 BillImportService 确认入库与分类安全兜底机制")
    void testImportServiceConfirm() {
        BillService mockBillService = mock(BillService.class);
        when(mockBillService.insertBatch(anyList(), eq(1)))
                .thenAnswer(invocation -> {
                    List<Bill> list = invocation.getArgument(0);
                    return list.size();
                });

        importService.setBillService(mockBillService);

        List<BillImportItemDTO> items = new ArrayList<>();

        // 已分类支出
        BillImportItemDTO item1 = new BillImportItemDTO();
        item1.setPayee("喜茶");
        item1.setType("支出");
        item1.setAmount(new BigDecimal("22.00"));
        item1.setCategory("餐饮美食");
        item1.setTxId("TX001");
        items.add(item1);

        // 待确认支出 -> 必须安全降级为 "其他支出"
        BillImportItemDTO item2 = new BillImportItemDTO();
        item2.setPayee("龙江好日子东京城店");
        item2.setType("支出");
        item2.setAmount(new BigDecimal("48.00"));
        item2.setCategory("待确认");
        item2.setTxId("TX002");
        items.add(item2);

        // 待确认收入 -> 必须安全降级为 "其他收入"
        BillImportItemDTO item3 = new BillImportItemDTO();
        item3.setPayee("未知汇款");
        item3.setType("收入");
        item3.setAmount(new BigDecimal("100.00"));
        item3.setCategory("待确认");
        item3.setTxId("TX003");
        items.add(item3);

        BillImportConfirmDTO confirmDTO = new BillImportConfirmDTO(items, 1, "wechat", "SKIP");
        int count = importService.confirm(confirmDTO);

        assertEquals(3, count);

        // 捕获并断言落库实体
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(mockBillService).insertBatch(captor.capture(), eq(1));
        verify(mockBillService).syncUserAcfun(1);

        List<Bill> savedBills = (List<Bill>) captor.getValue();
        assertEquals(3, savedBills.size());

        // item1 维持已确认分类
        assertEquals("餐饮美食", savedBills.get(0).getCategory());
        assertEquals("单号:TX001", savedBills.get(0).getComment());

        // item2 待确认降级为其他支出
        assertEquals("其他支出", savedBills.get(1).getCategory());
        assertEquals("单号:TX002", savedBills.get(1).getComment());

        // item3 待确认降级为其他收入
        assertEquals("其他收入", savedBills.get(2).getCategory());
        assertEquals("单号:TX003", savedBills.get(2).getComment());
    }
}
