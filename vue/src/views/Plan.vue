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
                <!-- 支持超过100%的双层 Boss 血条式进度条：第一层主题色，超出100%的部分以流光金条覆盖在第一层之上 -->
                <div class="boss-progress-row">
                  <div class="boss-bar-track">
                    <div
                        class="boss-bar-base"
                        :style="{ width: getBaseWidth(item) + '%' }"
                    ></div>
                    <div
                        v-if="getRealPercent(item) > 100"
                        class="boss-bar-overfill"
                        :style="{ width: getOverfillWidth(item) + '%' }"
                    ></div>
                  </div>
                  <span class="boss-bar-label num-font" :class="{ 'is-overfill': getRealPercent(item) > 100 }">
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

// 实时根据存钱明细累计金额与目标金额计算真实百分比（支持超过 100% 的超额数值，无需等待后端重启）
const getRealPercent = (item) => {
  if (!item) return 0
  const target = Number(item.money) || 0
  if (target > 0 && item._realSum !== undefined) {
    return Math.max(0, Math.round((Number(item._realSum) * 100) / target))
  }
  return Math.max(0, Number(item.percent) || 0)
}

const getBaseWidth = (item) => {
  return Math.min(getRealPercent(item), 100)
}

const getOverfillWidth = (item) => {
  const pct = getRealPercent(item)
  if (pct <= 100) return 0
  return Math.min(pct - 100, 100)
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

/* Boss 血条式双层超额进度条 */
.boss-progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0;
}

.boss-bar-track {
  position: relative;
  flex: 1;
  height: 8px;
  border-radius: 999px;
  background-color: rgba(148, 163, 184, 0.22);
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
  font-size: 13px;
  font-weight: 600;
  min-width: 44px;
  text-align: right;
  color: var(--el-text-color-regular);
}

.boss-bar-label.is-overfill {
  color: #f59e0b;
  font-weight: 700;
}

html.dark .boss-bar-label.is-overfill {
  color: #fbbf24;
}

.plan-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 5px;
}

</style>
