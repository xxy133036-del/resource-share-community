/**
 * 管理员端 Axios 实例
 * baseURL: /admin-api → 代理到 admin8012 服务
 */
import axios from 'axios'
import router from '@/router'
import { showToast } from '@/utils/toast'

const adminRequest = axios.create({
    baseURL: '/admin-api',
    timeout: 30000
})

// 请求拦截器：注入 Token
adminRequest.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            const cleanToken = token.startsWith('Bearer ') ? token.slice(7) : token
            config.headers['authorization'] = `Bearer ${cleanToken}`
        }
        return config
    },
    error => Promise.reject(error)
)

// 响应拦截器
adminRequest.interceptors.response.use(
    response => {
        // 文件下载（blob）
        if (response.config.responseType === 'blob') {
            const contentType = response.headers['content-type'] || ''
            if (contentType.includes('application/json')) {
                return response.data.text().then(text => {
                    try {
                        const errorRes = JSON.parse(text)
                        if (errorRes.code !== 200) {
                            showToast(errorRes.message || '操作失败', 'error')
                        }
                        return Promise.reject(new Error(errorRes.message || '操作失败'))
                    } catch (e) {
                        return Promise.reject(new Error('响应解析失败'))
                    }
                })
            }
            return response
        }

        const res = response.data

        // 登录失效
        if (res.code === 401 || (res.code === 500 && (res.message?.includes('登录') || res.message?.includes('Token')))) {
            localStorage.removeItem('token')
            localStorage.removeItem('role')
            showToast(res.message || '登录已失效，请重新登录', 'error')
            setTimeout(() => router.push('/login'), 300)
            return Promise.reject(new Error(res.message || '登录状态已失效'))
        }

        return res
    },
    error => {
        if (error.response && error.response.status === 401) {
            localStorage.removeItem('token')
            localStorage.removeItem('role')
            showToast('登录已失效，请重新登录', 'error')
            setTimeout(() => router.push('/login'), 300)
        } else if (error.response && error.response.data?.message) {
            showToast(error.response.data.message, 'error')
        }
        console.error('Admin 请求异常：', error)
        return Promise.reject(error)
    }
)

export default adminRequest
