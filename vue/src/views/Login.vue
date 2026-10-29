<template>
  <div class="auth-container">
    <video class="loginbg" autoplay loop muted playsinline disablePictureInPicture controlsList="nodownload">
      <source src="@/assets/loginbg1080.mp4" type="video/mp4">
    </video>
    <div class="bg-overlay"></div>
    <ParticleBackground :isDark="isDark" :particleCount="55" />

    <div class="auth-shell">
      <div class="brand-showcase stagger-1">
        <div class="brand-pill">
          <span class="pulse-dot"></span>
          <span>AI-Driven Personal Finance Platform</span>
        </div>
        <h1 class="brand-headline">星界</h1>
        <p class="brand-desc">
          融合 DeepSeek 智能账单诊断、可视化收支看板与目标储蓄规划，让每一笔收支清晰可控。
        </p>
        <div class="feature-badges">
          <div class="feature-item"><el-icon><Cpu /></el-icon><span>DeepSeek 智能财务顾问</span></div>
          <div class="feature-item"><el-icon><PieChart /></el-icon><span>实时动态收支图表</span></div>
          <div class="feature-item"><el-icon><Lock /></el-icon><span>AES-128 密钥安全加密</span></div>
        </div>
      </div>

      <div class="mica-auth-card stagger-2">
        <!-- 浮窗右上角纸张卷边（Page Curl / Dog-ear）明暗主题切换按钮 -->
        <button
            type="button"
            class="page-curl-theme-btn"
            :title="isDark ? '切换至浅色模式' : '切换至暗夜模式'"
            @click="handleSpotlightThemeToggle($event)"
        >
          <span class="curl-peek-sky">
            <el-icon v-if="isDark" class="curl-icon moon"><Moon /></el-icon>
            <el-icon v-else class="curl-icon sun"><Sunny /></el-icon>
          </span>
          <span class="curl-flap"></span>
        </button>

        <div class="card-top">
          <div class="brand-logo-badge">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" class="card-logo xingjie-logo-svg" aria-label="星界 Logo">
              <path fill="currentColor" d="M 83 6 L 94 6 L 94 25.5 L 100 22 L 106 25.5 L 106 6 L 117 6 L 117 30 L 166 58 L 166 86 L 138 86 L 138 72 L 100 50 L 68.4 68.3 L 88 79.1 L 83.6 87.1 L 89.2 90.2 L 86 96 L 80.4 92.9 L 75.9 100.9 L 34 77.7 L 34 67.8 L 76.2 91.2 L 79.9 84.5 L 34 59.1 L 34 58 L 83 30 Z" />
              <path fill="currentColor" d="M 117 194 L 106 194 L 106 174.5 L 100 178 L 94 174.5 L 94 194 L 83 194 L 83 170 L 34 142 L 34 114 L 62 114 L 62 128 L 100 150 L 131.6 131.7 L 112 120.9 L 116.4 112.9 L 110.8 109.8 L 114 104 L 119.6 107.1 L 124.1 99.1 L 166 122.3 L 166 132.2 L 123.8 108.8 L 120.1 115.5 L 166 140.9 L 166 142 L 117 170 Z" />
              <g transform="translate(100, 100) rotate(29)">
                <path fill="currentColor" fill-rule="evenodd" d="M -7.4 -15.4 L 7.4 -15.4 L 15.4 -7.4 L 15.4 7.4 L 7.4 15.4 L -7.4 15.4 L -15.4 7.4 L -15.4 -7.4 Z M 0 -8.7 A 8.7 8.7 0 1 1 0 8.7 A 8.7 8.7 0 1 1 0 -8.7 Z" />
              </g>
            </svg>
          </div>
          <div class="card-title-group">
            <div class="card-title">欢迎回来</div>
            <div class="card-subtitle">请登录您的星界账户</div>
          </div>

          <!-- 标题栏右侧 T 恤图标主题配色切换按钮 -->
          <el-popover
              placement="bottom-end"
              :width="242"
              trigger="click"
              popper-class="palette-popover"
          >
            <template #reference>
              <button type="button" class="tshirt-theme-btn" title="切换主题配色">
                <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20.38 3.46 16 2a4 4 0 0 1-8 0L3.62 3.46a2 2 0 0 0-1.34 2.23l.58 3.47a1 1 0 0 0 .99.84H6v10c0 1.1.9 2 2 2h8a2 2 0 0 0 2-2V10h2.15a1 1 0 0 0 .99-.84l.58-3.47a2 2 0 0 0-1.34-2.23z"/>
                </svg>
                <span class="tshirt-color-dot"></span>
              </button>
            </template>

            <div class="auth-palette-panel">
              <div class="auth-palette-head">
                <span class="auth-palette-title">主题色与粒子光效</span>
                <span class="auth-palette-sub">随心配</span>
              </div>
              <div class="auth-palette-list">
                <div
                    v-for="item in presets"
                    :key="item.key"
                    class="auth-palette-option"
                    :class="{ active: activeThemeKey === item.key }"
                    @click="selectAccentTheme(item.key, $event)"
                >
                  <span class="auth-swatch-circle" :style="{ background: `linear-gradient(135deg, ${item.primary}, ${item.secondary})` }"></span>
                  <div class="auth-swatch-meta">
                    <span class="auth-swatch-name">{{ item.name }}</span>
                    <span class="auth-swatch-desc">{{ item.desc }}</span>
                  </div>
                  <el-icon v-if="activeThemeKey === item.key" class="auth-swatch-check"><Check /></el-icon>
                </div>
              </div>
            </div>
          </el-popover>
        </div>

        <el-form ref="formRef" :rules="data.rules" :model="data.form" size="large" @keyup.enter="login">
          <el-form-item prop="username">
            <el-input v-model="data.form.username" placeholder="请输入用户名" prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="pwd">
            <el-input type="password" v-model="data.form.pwd" placeholder="请输入登录密码" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item prop="perm" class="role-form-item">
            <div class="role-segmented">
              <div
                  class="role-option"
                  :class="{ active: String(data.form.perm) === '1' }"
                  @click="data.form.perm = '1'"
              >
                个人用户
              </div>
              <div
                  class="role-option"
                  :class="{ active: String(data.form.perm) === '10' }"
                  @click="data.form.perm = '10'"
              >
                系统管理员
              </div>
            </div>
          </el-form-item>

          <el-button @click="login" :loading="loading" size="large" type="primary" class="submit-btn">
            立即登录
          </el-button>

          <div class="auth-footer">
            还没有账户？
            <router-link to="/register" class="auth-link">免费注册新账号 →</router-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, nextTick } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Cpu, PieChart, Lock, Check, Sunny, Moon } from "@element-plus/icons-vue";
