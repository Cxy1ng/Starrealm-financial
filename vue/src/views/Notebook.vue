<template>
  <div>
    <el-card>
      <div class="table-search-row">
        <el-input
            v-model="data.title"
            class="search-input"
            placeholder="输入日记标题以查询"
            clearable
            :prefix-icon="Search"
        />
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        <el-button class="btn-toolbar-reset" :icon="Refresh" @click="reset">重置</el-button>
        <el-button type="success" :icon="Plus" @click="handleAdd" v-if="data.user.role === 'USER'">新建日记</el-button>
        <el-button type="danger" :icon="Delete" @click="delBatch" v-if="data.user.perm >= 10">批量删除</el-button>
      </div>
    </el-card>
    <el-card style="margin-top: 5px" v-if="data.user.role === 'USER'">
      <el-row :gutter="16" v-if="data.tableData.length">
        <el-col
            v-for="item in data.tableData"
            :key="item.id"
            :xs="24"
            :sm="24"
            :md="12"
            :lg="12"
            style="margin-bottom: 16px"
        >
          <el-card class="card-content" shadow="hover">
            <div class="card-body">
              <el-image :src="item.cover" fit="cover" class="card-image" />
              <div class="card-main">
                <div class="card-title line1">{{ item.title }}</div>
                <div class="card-info">
                  <i class="el-icon-user" /> {{ item.userName }}
                  <i class="el-icon-date" style="margin-left: 10px" /> {{ item.date }}
                </div>
                <div class="card-actions">
                  <el-button @click="showContent(item.content)" :icon="Document" link>查看</el-button>
                  <el-button @click="handleEdit(item)" :icon="Edit" type="primary" link>编辑</el-button>
                  <el-button @click="del(item.id)" :icon="Delete" type="danger" link>删除</el-button>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-card v-else class="empty-placeholder-card" shadow="never">
        <div class="empty-card-inner">
          <div class="empty-card-title">暂无理财日记</div>
          <div class="empty-card-sub">当前还没有任何理财日记，点击上方「新建日记」记录您的第一篇理财心得吧</div>
          <el-button type="primary" size="small" style="margin-top: 8px" @click="handleAdd">
            新建理财日记
          </el-button>
        </div>
      </el-card>
    </el-card>

    <el-card style="margin-top: 5px" v-else-if="data.user.perm >= 10">
      <el-table
          :data="data.tableData"
          style="width: 100%"
          border
          @selection-change="handleSelectionChange"
      >
        <template #empty>
          <div class="empty-table-placeholder">
            <div class="empty-card-title">表格是空的 · 暂无理财日记数据</div>
            <div class="empty-card-sub">当前没有任何用户的理财日记记录</div>
          </div>
        </template>
        <el-table-column type="selection" width="55" align="center" sortable/>
        <el-table-column prop="id" label="ID" width="80" align="center" sortable/>
        <el-table-column label="封面">
          <template #default="scope">
            <el-image :src="scope.row.cover" style="max-width: 120px; height: 60px" fit="cover"/>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" show-overflow-tooltip/>
        <el-table-column label="内容">
          <template #default="scope">
            <el-button @click="showContent(scope.row.content)">查看内容</el-button>
          </template>
        </el-table-column>
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

    <el-dialog v-model="data.formVisible" title="日记信息" width="min(760px, 92vw)">
      <el-form :model="data.form" :rules="data.rules" ref="formRef" label-width="auto" style="margin-right: 20px">
        <el-form-item label="日记标题" prop="title">
          <el-input v-model="data.form.title" />
        </el-form-item>
        <el-form-item label="日记封面" prop="cover">
          <el-upload
              action="http://localhost:8080/files/upload"
              list-type="picture"
              :headers="{ token: data.user.token }"
              :on-success="handleCoverSuccess"
          >
            <el-button type="primary">上传封面</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="内容">
          <Toolbar style="border-bottom: 1px solid var(--w-e-toolbar-border-color)" :editor="editorRef" :default-config="toolbarConfig" mode="default" />
          <Editor style="height: 300px; width: 100%; overflow-y: hidden;" v-model="data.form.content" :default-config="editorConfig" :mode="mode" @onCreated="handleEditorCreated" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="data.formVisible = false">取消</el-button>
          <el-button type="primary" @click="save">保存</el-button>
        </span>
      </template>
    </el-dialog>
    <el-dialog v-model="data.contentVisible" title="日记内容" width="min(760px, 92vw)">
      <div v-html="data.content" style="max-height: 400px; overflow-y: auto; padding: 0 20px;"></div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, shallowRef, onBeforeUnmount } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import {ElMessage, ElMessageBox} from "element-plus";
