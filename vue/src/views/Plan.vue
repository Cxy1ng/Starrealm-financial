<template>
  <div>
    <el-card>
      <div class="table-search-row">
        <el-input
            v-model="data.name"
            class="search-input"
            placeholder="输入计划名称以查询"
            clearable
            :prefix-icon="Search"
        />
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        <el-button class="btn-toolbar-reset" :icon="Refresh" @click="reset">重置</el-button>
        <el-button type="success" :icon="Plus" @click="handleAdd" v-if="data.user.role === 'USER'">新建计划</el-button>
        <el-button type="danger" :icon="Delete" @click="delBatch" v-if="data.user.perm >= 10">批量删除</el-button>
      </div>
    </el-card>

    <el-card style="margin-top: 10px" v-if="data.tableData.length">
      <el-row :gutter="20">
        <el-col
            v-for="item in data.tableData"
            :key="item.id"
            :xs="24"
            :sm="24"
            :md="12"
            :lg="12"
        >
          <el-card class="plan-card" shadow="hover" @click="router.push('/main/planDetail?planId=' + item.id)">
            <div class="plan-card-body">
              <el-image :src="item.cover" fit="cover" class="plan-cover" />
              <div class="plan-content">
                <div class="plan-header">
                  <el-tag :type="statusTagType(item.status)" size="small">{{ item.status }}</el-tag>
                  <div class="plan-title">{{ item.name }}</div>
                  <div class="plan-date"><i class="el-icon-date"></i> {{ item.end }}</div>
                </div>
                <BossProgressBar
                    :percent="getRealPercent(item)"
                    :from-percent="animMap[item.id]?.fromPct ?? getRealPercent(item)"
                    :animate-trigger="animMap[item.id]?.trigger ?? 0"
                />
                <div class="plan-footer">
                  <div class="plan-user-info">
                    <i class="el-icon-user"></i> {{ item.userName }}
                    <span style="margin-left: 12px"><i class="el-icon-date"></i> {{ item.date }}</span>
                  </div>
                  <div class="plan-actions">
                    <el-button
                        size="small"
                        type="primary"
                        :disabled="isPlanExpired(item)"
                        :title="isPlanExpired(item) ? '该存钱计划已结束过期，不可继续存钱' : '存入一笔金额'"
                        @click.stop="addPlanDetail(item)"
                    >
                      {{ isPlanExpired(item) ? '已结束' : '存一笔' }}
                    </el-button>
                    <el-button size="small" @click.stop="handleEdit(item)" :icon="Edit" link>编辑</el-button>
                    <el-button size="small" @click.stop="del(item.id)" :icon="Delete" link type="danger">删除</el-button>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
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

    <el-card style="margin-top: 10px" v-else>
      <el-card class="empty-placeholder-card" shadow="never">
        <div class="empty-card-inner">
          <div class="empty-card-title">暂无存钱计划</div>
          <div class="empty-card-sub">
            {{ data.user.role === 'USER' ? '当前还没有任何存钱计划，点击上方「新建计划」开启您的第一个储蓄目标吧' : '当前没有任何存钱计划记录' }}
          </div>
          <el-button v-if="data.user.role === 'USER'" type="primary" size="small" style="margin-top: 8px" @click="handleAdd">
            新建存钱计划
          </el-button>
        </div>
      </el-card>
    </el-card>

    <el-dialog title="计划信息" v-model="data.fromVisible" width="70vh" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="data.form" label-width="auto" style="padding-right: 50px" :rules="data.rules" ref="formRef">
        <el-form-item label="计划名称" prop="name">
          <el-input v-model="data.form.name" placeholder="计划名称"></el-input>
        </el-form-item>
        <el-form-item label="封面" prop="cover">
          <el-upload
              action="http://localhost:8080/files/upload"
              :headers="{ token: data.user.token }"
              list-type="picture"
              :on-success="handleImgSuccess"
          >
            <el-button type="primary">上传封面</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="总金额" prop="money">
          <el-input v-model="data.form.money" placeholder="总金额"></el-input>
        </el-form-item>
        <el-form-item label="开始日期" prop="start">
          <el-date-picker value-format="YYYY-MM-DD" format="YYYY/MM/DD" v-model="data.form.start"
                          placeholder="开始日期" style="width: 100%"></el-date-picker>
        </el-form-item>
        <el-form-item label="结束日期" prop="end">
          <el-date-picker value-format="YYYY-MM-DD" format="YYYY/MM/DD" v-model="data.form.end"
                          placeholder="结束日期" style="width: 100%"></el-date-picker>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="data.fromVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="存钱" v-model="data.detailFromVisible" width="70vh" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="data.detailForm" label-width="auto" style="padding-right: 50px" :rules="data.rules" ref="formRef">
        <el-form-item label="金额" prop="money">
          <el-input v-model="data.detailForm.money" placeholder="金额"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="data.detailFromVisible = false">取消</el-button>
          <el-button type="primary" @click="savePlanDetail">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import {reactive, ref} from "vue";
