<template>
  <el-container class="antd-pro-layout">
    <!-- 全局动态流体极光与鼠标交互粒子背景（跟随主题色与明暗模式实时切换） -->
    <ParticleBackground :isDark="isDark" />

    <!-- Ant Design Pro + Mica 云母毛玻璃侧边栏 Sider -->
    <el-aside :width="isCollapsed ? '68px' : '236px'" class="antd-sider">
      <div class="sider-logo" @click="router.push('/main/home')">
        <div class="logo-badge">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" class="logo-img xingjie-logo-svg" aria-label="星界 Logo">
            <path fill="currentColor" d="M 83 6 L 94 6 L 94 25.5 L 100 22 L 106 25.5 L 106 6 L 117 6 L 117 30 L 166 58 L 166 86 L 138 86 L 138 72 L 100 50 L 68.4 68.3 L 88 79.1 L 83.6 87.1 L 89.2 90.2 L 86 96 L 80.4 92.9 L 75.9 100.9 L 34 77.7 L 34 67.8 L 76.2 91.2 L 79.9 84.5 L 34 59.1 L 34 58 L 83 30 Z" />
            <path fill="currentColor" d="M 117 194 L 106 194 L 106 174.5 L 100 178 L 94 174.5 L 94 194 L 83 194 L 83 170 L 34 142 L 34 114 L 62 114 L 62 128 L 100 150 L 131.6 131.7 L 112 120.9 L 116.4 112.9 L 110.8 109.8 L 114 104 L 119.6 107.1 L 124.1 99.1 L 166 122.3 L 166 132.2 L 123.8 108.8 L 120.1 115.5 L 166 140.9 L 166 142 L 117 170 Z" />
            <g transform="translate(100, 100) rotate(29)">
              <path fill="currentColor" fill-rule="evenodd" d="M -7.4 -15.4 L 7.4 -15.4 L 15.4 -7.4 L 15.4 7.4 L 7.4 15.4 L -7.4 15.4 L -15.4 7.4 L -15.4 -7.4 Z M 0 -8.7 A 8.7 8.7 0 1 1 0 8.7 A 8.7 8.7 0 1 1 0 -8.7 Z" />
            </g>
          </svg>
        </div>
        <div v-show="!isCollapsed" class="logo-text-group">
          <span class="logo-text">星界</span>
          <span class="logo-sub">Star Realm</span>
        </div>
      </div>

      <el-scrollbar class="sider-menu-scroll">
        <el-menu
            router
            :collapse="isCollapsed"
            :collapse-transition="false"
            :default-active="route.path"
            :default-openeds="['1', '2', '3', '4']"
            class="antd-menu"
        >
          <el-menu-item index="/main/home">
            <el-icon><House /></el-icon>
            <template #title>工作台总览</template>
          </el-menu-item>

          <el-sub-menu index="1">
            <template #title>
              <el-icon><DataAnalysis /></el-icon>
              <span>{{ data.user.perm >= 10 ? '数据审核' : '账务与分析' }}</span>
            </template>
            <el-menu-item index="/main/bill">
              {{ data.user.perm >= 10 ? '账单审核' : '收支账单' }}
            </el-menu-item>
            <el-menu-item index="/main/AIAnalysis" v-if="data.user.role === 'USER'">
              AI 智能分析
            </el-menu-item>
            <el-menu-item index="/main/plan">
              {{ data.user.perm >= 10 ? '存钱计划审核' : '存钱计划' }}
            </el-menu-item>
            <el-menu-item index="/main/notebook">
              {{ data.user.perm >= 10 ? '理财日记审核' : '理财日记' }}
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="3" v-if="data.user.perm >= 10">
            <template #title>
              <el-icon><User /></el-icon>
              <span>用户与权限</span>
            </template>
            <el-menu-item index="/main/user">普通用户管理</el-menu-item>
            <el-menu-item index="/main/admin" v-if="data.user.perm === 15">管理员管理</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="4" v-if="data.user.perm >= 10">
            <template #title>
              <el-icon><MessageBox /></el-icon>
              <span>平台配置</span>
            </template>
            <el-menu-item index="/main/siteConfig">网站公共配置</el-menu-item>
            <el-menu-item index="/main/notice">系统公告</el-menu-item>
            <el-menu-item index="/main/detail">账户资产总览</el-menu-item>
            <el-menu-item index="/main/category">收支分类字典</el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/main/stats">
            <el-icon><Setting /></el-icon>
            <template #title>个人中心 &amp; 安全设置</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>

      <!-- 侧边栏底部实时数字时钟（时:分:秒 + 日期小字） -->
      <div class="sider-clock-card" :class="{ 'is-collapsed': isCollapsed }">
        <div class="clock-time num-font">{{ isCollapsed ? (clockTimeShort || '00:00') : (clockTimeFull || '00:00:00') }}</div>
        <div v-if="!isCollapsed" class="clock-date num-font">{{ clockDateSub }}</div>
      </div>

      <div class="sider-footer" @click="isCollapsed = !isCollapsed">
        <el-icon :size="16">
          <Expand v-if="isCollapsed" />
          <Fold v-else />
        </el-icon>
        <span v-show="!isCollapsed" class="collapse-label">收起侧边导航</span>
      </div>
    </el-aside>

    <!-- 右侧主容器 -->
    <el-container class="antd-main-container">
      <!-- 云母半透明顶栏 Header -->
      <el-header class="antd-header">
        <div class="header-left">
          <el-button
              link
              class="trigger-btn"
              @click="isCollapsed = !isCollapsed"
          >
            <el-icon :size="18">
              <Expand v-if="isCollapsed" />
              <Fold v-else />
            </el-icon>
          </el-button>
          <el-breadcrumb separator="/" class="antd-breadcrumb">
            <el-breadcrumb-item :to="{ path: '/main/home' }">星界</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 汇率 & AI 助手侧边抽屉入口 -->
          <el-button
              class="exchange-pill-btn"
              @click="toggleRightAside"
          >
            <el-icon :size="15" style="margin-right: 6px"><Coin /></el-icon>
            <span>汇率 & AI 助手</span>
          </el-button>

          <!-- 主题调色盘弹出选择器（极客蓝 / 青翠绿 / 紫罗兰 / 蛋黄琥珀） -->
          <el-popover
              placement="bottom-end"
              :width="248"
              trigger="click"
              popper-class="palette-popover"
          >
            <template #reference>
              <button type="button" class="header-icon-btn palette-trigger" title="切换主题色与粒子色彩">
                <!-- Ant Design BgColors 风格矢量调色盘图标 -->
                <svg viewBox="0 0 1024 1024" width="18" height="18" fill="currentColor">
                  <path d="M512 64C264.6 64 64 250.6 64 480.8c0 126.3 61.1 241.6 164.6 318.2 35.8 26.5 85.4 7.8 97.4-34.8 8.3-29.4 35.2-49.8 65.8-49.8h54.4c151 0 273.6-114.2 273.6-255 0-38.4 31.1-69.5 69.5-69.5h81.1c52.8 0 89.6-52.2 71.9-101.9C885.8 140.4 712.9 64 512 64zm-224 384a48 48 0 1 1 0-96 48 48 0 0 1 0 96zm128-128a48 48 0 1 1 0-96 48 48 0 0 1 0 96zm192 0a48 48 0 1 1 0-96 48 48 0 0 1 0 96zm128 128a48 48 0 1 1 0-96 48 48 0 0 1 0 96z" />
                </svg>
                <span class="active-color-dot"></span>
              </button>
            </template>

            <div class="palette-panel">
              <div class="palette-head">
                <span class="palette-title">主题色与粒子光效</span>
                <span class="palette-sub">随心配</span>
              </div>
              <div class="palette-list">
                <div
                    v-for="item in presets"
                    :key="item.key"
                    class="palette-option"
                    :class="{ active: activeThemeKey === item.key }"
                    @click="selectAccentTheme(item.key, $event)"
                >
                  <span class="swatch-circle" :style="{ background: `linear-gradient(135deg, ${item.primary}, ${item.secondary})` }"></span>
                  <div class="swatch-meta">
                    <span class="swatch-name">{{ item.name }}</span>
                    <span class="swatch-desc">{{ item.desc }}</span>
                  </div>
                  <el-icon v-if="activeThemeKey === item.key" class="swatch-check"><Check /></el-icon>
                </div>
              </div>
            </div>
          </el-popover>

          <!-- 聚光灯扩散动画明暗主题切换按钮 -->
          <button
              type="button"
              class="header-icon-btn spotlight-theme-btn"
              :title="isDark ? '切换至浅色模式 (聚光灯渲染)' : '切换至暗夜模式 (聚光灯渲染)'"
              @click="handleSpotlightThemeToggle($event)"
          >
            <Transition name="icon-spin" mode="out-in">
              <el-icon v-if="isDark" key="moon" :size="17" class="moon-icon"><Moon /></el-icon>
              <el-icon v-else key="sun" :size="18" class="sun-icon"><Sunny /></el-icon>
            </Transition>
          </button>

          <!-- 个人头像下拉菜单 -->
          <el-dropdown trigger="click">
            <div class="user-profile-trigger">
              <el-avatar
                  :size="30"
                  :src="data.user.avatar || avatarFallback"
                  class="user-avatar-ring"
              />
              <div class="user-meta">
                <span class="user-name">{{ data.user.nickname || data.user.username }}</span>
                <span class="user-role-badge">{{ data.user.role === 'USER' ? '个人账户' : '管理员' }}</span>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/main/stats')">
                  <el-icon><Setting /></el-icon>个人中心 &amp; 安全设置
                </el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区：带丝滑路由过渡动画与抽屉式汇率面板 -->
      <el-container class="antd-content-body">
        <el-main class="antd-page-view">
          <RouterView v-slot="{ Component, route: viewRoute }">
            <Transition name="page-slide-blur" mode="out-in">
              <div :key="viewRoute.fullPath" class="page-transition-shell">
                <component :is="Component" @updateUser="updateUser" :isDark="isDark" />
              </div>
            </Transition>
          </RouterView>
        </el-main>

        <el-drawer
            v-model="showExchangeDrawer"
            title="外汇快算 & 智能理财客服"
            direction="rtl"
            size="430px"
            class="assistant-drawer"
        >
          <Exchange />
        </el-drawer>
      </el-container>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onBeforeUnmount, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  Check,
  Coin,
  DataAnalysis,
  Expand,
  Fold,
  House,
  Key,
  MessageBox,
  Moon,
  Setting,
  Sunny,
  SwitchButton,
  User
} from "@element-plus/icons-vue";
import { useDark } from "@vueuse/core";
import Exchange from "@/views/Exchange.vue";
import ParticleBackground from "@/components/ParticleBackground.vue";
import avatarFallback from "@/assets/avatar.jpg";
import { useThemePalette } from "@/utils/theme.js";