import { useDark } from "@vueuse/core";
import { request } from "@/utils/request.js";
import ParticleBackground from "@/components/ParticleBackground.vue";
import { useThemePalette } from "@/utils/theme.js";

const router = useRouter();
const loading = ref(false);
const formRef = ref();
const isDark = useDark();
const { presets, activeThemeKey, applyThemePalette } = useThemePalette();

const runSpotlightTransition = (event, stateMutator) => {
  if (!document.startViewTransition) {
    stateMutator();
    return;
  }
  const transition = document.startViewTransition(async () => {
    stateMutator();
    await nextTick();
  });
  transition.ready.catch(() => {});
};

const handleSpotlightThemeToggle = (event) => {
  runSpotlightTransition(event, () => {
    isDark.value = !isDark.value;
    applyThemePalette(activeThemeKey.value, isDark.value);
  });
};

const selectAccentTheme = (themeKey, event) => {
  if (themeKey === activeThemeKey.value) return;
  runSpotlightTransition(event, () => {
    applyThemePalette(themeKey, isDark.value);
  });
};

const data = reactive({
  form: { perm: '1' },
  rules: {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' }
    ],
    pwd: [
      { required: true, message: '请输入密码', trigger: 'blur' }
    ]
  }
});

const login = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      loading.value = true;
      request.post('/login', data.form).then(res => {
        loading.value = false;
        if (res.code === 200) {
          localStorage.setItem('loginToken', JSON.stringify(res.data));
          ElMessage.success('登录成功，欢迎回来！');
          router.push('/main/home');
        } else {
          ElMessage.error(res.msg);
        }
      }).catch(() => {
        loading.value = false;
      });
    }
  });
};
</script>