import request from "@/utils/request.js";
import {ElMessage, ElMessageBox} from "element-plus";
import { useRouter } from 'vue-router'
import {Delete, Edit, Search, Refresh, Plus} from "@element-plus/icons-vue";
import BossProgressBar from "@/components/BossProgressBar.vue";

const router = useRouter()
const formRef = ref()

const data = reactive({
  user: JSON.parse(localStorage.getItem('loginToken') || '{}'),
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  name: null,
  fromVisible: false,
  detailFromVisible: false,
  form: {},
  rules: {
    name: [
      {required: true, message: '请输入标题', trigger: 'blur'},
    ],
    money: [
      { required: true, message: '请输入金额', trigger: 'blur' },
      { pattern: /^(0|[1-9]\d*)(\.\d{1,2})?$/, message: '请输入最多两位小数的正数', trigger: 'blur' },
    ],
    start: [
      {required: true, message: '请输入开始日期', trigger: 'blur'},
    ],
    end: [
      {required: true, message: '请输入结束日期', trigger: 'blur'},
    ]
  },
  ids: [],
  detailForm: {}
})

// 实时根据存钱明细累计金额与目标金额计算真实百分比（支持超过 100% 的超额数值）
const getRealPercent = (item) => {
  if (!item) return 0
  const target = Number(item.money) || 0
  if (target > 0 && item._realSum !== undefined) {
    return Math.max(0, Math.round((Number(item._realSum) * 100) / target))
  }
  return Math.max(0, Number(item.percent) || 0)
}

// 顺序充能触发器映射表：记录每个计划存一笔前的起始百分比与动画触发计数
const animMap = reactive({})

const load = (animatePlanId = null, prevPct = null) => {
  request.get('/plan/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      name: data.name,
    }
  }).then(res => {
    const list = res.data?.list || []
    data.tableData = list
    data.total = res.data?.total || 0
    // 拉取全量明细并在前端按计划汇总真实存入总额，彻底突破旧后端 100% 封顶限制
    request.get('/planDetail/selectAll').then(detailRes => {
      const details = detailRes.data || []
      const sumMap = {}
      for (const d of details) {
        const pid = d.planId
        if (pid != null) {
          sumMap[pid] = (sumMap[pid] || 0) + (Number(d.money) || 0)
        }
      }
      data.tableData.forEach(p => {
        p._realSum = sumMap[p.id] || 0
        p.percent = getRealPercent(p)
        if (animatePlanId && p.id === animatePlanId && prevPct !== null) {
          const prevTrig = animMap[p.id]?.trigger || 0
          animMap[p.id] = { fromPct: prevPct, trigger: prevTrig + 1 }
        } else if (!animMap[p.id]) {
          animMap[p.id] = { fromPct: p.percent, trigger: 0 }
        }
        if (isPlanExpired(p)) {
          p.status = '已结束'
        } else if (p._realSum >= (Number(p.money) || 0) && (Number(p.money) || 0) > 0) {
          p.status = '已完成'
        }
      })
    }).catch(() => {})
  })
}
load()

const reset = () => {
  data.name = null
  load()
}

const handleCurrentChange = (pageNum) => {
  load(pageNum)
}

const handleAdd = () => {
  data.form = {}
  data.fromVisible = true
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.fromVisible = true
}

