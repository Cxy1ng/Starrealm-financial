package com.cxy1ng.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.entity.SiteConfig;
import com.cxy1ng.exception.CustomException;
import com.cxy1ng.mapper.SiteConfigMapper;
import com.cxy1ng.utils.ApiKeyCryptoUtil;
import com.cxy1ng.utils.TokenUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SiteConfigService {

    @Resource
    private SiteConfigMapper siteConfigMapper;

    @PostConstruct
    public void initColumnCapacity() {
        try {
            siteConfigMapper.ensureLongTextColumn();
        } catch (Exception ignored) {
        }
    }

    private boolean isSecretKey(String key) {
        return key != null && (key.endsWith("_key") || key.endsWith("_secret") || key.contains("api_key"));
    }

    private void checkAdminPerm() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || currentUser.getPerm() == null) {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (!PermEnum.ADMIN.equals(role) && !PermEnum.SUPER_ADMIN.equals(role)) {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    /**
     * 管理员查询全部网站公共配置（敏感 Key 自动脱敏，过滤内部系统状态键）
     */
    public List<SiteConfig> selectAllMasked() {
        checkAdminPerm();
        List<SiteConfig> all = siteConfigMapper.selectAll();
        List<SiteConfig> list = new java.util.ArrayList<>();
        for (SiteConfig item : all) {
            String k = item.getConfigKey();
            if (k != null && (k.startsWith("user_ban_") || k.startsWith("account_ban_") || k.startsWith("ai_ban_") || k.startsWith("user_violations_") || k.startsWith("ai_chat_state_"))) {
                continue;
            }
            boolean hasVal = StrUtil.isNotBlank(item.getConfigValue());
            item.setHasValue(hasVal);
            if (isSecretKey(k) && hasVal) {
                item.setConfigValue(ApiKeyCryptoUtil.mask(item.getConfigValue()));
            }
            list.add(item);
        }
        return list;
    }

    /**
     * 内部服务直接写入或更新单条系统状态键值（无需管理员权限，供 AI 历史存档、24h 违规记录与自动封禁调用）
     */
    public void putSystemValue(String key, String rawVal, String description, String operator) {
        if (StrUtil.isBlank(key)) return;
        String now = DateUtil.now();
        String op = StrUtil.isNotBlank(operator) ? operator : "system";
        String valueToStore = rawVal != null ? rawVal : "";
        SiteConfig existing = siteConfigMapper.selectByKey(key);
        if (existing == null) {
            SiteConfig created = new SiteConfig();
            created.setConfigKey(key);
            created.setConfigValue(valueToStore);
            created.setDescription(StrUtil.isNotBlank(description) ? description : key);
            created.setUpdatedTime(now);
            created.setUpdatedBy(op);
            siteConfigMapper.insert(created);
        } else {
            existing.setConfigValue(valueToStore);
            if (StrUtil.isNotBlank(description)) {
                existing.setDescription(description);
            }
            existing.setUpdatedTime(now);
            existing.setUpdatedBy(op);
            siteConfigMapper.updateByKey(existing);
        }
    }

    /**
     * 内部服务读取解密后的配置明文（供汇率转换、AI 公共兜底等调用）
     */
    public String getPlainValue(String configKey, String defaultValue) {
        SiteConfig config = siteConfigMapper.selectByKey(configKey);
        if (config == null || StrUtil.isBlank(config.getConfigValue())) {
            return defaultValue;
        }
        if (isSecretKey(configKey)) {
            String decrypted = ApiKeyCryptoUtil.decrypt(config.getConfigValue());
            return StrUtil.isNotBlank(decrypted) ? decrypted.trim() : defaultValue;
        }
        return config.getConfigValue().trim();
    }

    /**
     * 管理员批量保存网站公共配置（敏感 Key 自动 AES-128 加密）
     */
    public void saveBatch(Map<String, String> payload) {
        checkAdminPerm();
        Account currentUser = TokenUtils.getCurrentUser();
        String operator = currentUser != null && StrUtil.isNotBlank(currentUser.getUsername())
                ? currentUser.getUsername() : "admin";
        String now = DateUtil.now();

        for (Map.Entry<String, String> entry : payload.entrySet()) {
            String key = entry.getKey();
            String rawVal = entry.getValue();
            if (StrUtil.isBlank(key)) continue;

            SiteConfig existing = siteConfigMapper.selectByKey(key);
            String valueToStore = rawVal != null ? rawVal.trim() : "";

            if (isSecretKey(key)) {
                // 如果前端传回的是包含 **** 的脱敏占位串，则保持原密文不变
                if (valueToStore.contains("****")) {
                    continue;
                }
                valueToStore = StrUtil.isNotBlank(valueToStore) ? ApiKeyCryptoUtil.encrypt(valueToStore) : null;
            }

            if (existing == null) {
                SiteConfig created = new SiteConfig();
                created.setConfigKey(key);
                created.setConfigValue(valueToStore);
                created.setDescription(key);
                created.setUpdatedTime(now);
                created.setUpdatedBy(operator);
                siteConfigMapper.insert(created);
            } else {
                existing.setConfigValue(valueToStore);
                existing.setUpdatedTime(now);
                existing.setUpdatedBy(operator);
                siteConfigMapper.updateByKey(existing);
            }
        }
    }
}
