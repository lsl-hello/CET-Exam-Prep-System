<template>
  <div class="profile-page">
    <el-card class="profile-card">
      <template #header><span>我的资料</span></template>
      <el-form :model="profileForm" label-width="100px" style="max-width:480px">
        <el-form-item label="用户名">
          <el-input :value="profileForm.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag>{{ roleLabel }}</el-tag>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="profileForm.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="profile-card">
      <template #header><span>修改密码</span></template>
      <el-form :model="pwdForm" label-width="100px" style="max-width:480px">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingPwd" @click="savePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { updateProfile } from '@/api/auth'

const userStore = useUserStore()

const roleLabel = computed(() => {
  const map = { visitor: '游客', student: '六级考生', admin: '管理员' }
  return map[userStore.role] || '游客'
})

const profileForm = reactive({
  username: '',
  nickname: '',
  phone: ''
})
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const savingProfile = ref(false)
const savingPwd = ref(false)

const loadProfile = () => {
  const info = userStore.userInfo || {}
  profileForm.username = info.username || ''
  profileForm.nickname = info.nickname || ''
  profileForm.phone = info.phone || ''
}

const saveProfile = async () => {
  if (!profileForm.nickname.trim()) {
    return ElMessage.warning('昵称不能为空')
  }
  savingProfile.value = true
  try {
    const res = await updateProfile({ nickname: profileForm.nickname, phone: profileForm.phone })
    if (res.code === 200) {
      // 更新本地 store + localStorage，头部昵称同步刷新
      userStore.userInfo = res.data
      localStorage.setItem('userInfo', JSON.stringify(res.data))
      ElMessage.success('资料已更新')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    savingProfile.value = false
  }
}

const savePassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    return ElMessage.warning('请填写原密码和新密码')
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    return ElMessage.warning('两次新密码不一致')
  }
  if (pwdForm.newPassword.length < 6) {
    return ElMessage.warning('新密码至少 6 位')
  }
  savingPwd.value = true
  try {
    await userStore.changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码已修改')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '修改失败，请检查原密码')
  } finally {
    savingPwd.value = false
  }
}

onMounted(() => {
  loadProfile()
  // 若 store 里没有最新信息，拉取一次
  if (!userStore.userInfo) {
    userStore.fetchInfo().then(loadProfile)
  }
})
</script>

<style scoped>
.profile-page {
  padding: 20px;
  max-width: 640px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
</style>
