<template>
  <div class="profile-page-wrapper">
    <div class="profile-card">
      <div class="profile-header">
        <h2>个人资料设置</h2>
        <p>更新您的账号隐私信息与个人公开档案</p>
      </div>

      <form @submit.prevent="saveProfile" class="profile-form">

        <div class="avatar-upload-section">
          <div class="avatar-preview-box" @click="triggerFileInput">
            <img :src="avatarPreview || 'https://api.dicebear.com/7.x/notionists/svg?seed=Felix'" class="large-avatar" />
            <div class="upload-mask">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="camera-icon">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6.827 6.175A2.31 2.31 0 0 1 5.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 0 0 2.25 2.25h15A2.25 2.25 0 0 0 21.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 0 0-1.134-.175 2.31 2.31 0 0 1-1.64-1.055l-.822-1.316a2.192 2.192 0 0 0-1.736-1.039 48.774 48.774 0 0 0-5.232 0 2.192 2.192 0 0 0-1.736 1.039l-.821 1.316Z" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 12.75a4.5 4.5 0 1 1-9 0 4.5 4.5 0 0 1 9 0ZM18.75 10.5h.008v.008h-.008V10.5Z" />
              </svg>
            </div>
          </div>
          <input type="file" ref="fileInput" @change="handleAvatarChange" accept="image/*" style="display: none" />
          <p class="upload-hint">点击头像上传本地图片（单个文件 10MB 负载以内）</p>
        </div>

        <div class="form-grid">
          <div class="form-column">
            <h3 class="section-title">身份与登录凭证（可编辑）</h3>

            <div class="form-group">
              <label for="username">账号名称</label>
              <input id="username" type="text" v-model="formData.username" required />
            </div>

            <div class="form-group">
              <label for="nickname">个人昵称</label>
              <input id="nickname" type="text" v-model="formData.nickname" placeholder="12个字符以内" maxlength="12" required />
            </div>

            <div class="form-group">
              <label for="password">安全密码</label>
              <div class="input-wrapper">
                <input :type="showPassword ? 'text' : 'password'" id="password" v-model="formData.password" />
                <button type="button" class="eye-btn" @click="showPassword = !showPassword">
                  <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5"><path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" /></svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5"><path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" /><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
                </button>
              </div>
            </div>
          </div>

          <div class="form-column read-only-column">
            <h3 class="section-title">系统账号档案</h3>

            <div class="form-group disabled-group">
              <label>账户累积积分</label>
              <div class="disabled-value-box">
                <span class="points-highlight">{{ formData.points }}</span> Points
              </div>
            </div>

            <div class="form-group disabled-group">
              <label>账号创建时间</label>
              <input type="text" :value="formData.createTime" disabled class="disabled-input" />
            </div>

            <div class="form-group disabled-group">
              <label>资料最后更新</label>
              <input type="text" :value="formData.updateTime" disabled class="disabled-input" />
            </div>
          </div>
        </div>

        <button type="submit" class="save-btn" :disabled="isSaving">
          {{ isSaving ? '正在保存修改...' : '保存更改' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { showToast } from '@/utils/toast'
import { getAvatarUrl } from '@/utils/config'

const fileInput = ref(null)
const showPassword = ref(false)
const isSaving = ref(false)
const selectedFile = ref(null)
const avatarPreview = ref('')

const formData = ref({
  id: '',
  username: '',
  password: '',
  nickname: '',
  avatar: '',
  points: 0,
  createTime: '',
  updateTime: ''
})

let originalFormString = ''

onMounted(async () => {
  fetchUserInfo()
})

const fetchUserInfo = async () => {
  try {
    const res = await request.get('/home')

    if (res.code === 200) {
      const data = res.data.userAndCheckInStatus.user

      formData.value = {
        id: data.id,
        username: data.username,
        password: '',
        nickname: data.nickname,
        avatar: data.avatar,
        points: data.points,
        createTime: data.createTime,
        updateTime: data.updateTime
      }

      if (data.avatar) {
        avatarPreview.value = getAvatarUrl(data.avatar)
      } else {
        avatarPreview.value = getAvatarUrl(null)
      }

      originalFormString = JSON.stringify(formData.value)

      window.dispatchEvent(new CustomEvent('user-info-updated', { detail: data }))
    } else {
      showToast(res.message || '获取用户信息失败', 'error')
    }
  } catch (error) {
    console.error(error)
    showToast('拉取用户信息异常，请检查网络', 'error')
  }
}

const triggerFileInput = () => {
  fileInput.value.click()
}

const handleAvatarChange = (event) => {
  const file = event.target.files[0]
  if (!file) return

  if (file.size > 10 * 1024 * 1024) {
    showToast('上传失败：图片大小不能超过 10MB！', 'error')
    return
  }

  selectedFile.value = file
  avatarPreview.value = URL.createObjectURL(file)
  showToast('头像已载入预览，请点击下方保存按钮生效', 'success')
}

const saveProfile = async () => {
  if (isSaving.value) return

  if (!formData.value.nickname.trim()) {
    showToast('昵称不能为空', 'error')
    return
  }

  const hasFileChange = !!selectedFile.value
  const hasInfoChange = JSON.stringify(formData.value) !== originalFormString

  if (!hasFileChange && !hasInfoChange) {
    showToast('未检测到任何修改', 'error')
    return
  }

  isSaving.value = true
  const reqQueue = []

  if (hasFileChange) {
    const uploadData = new FormData()
    uploadData.append('headimg', selectedFile.value)

    reqQueue.push(
        request.post('/uploadHeadImg', uploadData, {
          headers: {'Content-Type': 'multipart/form-data'}
        })
    )
  }

  if (hasInfoChange) {
    const userPayload = {
      id: formData.value.id,
      username: formData.value.username,
      nickname: formData.value.nickname,
      password: formData.value.password || null
    }
    reqQueue.push(request.post('/updateUser', userPayload))
  }

  try {
    const results = await Promise.all(reqQueue)

    for (const res of results) {
      if (res && res.code !== 200) {
        showToast(res.message || '保存过程中出现错误', 'error')
        return
      }
    }

    showToast('个人资料同步成功！', 'success')

    selectedFile.value = null
    formData.value.password = ''
    originalFormString = JSON.stringify(formData.value)

  } catch (error) {
    console.error("捕获到请求异常:", error)
    showToast(error.response?.data?.message || '服务器响应异常', 'error')
  } finally {
    isSaving.value = false
  }
}
</script>

<style scoped>
.profile-page-wrapper {
  min-height: calc(100vh - 88px);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  box-sizing: border-box;
}

.profile-card {
  width: 100%;
  max-width: 800px;
  background: #ffffff;
  padding: 40px;
  border-radius: 16px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02);
  box-sizing: border-box;
}

.profile-header {
  margin-bottom: 36px;
  text-align: left;
}

.profile-header h2 {
  margin: 0 0 6px 0;
  font-size: 22px;
  font-weight: 600;
  color: #111827;
}

.profile-header p {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
}

.avatar-upload-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 36px;
}

