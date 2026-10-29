<template>
  <div class="assistant-sidebar">
    <!-- ================= 上半部分：紧凑云母实时汇率换算器（通过后端请求 Finexly 单价汇率，前端本地实时乘算金额） ================= -->
    <div class="exchange-panel">
      <div class="panel-header">
        <div class="panel-title-wrap">
          <span class="panel-icon-badge">
            <el-icon><Coin /></el-icon>
          </span>
          <span class="panel-title">实时汇率快算</span>
        </div>
        <div class="panel-actions">
          <span v-if="exchangeRate" class="rate-badge num-font">
            1 {{ data.form.base }} = {{ Number(exchangeRate).toFixed(4) }} {{ data.form.target }}
          </span>
          <el-button
              size="small"
              type="primary"
              link
              :loading="data.loading"
              @click="fetchExchangeRate"
          >
            刷新汇率
          </el-button>
        </div>
      </div>

      <!-- 币种选择与互换：仅在切换币种或点击刷新时向 API 查询单价汇率 -->
      <div class="currency-swap-row">
        <el-select v-model="data.form.base" size="default" @change="fetchExchangeRate" class="currency-select">
          <el-option
              v-for="c in data.currencies"
              :key="c.code"
              :label="`${c.name} (${c.code})`"
              :value="c.code"
          />
        </el-select>

        <el-button circle size="small" class="swap-btn" @click="swapCurrencies" title="互换币种">
          <el-icon><Switch /></el-icon>
        </el-button>

        <el-select v-model="data.form.target" size="default" @change="fetchExchangeRate" class="currency-select">
          <el-option
              v-for="c in data.currencies"
              :key="c.code"
              :label="`${c.name} (${c.code})`"
              :value="c.code"
          />
        </el-select>
      </div>

      <!-- 金额输入与本地实时乘算结果：修改金额不触发任何网络请求，直接在本地用 amount * exchangeRate 实时计算 -->
      <div class="amount-convert-grid">
        <div class="amount-box">
          <span class="box-label">持有金额 ({{ data.form.base }})</span>
          <el-input
              v-model.number="data.form.amount"
              type="number"
              min="0"
              placeholder="输入金额 (本地实时换算)"
              class="num-font"
          />
        </div>
        <div class="amount-box result-highlight">
          <span class="box-label">本地折算结果 ({{ data.form.target }})</span>
          <div class="converted-val num-font">
            {{ convertedResult }}
          </div>
        </div>
      </div>

      <div class="exchange-foot">
        <span>{{ rateStatusText }}</span>
        <el-button link size="small" type="primary" @click="askRateAdvice">询问 AI 换汇建议 →</el-button>
      </div>
    </div>

    <!-- ================= 下半部分：客服样式 DeepSeek 流式聊天窗口 ================= -->
    <div class="chat-widget">
      <!-- 客服窗口头部：模型名 + 小字版本号 + 连通性指示灯 -->
      <div class="chat-header">
        <div class="chat-title-group">
          <div class="ai-avatar-badge">
            <el-icon :size="18"><Cpu /></el-icon>
          </div>
          <div class="model-info">
            <div class="model-name-row">
              <span class="model-main-name">Deepseek V4.1 Flash</span>
              <span class="model-sub-ver num-font">1017</span>
            </div>
            <div class="model-status-row" @click="checkConnectivity" title="点击重新检测连通性">
              <span class="status-dot" :class="connStatus.state"></span>
              <span class="status-text">
                {{ isTyping ? '对方正在输入...' : connStatus.message }}
              </span>
            </div>
          </div>
        </div>

        <div class="chat-header-tools">
          <el-tooltip content="检测模型连通性" placement="top">
            <el-button circle size="small" text @click="checkConnectivity" :loading="connStatus.checking">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="清空对话记录" placement="top">
            <el-button circle size="small" text @click="clearChat">
              <el-icon><Delete /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <!-- 消息气泡列表区 -->
      <div ref="chatScrollRef" class="chat-messages" @scroll="handleChatScroll">
        <div
            v-for="(msg, index) in messages"
            :key="index"
            class="msg-row"
            :class="msg.role === 'user' ? 'msg-user' : 'msg-assistant'"
        >
          <div v-if="msg.role === 'assistant'" class="msg-avatar">
            <el-icon :size="15"><Service /></el-icon>
          </div>

          <div class="msg-bubble-wrap">
            <!-- 等待首个流式 Token 期间展示类 Telegram 的对方输入中动画 -->
            <div v-if="msg.role === 'assistant' && msg.waitingFirstToken" class="tg-typing-bubble">
              <div class="tg-dots">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <span class="tg-typing-label">DeepSeek 正在输入</span>
            </div>

            <!-- 正常/流式输出中的气泡 -->
            <div
                v-else
                class="msg-bubble"
                :class="{ 'streaming-cursor': msg.streaming }"
            >
              <div
                  v-if="msg.role === 'assistant'"
                  class="markdown-chat"
                  v-html="renderMarkdown(msg.content)"
              ></div>
              <div v-else class="user-text">{{ msg.content }}</div>
            </div>
            <div class="msg-time num-font">{{ msg.time }}</div>
          </div>
        </div>
      </div>

      <!-- 快捷提问胶囊 -->
      <div class="quick-prompts">
        <span class="prompt-chip" @click="sendQuickPrompt('现在美元兑人民币适合换汇吗？')">
          <el-icon><TrendCharts /></el-icon> 换汇时机分析
        </span>
        <span class="prompt-chip" @click="sendQuickPrompt('如何用 50/30/20 法则规划月薪？')">
          <el-icon><PieChart /></el-icon> 50/30/20 存钱法
        </span>
        <span class="prompt-chip" @click="sendQuickPrompt('帮我制定一份低风险稳健理财方案')">
          <el-icon><Aim /></el-icon> 稳健配置建议
        </span>
      </div>

      <!-- 底部客服消息发送栏 -->
      <div class="chat-input-bar">
        <el-input
            v-model="chatInput"
            placeholder="输入理财或汇率问题，按 Enter 发送..."
            :disabled="isTyping"
            @keydown.enter.exact.prevent="sendMessage"
            clearable
        />
        <el-button
            type="primary"
            :loading="isTyping"
            :disabled="!chatInput.trim()"
            @click="sendMessage"
            class="send-btn"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  Delete,
  Coin,
  Switch,
  Cpu,
  Service,
  TrendCharts,
  PieChart,
  Aim
} from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import request from '@/utils/request.js'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const renderMarkdown = (text) => md.render(text || '')

