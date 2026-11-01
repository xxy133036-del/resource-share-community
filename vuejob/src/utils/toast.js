// src/utils/toast.js
import { reactive } from 'vue'

// 1. 定义全局响应式状态
export const toastState = reactive({
    visible: false,
    message: '',
    type: 'success' // 'success' 或 'error'
})

let toastTimer = null

// 2. 导出全局触发方法
export const showToast = (message, type = 'success') => {
    toastState.message = message
    toastState.type = type
    toastState.visible = true

    // 防抖：如果连续点击，清除上一个定时器，重新计时 3 秒
    if (toastTimer) clearTimeout(toastTimer)
    toastTimer = setTimeout(() => {
        toastState.visible = false
    }, 3000)
}