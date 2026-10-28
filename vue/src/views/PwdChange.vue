<template>
  <div style="margin: 19px 0 20px 0">
    <el-breadcrumb :separator-icon="ArrowRight">
      <el-breadcrumb-item :to="{ path: '/main/home' }">系统首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/main/stats' }">个人信息</el-breadcrumb-item>
      <el-breadcrumb-item>修改密码</el-breadcrumb-item>
    </el-breadcrumb>
  </div>
  <el-card style="padding: 40px 20px; width: 50vw">
    <el-form ref="formRef" :rules="data.rules" :model="data.form" style="padding: 10px 50px 0 20px;" label-position="right" label-width="auto">
      <el-form-item label="旧密码" prop="pwd">
        <el-input type="password" v-model="data.form.pwd" autocomplete="off" show-password/>
      </el-form-item>
      <el-form-item label="新密码" prop="newPwd">
        <el-input type="password" v-model="data.form.newPwd" autocomplete="off" show-password/>
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPwd" required>
        <el-input type="password" v-model="data.form.confirmPwd" autocomplete="off" show-password/>
      </el-form-item>
      <div style="text-align: center">
        <el-button type="primary" @click="updatePwd" style="padding: 20px 30px;">
          Submit
        </el-button>
      </div>
    </el-form>
  </el-card>
</template>

<script setup>
import { ElMessage } from "element-plus";
import { ref, reactive } from "vue";
import request from "@/utils/request.js";
import {ArrowRight} from "@element-plus/icons-vue";

const validatePass = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
  } else if (value !== data.form.newPwd) {
    callback(new Error("两次密码不一致"))
  } else {
    callback()
  }
}

const formRef = ref()
const data = reactive({
  user: JSON.parse(localStorage.getItem('loginToken') || '{}'),
  form: {},
  rules: {
    pwd: [
      { required: true, message: '请输入旧密码', trigger: 'blur' },
    ],
    newPwd: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
    ],
    confirmPwd: [
      { validator: validatePass, trigger: 'blur' },
    ],
  }
})

const updatePwd = () => {
  data.form.id = data.user.id
  data.form.perm = data.user.perm
  data.form.role = data.user.role
  data.form.username = data.user.username
  formRef.value.validate((valid) => {
    if (valid) {
      request.put('/updatePwd', data.form).then(res => {
        if (res.code === 200) {
          ElMessage.success("修改成功")
          localStorage.removeItem('loginToken')
          setTimeout(() => {
            location.href = '/login'
          }, 500)
        } else {
          ElMessage.error(res.msg)
        }
      })
    }
  })
}
</script>

<style scoped>

</style>
