<template>
  <el-popover
      :placement="placement"
      :width="318"
      trigger="click"
      popper-class="contact-support-popover"
  >
    <template #reference>
      <!-- 模式 1：低调精致的角落耳机图标按钮（用于登录/注册界面角落） -->
      <button
          v-if="variant === 'corner'"
          type="button"
          class="contact-corner-btn"
          title="联系我们 · 客户支持与申诉中心"
      >
        <span class="headset-icon-ring">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 18v-6a9 9 0 0 1 18 0v6" />
            <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z" />
          </svg>
        </span>
        <span class="corner-btn-label">{{ buttonText || '联系我们' }}</span>
      </button>

      <!-- 模式 2：内嵌于封禁通知气泡右侧的胶囊按钮 -->
      <button
          v-else
          type="button"
          class="contact-banner-btn"
      >
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 18v-6a9 9 0 0 1 18 0v6" />
          <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z" />
        </svg>
        <span>{{ buttonText || '联系我们' }}</span>
      </button>
    </template>

    <!-- 弹出气泡卡片内容 -->
    <div class="support-card-body">
      <div class="support-card-head">
        <div class="support-badge">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 18v-6a9 9 0 0 1 18 0v6" />
            <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z" />
          </svg>
        </div>
        <div class="support-head-meta">
          <div class="support-title">星界 · 客户支持与安全申诉</div>
          <div class="support-sub">Star Realm Support &amp; Appeal Desk</div>
        </div>
      </div>

      <div class="support-items">
        <div class="support-row" @click="copyField('starrealm@contact-us.com', '官方邮箱')">
          <div class="row-icon">
            <el-icon><Message /></el-icon>
          </div>
          <div class="row-content">
            <span class="row-label">官方联系与申诉邮箱（点击复制）</span>
            <span class="row-value num-font highlight">starrealm@contact-us.com</span>
          </div>
        </div>

        <div class="support-row" @click="copyField('+86 (021) 5089-7720', '客服与传真电话')">
          <div class="row-icon">
            <el-icon><Phone /></el-icon>
          </div>
          <div class="row-content">
            <span class="row-label">客服直线 / 传真电话（点击复制）</span>
            <span class="row-value num-font">+86 (021) 5089-7720 / FAX-7729</span>
          </div>
        </div>

        <div class="support-row static">
          <div class="row-icon">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="row-content">
            <span class="row-label">人工受理工作时间（周一至周五）</span>
            <span class="row-value num-font">09:00 - 12:00 ， 14:00 - 18:00</span>
          </div>
        </div>
      </div>

      <div class="support-foot-note">
        如对 AI 对话权限限制或账号安全策略有疑问，请发送您的用户名与说明至上述邮箱，我们将在工作时间内优先核实处理。
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { Message, Phone, Clock } from '@element-plus/icons-vue'

defineProps({
  variant: {
    type: String,
    default: 'corner' // 'corner' | 'banner'
  },
  placement: {
    type: String,
    default: 'top-end'
  },
  buttonText: {
    type: String,
    default: '联系我们'
  }
})

const copyField = async (val, label) => {
  try {
    await navigator.clipboard.writeText(val)
    ElMessage.success(`${label}已复制：${val}`)
  } catch {
    ElMessage.info(`${label}：${val}`)
  }
}
</script>

<style scoped>
/* 模式 1：角落低调精致的耳机云母胶囊按钮 */
.contact-corner-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 13px 6px 8px;
  border-radius: 999px;
  border: 1px solid rgba(var(--theme-rgb), 0.26);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px) saturate(180%);
  -webkit-backdrop-filter: blur(18px) saturate(180%);
  color: var(--el-text-color-regular, #334155);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 22px -6px rgba(15, 23, 42, 0.14);
  transition: all 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}

html.dark .contact-corner-btn {
  background: rgba(15, 23, 42, 0.76);
  border-color: rgba(var(--theme-rgb), 0.35);
  color: #e2e8f0;
  box-shadow: 0 10px 26px -6px rgba(0, 0, 0, 0.55);
}

.contact-corner-btn:hover {
  transform: translateY(-2px);
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  box-shadow: 0 12px 26px -6px rgba(var(--theme-rgb), 0.28);
}

