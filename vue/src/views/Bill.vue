<template>
  <div>
    <el-card>
      <div class="table-search-row">
        <el-input
            v-model="data.type"
            class="search-input"
            placeholder="输入账单类型以查询"
            clearable
            :prefix-icon="Search"
        />
        <el-input
            v-model="data.category"
            class="search-input"
            placeholder="输入账单分类以查询"
            clearable
            :prefix-icon="Search"
        />
        <el-date-picker
            class="search-date"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            v-model="data.start"
            placeholder="请选择查询开始日期"/>
        <el-date-picker
            class="search-date"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            v-model="data.end"
            placeholder="请选择查询结束日期"
            :disabled-date="disabledDate"
            :shortcuts="shortcuts"/>
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        <el-button class="btn-toolbar-reset" :icon="Refresh" @click="reset">重置</el-button>
      </div>
    </el-card>
    <el-card style="margin-top: 5px">
      <div class="bill-toolbar">
        <el-button type="success" :icon="Plus" @click="handleAdd" v-if="data.user.role === 'USER'">新建账单</el-button>
        <el-button type="danger" :icon="Delete" @click="delBatch">批量删除</el-button>
        <el-upload
            v-if="data.user.role === 'USER'"
            class="bill-upload-inline"
            action="http://localhost:8080/bill/import"
            :headers="{token: data.user.token}"
            :show-file-list="false"
            :on-success="handleImportSuccess"
        >
          <el-button class="btn-toolbar-import" :icon="Upload">导入</el-button>
        </el-upload>
        <el-button class="btn-toolbar-export" :icon="Download" @click="exportData" v-if="data.user.role === 'USER'">导出</el-button>
      </div>
    </el-card>
    <el-card style="margin-top: 5px">
      <el-table :data="data.tableData"
                :table-layout="'auto'"
                :default-sort="{ prop: 'id', order: 'descending'}"
                @selection-change="handleSelectionChange"
                >
        <template #empty>
          <div class="empty-table-placeholder">
            <div class="empty-table-title">暂无收支账单数据</div>
            <div class="empty-table-sub">当前没有任何账单记录，您可以点击上方「新建账单」或「导入」添加账单</div>
          </div>
        </template>
        <el-table-column type="selection" width="55" align="center"/>
        <el-table-column label="ID" prop="id" width="80" align="center" sortable />
        <el-table-column label="交易时间" prop="time" :formatter="formatTime"/>
        <el-table-column label="交易分类" prop="category"/>
        <el-table-column label="收款方" prop="payee" show-overflow-tooltip/>
        <el-table-column label="交易说明" prop="detail" show-overflow-tooltip/>
        <el-table-column label="账单类型" prop="type"/>
        <el-table-column label="金额（元）" prop="amount"/>
        <el-table-column label="收付款方式" prop="payment" show-overflow-tooltip/>
        <el-table-column label="交易状态" prop="status"/>
        <el-table-column label="备注" prop="comment" show-overflow-tooltip/>
        <el-table-column v-if="data.user.perm >= 10" label="用户ID" prop="userId" show-overflow-tooltip/>
        <el-table-column label="操作" align="center" width="160">
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
    <el-dialog v-model="data.formVisible" title="账单信息" width="40vw" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" style="padding: 10px 50px 0 20px;" label-position="right" label-width="auto">
        <el-form-item label="账单类型" prop="type">
          <el-select style="width: 100%;" v-model="data.form.type" @change="getCategoryList">
            <el-option value="收入" />
            <el-option value="支出" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易分类" prop="category">
          <el-select style="width: 100%;" v-model="data.form.category" placeholder="请选择账单分类">
            <el-option v-for="item in data.categoryList" :key="item.id" :value="item.name"/>
          </el-select>
        </el-form-item>
        <el-form-item label="收款方" prop="payee">
          <el-input v-model="data.form.payee" placeholder="请输入收款方"/>
        </el-form-item>
        <el-form-item label="交易说明" prop="detail">
          <el-input v-model="data.form.detail" placeholder="请输入交易说明"/>
        </el-form-item>
        <el-form-item label="收付款方式" prop="payment">
          <el-select style="width: 100%;" v-model="data.form.payment" placeholder="请选择交易方式">
            <el-option value="支付宝"/>
            <el-option value="微信支付"/>
            <el-option value="银行卡支付"/>
            <el-option value="现金支付"/>
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input v-model="data.form.amount" placeholder="请输入金额" :min="0.1"/>
        </el-form-item>
        <el-form-item label="交易状态" prop="status">
          <el-select style="width: 100%;" v-model="data.form.status" placeholder="请选择交易状态">
            <el-option value="交易成功"/>
            <el-option value="已全额退款"/>
            <el-option value="存入成功"/>
            <el-option value="已到账"/>
            <el-option value="等待确认收货"/>
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="comment">
          <el-input v-model="data.form.comment" placeholder="请输入备注" type="textarea"/>
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
  </div>
