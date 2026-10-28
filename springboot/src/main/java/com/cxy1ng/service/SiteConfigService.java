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
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SiteConfigService {

    @Resource
    private SiteConfigMapper siteConfigMapper;

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
     * 管理员查询全部网站公共配置（敏感 Key 自动脱敏）
     */
    public List<SiteConfig> selectAllMasked() {
        checkAdminPerm();
        List<SiteConfig> list = siteConfigMapper.selectAll();
        for (SiteConfig item : list) {
            boolean hasVal = StrUtil.isNotBlank(item.getConfigValue());
            item.setHasValue(hasVal);
            if (isSecretKey(item.getConfigKey()) && hasVal) {
                item.setConfigValue(ApiKeyCryptoUtil.mask(item.getConfigValue()));
            }
        }
        return list;
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
