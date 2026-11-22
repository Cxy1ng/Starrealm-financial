package com.cxy1ng.service;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.cxy1ng.dto.bill.BillImportItemDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 账单智能分类引擎
 * 基于 category-rules.json 加载商户品牌库、领域/邮箱特征、高优先级正则树及渠道原生降级规则，
 * 实现毫秒级、0 成本、高精准度的账单分类识别与待确认/兜底状态标记。
 */
@Service
public class BillCategoryClassifier {

    private static final Logger log = LoggerFactory.getLogger(BillCategoryClassifier.class);

    public static final String UNCERTAIN_CATEGORY = "待确认";
    public static final String DEFAULT_EXPENSE_FALLBACK = "其他支出";
    public static final String DEFAULT_INCOME_FALLBACK = "其他收入";

    public static final List<String> EXPENSE_CATEGORIES = Collections.unmodifiableList(Arrays.asList(
            "餐饮美食", "交通出行", "日常购物", "生活缴费",
            "文化休闲", "数码科技", "医疗健康", "红包转账", "其他支出"
    ));

    public static final List<String> INCOME_CATEGORIES = Collections.unmodifiableList(Arrays.asList(
            "工资薪金", "退款返还", "红包转账", "投资理财",
            "副业外快", "补贴奖励", "二手闲置", "其他收入"
    ));

    private final List<DomainRule> domainRules = new ArrayList<>();
    private final List<BrandRule> brandRules = new ArrayList<>();
    private final List<RegexRule> highPriorityRules = new ArrayList<>(); // priority >= 90
    private final List<RegexRule> normalPriorityRules = new ArrayList<>(); // priority < 90
    private final List<FallbackRule> fallbackRules = new ArrayList<>();

    private volatile boolean initialized = false;

    public BillCategoryClassifier() {
        init();
    }

    /**
     * 容器初始化或显式调用加载规则库
     */
    @PostConstruct
    public synchronized void init() {
        if (initialized) {
            return;
        }
        try {
            loadRules();
            initialized = true;
            log.info("BillCategoryClassifier initialized successfully: {} domain patterns, {} brands, {} rules ({} high-priority, {} normal), {} fallbacks.",
                    domainRules.size(), brandRules.size(), (highPriorityRules.size() + normalPriorityRules.size()),
                    highPriorityRules.size(), normalPriorityRules.size(), fallbackRules.size());
        } catch (Exception e) {
            log.error("Failed to load category-rules.json for BillCategoryClassifier", e);
        }
    }

