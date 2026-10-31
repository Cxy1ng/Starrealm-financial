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
      <el-button type="info" @click="handleAdd">添加用户</el-button>
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
        <el-table-column label="用户名" prop="username" show-overflow-tooltip />
        <el-table-column label="昵称" prop="nickname" show-overflow-tooltip/>
        <el-table-column label="性别" prop="sex" />
        <el-table-column label="头像">
          <template #default="scope">
            <el-avatar :src="scope.row.avatar" style="width: 40px; height: 40px; border-radius: 50%"/>
          </template>
        </el-table-column>
        <el-table-column label="手机号" prop="phone" />
        <el-table-column label="邮箱" prop="email" show-overflow-tooltip/>
        <el-table-column label="生日" prop="birth" />
        <el-table-column label="账号状态" width="120" align="center">
          <template #default="scope">
            <el-tooltip
                v-if="scope.row.banned === 1"
                :content="'封禁原因：' + (scope.row.banReason || '违反《星界用户服务协议》') + (scope.row.banTime ? ' (' + scope.row.banTime + ')' : '')"
                placement="top"
            >
              <el-tag type="danger" effect="dark" size="small">已封禁</el-tag>
            </el-tooltip>
            <el-tag v-else type="success" effect="light" size="small">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="scope">
            <el-button
                link
                @click="handleUpdate(scope.row)"
                :icon="Edit">
              编辑
            </el-button>
            <el-button
                v-if="scope.row.banned === 1"
                link
                type="success"
                @click="handleUnban(scope.row)"
                :icon="Unlock"
            >
              解封
            </el-button>
            <el-button
                v-else
                link
                type="warning"
                @click="handleOpenBan(scope.row)"
                :icon="Lock"
            >
              封禁
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

    <el-dialog v-model="data.formVisible" title="用户信息" width="40vw" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" style="padding: 10px 50px 0 20px;" label-position="right" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input :disabled="data.form.id" v-model="data.form.username" autocomplete="off" placeholder="请输入用户名" clearable/>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="data.form.nickname" autocomplete="off" placeholder="请输入昵称" clearable/>
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="data.form.sex">
            <el-radio value="男">
              <el-icon style="padding-right: 5px"><Male /></el-icon>男
            </el-radio>
            <el-radio value="女">
              <el-icon style="padding-right: 5px"><Female /></el-icon>女
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="data.form.phone" autocomplete="off" placeholder="请输入手机号" clearable/>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="data.form.email" autocomplete="off" placeholder="请输入邮箱" clearable/>
        </el-form-item>
        <el-form-item label="生日" prop="birth">
          <el-date-picker
              style="width: 100%"
              v-model="data.form.birth"
              type="date"
              placeholder="请选择日期"
              format="YYYY/MM/DD"
              value-format="YYYY-MM-DD"
              clearable
          />
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
        <el-form-item label="个人简介" prop="descriptiontext">
          <el-input v-model="data.form.descriptiontext" autocomplete="off" placeholder="介绍一下自己吧" type="textarea" :autosize="{ minRows: 1, maxRows: 6 }" maxlength="200" show-word-limit/>
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

    <el-dialog v-model="data.banVisible" title="封禁违规账号" width="460px" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="data.banForm" label-position="top" style="padding: 4px 10px 0;">
        <el-alert
            title="封禁后该用户将立即被强制下线且无法登录星界平台，直至管理员解除封禁。"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 14px;"
        />
        <el-form-item label="目标账号">
          <el-input :model-value="data.banForm.username + (data.banForm.nickname ? ' (' + data.banForm.nickname + ')' : '')" disabled />
        </el-form-item>
        <el-form-item label="快捷违规原因">
          <div style="display: flex; flex-wrap: wrap; gap: 6px;">
            <el-tag
                v-for="preset in banReasonPresets"
                :key="preset"
                size="small"
                effect="plain"
                style="cursor: pointer;"
                @click="data.banForm.banReason = preset"
            >
              {{ preset }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="封禁原因说明（将展示给被封禁用户）">
          <el-input
              v-model="data.banForm.banReason"
              type="textarea"
              :rows="3"
              maxlength="120"
              show-word-limit
              placeholder="请输入封禁原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="data.banVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmBan">确认封禁</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {reactive, ref} from "vue";
import {Delete, Edit, Female, Lock, Male, Plus, Search, Unlock} from "@element-plus/icons-vue";
import {request} from "@/utils/request.js";
import {ElMessage, ElMessageBox} from 'element-plus'

const banReasonPresets = [
  '违反《星界用户服务协议》第 4.2 条：恶意提示词注入或诱导 AI 角色扮演越狱',
  '违反《星界用户服务协议》第 4.1 条：发送涉政、色情或违禁违规内容',
  '违反《星界用户服务协议》第 3.3 条：异常高频滥用平台公共 AI 算力配额',
  '账号存在安全风险或异常登录行为，暂时冻结保护'
]

const data = reactive({
  user: JSON.parse(localStorage.getItem('loginToken') || '{}'),
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  banVisible: false,
  banForm: {
    id: null,
    username: '',
    nickname: '',
    banReason: ''
  },
  ids:[],
  rules: {
    username: [
      { required: true, message: '请输入有效的用户名', trigger: 'blur' }
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
  request.get('/user/selectPage', {
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

const handleOpenBan = (row) => {
  data.banForm = {
    id: row.id,
    username: row.username,
    nickname: row.nickname,
    banReason: banReasonPresets[0]
  }
  data.banVisible = true
}

const confirmBan = () => {
  request.put('/user/ban', {
    id: data.banForm.id,
    banned: 1,
    banReason: data.banForm.banReason
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success(`已封禁账号 ${data.banForm.username}`)
      data.banVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const handleUnban = (row) => {
  ElMessageBox.confirm(`确定要解除账号「${row.username}」的封禁状态，恢复其正常登录权限吗？`, '解封确认', {
    confirmButtonText: '确认解封',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    request.put('/user/ban', {
      id: row.id,
      banned: 0,
      banReason: ''
    }).then(res => {
      if (res.code === 200) {
        ElMessage.success(`已解除账号 ${row.username} 的封禁`)
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

const handleDelete = (id) => {
  ElMessageBox.confirm('删除用户信息是永久且不可逆的。您确定要删除吗？','删除确认',{
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    request.delete('/user/delete/' +id).then(res=>{
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
  request.post('/user/insert', data.form).then(res => {
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
  request.put('/user/update', data.form).then(res => {
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
    ElMessageBox.confirm('删除用户信息是永久且不可逆的。您确定要删除吗？','删除确认',{
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      request.delete('/user/delete/batch', {data: data.ids}).then(res => {
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
