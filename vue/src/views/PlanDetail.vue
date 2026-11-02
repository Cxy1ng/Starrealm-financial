<template>
  <div>
    <div class="plan-card">
      <div class="plan-header">
        <el-tag
            v-if="data.plan.status"
            :type="statusTagType[data.plan.status]"
            class="status-tag"
        >
          {{ data.plan.status }}
        </el-tag>
        <div class="plan-title">
          {{ data.plan.name }}
          <span class="plan-money num-font">目标 ￥{{ data.plan.money }}</span>
          <el-button
              type="primary"
              size="small"
              class="plan-button"
              :disabled="isExpired"
              :title="isExpired ? '该存钱计划已结束过期，已停止存钱' : '存入一笔金额'"
              @click="addPlanDetail(data.planId)"
          >
            {{ isExpired ? '计划已结束' : '存一笔' }}
          </el-button>
        </div>
        <div class="plan-end num-font">
          截止日期：{{ data.plan.end }}
        </div>
      </div>

      <BossProgressBar
          :percent="realPercent"
          :from-percent="animFromPct"
          :animate-trigger="animTrigger"
          large
      />
    </div>

    <div class="record-list">
      <el-row :gutter="16" v-if="data.tableData.length">
        <el-col
            v-for="item in data.tableData"
            :key="item.id"
            :xs="24"
            :sm="12"
            :md="6"
        >
          <div class="record-card">
            <div class="record-header">
              <div class="record-money num-font">存入 ￥{{ item.money }}</div>
              <div class="record-sum num-font">累计 <span class="record-sum-amount">￥{{ item.sum }}</span></div>
            </div>
            <div class="record-footer">
              <div class="record-date num-font">{{ item.date }}</div>
              <el-button
                  @click="del(item.id)"
                  :icon="Delete"
                  size="small"
                  type="danger"
                  :disabled="isExpired"
                  :title="isExpired ? '该存钱计划已结束过期，不可删除明细记录' : '删除此笔存钱记录'"
              >
                删除
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>

      <div v-else class="record-empty-card">
        <div class="record-empty-title">暂无存钱打卡记录</div>
        <div class="record-empty-sub">
          {{ isExpired ? '该存钱计划已结束，期间未产生存钱记录' : '当前还没有任何存入明细，点击右上方「存一笔」记录您的第一笔储蓄吧' }}
        </div>
        <el-button
            v-if="!isExpired"
            type="primary"
            size="small"
            style="margin-top: 6px"
            @click="addPlanDetail(data.planId)"
        >
          立即存一笔
        </el-button>
      </div>
    </div>

    <el-dialog title="存钱" v-model="data.detailFromVisible" width="40vw" :close-on-click-modal="false" destroy-on-close>
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
import {reactive, ref, computed} from "vue";
const formRef = ref()
import request from "@/utils/request.js";
import {ElMessage, ElMessageBox} from "element-plus";
import { useRoute } from 'vue-router'
import {Delete} from "@element-plus/icons-vue";
import BossProgressBar from "@/components/BossProgressBar.vue";

const route = useRoute()
const data = reactive({
  user: JSON.parse(localStorage.getItem('loginToken') || '{}'),
  plan: {},
  planId: route.query.planId,
  detailForm: {},
  tableData: [],
  detailFromVisible: false,
  rules: {
    money: [
      { required: true, message: '请输入金额', trigger: 'blur'},
      { pattern: /^\d+$/, message: '请输入正整数', trigger: 'blur'},
    ],
  }
})

// 实时根据当前明细列表汇总累计存入金额，计算超过 100% 的真实百分比
const totalSaved = computed(() => {
  if (!Array.isArray(data.tableData) || data.tableData.length === 0) return 0
  return data.tableData.reduce((acc, item) => acc + (Number(item.money) || 0), 0)
})

const realPercent = computed(() => {
  const target = Number(data.plan?.money) || 0
  if (target > 0) {
    return Math.max(0, Math.round((totalSaved.value * 100) / target))
  }
  return Math.max(0, Number(data.plan?.percent) || 0)
})

const animFromPct = ref(0)
const animTrigger = ref(0)

