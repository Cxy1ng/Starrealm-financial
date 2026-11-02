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
                自备 Key 模式
              </el-tag>
            </div>
          </template>

          <!-- 个人中心同款金库级安全状态横幅 -->
          <div class="vault-status-banner is-active">
            <div class="vault-shield-icon">
              <el-icon :size="24"><CircleCheckFilled /></el-icon>
            </div>
            <div class="vault-status-text">
              <div class="vault-main-line">全站采用用户自备 API Key 模式</div>
              <div class="vault-sub-line">
                平台不统一提供公共 API 额度，用户需在个人中心配置个人 Key 后方可使用智能分析（未注册用户可指引其前往 <a href="https://platform.deepseek.com/" target="_blank" rel="noopener noreferrer" style="color: var(--el-color-primary); text-decoration: underline;">DeepSeek 开放平台官网</a> 获取）。
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

    <!-- 第二行：用户默认头像配置 (左侧1:1锁定正方形比例截取框 + 右侧大中小场景预览) -->
    <el-row :gutter="18" class="stagger-3" style="margin-top: 18px;">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="config-section-card">
          <template #header>
            <div class="card-head-row">
              <div class="head-left">
                <el-icon class="head-icon"><Avatar /></el-icon>
                <span class="head-title">新用户默认头像配置</span>
              </div>
              <el-tag :type="form.default_user_avatar ? 'success' : 'info'" size="small" effect="light" round>
                {{ form.default_user_avatar ? '已设置自定义默认头像' : '使用系统内置默认头像' }}
              </el-tag>
            </div>
          </template>

          <div class="avatar-editor-layout">
            <!-- 左侧：1:1 锁定正方形比例截取框 -->
            <div class="cropper-column">
              <div class="cropper-box-wrap">
                <canvas
                    ref="cropperCanvasRef"
                    width="220"
                    height="220"
                    class="cropper-canvas"
                    @mousedown="onCanvasMouseDown"
                    @mousemove="onCanvasMouseMove"
                    @mouseup="onCanvasMouseUp"
                    @mouseleave="onCanvasMouseUp"
                    @touchstart.passive="onCanvasTouchStart"
                    @touchmove.prevent="onCanvasTouchMove"
                    @touchend="onCanvasTouchEnd"
                    @wheel.prevent="onCanvasWheel"
                ></canvas>
                <div class="cropper-hint-badge">
                  <el-icon><Rank /></el-icon> 拖拽平移 · 滚轮缩放
                </div>
              </div>

              <!-- 缩放控制条 -->
              <div class="cropper-zoom-row">
                <el-icon class="zoom-icon-btn" title="缩小" @click="zoomOut"><ZoomOut /></el-icon>
                <el-slider
                    v-model="zoom"
                    :min="1"
                    :max="3"
                    :step="0.02"
                    :show-tooltip="false"
                    class="zoom-slider"
                    @input="drawCropper"
                />
                <el-icon class="zoom-icon-btn" title="放大" @click="zoomIn"><ZoomIn /></el-icon>
                <span class="zoom-val-text">{{ Math.round(zoom * 100) }}%</span>
              </div>

              <!-- 操作按钮行 -->
              <div class="cropper-btn-row">
                <el-upload
                    action="#"
                    :auto-upload="false"
                    :show-file-list="false"
                    :on-change="onFileSelected"
                    accept="image/*"
                >
                  <el-button size="small" type="primary" plain>
                    <el-icon style="margin-right: 4px;"><Upload /></el-icon>选择本地图片
                  </el-button>
                </el-upload>

                <el-button
                    v-if="isCroppedModified"
                    size="small"
                    type="success"
                    :loading="uploadingAvatar"
                    @click="applyAndUploadCropped"
                >
                  <el-icon style="margin-right: 4px;"><Check /></el-icon>应用并上传
                </el-button>

                <el-button
                    v-if="form.default_user_avatar"
                    size="small"
                    type="danger"
                    link
                    @click="resetToDefaultAvatar"
                >
                  恢复内置
                </el-button>
              </div>
            </div>

            <!-- 右侧：从大到小三种场景预览 + 单一按钮下拉切换身份流光 -->
            <div class="previews-column">
              <div class="previews-header">
                <div class="previews-title">
                  <el-icon class="title-icon"><View /></el-icon>
                  <span>全站场景等比效果</span>
                </div>

                <!-- 头像框切换：单个按钮 + 下拉菜单 -->
                <el-dropdown trigger="click" @command="handleRoleCommand">
                  <el-button size="small" class="role-switch-dropdown-btn">
                    <span class="role-swatch-dot" :class="'swatch-' + previewRole"></span>
                    <span class="role-btn-text">{{ roleNameMap[previewRole] }}</span>
                    <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="user">
                        <div class="dropdown-role-item">
                          <span class="role-swatch-dot swatch-user"></span>
                          <span>普通用户 (星界蓝)</span>
                        </div>
                      </el-dropdown-item>
                      <el-dropdown-item command="admin">
                        <div class="dropdown-role-item">
                          <span class="role-swatch-dot swatch-admin"></span>
                          <span>管理员 (翡翠绿)</span>
                        </div>
                      </el-dropdown-item>
                      <el-dropdown-item command="super">
                        <div class="dropdown-role-item">
                          <span class="role-swatch-dot swatch-super"></span>
                          <span>超级管理员 (鎏金)</span>
                        </div>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>

              <!-- 从大到小三行紧凑场景预览 -->
              <div class="previews-vertical-stack">
                <!-- 1. 大尺寸 (80px) -->
                <div class="preview-item-row">
                  <div
                      class="avatar-shimmer-ring preview-ring-large"
                      :class="previewRingClass"
                  >
                    <el-avatar
                        :src="displayAvatar"
                        class="preview-avatar-img"
                    />
                  </div>
                  <div class="preview-item-meta">
                    <div class="meta-headline">
                      <span class="meta-title">个人中心 Hero</span>
                      <span class="preview-badge badge-large">80px · 资料主位</span>
                    </div>
                    <span class="meta-desc">个人信息主卡片与核心资料位</span>
                  </div>
                </div>

                <!-- 2. 中尺寸 (48px) -->
                <div class="preview-item-row">
                  <div
                      class="avatar-shimmer-ring preview-ring-medium"
                      :class="previewRingClass"
                  >
                    <el-avatar
                        :src="displayAvatar"
                        class="preview-avatar-img"
                    />
                  </div>
                  <div class="preview-item-meta">
                    <div class="meta-headline">
                      <span class="meta-title">管理与列表表格</span>
                      <span class="preview-badge badge-medium">48px · 表格行内</span>
                    </div>
                    <span class="meta-desc">用户管理、权限管理表格行内头像</span>
                  </div>
                </div>

                <!-- 3. 小尺寸 (34px) -->
                <div class="preview-item-row">
                  <div
                      class="avatar-shimmer-ring preview-ring-small"
                      :class="previewRingClass"
                  >
                    <el-avatar
                        :src="displayAvatar"
                        class="preview-avatar-img"
                    />
                  </div>
                  <div class="preview-item-meta">
                    <div class="meta-headline">
                      <span class="meta-title">顶栏导航菜单</span>
                      <span class="preview-badge badge-small">34px · 快捷下拉</span>
                    </div>
                    <span class="meta-desc">顶部 Header 快捷下拉与系统导航头像</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="field-help" style="margin-top: 14px;">
            新注册或未自定义头像的账号将统一呈现该默认头像。如调整了截取区域，请点击【应用并上传】，再点击右上角【保存全部配置】使配置永久生效。
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, Refresh, Check, Coin, Cpu, CircleCheckFilled, Avatar, Upload, View, ArrowDown, ZoomIn, ZoomOut, Rank } from '@element-plus/icons-vue'
import defaultAvatar from '@/assets/avatar.jpg'
import request from '@/utils/request.js'

