<template>
  <div class="layout-wrapper">
    <nav class="navbar">
      <div class="nav-content">
        <div class="brand" @click="$router.push('/sys/home')" style="cursor: pointer;">
          <span class="logo-mark"></span>
          <span class="brand-name">Nexus</span>
        </div>

        <div
            class="user-menu-container"
            @mouseenter="showDropdown = true"
            @mouseleave="showDropdown = false"
        >
          <div class="avatar-container">
            <img :src="displayAvatar" class="avatar-img" />
          </div>

          <Transition name="dropdown-fade">
            <div v-if="showDropdown" class="dropdown-menu">
              <div class="dropdown-header">
                <p class="user-nickname">{{ userInfo.nickname || '未设置昵称' }}</p>
                <p class="user-username">@{{ userInfo.username }}</p>
              </div>
              <hr class="divider" />

              <div class="dropdown-item" @click="goTo('/sys/profile')">
                <span>个人中心</span>
              </div>
              <div class="dropdown-item" @click="goTo('/sys/messages')">
                <span>我的消息</span>
                <span class="msg-badge" v-if="unreadMessageCount > 0">{{ unreadMessageCount > 99 ? '99+' : unreadMessageCount }}</span>
              </div>
              <div class="dropdown-item" @click="goTo('/sys/ai-service')">
                <span>AI 客服</span>
              </div>
              <div class="dropdown-item" @click="goTo('/sys/my-posts')">
                <span>我的发布</span>
              </div>
              <div class="dropdown-item" @click="goTo('/sys/my-favorites')">
                <span>我的收藏</span>
              </div>

              <hr class="divider" />

              <div class="dropdown-action">
                <button
                    class="checkin-btn"
                    :disabled="isCheckedIn"
                    @click="handleCheckIn"
                    :class="{ 'checked': isCheckedIn }"
                >
                  {{ isCheckedIn ? '已签到' : '每日签到' }}
                </button>
              </div>

              <hr class="divider" />

              <div class="dropdown-item logout-item" @click="handleLogout">
                <span>退出登录</span>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </nav>

    <main class="page-content">
      <router-view></router-view>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { showToast } from '@/utils/toast'
import { getAvatarUrl } from '@/utils/config'

const router = useRouter()
const showDropdown = ref(false)
const isCheckedIn = ref(false)
const unreadMessageCount = ref(0)

const userInfo = ref({
  username: '加载中...',
  nickname: '',
  avatar: ''
})

const displayAvatar = computed(() => {
  return getAvatarUrl(userInfo.value.avatar)
})

const updateUserInfoHandler = (event) => {
  const userData = event.detail
  userInfo.value = {
    username: userData.username,
    nickname: userData.nickname,
    avatar: userData.avatar
  }
  if (userData.hasCheckedIn !== undefined) {
    isCheckedIn.value = userData.hasCheckedIn
  }
}

// 在 setup 阶段注册监听器，确保子组件 mount 时能接收到事件
window.addEventListener('user-info-updated', updateUserInfoHandler)

onMounted(() => {
  // 首页由 Home.vue 的 /home 请求统一返回用户信息 + 文件列表，通过 user-info-updated 事件传递
  // 非首页（如 /sys/profile）不会触发该事件，这里设置兜底定时器
  if (window.__homePageActive) {
    // 首页已接管，不再重复请求；重置标记
    window.__homePageActive = false
    // 首页下 Home.vue 也会调 /home，这里只拉未读消息数
    fetchUnreadCount()
  } else {
    // 非首页：直接请求用户信息 + 未读数
    fetchUserStatus()
    fetchUnreadCount()
  }

  // 供子页面（MyMessage.vue）在标记已读后主动刷新顶部未读徽标
  window.__refreshUnreadCount = fetchUnreadCount
})

onUnmounted(() => {
  window.removeEventListener('user-info-updated', updateUserInfoHandler)
  window.__refreshUnreadCount = null
})

