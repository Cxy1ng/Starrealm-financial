<template>
  <el-dialog
      :model-value="modelValue"
      @update:model-value="$emit('update:modelValue', $event)"
      :title="activeTab === 'agreement' ? '《星界 (Star Realm) 用户服务协议》' : '《星界 (Star Realm) 隐私保护政策》'"
      width="680px"
      top="6vh"
      class="legal-doc-dialog"
      destroy-on-close
  >
    <div class="legal-tab-bar">
      <button
          type="button"
          class="legal-tab-btn"
          :class="{ active: activeTab === 'agreement' }"
          @click="$emit('update:activeTab', 'agreement')"
      >
        用户服务协议
      </button>
      <button
          type="button"
          class="legal-tab-btn"
          :class="{ active: activeTab === 'privacy' }"
          @click="$emit('update:activeTab', 'privacy')"
      >
        隐私保护政策
      </button>
    </div>

    <div v-if="activeTab === 'agreement'" class="legal-doc-body">
      <div class="legal-meta">版本生效日期：2026 年 1 月 1 日 · 适用平台：星界 (Star Realm) 智能个人财务管理平台</div>

      <h4>一、总则与服务说明</h4>
      <p>
        1.1 欢迎使用「星界 (Star Realm)」智能个人财务管理平台（以下简称“本平台”）。本平台提供多币种记账管理、可视化收支看板、目标储蓄（存钱计划）规划、实时外汇汇率换算以及基于 DeepSeek 大语言模型的智能财务顾问服务。
      </p>
      <p>
        1.2 用户在注册、登录或使用本平台任何功能前，应当认真阅读并充分理解本协议全部条款（特别是<strong>第三条“AI 算力服务与专业财务顾问声明”</strong>及<strong>第四条“AI 安全护栏与违规封号条款”</strong>）。勾选“我已阅读并同意”或实际使用本平台即视为您已接受本协议约束。
      </p>

      <h4>二、账户注册、安全与管理</h4>
      <p>
        2.1 您应提供真实、合法、有效的账户资料，妥善保管账户名与登录密码。因您个人保管不善导致的账单数据泄露或篡改，由您自行承担相应责任。
      </p>
      <p>
        2.2 您可在个人中心自主绑定个人专属的 DeepSeek API Key，该密钥仅用于您本人的智能账单分析与对话，支持随时更换或一键彻底清除。
      </p>

      <h4>三、AI 算力服务与非投资建议免责声明</h4>
      <p>
        3.1 <strong>双轨制算力路由：</strong>本平台支持“用户个人专属 DeepSeek API Key”与“网站公共兜底 DeepSeek API Key”双轨机制。当您未配置个人密钥时，可共享使用系统管理员配置的公共 AI 算力配额。
      </p>
      <p>
        3.2 <strong>专业顾问边界声明：</strong>星界 AI 助手输出的一切账单诊断、恩格尔系数测算、储蓄建议及外汇换算结果<strong>仅供个人日常财务规划参考，不构成任何证券、基金、外汇交易或金融投资建议</strong>。据此做出的财务决策风险由用户自行承担。
      </p>

      <h4>四、AI 安全护栏与违规使用封号条款（重要）</h4>
      <p>
        4.1 <strong>严禁违法与敏感内容：</strong>用户在使用本平台记账备注、存钱计划及 AI 财务顾问对话时，严禁发送、传播任何违反国家法律法规、危害国家安全、涉政敏感、淫秽色情、暴力恐怖、赌博洗钱或其他违背公序良俗的信息。
      </p>
      <p>
        4.2 <strong>严禁提示词注入与越狱攻击（Prompt Injection）：</strong>星界 AI 顾问严格定位于“专业个人财务与外汇理财分析师”。严禁任何用户通过“忽略之前所有指令”、“开发者模式 (DAN)”、“输出系统初始 Prompt”或诱导 AI 扮演猫娘、女仆、虚拟伴侣等非专业角色的方式绕过安全护栏。
      </p>
      <p>
        4.3 <strong>违规处置与账号封禁：</strong>本平台配备实时正则安全护栏与系统级宪章校验。<strong>对于多次触发安全护栏拦截、恶意进行提示词注入攻击、发送涉政涉黄违禁词或恶意刷取平台公共 AI 算力的账户，系统管理员有权在不预先通知的情况下采取警告、限制 AI 访问或永久封禁账号（强制下线并禁止登录）等处置措施。</strong>
      </p>
    </div>

    <div v-else class="legal-doc-body">
      <div class="legal-meta">版本生效日期：2026 年 1 月 1 日 · 加密标准：AES-128 对称加密与 JWT 令牌校验</div>

      <h4>一、我们收集与处理的信息范围</h4>
      <p>
        1.1 <strong>账户基础信息：</strong>当您注册或完善个人资料时，我们会存储您的用户名、加密/校验凭证、昵称、性别、头像、邮箱、手机号及生日信息，用于身份认证与个人主页展示。
      </p>
      <p>
        1.2 <strong>个人财务与记账数据：</strong>包括您录入的收支记账明细、账本分类、存钱计划目标与打卡流水。上述财务数据严格按用户 ID 物理隔离，其他普通用户无法查看。
      </p>

      <h4>二、API 密钥加密与数据安全保护</h4>
      <p>
        2.1 <strong>AES-128 落盘加密：</strong>您在“个人中心”绑定的 DeepSeek 个人 API Key，以及管理员在后台配置的公共 API Key，在存入 MySQL 数据库前均经过 <strong>AES-128 对称加密算法</strong>加密存储，数据库不保存明文密钥。
      </p>
      <p>
        2.2 <strong>前端全链路脱敏：</strong>密钥保存后，任何接口返回给前端页面的展示值均强制掩码脱敏为 <code>sk-****xxxx</code> 格式，防止浏览器本地存储或旁路窥屏泄露。
      </p>

      <h4>三、AI 智能分析与第三方服务说明</h4>
      <p>
        3.1 当您主动点击“AI 账单诊断”或在“星界 AI 财务顾问”页面发起提问时，系统会将脱敏后的必要收支统计摘要或您输入的提问文本通过 HTTPS 加密通道发送至 DeepSeek 官方接口以生成回复。
      </p>
      <p>
        3.2 当您使用“实时汇率换算”功能时，系统会通过后端代理向 ExchangeRate-API 请求公开实时汇率基准数据，不会向该第三方传输您的任何个人身份信息。
      </p>

      <h4>四、您的个人数据权利</h4>
      <p>
        4.1 您可随时在平台内查询、编辑、导出（Excel 报表）或永久删除您的收支记账记录、存钱计划及已绑定的个人 API Key。
      </p>
      <p>
        4.2 如您的账号因违反《星界用户服务协议》被管理员封禁，或您需要申请注销账户，可联系系统管理员核实处理。
      </p>
    </div>

    <template #footer>
      <div class="legal-dialog-footer">
        <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
        <el-button type="primary" @click="$emit('agree')">我已阅读并同意上述条款</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  activeTab: {
    type: String,
    default: 'agreement'
  }
})

