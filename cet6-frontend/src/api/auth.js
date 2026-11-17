import request from '@/utils/request'

export const login = data => request.post('/auth/login', data)
export const register = data => request.post('/auth/register', data)
export const getUserInfo = () => request.get('/auth/info')
export const changePassword = data => request.post('/auth/change-password', data)
export const updateProfile = data => request.put('/auth/profile', data)
export const getHomeData = () => request.get('/public/home')
