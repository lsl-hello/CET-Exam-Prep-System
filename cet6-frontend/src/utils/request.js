import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

request.interceptors.response.use(
  res => {
    const data = res.data
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      if (data.code === 401) {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      }
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  err => {
    ElMessage.error(err.response?.data?.message || '网络异常')
    if (err.response?.status === 401 || err.response?.status === 403) {
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
    }
    return Promise.reject(err)
  }
)

export default request
