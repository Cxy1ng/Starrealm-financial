<template>
  <div class="site-config-page">
    <el-card shadow="never" class="config-hero-card stagger-1">
      <div class="hero-head">
        <div class="hero-title-group">
          <div class="hero-icon-badge">
            <el-icon :size="22"><Setting /></el-icon>
          </div>
          <div>
            <h2 class="page-title">网站公共资源与 API 密钥配置</h2>
            <p class="page-desc">
              此处的配置属于全站公共资源（存储在数据库 <code>site_config</code> 表中），不绑定于单个管理员个人账户。所有 API 密钥均通过 AES-128 对称加密落库。
            </p>
          </div>
        </div>
        <div class="hero-actions">
          <el-button @click="loadConfigs" :loading="loading">
            <el-icon style="margin-right: 5px"><Refresh /></el-icon>刷新配置
          </el-button>
          <el-button type="primary" @click="saveConfigs" :loading="saving">
            <el-icon style="margin-right: 5px"><Check /></el-icon>加密保存全部配置
          </el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="18" class="stagger-2">
      <!-- Finexly 实时汇率 API 配置卡片 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="config-section-card">
          <template #header>
            <div class="card-head-row">
              <div class="head-left">
                <el-icon class="head-icon"><Coin /></el-icon>
                <span class="head-title">Finexly 实时汇率 API 配置</span>
              </div>
              <el-tag :type="statusMeta.finexly_api_key ? 'success' : 'warning'" size="small" effect="light">
                {{ statusMeta.finexly_api_key ? '已加密配置' : '待配置' }}
              </el-tag>
            </div>
          </template>

          <el-form label-position="top" :model="form">
            <el-form-item label="Finexly API Key (Authorization: Bearer YOUR_KEY)">
              <el-input
                  v-model="form.finexly_api_key"
                  type="password"
                  show-password
                  clearable
                  placeholder="请输入 Finexly API Key，保存时自动 AES-128 加密"
              />
              <div class="field-help">
                用于调用 <code>https://api.finexly.com/v1/rate?from=USD&amp;to=EUR</code> 获取实时基准汇率。前端获取单价汇率后将在本地实时计算转换金额。
              </div>
            </el-form-item>

            <div class="test-bar">
              <el-button size="small" type="primary" plain :loading="testingRate" @click="testFinexlyRate">
                测试汇率接口 (USD → CNY)
              </el-button>
              <span v-if="rateTestResult" class="test-result num-font">{{ rateTestResult }}</span>
            </div>
          </el-form>
        </el-card>
      </el-col>

      <!-- DeepSeek 公共接口与备用密钥配置卡片 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="config-section-card">
          <template #header>
            <div class="card-head-row">
              <div class="head-left">
                <el-icon class="head-icon"><Cpu /></el-icon>
                <span class="head-title">DeepSeek 智能服务配置</span>
              </div>
              <el-tag type="success" size="small" effect="light" round>
                用户自备 Key 架构
              </el-tag>
            </div>
          </template>

          <!-- 个人中心同款金库级安全状态横幅 -->
          <div class="vault-status-banner is-active">
            <div class="vault-shield-icon">
              <el-icon :size="24"><CircleCheckFilled /></el-icon>
            </div>
            <div class="vault-status-text">
              <div class="vault-main-line">全站统一采用「用户自备 API 密钥」零托管安全模式</div>
              <div class="vault-sub-line">
                平台不设全局公摊 API Key 资金池，杜绝算力挤占与公摊消耗。用户与管理员在【个人中心 &amp; 安全设置】中加密保存个人专属 DeepSeek Key 即可畅享全套智能财务分析与侧边栏理财助手。
              </div>
            </div>
          </div>

          <el-form label-position="top" :model="form" style="margin-top: 16px;">
            <el-form-item label="DeepSeek API 基础端点 (Base URL)">
              <el-input
                  v-model="form.deepseek_base_url"
                  clearable
                  placeholder="默认 https://api.deepseek.com"
              />
              <div class="field-help">
                官方标准端点为 <code>https://api.deepseek.com</code>（系统发起大模型推理调用时会自动拼接 <code>/v1/chat/completions</code> 与 <code>/v1/models</code>；如自建反代或中转网关可在此直接替换）。
              </div>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, Refresh, Check, Coin, Cpu, CircleCheckFilled } from '@element-plus/icons-vue'
