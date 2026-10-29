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

      <!-- 四层 Boss 充能血条：主题色(0-100%) -> 紫色(100-200%) -> 红色(200-300%) -> 金色(300%+) -->
      <div class="boss-progress-row">
        <div class="boss-bar-track" :class="'stage-' + activeStage">
          <!-- 第一层：主题色 -->
          <div
              class="boss-bar-layer layer-theme"
              :style="{ width: stage1Width + '%' }"
          ></div>
          <!-- 第二层：紫色（第一层满100%后起步） -->
          <div
              v-if="realPercent > 100"
              class="boss-bar-layer layer-purple"
              :style="{ width: stage2Width + '%' }"
          ></div>
          <!-- 第三层：红色（第二层满100%后起步） -->
          <div
              v-if="realPercent > 200"
              class="boss-bar-layer layer-red"
              :style="{ width: stage3Width + '%' }"
          ></div>
          <!-- 第四层：金色（第三层满100%后起步） -->
          <div
              v-if="realPercent > 300"
              class="boss-bar-layer layer-gold"
              :style="{ width: stage4Width + '%' }"
          ></div>

          <!-- 充能头（移动端点）处跟随移动的电火花粒子特效 -->
          <div
              class="spark-emitter"
              :class="'spark-stage-' + activeStage"
              :style="{ left: currentTipPosition + '%' }"
          >
            <span class="spark-core"></span>
            <span class="spark-arc arc-1"></span>
            <span class="spark-arc arc-2"></span>
            <span class="spark-arc arc-3" v-if="activeStage >= 3"></span>
            <span class="spark-lightning" v-if="activeStage === 4"></span>
          </div>
        </div>
        <span class="boss-bar-label num-font" :class="'label-stage-' + activeStage">
          <span v-if="activeStage > 1" class="stage-tag">x{{ activeStage }}</span>
          {{ realPercent }}%
        </span>
      </div>
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
      <el-form :model="data.detailForm" label-width="100px" style="padding-right: 50px" :rules="data.rules" ref="formRef">
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

// 当前处于第几阶段 (1:主题色, 2:紫色, 3:红色, 4:金色)
const activeStage = computed(() => {
  const pct = realPercent.value
  if (pct > 300) return 4
  if (pct > 200) return 3
  if (pct > 100) return 2
  return 1
})

// 严格顺序充能：前一层必须满 100% 后，下一层才开始充能
const stage1Width = computed(() => Math.min(realPercent.value, 100))
const stage2Width = computed(() => {
  if (realPercent.value <= 100) return 0
  return Math.min(realPercent.value - 100, 100)
})
const stage3Width = computed(() => {
  if (realPercent.value <= 200) return 0
  return Math.min(realPercent.value - 200, 100)
})
const stage4Width = computed(() => {
  if (realPercent.value <= 300) return 0
  return Math.min(realPercent.value - 300, 100)
})

const currentTipPosition = computed(() => {
  const s = activeStage.value
  if (s === 4) return stage4Width.value
  if (s === 3) return stage3Width.value
  if (s === 2) return stage2Width.value
  return stage1Width.value
})

