<template>
  <div>
    <el-card>
      <el-input
          v-model="data.username"
          style="width: 240px; margin-right: 15px"
          placeholder="输入用户名以查询"
          clearable
          :prefix-icon="Search"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="warning" @click="reset">清空</el-button>
      <el-button type="info" @click="handleAdd">添加用户</el-button>
      <el-button type="danger" @click="delBatch">批量删除</el-button>
    </el-card>
    <el-card style="margin-top: 5px">
      <el-table :data="data.tableData"
                :table-layout="'auto'"
                :default-sort="{ prop: 'id', order: 'descending'}"
                @selection-change="handleSelectionChange"
                >
        <el-table-column type="selection" width="55" align="center"/>
        <el-table-column label="ID" prop="id" sortable width="80" align="center"/>
        <el-table-column label="用户名" prop="username" show-overflow-tooltip />
        <el-table-column label="昵称" prop="nickname" show-overflow-tooltip/>
        <el-table-column label="性别" prop="sex" />
        <el-table-column label="头像">
          <template #default="scope">
            <el-avatar :src="scope.row.avatar" style="width: 40px; height: 40px; border-radius: 50%"/>
          </template>
        </el-table-column>
        <el-table-column label="手机号" prop="phone" />
        <el-table-column label="邮箱" prop="email" show-overflow-tooltip/>
        <el-table-column label="账号状态 (内容合规)" width="145" align="center">
          <template #default="scope">
            <el-tooltip
                v-if="scope.row.accountBanned === 1 || scope.row.banned === 1"
                :content="'账号封禁原因：' + (scope.row.accountBanReason || scope.row.banReason || '存钱日记/计划/简介内容违规') + (scope.row.accountBanTime ? ' (' + scope.row.accountBanTime + ')' : '')"
                placement="top"
            >
              <el-tag type="danger" effect="dark" size="small">账号已封禁</el-tag>
            </el-tooltip>
            <el-tag v-else type="success" effect="light" size="small">正常登录</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="AI 对话与 24h 违规审计" width="230" align="center">
          <template #default="scope">
            <div style="display: flex; align-items: center; justify-content: center; gap: 6px; flex-wrap: wrap;">
              <el-tooltip
                  v-if="scope.row.aiBanned === 1"
                  :content="`第 ${scope.row.aiBanCount || scope.row.aiBanLevel || 1} 次封禁 (${scope.row.aiBanDuration || '24小时'}) | 原因：${scope.row.aiBanReason || '24h违规超限'} | 到期：${scope.row.aiBanExpireTime || '永久'}`"
                  placement="top"
              >
                <el-tag type="danger" effect="dark" size="small" style="cursor: pointer;" @click="handleOpenAiAudit(scope.row)">
                  AI封禁 · {{ scope.row.aiBanDuration || '24小时' }}
                </el-tag>
              </el-tooltip>
              <el-tag
                  v-else
                  :type="(scope.row.aiBanCount || 0) > 0 ? 'warning' : 'primary'"
                  effect="plain"
                  size="small"
                  style="cursor: pointer;"
                  @click="handleOpenAiAudit(scope.row)"
              >
                AI正常{{ (scope.row.aiBanCount || 0) > 0 ? ` (累犯${scope.row.aiBanCount}次)` : '' }}
              </el-tag>

              <el-tag
                  :type="(scope.row.violationCount24h || 0) > 0 ? 'warning' : 'info'"
                  effect="light"
                  size="small"
                  style="cursor: pointer;"
                  @click="handleOpenAiAudit(scope.row)"
              >
                24h违规: {{ scope.row.violationCount24h || 0 }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="310" align="center" fixed="right">
          <template #default="scope">
            <div class="table-action-group">
              <el-button
                  link
                  type="primary"
                  @click="handleUpdate(scope.row)"
                  :icon="Edit">
                编辑
              </el-button>
              <el-button
                  link
                  type="warning"
                  @click="handleOpenAiAudit(scope.row)"
                  :icon="Warning"
              >
                AI审核
              </el-button>
              <el-button
                  v-if="scope.row.accountBanned === 1 || scope.row.banned === 1"
                  link
                  type="success"
                  @click="handleUnban(scope.row)"
                  :icon="Unlock"
              >
                解封
              </el-button>
              <el-button
                  v-else
                  link
                  type="warning"
                  @click="handleOpenBan(scope.row)"
                  :icon="Lock"
              >
                封禁
              </el-button>
              <el-button
                  link
                  type="danger"
                  @click="handleDelete(scope.row.id)"
                  :icon="Delete"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 15px">
        <el-pagination
            @current-change="load"
            @size-change="load"
            v-model:current-page="data.pageNum"
            v-model:page-size="data.pageSize"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="data.total"
        />
      </div>
    </el-card>

    <el-dialog v-model="data.formVisible" title="用户信息" width="40vw" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" style="padding: 10px 50px 0 20px;" label-position="right" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input :disabled="data.form.id" v-model="data.form.username" autocomplete="off" placeholder="请输入用户名" clearable/>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="data.form.nickname" autocomplete="off" placeholder="请输入昵称" clearable/>
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="data.form.sex">
            <el-radio value="男">
              <el-icon style="padding-right: 5px"><Male /></el-icon>男
            </el-radio>
            <el-radio value="女">
              <el-icon style="padding-right: 5px"><Female /></el-icon>女
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="data.form.phone" autocomplete="off" placeholder="请输入手机号" clearable/>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="data.form.email" autocomplete="off" placeholder="请输入邮箱" clearable/>
        </el-form-item>
        <el-form-item label="生日" prop="birth">
          <el-date-picker
              style="width: 100%"
              v-model="data.form.birth"
              type="date"
              placeholder="请选择日期"
              format="YYYY/MM/DD"
              value-format="YYYY-MM-DD"
              clearable
          />
        </el-form-item>
        <el-form-item label="头像">
          <el-upload
              class="avatar-uploader"
              action="http://localhost:8080/files/upload"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
          >
            <img v-if="data.form.avatar" :src="data.form.avatar" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="个人简介" prop="descriptiontext">
          <el-input v-model="data.form.descriptiontext" autocomplete="off" placeholder="介绍一下自己吧" type="textarea" :autosize="{ minRows: 1, maxRows: 6 }" maxlength="200" show-word-limit/>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="data.formVisible = false">取消</el-button>
          <el-button type="primary" @click="save">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 账号封禁弹窗：仅针对存钱日记、存钱计划、个人简介等社区内容违规 -->
    <el-dialog v-model="data.banVisible" title="封禁违规账号（存钱日记 / 存钱计划 / 个人简介违规）" width="500px" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="data.banForm" label-position="top" style="padding: 4px 10px 0;">
        <el-alert
            title="注意：全账号封禁仅适用于「存钱日记」、「存钱计划」或「个人简介」出现违规违禁内容。封禁后该用户将被强制下线且无法登录。若仅需限制 AI 对话功能，请使用【AI审核】。"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 14px;"
        />
        <el-form-item label="目标账号">
          <el-input :model-value="data.banForm.username + (data.banForm.nickname ? ' (' + data.banForm.nickname + ')' : '')" disabled />
        </el-form-item>
        <el-form-item label="快捷违规原因（日记 / 计划 / 简介违规）">
          <div style="display: flex; flex-wrap: wrap; gap: 6px;">
            <el-tag
                v-for="preset in banReasonPresets"
                :key="preset"
                size="small"
                effect="plain"
                style="cursor: pointer;"
                @click="data.banForm.banReason = preset"
            >
              {{ preset }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="封禁原因说明（将展示给被封禁用户）">
          <el-input
              v-model="data.banForm.banReason"
              type="textarea"
              :rows="3"
              maxlength="120"
              show-word-limit
              placeholder="请输入账号封禁原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="data.banVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmBan">确认封禁账号</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- AI 安全审计与阶梯封禁统一审核弹窗 -->
    <el-dialog
        v-model="data.aiAuditVisible"
        :title="`AI 安全审计与阶梯封禁管理 — ${data.aiAuditRow.username || ''}`"
        width="720px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <div style="padding: 0 6px;">
        <!-- 阶梯规则提示 -->
        <el-alert
            title="阶梯式 AI 封禁规则：24 小时内累计违规超过 10 次将自动封禁 AI 对话功能。第 1 次封禁 24 小时 · 第 2 次封禁 3 天 · 第 3 次封禁 1 个月 · 第 4 次永久封禁。管理员可在此统一审核、解除或降级处罚。"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 14px;"
        />

        <!-- 当前封禁状态概览卡片 -->
        <div style="padding: 12px 16px; border-radius: 10px; background: var(--el-fill-color-light); margin-bottom: 16px; display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px;">
          <div>
            <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 6px;">
              <span style="font-weight: 700; font-size: 14px;">当前 AI 权限状态：</span>
              <el-tag v-if="data.aiAuditRow.aiBanned === 1" type="danger" effect="dark" size="small">
                已封禁（第 {{ data.aiAuditRow.aiBanLevel || 1 }} 档 · {{ data.aiAuditRow.aiBanDuration || '24小时' }}）
              </el-tag>
              <el-tag v-else type="success" effect="dark" size="small">正常可用</el-tag>
              <el-tag type="warning" effect="plain" size="small">
                累计触发封禁：{{ data.aiAuditRow.aiBanCount || 0 }} 次
              </el-tag>
              <el-tag type="info" effect="plain" size="small">
                24h 内违规：{{ data.aiAuditLogs.length }} 次
              </el-tag>
            </div>
            <div v-if="data.aiAuditRow.aiBanned === 1" style="font-size: 12.5px; color: var(--el-text-color-regular); line-height: 1.6;">
              <div><strong>封禁原因：</strong>{{ data.aiAuditRow.aiBanReason || '24 小时内累计违规超过 10 次自动处分' }}</div>
              <div>
                <strong>封禁时间：</strong>{{ data.aiAuditRow.aiBanTime || '-' }}
                <span style="margin: 0 8px;">|</span>
                <strong>封禁时长：</strong>{{ data.aiAuditRow.aiBanDuration || '24小时' }}
                <span style="margin: 0 8px;">|</span>
                <strong>预计解封：</strong>{{ data.aiAuditRow.aiBanExpireTime || '永久' }}
              </div>
            </div>
          </div>

          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            <el-button
                v-if="data.aiAuditRow.aiBanned === 1"
                type="success"
                size="small"
                @click="submitAiAudit('UNBAN', false)"
            >
              解除当前封禁
            </el-button>
            <el-button
                v-if="data.aiAuditRow.aiBanned === 1 || (data.aiAuditRow.aiBanCount || 0) > 0"
                type="primary"
                plain
                size="small"
                @click="submitAiAudit('UNBAN', true)"
            >
              解封并重置累犯阶梯为 0
            </el-button>
          </div>
        </div>

        <!-- 审核降级 / 手动调整处罚档位 -->
        <el-card shadow="never" style="margin-bottom: 16px;">
          <template #header>
            <div style="display: flex; align-items: center; justify-content: space-between;">
              <span style="font-weight: 700; font-size: 13.5px;">管理员审核降级 / 调整 AI 封禁档位</span>
              <span style="font-size: 12px; color: var(--el-text-color-secondary);">支持将永久/长时封禁降级为低档位，或手动执行阶梯封禁</span>
            </div>
          </template>
          <el-form :model="data.aiAuditForm" label-position="top">
            <el-form-item label="选择目标处罚档位">
              <el-radio-group v-model="data.aiAuditForm.targetLevel">
                <el-radio-button :value="1">第 1 档：24 小时</el-radio-button>
                <el-radio-button :value="2">第 2 档：3 天</el-radio-button>
                <el-radio-button :value="3">第 3 档：1 个月</el-radio-button>
                <el-radio-button :value="4">第 4 档：永久封禁</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="审核处理原因说明（将同步显示在用户 AI 输入框上方的封禁通知气泡中）">
              <el-input
                  v-model="data.aiAuditForm.reason"
                  placeholder="可填写降级或处分原因，留空将自动生成规范说明"
                  maxlength="140"
                  show-word-limit
              />
            </el-form-item>
            <div style="display: flex; justify-content: flex-end; gap: 8px;">
              <el-button
                  v-if="data.aiAuditRow.aiBanned === 1 && data.aiAuditForm.targetLevel < (data.aiAuditRow.aiBanLevel || 4)"
                  type="warning"
                  @click="submitAiAudit('DOWNGRADE')"
              >
                确认降级为第 {{ data.aiAuditForm.targetLevel }} 档
              </el-button>
              <el-button
                  type="danger"
                  plain
                  @click="submitAiAudit('BAN')"
              >
                应用第 {{ data.aiAuditForm.targetLevel }} 档 AI 封禁
              </el-button>
            </div>
          </el-form>
        </el-card>

        <!-- 24 小时内 AI 违规行为记录表格 -->
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;">
          <span style="font-weight: 700; font-size: 13.5px;">最近 24 小时内 AI 违规拦截记录（共 {{ data.aiAuditLogs.length }} 条）</span>
          <el-button
              v-if="data.aiAuditLogs.length > 0"
              link
              type="danger"
              size="small"
              @click="submitAiAudit('CLEAR_VIOLATIONS')"
          >
            清空 24h 违规记录
          </el-button>
        </div>
        <el-table :data="data.aiAuditLogs" size="small" border max-height="230" empty-text="最近 24 小时内无任何 AI 违规行为记录">
          <el-table-column label="时间" prop="time" width="155" />
          <el-table-column label="拦截防线" prop="source" width="135" />
          <el-table-column label="违规类型" prop="category" width="175" show-overflow-tooltip />
          <el-table-column label="拦截摘要" prop="snippet" show-overflow-tooltip />
          <el-table-column label="状态" width="85" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.settled ? 'info' : 'warning'" size="small">
                {{ scope.row.settled ? '已处分' : '计次中' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="data.aiAuditVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {reactive, ref} from "vue";
import {Delete, Edit, Female, Lock, Male, Plus, Search, Unlock, Warning} from "@element-plus/icons-vue";
import {request} from "@/utils/request.js";
import {ElMessage, ElMessageBox} from 'element-plus'
import { validatePhoneRule, validateEmailRule } from "@/utils/validator.js";

// 账号封禁原因预设：严格对应存钱日记、存钱计划、个人简介等社区内容违规
const banReasonPresets = [
  '存钱日记（理财日记）包含违禁、低俗或违法违规内容，违反《星界用户服务协议》',
  '存钱计划标题或详情存在违规导流、欺诈或不良信息',
  '个人简介或昵称包含违禁敏感词汇、冒充官方或恶意营销信息',
  '多次在存钱日记/存钱计划/个人简介中发布违规内容，屡教不改'
]

const data = reactive({
  user: JSON.parse(localStorage.getItem('loginToken') || '{}'),
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  banVisible: false,
  banForm: {
    id: null,
    username: '',
    nickname: '',
    banReason: ''
  },
  aiAuditVisible: false,
  aiAuditRow: {},
  aiAuditLogs: [],
  aiAuditForm: {
    targetLevel: 1,
    reason: ''
  },
  ids:[],
  rules: {
    username: [
      { required: true, message: '请输入有效的用户名', trigger: 'blur' }
    ],
    phone: [
      { required: true, message: '请输入有效的手机号', trigger: 'blur' },
      { validator: validatePhoneRule, trigger: 'blur' }
    ],
    email: [
      { required: true, message: '请输入有效的邮箱地址', trigger: 'blur' },
      { validator: validateEmailRule, trigger: 'blur' }
    ]
  }
})

const formRef = ref()

const load = () => {
  request.get('/user/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      username: data.username,
    }
  }).then(res => {
    data.tableData = res.data.list
    data.total = res.data.total
  })
}
load()

const reset = () => {
  data.username = null
  load()
}

const handleAdd = () => {
  data.form = {}
  data.formVisible = true
}

const handleUpdate = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

const handleOpenAiAudit = (row) => {
  data.aiAuditRow = JSON.parse(JSON.stringify(row))
  let logs = []
  try {
    logs = JSON.parse(row.violationLogs || '[]')
  } catch {
    logs = []
  }
  data.aiAuditLogs = Array.isArray(logs) ? logs.slice().reverse() : []
  const currentLvl = row.aiBanLevel || 1
  data.aiAuditForm = {
    targetLevel: row.aiBanned === 1 && currentLvl > 1 ? currentLvl - 1 : currentLvl,
    reason: row.aiBanReason || ''
  }
  data.aiAuditVisible = true
}

const submitAiAudit = (action, resetCount = false) => {
  const userId = data.aiAuditRow.id
  if (!userId) return
  request.put('/user/aiBan', {
    userId,
    action,
    resetCount,
    targetLevel: data.aiAuditForm.targetLevel,
    reason: data.aiAuditForm.reason
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('AI 安全审核操作已生效')
      request.get('/user/selectPage', {
        params: {
          pageNum: data.pageNum,
          pageSize: data.pageSize,
          username: data.username,
        }
      }).then(pageRes => {
        data.tableData = pageRes.data.list
        data.total = pageRes.data.total
        const updated = data.tableData.find(u => u.id === userId)
        if (updated) {
          handleOpenAiAudit(updated)
        } else {
          data.aiAuditVisible = false
        }
      })
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  })
}

const handleOpenBan = (row) => {
  data.banForm = {
    id: row.id,
    username: row.username,
    nickname: row.nickname,
    banReason: banReasonPresets[0]
  }
  data.banVisible = true
}

const confirmBan = () => {
  request.put('/user/ban', {
    id: data.banForm.id,
    banned: 1,
    banReason: data.banForm.banReason
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success(`已封禁账号 ${data.banForm.username}`)
      data.banVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const handleUnban = (row) => {
  ElMessageBox.confirm(`确定要解除账号「${row.username}」的封禁状态，恢复其正常登录权限吗？`, '解封确认', {
    confirmButtonText: '确认解封',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    request.put('/user/ban', {
      id: row.id,
      banned: 0,
      banReason: ''
    }).then(res => {
      if (res.code === 200) {
        ElMessage.success(`已解除账号 ${row.username} 的封禁`)
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

const handleDelete = (id) => {
  ElMessageBox.confirm('删除用户信息是永久且不可逆的。您确定要删除吗？','删除确认',{
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    request.delete('/user/delete/' +id).then(res=>{
      if (res.code === 200) {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch()
}

const handleSelectionChange = (rows) => {
  data.ids = rows.map(row => row.id)
}

const save = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      data.form.id ? update() : add()
    }
  })
}

const add = () => {
  const username = data.form.username
  request.post('/user/insert', data.form).then(res => {
    if (res.code === 200) {
      data.formVisible = false
      load()
      const defaultPassword = res.data || '123456'
      ElMessageBox.alert(
          `<div style="font-size: 14px; line-height: 1.8;">
            <p>普通用户账户 <b>${username}</b> 已成功创建！</p>
            <p>系统已为其分配初始默认密码：</p>
            <div style="display: flex; align-items: center; justify-content: center; gap: 10px; margin: 12px 0;">
              <span style="padding: 8px 16px; background: rgba(22, 119, 255, 0.08); border: 1px dashed #1677ff; border-radius: 8px; font-family: monospace; font-size: 16px; font-weight: bold; color: #1677ff; letter-spacing: 1px; user-select: all;">
                ${defaultPassword}
              </span>
              <button
                type="button"
                onclick="window.__copyTempPassword(this, '${defaultPassword}')"
                title="点击复制初始密码"
                style="display: inline-flex; align-items: center; gap: 5px; padding: 7px 14px; background: #1677ff; color: #ffffff; border: none; border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.2s ease; box-shadow: 0 2px 8px rgba(22, 119, 255, 0.28);"
              >
                <svg viewBox="0 0 1024 1024" width="13" height="13" fill="currentColor" style="vertical-align: -1px;"><path d="M768 832a64 64 0 0 1-64 64H256a64 64 0 0 1-64-64V384a64 64 0 0 1 64-64h448a64 64 0 0 1 64 64v448z m64-576V192a64 64 0 0 0-64-64H320a64 64 0 0 0-64 64v64h448a64 64 0 0 1 64 64v320h64a64 64 0 0 0 64-64V256z"></path></svg>
                <span>点击复制</span>
              </button>
            </div>
            <p style="color: #f59e0b; font-size: 12.5px;">请复制并告知该用户。用户首次登录时将提示修改密码。</p>
          </div>`,
          '用户创建成功',
          {
            dangerouslyUseHTMLString: true,
            confirmButtonText: '我已复制并知晓',
            type: 'success'
          }
      )
    } else {
      ElMessage.error(res.msg || '添加失败')
    }
  })
}

const update = () => {
  request.put('/user/update', data.form).then(res => {
    if (res.code === 200) {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请选择数据')
  } else {
    ElMessageBox.confirm('删除用户信息是永久且不可逆的。您确定要删除吗？','删除确认',{
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      request.delete('/user/delete/batch', {data: data.ids}).then(res => {
        if (res.code === 200) {
          ElMessage.success('操作成功')
          load()
        } else {
          ElMessage.error(res.msg)
        }
      })
    }).catch()
  }
}

const handleAvatarSuccess = (res) => {
  console.log(res.data)
  data.form.avatar = res.data
}
</script>

<style scoped>
.avatar-uploader {
  width: 100%;
  margin-bottom: 20px;
}
.avatar-uploader .avatar {
  width: 80px;
  height: 80px;
  display: block;
}
</style>

<style>
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 80px;
  height: 80px;
  text-align: center;
}
</style>