const loading = ref(false)
const saving = ref(false)
const testingRate = ref(false)
const rateTestResult = ref('')

const cropperCanvasRef = ref(null)
const previewRole = ref('user')
const zoom = ref(1)
const panX = ref(0)
const panY = ref(0)
const isCroppedModified = ref(false)
const uploadingAvatar = ref(false)
const croppedDataUrl = ref('')

const roleNameMap = {
  user: '普通用户 (星界蓝)',
  admin: '管理员 (翡翠绿)',
  super: '超级管理员 (鎏金)'
}

const previewRingClass = computed(() => {
  if (previewRole.value === 'super') return 'ring-super-admin'
  if (previewRole.value === 'admin') return 'ring-admin'
  return 'ring-user'
})

const handleRoleCommand = (cmd) => {
  previewRole.value = cmd
}

const displayAvatar = computed(() => {
  return croppedDataUrl.value || form.default_user_avatar || defaultAvatar
})

let activeImg = null
let baseScale = 1
let isDragging = false
let dragStartX = 0
let dragStartY = 0
let panStartX = 0
let panStartY = 0
let exportCanvas = null

const CANVAS_SIZE = 220
const EXPORT_SIZE = 256

const initCropperImage = (src) => {
  if (!src) src = defaultAvatar
  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.onload = () => {
    activeImg = img
    baseScale = Math.max(CANVAS_SIZE / img.width, CANVAS_SIZE / img.height)
    zoom.value = 1
    panX.value = 0
    panY.value = 0
    drawCropper()
  }
  img.onerror = () => {
    if (src !== defaultAvatar) {
      initCropperImage(defaultAvatar)
    }
  }
  img.src = src
}

