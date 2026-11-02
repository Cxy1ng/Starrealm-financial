<template>
  <div class="personal-bento-page">
    <!-- ================= 顶部云母个人名片 Hero 横幅 ================= -->
    <div class="profile-hero-card stagger-1">
      <div class="hero-ambient-glow"></div>
      <div class="hero-main-row">
        <div class="avatar-column">
          <el-upload
              class="hero-avatar-uploader"
              action="http://localhost:8080/files/upload"
              :headers="{ token: data.user.token }"
              :show-file-list="false"
              :on-success="handleQuickAvatarSuccess"
          >
            <div class="avatar-ring-wrap" :class="{ 'ring-super-admin': data.user.perm === 15, 'ring-admin': data.user.perm >= 10 && data.user.perm < 15, 'ring-user': !data.user.perm || data.user.perm < 10 }" title="点击直接更换头像">
              <el-avatar
                  :size="92"
                  :src="data.form.avatar || data.user.avatar || defaultAvatar"
                  class="hero-avatar-img"
              />
              <div class="avatar-hover-mask">
                <el-icon :size="20"><Camera /></el-icon>
                <span>更换</span>
              </div>
            </div>
          </el-upload>
        </div>

        <div class="identity-column">
          <div class="name-badge-row">
            <h2 class="display-name" :class="{ 'name-super-admin': data.user.perm === 15, 'name-admin': data.user.perm >= 10 && data.user.perm < 15 }">{{ data.form.nickname || data.form.username || data.user.username }}</h2>
            <span class="role-pill" :class="{ 'pill-super-admin': data.user.perm === 15, 'pill-admin': data.user.perm >= 10 && data.user.perm < 15, 'pill-user': !data.user.perm || data.user.perm < 10 }">{{ roleLabel }}</span>
            <span class="security-pill" :class="{ 'is-ready': hasStoredKey }">
              <span class="dot"></span>
              {{ hasStoredKey ? 'AI 密钥已加密就绪' : 'AI 密钥待配置' }}
            </span>
          </div>
          <p class="bio-line" v-if="data.user.role === 'USER'">
            {{ data.form.descriptiontext || '保持热爱，理性记账，让每一笔收支都有迹可循。' }}
          </p>
          <p class="bio-line" v-else>
            系统管理与平台审计权限已激活，您可管理全站公共资源与数据审核。
          </p>
        </div>

        <div class="hero-cta-column">
          <el-button type="primary" class="hero-btn" @click="openEditDialog">
            <el-icon style="margin-right: 6px"><EditPen /></el-icon>编辑个人资料
          </el-button>
          <el-button class="hero-btn secondary-btn" @click="openPwdDialog">
            <el-icon style="margin-right: 6px"><Lock /></el-icon>修改登录密码
          </el-button>
        </div>
      </div>
    </div>

    <!-- ================= 下方双栏 Bento 网格：左栏账户档案卡片 + 右栏安全中心与 AI 密钥金库 ================= -->
    <el-row :gutter="20" class="bento-grid-row stagger-2">
      <!-- 左栏：基本资料详情磁贴 -->
      <el-col :xs="24" :lg="14">
        <div class="bento-panel profile-detail-panel">
          <div class="panel-head">
            <div class="panel-title-group">
              <div class="panel-icon-box">
                <el-icon :size="18"><Postcard /></el-icon>
              </div>
              <div>
                <h3 class="panel-title">账户基本资料</h3>
                <span class="panel-sub">您的个人身份信息与联系方式档案</span>
              </div>
            </div>
            <el-button type="primary" link @click="openEditDialog">
              <el-icon style="margin-right: 4px"><EditPen /></el-icon>修改资料
            </el-button>
          </div>

          <div class="info-tiles-grid">
            <div class="info-tile">
              <div class="tile-icon"><el-icon><User /></el-icon></div>
              <div class="tile-body">
                <span class="tile-label">登录用户名</span>
                <span class="tile-value num-font">{{ data.form.username || data.user.username || '-' }}</span>
              </div>
            </div>

            <div class="info-tile">
              <div class="tile-icon"><el-icon><Avatar /></el-icon></div>
              <div class="tile-body">
                <span class="tile-label">显示昵称</span>
                <span class="tile-value">{{ data.form.nickname || '-' }}</span>
              </div>
            </div>

            <div class="info-tile">
              <div class="tile-icon"><el-icon><Iphone /></el-icon></div>
              <div class="tile-body">
                <span class="tile-label">绑定手机</span>
                <span class="tile-value num-font">{{ maskPhone(data.form.phone) }}</span>
              </div>
            </div>

            <div class="info-tile">
              <div class="tile-icon"><el-icon><Message /></el-icon></div>
              <div class="tile-body">
                <span class="tile-label">电子邮箱</span>
                <span class="tile-value num-font">{{ maskEmail(data.form.email) }}</span>
              </div>
            </div>

            <template v-if="data.user.role === 'USER'">
              <div class="info-tile">
                <div class="tile-icon">
                  <el-icon><Female v-if="data.form.sex === '女'" /><Male v-else /></el-icon>
                </div>
                <div class="tile-body">
                  <span class="tile-label">性别</span>
                  <span class="tile-value">{{ data.form.sex || '未设置' }}</span>
                </div>
              </div>

              <div class="info-tile">
                <div class="tile-icon"><el-icon><Calendar /></el-icon></div>
                <div class="tile-body">
                  <span class="tile-label">出生日期</span>
                  <span class="tile-value num-font">{{ data.form.birth || '未设置' }}</span>
                </div>
              </div>

              <!-- 设置了出生日期的用户专属星座小卡片 -->
              <div v-if="zodiacInfo" class="info-tile zodiac-tile">
                <div class="tile-icon zodiac-icon">{{ zodiacInfo.symbol }}</div>
                <div class="tile-body">
                  <span class="tile-label">专属星座 · {{ zodiacInfo.element }}</span>
                  <span class="tile-value">{{ zodiacInfo.name }} ({{ zodiacInfo.range }})</span>
                </div>
              </div>
            </template>
          </div>

          <div v-if="data.user.role === 'USER'" class="bio-showcase-box">
            <div class="bio-box-label">
              <el-icon style="margin-right: 5px; vertical-align: -2px;"><Document /></el-icon>
              个人简介 / 理财格言
            </div>
            <div class="bio-box-content">
              {{ data.form.descriptiontext || '暂未填写个人简介，点击右上角「修改资料」记录您的理财目标吧。' }}
            </div>
          </div>
          <div v-else class="bio-showcase-box">
            <div class="bio-box-label">
              <el-icon style="margin-right: 5px; vertical-align: -2px;"><Document /></el-icon>
              管理员权限说明
            </div>
            <div class="bio-box-content">
              当前账号具备系统管理与全站数据审计权限，可统一调度公共配置、媒体资源及违规处置。
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右栏：安全中心 & DeepSeek 密钥加密金库 -->
      <el-col :xs="24" :lg="10">
        <div class="right-security-stack">
          <!-- 卡片 1：DeepSeek API Key 独立加密金库（不展示任何 sk-xxxx 内容，仅告知是否已加密存过） -->
          <div class="bento-panel vault-panel">
            <div class="panel-head">
              <div class="panel-title-group">
                <div class="panel-icon-box vault-icon">
                  <el-icon :size="18"><Key /></el-icon>
                </div>
                <div>
                  <h3 class="panel-title">DeepSeek API配置</h3>
                  <span class="panel-sub">加密存储，仅供该账户使用</span>
                </div>
              </div>
              <el-tag :type="hasStoredKey ? 'success' : 'warning'" effect="light" round>
                {{ hasStoredKey ? '已加密存储' : '未配置密钥' }}
              </el-tag>
            </div>

            <div class="vault-status-banner" :class="{ 'is-active': hasStoredKey }">
              <div class="vault-shield-icon">
                <el-icon :size="26"><CircleCheckFilled v-if="hasStoredKey" /><WarningFilled v-else /></el-icon>
              </div>
              <div class="vault-status-text">
                <div class="vault-main-line">
                  {{ hasStoredKey ? '已配置个人 API Key' : '未配置个人 API Key' }}
                </div>
                <div class="vault-sub-line">
                  {{ hasStoredKey ? '可在下方输入新密钥进行更新或清除。' : '配置后即可使用智能账单分析与 AI 财务助手。' }}
                </div>
              </div>
            </div>

            <!-- DeepSeek 注册与获取 API Key 引导 -->
            <div class="vault-guide-box">
              <div class="guide-box-content">
                <el-icon class="guide-lead-icon"><Promotion /></el-icon>
                <div class="guide-text">
                  <span>没有 API Key 或未注册账号？前往</span>
                  <a
                    href="https://platform.deepseek.com/"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="deepseek-portal-link"
                  >
                    DeepSeek 开放平台官网
                    <el-icon :size="12" style="margin-left: 2px;"><TopRight /></el-icon>
                  </a>
                  <span>注册并创建专属 API Key。</span>
                </div>
              </div>
            </div>

            <div class="vault-input-area">
              <div class="input-Row">
                <el-input
                    v-model="newApiKeyInput"
                    type="password"
                    show-password
                    clearable
                    autocomplete="new-password"
                    :placeholder="hasStoredKey ? '输入新的 API Key 以覆盖原有加密密钥...' : '请输入您的 DeepSeek API Key (sk-...)'"
                />
              </div>
              <div class="vault-btn-row">
                <el-button
                    type="primary"
                    :loading="savingKey"
                    :disabled="!newApiKeyInput.trim()"
                    @click="saveApiKeyOnly"
                >
                  <el-icon style="margin-right: 5px"><Check /></el-icon>
                  保存密钥
                </el-button>
                <el-button
                    v-if="hasStoredKey"
                    type="danger"
                    plain
                    :loading="savingKey"
                    @click="clearStoredApiKey"
                >
                  <el-icon style="margin-right: 4px"><Delete /></el-icon>清除已存密钥
                </el-button>
              </div>
            </div>
          </div>

          <!-- 卡片 2：网站字体大小设置 -->
          <div class="bento-panel security-card">
            <div class="security-row">
              <div class="panel-title-group">
                <div class="panel-icon-box" style="background: rgba(16, 185, 129, 0.12); color: #10b981;">
                  <el-icon :size="18"><ZoomIn /></el-icon>
                </div>
                <div>
                  <h3 class="panel-title">网站字体大小设置</h3>
                  <span class="panel-sub">实时调整全站界面文字与控件尺寸，所选字号即时预览生效</span>
                </div>
              </div>
              <el-select
                  v-model="currentFontSize"
                  style="width: 168px;"
                  :style="{ fontSize: currentFontSize }"
                  @change="handleFontSizeChange"
              >
                <el-option
                    v-for="opt in fontSizePresets"
                    :key="opt.key"
                    :label="opt.label"
                    :value="opt.key"
                    :style="{ fontSize: opt.size, height: 'auto', lineHeight: '2.1', padding: '4px 16px' }"
                >
                  <span :style="{ fontSize: opt.size, fontWeight: currentFontSize === opt.key ? 700 : 500 }">
                    {{ opt.label }}
                  </span>
                </el-option>
              </el-select>
            </div>
          </div>

          <!-- 卡片 3：账户密码与登录安全（点击直接弹出修改密码弹窗） -->
          <div class="bento-panel security-card">
            <div class="security-row">
              <div class="panel-title-group">
                <div class="panel-icon-box lock-icon">
                  <el-icon :size="18"><Lock /></el-icon>
                </div>
                <div>
                  <h3 class="panel-title">登录密码与凭据安全</h3>
                  <span class="panel-sub">定期更新高强度密码可有效保护您的账务隐私</span>
                </div>
              </div>
              <el-button type="primary" plain @click="openPwdDialog">
                修改密码
              </el-button>
            </div>
          </div>

          <!-- 卡片 4：账户注销危险区域（红色边框统一样式，双击防误触 + 密码校验） -->
          <div class="bento-panel security-card danger-zone-card" v-if="data.user.perm < 15">
            <div class="security-row">
              <div class="panel-title-group">
                <div class="panel-icon-box danger-icon-box">
                  <el-icon :size="18"><DeleteFilled /></el-icon>
                </div>
                <div>
                  <h3 class="panel-title danger-title">账户注销</h3>
                  <span class="panel-sub">注销后您的账单、存钱计划、日记与云端媒体文件将被永久物理删除</span>
                </div>
              </div>
              <el-button
                  type="danger"
                  :plain="deleteClickCount === 0"
                  class="danger-action-btn"
                  @click="handleDeleteAccountClick"
              >
                <el-icon style="margin-right: 4px"><Warning /></el-icon>
                {{ deleteClickCount === 0 ? '注销账户' : '再次点击确认注销' }}
              </el-button>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ================= 弹窗 1：编辑个人基本资料 ================= -->
    <el-dialog
        v-model="data.formVisible"
        title="编辑个人档案资料"
        width="520px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-form
          ref="formRef"
          :rules="data.rules"
          :model="editForm"
          style="padding: 8px 16px 0 12px;"
          label-position="right"
          label-width="96px"
      >
        <el-upload
            class="avatar-uploader"
            action="http://localhost:8080/files/upload"
            :headers="{ token: data.user.token }"
            :show-file-list="false"
            :on-success="handleDialogAvatarSuccess"
        >
          <img v-if="editForm.avatar" :src="editForm.avatar" class="avatar" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="显示昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入您的昵称" clearable />
        </el-form-item>
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱地址" clearable />
        </el-form-item>
        <template v-if="data.user.role === 'USER'">
          <el-form-item label="性别" prop="sex">
            <el-radio-group v-model="editForm.sex">
              <el-radio value="男">
                <el-icon style="padding-right: 4px"><Male /></el-icon>男
              </el-radio>
              <el-radio value="女">
                <el-icon style="padding-right: 4px"><Female /></el-icon>女
              </el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="出生日期" prop="birth">
            <el-date-picker
                style="width: 100%"
                v-model="editForm.birth"
                type="date"
                placeholder="请选择出生日期"
                format="YYYY/MM/DD"
                value-format="YYYY-MM-DD"
                clearable
            />
          </el-form-item>
          <el-form-item label="个人简介" prop="descriptiontext">
            <el-input
                v-model="editForm.descriptiontext"
                placeholder="写一句理财格言或个人简介吧"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 4 }"
                maxlength="200"
                show-word-limit
            />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="data.formVisible = false">取消</el-button>
        <el-button type="primary" @click="updateUserProfile">保存资料</el-button>
      </template>
    </el-dialog>

    <!-- ================= 弹窗 2：修改登录密码（整合在个人中心内部，无需独立子页面） ================= -->
    <el-dialog
        v-model="pwdDialogVisible"
        title="修改账户登录密码"
        width="460px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <div class="pwd-dialog-tip">
        <el-icon><Lock /></el-icon>
        <span>修改密码成功后，当前登录会话将失效并跳转至重新登录。</span>
      </div>
      <el-form
          ref="pwdFormRef"
          :model="pwdForm"
          :rules="pwdRules"
          label-position="top"
          style="padding: 4px 8px 0 8px"
      >
        <el-form-item label="当前旧密码" prop="pwd">
          <el-input
              v-model="pwdForm.pwd"
              type="password"
              show-password
              placeholder="请输入当前使用的旧密码"
              autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item label="设置新密码" prop="newPwd">
          <el-input
              v-model="pwdForm.newPwd"
              type="password"
              show-password
              placeholder="请输入新密码"
              autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPwd">
          <el-input
              v-model="pwdForm.confirmPwd"
              type="password"
              show-password
              placeholder="请再次输入新密码"
              autocomplete="new-password"
              @keydown.enter.prevent="submitPwdChange"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingPwd" @click="submitPwdChange">
          确认修改密码
        </el-button>
      </template>
    </el-dialog>

    <!-- ================= 弹窗 3：注销账户安全验证弹窗 ================= -->
    <el-dialog
        v-model="deleteAccountDialogVisible"
        title="安全确认 · 注销账户"
        width="440px"
        align-center
        destroy-on-close
    >
      <div style="margin-bottom: 14px;">
        <el-alert
            title="此操作不可逆！注销后您的所有财务记账数据、日记、存钱计划与头像图片均会被永久清除。"
            type="error"
            :closable="false"
            show-icon
        />
      </div>
      <el-form label-position="top">
        <el-form-item label="请输入当前登录密码进行验证">
          <el-input
              v-model="deleteAccountPwd"
              type="password"
              placeholder="请输入当前密码以确认注销"
              show-password
              autocomplete="current-password"
              @keydown.enter.prevent="submitDeleteAccount"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 10px;">
          <el-button @click="deleteAccountDialogVisible = false">取消</el-button>
          <el-button
              type="danger"
              :loading="submittingDeleteAccount"
              :disabled="!deleteAccountPwd.trim()"
              @click="submitDeleteAccount"
          >
            确认永久注销
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {
  Avatar,
  Calendar,
  Camera,
  Check,
  CircleCheckFilled,
  DeleteFilled,
  Warning,
  Delete,
  EditPen,
  Female,
  Iphone,
  Key,
  Lock,
  Male,
  Message,
  Plus,
  Document,
  Postcard,
  Promotion,
  TopRight,
  User,
  WarningFilled,
  ZoomIn
} from "@element-plus/icons-vue";
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import defaultAvatar from "@/assets/avatar.jpg";
import { useThemePalette } from "@/utils/theme.js";