.headset-icon-ring {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(var(--theme-rgb), 0.18), rgba(var(--theme-rgb), 0.08));
  color: var(--el-color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;
}

.contact-corner-btn:hover .headset-icon-ring {
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary));
  color: #ffffff;
}

/* 模式 2：内嵌于封禁通知气泡的联系我们按钮 */
.contact-banner-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid rgba(var(--theme-rgb), 0.38);
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary));
  color: #ffffff;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  box-shadow: 0 6px 16px -4px rgba(var(--theme-rgb), 0.4);
  transition: all 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}

.contact-banner-btn:hover {
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 8px 20px -3px rgba(var(--theme-rgb), 0.55);
  filter: brightness(1.06);
}

/* 气泡内部排版 */
.support-card-body {
  padding: 2px;
  user-select: text;
  -webkit-user-select: text;
}

.support-card-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px solid var(--table-border-color, rgba(148, 163, 184, 0.22));
}

.support-badge {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary));
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 6px 14px -3px rgba(var(--theme-rgb), 0.35);
}

.support-head-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.support-title {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.support-sub {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.support-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.support-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(var(--theme-rgb), 0.05);
  border: 1px solid rgba(var(--theme-rgb), 0.14);
  cursor: pointer;
  transition: all 0.2s ease;
}

.support-row.static {
  cursor: default;
}

.support-row:not(.static):hover {
  background: rgba(var(--theme-rgb), 0.12);
  border-color: rgba(var(--theme-rgb), 0.35);
  transform: translateX(2px);
}

.row-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(var(--theme-rgb), 0.12);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.row-content {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.row-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.row-value {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.row-value.highlight {
  color: var(--el-color-primary);
}

.support-foot-note {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed rgba(148, 163, 184, 0.28);
  font-size: 11.5px;
  line-height: 1.55;
  color: var(--el-text-color-secondary);
}

.contact-corner-btn {
  height: calc(36px * var(--app-font-scale, 1)) !important;
  padding: 0 calc(14px * var(--app-font-scale, 1)) 0 calc(8px * var(--app-font-scale, 1)) !important;
  font-size: calc(12.5px * var(--app-font-scale, 1)) !important;
  gap: calc(7px * var(--app-font-scale, 1)) !important;
}

.headset-icon-ring {
  width: calc(26px * var(--app-font-scale, 1)) !important;
  height: calc(26px * var(--app-font-scale, 1)) !important;
}

.headset-icon-ring svg {
  width: calc(17px * var(--app-font-scale, 1)) !important;
  height: calc(17px * var(--app-font-scale, 1)) !important;
}

.contact-banner-btn {
  height: calc(32px * var(--app-font-scale, 1)) !important;
  padding: 0 calc(14px * var(--app-font-scale, 1)) !important;
  font-size: calc(12.5px * var(--app-font-scale, 1)) !important;
  gap: calc(6px * var(--app-font-scale, 1)) !important;
}

.contact-banner-btn svg {
  width: calc(15px * var(--app-font-scale, 1)) !important;
  height: calc(15px * var(--app-font-scale, 1)) !important;
}

.support-badge {
  width: calc(36px * var(--app-font-scale, 1)) !important;
  height: calc(36px * var(--app-font-scale, 1)) !important;
}

.support-badge svg {
  width: calc(18px * var(--app-font-scale, 1)) !important;
  height: calc(18px * var(--app-font-scale, 1)) !important;
}

.support-title {
  font-size: calc(13.5px * var(--app-font-scale, 1)) !important;
}

.support-sub {
  font-size: calc(11px * var(--app-font-scale, 1)) !important;
}

.row-icon {
  width: calc(28px * var(--app-font-scale, 1)) !important;
  height: calc(28px * var(--app-font-scale, 1)) !important;
}

.row-icon .el-icon {
  font-size: calc(15px * var(--app-font-scale, 1)) !important;
}

.row-label {
  font-size: calc(11px * var(--app-font-scale, 1)) !important;
}

.row-value {
  font-size: calc(12.5px * var(--app-font-scale, 1)) !important;
}

.support-foot-note {
  font-size: calc(11.5px * var(--app-font-scale, 1)) !important;
}

</style>
