<template>
  <div class="ai-analysis-stage">
    <!-- ================= 主舞台容器（当弹出云母饼图悬浮窗时，自动给后方舞台加上高斯模糊与微缩景深，且完全不中断流式输出） ================= -->
    <div class="stage-main-layer" :class="{ 'is-blurred-behind': showChartOverlay && hasStartedChat }">
      <!-- 顶部云母模型状态栏 -->
      <header class="stage-header stagger-1">
        <div class="header-brand">
          <div class="ds-glow-badge">
            <el-icon :size="20"><Cpu /></el-icon>
          </div>
          <div class="model-meta">
            <div class="model-title-row">
              <span class="model-name">Deepseek V4.1 Flash</span>
              <span class="model-build num-font">1017</span>
              <span class="scene-tag">智能账单分析</span>
            </div>
            <div class="conn-status-pill" @click="checkConnectivity" title="点击刷新服务状态">
              <span class="status-dot" :class="connStatus.state"></span>
              <span class="status-label">
                {{ isStreaming ? '正在生成财务诊断分析...' : connStatus.message }}
              </span>
            </div>
          </div>
        </div>

        <div class="header-actions">
          <el-tooltip content="检查服务状态" placement="bottom">
            <el-button circle size="small" @click="checkConnectivity" :loading="connStatus.checking">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
          <el-button
              v-if="hasStartedChat"
              size="small"
              type="primary"
              @click="startNewSession"
          >
            <el-icon style="margin-right: 4px"><Plus /></el-icon>开启新对话
          </el-button>
          <el-button
              v-if="hasStartedChat"
              size="small"
              plain
              @click="resetToInitialChart"
          >
            <el-icon style="margin-right: 4px"><PieChart /></el-icon>返回收支看板
          </el-button>
          <el-button
              size="small"
              type="success"
              plain
              @click="showHistoryDrawer = true"
          >
            <el-icon style="margin-right: 4px"><Collection /></el-icon>历史对话存档 ({{ chatHistoryList.length }})
          </el-button>
          <el-button
              size="small"
              type="primary"
              plain
              @click="router.push('/main/stats')"
          >
            <el-icon style="margin-right: 4px"><Key /></el-icon>API 密钥设置
          </el-button>
        </div>
      </header>

      <!-- 中央可滚动内容区：未开始对话时展示双饼图看板，开始对话后平滑收起至左下角并展示流式对话流 -->
      <div ref="chatViewportRef" class="stage-viewport" @scroll="handleViewportScroll">
        <!-- 初始状态：居中展示收支分类双饼图，开始对话时触发向输入框左下角收缩的动画 -->
        <Transition name="chart-dock-collapse">
          <div v-show="!hasStartedChat" class="initial-charts-hero stagger-2">
            <div class="hero-intro">
              <h3 class="hero-heading">
                <el-icon class="heading-icon"><PieChart /></el-icon>
                <span>您的实时收支结构全景</span>
              </h3>
              <p class="hero-sub">
                AI 顾问将结合您的当期收支与资产结构，为您提供定制化财务诊断与预算建议。
              </p>
            </div>

            <el-row :gutter="20" class="charts-grid">
              <el-col :xs="24" :md="12">
                <div class="mica-chart-box">
                  <div ref="inlinePie1Ref" class="echarts-box"></div>
                </div>
              </el-col>
              <el-col :xs="24" :md="12">
                <div class="mica-chart-box">
                  <div ref="inlinePie2Ref" class="echarts-box"></div>
                </div>
              </el-col>
            </el-row>

            <!-- 快捷提问引导卡片（采用 Ant Design 矢量图标替代 Emoji） -->
            <div class="starter-prompts">
              <div
                  v-for="item in quickStarters"
                  :key="item.title"
                  class="starter-card"
                  @click="useQuickStarter(item.prompt)"
              >
                <div class="starter-icon-box">
                  <el-icon :size="20"><component :is="item.iconComp" /></el-icon>
                </div>
                <div class="starter-text">
                  <div class="starter-title">{{ item.title }}</div>
                  <div class="starter-desc">{{ item.desc }}</div>
                </div>
              </div>
            </div>
          </div>
        </Transition>

        <!-- 开始聊天后：流式分析对话列表 -->
        <Transition name="chat-stream-fade">
          <div v-if="hasStartedChat" class="analysis-chat-stream">
            <div
                v-for="(msg, idx) in chatList"
                :key="idx"
                class="stream-item"
                :class="msg.role === 'user' ? 'is-user' : 'is-ai'"
            >
              <!-- AI 消息：上方头像 + 右侧跑马灯流光渐变状态词 -->
              <template v-if="msg.role === 'assistant'">
                <div class="ai-message-block">
                  <div class="ai-head-row">
                    <div class="ai-badge-icon" :class="{ 'is-active-glow': msg.waitingFirstToken || msg.streaming }">
                      <el-icon :size="18"><Service /></el-icon>
                    </div>
                    <div class="ai-head-info">
                      <span class="ai-sender-name">Deepseek V4.1 Flash</span>
                      <!-- 思考与生成过程中的从左至右跑马灯式主题色明暗渐变状态词 -->
                      <span v-if="msg.waitingFirstToken || msg.streaming" class="shimmer-status-pill">
                        <span class="shimmer-dot"></span>
                        <span class="shimmer-marquee-text">
                          {{ msg.waitingFirstToken ? '深度思考中……' : '正在生成……' }}
                        </span>
                      </span>
                      <span v-else class="ai-done-tag">分析完成 · {{ msg.time }}</span>
                    </div>
                  </div>

                  <div class="stream-bubble-col">
                    <!-- 等待首个流式 Token 时的类 Telegram 对方正在输入动画 -->
                    <div v-if="msg.waitingFirstToken" class="tg-typing-box">
                      <div class="tg-dots">
                        <span></span>
                        <span></span>
                        <span></span>
                      </div>
                      <span class="tg-text shimmer-marquee-text">正在结合您的实时收支分类数据深度思考中……</span>
                    </div>

                    <!-- 正常消息或流式输出中的 Markdown 报告卡片 -->
                    <div
                        v-else
                        class="stream-card"
                    >
                      <div class="report-meta-bar">
                        <span class="meta-left">
                          <el-icon><DataLine /></el-icon>
                          <span>基于本账户实时收支分类数据生成</span>
                        </span>
                        <span class="num-font">{{ msg.time }}</span>
                      </div>
                      <div
                          class="markdown-body"
                          @click="handleMarkdownBlockCopy($event)"
                          v-html="renderMessageHtml(msg)"
                      ></div>
                      <!-- 每条 AI 消息底部独立小工具栏：仅在流式生成完全结束后才显示，包含复制、完成时间与免责提示语 -->
                      <div v-if="!msg.waitingFirstToken && !msg.streaming" class="ai-msg-footer-bar">
                        <div class="ai-footer-left">
                          <button type="button" class="ai-msg-action-btn" @click="copyFullMessage(msg.content)">
                            <el-icon><CopyDocument /></el-icon>
                            <span>复制</span>
                          </button>
                          <span class="ai-footer-divider"></span>
                          <span class="ai-footer-time num-font">
                            <el-icon style="vertical-align: -2px; margin-right: 3px"><Clock /></el-icon>生成时间：{{ msg.time }}
                          </span>
                        </div>
                        <div class="ai-footer-right">
                          <span class="ai-disclaimer-badge">
                            <el-icon style="vertical-align: -2px; margin-right: 3px"><InfoFilled /></el-icon>AI可能会犯错，仅供参考
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </template>

              <!-- 用户提问气泡 -->
              <template v-else>
                <div class="stream-bubble-col">
                  <div class="stream-card">
                    <div class="user-query-text">{{ msg.content }}</div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </Transition>
      </div>
    </div>

    <!-- ================= 历史对话存档抽屉 ================= -->
    <el-drawer
        v-model="showHistoryDrawer"
        title="历史账单分析存档"
        direction="rtl"
        size="400px"
        destroy-on-close
    >
      <div class="history-drawer-body">
        <div v-if="chatHistoryList.length === 0" class="history-empty">
          <el-icon :size="36" style="color: var(--el-text-color-secondary)"><Collection /></el-icon>
          <p>暂无历史对话存档</p>
          <span class="history-empty-sub">每次清空或重新开始对话时会自动存档当前会话</span>
        </div>
        <div v-else class="history-list">
          <div
              v-for="hist in chatHistoryList"
              :key="hist.id"
              class="history-item-card"
              @click="loadHistorySession(hist)"
          >
            <div class="hist-card-head">
              <span class="hist-title">{{ hist.title }}</span>
              <el-button type="danger" link size="small" @click="deleteHistorySession(hist.id, $event)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <div class="hist-time num-font">存档时间：{{ hist.time }}</div>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- ================= 云母饼图悬浮窗（开启时后方流式对话内容自动高斯模糊，且流式传输在后台继续实时更新） ================= -->
    <Transition name="mica-modal-pop">
      <div
          v-if="showChartOverlay && hasStartedChat"
          class="chart-overlay-backdrop"
          @click.self="showChartOverlay = false"
      >
        <div class="mica-floating-window">
          <div class="floating-window-header">
            <div class="window-title-wrap">
              <span class="window-dot"></span>
              <span class="window-title">实时账户收支结构饼图</span>
              <span v-if="isStreaming" class="bg-streaming-pill">
                <span class="mini-pulse"></span>
                后台流式输出进行中...
              </span>
            </div>
            <el-button circle size="small" @click="showChartOverlay = false">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>

          <el-row :gutter="18" class="floating-charts-row">
            <el-col :xs="24" :md="12">
              <div class="floating-chart-pane">
                <div ref="modalPie1Ref" class="echarts-modal-box"></div>
              </div>
            </el-col>
            <el-col :xs="24" :md="12">
              <div class="floating-chart-pane">
                <div ref="modalPie2Ref" class="echarts-modal-box"></div>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
    </Transition>

    <!-- ================= 子页面底部正中央悬浮云母输入坞 (Floating Dock) ================= -->
    <div class="floating-dock-wrapper">
      <!-- 当用户的 AI 对话功能被封禁时，在输入框正上方展示不可关闭的输入框同款等宽云母气泡（含小标题、Icon、封禁用语、封禁原因、封禁时间、封禁时长与联系我们按钮） -->
      <Transition name="ban-banner-pop">
        <div v-if="aiBanState.banned" class="mica-ban-notice-bar">
          <div class="ban-notice-main">
            <div class="ban-shield-icon">
              <el-icon :size="20"><Lock /></el-icon>
            </div>
            <div class="ban-notice-text">
              <div class="ban-headline-row">
                <span class="ban-headline">AI 智能对话功能已受限</span>
                <span class="ban-level-pill num-font">
                  封禁时长：{{ aiBanState.banDuration || '24小时' }}
                </span>
              </div>
              <div class="ban-statement">
                因检测到您的账户存在违反
                <button type="button" class="legal-inline-link" @click="openLegalDoc('agreement')">《星界用户服务协议》</button>
                的提问行为，当前 AI 智能对话功能已暂停使用。
              </div>
              <div class="ban-sub-reason">
                封禁原因：{{ aiBanState.banReason || '多次发送违反《星界用户服务协议》的受限或敏感内容' }}
              </div>
              <div class="ban-meta-row num-font">
                <span><el-icon style="vertical-align: -2px; margin-right: 3px"><Clock /></el-icon>封禁时间：{{ aiBanState.banTime || '刚刚' }}</span>
                <span class="meta-divider">|</span>
                <span>封禁时长：<strong>{{ aiBanState.banDuration || '24小时' }}</strong></span>
                <span class="meta-divider" v-if="aiBanState.banExpireTime">|</span>
                <span v-if="aiBanState.banExpireTime">预计解封：{{ aiBanState.banExpireTime }}</span>
              </div>
            </div>
          </div>

          <div class="ban-notice-right">
            <ContactSupportPopover
                variant="banner"
                placement="top-end"
                button-text="联系我们"
            />
          </div>
        </div>
      </Transition>

      <div class="mica-composer-dock" :class="{ 'is-focused': inputFocused, 'is-disabled-dock': aiBanState.banned }">
        <!-- 自适应多行输入框 -->
        <el-input
            v-model="data.input"
            type="textarea"
            :disabled="aiBanState.banned"
            :autosize="{ minRows: 2, maxRows: 5 }"
            :placeholder="aiBanState.banned ? '您的 AI 对话权限已被限制，暂无法发送消息，请点击上方「联系我们」申诉解封...' : '请输入您想咨询的财务问题 (Enter 发送)...'"
            class="dock-textarea"
            @focus="inputFocused = true"
            @blur="inputFocused = false"
            @keydown.enter.exact.prevent="analyze"
        />

        <!-- 输入框底部内嵌工具栏：左下角收纳后的饼图开关按钮 + 右下角镶嵌发送按钮 -->
        <div class="dock-inner-toolbar">
          <div class="dock-bottom-left">
            <Transition name="mini-chart-pill-pop">
              <button
                  v-if="hasStartedChat"
                  type="button"
                  class="chart-dock-trigger"
                  :class="{ active: showChartOverlay }"
                  @click="toggleChartOverlay"
              >
                <el-icon :size="14"><PieChart /></el-icon>
                <span>{{ showChartOverlay ? '收起收支饼图' : '查看收支饼图' }}</span>
                <span class="pill-count num-font">{{ data.incomeList.length + data.payList.length }}项</span>
              </button>
            </Transition>
            <span v-if="!hasStartedChat" class="dock-hint">
              <el-icon><InfoFilled /></el-icon>
              <span>{{ aiBanState.banned ? '您的账单记账与图表功能仍可正常使用' : '提问后上方饼图将自动收纳至此，随时点击呼出' }}</span>
            </span>
          </div>

          <div class="dock-bottom-right">
            <el-button
                type="primary"
                class="embedded-send-btn"
                :loading="isStreaming"
                :disabled="aiBanState.banned || (!data.input.trim() && !isStreaming)"
                @click="analyze"
            >
              <span>{{ aiBanState.banned ? '已受限' : (isStreaming ? '思考中' : '发送分析') }}</span>
              <el-icon v-if="!isStreaming && !aiBanState.banned" style="margin-left: 5px"><Promotion /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 与登录/注册页同款的《星界用户服务协议》与《隐私保护政策》悬浮窗 -->
    <LegalAgreementDialog
        v-model="legalDialogVisible"
        v-model:activeTab="legalActiveTab"
        @agree="legalDialogVisible = false"
    />

    <!-- 误判反馈 / 联系我们弹窗（当点击拦截提示中的“提交反馈”链接时呼出） -->
    <el-dialog
        v-model="feedbackDialogVisible"
        title="提交反馈 · 客户支持与申诉"
        width="400px"
        destroy-on-close
    >
      <div class="feedback-dialog-body">
        <p class="feedback-tip">
          如果您认为刚才的提示词拦截属于误判，或对 AI 对话限制有疑问，请通过以下方式联系我们：
        </p>
        <div class="feedback-info-box">
          <div class="feedback-row">
            <span class="fb-label">官方邮箱：</span>
            <span class="fb-val num-font">starrealm@contact-us.com</span>
          </div>
          <div class="feedback-row">
            <span class="fb-label">传真电话：</span>
            <span class="fb-val num-font">+86 (021) 5089-7720 / FAX-7729</span>
          </div>
          <div class="feedback-row">
            <span class="fb-label">工作时间：</span>
            <span class="fb-val num-font">09:00 - 12:00 ， 14:00 - 18:00</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="feedbackDialogVisible = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, watch, nextTick, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  Delete,
  Key,
  Close,
  Promotion,
  Cpu,
  Service,
  PieChart,
  DataLine,
  InfoFilled,
  Search,
  Aim,
  TrendCharts,
  CopyDocument,
  Clock,
  Collection,
  Plus,
  Lock
} from '@element-plus/icons-vue'
import { useDark } from '@vueuse/core'
import MarkdownIt from 'markdown-it'
import ContactSupportPopover from '@/components/ContactSupportPopover.vue'
import LegalAgreementDialog from '@/components/LegalAgreementDialog.vue'
import { inspectUserPromptLocally } from '@/utils/aiGuardrail.js'
import { getChartFontScale } from '@/utils/theme.js'