.avatar-preview-box {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid #111827;
  background: #f9fafb;
}

.large-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-preview-box:hover .upload-mask {
  opacity: 1;
}

.camera-icon {
  width: 24px;
  height: 24px;
  color: #ffffff;
}

.upload-hint {
  font-size: 12px;
  color: #9ca3af;
  margin: 12px 0 0 0;
}

.form-grid {
  display: flex;
  gap: 40px;
  text-align: left;
}

.form-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 4px 0;
  border-left: 3px solid #111827;
  padding-left: 8px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
}

.form-group input {
  height: 40px;
  padding: 0 14px;
  font-size: 14px;
  color: #111827;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  outline: none;
}

.form-group input:focus:not(:disabled) {
  border-color: #111827;
  box-shadow: 0 0 0 1px #111827;
  background-color: #ffffff;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-wrapper input {
  width: 100%;
  padding-right: 40px;
}

.eye-btn {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  cursor: pointer;
  color: #9ca3af;
  display: flex;
}

.eye-btn:hover {
  color: #111827;
}

.eye-btn svg {
  width: 20px;
  height: 20px;
}

.read-only-column {
  background-color: #fcfcfc;
  padding: 16px;
  border-radius: 12px;
  border: 1px dashed #e5e7eb;
}

.disabled-value-box {
  height: 40px;
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #4b5563;
  padding-left: 2px;
}

.points-highlight {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin-right: 6px;
}

.disabled-input {
  background-color: #f3f4f6 !important;
  color: #9ca3af !important;
  cursor: not-allowed;
}

.save-btn {
  width: 100%;
  height: 44px;
  background-color: #111827;
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  margin-top: 36px;
  transition: background-color 0.2s;
}

.save-btn:hover {
  background-color: #374151;
}
</style>