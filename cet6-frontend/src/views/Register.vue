<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>注册账号</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleRegister" style="width:100%">注册</el-button>
        </el-form-item>
        <div class="tips">
          <el-link type="primary" @click="$router.push('/login')">已有账号？去登录</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '', nickname: '', phone: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page { display: flex; justify-content: center; padding-top: 80px; }
.auth-card { width: 420px; }
.auth-card h2 { text-align: center; margin-bottom: 24px; }
.tips { text-align: center; }
</style>
