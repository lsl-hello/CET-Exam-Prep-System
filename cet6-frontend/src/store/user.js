import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo, changePassword as changePasswordApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const forceChange = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || 'visitor')
  const isAdmin = computed(() => role.value === 'admin')
  const isStudent = computed(() => role.value === 'student')

  async function login(form) {
    const res = await loginApi(form)
    // 进度数据用 userId 隔离存 localStorage，切换账号互不干扰，无需清除
    token.value = res.data.token
    userInfo.value = res.data.userInfo
    forceChange.value = !!res.data.forceChange
    localStorage.setItem('token', token.value)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  async function fetchInfo() {
    const res = await getUserInfo()
    userInfo.value = res.data
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  async function changePassword(oldPassword, newPassword) {
    await changePasswordApi({ oldPassword, newPassword })
    forceChange.value = false
  }

  function logout() {
    // 进度数据存 localStorage 且按 userId 隔离，退出登录不清除，下次同账号登录仍可恢复
    token.value = ''
    userInfo.value = null
    forceChange.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, forceChange, isLoggedIn, role, isAdmin, isStudent, login, fetchInfo, changePassword, logout }
})