import { validatePasswordRule, validatePhoneRule, validateEmailRule } from "@/utils/validator.js";
import { clearLoginSessionCookie } from "@/utils/cookie.js";

const route = useRoute();
const router = useRouter();
const emit = defineEmits(["updateUser"]);
const { fontSizePresets, activeFontSize: currentFontSize, applyGlobalFontSize } = useThemePalette();

const handleFontSizeChange = (val) => {
  applyGlobalFontSize(val);
  ElMessage.success(`网站字号已切换为 ${val}`);
};

const data = reactive({
  user: JSON.parse(localStorage.getItem("loginToken") || "{}"),
  form: {},
  formVisible: false,
  rules: {
    username: [{ required: true, message: "请输入有效的用户名", trigger: "blur" }],
    phone: [{ validator: validatePhoneRule, trigger: "blur" }],
    email: [{ validator: validateEmailRule, trigger: "blur" }]
  }
});

const editForm = reactive({});
const formRef = ref();
const hasStoredKey = ref(false);
const newApiKeyInput = ref("");
const savingKey = ref(false);

const roleLabel = computed(() => {
  if (data.user.perm === 15) return "超级管理员";
  if (data.user.perm >= 10) return "系统管理员";
  return "个人理财账户";
});

// 手机号与邮箱隐私脱敏展示
const maskPhone = (phone) => {
  if (!phone) return "未绑定手机号";
  const str = String(phone).trim();
  if (str.length >= 7) {
    return str.slice(0, 3) + "****" + str.slice(-4);
  }
  return str;
};