// ==================== 上半部分：Finexly 汇率获取 + 本地金额实时乘算 ====================
const data = reactive({
  user: JSON.parse(localStorage.getItem('loginToken') || '{}'),
  form: {
    base: 'USD',
    target: 'CNY',
    amount: 100
  },
  currencies: [
    { name: '美元', code: 'USD' },
    { name: '人民币', code: 'CNY' },
    { name: '欧元', code: 'EUR' },
    { name: '日元', code: 'JPY' },
    { name: '港币', code: 'HKD' },
    { name: '英镑', code: 'GBP' },
    { name: '澳元', code: 'AUD' },
    { name: '加元', code: 'CAD' },
    { name: '新台币', code: 'TWD' },
    { name: '韩元', code: 'KRW' }
  ],
  loading: false
})

// 从后端 /exchange/rate (Finexly API) 拿到的单价汇率
const exchangeRate = ref(0)
const updateTime = ref('')
const rateErrorMsg = ref('')

// 纯本地计算折合结果：修改金额时无需请求 API，立即响应
const convertedResult = computed(() => {
  const amt = Number(data.form.amount)
  const rate = Number(exchangeRate.value)
  if (Number.isNaN(amt) || amt < 0 || !rate) {
    return '0.00'
  }
  return (amt * rate).toFixed(4)
})

const rateStatusText = computed(() => {
  if (rateErrorMsg.value) return rateErrorMsg.value
  if (updateTime.value) return `Finexly 汇率同步：${updateTime.value}`
  return '切换币种或点击刷新获取 Finexly 汇率'
})

const fetchExchangeRate = async () => {
  const { base, target } = data.form
  rateErrorMsg.value = ''

  if (base === target) {
    exchangeRate.value = 1
    updateTime.value = new Date().toLocaleTimeString()
    return
  }

  data.loading = true
  try {
    const res = await request.get('/exchange/rate', {
      params: { from: base, to: target }
    })
    if (res.code === 200 && res.data?.rate) {
      exchangeRate.value = Number(res.data.rate)
      updateTime.value = res.data.updateTime || new Date().toLocaleTimeString()
    } else {
      exchangeRate.value = 0
      rateErrorMsg.value = res.msg || '未配置 Finexly API Key'
    }
  } catch (err) {
    rateErrorMsg.value = '汇率服务暂不可用'
  } finally {
    data.loading = false
  }
}

