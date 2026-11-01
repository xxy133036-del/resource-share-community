<template>
  <div class="page-wrapper">
    <div class="login-card">
      <!-- 角色切换 -->
      <div class="role-toggle">
        <button
          :class="['role-btn', { active: loginRole === 'user' }]"
          @click="switchRole('user')"
        >用户登录</button>
        <button
          :class="['role-btn', { active: loginRole === 'admin' }]"
          @click="switchRole('admin')"
        >管理员登录</button>
      </div>

      <div class="header">
        <h2>{{ loginRole === 'admin' ? '管理员登录' : '欢迎回来' }}</h2>
        <p>{{ loginRole === 'admin' ? '请输入管理员账号密码登录后台' : '请输入您的账号和密码登录系统' }}</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="username">{{ loginRole === 'admin' ? '管理员账号' : '账号' }}</label>
          <input
            id="username"
            type="text"
            v-model="formData.username"
            :placeholder="loginRole === 'admin' ? '输入管理员账号' : '输入您的账号'"
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
              placeholder="输入您的密码"
              required
            />
            <button type="button" class="eye-btn" @click="showPassword = !showPassword">
              <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="eye-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" /></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="eye-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" /><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
            </button>
          </div>
        </div>

        <button type="submit" class="submit-btn" :disabled="isLoading">
          {{ isLoading ? '登录中...' : '登录' }}
        </button>

        <div v-if="loginRole === 'user'" class="footer-action">
          <span class="text-gray">还没有账号？</span>
          <a href="javascript:void(0)" class="link-primary" @click="goToRegister">立即注册</a>
        </div>
        <div v-else class="footer-action">
          <span class="text-gray">返回</span>
          <a href="javascript:void(0)" class="link-primary" @click="switchRole('user')">用户登录</a>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import adminRequest from '@/utils/adminRequest'
import { showToast } from '@/utils/toast'

const router = useRouter()
const showPassword = ref(false)
const isLoading = ref(false)
const loginRole = ref('user')

const formData = ref({
  username: '',
  password: ''
})

const switchRole = (role) => {
  loginRole.value = role
  formData.value = { username: '', password: '' }
  showPassword.value = false
}

const handleLogin = async () => {
  if (isLoading.value) return
  isLoading.value = true

  try {
    const isAdmin = loginRole.value === 'admin'
    const res = isAdmin
      ? await adminRequest.post('/login', { adminName: formData.value.username, password: formData.value.password })
      : await request.post('/login', formData.value)

    if (res.code === 200) {
      showToast('登录成功！', 'success')
      localStorage.setItem('token', res.data)
      localStorage.setItem('role', isAdmin ? 'admin' : 'user')
      // 管理员权限由 AdminLayout 挂载后从 /home 获取并设置，此处不设初始值
      // 以便路由守卫能区分"权限尚未加载"和"确实无权限"两种情况
      router.push(isAdmin ? '/admin/dashboard' : '/sys/home')
    } else {
      showToast(res.message || '账号或密码错误，请重试', 'error')
    }
  } catch (error) {
    console.error('登录异常：', error)
    showToast(error.message || '系统异常，请稍后再试', 'error')
  } finally {
    isLoading.value = false
  }
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
/* ===== 角色切换 Toggle ===== */
.role-toggle {
  display: flex;
  background: #f3f4f6;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 32px;
}

.role-btn {
  flex: 1;
  padding: 10px 0;
  font-size: 14px;
  font-weight: 500;
  border: none;
  background: transparent;
  color: #6b7280;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.role-btn.active {
  background: #ffffff;
  color: #111827;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  font-weight: 600;
}

.role-btn:hover:not(.active) { color: #374151; }

/* ===== 基础布局 ===== */
.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.input-wrapper input {
  width: 100%;
  padding-right: 40px;
  box-sizing: border-box;
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
.eye-btn:hover { color: #374151; }
.eye-icon { width: 20px; height: 20px; }

.page-wrapper {
  min-height: 100vh;
  width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f6fa;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  box-sizing: border-box;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: #ffffff;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04), 0 1px 4px rgba(0, 0, 0, 0.03);
  box-sizing: border-box;
}

.header {
  margin-bottom: 28px;
  text-align: left;
}

.header h2 {
  margin: 0 0 6px 0;
  font-size: 24px;
  font-weight: 700;
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
  margin-bottom: 18px;
}

.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}

.form-group input {
  height: 46px;
  padding: 0 16px;
  font-size: 14px;
  color: #111827;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
  width: 100%;
}

.form-group input::placeholder { color: #9ca3af; }

.form-group input:focus {
  background-color: #ffffff;
  border-color: #111827;
  box-shadow: 0 0 0 3px rgba(17, 24, 39, 0.06);
}

.submit-btn {
  width: 100%;
  height: 48px;
  margin-top: 8px;
  background-color: #111827;
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.submit-btn:hover:not(:disabled) {
  background-color: #1f2937;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(17, 24, 39, 0.2);
}

.submit-btn:active:not(:disabled) { transform: scale(0.98); }

.submit-btn:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.footer-action {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
}

.text-gray { color: #6b7280; }

.link-primary {
  color: #111827;
  font-weight: 600;
  text-decoration: none;
  margin-left: 4px;
  transition: opacity 0.2s;
}

.link-primary:hover { opacity: 0.7; }
</style>
