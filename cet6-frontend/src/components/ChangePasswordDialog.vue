<template>
  <el-dialog
    v-model="visible"
    title="首次登录请修改密码"
    width="420px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    align-center
  >
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="检测到当前账号使用默认密码或密码已被重置，为了账号安全，请先修改密码再继续使用系统。"
      style="margin-bottom: 16px;"
    />
    <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入当前密码" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password placeholder="6-32 位新密码" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="loading" @click="handleSubmit">提交并继续</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const visible = computed({
  get: () => userStore.isLoggedIn && userStore.forceChange,
  set: () => {}
})

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== form.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '新密码长度需在 6-32 位之间', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: 'blur' }]
}

// 弹窗每次打开时清空表单
watch(visible, v => {
  if (v) {
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    formRef.value?.clearValidate()
  }
})

async function handleSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.changePassword(form.oldPassword, form.newPassword)
    ElMessage.success('密码修改成功，已自动解锁')
  } finally {
    loading.value = false
  }
}
</script>