const swapCurrencies = () => {
  const temp = data.form.base
  data.form.base = data.form.target
  data.form.target = temp
  fetchExchangeRate()
}

// ==================== 下半部分：DeepSeek 连通性与流式客服聊天 ====================
const connStatus = reactive({
  checking: false,
  connected: false,
  state: 'checking',
  message: '正在检测模型连通性...'
})

const checkConnectivity = async () => {
  connStatus.checking = true
  connStatus.state = 'checking'
  connStatus.message = '正在检测连通性...'
  try {
    const res = await request.get('/deepseek/status')
    if (res.code === 200 && res.data) {
      connStatus.connected = !!res.data.connected
      connStatus.state = res.data.state || (res.data.connected ? 'online' : 'error')
      connStatus.message = res.data.message || (res.data.connected ? '在线' : '未连接')
    } else {
      connStatus.connected = false
      connStatus.state = 'error'
      connStatus.message = '状态检测失败'
    }
  } catch (e) {
    connStatus.connected = false
    connStatus.state = 'error'
    connStatus.message = '服务不可达'
  } finally {
    connStatus.checking = false
  }
}

const chatScrollRef = ref(null)
const chatInput = ref('')
const isTyping = ref(false)
const userScrolledUp = ref(false)

const handleChatScroll = () => {
  const el = chatScrollRef.value
  if (!el) return
  const dist = el.scrollHeight - el.scrollTop - el.clientHeight
  userScrolledUp.value = dist > 60
}

const nowTimeStr = () => {
  const d = new Date()
  return d.toTimeString().slice(0, 5)
}

const messages = ref([
  {
    role: 'assistant',
    content: '您好！我是您的智能外汇与私人理财客服助手 **Deepseek V4.1 Flash**。上方汇率由 Finexly API 驱动并在本地实时折算，有任何换汇或理财问题都可以问我。',
    time: nowTimeStr(),
    waitingFirstToken: false,
    streaming: false
  }
])

const scrollToBottom = (force = false) => {
  if (force) userScrolledUp.value = false
  if (userScrolledUp.value && !force) return
  nextTick(() => {
    if (chatScrollRef.value) {
      chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
    }
  })
}

const clearChat = () => {
  messages.value = [
    {
      role: 'assistant',
      content: '对话已重置。请问有什么理财或外汇问题可以帮您？',
      time: nowTimeStr(),
      waitingFirstToken: false,
      streaming: false
    }
  ]
}

const askRateAdvice = () => {
  const { base, target, amount } = data.form
  const rateInfo = exchangeRate.value
    ? `当前 Finexly 实时汇率约为 1 ${base} = ${Number(exchangeRate.value).toFixed(4)} ${target}，本地折算结果为 ${convertedResult.value} ${target}`
    : '当前尚未获取实时汇率'
  sendQuickPrompt(`我准备将 ${amount} ${base} 兑换为 ${target}（${rateInfo}），请帮我分析一下近期汇率走势与换汇建议。`)
}

const sendQuickPrompt = (text) => {
  if (isTyping.value) return
  chatInput.value = text
  sendMessage()
}

