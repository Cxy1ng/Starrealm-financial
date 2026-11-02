<template>
  <div class="notfound-page" :class="{ dark: isDark }">
    <ParticleBackground />
    <el-card class="notfound-card stagger-1" shadow="never">
      <img src="@/assets/404.svg" alt="404" class="notfound-illustration" />
      <h2 class="notfound-title">404 · 页面迷路了</h2>
      <p class="notfound-desc">抱歉，您访问的页面地址不存在或已被移动。</p>
      <el-button type="primary" size="large" @click="handleReturn" class="notfound-btn">
        {{ isLoggedIn ? '返回工作台首页' : '返回登录页面' }}
      </el-button>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import ParticleBackground from "@/components/ParticleBackground.vue";
import { useDark } from "@vueuse/core";
import { useThemePalette } from "@/utils/theme.js";
import { getLoginSessionFromCookie } from "@/utils/cookie.js";

const router = useRouter();
const isDark = useDark();
const { presets, activeThemeKey, applyThemePalette, activeFontSize, applyGlobalFontSize } = useThemePalette();

onMounted(() => {
  applyThemePalette(activeThemeKey.value, isDark.value);
  applyGlobalFontSize(activeFontSize.value);
});

const isLoggedIn = computed(() => {
  try {
    const u = JSON.parse(localStorage.getItem("loginToken") || "{}");
    if (u?.token) return true;
  } catch {}
  const c = getLoginSessionFromCookie();
  return Boolean(c?.token);
});

const handleReturn = () => {
  if (isLoggedIn.value) {
    router.push("/main/home");
  } else {
    router.push("/login");
  }
};
</script>

<style scoped>
.notfound-page {
  height: 100vh;
  width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: var(--el-bg-color-page, #f8fafc);
  overflow: hidden;
  transition: background-color 0.3s ease;
}

:deep(html.dark) .notfound-page,
.notfound-page.dark {
  background: #0b0f19;
}

.notfound-card {
  position: relative;
  z-index: 5;
  text-align: center;
  padding: 36px 48px;
  max-width: 520px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.82) !important;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(var(--theme-rgb, 22, 119, 255), 0.16) !important;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(html.dark) .notfound-card,
.dark .notfound-card {
  background: rgba(18, 24, 38, 0.82) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.45);
}

.notfound-illustration {
  width: 100%;
  max-width: 320px;
  height: auto;
  margin-bottom: 16px;
  filter: drop-shadow(0 8px 24px rgba(var(--theme-rgb, 22, 119, 255), 0.18));
  transition: filter 0.3s ease;
}

:deep(html.dark) .notfound-illustration,
.dark .notfound-illustration {
  filter: drop-shadow(0 8px 28px rgba(var(--theme-rgb, 22, 119, 255), 0.32)) brightness(0.92);
}

.notfound-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 8px 0;
  color: var(--el-text-color-primary, #0f172a);
}

.notfound-desc {
  color: var(--el-text-color-secondary, #64748b);
  margin: 0 0 24px 0;
  font-size: 14px;
  line-height: 1.6;
}

.notfound-btn {
  padding: 12px 32px;
  font-weight: 600;
  border-radius: 10px;
  box-shadow: 0 6px 18px rgba(var(--theme-rgb, 22, 119, 255), 0.28);
}
</style>