const route = useRoute();
const router = useRouter();
const isDark = useDark();
const isCollapsed = ref(false);
const showExchangeDrawer = ref(false);

const { presets, activeThemeKey, applyThemePalette } = useThemePalette();

const data = reactive({
  user: JSON.parse(localStorage.getItem("loginToken") || "{}")
});

if (!data.user || !data.user.id) {
  router.push("/login");
}

const currentTitle = computed(() => {
  const raw = route.meta?.title || "当前页面";
  return raw.replace(/^(星界|智汇账本)\s*-\s*/, "");
});

// 聚光灯从点击位置发散至全屏的实时渲染切换效果 (View Transitions API + Web Animations clipPath)
const runSpotlightTransition = (event, stateMutator) => {
  const x = event?.clientX ?? window.innerWidth - 80;
  const y = event?.clientY ?? 32;
  const endRadius = Math.hypot(
    Math.max(x, window.innerWidth - x),
    Math.max(y, window.innerHeight - y)
  );
  if (!document.startViewTransition) {
    stateMutator();
    return;
  }
  const transition = document.startViewTransition(async () => {
    stateMutator();
    await nextTick();
  });
  transition.ready
    .then(() => {
      document.documentElement.animate(
        {
          clipPath: [
            `circle(0px at ${x}px ${y}px)`,
            `circle(${endRadius}px at ${x}px ${y}px)`
          ]
        },
        {
          duration: 540,
          easing: "cubic-bezier(0.22, 1, 0.36, 1)",
          pseudoElement: "::view-transition-new(root)"
        }
      );
    })
    .catch(() => {});
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

const toggleRightAside = () => {
  showExchangeDrawer.value = !showExchangeDrawer.value;
};

const logout = () => {
  localStorage.removeItem("loginToken");
  router.replace("/login");
};

const updateUser = () => {
  data.user = JSON.parse(localStorage.getItem("loginToken") || "{}");
};

// 侧边导航底部实时数字时钟（setup 阶段立即初始化，保证首帧即渲染时间与日期）
const pad2 = (n) => String(n).padStart(2, "0");
const weekShort = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];

