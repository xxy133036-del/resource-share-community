import axios from 'axios'
import router from '@/router'
import { showToast } from '@/utils/toast'


const service = axios.create({
    baseURL: '/api',
    timeout: 30000 // 请求超时时间：30秒（支持文件上传等耗时操作）
})

// 2. 请求拦截器：每次发请求之前，都会偷偷走这里
service.interceptors.request.use(
    config => {
        // 从 localStorage 获取 token 并添加到请求头
        const token = localStorage.getItem('token')
        if (token) {
            // 兼容旧格式（Bearer xxx）和新格式（纯 token），先清理再统一加前缀
            const cleanToken = token.startsWith('Bearer ') ? token.slice(7) : token
            config.headers['authorization'] = `Bearer ${cleanToken}`
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 3. 响应拦截器：后端返回数据后，到页面之前，先走这里
service.interceptors.response.use(
    response => {
        // 如果是 blob 类型（文件下载），返回完整的 response 对象
        if (response.config.responseType === 'blob') {
            // 检查 blob 是否是 JSON 错误（后端可能返回 JSON 错误而非文件流）
            const contentType = response.headers['content-type'] || ''
            if (contentType.includes('application/json')) {
                // 是 JSON 错误，需要解析
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
            // 是正常的文件 blob，返回完整 response（包含 headers）
            return response
        }

        const res = response.data

        if (res.code === 401 || (res.code === 500 && (res.message?.includes('登录') || res.message?.includes('Token')))) {
            console.warn('业务层面判定登录失效，正在强制登出...')

            localStorage.removeItem('token')

            showToast(res.message || '用户未登录或登录已失效，请重新登录', 'error')

            setTimeout(() => {
                router.push('/login')
            }, 300)

            return Promise.reject(new Error(res.message || '登录状态已失效'))
        }

        return res
    },
    error => {
        if (error.response && error.response.status === 401) {
            console.warn('HTTP 层面判定登录失效，正在强制登出...')
            localStorage.removeItem('token')

            const message = error.response.data?.message || '用户未登录或登录已失效，请重新登录'
            showToast(message, 'error')

            setTimeout(() => {
                router.push('/login')
            }, 300)
        } else if (error.response && error.response.data?.message) {
            showToast(error.response.data.message, 'error')
        }

        console.error('全局网络请求报错：', error)
        return Promise.reject(error)
    }
)

export default service // 导出这个被终极强化的 axios