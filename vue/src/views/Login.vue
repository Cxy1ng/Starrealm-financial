<template>
  <div class="auth-container">
    <video class="loginbg" autoplay loop muted playsinline disablePictureInPicture controlsList="nodownload">
      <source src="@/assets/loginbg1080.mp4" type="video/mp4">
    </video>
    <div class="bg-overlay"></div>
    <ParticleBackground :isDark="true" :particleCount="55" />

    <div class="auth-shell">
      <div class="brand-showcase stagger-1">
        <div class="brand-pill">
          <span class="pulse-dot"></span>
          <span>AI-Driven Personal Finance Platform</span>
        </div>
        <h1 class="brand-headline">智汇账本</h1>
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
        <div class="card-top">
          <img src="@/assets/Ledger.png" alt="logo" class="card-logo" />
          <div>
            <div class="card-title">欢迎回来</div>
            <div class="card-subtitle">请登录您的智汇账本账户</div>
          </div>
        </div>

        <el-form ref="formRef" :rules="data.rules" :model="data.form" size="large" @keyup.enter="login">
          <el-form-item prop="username">
            <el-input v-model="data.form.username" placeholder="请输入用户名" prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="pwd">
            <el-input type="password" v-model="data.form.pwd" placeholder="请输入登录密码" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item prop="perm">
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
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Cpu, PieChart, Lock } from "@element-plus/icons-vue";
import { request } from "@/utils/request.js";
import ParticleBackground from "@/components/ParticleBackground.vue";

const router = useRouter();
const loading = ref(false);
const formRef = ref();

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
  background: #38bdf8;
  box-shadow: 0 0 10px #38bdf8;
}

.brand-headline {
  font-size: 46px;
  font-weight: 800;
  margin: 0 0 16px 0;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #ffffff 30%, #93c5fd 100%);
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
  width: 400px;
  padding: 40px 36px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(28px) saturate(190%);
  -webkit-backdrop-filter: blur(28px) saturate(190%);
  border: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.32);
}

.card-top {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}

.card-logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
}

.card-title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.card-subtitle {
  font-size: 13px;
  color: #64748b;
  margin-top: 2px;
}

.role-segmented {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 4px;
  background: rgba(15, 23, 42, 0.06);
  border-radius: 10px;
  gap: 4px;
}

.role-option {
  text-align: center;
  padding: 7px 0;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.22s ease;
}

.role-option.active {
  background: #ffffff;
  color: #1677ff;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  margin-top: 4px;
}

.auth-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: #475569;
}

.auth-link {
  color: #1677ff;
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
