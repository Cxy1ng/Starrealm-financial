<template>
  <div>
    <el-card>
      <div class="table-search-row">
        <el-input
            v-model="data.userName"
            class="search-input"
            placeholder="输入用户名以查询"
            clearable
            :prefix-icon="Search"
        />
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        <el-button class="btn-toolbar-reset" :icon="Refresh" @click="reset">重置</el-button>
        <el-button type="danger" :icon="Delete" @click="delBatch">批量删除</el-button>
      </div>
    </el-card>
    <el-card style="margin-top: 5px">
      <el-table :data="data.tableData"
                :table-layout="'auto'"
                :default-sort="{ prop: 'id', order: 'descending'}"
                @selection-change="handleSelectionChange"
                >
        <el-table-column type="selection" width="55" align="center"/>
        <el-table-column label="ID" prop="id" sortable />
        <el-table-column label="用户 ID" prop="userId"/>
        <el-table-column label="用户名" prop="userName" show-overflow-tooltip/>
        <el-table-column label="账户总收支差" prop="balance" />
        <el-table-column label="账户总收入" prop="income" />
        <el-table-column label="账户总支出" prop="pay" />
        <el-table-column label="操作" align="center" width="120">
          <template #default="scope">
            <div class="table-action-group">
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
  </div>
</template>

<script setup>
import {reactive} from "vue";
import {Delete, Search, Refresh} from "@element-plus/icons-vue";
import {ElMessage, ElMessageBox} from "element-plus";
import {request} from "@/utils/request.js";

const data = reactive({
  user: JSON.parse(localStorage.getItem('loginToken') || '{}'),
  userName: null,
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  ids:[],
  rules: {}
})

const load = () => {
  request.get('/acfun/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      userName: data.userName,
    }
  }).then(res => {
    data.tableData = res.data.list
    data.total = res.data.total
  })
}
load()

const reset = () => {
  data.userName = null
  load()
}

const handleDelete = (id) => {
  ElMessageBox.confirm('删除数据是永久且不可逆的。您确定要删除吗？','删除确认',{
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    request.delete('/acfun/delete/' +id).then(res=>{
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
  console.log(data.ids)
}

const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请选择数据')
  } else {
    ElMessageBox.confirm('删除数据是永久且不可逆的。您确定要删除吗？','删除确认',{
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      request.delete('/acfun/delete/batch', {data: data.ids}).then(res => {
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
</script>

<style scoped>

</style>
