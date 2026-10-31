import axios from 'axios'
import { ElMessage } from 'element-plus'
import { auth } from '../store/auth'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  if (auth.token) {
    config.headers.Authorization = 'Bearer ' + auth.token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 后端统一返回 { code, message, data }，code=0 才算成功
    if (body && body.code === 0) {
      return body.data
    }
    ElMessage.error((body && body.message) || '请求失败')
    return Promise.reject(new Error((body && body.message) || '请求失败'))
  },
  (error) => {
    const status = error.response && error.response.status
    const body = error.response && error.response.data
    if (status === 401) {
      auth.logout()
      router.push('/login')
      ElMessage.warning('登录已过期，请重新登录')
    } else {
      ElMessage.error((body && body.message) || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