const router = useRouter()
const isDark = useDark()
const legalDialogVisible = ref(false)
const legalActiveTab = ref('agreement')
const feedbackDialogVisible = ref(false)

const openLegalDoc = (tab = 'agreement') => {
  legalActiveTab.value = tab
  legalDialogVisible.value = true
}
const md = new MarkdownIt({ html: false, linkify: true, typographer: true, breaks: true })

const escapeHtml = (str = '') =>
  str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')

// 自定义代码块渲染：独立顶栏 + 语言标识 + 一键复制按钮与复制成功气泡
md.renderer.rules.fence = (tokens, idx) => {
  const token = tokens[idx]
  const lang = (token.info || '').trim().split(/\s+/)[0] || 'text'
  const rawCode = token.content || ''
  return `<div class="md-block-card md-code-card">` +
    `<div class="md-block-toolbar">` +
      `<span class="md-block-tag"><span class="md-tag-dot"></span>${escapeHtml(lang.toUpperCase())}</span>` +
      `<button type="button" class="md-copy-btn" data-copy-kind="code">` +
        `<span class="md-copy-label">复制代码</span>` +
        `<span class="md-copied-popover">已复制到剪贴板</span>` +
      `</button>` +
    `</div>` +
    `<pre class="md-pre"><code>${escapeHtml(rawCode)}</code></pre>` +
  `</div>`
}

// 自定义表格渲染：独立外框容器 + 表格顶栏 + 一键复制表格按钮与复制成功气泡
md.renderer.rules.table_open = () => {
  return `<div class="md-block-card md-table-card">` +
    `<div class="md-block-toolbar">` +
      `<span class="md-block-tag"><span class="md-tag-dot"></span>数据透视表</span>` +
      `<button type="button" class="md-copy-btn" data-copy-kind="table">` +
        `<span class="md-copy-label">复制表格</span>` +
        `<span class="md-copied-popover">表格已复制</span>` +
      `</button>` +
    `</div>` +
    `<div class="md-table-scroll"><table class="md-styled-table">`
}