const drawCropper = () => {
  const canvas = cropperCanvasRef.value
  if (!canvas || !activeImg) return
  const ctx = canvas.getContext('2d')

  const s = baseScale * zoom.value
  const dw = activeImg.width * s
  const dh = activeImg.height * s

  // 边界约束：禁止露白，锁定正方形 1:1 区域完全被图片覆盖
  const maxPanX = Math.max(0, (dw - CANVAS_SIZE) / 2)
  const maxPanY = Math.max(0, (dh - CANVAS_SIZE) / 2)
  panX.value = Math.max(-maxPanX, Math.min(maxPanX, panX.value))
  panY.value = Math.max(-maxPanY, Math.min(maxPanY, panY.value))

  const dx = (CANVAS_SIZE - dw) / 2 + panX.value
  const dy = (CANVAS_SIZE - dh) / 2 + panY.value

  // 1. 清空主画布并绘制图片
  ctx.clearRect(0, 0, CANVAS_SIZE, CANVAS_SIZE)
  ctx.drawImage(activeImg, dx, dy, dw, dh)

  // 2. 绘制 1:1 锁定正方形比例辅助九宫格
  ctx.save()
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.28)'
  ctx.lineWidth = 1
  ctx.setLineDash([4, 4])
  const step = CANVAS_SIZE / 3
  ctx.beginPath()
  ctx.moveTo(step, 0); ctx.lineTo(step, CANVAS_SIZE)
  ctx.moveTo(step * 2, 0); ctx.lineTo(step * 2, CANVAS_SIZE)
  ctx.moveTo(0, step); ctx.lineTo(CANVAS_SIZE, step)
  ctx.moveTo(0, step * 2); ctx.lineTo(CANVAS_SIZE, step * 2)
  ctx.stroke()

  // 3. 绘制四角高亮瞄准标
  ctx.setLineDash([])
  ctx.strokeStyle = '#3b82f6'
  ctx.lineWidth = 2.5
  const cornerLen = 14
  ctx.beginPath()
  ctx.moveTo(0, cornerLen); ctx.lineTo(0, 0); ctx.lineTo(cornerLen, 0)
  ctx.moveTo(CANVAS_SIZE - cornerLen, 0); ctx.lineTo(CANVAS_SIZE, 0); ctx.lineTo(CANVAS_SIZE, cornerLen)
  ctx.moveTo(CANVAS_SIZE, CANVAS_SIZE - cornerLen); ctx.lineTo(CANVAS_SIZE, CANVAS_SIZE); ctx.lineTo(CANVAS_SIZE - cornerLen, CANVAS_SIZE)
  ctx.moveTo(cornerLen, CANVAS_SIZE); ctx.lineTo(0, CANVAS_SIZE); ctx.lineTo(0, CANVAS_SIZE - cornerLen)
  ctx.stroke()
  ctx.restore()

  // 4. 同步无网格高清裁切结果至 offscreen canvas，供右侧大中小预览实时联动
  if (!exportCanvas) {
    exportCanvas = document.createElement('canvas')
    exportCanvas.width = EXPORT_SIZE
    exportCanvas.height = EXPORT_SIZE
  }
  const expCtx = exportCanvas.getContext('2d')
  expCtx.clearRect(0, 0, EXPORT_SIZE, EXPORT_SIZE)
  const ratio = EXPORT_SIZE / CANVAS_SIZE
  const expDw = dw * ratio
  const expDh = dh * ratio
  const expDx = (EXPORT_SIZE - expDw) / 2 + panX.value * ratio
  const expDy = (EXPORT_SIZE - expDh) / 2 + panY.value * ratio
  expCtx.drawImage(activeImg, expDx, expDy, expDw, expDh)
  croppedDataUrl.value = exportCanvas.toDataURL('image/jpeg', 0.92)
}