const maskEmail = (email) => {
  if (!email) return "未绑定邮箱";
  const str = String(email).trim();
  const atIdx = str.indexOf("@");
  if (atIdx <= 0) return str;
  const local = str.slice(0, atIdx);
  const domain = str.slice(atIdx);
  if (local.length <= 2) {
    return local.slice(0, 1) + "***" + domain;
  }
  return local.slice(0, 2) + "****" + local.slice(-1) + domain;
};

// 根据出生日期计算星座信息
const zodiacInfo = computed(() => {
  const birth = data.form.birth;
  if (!birth) return null;
  const parts = String(birth).split(/[-/]/);
  if (parts.length < 3) return null;
  const m = parseInt(parts[1], 10);
  const d = parseInt(parts[2], 10);
  if (!m || !d) return null;
  const md = m * 100 + d;

  const list = [
    { name: "摩羯座", symbol: "♑", range: "12.22-01.19", element: "土象星座", test: (v) => v >= 1222 || v <= 119 },
    { name: "水瓶座", symbol: "♒", range: "01.20-02.18", element: "风象星座", test: (v) => v >= 120 && v <= 218 },
    { name: "双鱼座", symbol: "♓", range: "02.19-03.20", element: "水象星座", test: (v) => v >= 219 && v <= 320 },
    { name: "白羊座", symbol: "♈", range: "03.21-04.19", element: "火象星座", test: (v) => v >= 321 && v <= 419 },
    { name: "金牛座", symbol: "♉", range: "04.20-05.20", element: "土象星座", test: (v) => v >= 420 && v <= 520 },
    { name: "双子座", symbol: "♊", range: "05.21-06.21", element: "风象星座", test: (v) => v >= 521 && v <= 621 },
    { name: "巨蟹座", symbol: "♋", range: "06.22-07.22", element: "水象星座", test: (v) => v >= 622 && v <= 722 },
    { name: "狮子座", symbol: "♌", range: "07.23-08.22", element: "火象星座", test: (v) => v >= 723 && v <= 822 },
    { name: "处女座", symbol: "♍", range: "08.23-09.22", element: "土象星座", test: (v) => v >= 823 && v <= 922 },
    { name: "天秤座", symbol: "♎", range: "09.23-10.23", element: "风象星座", test: (v) => v >= 923 && v <= 1023 },
    { name: "天蝎座", symbol: "♏", range: "10.24-11.22", element: "水象星座", test: (v) => v >= 1024 && v <= 1122 },
    { name: "射手座", symbol: "♐", range: "11.23-12.21", element: "火象星座", test: (v) => v >= 1123 && v <= 1221 }
  ];
  return list.find((item) => item.test(md)) || null;
});