md.renderer.rules.table_close = () => {
  return `</table></div></div>`
}

const renderMarkdown = (txt) => md.render(txt || '')

// 处理代码块与表格的一键复制交互，以及拦截提示中的《星界用户服务协议》和「提交反馈」内嵌链接点击
const handleMarkdownBlockCopy = async (event) => {
  const linkEl = event.target?.closest?.('a')
  if (linkEl) {
    const href = linkEl.getAttribute('href') || ''
    if (href === '#xingjie-agreement') {
      event.preventDefault()
      openLegalDoc('agreement')
      return
    }
    if (href === '#xingjie-feedback') {
      event.preventDefault()
      feedbackDialogVisible.value = true
      return
    }
  }

  const btn = event.target?.closest?.('.md-copy-btn')
  if (!btn) return
  const card = btn.closest('.md-block-card')
  if (!card) return

  const kind = btn.getAttribute('data-copy-kind')
  let textToCopy = ''

  if (kind === 'code') {
    const codeEl = card.querySelector('pre code')
    textToCopy = codeEl ? codeEl.innerText : ''
  } else if (kind === 'table') {
    const rows = Array.from(card.querySelectorAll('table tr'))
    textToCopy = rows
      .map((tr) =>
        Array.from(tr.querySelectorAll('th, td'))
          .map((cell) => cell.innerText.replace(/\s+/g, ' ').trim())
          .join('\t')
      )
      .join('\n')
  }

  if (!textToCopy) return

  try {
    await navigator.clipboard.writeText(textToCopy)
    btn.classList.add('is-copied')
    const labelEl = btn.querySelector('.md-copy-label')
    const oldText = labelEl ? labelEl.textContent : ''
    if (labelEl) labelEl.textContent = '已复制 ✓'
    ElMessage.success({ message: kind === 'table' ? '表格内容已复制到剪贴板' : '代码已复制到剪贴板', duration: 1600 })
    setTimeout(() => {
      btn.classList.remove('is-copied')
      if (labelEl && oldText) labelEl.textContent = oldText
    }, 1800)
  } catch {
    ElMessage.warning('复制失败，请手动选中复制')
  }
}

/**
 * 渲染 AI 消息 HTML：当处于流式输出 (msg.streaming) 时，
 * 通过 DOM 树深度递归定位最后一个包含真实内容的叶子元素（如最后一个 <li> 文本、<td> 单元格、<code> 或 <p>），
 * 将细长针闪烁光标紧贴在正在生成的最后一个文字后面，绝不掉到列表/表格外侧底部的空行。
 */
const renderMessageHtml = (msg) => {
  const rawHtml = renderMarkdown(msg.content || '')
  if (!msg.streaming) return rawHtml
  const needleHtml = '<span class="needle-caret" aria-hidden="true"></span>'
  if (typeof document === 'undefined') {
    return rawHtml + needleHtml
  }
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = rawHtml

  const findLastContentTarget = (root) => {
    const children = Array.from(root.children || []).filter(
      (el) => !el.classList?.contains('md-block-toolbar')
    )
    for (let i = children.length - 1; i >= 0; i--) {
      const child = children[i]
      const tag = child.tagName?.toLowerCase()
      if (tag === 'hr' || tag === 'br') continue
      const deeper = findLastContentTarget(child)
      if (deeper) return deeper
      if ((child.textContent || '').trim().length > 0 || ['p', 'li', 'td', 'th', 'code', 'h1', 'h2', 'h3', 'h4'].includes(tag)) {
        return child
      }
    }
    return null
  }

  const targetEl = findLastContentTarget(tempDiv) || tempDiv
  const caretSpan = document.createElement('span')
  caretSpan.className = 'needle-caret'
  caretSpan.setAttribute('aria-hidden', 'true')
  targetEl.appendChild(caretSpan)
  return tempDiv.innerHTML
}

const data = reactive({
  user: JSON.parse(localStorage.getItem('loginToken') || '{}'),
  incomeList: [],
  payList: [],
  input: ''
})

const quickStarters = [
  {
    iconComp: markRaw(Search),
    title: '全面总结收支结构',
    desc: '剖析核心收入来源与最大支出项',
    prompt: '请帮我全面总结当前的收支结构特点，指出最大支出项并给出优化建议。'
  },
  {
    iconComp: markRaw(Aim),
    title: '结余率与抗风险评估',
    desc: '诊断储蓄空间与应急资金健康度',
    prompt: '根据我的收支分类数据，评估我的净结余率与财务抗风险能力，并给出具体的存钱计划建议。'
  },
  {
    iconComp: markRaw(TrendCharts),
    title: '定制月度节流方案',
    desc: '找出可削减的非必要开支比例',
    prompt: '帮我看看哪些支出分类存在节流空间，制定一份可落地的月度预算控制方案。'
  }
]

// ==================== 模型连通性状态检测 & AI 阶梯封禁状态 ====================
const connStatus = reactive({
  checking: false,
  connected: false,
  state: 'checking',
  message: '正在检测模型连通性...'
})

const aiBanState = reactive({
  banned: false,
  banLevel: 0,
  banCount: 0,
  banReason: '',
  banTime: '',
  banDuration: '',
  banExpireTime: '',
  violationCount24h: 0
})

const applyBanMetaFromPayload = (payload = {}) => {
  const isBanned = payload.aiBanned === true || payload.aiBanned === 1 || payload.banned === 1
  aiBanState.banned = isBanned
  aiBanState.banLevel = Number(payload.aiBanLevel || 0)
  aiBanState.banCount = Number(payload.aiBanCount || 0)
  aiBanState.banReason = payload.aiBanReason || payload.banReason || ''
  aiBanState.banTime = payload.aiBanTime || payload.banTime || ''
  aiBanState.banDuration = payload.aiBanDuration || payload.banDuration || (aiBanState.banLevel === 1 ? '24小时' : aiBanState.banLevel === 2 ? '3天' : aiBanState.banLevel === 3 ? '1个月' : aiBanState.banLevel >= 4 ? '永久' : '')
  aiBanState.banExpireTime = payload.aiBanExpireTime || payload.banExpireTime || ''
  if (payload.violationCount24h !== undefined) {
    aiBanState.violationCount24h = Number(payload.violationCount24h || 0)
  }
  if (isBanned) {
    connStatus.connected = false
    connStatus.state = 'error'
    connStatus.message = `AI 对话已封禁 (${aiBanState.banDuration || '受限'})`
  }
}

const checkConnectivity = async () => {
  connStatus.checking = true
  connStatus.state = 'checking'
  connStatus.message = '正在检测连通性...'
  try {
    const res = await request.get('/deepseek/status')
    if (res.code === 200 && res.data) {
      applyBanMetaFromPayload(res.data)
      if (!aiBanState.banned) {
        connStatus.connected = !!res.data.connected
        connStatus.state = res.data.state || (res.data.connected ? 'online' : 'error')
        connStatus.message = res.data.message || (res.data.connected ? '在线' : '未连接')
      }
    } else {
      connStatus.connected = false
      connStatus.state = 'error'
      connStatus.message = '状态检测失败'
    }
  } catch {
    connStatus.connected = false
    connStatus.state = 'error'
    connStatus.message = '服务不可达'
  } finally {
    connStatus.checking = false
  }
}

// ==================== 收支饼图渲染（主舞台初始饼图 + 云母悬浮窗饼图） ====================
const inlinePie1Ref = ref(null)
const inlinePie2Ref = ref(null)
const modalPie1Ref = ref(null)
const modalPie2Ref = ref(null)

const buildPieOption = (titleText, subText, dark, list, palette) => {
  const scale = getChartFontScale();
  return {
  title: {
    text: titleText,
    subtext: subText,
    left: 'left',
    textStyle: { fontSize: Math.round(15 * scale), fontWeight: 700, color: dark ? '#f1f5f9' : '#0f172a' },
    subtextStyle: { fontSize: Math.round(12 * scale), color: dark ? '#94a3b8' : '#64748b' }
  },
  color: palette,
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'item',
    textStyle: { fontSize: Math.round(13 * scale) },
    formatter: '{b}: ¥{c} ({d}%)'
  },
  legend: {
    orient: 'horizontal',
    bottom: 0,
    textStyle: { color: dark ? '#cbd5e1' : '#475569', fontSize: Math.round(12 * scale) }
  },
  series: [
    {
      type: 'pie',
      radius: ['42%', '70%'],
      center: ['50%', '48%'],
      avoidLabelOverlap: false,
      showEmptyCircle: true,
      emptyCircleStyle: {
        color: dark ? 'rgba(148, 163, 184, 0.14)' : 'rgba(148, 163, 184, 0.22)',
        borderColor: dark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(148, 163, 184, 0.28)',
        borderWidth: 1
      },
      itemStyle: {
        borderRadius: 10,
        borderColor: dark ? '#1e293b' : '#ffffff',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center',
        formatter: (p) => `${p.name}\n${p.percent}%`
      },
      emphasis: {
        label: {
          show: true,
          fontSize: Math.round(16 * scale),
          fontWeight: 'bold',
          color: dark ? '#ffffff' : '#0f172a'
        }
      },
      labelLine: { show: false },
      data: list.map(item => ({
        name: item.category,
        value: item.amount
      }))
    }
  ]
  }
}

