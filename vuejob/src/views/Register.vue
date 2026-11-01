<template>
  <div class="page-wrapper">
    <div class="register-card">
      <div class="header">
        <h2>创建账号</h2>
        <p>欢迎加入我们，开启您的全新体验</p>
      </div>

      <form @submit.prevent="handleRegister" class="register-form">

        <div class="form-group">
          <label for="username">账号</label>
          <input
              id="username"
              type="text"
              v-model="formData.username"
              placeholder="12位以内，仅限数字或字母"
              maxlength="12"
              required
              autocomplete="off"
          />
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <div class="input-wrapper">
            <input
                id="password"
                :type="showPassword ? 'text' : 'password'"
                v-model="formData.password"
                placeholder="至少6位，仅限数字或字母"
                required
            />
            <button type="button" class="eye-btn" @click="showPassword = !showPassword">
              <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </button>
          </div>
        </div>

        <div class="form-group">
          <label for="nickname">昵称</label>
          <input
              id="nickname"
              type="text"
              v-model="formData.nickname"
              placeholder="12个字符以内"
              maxlength="12"
              required
              autocomplete="off"
          />
        </div>

        <button type="submit" class="submit-btn" :disabled="isLoading">
          {{ isLoading ? '注册中...' : '立即注册' }}
        </button>

      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'
import { useRouter } from 'vue-router'
// 1. 引入全局 Toast
import { showToast } from '@/utils/toast'

const router = useRouter()

const formData = ref({
  username: '',
  password: '',
  nickname: '',
})

const isLoading = ref(false)
const showPassword = ref(false)

const validateForm = () => {
  const usernameRegex = /^[a-zA-Z0-9]+$/
  if (!usernameRegex.test(formData.value.username)) {
    showToast('格式错误：账号只能包含数字和字母！', 'error')
    return false
  }
  if (formData.value.username.length > 12) {
    showToast('格式错误：账号必须在12个字符以内！', 'error')
    return false
  }

  const passwordRegex = /^[a-zA-Z0-9]{6,}$/
  if (!passwordRegex.test(formData.value.password)) {
    showToast('格式错误：密码必须在6个字符以上，且只能包含数字和字母！', 'error')
    return false
  }

  if (formData.value.nickname.trim().length === 0) {
    showToast('格式错误：昵称不能为空或全空格！', 'error')
    return false
  }
  if (formData.value.nickname.length > 12) {
    showToast('格式错误：昵称必须在12个字符以内！', 'error')
    return false
  }

  return true
}

const handleRegister = async () => {
  if (!validateForm()) {
    return
  }

  if (isLoading.value) return
  isLoading.value = true

  try {
    const res = await request.post('/register', formData.value)

    // 2. 统一对接后端 CommonResult 的 code 逻辑
    if (res.code === 200) {
      showToast('注册成功！欢迎加入', 'success')
      localStorage.setItem('token', res.data)

      // 状态重置
      formData.value = {username: '', password: '', nickname: ''}

      // 直接跳转
      router.push('/sys/home')
    } else {
      // 捕获并展示你后端 RegisterController 返回的错误信息
      showToast(res.message || '注册失败，请稍后重试', 'error')
    }

  } catch (error) {
    console.error('注册遇到问题：', error)
    showToast('注册失败，请检查网络或联系管理员', 'error')
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.page-wrapper {
  min-height: 100vh;
  width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafafa;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  box-sizing: border-box;
}

.register-card {
  width: 100%;
  max-width: 380px;
  background: #ffffff;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05),
  0 10px 15px -3px rgba(0, 0, 0, 0.03);
  box-sizing: border-box;
}

.header {
  margin-bottom: 32px;
  text-align: left;
}

.header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  letter-spacing: -0.5px;
}

.header p {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
}

.form-group {
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
}

.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
}

.form-group input {
  width: 100%;
  box-sizing: border-box;
  height: 44px;
  padding: 0 16px;
  font-size: 14px;
  color: #111827;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  outline: none;
  transition: all 0.2s ease;
}

.form-group input::placeholder {
  color: #9ca3af;
}

.form-group input:focus {
  background-color: #ffffff;
  border-color: #111827;
  box-shadow: 0 0 0 1px #111827;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.input-wrapper input {
  padding-right: 40px;
}

.eye-btn {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  color: #9ca3af;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.eye-btn:hover {
  color: #374151;
}

.eye-btn svg {
  width: 20px;
  height: 20px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 12px;
  background-color: #111827;
  color: #ffffff;
  font-size: 15px;
  font-weight: 500;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.1s ease;
}

.submit-btn:hover:not(:disabled) {
  background-color: #374151;
}

.submit-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.submit-btn:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
}
</style>