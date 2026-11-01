<template>
  <div class="dashboard">
    <div class="page-top">
      <div>
        <h1 class="page-title">首页</h1>
        <p class="page-desc">欢迎回来，{{ adminInfo.adminName || '管理员' }}</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon-box" style="background: #eff6ff;">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="#3b82f6" class="stat-svg"><path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" /></svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.totalResources }}</span>
          <span class="stat-label">资源总数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-box" style="background: #fef3c7;">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="#f59e0b" class="stat-svg"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.pendingReviews }}</span>
          <span class="stat-label">待审核资源</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-box" style="background: #f0fdf4;">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="#22c55e" class="stat-svg"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" /></svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.totalUsers }}</span>
          <span class="stat-label">用户总数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-box" style="background: #f5f3ff;">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="#8b5cf6" class="stat-svg"><path stroke-linecap="round" stroke-linejoin="round" d="M10.34 15.84c-.688-.06-1.386-.09-2.09-.09H7.5a4.5 4.5 0 110-9h.75c.704 0 1.402-.03 2.09-.09m0 9.18c.253.962.584 1.892.985 2.783.247.55.06 1.21-.463 1.511l-.657.38a.546.546 0 01-.498-.016l-.262-.152a3.03 3.03 0 01-1.29-3.096m.178-5.466c.876-.468 1.687-.905 2.562-1.081M12 8.25V6.75m0 0V4.5m0 2.25c0 .414.336.75.75.75h.008a.75.75 0 00.75-.75V4.5a.75.75 0 00-.75-.75h-.008a.75.75 0 00-.75.75z" /></svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.totalNotices }}</span>
          <span class="stat-label">公告总数</span>
        </div>
      </div>
    </div>

    <!-- 管理员信息 -->
    <div class="info-card">
      <div class="info-card-header">
        <h3 class="card-title">管理员信息</h3>
        <button class="edit-btn" @click="showEditForm = !showEditForm">{{ showEditForm ? '收起编辑' : '修改账号密码' }}</button>
      </div>

      <!-- 查看态 -->
      <div class="info-grid" v-if="!showEditForm">
        <div class="info-item">
          <span class="info-label">账号</span>
          <span class="info-value">{{ adminInfo.adminName || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">角色</span>
          <span class="info-value">{{ adminInfo.roleName || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">权限</span>
          <div class="privilege-tags">
            <span v-for="p in adminInfo.privileges" :key="p" class="privilege-tag">{{ p }}</span>
            <span v-if="!adminInfo.privileges || adminInfo.privileges.length === 0" class="info-value">-</span>
          </div>
        </div>
      </div>

      <!-- 编辑态：修改账号密码 -->
      <form v-else class="edit-form" @submit.prevent="handleUpdateSelf">
        <div class="form-group">
          <label>新账号名</label>
          <input type="text" v-model="editForm.adminName" :placeholder="adminInfo.adminName" />
        </div>
        <div class="form-group">
          <label>新密码 <span class="req">*</span></label>
          <input type="password" v-model="editForm.password" placeholder="输入新密码（至少6位）" />
        </div>
        <div class="edit-actions">
          <button type="button" class="btn-cancel" @click="showEditForm = false">取消</button>
          <button type="submit" class="btn-save" :disabled="updating">{{ updating ? '保存中...' : '保存修改' }}</button>
        </div>
      </form>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-actions">
      <h3 class="card-title">快捷操作</h3>
      <div class="actions-grid">
        <button class="action-card" @click="$router.push('/admin/resources')" v-if="hasPrivilege('资源管理')">
          <span class="action-icon">📁</span>
          <span class="action-label">资源审核</span>
        </button>
        <button class="action-card" @click="$router.push('/admin/users')" v-if="hasPrivilege('用户管理')">
          <span class="action-icon">👥</span>
          <span class="action-label">用户管理</span>
        </button>
        <button class="action-card" @click="$router.push('/admin/notices')" v-if="hasPrivilege('公告管理')">
          <span class="action-icon">📢</span>
          <span class="action-label">发布公告</span>
        </button>
        <button class="action-card" @click="$router.push('/admin/admins')" v-if="hasPrivilege('管理员管理')">
          <span class="action-icon">🔑</span>
          <span class="action-label">管理员</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminRequest from '@/utils/adminRequest'
import { showToast } from '@/utils/toast'

const adminInfo = ref({ adminName: '', roleName: '', privileges: [] })
const stats = ref({ totalResources: 0, totalUsers: 0, pendingReviews: 0, totalNotices: 0 })
const privileges = ref([])

// 编辑自己账号密码
const showEditForm = ref(false)
const updating = ref(false)
const editForm = ref({ adminName: '', password: '' })

const handleUpdateSelf = async () => {
  if (!editForm.value.password) {
    showToast('请输入新密码', 'error')
    return
  }
  updating.value = true
  try {
    const res = await adminRequest.post('/updateUser', {
      adminName: editForm.value.adminName || adminInfo.value.adminName,
      password: editForm.value.password
    })
    if (res.code === 200) {
      showToast('信息更新成功', 'success')
      if (editForm.value.adminName) adminInfo.value.adminName = editForm.value.adminName
      editForm.value = { adminName: '', password: '' }
      showEditForm.value = false
    } else {
      showToast(res.message || '更新失败', 'error')
    }
  } catch (e) {
    console.error('更新管理员信息失败:', e)
    showToast('操作异常', 'error')
  } finally {
    updating.value = false
  }
}

/** 判断当前管理员是否有指定权限 */
const hasPrivilege = (name) => privileges.value.includes(name)

onMounted(async () => {
  try {
    const res = await adminRequest.get('/home')
    if (res.code === 200) {
      const data = res.data?.adminAndPrivileges
      // 存储权限列表（快捷操作按钮 + 路由守卫复用）
      const privs = data?.privileges || []
      privileges.value = privs
      localStorage.setItem('adminPrivileges', JSON.stringify(privs))
      if (data?.admin) {
        adminInfo.value = {
          adminName: data.admin.adminName || '',
          roleName: data.admin.roleName || '',
          privileges: privs
        }
      }
    }
  } catch (e) {
    console.error('加载仪表盘失败', e)
    showToast('获取管理员信息失败，请确认后端服务已启动', 'error')
  }

  // 加载统计数据
  try {
    const [resRes, noticeRes, userRes] = await Promise.all([
      adminRequest.get('/resourcesList', { params: { pageNum: 1, pageSize: 1 } }),
      adminRequest.get('/queryNoticeList', { params: { pageNum: 1, pageSize: 1 } }),
      adminRequest.get('/getUserListByAdditions', { params: { pageNum: 1, pageSize: 1 } })
    ])
    if (resRes.code === 200) stats.value.totalResources = resRes.data?.total || 0
    if (noticeRes.code === 200) stats.value.totalNotices = noticeRes.data?.total || 0
    if (userRes.code === 200) stats.value.totalUsers = userRes.data?.total || 0
  } catch (e) { console.error('加载统计数据失败', e) }

  // 待审核数 = status=0
  try {
    const pendingRes = await adminRequest.get('/resourcesList', { params: { status: 0, pageNum: 1, pageSize: 1 } })
    if (pendingRes.code === 200) stats.value.pendingReviews = pendingRes.data?.total || 0
  } catch (e) {}
})
</script>

<style scoped>
.dashboard { max-width: 1200px; }

.page-top { margin-bottom: 32px; }
.page-title { margin: 0; font-size: 28px; font-weight: 700; color: #111827; letter-spacing: -0.5px; }
.page-desc { margin: 6px 0 0; font-size: 14px; color: #6b7280; }

/* 统计卡片 */
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 28px; }
.stat-card {
  background: #ffffff; border-radius: 14px; padding: 24px; display: flex; align-items: center; gap: 16px;
  border: 1px solid rgba(229,231,235,0.5); box-shadow: 0 1px 3px rgba(0,0,0,0.02);
  transition: box-shadow 0.2s;
}
.stat-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.stat-icon-box { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-svg { width: 24px; height: 24px; }
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 28px; font-weight: 700; color: #111827; letter-spacing: -0.5px; }
.stat-label { font-size: 13px; color: #6b7280; margin-top: 2px; }

/* 信息卡片 */
.info-card, .quick-actions {
  background: #ffffff; border-radius: 14px; padding: 28px;
  border: 1px solid rgba(229,231,235,0.5); margin-bottom: 24px;
}
.info-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.card-title { margin: 0; font-size: 17px; font-weight: 600; color: #111827; }
.edit-btn {
  padding: 8px 16px; background: #f3f4f6; border: 1px solid #e5e7eb; border-radius: 8px;
  font-size: 13px; color: #374151; cursor: pointer; transition: all 0.2s; font-family: inherit;
}
.edit-btn:hover { background: #e5e7eb; }
.info-grid { display: flex; flex-direction: column; gap: 16px; }
.info-item { display: flex; align-items: flex-start; gap: 16px; }
.info-label { min-width: 50px; font-size: 13px; color: #9ca3af; font-weight: 500; padding-top: 2px; }
.info-value { font-size: 14px; color: #374151; font-weight: 500; }
.privilege-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.privilege-tag {
  padding: 3px 10px; background: #f3f4f6; border-radius: 14px; font-size: 12px; color: #4b5563; font-weight: 500;
}

/* 编辑表单 */
.edit-form { display: flex; flex-direction: column; gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 8px; }
.form-group label { font-size: 14px; font-weight: 500; color: #374151; }
.req { color: #ef4444; }
.form-group input {
  padding: 10px 14px; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 14px;
  font-family: inherit; outline: none; background: #f9fafb; box-sizing: border-box;
}
.form-group input:focus { border-color: #111827; background: #fff; box-shadow: 0 0 0 3px rgba(17,24,39,0.06); }
.edit-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 4px; }
.btn-cancel {
  padding: 10px 20px; background: transparent; border: 1px solid #d1d5db; border-radius: 8px;
  font-size: 14px; cursor: pointer; color: #374151; font-family: inherit; transition: all 0.2s;
}
.btn-cancel:hover { background: #f3f4f6; }
.btn-save {
  padding: 10px 20px; background: #111827; color: #fff; border: none; border-radius: 8px;
  font-size: 14px; font-weight: 500; cursor: pointer; font-family: inherit; transition: all 0.2s;
}
.btn-save:hover { background: #1f2937; }
.btn-save:disabled { background: #9ca3af; cursor: not-allowed; }

/* 快捷操作 */
.actions-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.action-card {
  display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 24px 16px;
  background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 12px; cursor: pointer;
  transition: all 0.2s; font-family: inherit;
}
.action-card:hover { background: #f3f4f6; border-color: #d1d5db; transform: translateY(-2px); box-shadow: 0 4px 8px rgba(0,0,0,0.04); }
.action-icon { font-size: 28px; }
.action-label { font-size: 14px; font-weight: 500; color: #374151; }

@media (max-width: 900px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .actions-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
