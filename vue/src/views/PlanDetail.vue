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

      <!-- Boss 血条式双层超额进度条：第一层主题色，超出100%的部分以流光金色进度条叠盖在第一层之上 -->
      <div class="boss-progress-row">
        <div class="boss-bar-track">
          <div
              class="boss-bar-base"
              :style="{ width: baseBarWidth + '%' }"
          ></div>
          <div
              v-if="realPercent > 100"
              class="boss-bar-overfill"
              :style="{ width: overfillBarWidth + '%' }"
          ></div>
        </div>
        <span class="boss-bar-label num-font" :class="{ 'is-overfill': realPercent > 100 }">
          {{ realPercent }}%
        </span>
      </div>
    </div>

    <div class="record-list">
      <el-row :gutter="16">
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

// 实时根据当前明细列表汇总累计存入金额，计算超过 100% 的真实百分比与 Boss 血条第二层金色进度宽度
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

const baseBarWidth = computed(() => Math.min(realPercent.value, 100))
const overfillBarWidth = computed(() => {
  if (realPercent.value <= 100) return 0
  return Math.min(realPercent.value - 100, 100)
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

/* Boss 血条式双层超额进度条 */
.boss-progress-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.boss-bar-track {
  position: relative;
  flex: 1;
  height: 10px;
  border-radius: 999px;
  background-color: var(--table-head-bg, #e4eefb);
  overflow: hidden;
}

html.dark .boss-bar-track {
  background-color: rgba(255, 255, 255, 0.1);
}

.boss-bar-base {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 999px;
  background-color: var(--el-color-primary);
  transition: width 0.35s cubic-bezier(0.22, 1, 0.36, 1);
  z-index: 1;
}

.boss-bar-overfill {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 999px;
  background: linear-gradient(90deg, #f59e0b 0%, #fbbf24 55%, #fde047 100%);
  box-shadow: 0 0 10px rgba(245, 158, 11, 0.55);
  transition: width 0.4s cubic-bezier(0.22, 1, 0.36, 1);
  z-index: 2;
}

.boss-bar-label {
  font-size: 14px;
  font-weight: 700;
  min-width: 48px;
  text-align: right;
  color: var(--el-text-color-primary);
}

.boss-bar-label.is-overfill {
  color: #f59e0b;
}

html.dark .boss-bar-label.is-overfill {
  color: #fbbf24;
}

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
</style>