// 鼠标拖拽平移
const onCanvasMouseDown = (e) => {
  isDragging = true
  dragStartX = e.clientX
  dragStartY = e.clientY
  panStartX = panX.value
  panStartY = panY.value
}
const onCanvasMouseMove = (e) => {
  if (!isDragging) return
  panX.value = panStartX + (e.clientX - dragStartX)
  panY.value = panStartY + (e.clientY - dragStartY)
  isCroppedModified.value = true
  drawCropper()
}
const onCanvasMouseUp = () => {
  isDragging = false
}

// 触摸屏拖拽
const onCanvasTouchStart = (e) => {
  if (e.touches.length === 1) {
    isDragging = true
    dragStartX = e.touches[0].clientX
    dragStartY = e.touches[0].clientY
    panStartX = panX.value
    panStartY = panY.value
  }
}
const onCanvasTouchMove = (e) => {
  if (!isDragging || e.touches.length !== 1) return
  panX.value = panStartX + (e.touches[0].clientX - dragStartX)
  panY.value = panStartY + (e.touches[0].clientY - dragStartY)
  isCroppedModified.value = true
  drawCropper()
}
const onCanvasTouchEnd = () => {
  isDragging = false
}

// 滚轮缩放
const onCanvasWheel = (e) => {
  const delta = e.deltaY > 0 ? -0.05 : 0.05
  zoom.value = Math.max(1, Math.min(3, Number((zoom.value + delta).toFixed(2))))
  isCroppedModified.value = true
  drawCropper()
}

const zoomIn = () => {
  zoom.value = Math.min(3, Number((zoom.value + 0.1).toFixed(2)))
  isCroppedModified.value = true
  drawCropper()
}
const zoomOut = () => {
  zoom.value = Math.max(1, Number((zoom.value - 0.1).toFixed(2)))
  isCroppedModified.value = true
  drawCropper()
}

// 选择本地图片
const onFileSelected = (uploadFile) => {
  const file = uploadFile.raw
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择有效的图片文件')
    return
  }
  const reader = new FileReader()
  reader.onload = (e) => {
    initCropperImage(e.target.result)
    isCroppedModified.value = true
    ElMessage.info('图片已载入截取框，可拖拽并调整缩放以微调头像')
  }
  reader.readAsDataURL(file)
}

// 确认并上传截取后的头像
const applyAndUploadCropped = async () => {
  if (!exportCanvas) return
  uploadingAvatar.value = true
  exportCanvas.toBlob(async (blob) => {
    if (!blob) {
      uploadingAvatar.value = false
      return
    }
    try {
      const formData = new FormData()
      formData.append('file', blob, 'default-avatar.jpg')
      const res = await request.post('/files/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      if (res.code === 200 && res.data) {
        form.default_user_avatar = res.data
        isCroppedModified.value = false
        ElMessage.success('默认头像已成功截取并上传！请点击右上角【保存全部配置】使设置永久生效')
      } else {
        ElMessage.error(res.msg || '上传截取头像失败')
      }
    } catch (err) {
      ElMessage.error('上传头像发生网络异常')
    } finally {
      uploadingAvatar.value = false
    }
  }, 'image/jpeg', 0.92)
}

// 恢复内置系统默认头像
const resetToDefaultAvatar = () => {
  form.default_user_avatar = ''
  isCroppedModified.value = false
  initCropperImage(defaultAvatar)
  ElMessage.success('已恢复为系统内置默认头像，请点击右上角【保存全部配置】使设置永久生效')
}

const form = reactive({
  finexly_api_key: '',
  deepseek_base_url: 'https://api.deepseek.com',
  default_user_avatar: ''
})