const getUpdateEndpoint = () => {
  if (data.user.role === "USER") return "/user/update";
  if (data.user.perm >= 10) return "/admin/update";
  return null;
};

const syncLocalStorageUser = (patch = {}) => {
  const mergedData = {
    ...data.user,
    ...data.form,
    ...patch,
    token: data.user.token,
    role: data.user.role,
    hasApiKey: hasStoredKey.value
  };
  delete mergedData.apiKey;
  localStorage.setItem("loginToken", JSON.stringify(mergedData));
  data.user = mergedData;
  emit("updateUser");
};

const loadProfile = () => {
  const url =
    data.user.role === "USER"
      ? "/user/selectById/" + data.user.id
      : data.user.perm >= 10
      ? "/admin/selectById/" + data.user.id
      : null;
  if (!url) return;

  request.get(url).then((res) => {
    if (res.code === 200 && res.data) {
      hasStoredKey.value = !!(res.data.hasApiKey || res.data.apiKey);
      // 不保留任何 sk-xxxx 掩码字符串
      res.data.apiKey = "";
      data.form = res.data;
    }
  });
};

const openEditDialog = () => {
  Object.assign(editForm, JSON.parse(JSON.stringify(data.form)));
  editForm.apiKey = "";
  data.formVisible = true;
};