const save = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      request({
        url: data.form.id ? '/plan/update' : '/plan/add',
        method: data.form.id ? 'PUT' : 'POST',
        data: data.form
      }).then(res => {
        if (res.code === 200) {
          ElMessage.success('保存成功')
          load()
          data.fromVisible = false
        } else {
          ElMessage.error(res.msg)
        }
      })
    }
  })
}

const del = (id) => {
  ElMessageBox.confirm('删除计划是永久且不可逆的。您确定要删除吗？','删除确认',{
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    request.delete('/plan/delete/' + id).then(res => {
      if (res.code === 200) {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('Choose an row')
  } else {
    ElMessageBox.confirm('删除计划是永久且不可逆的。您确定要删除吗？','删除确认',{
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      request.delete('/plan/delete/batch', { data: data.ids }).then(res => {
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

const handleSelectionChange = (rows) => {
  data.ids = rows.map(row => row.id)
}

const handleImgSuccess = (res) => {
  data.form.cover = res.data
}

const isPlanExpired = (item) => {
  if (!item) return false
  if (item.status === '已结束') return true
  if (!item.end) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const endDate = new Date(String(item.end).replace(/-/g, '/'))
  endDate.setHours(0, 0, 0, 0)
  return endDate.getTime() < today.getTime()
}

const addPlanDetail = (item) => {
  if (isPlanExpired(item)) {
    ElMessage.warning('该存钱计划已结束过期，已停止存钱功能')
    return
  }
  data.detailFromVisible = true
  data.detailForm = { planId: item.id }
}

const savePlanDetail = () => {
  const targetPlanId = data.detailForm.planId
  const targetPlan = data.tableData.find(p => p.id === targetPlanId)
  const prevPct = targetPlan ? getRealPercent(targetPlan) : 0
  request.post('/planDetail/add', data.detailForm).then(res => {
    if (res.code === 200) {
      ElMessage.success('存入成功，正在充能进度条...')
      load(targetPlanId, prevPct)
      data.detailFromVisible = false
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const statusTagType = (status) => {
  switch (status) {
    case '未开始': return 'warning'
    case '进行中': return 'primary'
    case '已完成': return 'success'
    case '已结束': return 'danger'
    default: return ''
  }
}


</script>
<style scoped>
.plan-card {
  margin-top: 10px;
}

.plan-card-body {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .plan-card-body {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .plan-content {
    align-items: center;
  }

  .plan-header,
  .plan-footer {
    flex-direction: column;
    align-items: center;
  }

  .plan-title {
    text-align: center;
    font-size: 16px;
  }

  .plan-date {
    margin-top: 4px;
  }

  .plan-actions {
    justify-content: center;
    flex-wrap: wrap;
  }

  .plan-cover {
    width: 100%;
    max-width: 300px;
  }
}


.plan-cover {
  width: 100px;
  height: 100px;
  border-radius: 6px;
  flex-shrink: 0;
}

.plan-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.plan-header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.plan-title {
  font-size: 16px;
  font-weight: 600;
  flex: 1;
}

.plan-date {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.plan-user-info {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

/* 四层 Boss 充能血条及双模态（即时推进电弧 + 持续均匀环绕粒子）特效系统 */
.boss-progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 14px 0 10px;
}

.boss-bar-track {
  position: relative;
  flex: 1;
  height: 10px;
  border-radius: 999px;
  background-color: rgba(148, 163, 184, 0.22);
  overflow: visible;
}

html.dark .boss-bar-track {
  background-color: rgba(255, 255, 255, 0.1);
}

.boss-bar-layer {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 999px;
  overflow: visible;
}

/* 第 1 层：系统主题色 */
.layer-theme {
  background: linear-gradient(90deg, var(--el-color-primary-dark-2, var(--el-color-primary)) 0%, var(--el-color-primary) 70%, var(--el-color-primary-light-3, var(--el-color-primary)) 100%);
  box-shadow: 0 0 8px rgba(var(--theme-rgb, 22, 119, 255), 0.45);
  z-index: 1;
}

/* 第 2 层：星云紫 */
.layer-purple {
  background: linear-gradient(90deg, #7e22ce 0%, #a855f7 65%, #d8b4fe 100%);
  box-shadow: 0 0 12px rgba(168, 85, 247, 0.65);
  z-index: 2;
}

/* 第 3 层：烈焰红 */
.layer-red {
  background: linear-gradient(90deg, #b91c1c 0%, #ef4444 65%, #fca5a5 100%);
  box-shadow: 0 0 14px rgba(239, 68, 68, 0.78);
  z-index: 3;
}

/* 第 4 层：流光金（强电雷霆） */
.layer-gold {
  background: linear-gradient(90deg, #d97706 0%, #f59e0b 45%, #fde047 85%, #ffffff 100%);
  box-shadow: 0 0 18px rgba(250, 204, 21, 0.9), 0 0 6px rgba(255, 255, 255, 0.8);
  z-index: 4;
}

/* ==================== 模态 A：已充能区域持续均匀分布的微观粒子与环绕电弧特效 ==================== */
.ambient-field {
  position: absolute;
  inset: -4px 0;
  pointer-events: none;
  overflow: visible;
}

.ambient-node {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

/* 第 2 层（紫色）：微观能量粒子 + 细微电弧围绕 */
.purple-field .ambient-node {
  width: 3.5px;
  height: 3.5px;
  border-radius: 50%;
  background: #f3e8ff;
  box-shadow: 0 0 5px #c084fc, 0 0 9px #a855f7;
  animation: dustDrift 2.4s ease-in-out infinite;
}
.purple-field .ambient-node:nth-child(odd) {
  animation-direction: reverse;
}
.purple-field .ambient-node::after {
  content: '';
  position: absolute;
  left: -4px;
  top: -3px;
  width: 10px;
  height: 7px;
  border-top: 1.2px solid rgba(216, 180, 254, 0.85);
  border-radius: 50%;
  animation: miniArcFlicker 0.85s steps(2, start) infinite;
}

/* 第 3 层（红色）：跃动能量粒子 + 中等仿真电弧围绕 */
.red-field .ambient-node {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #fee2e2;
  box-shadow: 0 0 6px #ef4444, 0 0 12px #dc2626;
  animation: dustDrift 1.8s ease-in-out infinite;
}
.red-field .ambient-node::before,
.red-field .ambient-node::after {
  content: '';
  position: absolute;
  left: -5px;
  top: -4px;
  width: 12px;
  height: 10px;
  border-top: 1.5px solid #fca5a5;
  border-bottom: 1.5px solid #ef4444;
  border-radius: 40%;
  filter: drop-shadow(0 0 4px #ef4444);
  animation: redArcOrbit 0.55s linear infinite;
}

/* 第 4 层（金色）：高能雷电色强电电弧流光围绕 */
.gold-field {
  inset: -7px 0;
}
.gold-lightning-svg {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  overflow: visible;
}
.bolt-line {
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.bolt-1 {
  stroke: #fef08a;
  stroke-width: 1.8;
  filter: drop-shadow(0 0 6px #f59e0b) drop-shadow(0 0 12px #fde047);
  stroke-dasharray: 20 10;
  animation: thunderBoltCrawl 0.48s linear infinite;
}
.bolt-2 {
  stroke: #ffffff;
  stroke-width: 1.3;
  filter: drop-shadow(0 0 7px #fbbf24);
  stroke-dasharray: 14 16;
  animation: thunderBoltCrawlReverse 0.36s linear infinite;
}
.gold-field .ambient-node {
  width: 4.5px;
  height: 4.5px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 0 8px #fde047, 0 0 16px #f59e0b;
  animation: goldSparkPulse 0.7s ease-in-out infinite alternate;
}

/* ==================== 模态 B：存一笔后的即时推进特效（彗星尾焰加减速推进） ==================== */
.spark-emitter {
  position: absolute;
  top: 50%;
  width: 28px;
  height: 28px;
  transform: translate(-50%, -50%);
  pointer-events: none;
  z-index: 12;
  /* 弹性逐渐加速的物理惯性动效 */
  transition: left 1.4s cubic-bezier(0.25, 1, 0.5, 1);
}

.spark-core {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 9px;
  height: 9px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: #ffffff;
  filter: drop-shadow(0 0 6px #ffffff);
}

/* 龟派气功 / 彗星拖尾尾焰 */
.comet-tail {
  position: absolute;
  right: 50%;
  top: 50%;
  height: 6px;
  transform: translateY(-50%);
  border-radius: 999px;
  pointer-events: none;
  transform-origin: right center;
}

.spark-stage-1 .comet-tail {
  background: linear-gradient(90deg, transparent, var(--el-color-primary));
  width: 32px;
  box-shadow: 0 0 10px var(--el-color-primary);
}
.spark-stage-2 .comet-tail {
  background: linear-gradient(90deg, transparent, #c084fc, #9333ea);
  width: 38px;
  box-shadow: 0 0 12px #a855f7;
}
.spark-stage-3 .comet-tail {
  background: linear-gradient(90deg, transparent, #fca5a5, #ef4444);
  width: 44px;
  box-shadow: 0 0 14px #ef4444;
}
.spark-stage-4 .comet-tail {
  background: linear-gradient(90deg, transparent, #fde047, #f59e0b, #ffffff);
  width: 52px;
  box-shadow: 0 0 18px #f59e0b, 0 0 8px #ffffff;
}

.tip-arc-svg {
  position: absolute;
  left: -12px;
  top: -12px;
  width: 48px;
  height: 48px;
  overflow: visible;
}

.tip-arc-path {
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
  animation: tipArcJitter 0.12s steps(2, start) infinite;
}
.tip-arc-path.a-2 { animation-delay: 0.03s; }
.tip-arc-path.a-3 { animation-delay: 0.07s; }
.tip-arc-path.a-4 { animation-delay: 0.1s; }

/* 各阶段推进电弧颜色配置 */
.spark-stage-1 .spark-core {
  box-shadow: 0 0 10px var(--el-color-primary), 0 0 20px var(--el-color-primary);
}
.spark-stage-2 .spark-core {
  box-shadow: 0 0 12px #e9d5ff, 0 0 24px #a855f7;
}
.spark-stage-2 .tip-arc-path {
  stroke: #e9d5ff;
  stroke-width: 1.6;
  filter: drop-shadow(0 0 5px #a855f7);
  transform: scale(0.7);
}

.spark-stage-3 .spark-core {
  box-shadow: 0 0 14px #fecaca, 0 0 28px #ef4444;
}
.spark-stage-3 .tip-arc-path {
  stroke: #fecaca;
  stroke-width: 2.1;
  filter: drop-shadow(0 0 6px #ef4444);
  transform: scale(0.92);
}

.spark-stage-4 .spark-core {
  width: 11px;
  height: 11px;
  box-shadow: 0 0 18px #ffffff, 0 0 32px #fde047, 0 0 46px #f59e0b;
}
.spark-stage-4 .tip-arc-path {
  stroke: #fef08a;
  stroke-width: 2.7;
  filter: drop-shadow(0 0 9px #f59e0b) drop-shadow(0 0 16px #fde047);
  transform: scale(1.18);
}

@keyframes dustDrift {
  0%, 100% { transform: translate(-50%, -50%) translateY(-3px) scale(0.85); opacity: 0.5;
  }
  50% { transform: translate(-50%, -50%) translateY(3px) scale(1.15); opacity: 1; }
}

@keyframes miniArcFlicker {
  0%, 100% { transform: rotate(-15deg) scaleX(0.8); opacity: 0.4; }
  50% { transform: rotate(20deg) scaleX(1.2); opacity: 0.95; }
}

@keyframes redArcOrbit {
  0% { transform: rotate(0deg) scale(0.9); }
  50% { transform: rotate(180deg) scale(1.12); }
  100% { transform: rotate(360deg) scale(0.9); }
}

@keyframes thunderBoltCrawl {
  0% { stroke-dashoffset: 0; opacity: 0.95; }
  50% { opacity: 0.65; }
  100% { stroke-dashoffset: -56; opacity: 1; }
}

@keyframes thunderBoltCrawlReverse {
  0% { stroke-dashoffset: 0; opacity: 0.85; }
  100% { stroke-dashoffset: 56; opacity: 1; }
}

@keyframes goldSparkPulse {
  0% { transform: translate(-50%, -50%) scale(0.75); opacity: 0.6; }
  100% { transform: translate(-50%, -50%) scale(1.3); opacity: 1; }
}

@keyframes tipArcJitter {
  0% { transform: rotate(0deg) scale(0.95); opacity: 0.95; }
  50% { transform: rotate(16deg) scale(1.08); opacity: 1; }
  100% { transform: rotate(-14deg) scale(0.9); opacity: 0.8; }
}

@keyframes burstFly1 {
  0% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
  100% { transform: translate(-16px, -11px) scale(0.1); opacity: 0; }
}
@keyframes burstFly2 {
  0% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
  100% { transform: translate(-14px, 11px) scale(0.1); opacity: 0; }
}
@keyframes burstFly3 {
  0% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
  100% { transform: translate(10px, -12px) scale(0.1); opacity: 0; }
}
@keyframes burstFly4 {
  0% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
  100% { transform: translate(11px, 12px) scale(0.1); opacity: 0; }
}

@keyframes tipArcJitter {
  0% { transform: rotate(0deg) scale(0.95); opacity: 0.95; }
  50% { transform: rotate(18deg) scale(1.08); opacity: 1; }
  100% { transform: rotate(-16deg) scale(0.9); opacity: 0.8; }
}

@keyframes ambientPurpleFloat {
  0%, 100% { transform: translate(-50%, -50%) translateY(-5px) scale(0.85); opacity: 0.45; }
  50% { transform: translate(-50%, -50%) translateY(5px) scale(1.15); opacity: 1; }
}
@keyframes ambientPurpleFloatAlt {
  0%, 100% { transform: translate(-50%, -50%) translateY(5px) scale(0.85); opacity: 0.45; }
  50% { transform: translate(-50%, -50%) translateY(-5px) scale(1.15); opacity: 1; }
}
@keyframes miniArcFlicker {
  0%, 100% { transform: rotate(-18deg) scaleX(0.85); opacity: 0.35; }
  50% { transform: rotate(22deg) scaleX(1.15); opacity: 0.95; }
}

@keyframes ambientRedFloat {
  0%, 100% { transform: translate(-50%, -50%) translateY(-6px) scale(0.9); opacity: 0.55; }
  50% { transform: translate(-50%, -50%) translateY(6px) scale(1.25); opacity: 1; }
}
@keyframes ambientRedFloatAlt {
  0%, 100% { transform: translate(-50%, -50%) translateY(6px) scale(0.9); opacity: 0.55; }
  50% { transform: translate(-50%, -50%) translateY(-6px) scale(1.25); opacity: 1; }
}
@keyframes redArcOrbit {
  0% { transform: rotate(0deg) scale(0.92); }
  50% { transform: rotate(180deg) scale(1.12); }
  100% { transform: rotate(360deg) scale(0.92); }
}

@keyframes thunderBoltCrawl {
  0% { stroke-dashoffset: 0; opacity: 0.95; }
  50% { opacity: 0.65; }
  100% { stroke-dashoffset: -56; opacity: 1; }
}
@keyframes thunderBoltCrawlReverse {
  0% { stroke-dashoffset: 0; opacity: 0.85; }
  100% { stroke-dashoffset: 56; opacity: 1; }
}
@keyframes goldSparkPulse {
  0% { transform: translate(-50%, -50%) scale(0.75); opacity: 0.55; }
  100% { transform: translate(-50%, -50%) scale(1.3); opacity: 1; }
}

.boss-bar-label {
  font-size: 13px;
  font-weight: 700;
  min-width: 58px;
  text-align: right;
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.stage-tag {
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 4px;
  color: #fff;
  line-height: 1.2;
}

.label-stage-1 { color: var(--el-color-primary); }
.label-stage-2 { color: #a855f7; }
.label-stage-2 .stage-tag { background: #9333ea; }
.label-stage-3 { color: #ef4444; }
.label-stage-3 .stage-tag { background: #dc2626; }
.label-stage-4 { color: #f59e0b; text-shadow: 0 0 6px rgba(245, 158, 11, 0.4); }
.label-stage-4 .stage-tag { background: linear-gradient(90deg, #d97706, #f59e0b); }

.plan-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 5px;
}

.empty-placeholder-card {
  border-style: dashed !important;
}

.empty-card-inner {
  padding: 40px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  text-align: center;
}

.empty-card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.empty-card-sub {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

</style>