const handleDefaultAvatarSuccess = (res) => {
  if (res.code === 200 && res.data) {
    form.default_user_avatar = res.data
    ElMessage.success('头像上传成功，请点击上方保存全部配置')
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

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
  if (isCroppedModified.value) {
    await applyAndUploadCropped()
  }
  saving.value = true
  try {
    const res = await request.put('/siteConfig/saveBatch', {
      finexly_api_key: form.finexly_api_key,
      deepseek_base_url: form.deepseek_base_url || 'https://api.deepseek.com',
      default_user_avatar: form.default_user_avatar || ''
    })
    if (res.code === 200) {
      ElMessage.success('网站公共配置已加密保存！')
      await loadConfigs()
  initCropperImage(form.default_user_avatar || defaultAvatar)
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

.avatar-editor-layout {
  display: flex;
  align-items: flex-start;
  gap: 18px;
  flex-wrap: wrap;
  padding: 14px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  width: 100%;
  box-sizing: border-box;
}

.cropper-column {
  flex: 0 0 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.cropper-box-wrap {
  width: 220px;
  height: 220px;
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.15);
  background: #0b1120;
}

.cropper-canvas {
  width: 220px;
  height: 220px;
  display: block;
  cursor: grab;
}

.cropper-canvas:active {
  cursor: grabbing;
}

.cropper-hint-badge {
  position: absolute;
  bottom: 6px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(4px);
  color: #e2e8f0;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 999px;
  pointer-events: none;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 4px;
}

.cropper-zoom-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
}

.zoom-icon-btn {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: color 0.2s;
}

.zoom-icon-btn:hover {
  color: var(--el-color-primary);
}

.zoom-slider {
  flex: 1;
}

.zoom-val-text {
  font-size: 11px;
  font-family: monospace;
  color: var(--el-text-color-secondary);
  width: 32px;
  text-align: right;
}

.cropper-btn-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.previews-column {
  flex: 1;
  min-width: 240px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.previews-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.previews-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.title-icon {
  color: var(--el-color-primary);
  font-size: 16px;
}

.role-switch-dropdown-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 6px;
}

.role-swatch-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.swatch-user {
  background: #2563eb;
  box-shadow: 0 0 6px rgba(37, 99, 235, 0.6);
}

.swatch-admin {
  background: #059669;
  box-shadow: 0 0 6px rgba(5, 150, 105, 0.6);
}

.swatch-super {
  background: #d97706;
  box-shadow: 0 0 6px rgba(217, 119, 6, 0.6);
}

.dropdown-role-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.previews-vertical-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-item-row {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 10px 14px;
  width: 100%;
  box-sizing: border-box;
  transition: all 0.22s ease;
}

.preview-item-row:hover {
  border-color: rgba(var(--theme-rgb), 0.35);
  box-shadow: 0 3px 12px -3px rgba(var(--theme-rgb), 0.12);
  transform: translateX(2px);
}

.preview-ring-large {
  width: calc(80px * var(--app-font-scale, 1)) !important;
  height: calc(80px * var(--app-font-scale, 1)) !important;
  min-width: calc(80px * var(--app-font-scale, 1)) !important;
  min-height: calc(80px * var(--app-font-scale, 1)) !important;
  max-width: calc(80px * var(--app-font-scale, 1)) !important;
  max-height: calc(80px * var(--app-font-scale, 1)) !important;
  padding: 3px !important;
}

.preview-ring-medium {
  width: calc(48px * var(--app-font-scale, 1)) !important;
  height: calc(48px * var(--app-font-scale, 1)) !important;
  min-width: calc(48px * var(--app-font-scale, 1)) !important;
  min-height: calc(48px * var(--app-font-scale, 1)) !important;
  max-width: calc(48px * var(--app-font-scale, 1)) !important;
  max-height: calc(48px * var(--app-font-scale, 1)) !important;
  padding: 2.5px !important;
}

.preview-ring-small {
  width: calc(34px * var(--app-font-scale, 1)) !important;
  height: calc(34px * var(--app-font-scale, 1)) !important;
  min-width: calc(34px * var(--app-font-scale, 1)) !important;
  min-height: calc(34px * var(--app-font-scale, 1)) !important;
  max-width: calc(34px * var(--app-font-scale, 1)) !important;
  max-height: calc(34px * var(--app-font-scale, 1)) !important;
  padding: 2px !important;
}

.preview-avatar-img {
  width: 100% !important;
  height: 100% !important;
  border-radius: 50% !important;
  display: block !important;
}

.preview-item-meta {
  display: flex;
  flex-direction: column;
  gap: 3px;
  flex: 1;
}

.meta-headline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
}

.preview-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 1.5px 8px;
  border-radius: 999px;
  letter-spacing: 0.02em;
}

.badge-large {
  background: rgba(99, 102, 241, 0.12);
  color: #6366f1;
}

html.dark .badge-large {
  background: rgba(99, 102, 241, 0.22);
  color: #818cf8;
}

.badge-medium {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}

html.dark .badge-medium {
  background: rgba(16, 185, 129, 0.22);
  color: #34d399;
}

.badge-small {
  background: rgba(245, 158, 11, 0.12);
  color: #d97706;
}

html.dark .badge-small {
  background: rgba(245, 158, 11, 0.22);
  color: #fbbf24;
}

.meta-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.meta-desc {
  font-size: 11.5px;
  color: var(--el-text-color-secondary);
  line-height: 1.3;
}
</style>