const handleQuickAvatarSuccess = (res) => {
  if (!res?.data) return;
  const api = getUpdateEndpoint();
  if (!api) return;
  const payload = { ...data.form, avatar: res.data, apiKey: null };
  request.put(api, payload).then((r) => {
    if (r.code === 200) {
      data.form.avatar = res.data;
      syncLocalStorageUser({ avatar: res.data });
      ElMessage.success("头像已更新");
    }
  });
};

const handleDialogAvatarSuccess = (res) => {
  if (res?.data) {
    editForm.avatar = res.data;
  }
};

const updateUserProfile = () => {
  formRef.value?.validate((valid) => {
    if (!valid) return;
    const api = getUpdateEndpoint();
    if (!api) return;
    const payload = { ...editForm, apiKey: null };
    request.put(api, payload).then((res) => {
      if (res.code === 200) {
        ElMessage.success("个人资料已保存");
        loadProfile();
        syncLocalStorageUser(editForm);
        data.formVisible = false;
      } else {
        ElMessage.error(res.msg || "保存失败");
      }
    });
  });
};

// ==================== 独立保存或清除 DeepSeek API Key ====================
const saveApiKeyOnly = async () => {
  const trimmed = newApiKeyInput.value.trim();
  if (!trimmed) return;
  const api = getUpdateEndpoint();
  if (!api) return;

  savingKey.value = true;
  try {
    const payload = {
      id: data.user.id,
      perm: data.user.perm,
      username: data.form.username || data.user.username,
      apiKey: trimmed
    };
    const res = await request.put(api, payload);
    if (res.code === 200) {
      hasStoredKey.value = true;
      newApiKeyInput.value = "";
      syncLocalStorageUser();
      ElMessage.success("API 密钥已保存");
    } else {
      ElMessage.error(res.msg || "密钥保存失败");
    }
  } finally {
    savingKey.value = false;
  }
};