const sendMessage = async () => {
  const text = chatInput.value.trim()
  if (!text || isTyping.value) return

  const userToken = JSON.parse(localStorage.getItem('loginToken') || '{}')?.token
  chatInput.value = ''
  messages.value.push({
    role: 'user',
    content: text,
    time: nowTimeStr()
  })

  const aiMsg = reactive({
    role: 'assistant',
    content: '',
    time: nowTimeStr(),
    waitingFirstToken: true,
    streaming: false
  })
  messages.value.push(aiMsg)
  isTyping.value = true
  scrollToBottom(true)

  const historyPayload = [
    {
      role: 'system',
      content: `你是星界 (Star Realm) 内置的智能外汇与理财客服助手（模型标识：Deepseek V4.1 Flash 1017）。当前用户上方汇率计算器状态为：${data.form.amount} ${data.form.base} -> ${data.form.target}（Finexly 单价汇率：${exchangeRate.value || '未查询'}，本地折算：${convertedResult.value}）。请用简洁、友好、条理清晰的中文回答。`
    },
    ...messages.value
      .slice(0, -1)
      .slice(-8)
      .map(m => ({ role: m.role, content: m.content }))
  ]

  try {
    const response = await fetch('http://127.0.0.1:8080/deepseek/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'token': userToken || ''
      },
      body: JSON.stringify({ messages: historyPayload })
    })

    const contentType = response.headers.get('content-type') || ''
    if (!response.ok || contentType.includes('application/json')) {
      const errJson = await response.json().catch(() => null)
      throw new Error(errJson?.msg || `请求异常 (${response.status})，请检查 API Key 配置`)
    }

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
          const delta = parsed.choices?.[0]?.delta?.content
          if (delta) {
            if (aiMsg.waitingFirstToken) {
              aiMsg.waitingFirstToken = false
              aiMsg.streaming = true
            }
            aiMsg.content += delta
            scrollToBottom()
          }
        } catch {
          // 忽略非完整 JSON 片段
        }
      }
    }

    if (aiMsg.waitingFirstToken) {
      aiMsg.waitingFirstToken = false
      if (!aiMsg.content) {
        aiMsg.content = '暂未获取到有效回复，请检查您的 API 密钥额度或网络连接。'
      }
    }
    aiMsg.streaming = false
    connStatus.connected = true
    connStatus.state = 'online'
    connStatus.message = '在线 · 流式响应正常'
  } catch (err) {
    aiMsg.waitingFirstToken = false
    aiMsg.streaming = false
    aiMsg.content = `**连接提示**：${err.message || '请前往【个人设置】或管理员【网站公共配置】设置有效的 DeepSeek API Key。'}`
    connStatus.connected = false
    connStatus.state = 'error'
    connStatus.message = '未就绪 · 请检查密钥'
  } finally {
    isTyping.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  fetchExchangeRate()
  checkConnectivity()
})
</script>

<style scoped>
.assistant-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 12px;
  overflow: hidden;
}

/* ==================== 上半部分：紧凑汇率卡片 ==================== */
.exchange-panel {
  flex-shrink: 0;
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--mica-bg-light);
  border: 1px solid rgba(var(--theme-rgb), 0.18);
  box-shadow: 0 4px 20px -4px rgba(15, 23, 42, 0.05);
}

