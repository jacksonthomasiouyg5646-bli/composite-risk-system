import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearSession, getStoredToken } from '../stores/session'

const http = axios.create({
  baseURL: '',
  timeout: 15000
})

const publicAuthPaths = [
  '/api/auth/login',
  '/api/auth/captcha'
]

function isPublicAuthRequest(url = '') {
  return publicAuthPaths.some((path) => url.startsWith(path))
}

http.interceptors.request.use((config) => {
  const token = getStoredToken()
  const url = config.url || ''

  if (token && !isPublicAuthRequest(url)) {
    config.headers.Authorization = `Bearer ${token}`
  } else if (isPublicAuthRequest(url)) {
    delete config.headers.Authorization
  }

  return config
})

http.interceptors.response.use(
  (response) => {
    if (response.headers['content-type']?.includes('text/csv')) {
      return response
    }

    const body = response.data
    if (body?.code !== 0) {
      ElMessage.error(body?.message || '\u8bf7\u6c42\u5931\u8d25')
      return Promise.reject(new Error(body?.message || 'request failed'))
    }

    return body.data
  },
  (error) => {
    const status = error.response?.status

    if (status === 401) {
      clearSession()
      if (location.pathname !== '/login') {
        location.replace('/login')
      }
    } else if (status === 403) {
      ElMessage.error(error.response?.data?.message || '\u65e0\u6743\u8bbf\u95ee\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55\u6216\u786e\u8ba4\u540e\u7aef\u7f51\u5173\u8de8\u57df\u914d\u7f6e')
    } else if ([500, 503].includes(status) && isPublicAuthRequest(error.config?.url)) {
      return Promise.reject(error)
    } else if (status === 503) {
      ElMessage.error(error.response?.data?.message || '\u540e\u7aef\u8ba4\u8bc1\u670d\u52a1\u672a\u542f\u52a8\uff0c\u8bf7\u5148\u542f\u52a8 auth-service')
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '\u7f51\u7edc\u5f02\u5e38')
    }

    return Promise.reject(error)
  }
)

export default http