const clearStoredApiKey = () => {
  ElMessageBox.confirm(
    "确定要清除当前账户已加密保存的 DeepSeek API 密钥吗？",
    "清除密钥确认",
    {
      confirmButtonText: "确认清除",
      cancelButtonText: "取消",
      type: "warning"
    }
  )
    .then(async () => {
      const api = getUpdateEndpoint();
      if (!api) return;
      savingKey.value = true;
      try {
        const payload = {
          id: data.user.id,
          perm: data.user.perm,
          username: data.form.username || data.user.username,
          apiKey: "__CLEAR__"
        };
        const res = await request.put(api, payload);
        if (res.code === 200) {
          hasStoredKey.value = false;
          newApiKeyInput.value = "";
          syncLocalStorageUser();
          ElMessage.success("已清除存储的 API 密钥");
        } else {
          ElMessage.error(res.msg || "操作失败");
        }
      } finally {
        savingKey.value = false;
      }
    })
    .catch(() => {});
};

// ==================== 修改登录密码弹窗逻辑 ====================
const deleteClickCount = ref(0);
let deleteClickTimer = null;
const deleteAccountDialogVisible = ref(false);
const deleteAccountPwd = ref('');
const submittingDeleteAccount = ref(false);

const handleDeleteAccountClick = () => {
  if (deleteClickCount.value === 0) {
    deleteClickCount.value = 1;
    ElMessage.warning('危险操作：请再次点击按钮以确认打开注销验证');
    clearTimeout(deleteClickTimer);
    deleteClickTimer = setTimeout(() => {
      deleteClickCount.value = 0;
    }, 4000);
  } else {
    clearTimeout(deleteClickTimer);
    deleteClickCount.value = 0;
    deleteAccountPwd.value = '';
    deleteAccountDialogVisible.value = true;
  }
};

const submitDeleteAccount = async () => {
  if (!deleteAccountPwd.value.trim()) {
    ElMessage.warning('请输入密码以完成身份验证');
    return;
  }
  submittingDeleteAccount.value = true;
  try {
    const res = await request.post('/user/deregister', {
      pwd: deleteAccountPwd.value.trim()
    });
    submittingDeleteAccount.value = false;
    if (res.code === 200) {
      ElMessage.success('账户已成功注销，关联数据与图片已清除');
      deleteAccountDialogVisible.value = false;
      localStorage.removeItem('loginToken');
      clearLoginSessionCookie();
      router.replace('/login');
    } else {
      ElMessage.error(res.msg || '注销失败，请核对密码是否正确');
    }
  } catch (err) {
    submittingDeleteAccount.value = false;
    ElMessage.error(err?.response?.data?.msg || '网络异常，注销失败');
  }
};

