import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/Home.vue') },
      { path: 'login', name: 'Login', component: () => import('@/views/Login.vue') },
      { path: 'register', name: 'Register', component: () => import('@/views/Register.vue') },
      { path: 'word-study', name: 'WordStudy', component: () => import('@/views/student/WordStudy.vue'), meta: { roles: ['student', 'admin'] } },
      { path: 'question-practice', name: 'QuestionPractice', component: () => import('@/views/student/QuestionPractice.vue'), meta: { roles: ['student', 'admin'] } },
      { path: 'error-book', name: 'ErrorBook', component: () => import('@/views/student/ErrorBook.vue'), meta: { roles: ['student', 'admin'] } },
      { path: 'study-report', name: 'StudyReport', component: () => import('@/views/student/StudyReport.vue'), meta: { roles: ['student', 'admin'] } },
      { path: 'study-plan', name: 'StudyPlan', component: () => import('@/views/student/StudyPlan.vue'), meta: { roles: ['student', 'admin'] } },
      { path: 'translate', name: 'Translate', component: () => import('@/views/student/Translate.vue'), meta: { roles: ['student', 'admin'] } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/student/Profile.vue'), meta: { roles: ['student', 'admin'] } },
      { path: 'messages', name: 'Messages', component: () => import('@/views/student/Messages.vue'), meta: { roles: ['student', 'admin'] } }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { roles: ['admin'] },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/Users.vue') },
      { path: 'notices', name: 'AdminNotices', component: () => import('@/views/admin/Notices.vue') },
      { path: 'wordbooks', name: 'AdminWordBooks', component: () => import('@/views/admin/WordBooks.vue') },
      { path: 'words', name: 'AdminWords', component: () => import('@/views/admin/Words.vue') },
      { path: 'papers', name: 'AdminPapers', component: () => import('@/views/admin/Papers.vue') },
      { path: 'questions', name: 'AdminQuestions', component: () => import('@/views/admin/Questions.vue') },
      { path: 'logs', name: 'AdminLogs', component: () => import('@/views/admin/Logs.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const role = userStore.role

  if (to.meta.roles && !to.meta.roles.includes(role)) {
    if (!userStore.isLoggedIn) {
      next('/login')
    } else {
      next('/')
    }
    return
  }
  next()
})

export default router
