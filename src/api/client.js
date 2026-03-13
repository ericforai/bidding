/**
 * HTTP 客户端封装
 * 基于 axios 实现，支持拦截器、错误处理、Token 管理
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_CONFIG, getApiUrl } from './config'

export const getAccessToken = () => localStorage.getItem('token') || sessionStorage.getItem('token')

// 创建 axios 实例
const httpClient = axios.create({
  baseURL: API_CONFIG.baseURL,
  timeout: API_CONFIG.timeout,
  headers: API_CONFIG.headers
})

// 请求拦截器
httpClient.interceptors.request.use(
  (config) => {
    // 添加 Token
    const token = getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
httpClient.interceptors.response.use(
  (response) => {
    // 后端返回的统一格式: { success: true, data: ..., message: ... }
    return response.data
  },
  (error) => {
    const { response } = error

    if (response) {
      // 服务器返回错误状态码
      switch (response.status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          // 清除 token 并跳转登录
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          window.location.href = '/login'
          break
        case 403:
          ElMessage.error('没有权限访问该资源')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误，请稍后重试')
          break
        default:
          ElMessage.error(response.data?.message || '请求失败')
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络连接')
    } else {
      ElMessage.error('网络连接失败，请检查后端服务是否启动')
    }

    return Promise.reject(error)
  }
)

export default httpClient
