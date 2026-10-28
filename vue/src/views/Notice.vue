<template>
  <div>
    <el-card>
      <el-input
          v-model="data.title"
          style="width: 240px; margin-right: 15px"
          placeholder="输入公告标题以查询"
          clearable
          :prefix-icon="Search"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="warning" @click="reset">清空</el-button>
      <el-button type="info" @click="handleAdd">新建公告</el-button>
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
        <el-table-column label="公告标题" prop="title" show-overflow-tooltip />
        <el-table-column label="内容" prop="content" show-overflow-tooltip/>
        <el-table-column label="发布时间" prop="time" show-overflow-tooltip/>
        <el-table-column label="发布者" prop="user" show-overflow-tooltip/>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button
                link
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
    <el-dialog v-model="data.formVisible" title="公告信息" width="40vw" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" style="padding: 10px 50px 0 20px;" label-position="right" label-width="100px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="data.form.title" autocomplete="off"/>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="data.form.content" type="textarea" :rows="5" autocomplete="off"/>
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
import {Delete, Edit, Search} from "@element-plus/icons-vue";
import {request} from "@/utils/request.js";
import {ElMessage, ElMessageBox} from 'element-plus'

const data = reactive({
  user: JSON.parse(localStorage.getItem('loginToken') || '{}'),
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  title: null,
  formVisible: false,
  ids:[],
  rules: {
    title: [
      {required: true, message: '请输入公告标题', trigger: 'blur'},
    ],
    content: [
      {required: true, message: '请输入公告内容', trigger: 'blur'},
    ]
  }
})

const formRef = ref()

const load = () => {
  request.get('/notice/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      title: data.title,
    }
  }).then(res => {
    data.tableData = res.data.list
    data.total = res.data.total
  })
}
load()

const reset = () => {
  data.title = null
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

const handleDelete = (id) => {
  ElMessageBox.confirm('删除公告是永久且不可逆的。您确定要删除吗？','删除确认',{
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    request.delete('/notice/delete/' +id).then(res=>{
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
  request.post('/notice/insert', data.form).then(res => {
    if (res.code === 200) {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const update = () => {
  request.put('/notice/update', data.form).then(res => {
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
    ElMessageBox.confirm('删除公告是永久且不可逆的。您确定要删除吗？','删除确认',{
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      request.delete('/notice/delete/batch', {data: data.ids}).then(res => {
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