const renderChartsOnPair = (dom1, dom2) => {
  if (!dom1 || !dom2) return
  echarts.getInstanceByDom(dom1)?.dispose()
  echarts.getInstanceByDom(dom2)?.dispose()
  const chart1 = echarts.init(dom1, isDark.value ? 'dark' : null)
  const chart2 = echarts.init(dom2, isDark.value ? 'dark' : null)
  chart1.setOption(
    buildPieOption(
      '账户收入分类统计',
      `共 ${data.incomeList.length} 个收入维度`,
      isDark.value,
      data.incomeList,
      ['#10b981', '#06b6d4', '#3b82f6', '#8b5cf6', '#f59e0b']
    )
  )
  chart2.setOption(
    buildPieOption(
      '账户支出分类统计',
      `共 ${data.payList.length} 个支出维度`,
      isDark.value,
      data.payList,
      ['#1677ff', '#6366f1', '#ec4899', '#f97316', '#14b8a6']
    )
  )
}

const fetchBillCountsAndRender = async () => {
  try {
    const [incRes, payRes] = await Promise.all([
      request.get('/bill/count', { params: { type: '收入' } }),
      request.get('/bill/count', { params: { type: '支出' } })
    ])
    data.incomeList = incRes.data || []
    data.payList = payRes.data || []
    nextTick(() => {
      renderChartsOnPair(inlinePie1Ref.value, inlinePie2Ref.value)
      if (showChartOverlay.value) {
        renderChartsOnPair(modalPie1Ref.value, modalPie2Ref.value)
      }
    })
  } catch (e) {
    console.error(e)
  }
}

// ==================== 对话与流式输出控制（按用户隔离 + 切换页面不丢失 + 第一条回复后自动存档 + 首行充当标题） ====================
const currentUserKey = () => {
  const uid = data.user?.id || data.user?.username || 'guest'
  return {
    historyKey: `star_realm_ai_history_${uid}`,
    activeKey: `star_realm_ai_active_${uid}`
  }
}

const hasStartedChat = ref(false)
const showChartOverlay = ref(false)
const showHistoryDrawer = ref(false)
const inputFocused = ref(false)
const isStreaming = ref(false)
const chatViewportRef = ref(null)
const chatList = ref([])
const currentSessionId = ref(null)

const chatHistoryList = ref([])