const fetchUserStatus = async () => {
  try {
    const res = await request.get('/home')
    if (res.code === 200) {
      userInfo.value = res.data.userAndCheckInStatus.user
      isCheckedIn.value = res.data.userAndCheckInStatus.hasCheckedIn || false
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

const fetchUnreadCount = async () => {
  try {
    // 从摘要接口里汇总所有类型的未读数
    const res = await request.get('/messageSummary')
    if (res.code === 200) {
      const list = res.data || []
      let total = 0
      list.forEach(item => { total += (item.unreadCount || 0) })
      unreadMessageCount.value = total
    }
  } catch (error) {
    // 消息接口可能暂未就绪，静默处理
  }
}

const goTo = (path) => {
  showDropdown.value = false
  router.push(path)
}

const handleCheckIn = async () => {
  if (isCheckedIn.value) return

  try {
    const res = await request.post('/dailyCheck')

    if (res.code === 200) {
      isCheckedIn.value = true
      showToast(res.data || '签到成功！', 'success')
      setTimeout(() => {
        fetchUserStatus()
      }, 300)
    } else {
      showToast(res.message || '签到异常', 'error')
      if (res.message && res.message.includes('今天')) {
        isCheckedIn.value = true
      }
    }
  } catch (error) {
    console.error('签到失败:', error)
    showToast(error.response?.data?.message || '签到失败，请检查网络', 'error')
  }
}

const handleLogout = () => {
  localStorage.removeItem('token')

  showToast('已成功退出登录', 'success')

  setTimeout(() => {
    router.push('/login')
  }, 300)
}
</script>

<style scoped>
.layout-wrapper { min-height: 100vh; background-color: #fafafa; }
.navbar { position: sticky; top: 0; z-index: 50; background-color: rgba(255, 255, 255, 0.8); backdrop-filter: blur(12px); border-bottom: 1px solid rgba(229, 231, 235, 0.5); height: 64px; }
.nav-content { max-width: 1200px; margin: 0 auto; height: 100%; padding: 0 24px; display: flex; align-items: center; justify-content: space-between; }
.brand { display: flex; align-items: center; gap: 12px; font-weight: bold; }
.logo-mark { width: 24px; height: 24px; background: #111827; border-radius: 6px; }
.page-content { padding-top: 24px; max-width: 1200px; margin: 0 auto; padding-left: 24px; padding-right: 24px; box-sizing: border-box;}

.user-menu-container {
  position: relative;
  padding: 10px 0;
}
.avatar-container { width: 36px; height: 36px; border-radius: 50%; overflow: hidden; cursor: pointer; border: 1px solid #e5e7eb; transition: transform 0.2s; }
.avatar-container:hover { transform: scale(1.05); }
.avatar-img { width: 100%; height: 100%; background: #f9fafb; object-fit: cover; }

.dropdown-menu {
  position: absolute;
  top: 52px;
  right: 0;
  width: 220px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 10px 30px -5px rgba(0, 0, 0, 0.08), 0 4px 12px -4px rgba(0, 0, 0, 0.04);
  border: 1px solid #e5e7eb;
  padding: 12px 4px;
  display: flex;
  flex-direction: column;
}

.dropdown-header { padding: 8px 16px; text-align: left;}
.user-nickname { margin: 0; font-size: 14px; font-weight: 600; color: #111827; }
.user-username { margin: 2px 0 0 0; font-size: 12px; color: #9ca3af; }

.divider { border: 0; border-top: 1px solid #f3f4f6; margin: 8px 0; }

.dropdown-item {
  padding: 10px 16px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: background-color 0.2s, color 0.2s;
  text-align: left;
}
.dropdown-item:hover { background-color: #f9fafb; color: #111827; }

.logout-item {
  color: #ef4444 !important;
}
.logout-item:hover {
  background-color: #fef2f2 !important;
  color: #dc2626 !important;
}

.badge { width: 6px; height: 6px; background-color: #ef4444; border-radius: 50%; }
.msg-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background-color: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.dropdown-action { padding: 4px 12px; }
.checkin-btn {
  width: 100%;
  height: 36px;
  background-color: #111827;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.checkin-btn:hover:not(:disabled) { background-color: #374151; }
.checkin-btn:disabled.checked { background-color: #f3f4f6; color: #9ca3af; cursor: not-allowed; border: 1px solid #e5e7eb; }

.dropdown-fade-enter-active, .dropdown-fade-leave-active { transition: all 0.2s ease; }
.dropdown-fade-enter-from, .dropdown-fade-leave-to { opacity: 0; transform: translateY(10px); }
</style>