html.dark .exchange-panel {
  background: var(--table-row-alt-bg, #101829);
  border-color: var(--table-border-color, #1e2c46);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.panel-title-wrap {
  display: flex;
  align-items: center;
  gap: 7px;
}

.panel-icon-badge {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  background: rgba(var(--theme-rgb), 0.12);
  color: var(--el-color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.panel-title {
  font-size: 14px;
  font-weight: 700;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.rate-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(var(--theme-rgb), 0.12);
  color: var(--el-color-primary);
  font-weight: 600;
}

.currency-swap-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.currency-select {
  flex: 1;
}

.swap-btn {
  flex-shrink: 0;
  color: var(--el-color-primary);
  border-color: rgba(var(--theme-rgb), 0.25);
  background: rgba(var(--theme-rgb), 0.08);
}

.swap-btn:hover {
  transform: rotate(180deg) !important;
}

.amount-convert-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.amount-box {
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.03);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

html.dark .amount-box {
  background: rgba(15, 23, 42, 0.45);
}

.result-highlight {
  background: linear-gradient(135deg, rgba(var(--theme-rgb), 0.1), rgba(6, 182, 212, 0.06));
  border-color: rgba(var(--theme-rgb), 0.28);
}

.box-label {
  display: block;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.converted-val {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-color-primary);
  line-height: 32px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exchange-foot {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

/* ==================== 下半部分：客服样式聊天窗口 ==================== */
.chat-widget {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  background: var(--mica-bg-light);
  border: 1px solid rgba(226, 232, 240, 0.85);
  box-shadow: 0 8px 28px -6px rgba(15, 23, 42, 0.06);
  overflow: hidden;
}

html.dark .chat-widget {
  background: var(--table-row-bg, #0c1322);
  border-color: var(--table-border-color, #1e2c46);
}

.chat-header {
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(90deg, rgba(var(--theme-rgb), 0.1), rgba(var(--theme-rgb), 0.03));
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.chat-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-avatar-badge {
  width: 36px;
  height: 36px;
  border-radius: 11px;
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(var(--theme-rgb), 0.28);
}

.model-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.model-name-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.model-main-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.model-sub-ver {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 5px;
  background: rgba(var(--theme-rgb), 0.12);
  color: var(--el-color-primary);
}

.model-status-row {
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
  transition: all 0.3s ease;
}

.status-dot.online {
  background-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
  animation: pulseGreen 2s infinite;
}

.status-dot.checking {
  background-color: var(--el-color-primary);
  animation: pulseBlue 1.2s infinite;
}

.status-dot.unconfigured {
  background-color: #f59e0b;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.2);
}

.status-dot.error {
  background-color: #ef4444;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.2);
}

@keyframes pulseGreen {
  0%, 100% { box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.25); }
  50% { box-shadow: 0 0 0 5px rgba(16, 185, 129, 0.08); }
}

@keyframes pulseBlue {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}

.status-text {
  font-size: 11.5px;
  color: var(--el-text-color-secondary);
}

.chat-header-tools {
  display: flex;
  align-items: center;
  gap: 2px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  max-width: 92%;
  animation: bubbleIn 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes bubbleIn {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.msg-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg-assistant {
  align-self: flex-start;
}

.msg-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(var(--theme-rgb), 0.12);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.msg-bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.msg-user .msg-bubble-wrap {
  align-items: flex-end;
}

.msg-bubble {
  padding: 9px 13px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.55;
  word-break: break-word;
}

.msg-assistant .msg-bubble {
  background: rgba(241, 245, 249, 0.9);
  color: var(--el-text-color-primary);
  border-top-left-radius: 4px;
  border: 1px solid rgba(226, 232, 240, 0.8);
}

html.dark .msg-assistant .msg-bubble {
  background: rgba(30, 41, 59, 0.88);
  border-color: rgba(255, 255, 255, 0.08);
}

.msg-user .msg-bubble {
  background: linear-gradient(135deg, rgba(var(--theme-rgb), 0.92), rgba(var(--theme-rgb), 0.78));
  color: #ffffff;
  border-top-right-radius: 4px;
  box-shadow: 0 4px 12px rgba(var(--theme-rgb), 0.22);
}

.streaming-cursor :deep(p:last-child)::after {
  content: '▍';
  display: inline-block;
  margin-left: 2px;
  color: var(--el-color-primary);
  animation: blinkCursor 0.8s infinite;
}

@keyframes blinkCursor {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.tg-typing-bubble {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 14px;
  border-top-left-radius: 4px;
  background: rgba(var(--theme-rgb), 0.09);
  border: 1px solid rgba(var(--theme-rgb), 0.2);
}

.tg-dots {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.tg-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: var(--el-color-primary);
  animation: tgBounce 1.3s infinite ease-in-out;
}

.tg-dots span:nth-child(1) { animation-delay: 0s; }
.tg-dots span:nth-child(2) { animation-delay: 0.18s; }
.tg-dots span:nth-child(3) { animation-delay: 0.36s; }

@keyframes tgBounce {
  0%, 60%, 100% {
    transform: translateY(0) scale(0.85);
    opacity: 0.45;
  }
  30% {
    transform: translateY(-5px) scale(1.1);
    opacity: 1;
  }
}

.tg-typing-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--el-color-primary);
}

.msg-time {
  font-size: 10px;
  color: var(--el-text-color-secondary);
  padding: 0 2px;
}

.markdown-chat :deep(p) {
  margin: 4px 0;
}
.markdown-chat :deep(p:first-child) {
  margin-top: 0;
}
.markdown-chat :deep(p:last-child) {
  margin-bottom: 0;
}
.markdown-chat :deep(ul), .markdown-chat :deep(ol) {
  margin: 4px 0;
  padding-left: 18px;
}
.markdown-chat :deep(code) {
  font-family: var(--font-mono);
  background: rgba(15, 23, 42, 0.08);
  padding: 1px 4px;
  border-radius: 4px;
  font-size: 12px;
}

.quick-prompts {
  display: flex;
  gap: 6px;
  padding: 6px 12px;
  overflow-x: auto;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
  white-space: nowrap;
}

.prompt-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(var(--theme-rgb), 0.07);
  border: 1px solid rgba(var(--theme-rgb), 0.18);
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: all 0.2s ease;
}

.prompt-chip:hover {
  background: var(--el-color-primary);
  color: #ffffff;
  border-color: var(--el-color-primary);
}

.chat-input-bar {
  padding: 10px 12px;
  display: flex;
  gap: 8px;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(255, 255, 255, 0.45);
}

html.dark .chat-input-bar {
  background: rgba(15, 23, 42, 0.45);
}

.send-btn {
  padding: 0 16px;
}
</style>