// 提取第一条 AI 回复的首行有效文本作为存档标题（若尚未生成完毕则回退到用户首条提问首行）
const extractArchiveTitle = (messages = []) => {
  const firstAi = messages.find(m => m.role === 'assistant' && m.content && !m.waitingFirstToken && !m.content.startsWith('**智能分析中断**'))
  const rawSource = firstAi?.content || messages.find(m => m.role === 'user')?.content || '财务深度诊断会话'
  const lines = String(rawSource)
    .split(/\r?\n/)
    .map(l => l.replace(/^[#>*\-\d.\s`]+/, '').replace(/[*_`~[\]]/g, '').trim())
    .filter(Boolean)
  const firstLine = lines[0] || '财务深度诊断会话'
  return firstLine.length > 26 ? firstLine.slice(0, 26) + '...' : firstLine
}

// 同步当前用户的历史对话存档与当前活跃会话到后端数据库（即使前后端重新编译重启也永不丢失）
let syncTimer = null
const syncChatStateToBackend = (immediate = false) => {
  const doSync = () => {
    const tokenObj = JSON.parse(localStorage.getItem('loginToken') || '{}')
    if (!tokenObj || !tokenObj.token) return
    const cleanActive = (hasStartedChat.value && chatList.value.length > 0)
      ? {
          sessionId: currentSessionId.value,
          hasStartedChat: hasStartedChat.value,
          messages: chatList.value.map(m => ({
            role: m.role,
            content: m.content || '',
            time: m.time || '',
            waitingFirstToken: false,
            streaming: false
          }))
        }
      : null
    request.post('/deepseek/chatState', {
      historyList: chatHistoryList.value,
      activeSession: cleanActive
    }).catch(() => {})
  }
  if (immediate) {
    if (syncTimer) clearTimeout(syncTimer)
    doSync()
  } else {
    if (syncTimer) clearTimeout(syncTimer)
    syncTimer = setTimeout(doSync, 350)
  }
}

// 持久化当前正在进行的会话状态，保证切换到其他子页面或重新编译后内容完全保留不丢失
const persistActiveSessionState = (syncRemote = true) => {
  const { activeKey } = currentUserKey()
  if (!hasStartedChat.value || chatList.value.length === 0) {
    localStorage.removeItem(activeKey)
    if (syncRemote) syncChatStateToBackend()
    return
  }
  const cleanMessages = chatList.value.map(m => ({
    role: m.role,
    content: m.content || '',
    time: m.time || '',
    waitingFirstToken: false,
    streaming: false
  }))
  localStorage.setItem(activeKey, JSON.stringify({
    sessionId: currentSessionId.value,
    hasStartedChat: hasStartedChat.value,
    messages: cleanMessages
  }))
  if (syncRemote) syncChatStateToBackend()
}

// 当生成第一条回复后（以及后续每轮回复后）自动写入/更新历史存档并同步至后端数据库
const saveCurrentChatToHistory = () => {
  if (chatList.value.length === 0) return
  const hasAnyReply = chatList.value.some(m => m.role === 'assistant' && (m.content || '').trim().length > 0)
  if (!hasAnyReply) return

  const { historyKey } = currentUserKey()
  if (!currentSessionId.value) {
    currentSessionId.value = Date.now()
  }
  const title = extractArchiveTitle(chatList.value)
  const serializedMessages = chatList.value.map(m => ({
    role: m.role,
    content: m.content || '',
    time: m.time || '',
    waitingFirstToken: false,
    streaming: false
  }))

  const existingIdx = chatHistoryList.value.findIndex(h => h.id === currentSessionId.value)
  const archiveItem = {
    id: currentSessionId.value,
    title,
    time: new Date().toLocaleString(),
    messages: serializedMessages
  }

  if (existingIdx !== -1) {
    chatHistoryList.value.splice(existingIdx, 1, archiveItem)
  } else {
    chatHistoryList.value.unshift(archiveItem)
    if (chatHistoryList.value.length > 30) chatHistoryList.value.pop()
  }
  localStorage.setItem(historyKey, JSON.stringify(chatHistoryList.value))
  persistActiveSessionState(false)
  syncChatStateToBackend(true)
}

const restoreHistoryAndActiveSession = async () => {
  const { historyKey, activeKey } = currentUserKey()
  try {
    chatHistoryList.value = JSON.parse(localStorage.getItem(historyKey) || '[]')
  } catch {
    chatHistoryList.value = []
  }
  try {
    const savedActive = JSON.parse(localStorage.getItem(activeKey) || 'null')
    if (savedActive && Array.isArray(savedActive.messages) && savedActive.messages.length > 0) {
      currentSessionId.value = savedActive.sessionId || Date.now()
      chatList.value = savedActive.messages
      hasStartedChat.value = true
      nextTick(() => scrollToBottom(true))
    }
  } catch {
    // ignore
  }

  // 从后端数据库拉取持久化存档（解决前后端重新编译或跨浏览器丢失问题）
  try {
    const res = await request.get('/deepseek/chatState')
    if (res.code === 200 && res.data) {
      applyBanMetaFromPayload(res.data)
      const remoteHistory = Array.isArray(res.data.historyList) ? res.data.historyList : []
      if (remoteHistory.length > 0) {
        // 合并云端与本地历史存档（按 id 去重）
        const mergedMap = new Map()
        remoteHistory.forEach(item => {
          if (item && item.id) mergedMap.set(item.id, item)
        })
        chatHistoryList.value.forEach(item => {
          if (item && item.id && !mergedMap.has(item.id)) {
            mergedMap.set(item.id, item)
          }
        })
        chatHistoryList.value = Array.from(mergedMap.values()).slice(0, 30)
        localStorage.setItem(historyKey, JSON.stringify(chatHistoryList.value))
      } else if (chatHistoryList.value.length > 0) {
        // 本地有旧存档而后端还没有时，自动上传备份到后端
        syncChatStateToBackend(true)
      }

      const remoteActive = res.data.activeSession
      if ((!hasStartedChat.value || chatList.value.length === 0) && remoteActive && Array.isArray(remoteActive.messages) && remoteActive.messages.length > 0) {
        currentSessionId.value = remoteActive.sessionId || Date.now()
        chatList.value = remoteActive.messages
        hasStartedChat.value = true
        localStorage.setItem(activeKey, JSON.stringify(remoteActive))
        nextTick(() => scrollToBottom(true))
      }
    }
  } catch {
    // ignore
  }
}

const loadHistorySession = (item) => {
  if (isStreaming.value) return
  currentSessionId.value = item.id
  chatList.value = JSON.parse(JSON.stringify(item.messages || []))
  hasStartedChat.value = true
  showHistoryDrawer.value = false
  persistActiveSessionState(true)
  scrollToBottom(true)
}

const startNewSession = () => {
  if (isStreaming.value) return
  saveCurrentChatToHistory()
  currentSessionId.value = Date.now()
  chatList.value = []
  hasStartedChat.value = false
  showChartOverlay.value = false
  const { activeKey } = currentUserKey()
  localStorage.removeItem(activeKey)
  syncChatStateToBackend(true)
  nextTick(() => {
    renderChartsOnPair(inlinePie1Ref.value, inlinePie2Ref.value)
  })
}

const deleteHistorySession = (id, event) => {
  event.stopPropagation()
  const { historyKey } = currentUserKey()
  chatHistoryList.value = chatHistoryList.value.filter(h => h.id !== id)
  localStorage.setItem(historyKey, JSON.stringify(chatHistoryList.value))
  if (currentSessionId.value === id) {
    currentSessionId.value = null
  }
  syncChatStateToBackend(true)
}

const copyFullMessage = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('AI 分析报告全文已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败')
  }
}

const nowTime = () => new Date().toTimeString().slice(0, 5)

// 智能跟随滚动：当用户主动向上滚动查阅已生成内容时，暂停强制拉回底部；当用户滚回底部附近（距离底部 <= 72px）时自动恢复跟随
const userScrolledUp = ref(false)

const handleViewportScroll = () => {
  const el = chatViewportRef.value
  if (!el) return
  const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight
  userScrolledUp.value = distanceToBottom > 72
}

const scrollToBottom = (force = false) => {
  if (force) {
    userScrolledUp.value = false
  }
  if (userScrolledUp.value && !force) return
  nextTick(() => {
    if (chatViewportRef.value) {
      chatViewportRef.value.scrollTop = chatViewportRef.value.scrollHeight
    }
  })
}

const toggleChartOverlay = () => {
  showChartOverlay.value = !showChartOverlay.value
  if (showChartOverlay.value) {
    nextTick(() => {
      renderChartsOnPair(modalPie1Ref.value, modalPie2Ref.value)
    })
  }
}

const resetToInitialChart = () => {
  if (isStreaming.value) return
  saveCurrentChatToHistory()
  showChartOverlay.value = false
  hasStartedChat.value = false
  const { activeKey } = currentUserKey()
  localStorage.removeItem(activeKey)
  nextTick(() => {
    renderChartsOnPair(inlinePie1Ref.value, inlinePie2Ref.value)
  })
}

const useQuickStarter = (promptText) => {
  if (aiBanState.banned) {
    ElMessage.error('您的 AI 对话功能已被封禁，暂无法发起提问')
    return
  }
  data.input = promptText
  analyze()
}

const analyze = async () => {
  if (aiBanState.banned) {
    ElMessage.error('您的 AI 智能对话功能处于封禁期，无法发送消息')
    return
  }
  const userText = data.input.trim()
  if (!userText || isStreaming.value) {
    if (!userText) ElMessage.warning('请输入您想分析的财务问题')
    return
  }

  // 1. 触发饼图向输入框左下角收纳动画，进入对话视图
  if (!hasStartedChat.value) {
    hasStartedChat.value = true
    if (!currentSessionId.value) {
      currentSessionId.value = Date.now()
    }
  }

  data.input = ''
  chatList.value.push({
    role: 'user',
    content: userText,
    time: nowTime()
  })
  persistActiveSessionState()

  // 1.2 优先检查当前用户是否已配置个人 DeepSeek API Key（未配置则直接提示，不触发敏感词判定）
  const currentUser = JSON.parse(localStorage.getItem('loginToken') || '{}')
  if (!currentUser.hasApiKey) {
    const noKeyMsg = reactive({
      role: 'assistant',
      content: '当前账户尚未配置个人 DeepSeek API Key。请前往【个人中心 & 安全设置】填入并保存您的专属密钥后即可开始智能对话与财务诊断。',
      time: nowTime(),
      waitingFirstToken: false,
      streaming: false
    })
    chatList.value.push(noKeyMsg)
    saveCurrentChatToHistory()
    scrollToBottom(true)
    return
  }

  // 1.5 确认配置 Key 后，进行前端本地合规拦截检查（直接拦截并静默记录 24h 违规档案，不消耗 API 算力）
  const localGuard = inspectUserPromptLocally(userText)
  if (localGuard.blocked) {
    const blockedMsg = reactive({
      role: 'assistant',
      content: localGuard.reply,
      time: nowTime(),
      waitingFirstToken: false,
      streaming: false
    })
    chatList.value.push(blockedMsg)
    scrollToBottom(true)

    try {
      const vioRes = await request.post('/deepseek/reportViolation', {
        category: localGuard.category,
        snippet: localGuard.snippet,
        source: '前端拦截'
      })
      if (vioRes.code === 200 && vioRes.data) {
        applyBanMetaFromPayload(vioRes.data)
      }
    } catch {
      // ignore
    }
    saveCurrentChatToHistory()
    scrollToBottom(true)
    return
  }

  // 2. 插入带类 Telegram 输入中等待动画的 AI 气泡
  const aiMsg = reactive({
    role: 'assistant',
    content: '',
    time: nowTime(),
    waitingFirstToken: true,
    streaming: false
  })
  chatList.value.push(aiMsg)
  isStreaming.value = true
  scrollToBottom(true)

  const systemPrompt = `你是一位经验丰富、客观稳重的私人财务分析师（模型：Deepseek V4.1 Flash 1017）。
请结合以下来自“星界 (Star Realm)”用户的实时收支分类汇总数据，使用结构清晰的 Markdown 格式（包含核心结论、收支结构亮点/风险点、可执行理财建议）回答用户。
【实时收入分类汇总】：${JSON.stringify(data.incomeList)}
【实时支出分类汇总】：${JSON.stringify(data.payList)}`

  const historyMessages = [
    { role: 'system', content: systemPrompt },
    ...chatList.value
      .slice(0, -1)
      .slice(-6)
      .map(m => ({ role: m.role, content: m.content }))
  ]

  const userToken = JSON.parse(localStorage.getItem('loginToken') || '{}')?.token

  // ==================== 平滑定速打字机队列（约 28 字/秒，比正常阅读速度稍快且始终匀速丝滑） ====================
  let charQueue = []
  let networkDone = false
  let typewriterTimer = null
  let finishTypingResolve = null
  const typingFinishedPromise = new Promise((resolve) => {
    finishTypingResolve = resolve
  })

  const startTypewriterLoop = () => {
    if (typewriterTimer) return
    // 每 34ms 输出 1 个字符（约 29 字/秒），即便后端瞬间吐出大段内容也保持恒定阅读节奏
    typewriterTimer = setInterval(() => {
      if (charQueue.length > 0) {
        if (aiMsg.waitingFirstToken) {
          aiMsg.waitingFirstToken = false
          aiMsg.streaming = true
        }
        // 每次取出 1 个字符，若积压非常多（>260字）也最多只取 2 个字符，确保视觉上柔和匀速
        const step = charQueue.length > 260 ? 2 : 1
        const chunk = charQueue.splice(0, step).join('')
        aiMsg.content += chunk
        scrollToBottom()
      } else if (networkDone) {
        clearInterval(typewriterTimer)
        typewriterTimer = null
        finishTypingResolve?.()
      }
    }, 34)
  }

  try {
    const response = await fetch('http://127.0.0.1:8080/deepseek/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'token': userToken || ''
      },
      body: JSON.stringify({ messages: historyMessages })
    })

    const contentType = response.headers.get('content-type') || ''
    if (!response.ok || contentType.includes('application/json')) {
      const errJson = await response.json().catch(() => null)
      throw new Error(errJson?.msg || `请求异常 (${response.status})，请检查【个人设置】中的 API Key`)
    }

    startTypewriterLoop()

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const rawLine of lines) {
        const line = rawLine.trim()
        if (!line) continue
        const payloadStr = line.startsWith('data:') ? line.slice(5).trim() : line
        if (payloadStr === '[DONE]') continue

        try {
          const parsed = JSON.parse(payloadStr)
          if (parsed.violationMeta) {
            applyBanMetaFromPayload(parsed.violationMeta)
          }
          const delta = parsed.choices?.[0]?.delta?.content
          if (delta) {
            // 将后端到达的文本拆分为单字符推入平滑缓冲队列
            charQueue.push(...Array.from(delta))
          }
        } catch {
          // 忽略半包 JSON
        }
      }
    }

    networkDone = true
    // 等待前端平滑打字机队列全部吐完后再结束流式状态
    if (typewriterTimer) {
      await typingFinishedPromise
    }

    if (aiMsg.waitingFirstToken) {
      aiMsg.waitingFirstToken = false
      if (!aiMsg.content) {
        aiMsg.content = '暂未获取到有效分析结果，请检查您的 API Key 余额或网络连接。'
      }
    }
    aiMsg.streaming = false
    aiMsg.time = nowTime()
    connStatus.connected = true
    connStatus.state = 'online'
    connStatus.message = '已连接 · 流式输出正常'
  } catch (err) {
    if (typewriterTimer) {
      clearInterval(typewriterTimer)
      typewriterTimer = null
    }
    aiMsg.waitingFirstToken = false
    aiMsg.streaming = false
    aiMsg.time = nowTime()
    aiMsg.content = `**智能分析中断**：${err.message || '请前往【个人设置】填写有效的 DeepSeek API 密钥。'}`
    connStatus.connected = false
    connStatus.state = 'error'
    connStatus.message = '未就绪 · 请检查密钥'
    checkConnectivity()
  } finally {
    isStreaming.value = false
    // 生成第一条（及后续每条）回复完成后，立即自动生成/更新历史存档（并以回复内容首行作为标题）
    saveCurrentChatToHistory()
    scrollToBottom()
  }
}

