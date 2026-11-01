<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-brand" @click="$router.push('/admin/dashboard')">
        <div class="brand-icon"></div>
        <div class="brand-text">
          <span class="brand-title">{{ adminRoleName }}</span>
          <span class="brand-sub">管理后台</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <!-- 首页：所有人可见 -->
        <router-link to="/admin/dashboard" class="nav-item" active-class="nav-active">
          <span class="nav-icon">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6A2.25 2.25 0 016 3.75h2.25A2.25 2.25 0 0110.5 6v2.25a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25V6zM3.75 15.75A2.25 2.25 0 016 13.5h2.25a2.25 2.25 0 012.25 2.25V18a2.25 2.25 0 01-2.25 2.25H6A2.25 2.25 0 013.75 18v-2.25zM13.5 6a2.25 2.25 0 012.25-2.25H18A2.25 2.25 0 0120.25 6v2.25A2.25 2.25 0 0118 10.5h-2.25a2.25 2.25 0 01-2.25-2.25V6zM13.5 15.75a2.25 2.25 0 012.25-2.25H18a2.25 2.25 0 012.25 2.25V18A2.25 2.25 0 0118 20.25h-2.25A2.25 2.25 0 0113.5 18v-2.25z" /></svg>
          </span>
          <span class="nav-label">首页</span>
        </router-link>

        <!-- 资源审核：需要 资源管理 权限 -->
        <router-link v-if="hasPrivilege('资源管理')" to="/admin/resources" class="nav-item" active-class="nav-active">
          <span class="nav-icon">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" /></svg>
          </span>
          <span class="nav-label">资源审核</span>
        </router-link>

        <!-- 用户管理：需要 用户管理 权限 -->
        <router-link v-if="hasPrivilege('用户管理')" to="/admin/users" class="nav-item" active-class="nav-active">
          <span class="nav-icon">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" /></svg>
          </span>
          <span class="nav-label">用户管理</span>
        </router-link>

        <!-- 公告管理：需要 公告管理 权限 -->
        <router-link v-if="hasPrivilege('公告管理')" to="/admin/notices" class="nav-item" active-class="nav-active">
          <span class="nav-icon">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M10.34 15.84c-.688-.06-1.386-.09-2.09-.09H7.5a4.5 4.5 0 110-9h.75c.704 0 1.402-.03 2.09-.09m0 9.18c.253.962.584 1.892.985 2.783.247.55.06 1.21-.463 1.511l-.657.38a.546.546 0 01-.498-.016l-.262-.152a3.03 3.03 0 01-1.29-3.096m.178-5.466c.876-.468 1.687-.905 2.562-1.081M12 8.25V6.75m0 0V4.5m0 2.25c0 .414.336.75.75.75h.008a.75.75 0 00.75-.75V4.5a.75.75 0 00-.75-.75h-.008a.75.75 0 00-.75.75z" /></svg>
          </span>
          <span class="nav-label">公告管理</span>
        </router-link>

        <!-- 管理员管理：需要 管理员管理 权限 -->
        <router-link v-if="hasPrivilege('管理员管理')" to="/admin/admins" class="nav-item" active-class="nav-active">
          <span class="nav-icon">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" /></svg>
          </span>
          <span class="nav-label">管理员</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="admin-info">
          <div class="admin-avatar">{{ adminInitial }}</div>
          <div class="admin-name">{{ adminName }}</div>
        </div>
        <button class="logout-btn" @click="handleLogout" title="退出登录">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9" /></svg>
        </button>
      </div>
    </aside>

    <!-- 内容区 -->
    <main class="main-content">
      <div class="content-wrapper">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import adminRequest from '@/utils/adminRequest'
import { showToast } from '@/utils/toast'

const router = useRouter()
const adminName = ref('管理员')
const adminInitial = ref('A')
const adminRoleName = ref('管理后台')
const privileges = ref([])

/** 判断当前管理员是否有指定权限 */
const hasPrivilege = (name) => {
  return privileges.value.includes(name)
}

onMounted(async () => {
  try {
    const res = await adminRequest.get('/home')
    if (res.code === 200) {
      const data = res.data?.adminAndPrivileges
      if (data?.admin) {
        const name = data.admin.adminName || '管理员'
        adminName.value = name
        adminInitial.value = name.charAt(0).toUpperCase()
        adminRoleName.value = data.admin.roleName || '管理后台'
      }
      // 存储权限列表（侧边栏 + 路由守卫共用）
      const privs = data?.privileges || []
      privileges.value = privs
      localStorage.setItem('adminPrivileges', JSON.stringify(privs))
    }
  } catch (e) {
    console.error('获取管理员信息失败', e)
  }
})

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('adminPrivileges')
  showToast('已退出管理后台', 'success')
  setTimeout(() => router.push('/login'), 200)
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #f5f6fa;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
}

/* ===== 侧边栏 ===== */
.sidebar {
  width: 220px;
  min-width: 220px;
  background: #111827;
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  overflow-y: auto;
}

.sidebar-brand {
  padding: 28px 20px 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.brand-icon {
  width: 32px;
  height: 32px;
  background: #ffffff;
  border-radius: 8px;
  flex-shrink: 0;
}

.brand-text { display: flex; flex-direction: column; }

.brand-title {
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: -0.5px;
}

.brand-sub {
  font-size: 10px;
  color: rgba(255,255,255,0.4);
  font-weight: 500;
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-top: 1px;
}

/* 导航菜单 */
.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  color: rgba(255,255,255,0.55);
  text-decoration: none;
  transition: all 0.2s ease;
  position: relative;
}

.nav-item:hover {
  color: #ffffff;
  background: rgba(255,255,255,0.06);
}

.nav-active {
  color: #ffffff !important;
  background: rgba(255,255,255,0.1) !important;
}

.nav-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  background: #ffffff;
  border-radius: 0 3px 3px 0;
}

.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-icon svg { width: 18px; height: 18px; }

/* 侧边栏底部 */
.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255,255,255,0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.admin-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(255,255,255,0.15);
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.admin-name {
  font-size: 13px;
  color: rgba(255,255,255,0.7);
  font-weight: 500;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: transparent;
  border: none;
  color: rgba(255,255,255,0.4);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}

.logout-btn svg { width: 16px; height: 16px; }

/* ===== 内容区 ===== */
.main-content {
  flex: 1;
  margin-left: 220px;
  min-height: 100vh;
}

.content-wrapper {
  padding: 32px;
  max-width: 1400px;
}
</style>