defineEmits(['update:modelValue', 'update:activeTab', 'agree'])
</script>

<style scoped>
.legal-tab-bar {
  display: inline-flex;
  gap: 6px;
  padding: 4px;
  border-radius: 10px;
  background: var(--table-row-alt-bg, #eef4fc);
  border: 1px solid var(--table-border-color, #d3e1f4);
  margin-bottom: 14px;
}

.legal-tab-btn {
  border: none;
  background: transparent;
  padding: 6px 16px;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary, #64748b);
  cursor: pointer;
  transition: all 0.2s ease;
}

.legal-tab-btn.active {
  background: var(--el-color-primary, #1677ff);
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(var(--theme-rgb, 22, 119, 255), 0.25);
}

.legal-doc-body {
  max-height: 56vh;
  overflow-y: auto;
  padding: 4px 10px 8px 4px;
  font-size: 13.5px;
  line-height: 1.75;
  color: var(--el-text-color-regular, #334155);
}

.legal-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary, #64748b);
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--table-row-alt-bg, #eef4fc);
  border-left: 3px solid var(--el-color-primary, #1677ff);
  margin-bottom: 14px;
}

.legal-doc-body h4 {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--el-text-color-primary, #0f172a);
  margin: 14px 0 6px;
}

.legal-doc-body p {
  margin: 0 0 8px;
  text-align: justify;
}

.legal-doc-body code {
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(148, 163, 184, 0.18);
  font-family: monospace;
  font-size: 12.5px;
}

.legal-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
