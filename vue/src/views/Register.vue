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
          <span>Start Your Smart Wealth Journey</span>
        </div>
        <h1 class="brand-headline">开启智能记账</h1>
        <p class="brand-desc">
          只需 30 秒创建您的专属智汇账本账户，即刻体验多维可视化收支报表、目标存钱计划与 DeepSeek 私人理财顾问。
        </p>
      </div>

      <div class="mica-auth-card stagger-2">
        <div class="card-top">
          <img src="@/assets/Ledger.svg" alt="logo" class="card-logo" style="width: 44px; height: 44px;" />
          <div>
            <div class="card-title">创建新账户</div>
            <div class="card-subtitle">加入星界，科学规划个人财务</div>
          </div>
        </div>

        <el-form ref="formRef" :rules="data.rules" :model="data.form" size="large" @keyup.enter="register">
          <el-form-item prop="username">
            <el-input v-model="data.form.username" placeholder="请输入用户名" prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="pwd">
            <el-input type="password" v-model="data.form.pwd" placeholder="请设置登录密码" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item prop="confirmPwd">
            <el-input type="password" v-model="data.form.confirmPwd" placeholder="请再次输入确认密码" prefix-icon="Check" show-password />
          </el-form-item>

          <el-button @click="register" :loading="loading" size="large" type="primary" class="submit-btn">
            立即注册
          </el-button>

          <div class="auth-footer">
            已有账户？
            <router-link to="/login" class="auth-link">返回登录 →</router-link>
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
import { request } from "@/utils/request.js";
import ParticleBackground from "@/components/ParticleBackground.vue";

const router = useRouter();
const loading = ref(false);
const formRef = ref();

const validatePass = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'));
  } else if (value !== data.form.pwd) {
    callback(new Error("两次密码不一致"));
  } else {
    callback();
  }
};

const data = reactive({
  form: { perm: 1 },
  rules: {
    username: [
      { required: true, message: '请输入有效的用户名', trigger: 'blur' }
    ],
    pwd: [
      { required: true, message: '请输入有效的密码', trigger: 'blur' }
    ],
    confirmPwd: [
      { validator: validatePass, trigger: 'blur' }
    ]
  }
});

const register = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      loading.value = true;
      request.post('/register', data.form).then(res => {
        loading.value = false;
        if (res.code === 200) {
          ElMessage.success('注册成功，请登录！');
          router.push('/login');
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
  width: min(1040px, 92vw);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 48px;
}

.brand-showcase {
  flex: 1;
  color: #ffffff;
  max-width: 480px;
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
  margin-bottom: 20px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 10px #34d399;
}

.brand-headline {
  font-size: 44px;
  font-weight: 800;
  margin: 0 0 16px 0;
  background: linear-gradient(135deg, #ffffff 30%, #6ee7b7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.brand-desc {
  font-size: 15px;
  line-height: 1.75;
  color: rgba(241, 245, 249, 0.85);
}

.mica-auth-card {
  width: 400px;
  padding: 40px 36px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.84);
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

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  margin-top: 6px;
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