const pwdDialogVisible = ref(false);
const submittingPwd = ref(false);
const pwdFormRef = ref();
const pwdForm = reactive({
  pwd: "",
  newPwd: "",
  confirmPwd: ""
});

const validateConfirmPwd = (rule, value, callback) => {
  if (!value) {
    callback(new Error("请再次确认新密码"));
  } else if (value !== pwdForm.newPwd) {
    callback(new Error("两次输入的新密码不一致"));
  } else {
    callback();
  }
};

const pwdRules = {
  pwd: [{ required: true, message: "请输入当前旧密码", trigger: "blur" }],
  newPwd: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { validator: validatePasswordRule, trigger: "blur" }
  ],
  confirmPwd: [{ validator: validateConfirmPwd, trigger: "blur" }]
};

const openPwdDialog = () => {
  pwdForm.pwd = "";
  pwdForm.newPwd = "";
  pwdForm.confirmPwd = "";
  pwdDialogVisible.value = true;
};

const submitPwdChange = () => {
  pwdFormRef.value?.validate((valid) => {
    if (!valid) return;
    submittingPwd.value = true;
    const payload = {
      id: data.user.id,
      perm: data.user.perm,
      role: data.user.role,
      username: data.user.username,
      pwd: pwdForm.pwd,
      newPwd: pwdForm.newPwd
    };
    request
      .put("/updatePwd", payload)
      .then((res) => {
        if (res.code === 200) {
          ElMessage.success("密码修改成功，请重新登录");
          pwdDialogVisible.value = false;
          localStorage.removeItem("loginToken");
          clearLoginSessionCookie();
          request.post('/logout').catch(() => {});
          router.replace('/login');
        } else {
          ElMessage.error(res.msg || "密码修改失败");
        }
      })
      .finally(() => {
        submittingPwd.value = false;
      });
  });
};

watch(
  () => route.query.action,
  (act) => {
    if (act === "pwd") {
      openPwdDialog();
      router.replace({ path: "/main/stats" });
    }
  },
  { immediate: true }
);

onMounted(() => {
  applyGlobalFontSize(currentFontSize.value);
  loadProfile();
});
</script>

<style scoped>
.personal-bento-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1220px;
  margin: 0 auto;
  padding-bottom: 36px;
}

/* ================= 顶部云母个人名片 Hero 横幅 ================= */
.profile-hero-card {
  position: relative;
  overflow: hidden;
  padding: 26px 30px;
  border-radius: 20px;
  background: var(--mica-bg-light);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(var(--theme-rgb), 0.22);
  box-shadow: var(--mica-shadow-light);
}

html.dark .profile-hero-card {
  background: var(--mica-bg-dark);
  border-color: rgba(255, 255, 255, 0.09);
  box-shadow: var(--mica-shadow-dark);
}

.hero-ambient-glow {
  position: absolute;
  top: -60%;
  right: -8%;
  width: 380px;
  height: 380px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(var(--theme-rgb), 0.18) 0%, transparent 70%);
  pointer-events: none;
}

.hero-main-row {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.avatar-ring-wrap {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  padding: 2px;
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary));
  box-shadow: 0 10px 25px -6px rgba(var(--theme-rgb), 0.4);
  cursor: pointer;
  overflow: hidden;
}

.hero-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid var(--el-bg-color);
}

.avatar-hover-mask {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.58);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 11px;
  opacity: 0;
  transition: opacity 0.22s ease;
}

.avatar-ring-wrap:hover .avatar-hover-mask {
  opacity: 1;
}

.identity-column {
  flex: 1;
  min-width: 240px;
}

.name-badge-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.display-name {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--el-text-color-primary);
}

.role-pill {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(var(--theme-rgb), 0.12);
  color: var(--el-color-primary);
  border: 1px solid rgba(var(--theme-rgb), 0.24);
}

.security-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(245, 158, 11, 0.12);
  color: #f59e0b;
}

.security-pill .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.security-pill.is-ready {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}

.bio-line {
  margin: 0 0 12px 0;
  font-size: 13.5px;
  color: var(--el-text-color-secondary);
  line-height: 1.55;
}

.quick-meta-chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  padding: 4px 11px;
  border-radius: 8px;
  background: rgba(148, 163, 184, 0.1);
  color: var(--el-text-color-regular);
}