const load = () => {
  request.get('/plan/selectById/' + data.planId).then(res => {
    data.plan = res.data || {};
  })
  request.get('/planDetail/selectAll', {
    params: {
      planId: data.planId
    }
  }).then(res => {
    const list = res.data || [];
    // 按时间从早到晚重算每笔明细的动态累计金额，确保删除某笔明细后累计金额也完全准确
    let running = 0;
    for (let i = list.length - 1; i >= 0; i--) {
      running += Number(list[i].money) || 0;
      list[i].sum = running.toFixed(2).replace(/\.00$/, '');
    }
    data.tableData = list;
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
  request.post('/planDetail/add', data.detailForm).then(res => {
    if (res.code === 200) {
      ElMessage.success('操作成功')
      load()
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

/* 四层 Boss 充能血条及流光粒子特效系统 */
.boss-progress-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.boss-bar-track {
  position: relative;
  flex: 1;
  height: 12px;
  border-radius: 999px;
  background-color: var(--table-head-bg, #e4eefb);
  overflow: visible; /* 允许粒子与电弧微微外溢营造强烈光效 */
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
  /* 慢速放缓的平滑充能缓动动画 */
  transition: width 1.3s cubic-bezier(0.16, 1, 0.3, 1);
}

/* 第 1 层：系统主题色 */
.layer-theme {
  background-color: var(--el-color-primary);
  box-shadow: 0 0 10px rgba(var(--theme-rgb, 64, 158, 255), 0.45);
  z-index: 1;
}

/* 第 2 层：星云紫 */
.layer-purple {
  background: linear-gradient(90deg, #9333ea 0%, #a855f7 60%, #c084fc 100%);
  box-shadow: 0 0 14px rgba(168, 85, 247, 0.65);
  z-index: 2;
}

/* 第 3 层：烈焰红 */
.layer-red {
  background: linear-gradient(90deg, #dc2626 0%, #ef4444 60%, #f87171 100%);
  box-shadow: 0 0 16px rgba(239, 68, 68, 0.75);
  z-index: 3;
}

/* 第 4 层：流光金（高能神圣） */
.layer-gold {
  background: linear-gradient(90deg, #d97706 0%, #f59e0b 50%, #fde047 100%);
  box-shadow: 0 0 20px rgba(245, 158, 11, 0.9);
  z-index: 4;
}

/* 从第 2 层开始：在已充能轨道上叠加循环渲染环境微光与电弧围绕动画 */
.boss-bar-track.stage-2::before {
  content: '';
  position: absolute;
  left: 0;
  top: -2px;
  bottom: -2px;
  right: 0;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(192, 132, 252, 0.35) 10%, transparent 60%);
  background-size: 26px 100%;
  animation: purpleParticlePulse 1.8s linear infinite;
  pointer-events: none;
  z-index: 5;
}

.boss-bar-track.stage-3::before {
  content: '';
  position: absolute;
  left: 0;
  top: -3px;
  bottom: -3px;
  right: 0;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(248, 113, 113, 0.45) 15%, transparent 65%);
  background-size: 22px 100%;
  animation: redArcSurround 1.2s ease-in-out infinite alternate;
  pointer-events: none;
  z-index: 5;
}

.boss-bar-track.stage-4::before {
  content: '';
  position: absolute;
  left: -2px;
  top: -4px;
  bottom: -4px;
  right: -2px;
  border-radius: 999px;
  background: repeating-linear-gradient(
      90deg,
      rgba(253, 224, 71, 0.6) 0px,
      rgba(251, 191, 36, 0.2) 8px,
      rgba(253, 224, 71, 0.7) 16px
  );
  animation: goldThunderRush 0.75s linear infinite;
  filter: drop-shadow(0 0 7px rgba(251, 191, 36, 0.95));
  pointer-events: none;
  z-index: 5;
}

/* 跟随充能端点移动的电火花粒子发生器 */
.spark-emitter {
  position: absolute;
  top: 50%;
  width: 16px;
  height: 16px;
  transform: translate(-50%, -50%);
  pointer-events: none;
  z-index: 10;
  transition: left 1.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.spark-core {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 6px;
  height: 6px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background-color: #fff;
}

/* 第 1 阶段：主题色点状火花 */
.spark-stage-1 .spark-core {
  box-shadow: 0 0 8px var(--el-color-primary), 0 0 16px var(--el-color-primary);
}
.spark-stage-1 .spark-arc {
  position: absolute;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background-color: var(--el-color-primary);
  animation: sparkDots 0.6s ease-out infinite;
}

/* 第 2 阶段：紫色细小电弧火花 */
.spark-stage-2 .spark-core {
  box-shadow: 0 0 10px #c084fc, 0 0 20px #a855f7;
}
.spark-stage-2 .spark-arc {
  position: absolute;
  width: 9px;
  height: 2px;
  background: #c084fc;
  box-shadow: 0 0 5px #a855f7;
  border-radius: 2px;
  animation: purpleSmallArcs 0.4s ease-in-out infinite alternate;
}

/* 第 3 阶段：红色中等跃动电弧 */
.spark-stage-3 .spark-core {
  box-shadow: 0 0 12px #f87171, 0 0 24px #ef4444;
}
.spark-stage-3 .spark-arc {
  position: absolute;
  width: 13px;
  height: 3px;
  background: #fecaca;
  box-shadow: 0 0 7px #ef4444;
  border-radius: 2px;
  animation: redMediumArcs 0.3s ease-in-out infinite alternate;
}

/* 第 4 阶段：金色强电流大电弧与雷电粒子 */
.spark-stage-4 .spark-core {
  width: 8px;
  height: 8px;
  background-color: #fff;
  box-shadow: 0 0 16px #fde047, 0 0 30px #f59e0b;
}
.spark-stage-4 .spark-arc {
  position: absolute;
  width: 18px;
  height: 3px;
  background: #fff;
  box-shadow: 0 0 8px #f59e0b, 0 0 16px #fde047;
  border-radius: 2px;
  animation: goldLargeArcs 0.22s steps(2, start) infinite;
}

.spark-stage-4 .spark-lightning {
  position: absolute;
  top: -9px;
  left: -9px;
  right: -9px;
  bottom: -9px;
  border: 1.5px dashed rgba(254, 240, 138, 0.95);
  border-radius: 50%;
  animation: lightningRotate 0.4s linear infinite;
}

.arc-1 { top: 0; left: 0; transform: rotate(25deg); }
.arc-2 { bottom: 0; right: 0; transform: rotate(-35deg); }
.arc-3 { top: -2px; right: -2px; transform: rotate(70deg); }

@keyframes sparkDots {
  0% { transform: translate(0, 0) scale(1); opacity: 1; }
  100% { transform: translate(6px, -6px) scale(0); opacity: 0; }
}

@keyframes purpleSmallArcs {
  0% { transform: translate(-2px, -2px) rotate(15deg); }
  100% { transform: translate(4px, 3px) rotate(60deg); }
}

@keyframes redMediumArcs {
  0% { transform: translate(-3px, -3px) rotate(-30deg) scale(1.1); }
  100% { transform: translate(5px, 4px) rotate(45deg) scale(0.9); }
}

@keyframes goldLargeArcs {
  0% { transform: translate(-5px, -3px) rotate(10deg) scaleX(1.4); }
  50% { transform: translate(4px, -4px) rotate(-45deg) scaleX(1.8); }
  100% { transform: translate(6px, 5px) rotate(80deg) scaleX(1.2); }
}

@keyframes lightningRotate {
  0% { transform: rotate(0deg) scale(0.9); }
  50% { transform: rotate(180deg) scale(1.2); }
  100% { transform: rotate(360deg) scale(0.9); }
}

@keyframes purpleParticlePulse {
  0% { background-position: 0 0; opacity: 0.4; }
  50% { opacity: 0.85; }
  100% { background-position: 26px 0; opacity: 0.4; }
}

@keyframes redArcSurround {
  0% { transform: scaleY(0.9); opacity: 0.5; }
  100% { transform: scaleY(1.3); opacity: 0.95; }
}

@keyframes goldThunderRush {
  0% { background-position: 0 0; }
  100% { background-position: 32px 0; }
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