const load = (shouldAnimate = false, prevPct = 0) => {
  Promise.all([
    request.get('/plan/selectById/' + data.planId),
    request.get('/planDetail/selectAll', { params: { planId: data.planId } })
  ]).then(([planRes, detailRes]) => {
    data.plan = planRes.data || {}
    const list = detailRes.data || []
    let running = 0
    for (let i = list.length - 1; i >= 0; i--) {
      running += Number(list[i].money) || 0
      list[i].sum = running.toFixed(2).replace(/\.00$/, '')
    }
    data.tableData = list
    if (shouldAnimate) {
      animFromPct.value = prevPct
      animTrigger.value++
    } else {
      animFromPct.value = realPercent.value
    }
  })
}
load()

const isExpired = computed(() => {
  const p = data.plan
  if (!p) return false
  if (p.status === '已结束') return true
  if (!p.end) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const endDate = new Date(String(p.end).replace(/-/g, '/'))
  endDate.setHours(0, 0, 0, 0)
  return endDate.getTime() < today.getTime()
})

const addPlanDetail = (planId) => {
  if (isExpired.value) {
    ElMessage.warning('该存钱计划已结束过期，已停止存钱功能')
    return
  }
  data.detailFromVisible = true;
  data.detailForm = { planId }
}

const savePlanDetail = () => {
  if (isExpired.value) {
    ElMessage.warning('该存钱计划已结束过期，已停止存钱功能')
    return
  }
  const prevPct = realPercent.value
  request.post('/planDetail/add', data.detailForm).then(res => {
    if (res.code === 200) {
      ElMessage.success('存入成功，正在充能进度条...')
      load(true, prevPct)
      data.detailFromVisible = false
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const del = (id) => {
  if (isExpired.value) {
    ElMessage.warning('该存钱计划已结束过期，不可删除历史明细')
    return
  }
  ElMessageBox.confirm('该操作是永久且不可逆的。您确定要删除吗？','删除确认',{
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    request.delete('/planDetail/delete/' + id).then(res=>{
      if (res.code === 200) {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch()
}

const statusTagType = {
  '未开始': 'warning',
  '进行中': 'primary',
  '已结束': 'danger',
  '已完成': 'success',
}
</script>

<style scoped>
.plan-card {
  background-color: var(--table-row-bg, #f6f9fe);
  border: 1px solid var(--table-border-color, #d3e1f4);
  padding: 20px 22px;
  border-radius: 12px;
  margin-bottom: 20px;
  transition: background-color 0.25s ease, border-color 0.25s ease;
}

html.dark .plan-card {
  background-color: var(--table-row-bg, #0c1322);
  border-color: var(--table-border-color, #1e2c46);
}

.plan-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 14px;
}

.status-tag {
  min-width: 60px;
  text-align: center;
}

.plan-title {
  font-size: 19px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  display: flex;
  align-items: center;
  flex: 1;
  gap: 12px;
}

.plan-money {
  color: var(--el-color-primary);
  font-size: 15px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 6px;
  background-color: var(--table-head-bg, #e4eefb);
}

.plan-button {
  margin-left: auto;
}

.plan-end {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  white-space: nowrap;
}

/* 四层 Boss 充能血条及双模态（即时推进电弧 + 持续均匀环绕粒子）特效系统 */
.boss-progress-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
}

.boss-bar-track {
  position: relative;
  flex: 1;
  height: 12px;
  border-radius: 999px;
  background-color: var(--table-head-bg, #e4eefb);
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
  box-shadow: 0 0 10px rgba(var(--theme-rgb, 22, 119, 255), 0.45);
  z-index: 1;
}

/* 第 2 层：星云紫 */
.layer-purple {
  background: linear-gradient(90deg, #7e22ce 0%, #a855f7 65%, #d8b4fe 100%);
  box-shadow: 0 0 14px rgba(168, 85, 247, 0.65);
  z-index: 2;
}

/* 第 3 层：烈焰红 */
.layer-red {
  background: linear-gradient(90deg, #b91c1c 0%, #ef4444 65%, #fca5a5 100%);
  box-shadow: 0 0 16px rgba(239, 68, 68, 0.78);
  z-index: 3;
}

/* 第 4 层：流光金（强电雷霆） */
.layer-gold {
  background: linear-gradient(90deg, #d97706 0%, #f59e0b 45%, #fde047 85%, #ffffff 100%);
  box-shadow: 0 0 20px rgba(250, 204, 21, 0.9), 0 0 8px rgba(255, 255, 255, 0.8);
  z-index: 4;
}

/* ==================== 模态 A：已充能区域持续均匀分布的环绕粒子与雷电动画 ==================== */
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

/* 紫色层已充能区：均匀分布的紫色点状微粒向上下发散 + 细微电弧环绕 */
.purple-field .ambient-node {
  width: 4.5px;
  height: 4.5px;
  border-radius: 50%;
  background: #e9d5ff;
  box-shadow: 0 0 6px #a855f7, 0 0 10px #c084fc;
  animation: ambientPurpleFloat 2.1s ease-in-out infinite;
}
.purple-field .ambient-node:nth-child(even) {
  animation-name: ambientPurpleFloatAlt;
}
.purple-field .ambient-node::after {
  content: '';
  position: absolute;
  left: -3px;
  top: -2px;
  width: 10px;
  height: 8px;
  border-top: 1.3px solid rgba(216, 180, 254, 0.85);
  border-radius: 50%;
  animation: miniArcFlicker 0.9s steps(2, start) infinite;
}

/* 红色层已充能区：均匀分布的赤红能量粒子发散 + 跃动小电弧围绕 */
.red-field .ambient-node {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #fecaca;
  box-shadow: 0 0 8px #ef4444, 0 0 14px #f87171;
  animation: ambientRedFloat 1.6s ease-in-out infinite;
}
.red-field .ambient-node:nth-child(even) {
  animation-name: ambientRedFloatAlt;
}
.red-field .ambient-node::before,
.red-field .ambient-node::after {
  content: '';
  position: absolute;
  left: -5px;
  top: -4px;
  width: 14px;
  height: 12px;
  border-top: 1.5px solid #fca5a5;
  border-bottom: 1.5px solid #ef4444;
  border-radius: 45%;
  filter: drop-shadow(0 0 4px #ef4444);
  animation: redArcOrbit 0.65s linear infinite;
}

/* 金色层已充能区：模拟强电雷电色电弧沿着已充能进度条全身环绕游走 */
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
  filter: drop-shadow(0 0 5px #f59e0b) drop-shadow(0 0 10px #fde047);
  stroke-dasharray: 20 10;
  animation: thunderBoltCrawl 0.55s linear infinite;
}
.bolt-2 {
  stroke: #ffffff;
  stroke-width: 1.3;
  filter: drop-shadow(0 0 6px #fbbf24);
  stroke-dasharray: 14 16;
  animation: thunderBoltCrawlReverse 0.42s linear infinite;
}
.gold-field .ambient-node {
  width: 5.5px;
  height: 5.5px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 0 8px #fde047, 0 0 16px #f59e0b;
  animation: goldSparkPulse 0.8s ease-in-out infinite alternate;
}

/* ==================== 模态 B：存一笔后的即时推进电火花（跟随充能前端移动） ==================== */
.spark-emitter {
  position: absolute;
  top: 50%;
  width: 26px;
  height: 26px;
  transform: translate(-50%, -50%);
  pointer-events: none;
  z-index: 12;
}

.spark-core {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 8px;
  height: 8px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: #ffffff;
}

.spark-burst {
  position: absolute;
  left: 50%;
  top: 50%;
  border-radius: 50%;
}

.tip-arc-svg {
  position: absolute;
  left: -11px;
  top: -11px;
  width: 48px;
  height: 48px;
  overflow: visible;
}

.tip-arc-path {
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
  animation: tipArcJitter 0.14s steps(2, start) infinite;
}
.tip-arc-path.a-2 { animation-delay: 0.04s; }
.tip-arc-path.a-3 { animation-delay: 0.08s; }
.tip-arc-path.a-4 { animation-delay: 0.11s; }

/* 第 1 层推进特效：主题色点状粒子喷射 */
.spark-stage-1 .spark-core {
  box-shadow: 0 0 10px var(--el-color-primary), 0 0 18px var(--el-color-primary);
}
.spark-stage-1 .spark-burst {
  width: 4px;
  height: 4px;
  background: var(--el-color-primary);
  box-shadow: 0 0 6px var(--el-color-primary);
}

/* 第 2 层推进特效：紫色小电弧 + 紫光粒子 */
.spark-stage-2 .spark-core {
  box-shadow: 0 0 12px #d8b4fe, 0 0 22px #a855f7;
}
.spark-stage-2 .spark-burst {
  width: 4.5px;
  height: 4.5px;
  background: #e9d5ff;
  box-shadow: 0 0 8px #a855f7;
}
.spark-stage-2 .tip-arc-path {
  stroke: #e9d5ff;
  stroke-width: 1.6;
  filter: drop-shadow(0 0 4px #a855f7);
  transform: scale(0.7);
}

/* 第 3 层推进特效：红色中电弧 + 烈焰火花 */
.spark-stage-3 .spark-core {
  box-shadow: 0 0 14px #fca5a5, 0 0 26px #ef4444;
}
.spark-stage-3 .spark-burst {
  width: 5px;
  height: 5px;
  background: #fecaca;
  box-shadow: 0 0 10px #ef4444;
}
.spark-stage-3 .tip-arc-path {
  stroke: #fecaca;
  stroke-width: 2.1;
  filter: drop-shadow(0 0 6px #ef4444);
  transform: scale(0.92);
}

/* 第 4 层推进特效：金色强雷电大电弧 */
.spark-stage-4 .spark-core {
  width: 10px;
  height: 10px;
  box-shadow: 0 0 16px #ffffff, 0 0 28px #fde047, 0 0 40px #f59e0b;
}
.spark-stage-4 .spark-burst {
  width: 6px;
  height: 6px;
  background: #fef08a;
  box-shadow: 0 0 12px #f59e0b;
}
.spark-stage-4 .tip-arc-path {
  stroke: #fef08a;
  stroke-width: 2.6;
  filter: drop-shadow(0 0 8px #f59e0b) drop-shadow(0 0 14px #fde047);
  transform: scale(1.18);
}

.b-1 { animation: burstFly1 0.42s ease-out infinite; }
.b-2 { animation: burstFly2 0.46s ease-out infinite 0.08s; }
.b-3 { animation: burstFly3 0.38s ease-out infinite 0.15s; }
.b-4 { animation: burstFly4 0.44s ease-out infinite 0.22s; }

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
  font-size: 14px;
  font-weight: 700;
  min-width: 62px;
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

.record-list {
  margin-top: 10px;
}

.record-card {
  background-color: var(--table-row-alt-bg, #eef4fc);
  border: 1px solid var(--table-border-color, #d3e1f4);
  padding: 14px 16px;
  border-radius: 10px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.record-card:hover {
  background-color: var(--table-hover-bg, #dbe8fa);
}

html.dark .record-card {
  background-color: var(--table-row-alt-bg, #101829);
  border-color: var(--table-border-color, #1e2c46);
}

html.dark .record-card:hover {
  background-color: var(--table-hover-bg, #18243c);
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.record-money {
  flex: 1;
  font-size: 14.5px;
}

.record-sum {
  font-size: 12.5px;
  color: var(--el-text-color-secondary);
}

.record-sum-amount {
  color: var(--el-color-primary);
  font-size: 17px;
  font-weight: 700;
  margin-left: 4px;
}

.record-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  color: var(--el-text-color-secondary);
  align-items: center;
}

.record-empty-card {
  background-color: var(--table-row-alt-bg, #eef4fc);
  border: 1px dashed var(--table-border-color, #d3e1f4);
  border-radius: 12px;
  padding: 40px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  text-align: center;
}

html.dark .record-empty-card {
  background-color: var(--table-row-alt-bg, #101829);
  border-color: var(--table-border-color, #1e2c46);
}

.record-empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.record-empty-sub {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