</template>

<script setup>
import {reactive, ref} from "vue";
import {Delete, Edit, Search, Refresh, Plus, Download, Upload} from "@element-plus/icons-vue";
import {ElMessage, ElMessageBox} from "element-plus";
import {request} from "@/utils/request.js";
import dayjs from "dayjs";

const data = reactive({
  user: JSON.parse(localStorage.getItem('loginToken') || '{}'),
  form: {},
  name: null,
  tableData: [],
  type: null,
  category: null,
  start: null,
  end: null,
  pageNum: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  ids:[],
  rows:[],
  rules: {
    category: [
      {required: true, message: '请选择账单分类', trigger: 'blur'},
    ],
    type: [
      {required: true, message: '请选择收支类型', trigger: 'blur'},
    ],
    payment: [
      {required: true, message: '请选择收付款方式', trigger: 'blur'},
    ],
    amount: [
      {required: true, message: '请输入有效的金额', trigger: 'blur'},
      {pattern: /^(([1-9]{1}\d*)|([0]{1}))(\.(\d){1,2})?$/, message: '金额只能以数字形式输入，最多保留2位小数', trigger: 'blur'}
    ]
  },
  categoryList: []
})

const formRef = ref()

const load = () => {
  request.get('/bill/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      type: data.type,
      category: data.category,
      start: data.start,
      end: data.end,
    }
  }).then(res => {
    data.tableData = res.data.list
    data.total = res.data.total
  })
}
load()

const reset = () => {
  data.type = null
  data.category = null
  data.start = null
  data.end = null
  load()
}

const handleAdd = () => {
  data.formVisible = true
  data.form = {}
}

const handleUpdate = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  request.get('/category/selectAll',{
    params: {
      type: data.form.type
    }
  }).then(res => {
    data.categoryList = res.data || []
  })
  data.formVisible = true
}

const handleDelete = (id) => {
  ElMessageBox.confirm('删除账单是永久且不可逆的。您确定要删除吗？','删除确认',{
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    request.delete('/bill/delete/' +id).then(res=>{
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
  data.rows = rows
  data.ids = rows.map(v => v.id)
}

const save = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      data.form.id ? update() : add()
    }
  })
}

const add = () => {
  request.post('/bill/insert', data.form).then(res => {
    if (res.code === 200) {
      data.formVisible = false
      ElMessage.success('操作成功')
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const update = () => {
  request.put('/bill/update', data.form).then(res => {
    if (res.code === 200) {
      data.formVisible = false
      ElMessage.success('操作成功')
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('Choose an row')
  } else {
    ElMessageBox.confirm('删除账单是永久且不可逆的。您确定要删除吗？','删除确认',{
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      request.delete('/bill/delete/batch', {data: data.ids}).then(res => {
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

const exportData = () => {
  let idsStr = data.ids.join(",")
  let url = `http://localhost:8080/bill/export?ids=${idsStr}`
  let token = data.user.token
  if (token) {
    url += `&token=${token}`
  }
  window.open(url)
}
const handleImportSuccess = (res) => {
  if (res.code === 200) {
    const count = typeof res.data === 'number' ? res.data : null
    if (count === 0) {
      ElMessage.warning("未从文件中解析到可导入的账单记录")
    } else {
      ElMessage.success(count ? `成功导入 ${count} 条账单记录` : "导入成功")
    }
    load()
  } else {
    ElMessage.error(res.msg || "账单导入失败")
  }
}

const getCategoryList = () => {
  if (data.form.category) {
    data.form.category = ''
  }
  request.get('/category/selectAll',{
    params: {
      type: data.form.type
    }
  }).then(res => {
    data.categoryList = res.data || []
  })
}

const shortcuts = [
  {
    text: 'Today',
    value: new Date(),
  },
  {
    text: 'Yesterday',
    value: () => {
      const date = new Date()
      date.setTime(date.getTime() - 3600 * 1000 * 24)
      return date
    },
  },
  {
    text: 'A week ago',
    value: () => {
      const date = new Date()
      date.setTime(date.getTime() - 3600 * 1000 * 24 * 7)
      return date
    },
  },
]

const disabledDate = (time) => {
  return time.getTime() > Date.now();
}

const formatTime = (row, column, cellValue) => {
  return cellValue
      ? dayjs(cellValue).format('YYYY-MM-DD HH:mm:ss')
      : ''
}
</script>

<style scoped>
.search {
  width: 200px;
  margin-right: 10px;
  padding-bottom: 5px;
}
.bill-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.bill-toolbar :deep(.el-button) {
  margin: 0 !important;
}
.bill-upload-inline {
  display: inline-flex;
  align-items: center;
}
.bill-upload-inline :deep(.el-upload) {
  display: inline-flex;
  align-items: center;
}
.empty-table-placeholder {
  padding: 36px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.empty-table-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
.empty-table-sub {
  font-size: 12.5px;
  color: var(--el-text-color-secondary);
}
</style>
