<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>用户登录</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="admin / student1" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="123456" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" style="width:100%">登录</el-button>
        </el-form-item>
        <div class="tips">
          <p>测试账号：admin / student1，密码：123456</p>
          <el-link type="primary" @click="$router.push('/register')">没有账号？立即注册</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    if (userStore.isAdmin) router.push('/admin/dashboard')
    else router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page { display: flex; justify-content: center; padding-top: 80px; }
.auth-card { width: 420px; }
.auth-card h2 { text-align: center; margin-bottom: 24px; }
.tips { text-align: center; color: #909399; font-size: 13px; }
.tips p { margin-bottom: 8px; }
</style>