const handleResize = () => {
  if (inlinePie1Ref.value) echarts.getInstanceByDom(inlinePie1Ref.value)?.resize()
  if (inlinePie2Ref.value) echarts.getInstanceByDom(inlinePie2Ref.value)?.resize()
  if (modalPie1Ref.value) echarts.getInstanceByDom(modalPie1Ref.value)?.resize()
  if (modalPie2Ref.value) echarts.getInstanceByDom(modalPie2Ref.value)?.resize()
}

watch(isDark, () => {
  nextTick(() => {
    if (!hasStartedChat.value) {
      renderChartsOnPair(inlinePie1Ref.value, inlinePie2Ref.value)
    }
    if (showChartOverlay.value) {
      renderChartsOnPair(modalPie1Ref.value, modalPie2Ref.value)
    }
  })
})

const handleFontSizeChangeForCharts = () => {
  nextTick(() => {
    renderChartsOnPair(inlinePie1Ref.value, inlinePie2Ref.value)
    if (showChartOverlay.value) {
      renderChartsOnPair(modalPie1Ref.value, modalPie2Ref.value)
    }
  })
}

onMounted(() => {
  restoreHistoryAndActiveSession()
  checkConnectivity()
  fetchBillCountsAndRender()
  window.addEventListener('resize', handleResize)
  window.addEventListener('font-size-change', handleFontSizeChangeForCharts)
})

onBeforeUnmount(() => {
  persistActiveSessionState()
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('font-size-change', handleFontSizeChangeForCharts)
})
</script>

<style scoped>
.ai-analysis-stage {
  position: relative;
  height: calc(100vh - 104px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 18px;
}

/* 主舞台层：当云母饼图悬浮窗打开时，平滑施加背景高斯模糊与微缩景深 */
.stage-main-layer {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  transition: filter 0.36s cubic-bezier(0.22, 1, 0.36, 1),
              transform 0.36s cubic-bezier(0.22, 1, 0.36, 1),
              opacity 0.36s ease;
}

.stage-main-layer.is-blurred-behind {
  filter: blur(12px) saturate(120%);
  transform: scale(0.985);
  opacity: 0.68;
  pointer-events: none;
  user-select: none;
}

/* 顶部云母状态栏 */
.stage-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-radius: 16px;
  background: var(--mica-bg-light);
  backdrop-filter: blur(22px) saturate(180%);
  -webkit-backdrop-filter: blur(22px) saturate(180%);
  border: 1px solid rgba(226, 232, 240, 0.85);
  box-shadow: var(--mica-shadow-light);
  margin-bottom: 14px;
}

html.dark .stage-header {
  background: var(--mica-bg-dark);
  border-color: rgba(255, 255, 255, 0.08);
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ds-glow-badge {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary));
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(var(--theme-rgb), 0.3);
}

.model-meta {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.model-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-name {
  font-size: 15.5px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.model-build {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 6px;
  background: rgba(var(--theme-rgb), 0.12);
  color: var(--el-color-primary);
}

.scene-tag {
  font-size: 11.5px;
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  font-weight: 600;
}

.conn-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.status-dot.online {
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.22);
  animation: pulseDot 2s infinite;
}

.status-dot.checking {
  background: var(--el-color-primary);
  animation: pulseChecking 1s infinite;
}

.status-dot.unconfigured {
  background: #f59e0b;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.22);
}

.status-dot.error {
  background: #ef4444;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.22);
}

@keyframes pulseDot {
  0%, 100% { box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.26); }
  50% { box-shadow: 0 0 0 6px rgba(16, 185, 129, 0.06); }
}

@keyframes pulseChecking {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}

.status-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 中央滚动视口（底部预留给悬浮输入框的空间） */
.stage-viewport {
  flex: 1;
  overflow-y: auto;
  padding: 4px 4px 175px 4px;
}

/* 初始双饼图英雄区 */
.initial-charts-hero {
  transform-origin: 22% 95%;
}

/* 饼图向底部输入框左下角收纳的缩放平移动画 */
.chart-dock-collapse-enter-active {
  transition: all 0.52s cubic-bezier(0.22, 1, 0.36, 1);
}
.chart-dock-collapse-leave-active {
  transition: all 0.48s cubic-bezier(0.32, 0, 0.15, 1);
}
.chart-dock-collapse-enter-from,
.chart-dock-collapse-leave-to {
  opacity: 0;
  transform: translate3d(-180px, 280px, 0) scale(0.12);
  filter: blur(10px);
}

.hero-intro {
  text-align: center;
  max-width: 680px;
  margin: 4px auto 18px auto;
}

.hero-heading {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 6px 0;
  font-size: 20px;
  font-weight: 700;
}

.heading-icon {
  color: var(--el-color-primary);
}

.hero-sub {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.charts-grid {
  margin-bottom: 18px;
}

.mica-chart-box {
  padding: 18px;
  border-radius: 16px;
  background: var(--mica-bg-light);
  backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(226, 232, 240, 0.85);
  box-shadow: var(--mica-shadow-light);
}

html.dark .mica-chart-box {
  background: var(--mica-bg-dark);
  border-color: rgba(255, 255, 255, 0.08);
}

.echarts-box {
  width: 100%;
  height: 310px;
}

/* 快捷提问引导卡片 */
.starter-prompts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
  max-width: 920px;
  margin: 0 auto;
}

.starter-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 14px;
  background: var(--mica-bg-light);
  border: 1px solid rgba(var(--theme-rgb), 0.16);
  cursor: pointer;
  transition: all 0.24s cubic-bezier(0.22, 1, 0.36, 1);
}

html.dark .starter-card {
  background: var(--table-row-alt-bg, #101829);
  border-color: var(--table-border-color, #1e2c46);
}

.starter-card:hover {
  transform: translateY(-3px);
  border-color: var(--el-color-primary);
  box-shadow: 0 10px 24px -6px rgba(var(--theme-rgb), 0.2);
}

.starter-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: rgba(var(--theme-rgb), 0.12);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.starter-title {
  font-size: 13.5px;
  font-weight: 700;
  margin-bottom: 2px;
}

.starter-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 流式对话分析列表 */
.analysis-chat-stream {
  max-width: 880px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stream-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  animation: msgSlideUp 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes msgSlideUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stream-item.is-user {
  justify-content: flex-end;
}

.ai-message-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.ai-head-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-left: 2px;
}

.ai-badge-icon {
  width: 34px;
  height: 34px;
  border-radius: 11px;
  background: rgba(var(--theme-rgb), 0.12);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.ai-badge-icon.is-active-glow {
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary));
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(var(--theme-rgb), 0.35);
}

.ai-head-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ai-sender-name {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.ai-done-tag {
  font-size: 11.5px;
  color: var(--el-text-color-secondary);
}

/* 头像右侧：从左至右跑马灯式主题色明暗渐变状态词 */
.shimmer-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(var(--theme-rgb), 0.08);
  border: 1px solid rgba(var(--theme-rgb), 0.2);
}

.shimmer-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-primary);
  box-shadow: 0 0 8px var(--el-color-primary);
  animation: pulseChecking 1.2s infinite ease-in-out;
}

.shimmer-marquee-text {
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.02em;
  background: linear-gradient(
    90deg,
    rgba(var(--theme-rgb), 0.38) 0%,
    var(--theme-primary) 28%,
    var(--theme-secondary) 50%,
    var(--theme-primary) 72%,
    rgba(var(--theme-rgb), 0.38) 100%
  );
  background-size: 220% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
  animation: shimmerMarqueeLTR 2.2s linear infinite;
}

html.dark .shimmer-marquee-text {
  background: linear-gradient(
    90deg,
    rgba(var(--theme-rgb), 0.45) 0%,
    var(--theme-primary) 25%,
    #ffffff 50%,
    var(--theme-secondary) 75%,
    rgba(var(--theme-rgb), 0.45) 100%
  );
  background-size: 220% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

@keyframes shimmerMarqueeLTR {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: -120% 50%;
  }
}

.stream-bubble-col {
  max-width: 92%;
}

.stream-card {
  padding: 16px 20px;
  border-radius: 16px;
  background: var(--mica-bg-light);
  backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: var(--mica-shadow-light);
}