import request from "@/utils/request.js";
import {Delete, Edit, Plus, Search, Document, Refresh} from "@element-plus/icons-vue";
import { getStoredLoginUser } from "@/utils/auth.js";

const data = reactive({
  user: getStoredLoginUser(),
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  contentVisible: false,
  form: {},
  ids: [],
  content: null,
  rules: {
    title: [
      { required: true, message: '请输入标题', trigger: 'blur' }
    ],
    cover: [
      { required: true, message: '请上传封面图', trigger: 'blur' }
    ]
  }
})

const formRef = ref()
const editorRef = shallowRef()
const mode = 'default'
const toolbarConfig = {}
const editorConfig = {
  enableHtml: true,
  MENU_CONF: {}
}

editorConfig.MENU_CONF['uploadImage'] = {
  headers: {
    token: data.user.token,
  },
  server: 'http://localhost:8080/files/editor/upload',
  fieldName: 'file',
  maxFileSize: 10 * 1024 * 1024,
}

editorConfig.MENU_CONF['uploadVideo'] = {
  headers: {
    token: data.user.token,
  },
  server: 'http://localhost:8080/files/editor/upload',
  fieldName: 'file',
  maxFileSize: 20 * 1024 * 1024,
}
const handleEditorCreated = (editor) => {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

const load = () => {
  request.get('/notebooks', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      title: data.title
    }
  }).then(res => {
    if (res.code === 200) {
      data.tableData = res.data.list
      data.total = res.data.total
    } else {
      ElMessage.error(res.msg)
    }
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

const handleCoverSuccess = (res) => {
  data.form.cover = res.data
}

const save = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      data.form.id ? update() : add()
    }
  })
}

const handleUpdate = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

const handleDelete = (id) => {
  ElMessageBox.confirm('删除日记是永久且不可逆的。您确定要删除吗？','删除确认',{
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    request.delete('/notebooks/' + id).then(res => {
      if (res.code === 200) {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

const handleSelectionChange = (rows) => {
  data.ids = rows.map(row => row.id)
}



const add = () => {
  request.post('/notebooks', data.form).then(res => {
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
  request.put('/notebooks/' + data.form.id, data.form).then(res => {
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
    ElMessage.warning('Choose an row')
  } else {
    ElMessageBox.confirm('删除日记是永久且不可逆的。您确定要删除吗？','删除确认',{
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      request.delete('/notebooks/batch', { data: data.ids }).then(res => {
        if (res.code === 200) {
          ElMessage.success('操作成功')
          load()
        } else {
          ElMessage.error(res.msg)
        }
      })
    }).catch(() => {})
  }
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.content = data.form.content
  data.formVisible = true
}
const showContent = (content) => {
  data.content = content
  data.contentVisible = true
}
</script>
<style scoped>
.card-content {
  display: flex;
  flex-direction: column;
}

.card-body {
  display: flex;
  gap: 12px;
  align-items: center;
}

.card-image {
  width: 120px;
  height: 120px;
  flex-shrink: 0;
  border-radius: 4px;
  object-fit: cover;
}

.card-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.card-title {
  font-size: 18px;
  font-weight: bold;
}

.card-info {
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .card-body {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-image {
    width: 100%;
    height: auto;
  }

  .card-actions {
    justify-content: flex-start;
  }

  .card-title {
    font-size: 16px;
  }
}

.empty-placeholder-card {
  border-style: dashed !important;
}

.empty-card-inner,
.empty-table-placeholder {
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