import request from '@/utils/request.js'

const loading = ref(false)
const saving = ref(false)
const testingRate = ref(false)
const rateTestResult = ref('')

const form = reactive({
  finexly_api_key: '',
  deepseek_base_url: 'https://api.deepseek.com'
})

const statusMeta = reactive({
  finexly_api_key: false
})

const loadConfigs = async () => {
  loading.value = true
  try {
    const res = await request.get('/siteConfig/selectAll')
    if (res.code === 200 && Array.isArray(res.data)) {
      for (const item of res.data) {
        if (item.configKey in form) {
          form[item.configKey] = item.configValue || ''
        }
        if (item.configKey in statusMeta) {
          statusMeta[item.configKey] = !!item.hasValue
        }
      }
    } else {
      ElMessage.error(res.msg || '加载网站配置失败')
    }
  } finally {
    loading.value = false
  }
}

const saveConfigs = async () => {
  saving.value = true
  try {
    const res = await request.put('/siteConfig/saveBatch', {
      finexly_api_key: form.finexly_api_key,
      deepseek_base_url: form.deepseek_base_url || 'https://api.deepseek.com'
    })
    if (res.code === 200) {
      ElMessage.success('网站公共配置已加密保存！')
      await loadConfigs()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

const testFinexlyRate = async () => {
  testingRate.value = true
  rateTestResult.value = ''
  try {
    const res = await request.get('/exchange/rate', { params: { from: 'USD', to: 'CNY' } })
    if (res.code === 200 && res.data) {
      rateTestResult.value = `1 USD = ${res.data.rate} CNY (${res.data.updateTime})`
      ElMessage.success('Finexly 汇率接口连通成功')
    } else {
      rateTestResult.value = res.msg || '调用失败'
      ElMessage.warning(res.msg || '汇率查询失败')
    }
  } catch (e) {
    rateTestResult.value = '请求异常'
  } finally {
    testingRate.value = false
  }
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.site-config-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-hero-card {
  background: linear-gradient(135deg, rgba(var(--theme-rgb), 0.12), rgba(var(--theme-rgb), 0.03)) !important;
}

.hero-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.hero-title-group {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hero-icon-badge {
  width: 46px;
  height: 46px;
  border-radius: 13px;
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(var(--theme-rgb), 0.28);
}

.page-title {
  margin: 0 0 4px 0;
  font-size: 20px;
  font-weight: 700;
}

.page-desc {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.hero-actions {
  display: flex;
  gap: 10px;
}

.config-section-card {
  height: 100%;
}

.card-head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.head-icon {
  color: var(--el-color-primary);
  font-size: 18px;
}

.head-title {
  font-size: 15px;
  font-weight: 700;
}

.field-help {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}

.test-bar {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--el-border-color-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.test-result {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--el-color-primary);
}
.vault-status-banner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(245, 158, 11, 0.09);
  border: 1px solid rgba(245, 158, 11, 0.28);
  margin-bottom: 6px;
  transition: all 0.25s ease;
}

.vault-status-banner.is-active {
  background: rgba(16, 185, 129, 0.08);
  border-color: rgba(16, 185, 129, 0.26);
}

.vault-shield-icon {
  color: #f59e0b;
  margin-top: 1px;
  flex-shrink: 0;
}

.vault-status-banner.is-active .vault-shield-icon {
  color: #10b981;
}

.vault-status-text {
  flex: 1;
}

.vault-main-line {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.vault-sub-line {
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}
</style>