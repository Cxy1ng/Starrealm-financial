package com.cxy1ng.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.cxy1ng.common.Constants;
import com.cxy1ng.common.enums.PermEnum;
import com.cxy1ng.common.enums.ResultCodeEnum;
import com.cxy1ng.entity.Account;
import com.cxy1ng.entity.User;
import com.cxy1ng.exception.CustomException;
import com.cxy1ng.mapper.UserMapper;
import com.cxy1ng.utils.ApiKeyCryptoUtil;
import com.cxy1ng.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.apache.poi.util.StringUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private SiteConfigService siteConfigService;

    private static final long WINDOW_24H_MS = 24L * 60 * 60 * 1000;
    private static final int AUTO_BAN_THRESHOLD = 10;

    public static long getDurationMsByLevel(int level) {
        return switch (level) {
            case 1 -> 24L * 60 * 60 * 1000L;           // 一次：24小时
            case 2 -> 3L * 24 * 60 * 60 * 1000L;       // 二次：3天
            case 3 -> 30L * 24 * 60 * 60 * 1000L;      // 三次：1个月(30天)
            default -> -1L;                            // 四次及以上：永久
        };
    }

    public static String getDurationLabelByLevel(int level) {
        return switch (level) {
            case 1 -> "24小时";
            case 2 -> "3天";
            case 3 -> "1个月";
            default -> "永久";
        };
    }

    public void populateBanInfo(User user) {
        if (user == null || user.getId() == null) return;

        // 1. 读取整个账号封禁状态（仅针对存钱日记、存钱计划、个人简介内容违规）
        String accRaw = siteConfigService.getPlainValue("account_ban_" + user.getId(), "");
        if (StrUtil.isNotBlank(accRaw)) {
            try {
                cn.hutool.json.JSONObject accObj = cn.hutool.json.JSONUtil.parseObj(accRaw);
                int isAccBanned = accObj.getInt("banned", 0);
                String accReason = accObj.getStr("banReason", "");
                String accTime = accObj.getStr("banTime", "");
                user.setAccountBanned(isAccBanned);
                user.setAccountBanReason(accReason);
                user.setAccountBanTime(accTime);
                user.setBanned(isAccBanned);
                user.setBanReason(accReason);
                user.setBanTime(accTime);
            } catch (Exception ignored) {
                user.setAccountBanned(0);
                user.setBanned(0);
            }
        } else {
            user.setAccountBanned(0);
            user.setBanned(0);
        }

        // 2. 读取 AI 对话功能封禁状态（阶梯式：1次24h、2次3天、3次1个月、4次永久）
        String aiKey = "ai_ban_" + user.getId();
        String aiRaw = siteConfigService.getPlainValue(aiKey, "");
        if (StrUtil.isBlank(aiRaw)) {
            // 兼容旧版 user_ban_ 键并自动迁移为 ai_ban_
            String legacyRaw = siteConfigService.getPlainValue("user_ban_" + user.getId(), "");
            if (StrUtil.isNotBlank(legacyRaw)) {
                aiRaw = legacyRaw;
            }
        }
        if (StrUtil.isNotBlank(aiRaw)) {
            try {
                cn.hutool.json.JSONObject aiObj = cn.hutool.json.JSONUtil.parseObj(aiRaw);
                int aiBanned = aiObj.getInt("aiBanned", aiObj.getInt("banned", 0));
                int aiBanCount = aiObj.getInt("aiBanCount", aiBanned == 1 ? 1 : 0);
                int aiBanLevel = aiObj.getInt("aiBanLevel", aiBanCount > 0 ? Math.min(4, aiBanCount) : 0);
                String aiBanReason = aiObj.getStr("aiBanReason", aiObj.getStr("banReason", ""));
                String aiBanTime = aiObj.getStr("aiBanTime", aiObj.getStr("banTime", ""));
                String aiBanDuration = aiObj.getStr("aiBanDuration", aiBanLevel > 0 ? getDurationLabelByLevel(aiBanLevel) : "");
                String aiBanExpireTime = aiObj.getStr("aiBanExpireTime", aiBanLevel >= 4 ? "永久" : "");
                long expireMs = aiObj.getLong("aiBanExpireMs", -1L);

                // 检查非永久封禁是否已自然到期
                if (aiBanned == 1 && expireMs > 0 && System.currentTimeMillis() >= expireMs) {
                    aiBanned = 0;
                    aiObj.set("aiBanned", 0);
                    aiObj.set("banned", 0);
                    siteConfigService.putSystemValue(aiKey, aiObj.toString(), "用户AI对话阶梯封禁状态", "auto-expire");
                }

                user.setAiBanned(aiBanned);
                user.setAiBanCount(aiBanCount);
                user.setAiBanLevel(aiBanLevel);
                user.setAiBanReason(aiBanReason);
                user.setAiBanTime(aiBanTime);
                user.setAiBanDuration(aiBanDuration);
                user.setAiBanExpireTime(aiBanExpireTime);
            } catch (Exception ignored) {
                user.setAiBanned(0);
                user.setAiBanCount(0);
                user.setAiBanLevel(0);
            }
        } else {
            user.setAiBanned(0);
            user.setAiBanCount(0);
            user.setAiBanLevel(0);
        }

        // 3. 读取并滚动过滤 24 小时内的 AI 违规行为记录（绑定用户，管理员可见）
        String vioRaw = siteConfigService.getPlainValue("user_violations_" + user.getId(), "");
        cn.hutool.json.JSONArray validLogs = new cn.hutool.json.JSONArray();
        if (StrUtil.isNotBlank(vioRaw)) {
            long cutoff = System.currentTimeMillis() - WINDOW_24H_MS;
            try {
                cn.hutool.json.JSONArray arr = cn.hutool.json.JSONUtil.parseArray(vioRaw);
                for (int i = 0; i < arr.size(); i++) {
                    cn.hutool.json.JSONObject item = arr.getJSONObject(i);
                    if (item != null && item.getLong("ts", 0L) >= cutoff) {
                        validLogs.add(item);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        user.setViolationCount24h(validLogs.size());
        user.setViolationLogs(validLogs.toString());
    }

    /**
     * 构造并持久化指定等级的 AI 封禁记录
     */
    private cn.hutool.json.JSONObject applyAiBanByLevel(Integer userId, int banCount, int level, String reason, String operator) {
        int clampedLevel = Math.max(1, Math.min(4, level));
        long nowMs = System.currentTimeMillis();
        String nowStr = DateUtil.now();
        long durationMs = getDurationMsByLevel(clampedLevel);
        String durationLabel = getDurationLabelByLevel(clampedLevel);
        long expireMs = durationMs > 0 ? (nowMs + durationMs) : -1L;
        String expireTimeStr = durationMs > 0
                ? DateUtil.formatDateTime(new java.util.Date(expireMs))
                : "永久";

        cn.hutool.json.JSONObject banObj = new cn.hutool.json.JSONObject();
        banObj.set("aiBanned", 1);
        banObj.set("banned", 1);
        banObj.set("aiBanCount", Math.max(banCount, clampedLevel));
        banObj.set("aiBanLevel", clampedLevel);
        banObj.set("aiBanReason", reason);
        banObj.set("aiBanTime", nowStr);
        banObj.set("aiBanStartMs", nowMs);
        banObj.set("aiBanDuration", durationLabel);
        banObj.set("aiBanExpireTime", expireTimeStr);
        banObj.set("aiBanExpireMs", expireMs);

        siteConfigService.putSystemValue("ai_ban_" + userId, banObj.toString(), "用户AI对话阶梯封禁状态", operator);
        return banObj;
    }

    /**
     * 记录一次用户 AI 违规行为（24 小时滚动窗口），若 24h 内未处分的累计违规超过 10 次则自动按阶梯封禁其 AI 对话功能
     * （一次：24小时，二次：3天，三次：1个月，四次：永久）
     */
    public synchronized java.util.Map<String, Object> recordAiViolation(Integer userId, String category, String snippet, String source) {
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        if (userId == null) {
            res.put("violationCount24h", 0);
            res.put("aiBanned", 0);
            res.put("banned", 0);
            return res;
        }
        long nowMs = System.currentTimeMillis();
        long cutoff = nowMs - WINDOW_24H_MS;
        String vioKey = "user_violations_" + userId;
        String vioRaw = siteConfigService.getPlainValue(vioKey, "");
        cn.hutool.json.JSONArray validLogs = new cn.hutool.json.JSONArray();
        int uncountedCount = 0;

        if (StrUtil.isNotBlank(vioRaw)) {
            try {
                cn.hutool.json.JSONArray arr = cn.hutool.json.JSONUtil.parseArray(vioRaw);
                for (int i = 0; i < arr.size(); i++) {
                    cn.hutool.json.JSONObject item = arr.getJSONObject(i);
                    if (item != null && item.getLong("ts", 0L) >= cutoff) {
                        validLogs.add(item);
                        if (!item.getBool("settled", false)) {
                            uncountedCount++;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        String safeSnippet = snippet != null ? snippet.replaceAll("\\s+", " ").trim() : "";
        if (safeSnippet.length() > 140) {
            safeSnippet = safeSnippet.substring(0, 140) + "...";
        }

        cn.hutool.json.JSONObject newEntry = new cn.hutool.json.JSONObject();
        newEntry.set("ts", nowMs);
        newEntry.set("time", DateUtil.now());
        newEntry.set("category", StrUtil.isNotBlank(category) ? category : "违反《星界用户服务协议》");
        newEntry.set("source", StrUtil.isNotBlank(source) ? source : "前端拦截");
        newEntry.set("snippet", safeSnippet);
        newEntry.set("settled", false);
        validLogs.add(newEntry);
        uncountedCount++;

        int count24h = validLogs.size();

        // 读取当前 AI 封禁状态与历史累计封禁次数
        User tempUser = new User();
        tempUser.setId(userId);
        populateBanInfo(tempUser);

        int currentAiBanned = tempUser.getAiBanned() != null ? tempUser.getAiBanned() : 0;
        int prevBanCount = tempUser.getAiBanCount() != null ? tempUser.getAiBanCount() : 0;

        // 当 24 小时内累计违规超过 10 次（即 > 10）且当前未封禁时，触发自动阶梯封禁
        if (uncountedCount > AUTO_BAN_THRESHOLD && currentAiBanned != 1) {
            int nextBanCount = prevBanCount + 1;
            int nextLevel = Math.min(4, nextBanCount);
            String autoReason = "24 小时内多次发送违反《星界用户服务协议》的受限或敏感内容";

            // 将本轮触发封禁的违规记录标记为已结算，保留在 24h 列表供管理员审查
            for (int i = 0; i < validLogs.size(); i++) {
                cn.hutool.json.JSONObject item = validLogs.getJSONObject(i);
                if (item != null) {
                    item.set("settled", true);
                }
            }
            siteConfigService.putSystemValue(vioKey, validLogs.toString(), "用户24h内AI违规记录", "guardrail");
            applyAiBanByLevel(userId, nextBanCount, nextLevel, autoReason, "auto-guardrail");
            populateBanInfo(tempUser);
        } else {
            siteConfigService.putSystemValue(vioKey, validLogs.toString(), "用户24h内AI违规记录", "guardrail");
        }

        res.put("violationCount24h", count24h);
        res.put("uncountedCount24h", uncountedCount);
        res.put("threshold", AUTO_BAN_THRESHOLD);
        res.put("aiBanned", tempUser.getAiBanned());
        res.put("banned", tempUser.getAiBanned());
        res.put("aiBanLevel", tempUser.getAiBanLevel());
        res.put("aiBanCount", tempUser.getAiBanCount());
        res.put("aiBanReason", tempUser.getAiBanReason());
        res.put("banReason", tempUser.getAiBanReason());
        res.put("aiBanTime", tempUser.getAiBanTime());
        res.put("banTime", tempUser.getAiBanTime());
        res.put("aiBanDuration", tempUser.getAiBanDuration());
        res.put("aiBanExpireTime", tempUser.getAiBanExpireTime());
        return res;
    }

    /**
     * 管理员更新用户「账号封禁」状态（仅限存钱日记、存钱计划、个人简介等社区内容违规）
     */
    public void updateBanStatus(User req) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (!PermEnum.ADMIN.equals(role) && !PermEnum.SUPER_ADMIN.equals(role)) {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
        if (req == null || req.getId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        int banned = (req.getBanned() != null && req.getBanned() == 1) || (req.getAccountBanned() != null && req.getAccountBanned() == 1) ? 1 : 0;
        String key = "account_ban_" + req.getId();
        String operator = currentUser.getUsername();
        if (banned == 1) {
            String reason = StrUtil.isNotBlank(req.getBanReason()) ? req.getBanReason().trim()
                    : (StrUtil.isNotBlank(req.getAccountBanReason()) ? req.getAccountBanReason().trim()
                    : "存钱计划、理财日记或个人简介存在违规违禁内容，违反《星界用户服务协议》");
            cn.hutool.json.JSONObject obj = new cn.hutool.json.JSONObject();
            obj.set("banned", 1);
            obj.set("banReason", reason);
            obj.set("banTime", DateUtil.now());
            siteConfigService.putSystemValue(key, obj.toString(), "用户账号封禁状态(日记/计划/简介违规)", operator);
        } else {
            siteConfigService.putSystemValue(key, "", "用户账号封禁状态(日记/计划/简介违规)", operator);
        }
    }

    /**
     * 管理员审核、解除、降级或手动调整用户的 AI 对话功能封禁
     */
    public void auditAiBan(java.util.Map<String, Object> payload) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (!PermEnum.ADMIN.equals(role) && !PermEnum.SUPER_ADMIN.equals(role)) {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
        Integer userId = payload.get("userId") != null ? Integer.valueOf(String.valueOf(payload.get("userId"))) : null;
        if (userId == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        String action = String.valueOf(payload.getOrDefault("action", "UNBAN")).toUpperCase();
        String reason = String.valueOf(payload.getOrDefault("reason", "")).trim();
        String operator = currentUser.getUsername();

        User target = new User();
        target.setId(userId);
        populateBanInfo(target);
        int currentCount = target.getAiBanCount() != null ? target.getAiBanCount() : 0;
        int currentLevel = target.getAiBanLevel() != null ? target.getAiBanLevel() : 0;

        if ("CLEAR_VIOLATIONS".equals(action)) {
            siteConfigService.putSystemValue("user_violations_" + userId, "[]", "用户24h内AI违规记录", operator);
            return;
        }

        if ("UNBAN".equals(action)) {
            boolean resetCount = Boolean.parseBoolean(String.valueOf(payload.getOrDefault("resetCount", "false")));
            int nextCount = resetCount ? 0 : currentCount;
            cn.hutool.json.JSONObject obj = new cn.hutool.json.JSONObject();
            obj.set("aiBanned", 0);
            obj.set("banned", 0);
            obj.set("aiBanCount", nextCount);
            obj.set("aiBanLevel", 0);
            obj.set("aiBanReason", "");
            obj.set("aiBanTime", "");
            obj.set("aiBanDuration", "");
            obj.set("aiBanExpireTime", "");
            obj.set("aiBanExpireMs", -1L);
            siteConfigService.putSystemValue("ai_ban_" + userId, obj.toString(), "用户AI对话阶梯封禁状态", operator);
            siteConfigService.putSystemValue("user_ban_" + userId, "", "旧版AI封禁清理", operator);

            // 将当前 24h 违规记录全部标记为已结算（管理员仍可查看历史，但不会立刻再次触发自动封禁）
            String vioKey = "user_violations_" + userId;
            String vioRaw = siteConfigService.getPlainValue(vioKey, "");
            if (StrUtil.isNotBlank(vioRaw)) {
                try {
                    cn.hutool.json.JSONArray arr = cn.hutool.json.JSONUtil.parseArray(vioRaw);
                    for (int i = 0; i < arr.size(); i++) {
                        cn.hutool.json.JSONObject item = arr.getJSONObject(i);
                        if (item != null) item.set("settled", true);
                    }
                    siteConfigService.putSystemValue(vioKey, arr.toString(), "用户24h内AI违规记录", operator);
                } catch (Exception ignored) {
                }
            }
            return;
        }

        if ("DOWNGRADE".equals(action) || "BAN".equals(action)) {
            int targetLevel = payload.get("targetLevel") != null
                    ? Integer.parseInt(String.valueOf(payload.get("targetLevel")))
                    : Math.max(1, currentLevel - 1);
            targetLevel = Math.max(1, Math.min(4, targetLevel));
            String finalReason = StrUtil.isNotBlank(reason)
                    ? reason
                    : ("DOWNGRADE".equals(action)
                        ? "管理员审核降级为第 " + targetLevel + " 档处分（" + getDurationLabelByLevel(targetLevel) + "）：" + (StrUtil.isNotBlank(target.getAiBanReason()) ? target.getAiBanReason() : "违反 AI 安全规范")
                        : "违反《星界用户服务协议》第 4.2 条（恶意提示词破甲或违规提问），处以第 " + targetLevel + " 档 AI 对话封禁（" + getDurationLabelByLevel(targetLevel) + "）");
            applyAiBanByLevel(userId, targetLevel, targetLevel, finalReason, operator);
            siteConfigService.putSystemValue("user_ban_" + userId, "", "旧版AI封禁清理", operator);
        }
    }

    public List<User> selectAll(User user) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            List<User> list = userMapper.selectAll(user);
            list.forEach(this::populateBanInfo);
            return list;
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public User selectById(Integer id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setHasApiKey(StrUtil.isNotBlank(user.getApiKey()));
            populateBanInfo(user);
        }
        return user;
    }

    public PageInfo<User> selectPage(User user, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            PageHelper.startPage(pageNum, pageSize);
            List<User> list = userMapper.selectAll(user);
            list.forEach(this::populateBanInfo);
            return PageInfo.of(list);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public void insert(User user) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            User dbUser = userMapper.selectByUsername(user.getUsername());
            if (ObjectUtil.isNotNull(dbUser)) {
                throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
            }
            if (ObjectUtil.isEmpty(user.getUsername())) {
                user.setUsername(user.getUsername());
            }
            if (ObjectUtil.isEmpty(user.getNickname())) {
                user.setNickname(user.getUsername());
            }
            if (ObjectUtil.isEmpty(user.getPwd())) {
                user.setPwd(Constants.USER_DEFAULT_PASSWORD);
            }
            user.setPerm(PermEnum.USER.getMinPerm());
            user.setRole(String.valueOf(PermEnum.getRoleByPerm(user.getPerm())));
            userMapper.insert(user);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public void update(User user) {
        if ("__CLEAR__".equals(user.getApiKey())) {
            user.setApiKey("");
        } else if (StrUtil.isNotBlank(user.getApiKey())) {
            // 如果包含脱敏星号则不覆盖原数据库值
            if (user.getApiKey().contains("****")) {
                user.setApiKey(null);
            } else {
                user.setApiKey(ApiKeyCryptoUtil.encrypt(user.getApiKey()));
            }
        } else {
            user.setApiKey(null);
        }
        userMapper.update(user);
    }

    public void deleteById(Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            userMapper.deleteById(id);
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public void deleteBatch(List<Integer> ids) {
        Account currentUser = TokenUtils.getCurrentUser();
        PermEnum role = PermEnum.getRoleByPerm(currentUser.getPerm());
        if (role.equals(PermEnum.ADMIN) || role.equals(PermEnum.SUPER_ADMIN)) {
            for (Integer id : ids) {
                this.deleteById(id);
            }
        } else {
            throw new CustomException(ResultCodeEnum.PERMISSION_ERROR);
        }
    }

    public Account login(Account account) {
        User dbUser = userMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbUser)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!account.getPwd().equals(dbUser.getPwd())) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        populateBanInfo(dbUser);
        if (Integer.valueOf(1).equals(dbUser.getAccountBanned())) {
            String reason = StrUtil.isNotBlank(dbUser.getAccountBanReason())
                    ? dbUser.getAccountBanReason()
                    : "您的账号因存钱计划、理财日记或个人简介内容违规已被封禁";
            throw new CustomException(ResultCodeEnum.USER_BANNED_ERROR.code, "账号已被封禁：" + reason);
        }
        if (StrUtil.isBlank(dbUser.getRole())) {
            dbUser.setRole(String.valueOf(PermEnum.getRoleByPerm(dbUser.getPerm())));
        }
        String tokenData = dbUser.getId() + "-" + dbUser.getPerm() + "-" + dbUser.getRole();
        String token = TokenUtils.createToken(tokenData, dbUser.getPwd());
        dbUser.setToken(token);
        dbUser.setHasApiKey(StrUtil.isNotBlank(dbUser.getApiKey()));
        dbUser.setApiKey(ApiKeyCryptoUtil.mask(dbUser.getApiKey()));
        return dbUser;
    }

    public void register(Account account) {
        User dbUser = userMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNotNull(dbUser)) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        User user = new User();
        user.setUsername(account.getUsername());
        user.setNickname(StrUtil.isNotBlank(account.getUsername()) ? account.getUsername() : "新用户");
        user.setPwd(account.getPwd());
        user.setPerm(PermEnum.USER.getMinPerm());
        user.setRole(String.valueOf(PermEnum.USER));
        userMapper.insert(user);
    }

    public void updatePwd(Account account) {
        User dbUser = this.selectById(account.getId());
        if (ObjectUtil.isNull(dbUser)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!account.getPwd().equals(dbUser.getPwd())) {
            throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        dbUser.setPwd(account.getNewPwd());
        this.update(dbUser);
    }
}
