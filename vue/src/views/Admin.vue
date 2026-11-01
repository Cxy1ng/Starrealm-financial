<template>
  <div>
    <el-card>
      <el-input
          v-model="data.username"
          style="width: 240px; margin-right: 15px"
          placeholder="输入用户名以查询"
          clearable
          :prefix-icon="Search"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="warning" @click="reset">清空</el-button>
      <el-button type="info" @click="handleAdd" v-if="currentUser.perm === 15">添加管理员</el-button>
      <el-button type="danger" @click="delBatch" v-if="currentUser.perm === 15">批量删除</el-button>
    </el-card>
    <el-card style="margin-top: 5px">
      <el-table :data="data.tableData"
                :table-layout="'auto'"
                :default-sort="{ prop: 'id', order: 'descending'}"
                @selection-change="handleSelectionChange"
                >
        <el-table-column type="selection" width="55"/>
        <el-table-column label="ID" prop="id" sortable />
        <el-table-column label="用户名" prop="username" show-overflow-tooltip />
        <el-table-column label="昵称" prop="nickname" show-overflow-tooltip/>
        <el-table-column label="头像">
          <template #default="scope">
            <el-avatar :src="scope.row.avatar" style="width: 40px; height: 40px; border-radius: 50%"/>
          </template>
        </el-table-column>
        <el-table-column label="手机号" prop="phone" />
        <el-table-column label="邮箱" prop="email" show-overflow-tooltip/>
        <el-table-column label="权限身份" width="130" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.perm === 15 ? 'danger' : 'success'" size="small" effect="dark">
              {{ scope.row.perm === 15 ? '超级管理员' : '普通管理员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="110" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.banned === 1 ? 'danger' : 'success'" size="small" effect="light">
              {{ scope.row.banned === 1 ? '已封禁' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="scope">
            <div class="table-action-group">
              <!-- 超级管理员不可被普通管理员编辑；普通管理员不可修改超级管理员 -->
              <el-button
                  v-if="canOperate(scope.row)"
                  link
                  type="primary"
                  @click="handleUpdate(scope.row)"
                  :icon="Edit">
                编辑
              </el-button>
              <!-- 仅超级管理员可对普通管理员封禁/解封，且不能封禁自己 -->
              <template v-if="currentUser.perm === 15 && scope.row.perm < 15 && scope.row.id !== currentUser.id">
                <el-button
                    v-if="scope.row.banned === 1"
                    link
                    type="success"
                    @click="handleToggleBan(scope.row, 0)"
                    :icon="Unlock">
                  解封
                </el-button>
                <el-button
                    v-else
                    link
                    type="warning"
                    @click="handleToggleBan(scope.row, 1)"
                    :icon="Lock">
                  封禁
                </el-button>
              </template>
              <!-- 仅超级管理员可删除普通管理员，且不能删除超级管理员自身 -->
              <el-button
                  v-if="currentUser.perm === 15 && scope.row.perm < 15 && scope.row.id !== currentUser.id"
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
    <el-dialog v-model="data.formVisible" title="管理员信息" width="40vw" align-center destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" style="padding: 10px 50px 0 20px;" label-position="right" label-width="auto">
        <el-form-item label="用户名" prop="username">
          <el-input :disabled="data.form.id" v-model="data.form.username" autocomplete="off" placeholder="请输入用户名" clearable/>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="data.form.phone" autocomplete="off" placeholder="请输入手机号" clearable/>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="data.form.email" autocomplete="off" placeholder="请输入邮箱" clearable/>
        </el-form-item>
        <el-form-item label="角色权限" v-if="currentUser.perm === 15">
          <el-select v-model="data.form.perm" style="width: 100%;">
            <el-option :value="10" label="普通管理员 (系统管理员)" />
            <el-option :value="15" label="超级管理员 (最高权限)" />
          </el-select>
        </el-form-item>
        <el-form-item label="头像">
          <el-upload
              class="avatar-uploader"
              action="http://localhost:8080/files/upload"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
          >
            <img v-if="data.form.avatar" :src="data.form.avatar" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
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

<style scoped>
.avatar-uploader {
  width: 100%;
  margin-bottom: 20px;
}
.avatar-uploader .avatar {
  width: 80px;
  height: 80px;
  display: block;
}
</style>

<script setup>
import {reactive, ref} from "vue";
import {Delete, Edit, Female, Lock, Male, Plus, Search, Unlock} from "@element-plus/icons-vue";
import {request} from "@/utils/request.js";
import {ElMessage, ElMessageBox} from 'element-plus'

const currentUser = JSON.parse(localStorage.getItem('loginToken') || '{}')

const canOperate = (row) => {
  // 超级管理员(15)可以编辑自己和所有普通管理员
  if (currentUser.perm === 15) return true
  // 普通管理员只能编辑自己，绝不能操作超级管理员或其他管理员
  return row.id === currentUser.id
}

const data = reactive({
  username: null,
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  ids:[],
  rules: {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' }
    ],
    phone: [
      { required: true, message: '请输入有效的手机号', trigger: 'blur' }
    ],
    email: [
      { required: true, message: '请输入有效的邮箱地址', trigger: 'blur' }
    ]
  }
})

const formRef = ref()

const load = () => {
  request.get('/admin/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      username: data.username,
    }
  }).then(res => {
    data.tableData = res.data.list
    data.total = res.data.total
  })
}
load()

const reset = () => {
  data.username = null
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
  ElMessageBox.confirm('删除管理员信息是永久且不可逆的。您确定要删除吗？','删除确认',{
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    request.delete('admin/delete/' +id).then(res=>{
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

const save = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      data.form.id ? update() : add()
    }
  })
}

const add = () => {
  request.post('/admin/insert', data.form).then(res => {
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
  request.put('/admin/update', data.form).then(res => {
    if (res.code === 200) {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const handleToggleBan = (row, banned) => {
  const actionText = banned === 1 ? '封禁' : '解封'
  ElMessageBox.confirm(`确定要${actionText}管理员「${row.username}」吗？${banned === 1 ? '封禁后该管理员将被强制下线且无法登录后台。' : '解封后恢复正常管理权限。'}`, `${actionText}确认`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: banned === 1 ? 'warning' : 'info'
  }).then(() => {
    request.put('/admin/ban', {
      id: row.id,
      banned: banned
    }).then(res => {
      if (res.code === 200) {
        ElMessage.success(`已${actionText}管理员 ${row.username}`)
        load()
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    })
  }).catch(() => {})
}

const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('Choose an row')
  } else {
    ElMessageBox.confirm('删除管理员信息是永久且不可逆的。您确定要删除吗？','删除确认',{
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      request.delete('/admin/delete/batch', {data: data.ids}).then(res => {
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

const handleAvatarSuccess = (res) => {
  console.log(res.data)
  data.form.avatar = res.data
}
</script>


<style>
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 80px;
  height: 80px;
  text-align: center;
}
</style>
