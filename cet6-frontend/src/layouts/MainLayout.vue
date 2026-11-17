<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="logo" @click="$router.push('/')">
        <el-icon><Reading /></el-icon>
        <span>英语四六级备考系统</span>
      </div>
      <el-menu mode="horizontal" :ellipsis="false" router :default-active="$route.path" class="nav-menu">
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item v-if="userStore.isStudent || userStore.isAdmin" index="/word-study">背单词</el-menu-item>
        <el-menu-item v-if="userStore.isStudent || userStore.isAdmin" index="/question-practice">刷题</el-menu-item>
        <el-menu-item v-if="userStore.isStudent || userStore.isAdmin" index="/error-book">错题本</el-menu-item>
        <el-menu-item v-if="userStore.isStudent || userStore.isAdmin" index="/study-report">学习报告</el-menu-item>
        <el-menu-item v-if="userStore.isStudent || userStore.isAdmin" index="/study-plan">学习计划</el-menu-item>
        <el-menu-item v-if="userStore.isStudent || userStore.isAdmin" index="/translate">翻译</el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/admin/dashboard">管理后台</el-menu-item>
      </el-menu>
      <div class="user-area">
        <template v-if="userStore.isLoggedIn">
          <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99" type="danger" class="msg-badge">
            <el-button text @click="$router.push('/messages')"><el-icon><Bell /></el-icon></el-button>
          </el-badge>
          <el-dropdown>
            <span class="user-name">{{ userStore.userInfo?.nickname }} ({{ roleLabel }})</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">我的资料</el-dropdown-item>
                <el-dropdown-item @click="userStore.logout(); $router.push('/')">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" @click="$router.push('/login')">登录</el-button>
          <el-button @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>
    <el-main><router-view /></el-main>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getMessageList } from '@/api/student'

const userStore = useUserStore()
const route = useRoute()
const unreadCount = ref(0)

const roleLabel = computed(() => {
  const map = { visitor: '游客', student: '六级考生', admin: '管理员' }
  return map[userStore.role] || '游客'
})

// 拉取未读消息数（铃铛角标用）
const loadUnread = async () => {
  if (!(userStore.isStudent || userStore.isAdmin)) return
  try {
    const res = await getMessageList({ pageNum: 1, pageSize: 100 })
    unreadCount.value = (res.data.records || []).filter(m => !m.isRead).length
  } catch {}
}

let timer = null
onMounted(async () => {
  await loadUnread()
  // 每 60 秒刷新一次未读数，到点提醒后铃铛自动亮起，无需手动刷新页面
  timer = setInterval(loadUnread, 60000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
// 路由切换时也刷新一次（从消息页返回等场景）
watch(() => route.path, () => loadUnread())
</script>

<style scoped>
.layout { min-height: 100vh; }
.header { display: flex; align-items: center; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,.08); padding: 0 24px; }
.logo { display: flex; align-items: center; gap: 8px; font-size: 18px; font-weight: bold; color: #409eff; cursor: pointer; margin-right: 32px; white-space: nowrap; }
.nav-menu { flex: 1; border: none; }
.user-area { display: flex; align-items: center; gap: 12px; }
.user-name { cursor: pointer; color: #606266; }
.msg-badge { margin-right: 8px; }
</style>
