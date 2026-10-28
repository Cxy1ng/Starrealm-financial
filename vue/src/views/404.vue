<template>
  <div class="notfound-page">
    <ParticleBackground />
    <el-card class="notfound-card stagger-1" shadow="never">
      <img src="@/assets/404.svg" alt="404" class="notfound-illustration" />
      <h2 class="notfound-title">404 · 页面迷路了</h2>
      <p class="notfound-desc">抱歉，您访问的页面地址不存在或已被移动。</p>
      <el-button type="primary" size="large" @click="router.push('/main/home')">
        返回工作台首页
      </el-button>
    </el-card>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
import ParticleBackground from "@/components/ParticleBackground.vue";

const router = useRouter();
</script>

<style scoped>
.notfound-page {
  height: 100vh;
  width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.notfound-card {
  position: relative;
  z-index: 5;
  text-align: center;
  padding: 36px 48px;
  max-width: 520px;
}

.notfound-illustration {
  width: 100%;
  max-width: 320px;
  height: auto;
  margin-bottom: 16px;
}

.notfound-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 8px 0;
}

.notfound-desc {
  color: var(--el-text-color-secondary);
  margin: 0 0 24px 0;
}
</style>