const formatClockNow = () => {
  const d = new Date();
  const hh = pad2(d.getHours());
  const mm = pad2(d.getMinutes());
  const ss = pad2(d.getSeconds());
  return {
    full: `${hh}:${mm}:${ss}`,
    short: `${hh}:${mm}`,
    date: `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} · ${weekShort[d.getDay()]}`
  };
};

const initialClock = formatClockNow();
const clockTimeFull = ref(initialClock.full);
const clockTimeShort = ref(initialClock.short);
const clockDateSub = ref(initialClock.date);
let clockTimer = null;

const updateClock = () => {
  const next = formatClockNow();
  clockTimeFull.value = next.full;
  clockTimeShort.value = next.short;
  clockDateSub.value = next.date;
};

applyThemePalette(activeThemeKey.value, isDark.value);

onMounted(() => {
  applyThemePalette(activeThemeKey.value, isDark.value);
  updateClock();
  clockTimer = setInterval(updateClock, 1000);
});

onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer);
});
</script>

<style scoped>
.antd-pro-layout {
  position: relative;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: var(--antd-layout-bg);
  transition: background 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}

html.dark .antd-pro-layout {
  --antd-layout-bg: radial-gradient(120% 100% at 15% 0%, rgba(var(--theme-rgb), 0.16) 0%, transparent 55%),
                    radial-gradient(100% 90% at 90% 100%, rgba(var(--theme-rgb), 0.12) 0%, transparent 50%),
                    var(--theme-bg-dark, #080e1c);
  --antd-sider-bg: var(--table-row-bg, #0c1322);
  --antd-header-bg: var(--table-row-bg, #0c1322);
  --antd-border-color: var(--table-border-color, #1e2c46);
  --antd-menu-active-bg: var(--table-hover-bg, #18243c);
}

html:not(.dark) .antd-pro-layout {
  --antd-layout-bg: radial-gradient(120% 100% at 15% 0%, rgba(var(--theme-rgb), 0.14) 0%, transparent 55%),
                    radial-gradient(100% 90% at 90% 100%, rgba(var(--theme-rgb), 0.10) 0%, transparent 50%),
                    var(--theme-bg-light, #eaf2ff);
  --antd-sider-bg: var(--table-row-bg, #f6f9fe);
  --antd-header-bg: var(--table-row-bg, #f6f9fe);
  --antd-border-color: var(--table-border-color, #d3e1f4);
  --antd-menu-active-bg: var(--table-hover-bg, #dbe8fa);
}

/* Sider 云母材质样式 */
.antd-sider {
  position: relative;
  z-index: 20;
  background: var(--antd-sider-bg);
  backdrop-filter: blur(22px) saturate(180%);
  -webkit-backdrop-filter: blur(22px) saturate(180%);
  border-right: 1px solid var(--antd-border-color);
  display: flex;
  flex-direction: column;
  transition: width 0.28s cubic-bezier(0.22, 1, 0.36, 1);
  box-shadow: 4px 0 24px -4px rgba(15, 23, 42, 0.04);
  user-select: none;
}

.sider-logo {
  height: 64px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--antd-border-color);
  overflow: hidden;
  white-space: nowrap;
}

.logo-badge {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(var(--theme-rgb), 0.16), rgba(var(--theme-rgb), 0.06));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.sider-logo:hover .logo-badge {
  transform: rotate(-6deg) scale(1.06);
}

.logo-img {
  width: 26px;
  height: 26px;
  object-fit: contain;
}

.xingjie-logo-svg {
  color: #111827;
  transition: color 0.28s ease;
}

html.dark .xingjie-logo-svg {
  color: #f8fafc;
}

.logo-text-group {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.logo-text {
  font-size: 17px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 0.4px;
}

.logo-sub {
  font-size: 10px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  letter-spacing: 0.6px;
  text-transform: uppercase;
}

.sider-menu-scroll {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.antd-menu {
  border-right: none !important;
  padding: 10px;
  background-color: transparent !important;
}

/* 胶囊式流光菜单项 */
:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 42px !important;
  line-height: 42px !important;
  margin: 4px 0 !important;
  border-radius: 10px !important;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}

:deep(.el-sub-menu .el-menu-item) {
  height: 38px !important;
  line-height: 38px !important;
  padding-left: 44px !important;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: rgba(var(--theme-rgb), 0.08) !important;
  transform: translateX(3px);
}

:deep(.el-menu-item.is-active) {
  background: var(--antd-menu-active-bg) !important;
  color: var(--el-color-primary) !important;
  font-weight: 600;
  box-shadow: inset 3px 0 0 var(--el-color-primary);
}

.sider-clock-card {
  flex-shrink: 0;
  margin: 6px 12px 10px 12px;
  padding: 10px 12px;
  min-height: 54px;
  border-radius: 10px;
  background-color: var(--table-head-bg, #e4eefb);
  border: 1px solid var(--table-border-color, #d3e1f4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  transition: all 0.2s ease;
}

.sider-clock-card.is-collapsed {
  margin: 6px 6px 8px 6px;
  padding: 8px 4px;
  min-height: 38px;
}

.clock-time {
  display: block;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.06em;
  line-height: 1.3;
  color: var(--theme-primary, #1677ff) !important;
  -webkit-text-fill-color: var(--theme-primary, #1677ff) !important;
}

html.dark .clock-time {
  color: #f8fafc !important;
  -webkit-text-fill-color: #f8fafc !important;
}

.sider-clock-card.is-collapsed .clock-time {
  font-size: 12px;
  letter-spacing: 0;
}

.clock-date {
  display: block;
  margin-top: 3px;
  font-size: 11.5px;
  font-weight: 500;
  color: var(--el-text-color-regular, #475569) !important;
  -webkit-text-fill-color: var(--el-text-color-regular, #475569) !important;
  letter-spacing: 0.02em;
}

html.dark .clock-date {
  color: #94a3b8 !important;
  -webkit-text-fill-color: #94a3b8 !important;
}

.sider-footer {
  flex-shrink: 0;
  height: 46px;
  border-top: 1px solid var(--antd-border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  transition: all 0.2s;
}

.sider-footer:hover {
  color: var(--el-color-primary);
  background-color: rgba(var(--theme-rgb), 0.06);
}

/* 顶部云母导航栏 Header */
.antd-main-container {
  position: relative;
  z-index: 10;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.antd-header {
  height: 64px !important;
  background: var(--antd-header-bg);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid var(--antd-border-color);
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 15;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.trigger-btn {
  padding: 8px;
  border-radius: 8px;
  color: var(--el-text-color-regular);
}

.trigger-btn:hover {
  color: var(--el-color-primary);
  background-color: rgba(var(--theme-rgb), 0.08);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.exchange-pill-btn {
  border-radius: 999px !important;
  padding: 6px 14px !important;
  font-size: 13px;
  font-weight: 600;
  background: rgba(var(--theme-rgb), 0.1) !important;
  border: 1px solid rgba(var(--theme-rgb), 0.24) !important;
  color: var(--el-color-primary) !important;
}

.exchange-pill-btn:hover {
  background: var(--el-color-primary) !important;
  color: #ffffff !important;
}

/* 顶栏圆形图标按钮（调色盘 & 聚光灯明暗切换） */
.header-icon-btn {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(var(--theme-rgb), 0.2);
  background: rgba(var(--theme-rgb), 0.07);
  color: var(--el-text-color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.24s cubic-bezier(0.22, 1, 0.36, 1);
}

.header-icon-btn:hover {
  transform: translateY(-1.5px) scale(1.04);
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  box-shadow: 0 6px 16px -3px rgba(var(--theme-rgb), 0.28);
}

.active-color-dot {
  position: absolute;
  right: 4px;
  bottom: 4px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--theme-primary);
  border: 1.5px solid var(--el-bg-color-overlay);
}

.sun-icon {
  color: #0f172a;
}

.moon-icon {
  color: #60a5fa;
}

.icon-spin-enter-active,
.icon-spin-leave-active {
  transition: all 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}
.icon-spin-enter-from {
  opacity: 0;
  transform: rotate(-75deg) scale(0.5);
}
.icon-spin-leave-to {
  opacity: 0;
  transform: rotate(75deg) scale(0.5);
}

/* 调色盘面板样式 */
.palette-panel {
  padding: 4px 2px;
}

.palette-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  margin-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.palette-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.palette-sub {
  font-size: 11px;
  color: var(--el-color-primary);
  background: rgba(var(--theme-rgb), 0.1);
  padding: 1px 6px;
  border-radius: 999px;
  font-weight: 600;
}

.palette-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.palette-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 10px;
  border-radius: 10px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

.palette-option:hover {
  background: rgba(var(--theme-rgb), 0.06);
}

.palette-option.active {
  background: rgba(var(--theme-rgb), 0.1);
  border-color: rgba(var(--theme-rgb), 0.28);
}

.swatch-circle {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.18);
}

.swatch-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.swatch-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.swatch-desc {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.swatch-check {
  color: var(--el-color-primary);
  font-weight: bold;
}

.user-profile-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.22s ease;
}

.user-profile-trigger:hover {
  background-color: rgba(var(--theme-rgb), 0.06);
  border-color: rgba(var(--theme-rgb), 0.18);
}

.user-avatar-ring {
  border: 2px solid rgba(var(--theme-rgb), 0.4);
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.user-role-badge {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

/* 内容区域与子路由切换过渡动画 */
.antd-content-body {
  flex: 1;
  overflow: hidden;
}

.antd-page-view {
  padding: 20px 24px;
  overflow-y: auto;
  height: calc(100vh - 64px);
}

.page-transition-shell {
  min-height: 100%;
}

:deep(.assistant-drawer .el-drawer__header) {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--antd-border-color);
  font-weight: 700;
}

:deep(.assistant-drawer .el-drawer__body) {
  padding: 14px 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.page-slide-blur-enter-active {
  transition: opacity 0.34s cubic-bezier(0.22, 1, 0.36, 1),
              transform 0.34s cubic-bezier(0.22, 1, 0.36, 1),
              filter 0.34s cubic-bezier(0.22, 1, 0.36, 1);
}

.page-slide-blur-leave-active {
  transition: opacity 0.2s cubic-bezier(0.4, 0, 1, 1),
              transform 0.2s cubic-bezier(0.4, 0, 1, 1),
              filter 0.2s ease;
}

.page-slide-blur-enter-from {
  opacity: 0;
  transform: translateY(14px) scale(0.992);
  filter: blur(6px);
}

.page-slide-blur-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.996);
  filter: blur(4px);
}
</style>