    /**
     * 读取并解析 category-rules.json
     */
    private void loadRules() throws Exception {
        domainRules.clear();
        brandRules.clear();
        highPriorityRules.clear();
        normalPriorityRules.clear();
        fallbackRules.clear();

        InputStream is = obtainRulesInputStream();
        if (is == null) {
            throw new IllegalStateException("Cannot find rules/category-rules.json on classpath or filesystem");
        }

        String jsonContent;
        try (is) {
            jsonContent = IoUtil.read(is, StandardCharsets.UTF_8);
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonContent);

        // 1. 域名与凭证特征
        JsonNode domainsNode = root.get("domainPatterns");
        if (domainsNode != null && domainsNode.isArray()) {
            for (JsonNode d : domainsNode) {
                String patternStr = d.path("pattern").asText(null);
                if (StrUtil.isBlank(patternStr)) continue;
                DomainRule dr = new DomainRule();
                dr.pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                dr.counterparty = d.path("counterparty").asText("");
                dr.category = d.path("category").asText(null);
                dr.direction = d.path("direction").asText("支出");
                dr.defaultCategory = d.path("defaultCategory").asText(null);

                JsonNode detailMatches = d.get("detailMatches");
                if (detailMatches != null && detailMatches.isArray()) {
                    for (JsonNode dm : detailMatches) {
                        String kw = dm.path("keyword").asText(null);
                        String cat = dm.path("category").asText(null);
                        if (StrUtil.isNotBlank(kw) && StrUtil.isNotBlank(cat)) {
                            DetailMatch match = new DetailMatch();
                            match.keywordPattern = Pattern.compile(kw, Pattern.CASE_INSENSITIVE);
                            match.category = cat;
                            dr.detailMatches.add(match);
                        }
                    }
                }
                domainRules.add(dr);
            }
        }

        // 2. 品牌字典 (brandDictionary)
        JsonNode brandDictNode = root.get("brandDictionary");
        if (brandDictNode != null && brandDictNode.isArray()) {
            for (JsonNode b : brandDictNode) {
                String category = b.path("category").asText(null);
                String direction = b.path("direction").asText("支出");
                JsonNode brandsNode = b.get("brands");
                if (brandsNode != null && brandsNode.isArray()) {
                    for (JsonNode brandItem : brandsNode) {
                        String brand = brandItem.asText(null);
                        if (StrUtil.isNotBlank(brand)) {
                            BrandRule br = new BrandRule();
                            br.brand = brand.trim();
                            br.brandLower = br.brand.toLowerCase();
                            br.category = category;
                            br.direction = direction;
                            brandRules.add(br);
                        }
                    }
                }
            }
            // 按品牌词长度降序排序，长词优先匹配（如 "老百姓大药房" 优先于 "药房"）
            brandRules.sort((o1, o2) -> Integer.compare(o2.brand.length(), o1.brand.length()));
        }

        // 3. 关键词正则规则库 (rules)
        JsonNode rulesNode = root.get("rules");
        if (rulesNode != null && rulesNode.isArray()) {
            List<RegexRule> allRules = new ArrayList<>();
            for (JsonNode r : rulesNode) {
                String id = r.path("id").asText("");
                String direction = r.path("direction").asText("支出");
                String category = r.path("category").asText("");
                int priority = r.path("priority").asInt(50);
                String regex = r.path("regex").asText(null);
                if (StrUtil.isBlank(regex)) continue;

                RegexRule rr = new RegexRule();
                rr.id = id;
                rr.direction = direction;
                rr.category = category;
                rr.priority = priority;
                rr.compiledRegex = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

                JsonNode fieldsNode = r.get("matchFields");
                if (fieldsNode != null && fieldsNode.isArray()) {
                    for (JsonNode f : fieldsNode) {
                        rr.matchFields.add(f.asText().toUpperCase());
                    }
                }

                JsonNode excludesNode = r.get("excludes");
                if (excludesNode != null && excludesNode.isArray()) {
                    for (JsonNode ex : excludesNode) {
                        String exWord = ex.asText(null);
                        if (StrUtil.isNotBlank(exWord)) {
                            rr.excludes.add(exWord.trim().toLowerCase());
                        }
                    }
                }
                allRules.add(rr);
            }

            // 按优先级从高到低排序
            allRules.sort((r1, r2) -> Integer.compare(r2.priority, r1.priority));

            for (RegexRule rr : allRules) {
                if (rr.priority >= 90) {
                    highPriorityRules.add(rr);
                } else {
                    normalPriorityRules.add(rr);
                }
            }
        }

        // 4. 渠道原生与兜底规则 (nativeFallbacks)
        JsonNode fallbacksNode = root.get("nativeFallbacks");
        if (fallbacksNode != null && fallbacksNode.isArray()) {
            for (JsonNode fb : fallbacksNode) {
                String patternStr = fb.path("pattern").asText(null);
                String direction = fb.path("direction").asText("支出");
                String category = fb.path("category").asText("");
                if (StrUtil.isNotBlank(patternStr) && StrUtil.isNotBlank(category)) {
                    FallbackRule fbr = new FallbackRule();
                    fbr.pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                    fbr.direction = direction;
                    fbr.category = category;
                    fallbackRules.add(fbr);
                }
            }
        }
    }