.hero-cta-column {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hero-btn {
  min-width: 148px;
  height: 38px;
  border-radius: 10px !important;
  font-weight: 600;
  margin: 0 !important;
}

/* ================= 下方 Bento 双栏面板 ================= */
.bento-panel {
  padding: 22px 24px;
  border-radius: 18px;
  background: var(--table-row-bg, #f6f9fe);
  border: 1px solid var(--table-border-color, #d3e1f4);
  box-shadow: var(--mica-shadow-light);
}

html.dark .bento-panel {
  background: var(--table-row-bg, #0c1322);
  border-color: var(--table-border-color, #1e2c46);
  box-shadow: var(--mica-shadow-dark);
}

.profile-detail-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.panel-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.panel-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: rgba(var(--theme-rgb), 0.12);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.panel-title {
  margin: 0 0 2px 0;
  font-size: 16px;
  font-weight: 700;
}

.panel-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.info-tiles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.info-tile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--table-row-alt-bg, #eef4fc);
  border: 1px solid var(--table-border-color, #d3e1f4);
  transition: all 0.22s ease;
}

html.dark .info-tile {
  background: var(--table-row-alt-bg, #101829);
  border-color: var(--table-border-color, #1e2c46);
}

.info-tile:hover {
  border-color: rgba(var(--theme-rgb), 0.32);
  background: rgba(var(--theme-rgb), 0.05);
  transform: translateY(-2px);
}

.tile-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(var(--theme-rgb), 0.1);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  flex-shrink: 0;
}

.zodiac-tile {
  background: linear-gradient(135deg, rgba(var(--theme-rgb), 0.12), rgba(var(--theme-rgb), 0.04));
  border-color: rgba(var(--theme-rgb), 0.28);
}

.zodiac-icon {
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary));
  color: #ffffff;
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(var(--theme-rgb), 0.28);
}

.tile-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.tile-label {
  font-size: 11.5px;
  color: var(--el-text-color-secondary);
}

.tile-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bio-showcase-box {
  margin-top: 0;
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--table-row-alt-bg, #eef4fc);
  border: 1px dashed rgba(var(--theme-rgb), 0.35);
  transition: all 0.22s ease;
}

html.dark .bio-showcase-box {
  background: var(--table-row-alt-bg, #101829);
  border-color: rgba(var(--theme-rgb), 0.35);
}

.vault-guide-box {
  margin-bottom: 14px;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(var(--theme-rgb), 0.06);
  border: 1px solid rgba(var(--theme-rgb), 0.18);
  font-size: 12.5px;
  color: var(--el-text-color-regular);
}

.guide-box-content {
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1.5;
  flex-wrap: wrap;
}

.guide-lead-icon {
  color: var(--el-color-primary);
  font-size: 15px;
  flex-shrink: 0;
}

.guide-text {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.deepseek-portal-link {
  display: inline-flex;
  align-items: center;
  color: var(--el-color-primary);
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 3px;
  transition: opacity 0.2s ease;
}

.deepseek-portal-link:hover {
  opacity: 0.8;
}

.bio-box-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-primary);
  margin-bottom: 6px;
}

.bio-box-content {
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--el-text-color-regular);
}

/* ================= 右栏：密钥金库与密码安全 ================= */
.right-security-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
}

.vault-status-banner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(245, 158, 11, 0.09);
  border: 1px solid rgba(245, 158, 11, 0.28);
  margin-bottom: 16px;
}

.vault-status-banner.is-active {
  background: rgba(16, 185, 129, 0.09);
  border-color: rgba(16, 185, 129, 0.28);
}

.vault-shield-icon {
  color: #f59e0b;
  margin-top: 2px;
}

.vault-status-banner.is-active .vault-shield-icon {
  color: #10b981;
}

.vault-main-line {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.vault-sub-line {
  font-size: 12px;
  line-height: 1.55;
  color: var(--el-text-color-secondary);
}

.vault-input-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.vault-btn-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.security-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.pwd-dialog-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 14px;
  border-radius: 10px;
  background: rgba(var(--theme-rgb), 0.08);
  color: var(--el-color-primary);
  font-size: 12.5px;
}

.avatar-uploader {
  width: 100%;
  display: flex;
  justify-content: center;
  margin-bottom: 18px;
}

.avatar-uploader .avatar {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}
</style>

<style>
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 24px;
  color: #8c939d;
  width: 92px;
  height: 92px;
  text-align: center;
}

.danger-zone-card {
  border: 1px solid rgba(239, 68, 68, 0.38) !important;
  background: rgba(239, 68, 68, 0.035) !important;
  transition: all 0.25s ease;
}

.danger-zone-card:hover {
  border-color: rgba(239, 68, 68, 0.72) !important;
  box-shadow: 0 4px 18px rgba(239, 68, 68, 0.1) !important;
}

.danger-icon-box {
  background: rgba(239, 68, 68, 0.12) !important;
  color: #ef4444 !important;
}

.danger-title {
  color: #ef4444 !important;
}
</style>