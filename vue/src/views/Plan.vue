<template>
  <div>
    <el-card>
      <el-input
          v-model="data.name"
          style="width: 240px; margin-right: 15px"
          placeholder="输入计划名称以查询"
          clearable
          :prefix-icon="Search"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="warning" @click="reset">清空</el-button>
      <el-button type="info" @click="handleAdd" v-if="data.user.role === 'USER'">新建计划</el-button>
      <el-button type="danger" @click="delBatch" v-if="data.user.perm >= 10">批量删除</el-button>
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
                <!-- 四层 Boss 充能血条：主题色(0-100%) -> 紫色(100-200%) -> 红色(200-300%) -> 金色(300%+) -->
                <div class="boss-progress-row">
                  <div class="boss-bar-track" :class="'stage-' + getActiveStage(item)">
                    <!-- 第一层：主题色 -->
                    <div
                        class="boss-bar-layer layer-theme"
                        :style="{ width: getStageWidth(item, 1) + '%' }"
                    ></div>
                    <!-- 第二层：紫色（第一层满100%后起步） -->
                    <div
                        v-if="getRealPercent(item) > 100"
                        class="boss-bar-layer layer-purple"
                        :style="{ width: getStageWidth(item, 2) + '%' }"
                    ></div>
                    <!-- 第三层：红色（第二层满100%后起步） -->
                    <div
                        v-if="getRealPercent(item) > 200"
                        class="boss-bar-layer layer-red"
                        :style="{ width: getStageWidth(item, 3) + '%' }"
                    ></div>
                    <!-- 第四层：金色（第三层满100%后起步） -->
                    <div
                        v-if="getRealPercent(item) > 300"
                        class="boss-bar-layer layer-gold"
                        :style="{ width: getStageWidth(item, 4) + '%' }"
                    ></div>

                    <!-- 充能头（移动端点）处跟随移动的电火花粒子特效 -->
                    <div
                        class="spark-emitter"
                        :class="'spark-stage-' + getActiveStage(item)"
                        :style="{ left: getCurrentTipPosition(item) + '%' }"
                    >
                      <span class="spark-core"></span>
                      <span class="spark-arc arc-1"></span>
                      <span class="spark-arc arc-2"></span>
                      <span class="spark-arc arc-3" v-if="getActiveStage(item) >= 3"></span>
                      <span class="spark-lightning" v-if="getActiveStage(item) === 4"></span>
                    </div>
                  </div>
                  <span class="boss-bar-label num-font" :class="'label-stage-' + getActiveStage(item)">
                    <span v-if="getActiveStage(item) > 1" class="stage-tag">x{{ getActiveStage(item) }}</span>
                    {{ getRealPercent(item) }}%
                  </span>
                </div>
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
      <el-form :model="data.form" label-width="100px" style="padding-right: 50px" :rules="data.rules" ref="formRef">
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
import {reactive, ref} from "vue";
import request from "@/utils/request.js";
import {ElMessage, ElMessageBox} from "element-plus";
import { useRouter } from 'vue-router'
import {Delete, Edit, Search} from "@element-plus/icons-vue";

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

// 获取当前处于第几层 Boss 充能阶段 (1:主题色, 2:紫色, 3:红色, 4:金色)
const getActiveStage = (item) => {
  const pct = getRealPercent(item)
  if (pct > 300) return 4
  if (pct > 200) return 3
  if (pct > 100) return 2
  return 1
}

// 严格顺序充能：前一层必须达到 100% 后，下一层才开始充能（每层各代表 100% 阈值）
const getStageWidth = (item, stage) => {
  const pct = getRealPercent(item)
  if (stage === 1) {
    return Math.min(pct, 100)
  }
  const minRequired = (stage - 1) * 100
  if (pct <= minRequired) return 0
  return Math.min(pct - minRequired, 100)
}

// 当前处于充能状态的最前端位置（百分比），用于粒子电火花精确定位跟随
const getCurrentTipPosition = (item) => {
  const stage = getActiveStage(item)
  return getStageWidth(item, stage)
}

const load = () => {
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

/* 四层 Boss 充能血条及流光粒子特效系统 */
.boss-progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0;
}

.boss-bar-track {
  position: relative;
  flex: 1;
  height: 10px;
  border-radius: 999px;
  background-color: rgba(148, 163, 184, 0.22);
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
  transition: width 1.2s cubic-bezier(0.16, 1, 0.3, 1);
}

/* 第 1 层：系统主题色 */
.layer-theme {
  background-color: var(--el-color-primary);
  box-shadow: 0 0 8px rgba(var(--theme-rgb, 64, 158, 255), 0.4);
  z-index: 1;
}

/* 第 2 层：星云紫 */
.layer-purple {
  background: linear-gradient(90deg, #9333ea 0%, #a855f7 60%, #c084fc 100%);
  box-shadow: 0 0 12px rgba(168, 85, 247, 0.6);
  z-index: 2;
}

/* 第 3 层：烈焰红 */
.layer-red {
  background: linear-gradient(90deg, #dc2626 0%, #ef4444 60%, #f87171 100%);
  box-shadow: 0 0 14px rgba(239, 68, 68, 0.7);
  z-index: 3;
}

/* 第 4 层：流光金（高能神圣） */
.layer-gold {
  background: linear-gradient(90deg, #d97706 0%, #f59e0b 50%, #fde047 100%);
  box-shadow: 0 0 18px rgba(245, 158, 11, 0.85);
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
  background-size: 24px 100%;
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
  background-size: 20px 100%;
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
  filter: drop-shadow(0 0 6px rgba(251, 191, 36, 0.9));
  pointer-events: none;
  z-index: 5;
}

/* 跟随充能端点移动的电火花粒子发生器 */
.spark-emitter {
  position: absolute;
  top: 50%;
  width: 14px;
  height: 14px;
  transform: translate(-50%, -50%);
  pointer-events: none;
  z-index: 10;
  transition: left 1.2s cubic-bezier(0.16, 1, 0.3, 1);
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
  width: 8px;
  height: 2px;
  background: #c084fc;
  box-shadow: 0 0 4px #a855f7;
  border-radius: 2px;
  animation: purpleSmallArcs 0.4s ease-in-out infinite alternate;
}

/* 第 3 阶段：红色中等跃动电弧 */
.spark-stage-3 .spark-core {
  box-shadow: 0 0 12px #f87171, 0 0 24px #ef4444;
}
.spark-stage-3 .spark-arc {
  position: absolute;
  width: 12px;
  height: 3px;
  background: #fecaca;
  box-shadow: 0 0 6px #ef4444;
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
  width: 16px;
  height: 3px;
  background: #fff;
  box-shadow: 0 0 8px #f59e0b, 0 0 16px #fde047;
  border-radius: 2px;
  animation: goldLargeArcs 0.22s steps(2, start) infinite;
}

.spark-stage-4 .spark-lightning {
  position: absolute;
  top: -8px;
  left: -8px;
  right: -8px;
  bottom: -8px;
  border: 1px dashed rgba(254, 240, 138, 0.9);
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
  100% { background-position: 24px 0; opacity: 0.4; }
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