    /**
     * 多路径资源加载探针，确保在 IDE、Maven 构建、单元测试或独立运行环境均能精准读取
     */
    private InputStream obtainRulesInputStream() {
        try {
            ClassPathResource cpr = new ClassPathResource("rules/category-rules.json");
            if (cpr.exists()) {
                return cpr.getInputStream();
            }
        } catch (Exception ignored) {
        }

        InputStream is = getClass().getResourceAsStream("/rules/category-rules.json");
        if (is != null) return is;

        is = getClass().getClassLoader().getResourceAsStream("rules/category-rules.json");
        if (is != null) return is;

        is = Thread.currentThread().getContextClassLoader().getResourceAsStream("rules/category-rules.json");
        if (is != null) return is;

        // 文件系统兜底探测
        String[] candidatePaths = {
                "springboot/src/main/resources/rules/category-rules.json",
                "src/main/resources/rules/category-rules.json",
                "PersonalFiPlat/springboot/src/main/resources/rules/category-rules.json",
                "../springboot/src/main/resources/rules/category-rules.json",
                "rules/category-rules.json"
        };
        for (String cp : candidatePaths) {
            Path p = Paths.get(cp);
            if (Files.exists(p)) {
                try {
                    return Files.newInputStream(p);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    /**
     * 核心分类匹配接口
     *
     * @param payee       收款方/交易商户
     * @param detail      商品说明/摘要
     * @param direction   收支属性（"支出" 或 "收入"）
     * @return 分类匹配结果封装
     */
    public ClassificationResult classify(String payee, String detail, String direction) {
        return classify(payee, detail, direction, null);
    }

    /**
     * 全字段分类匹配接口
     *
     * @param payee       收款方/商户名
     * @param detail      商品描述/交易摘要
     * @param direction   收支方向（"支出" / "收入"）
     * @param rawCategory 渠道原始分类或交易类型（如微信中的“商户消费”、“转账”等）
     * @return 分类匹配结果封装
     */
    public ClassificationResult classify(String payee, String detail, String direction, String rawCategory) {
        if (!initialized) {
            init();
        }

        String p = payee != null ? payee.trim() : "";
        String d = detail != null ? detail.trim() : "";
        String rawCat = rawCategory != null ? rawCategory.trim() : "";
        String dir = normalizeDirection(direction, p, d, rawCat);

        String combined = (p + " " + d + " " + rawCat).trim();
        String pLower = p.toLowerCase();
        String dLower = d.toLowerCase();
        String combinedLower = combined.toLowerCase();

        // 阶段 1: 检查域名与电子凭证匹配 (domainPatterns)
        for (DomainRule dr : domainRules) {
            if (dr.direction != null && !dr.direction.equalsIgnoreCase(dir)) {
                continue;
            }
            if (dr.pattern.matcher(combined).find()) {
                if (!dr.detailMatches.isEmpty()) {
                    for (DetailMatch dm : dr.detailMatches) {
                        if (dm.keywordPattern.matcher(combined).find()) {
                            return new ClassificationResult(dm.category, false, "DOMAIN:" + dr.counterparty, dir);
                        }
                    }
                    if (dr.defaultCategory != null) {
                        return new ClassificationResult(dr.defaultCategory, false, "DOMAIN:" + dr.counterparty, dir);
                    }
                } else if (dr.category != null) {
                    return new ClassificationResult(dr.category, false, "DOMAIN:" + dr.counterparty, dir);
                }
            }
        }

        // 阶段 2: 检查超高优先级正则规则 (priority >= 90，如退款返还 100、工资薪金 95、红包转账 90)
        for (RegexRule rr : highPriorityRules) {
            if (matchRegexRule(rr, dir, p, d, pLower, dLower, combined, combinedLower)) {
                return new ClassificationResult(rr.category, false, "RULE:" + rr.id, dir);
            }
        }

        // 阶段 3: 检查品牌字典匹配 (brandDictionary)
        for (BrandRule br : brandRules) {
            if (br.direction != null && !br.direction.equalsIgnoreCase(dir)) {
                continue;
            }
            if (pLower.contains(br.brandLower) || dLower.contains(br.brandLower)) {
                return new ClassificationResult(br.category, false, "BRAND:" + br.brand, dir);
            }
        }

        // 阶段 4: 检查常规关键词正则规则 (priority < 90)
        for (RegexRule rr : normalPriorityRules) {
            if (matchRegexRule(rr, dir, p, d, pLower, dLower, combined, combinedLower)) {
                return new ClassificationResult(rr.category, false, "RULE:" + rr.id, dir);
            }
        }

        // 阶段 5: 检查渠道原生特征与降级规则 (nativeFallbacks)
        for (FallbackRule fbr : fallbackRules) {
            if (fbr.direction != null && !fbr.direction.equalsIgnoreCase(dir)) {
                continue;
            }
            if (fbr.pattern.matcher(combined).find()) {
                return new ClassificationResult(fbr.category, false, "FALLBACK:" + fbr.category, dir);
            }
        }

        // 阶段 6: 未命中任何规则，优雅标记为【待确认】，并生成安全兜底分类
        return new ClassificationResult(UNCERTAIN_CATEGORY, true, "UNMATCHED", dir);
    }

    /**
     * 对 BillImportItemDTO 实体执行智能归类，自动回填 category 与 isUncertain 字段
     */
    public ClassificationResult classify(BillImportItemDTO item) {
        if (item == null) {
            return new ClassificationResult(UNCERTAIN_CATEGORY, true, "NULL_ITEM", "支出");
        }
        ClassificationResult result = classify(item.getPayee(), item.getDetail(), item.getType(), item.getCategory());
        item.setCategory(result.getCategory());
        item.setIsUncertain(result.isUncertain());
        return result;
    }

    /**
     * 智能分类并可选立即执行兜底替换
     *
     * @param payee              收款方
     * @param detail             明细
     * @param direction          收支方向
     * @param fallbackIfUncertain 若为 true 且未识别时，将分类直接赋值为 "其他支出"/"其他收入"
     * @return 分类名称
     */
    public String classifyToCategoryName(String payee, String detail, String direction, boolean fallbackIfUncertain) {
        ClassificationResult result = classify(payee, detail, direction);
        return result.getEffectiveCategory(fallbackIfUncertain);
    }

    /**
     * 单个正则规则匹配校验（含 matchFields 过滤与 excludes 排除）
     */
    private boolean matchRegexRule(RegexRule rr, String dir, String p, String d,
                                   String pLower, String dLower, String combined, String combinedLower) {
        if (rr.direction != null && !rr.direction.equalsIgnoreCase(dir)) {
            return false;
        }

        boolean checkPayee = rr.matchFields.isEmpty() || rr.matchFields.contains("PAYEE");
        boolean checkDetail = rr.matchFields.isEmpty() || rr.matchFields.contains("DETAIL");

        // 1. 先验证 payee
        if (checkPayee && StrUtil.isNotBlank(p)) {
            if (!isExcluded(pLower, rr.excludes)) {
                Matcher m = rr.compiledRegex.matcher(p);
                if (m.find()) {
                    return true;
                }
            }
        }

        // 2. 再验证 detail
        if (checkDetail && StrUtil.isNotBlank(d)) {
            if (!isExcluded(dLower, rr.excludes)) {
                Matcher m = rr.compiledRegex.matcher(d);
                if (m.find()) {
                    return true;
                }
            }
        }

        // 3. 组合串探测（处理商户名与商品名跨字段特征）
        if (!isExcluded(combinedLower, rr.excludes)) {
            Matcher m = rr.compiledRegex.matcher(combined);
            if (m.find()) {
                return true;
            }
        }

        return false;
    }

    private boolean isExcluded(String targetTextLower, List<String> excludes) {
        if (excludes == null || excludes.isEmpty()) {
            return false;
        }
        for (String ex : excludes) {
            if (targetTextLower.contains(ex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 归一化收支方向为标准 "支出" 或 "收入"
     */
    public String normalizeDirection(String direction, String payee, String detail, String rawCategory) {
        if (StrUtil.isNotBlank(direction)) {
            String dir = direction.trim();
            if (dir.contains("支")) {
                return "支出";
            }
            if (dir.contains("收") && !dir.contains("不计")) {
                return "收入";
            }
        }
        // 若方向未明或为 "/"，从文本语义推断
        String combined = (payee + " " + detail + " " + rawCategory);
        if (combined.contains("退款") || combined.contains("转入") || combined.contains("收款")
                || combined.contains("收到") || combined.contains("代发工资") || combined.contains("薪资")) {
            return "收入";
        }
        return "支出";
    }

    /**
     * 根据收支方向获取兜底分类名称
     */
    public String getFallbackCategory(String direction) {
        return "收入".equals(direction) ? DEFAULT_INCOME_FALLBACK : DEFAULT_EXPENSE_FALLBACK;
    }

    public List<String> getExpenseCategories() {
        return EXPENSE_CATEGORIES;
    }

    public List<String> getIncomeCategories() {
        return INCOME_CATEGORIES;
    }

    public List<String> getAvailableCategories(String direction) {
        return "收入".equals(direction) ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
    }

    // ==========================================
    // 内部结构模型
    // ==========================================

    public static class ClassificationResult {
        private String category;
        private boolean isUncertain;
        private String matchedRule;
        private String direction;
        private String fallbackCategory;

        public ClassificationResult() {
        }

        public ClassificationResult(String category, boolean isUncertain, String matchedRule, String direction) {
            this.category = category;
            this.isUncertain = isUncertain;
            this.matchedRule = matchedRule;
            this.direction = direction;
            this.fallbackCategory = "收入".equals(direction) ? DEFAULT_INCOME_FALLBACK : DEFAULT_EXPENSE_FALLBACK;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public boolean isUncertain() {
            return isUncertain;
        }

        public Boolean getIsUncertain() {
            return isUncertain;
        }

        public void setUncertain(boolean uncertain) {
            isUncertain = uncertain;
        }

        public void setIsUncertain(Boolean uncertain) {
            isUncertain = Boolean.TRUE.equals(uncertain);
        }

        public String getMatchedRule() {
            return matchedRule;
        }

        public void setMatchedRule(String matchedRule) {
            this.matchedRule = matchedRule;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public String getFallbackCategory() {
            return fallbackCategory;
        }

        public void setFallbackCategory(String fallbackCategory) {
            this.fallbackCategory = fallbackCategory;
        }

        /**
         * 获取生效分类：若未确定则自动回退至兜底分类（其他支出/其他收入）
         */
        public String getEffectiveCategory() {
            return isUncertain ? fallbackCategory : category;
        }

        /**
         * 获取生效分类，根据入参决定是否降级
         */
        public String getEffectiveCategory(boolean fallbackIfUncertain) {
            return (isUncertain && fallbackIfUncertain) ? fallbackCategory : category;
        }

        @Override
        public String toString() {
            return "ClassificationResult{" +
                    "category='" + category + '\'' +
                    ", isUncertain=" + isUncertain +
                    ", matchedRule='" + matchedRule + '\'' +
                    ", direction='" + direction + '\'' +
                    ", fallbackCategory='" + fallbackCategory + '\'' +
                    '}';
        }
    }

    private static class DomainRule {
        Pattern pattern;
        String counterparty;
        String category;
        String direction;
        String defaultCategory;
        List<DetailMatch> detailMatches = new ArrayList<>();
    }

    private static class DetailMatch {
        Pattern keywordPattern;
        String category;
    }

    private static class BrandRule {
        String brand;
        String brandLower;
        String category;
        String direction;
    }

    private static class RegexRule {
        String id;
        String direction;
        String category;
        int priority;
        Pattern compiledRegex;
        List<String> matchFields = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
    }

    private static class FallbackRule {
        Pattern pattern;
        String direction;
        String category;
    }
}