<style scoped>
.auth-container {
  position: relative;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loginbg {
  object-fit: cover;
  position: fixed;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: -2;
}

.bg-overlay {
  position: fixed;
  inset: 0;
  background: radial-gradient(circle at 20% 50%, rgba(15, 23, 42, 0.55), rgba(15, 23, 42, 0.78));
  backdrop-filter: blur(4px);
  z-index: -1;
}

.auth-shell {
  position: relative;
  z-index: 5;
  width: min(1080px, 92vw);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 48px;
}

.brand-showcase {
  flex: 1;
  color: #ffffff;
  max-width: 500px;
}

.brand-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
  margin-bottom: 20px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--theme-primary, #38bdf8);
  box-shadow: 0 0 10px var(--theme-primary, #38bdf8);
}

.brand-headline {
  font-size: 46px;
  font-weight: 800;
  margin: 0 0 16px 0;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #ffffff 30%, var(--theme-primary) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.brand-desc {
  font-size: 15px;
  line-height: 1.75;
  color: rgba(241, 245, 249, 0.85);
  margin-bottom: 28px;
}

.feature-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.feature-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.14);
  font-size: 13px;
  color: #e2e8f0;
}

.mica-auth-card {
  position: relative;
  overflow: hidden;
  width: 400px;
  padding: 40px 36px;
  border-radius: 22px;
  background: var(--table-row-bg, #f6f9fe);
  border: 1px solid var(--table-border-color, #d3e1f4);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
  transition: background-color 0.28s ease, border-color 0.28s ease, box-shadow 0.28s ease;
}

html.dark .mica-auth-card {
  background: var(--table-row-bg, #0c1322);
  border-color: var(--table-border-color, #1e2c46);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.58);
}

/* ================= 浮窗右上角：精巧版纸张卷边（Page Curl / Dog-ear）明暗主题切换器 ================= */
.page-curl-theme-btn {
  position: absolute;
  top: 0;
  right: 0;
  width: 38px;
  height: 38px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  z-index: 15;
  outline: none;
}

/* 被掀起的一角背后露出的昼/夜底色三角区（面积适中、不干扰标题） */
.curl-peek-sky {
  position: absolute;
  inset: 0;
  clip-path: polygon(0 0, 100% 0, 100% 100%);
  background: linear-gradient(225deg, #1e293b 0%, #0f172a 70%);
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 5px 5px 0 0;
  transition: transform 0.15s ease;
}

html.dark .curl-peek-sky {
  background: linear-gradient(225deg, #fef3c7 0%, #fde68a 75%);
}

.curl-icon {
  font-size: 13px;
  transition: transform 0.15s ease;
}

.curl-icon.sun {
  color: #0f172a;
}

.curl-icon.moon {
  color: #d97706;
}

/* 卷边折页（缩小面积：只覆盖右上角约 60% 区域，折起动作明显但整体更精致小巧） */
.curl-flap {
  position: absolute;
  inset: 0;
  clip-path: polygon(0 0, 0 42%, 42% 0);
  background: linear-gradient(
    135deg,
    var(--table-head-bg, #e4eefb) 0%,
    #ffffff 65%,
    rgba(var(--theme-rgb), 0.25) 100%
  );
  border-bottom-left-radius: 8px;
  box-shadow: -2px 2px 6px rgba(15, 23, 42, 0.15);
  transform-origin: top right;
  transition: transform 0.2s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.2s ease;
}

html.dark .curl-flap {
  background: linear-gradient(
    135deg,
    var(--table-hover-bg, #18243c) 0%,
    #334155 65%,
    rgba(var(--theme-rgb), 0.4) 100%
  );
  box-shadow: -2px 2px 8px rgba(0, 0, 0, 0.5);
}

.page-curl-theme-btn:hover .curl-flap {
  transform: scale(1.32) translate(-3px, 3px);
  box-shadow: -4px 4px 12px rgba(15, 23, 42, 0.28);
}

.page-curl-theme-btn:hover .curl-icon {
  transform: scale(1.1);
}

.card-top {
  display: flex;
  align-items: center;
  gap: 13px;
  margin-bottom: 26px;
  padding-right: 16px;
}

.brand-logo-badge {
  width: 46px;
  height: 46px;
  border-radius: 13px;
  background: rgba(var(--theme-rgb), 0.1);
  border: 1px solid rgba(var(--theme-rgb), 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-logo {
  width: 30px;
  height: 30px;
}

.xingjie-logo-svg {
  color: #111827;
  transition: color 0.28s ease;
}

html.dark .xingjie-logo-svg {
  color: #f8fafc;
}

.card-title-group {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-size: 21px;
  font-weight: 700;
  color: var(--el-text-color-primary, #0f172a);
}

.card-subtitle {
  font-size: 12.5px;
  color: var(--el-text-color-secondary, #64748b);
  margin-top: 2px;
}

/* 标题栏右侧 T 恤主题色切换小按钮 */
.tshirt-theme-btn {
  position: relative;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  border: 1px solid var(--table-border-color, #d3e1f4);
  background: var(--table-row-alt-bg, #eef4fc);
  color: var(--el-color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.22s ease;
}

.tshirt-theme-btn:hover {
  transform: translateY(-1.5px);
  border-color: var(--el-color-primary);
  background: var(--table-hover-bg, #dbe8fa);
  box-shadow: 0 4px 12px rgba(var(--theme-rgb), 0.22);
}

.tshirt-color-dot {
  position: absolute;
  right: 4px;
  bottom: 4px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--theme-primary);
  border: 1.5px solid var(--table-row-bg, #ffffff);
}

/* 弹出配色面板样式 */
.auth-palette-panel {
  padding: 2px;
}

.auth-palette-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  margin-bottom: 8px;
  border-bottom: 1px solid var(--table-border-color, #d3e1f4);
}

.auth-palette-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.auth-palette-sub {
  font-size: 11px;
  color: var(--el-color-primary);
  background: rgba(var(--theme-rgb), 0.12);
  padding: 1px 6px;
  border-radius: 999px;
  font-weight: 600;
}

.auth-palette-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.auth-palette-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 10px;
  border-radius: 10px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

.auth-palette-option:hover {
  background: var(--table-hover-bg, rgba(var(--theme-rgb), 0.08));
}

.auth-palette-option.active {
  background: rgba(var(--theme-rgb), 0.12);
  border-color: rgba(var(--theme-rgb), 0.3);
}

.auth-swatch-circle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  flex-shrink: 0;
}

.auth-swatch-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.auth-swatch-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.auth-swatch-desc {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.auth-swatch-check {
  color: var(--el-color-primary);
  font-weight: bold;
}

/* 优化后的紧凑型「个人用户 / 系统管理员」切换分段器 */
.role-form-item {
  margin-bottom: 18px !important;
}

.role-form-item :deep(.el-form-item__content) {
  line-height: normal !important;
}

.role-segmented {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 3px;
  background: var(--table-row-alt-bg, #eef4fc);
  border: 1px solid var(--table-border-color, #d3e1f4);
  border-radius: 9px;
  gap: 4px;
}

.role-option {
  text-align: center;
  height: 30px;
  line-height: 30px;
  padding: 0 10px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--el-text-color-secondary, #475569);
  border-radius: 6px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;
}

.role-option.active {
  background: var(--el-color-primary);
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(var(--theme-rgb), 0.28);
}

.submit-btn {
  width: 100%;
  height: 42px;
  font-size: 15px;
  font-weight: 600;
  margin-top: 2px;
}

.auth-footer {
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
  color: var(--el-text-color-secondary, #475569);
}

.auth-link {
  color: var(--el-color-primary, #1677ff);
  font-weight: 600;
  text-decoration: none;
}

.auth-link:hover {
  text-decoration: underline;
}

@media (max-width: 860px) {
  .brand-showcase {
    display: none;
  }
  .auth-shell {
    justify-content: center;
  }
}
</style>