html.dark .stream-card {
  background: var(--table-row-bg, #0c1322);
  border-color: var(--table-border-color, #1e2c46);
}

.stream-item.is-user .stream-card {
  background: linear-gradient(135deg, rgba(var(--theme-rgb), 0.92), rgba(var(--theme-rgb), 0.76));
  color: #ffffff;
  border: none;
  border-top-right-radius: 4px;
  padding: 11px 18px;
  box-shadow: 0 8px 20px -4px rgba(var(--theme-rgb), 0.3);
}

.report-meta-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11.5px;
  color: var(--el-text-color-secondary);
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px dashed rgba(148, 163, 184, 0.25);
}

/* AI 消息底部小工具栏 */
.ai-msg-footer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding: 8px 12px;
  border-radius: 10px;
  background: rgba(var(--theme-rgb), 0.06);
  border: 1px solid rgba(var(--theme-rgb), 0.16);
  flex-wrap: wrap;
  gap: 10px;
}

html.dark .ai-msg-footer-bar {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
}

.ai-footer-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-footer-divider {
  width: 1px;
  height: 12px;
  background: rgba(148, 163, 184, 0.35);
}

.ai-msg-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: rgba(var(--theme-rgb), 0.12);
  border: 1px solid rgba(var(--theme-rgb), 0.24);
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  padding: 3px 10px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.ai-msg-action-btn:hover {
  background: var(--el-color-primary);
  color: #ffffff;
}

.ai-footer-time {
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.ai-disclaimer-badge {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

/* 历史对话存档抽屉样式 */
.history-drawer-body {
  padding: 10px 16px;
}

.history-empty {
  padding: 60px 20px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: var(--el-text-color-regular);
}

.history-empty-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item-card {
  padding: 12px 16px;
  border-radius: 12px;
  background: var(--table-row-alt-bg, #eef4fc);
  border: 1px solid var(--table-border-color, #d3e1f4);
  cursor: pointer;
  transition: all 0.2s ease;
}

html.dark .history-item-card {
  background: var(--table-row-alt-bg, #101829);
  border-color: var(--table-border-color, #1e2c46);
}

.history-item-card:hover {
  border-color: var(--el-color-primary);
  transform: translateY(-1px);
}

.hist-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.hist-title {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.hist-time {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.meta-left {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.user-query-text {
  font-size: 14px;
  line-height: 1.6;
}

/* 唯一末尾细长针闪烁光标（仅在最新输出位置出现一根 1.5px 细针） */
.markdown-body :deep(.needle-caret) {
  display: inline-block;
  width: 1.5px;
  height: 1.08em;
  margin-left: 3px;
  vertical-align: -0.15em;
  border-radius: 1px;
  background: var(--el-color-primary);
  box-shadow: 0 0 6px rgba(var(--theme-rgb), 0.65);
  animation: needleBlink 0.9s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes needleBlink {
  0%, 100% {
    opacity: 1;
    transform: scaleY(1);
  }
  50% {
    opacity: 0.12;
    transform: scaleY(0.88);
  }
}

/* 类 Telegram 等待输入动画 */
.tg-typing-box {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-radius: 16px;
  border-top-left-radius: 4px;
  background: rgba(var(--theme-rgb), 0.09);
  border: 1px solid rgba(var(--theme-rgb), 0.2);
}

.tg-dots {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.tg-dots span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--el-color-primary);
  animation: tgWave 1.3s infinite ease-in-out;
}

.tg-dots span:nth-child(1) { animation-delay: 0s; }
.tg-dots span:nth-child(2) { animation-delay: 0.18s; }
.tg-dots span:nth-child(3) { animation-delay: 0.36s; }

@keyframes tgWave {
  0%, 60%, 100% {
    transform: translateY(0) scale(0.85);
    opacity: 0.45;
  }
  30% {
    transform: translateY(-6px) scale(1.12);
    opacity: 1;
  }
}

.tg-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-color-primary);
}

/* ================= 云母饼图悬浮窗 (Floating Mica Overlay) ================= */
.chart-overlay-backdrop {
  position: absolute;
  inset: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 24px 140px 24px;
  background: rgba(15, 23, 42, 0.16);
}

.mica-floating-window {
  width: min(920px, 94%);
  padding: 20px 24px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(30px) saturate(190%);
  -webkit-backdrop-filter: blur(30px) saturate(190%);
  border: 1px solid rgba(255, 255, 255, 0.75);
  box-shadow: 0 28px 64px -12px rgba(15, 23, 42, 0.26);
  transform-origin: 25% 100%;
}

html.dark .mica-floating-window {
  background: var(--table-row-bg, #0c1322);
  border-color: var(--table-border-color, #1e2c46);
  box-shadow: 0 28px 64px -12px rgba(0, 0, 0, 0.65);
}

.floating-window-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.window-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.window-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary));
}

.window-title {
  font-size: 16px;
  font-weight: 700;
}

.bg-streaming-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(var(--theme-rgb), 0.12);
  color: var(--el-color-primary);
}

.mini-pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-primary);
  animation: pulseChecking 1s infinite;
}

.floating-chart-pane {
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(226, 232, 240, 0.7);
}

html.dark .floating-chart-pane {
  background: rgba(30, 41, 59, 0.55);
  border-color: rgba(255, 255, 255, 0.06);
}

.echarts-modal-box {
  width: 100%;
  height: 300px;
}

.mica-modal-pop-enter-active,
.mica-modal-pop-leave-active {
  transition: all 0.36s cubic-bezier(0.22, 1, 0.36, 1);
}

.mica-modal-pop-enter-from,
.mica-modal-pop-leave-to {
  opacity: 0;
  transform: translateY(40px) scale(0.92);
}

/* ================= 子页面底部中央悬浮云母输入坞 ================= */
.floating-dock-wrapper {
  position: absolute;
  left: 50%;
  bottom: 14px;
  transform: translateX(-50%);
  width: min(820px, 94%);
  z-index: 50;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 输入框正上方同款等宽不可关闭的 AI 封禁通知气泡 */
.mica-ban-notice-bar {
  width: 100%;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-radius: 18px;
  background: rgba(255, 250, 250, 0.92);
  backdrop-filter: blur(26px) saturate(190%);
  -webkit-backdrop-filter: blur(26px) saturate(190%);
  border: 1.5px solid rgba(239, 68, 68, 0.38);
  box-shadow: 0 14px 36px -8px rgba(239, 68, 68, 0.18),
              0 4px 12px -2px rgba(15, 23, 42, 0.08);
}

html.dark .mica-ban-notice-bar {
  background: rgba(28, 16, 24, 0.92);
  border-color: rgba(248, 113, 113, 0.42);
  box-shadow: 0 16px 40px -8px rgba(0, 0, 0, 0.65),
              0 0 0 1px rgba(248, 113, 113, 0.16);
}

.ban-notice-main {
  display: flex;
  align-items: flex-start;
  gap: 13px;
  flex: 1;
  min-width: 0;
}

.ban-shield-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.18), rgba(245, 158, 11, 0.16));
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.32);
  margin-top: 2px;
}

html.dark .ban-shield-icon {
  color: #f87171;
  background: linear-gradient(135deg, rgba(248, 113, 113, 0.22), rgba(251, 191, 36, 0.16));
  border-color: rgba(248, 113, 113, 0.38);
}

.ban-notice-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ban-headline-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.ban-headline {
  font-size: 14px;
  font-weight: 800;
  color: #dc2626;
  letter-spacing: 0.2px;
}

html.dark .ban-headline {
  color: #fca5a5;
}

.ban-level-pill {
  font-size: 11.5px;
  font-weight: 700;
  padding: 2px 9px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.14);
  color: #dc2626;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

html.dark .ban-level-pill {
  background: rgba(248, 113, 113, 0.18);
  color: #fecaca;
  border-color: rgba(248, 113, 113, 0.35);
}

.ban-count-pill {
  font-size: 11px;
  font-weight: 700;
  padding: 1.5px 8px;
  border-radius: 999px;
  background: rgba(245, 158, 11, 0.15);
  color: #d97706;
  border: 1px solid rgba(245, 158, 11, 0.3);
}

html.dark .ban-count-pill {
  color: #fcd34d;
}

.ban-statement {
  font-size: 12.5px;
  line-height: 1.45;
  color: var(--el-text-color-regular);
  font-weight: 500;
}

.legal-inline-link {
  border: none;
  background: transparent;
  padding: 0 2px;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--el-color-primary, #1677ff);
  cursor: pointer;
}

.legal-inline-link:hover {
  text-decoration: underline;
}

.feedback-dialog-body {
  font-size: 13.5px;
  line-height: 1.65;
  color: var(--el-text-color-regular);
}

.feedback-tip {
  margin: 0 0 12px 0;
}

.feedback-info-box {
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(var(--theme-rgb), 0.06);
  border: 1px solid rgba(var(--theme-rgb), 0.2);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feedback-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.fb-label {
  color: var(--el-text-color-secondary);
  font-weight: 600;
}

.fb-val {
  color: var(--el-color-primary);
  font-weight: 700;
  user-select: text;
}

.ban-sub-reason {
  font-size: 12px;
  line-height: 1.45;
  color: #b91c1c;
  font-weight: 600;
}

html.dark .ban-sub-reason {
  color: #fca5a5;
}

.ban-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 11.5px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.ban-meta-row strong {
  color: #dc2626;
  font-weight: 700;
}

html.dark .ban-meta-row strong {
  color: #f87171;
}

.meta-divider {
  opacity: 0.4;
}

.ban-notice-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.ban-banner-pop-enter-active,
.ban-banner-pop-leave-active {
  transition: all 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}

.ban-banner-pop-enter-from,
.ban-banner-pop-leave-to {
  opacity: 0;
  transform: translateY(14px) scale(0.98);
}

.mica-composer-dock.is-banned {
  border-color: rgba(239, 68, 68, 0.28);
  opacity: 0.82;
}

.mica-composer-dock {
  padding: 10px 12px 10px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(26px) saturate(190%);
  -webkit-backdrop-filter: blur(26px) saturate(190%);
  border: 1.5px solid rgba(var(--theme-rgb), 0.24);
  box-shadow: 0 18px 42px -8px rgba(15, 23, 42, 0.14),
              0 4px 12px -2px rgba(var(--theme-rgb), 0.08);
  transition: all 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}

html.dark .mica-composer-dock {
  background: var(--table-row-alt-bg, #101829);
  border-color: rgba(var(--theme-rgb), 0.3);
  box-shadow: 0 18px 42px -8px rgba(0, 0, 0, 0.6);
}

.mica-composer-dock.is-focused {
  border-color: var(--el-color-primary);
  box-shadow: 0 20px 48px -8px rgba(var(--theme-rgb), 0.24),
              0 0 0 3px rgba(var(--theme-rgb), 0.12);
}

/* 隐藏 el-textarea 默认外框，使其融于悬浮云母底座 */
.dock-textarea :deep(.el-textarea__inner) {
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
  padding: 4px 4px 8px 2px !important;
  font-size: 14px;
  line-height: 1.55;
  resize: none;
}

.dock-inner-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 6px;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
}

.dock-bottom-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 左下角收纳后的饼图呼出胶囊开关 */
.chart-dock-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid rgba(var(--theme-rgb), 0.3);
  background: rgba(var(--theme-rgb), 0.1);
  color: var(--el-color-primary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.24s cubic-bezier(0.22, 1, 0.36, 1);
}

.chart-dock-trigger:hover,
.chart-dock-trigger.active {
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary));
  color: #ffffff;
  border-color: transparent;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(var(--theme-rgb), 0.32);
}

.pill-count {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
}

.mini-chart-pill-pop-enter-active {
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.mini-chart-pill-pop-enter-from {
  opacity: 0;
  transform: scale(0.5) translateY(-12px);
}

.dock-hint {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 右下角镶嵌发送按钮 */
.embedded-send-btn {
  border-radius: 12px !important;
  padding: 8px 18px !important;
  font-weight: 600;
}

/* ================= 精修 Markdown 排版：字距、行距、标题层级比例、表格与代码块独立容器 ================= */
.markdown-body {
  font-size: 14.5px;
  line-height: 1.82;
  letter-spacing: 0.02em;
  color: var(--el-text-color-primary);
  word-break: break-word;
}

.markdown-body :deep(h1) {
  font-size: 1.42em;
  font-weight: 700;
  line-height: 1.38;
  letter-spacing: -0.01em;
  margin: 20px 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(var(--theme-rgb), 0.2);
  color: var(--el-text-color-primary);
}

.markdown-body :deep(h2) {
  font-size: 1.24em;
  font-weight: 700;
  line-height: 1.42;
  margin: 18px 0 10px 0;
  padding-left: 10px;
  border-left: 3.5px solid var(--el-color-primary);
  color: var(--el-text-color-primary);
}

.markdown-body :deep(h3) {
  font-size: 1.1em;
  font-weight: 700;
  line-height: 1.48;
  margin: 14px 0 8px 0;
  color: var(--el-text-color-primary);
}

.markdown-body :deep(h4) {
  font-size: 1.02em;
  font-weight: 600;
  margin: 12px 0 6px 0;
}

.markdown-body :deep(h1:first-child),
.markdown-body :deep(h2:first-child),
.markdown-body :deep(h3:first-child) {
  margin-top: 4px;
}

.markdown-body :deep(p) {
  margin: 10px 0;
  line-height: 1.82;
  letter-spacing: 0.02em;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 22px;
  margin: 10px 0;
}

.markdown-body :deep(li) {
  margin: 6px 0;
  line-height: 1.78;
}

.markdown-body :deep(li::marker) {
  color: var(--el-color-primary);
  font-weight: 600;
}

.markdown-body :deep(strong) {
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.markdown-body :deep(blockquote) {
  margin: 14px 0;
  padding: 10px 16px;
  border-left: 4px solid var(--el-color-primary);
  border-radius: 0 10px 10px 0;
  background: rgba(var(--theme-rgb), 0.06);
  color: var(--el-text-color-regular);
}

.markdown-body :deep(hr) {
  border: none;
  border-top: 1px dashed rgba(148, 163, 184, 0.35);
  margin: 18px 0;
}

/* 行内代码胶囊 */
.markdown-body :deep(:not(pre) > code) {
  font-family: var(--font-mono);
  font-size: 0.88em;
  background: rgba(var(--theme-rgb), 0.1);
  color: var(--el-color-primary);
  border: 1px solid rgba(var(--theme-rgb), 0.2);
  padding: 2px 6px;
  margin: 0 2px;
  border-radius: 6px;
}

/* 代码块与表格共用独立区域卡片容器 */
.markdown-body :deep(.md-block-card) {
  margin: 16px 0;
  border-radius: 13px;
  overflow: hidden;
  border: 1px solid var(--table-border-color, #d3e1f4);
  background: var(--table-row-alt-bg, #eef4fc);
  box-shadow: 0 4px 16px -4px rgba(15, 23, 42, 0.06);
}

html.dark .markdown-body :deep(.md-block-card) {
  background: var(--table-row-alt-bg, #101829);
  border-color: var(--table-border-color, #1e2c46);
  box-shadow: 0 6px 20px -4px rgba(0, 0, 0, 0.45);
}

.markdown-body :deep(.md-block-toolbar) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 14px;
  background: rgba(var(--theme-rgb), 0.08);
  border-bottom: 1px solid rgba(148, 163, 184, 0.26);
}

html.dark .markdown-body :deep(.md-block-toolbar) {
  background: rgba(var(--theme-rgb), 0.14);
  border-bottom-color: rgba(148, 163, 184, 0.2);
}

.markdown-body :deep(.md-block-tag) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-mono);
  font-size: 11.5px;
  font-weight: 700;
  color: var(--el-color-primary);
  letter-spacing: 0.04em;
}

.markdown-body :deep(.md-tag-dot) {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-primary);
}

/* 一键复制按钮与复制成功浮动气泡 */
.markdown-body :deep(.md-copy-btn) {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 7px;
  border: 1px solid rgba(var(--theme-rgb), 0.28);
  background: rgba(255, 255, 255, 0.75);
  color: var(--el-color-primary);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

html.dark .markdown-body :deep(.md-copy-btn) {
  background: rgba(30, 41, 59, 0.85);
}

.markdown-body :deep(.md-copy-btn:hover) {
  background: var(--el-color-primary);
  color: #ffffff;
  border-color: var(--el-color-primary);
}

.markdown-body :deep(.md-copy-btn.is-copied) {
  background: #10b981;
  color: #ffffff;
  border-color: #10b981;
}

.markdown-body :deep(.md-copied-popover) {
  position: absolute;
  right: 0;
  bottom: calc(100% + 7px);
  white-space: nowrap;
  padding: 4px 10px;
  border-radius: 7px;
  font-size: 11px;
  font-weight: 600;
  background: #0f172a;
  color: #ffffff;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.25);
  opacity: 0;
  transform: translateY(4px) scale(0.92);
  pointer-events: none;
  transition: all 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}

.markdown-body :deep(.md-copy-btn.is-copied .md-copied-popover) {
  opacity: 1;
  transform: translateY(0) scale(1);
}

/* 代码块内部排版 */
.markdown-body :deep(.md-pre) {
  margin: 0;
  padding: 14px 16px;
  overflow-x: auto;
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.65;
  color: var(--el-text-color-primary);
}

/* 表格容器与高可视度网格边框 */
.markdown-body :deep(.md-table-scroll) {
  width: 100%;
  overflow-x: auto;
}

.markdown-body :deep(.md-styled-table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 13.5px;
  line-height: 1.6;
}

.markdown-body :deep(.md-styled-table th),
.markdown-body :deep(.md-styled-table td) {
  padding: 10px 14px;
  text-align: left;
  border: 1px solid rgba(148, 163, 184, 0.38);
}

html.dark .markdown-body :deep(.md-styled-table th),
html.dark .markdown-body :deep(.md-styled-table td) {
  border-color: rgba(148, 163, 184, 0.28);
}

.markdown-body :deep(.md-styled-table th) {
  background: rgba(var(--theme-rgb), 0.1);
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.markdown-body :deep(.md-styled-table tbody tr:nth-child(even)) {
  background: rgba(148, 163, 184, 0.06);
}

.markdown-body :deep(.md-styled-table tbody tr:hover) {
  background: rgba(var(--theme-rgb), 0.08);
}
</style>
