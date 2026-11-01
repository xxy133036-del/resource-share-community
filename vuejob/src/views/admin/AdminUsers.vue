<template>
  <div class="page">
    <div class="page-top">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-desc">管理平台注册用户，冻结/解冻账号，查看用户资源</p>
      </div>
    </div>

    <!-- 搜索 & 筛选 -->
    <div class="filter-bar">
      <div class="search-wrap">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="search-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" /></svg>
        <input type="text" v-model="keyword" placeholder="搜索用户名或昵称..." @keyup.enter="doSearch" />
      </div>
      <select v-model="statusFilter" @change="doSearch" class="status-select">
        <option :value="null">全部状态</option>
        <option :value="1">正常</option>
        <option :value="0">冻结</option>
      </select>
      <input type="date" v-model="startDate" class="date-input" @change="doSearch" />
      <span class="date-sep">至</span>
      <input type="date" v-model="endDate" class="date-input" @change="doSearch" />
      <button class="search-btn" @click="doSearch">搜索</button>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <!-- 加载中 -->
      <div v-if="isLoading" class="loading-state">
        <div class="loading-spinner-large"></div>
        <p>正在加载用户数据...</p>
      </div>

      <!-- 用户列表 -->
      <template v-else>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>昵称</th>
              <th>积分</th>
              <th>状态</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in userList" :key="user.id">
              <td class="id-cell">{{ user.id }}</td>
              <td class="name-cell">{{ user.username }}</td>
              <td>{{ user.nickname || '-' }}</td>
              <td>{{ user.points || 0 }}</td>
              <td><span :class="['status-badge', user.status === 1 ? 'active' : 'frozen']">{{ user.status === 1 ? '正常' : '冻结' }}</span></td>
              <td class="time-cell">{{ user.createTime }}</td>
              <td class="action-cell">
                <button class="btn-view" @click="openDetail(user)">详情</button>
                <button v-if="user.status === 1" class="btn-reject" @click="toggleStatus(user, 0)">冻结</button>
                <button v-else class="btn-approve" @click="toggleStatus(user, 1)">解冻</button>
              </td>
            </tr>
            <tr v-if="userList.length === 0 && !isLoading">
              <td colspan="7" class="empty-row">暂无用户数据</td>
            </tr>
          </tbody>
        </table>

        <!-- 分页 -->
        <div v-if="totalPages > 1" class="pagination">
          <button :disabled="pageNum === 1" @click="goPage(pageNum - 1)">上一页</button>
          <span>{{ pageNum }} / {{ totalPages }}</span>
          <button :disabled="pageNum >= totalPages" @click="goPage(pageNum + 1)">下一页</button>
        </div>
      </template>
    </div>

    <!-- 详情弹窗 -->
    <Transition name="modal">
      <div v-if="showDetail" class="modal-overlay" @click.self="showDetail = false">
        <div class="modal-card">
          <div class="modal-header">
            <h3>用户详情</h3>
            <button class="modal-close" @click="showDetail = false">×</button>
          </div>
          <div class="detail-grid">
            <div class="detail-item"><span class="dl">ID</span><span class="dv">{{ detailUser.id }}</span></div>
            <div class="detail-item"><span class="dl">用户名</span><span class="dv">{{ detailUser.username }}</span></div>
            <div class="detail-item"><span class="dl">昵称</span><span class="dv">{{ detailUser.nickname || '-' }}</span></div>
            <div class="detail-item"><span class="dl">积分</span><span class="dv">{{ detailUser.points || 0 }}</span></div>
            <div class="detail-item"><span class="dl">状态</span><span class="dv">{{ detailUser.status === 1 ? '正常' : '冻结' }}</span></div>
            <div class="detail-item"><span class="dl">注册时间</span><span class="dv">{{ detailUser.createTime }}</span></div>
          </div>

          <h4 class="sub-title">发布的资源</h4>
          <div v-if="userFiles.length === 0" class="no-files">暂无发布</div>
          <div v-else class="file-list">
            <div v-for="f in userFiles" :key="f.id" class="file-row">
              <span class="f-name">{{ f.fileName }}</span>
              <span :class="['status-badge', statusClass(f.status)]">{{ statusText(f.status) }}</span>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
/**
 * 用户管理
 * API:
 * - /getUserListByAdditions?keyword=&status=&startDate=&endDate=&pageNum=&pageSize=  用户分页查询（含筛选）
 * - /updateUserStatusById?status=&userId=   冻结/解冻
 * - /getUserById?userId=                    查询单个用户
 * - /getUserFilesByUserId?userId=&pageNum=&pageSize=  用户发布的资源
 */
import { ref, onMounted } from 'vue'
import adminRequest from '@/utils/adminRequest'
import { showToast } from '@/utils/toast'

const keyword = ref('')
const statusFilter = ref(null)
const startDate = ref('')
const endDate = ref('')
const userList = ref([])
const isLoading = ref(false)
const pageNum = ref(1)
const totalPages = ref(1)

// 详情弹窗
const showDetail = ref(false)
const detailUser = ref({})
const userFiles = ref([])

onMounted(() => {
  fetchList()
})

const fetchList = async () => {
  isLoading.value = true
  try {
    const res = await adminRequest.get('/getUserListByAdditions', {
      params: {
        keyword: keyword.value || undefined,
        status: statusFilter.value ?? undefined,
        startDate: startDate.value || undefined,
        endDate: endDate.value || undefined,
        pageNum: pageNum.value,
        pageSize: 10
      }
    })
    if (res.code === 200) {
      const pageData = res.data
      userList.value = pageData?.records || []
      totalPages.value = pageData?.pages || 1
    } else {
      console.error('获取用户列表返回异常:', res)
      showToast(res.message || '获取用户列表失败', 'error')
    }
  } catch (e) {
    console.error('获取用户列表失败：', e)
    showToast('加载用户列表失败', 'error')
  } finally {
    isLoading.value = false
  }
}

const doSearch = () => { pageNum.value = 1; fetchList() }
const goPage = (p) => { pageNum.value = p; fetchList() }

const toggleStatus = async (user, newStatus) => {
  try {
    const res = await adminRequest.put('/updateUserStatusById', null, { params: { userId: user.id, status: newStatus } })
    if (res.code === 200) {
      user.status = newStatus
      showToast(newStatus === 1 ? '已解冻' : '已冻结', 'success')
    } else {
      console.error('更新用户状态返回异常:', res)
      showToast(res.message || '操作失败', 'error')
    }
  } catch (e) {
    console.error('更新用户状态异常:', e)
    showToast('操作异常，请打开浏览器控制台查看详情', 'error')
  }
}

const openDetail = async (user) => {
  detailUser.value = user
  showDetail.value = true
  userFiles.value = []
  try {
    const res = await adminRequest.get('/getUserFilesByUserId', { params: { userId: user.id, pageNum: 1, pageSize: 10 } })
    if (res.code === 200) {
      userFiles.value = res.data?.records || []
    } else {
      console.error('获取用户资源返回异常:', res)
    }
  } catch (e) {
    console.error('获取用户资源失败', e)
  }
}

const statusClass = (s) => s === 2 ? 'approved' : s === 1 ? 'rejected' : 'pending'
const statusText = (s) => s === 2 ? '已通过' : s === 1 ? '已下架' : '待审核'
</script>

<style scoped>
.page { max-width: 1200px; }
.page-top { margin-bottom: 28px; }
.page-title { margin: 0; font-size: 28px; font-weight: 700; color: #111827; letter-spacing: -0.5px; }
.page-desc { margin: 6px 0 0; font-size: 14px; color: #6b7280; }

/* 筛选 */
.filter-bar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.search-wrap { flex: 1; display: flex; align-items: center; background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 0 14px; }
.search-wrap:focus-within { border-color: #111827; box-shadow: 0 0 0 3px rgba(17,24,39,0.06); }
.search-icon { width: 18px; height: 18px; color: #9ca3af; flex-shrink: 0; }
.search-wrap input { flex: 1; border: none; padding: 12px 10px; font-size: 14px; outline: none; background: transparent; }
.status-select { padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 14px; color: #374151; background: #fff; outline: none; cursor: pointer; }
.status-select:focus { border-color: #111827; }
.date-input { padding: 12px 10px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 13px; outline: none; color: #374151; }
.date-input:focus { border-color: #111827; }
.date-sep { font-size: 13px; color: #9ca3af; }
.search-btn { padding: 12px 24px; background: #111827; color: #fff; border: none; border-radius: 10px; font-size: 14px; font-weight: 500; cursor: pointer; transition: background 0.2s; font-family: inherit; }
.search-btn:hover { background: #1f2937; }

/* 加载 */
.loading-state { text-align: center; padding: 60px 24px; color: #9ca3af; display: flex; flex-direction: column; align-items: center; gap: 16px; }
.loading-spinner-large { width: 36px; height: 36px; border: 3px solid #e5e7eb; border-top-color: #111827; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }


/* 表格 */
.table-card { background: #fff; border-radius: 14px; border: 1px solid rgba(229,231,235,0.5); overflow: hidden; }
table { width: 100%; border-collapse: collapse; }
thead th { padding: 14px 20px; font-size: 12px; font-weight: 600; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid #f3f4f6; text-align: left; background: #fafafa; }
tbody td { padding: 16px 20px; font-size: 14px; color: #374151; border-bottom: 1px solid #f9fafb; }
tbody tr:hover { background: #fafbfc; }
.id-cell { font-family: monospace; color: #9ca3af; }
.name-cell { font-weight: 600; color: #111827; }
.time-cell { white-space: nowrap; font-size: 13px; color: #6b7280; }
.status-badge { display: inline-block; padding: 4px 12px; border-radius: 14px; font-size: 12px; font-weight: 600; }
.status-badge.active { background: #dcfce7; color: #166534; }
.status-badge.frozen { background: #fef2f2; color: #991b1b; }
.status-badge.approved { background: #dcfce7; color: #166534; }
.status-badge.pending { background: #fef3c7; color: #92400e; }
.status-badge.rejected { background: #f3f4f6; color: #6b7280; }
.action-cell { display: flex; gap: 8px; }
.btn-view, .btn-approve, .btn-reject {
  padding: 6px 14px; border-radius: 8px; font-size: 12px; font-weight: 500; border: none; cursor: pointer; transition: all 0.15s; font-family: inherit;
}
.btn-view { background: #f3f4f6; color: #374151; }
.btn-view:hover { background: #e5e7eb; }
.btn-approve { background: #dcfce7; color: #166534; }
.btn-approve:hover { background: #bbf7d0; }
.btn-reject { background: #fef2f2; color: #991b1b; }
.btn-reject:hover { background: #fecaca; }
.empty-row { text-align: center; padding: 48px !important; color: #9ca3af; }

.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; padding: 20px; border-top: 1px solid #f3f4f6; }
.pagination button { padding: 8px 18px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 13px; color: #374151; cursor: pointer; transition: all 0.2s; font-family: inherit; }
.pagination button:hover:not(:disabled) { border-color: #111827; }
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.pagination span { font-size: 13px; color: #6b7280; }

/* 详情弹窗 */
.modal-overlay { position: fixed; inset: 0; background: rgba(17,24,39,0.5); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal-card { background: #fff; border-radius: 16px; width: 90%; max-width: 560px; padding: 32px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); max-height: 80vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.modal-header h3 { margin: 0; font-size: 20px; font-weight: 600; }
.modal-close { background: none; border: none; font-size: 22px; cursor: pointer; color: #9ca3af; }
.modal-close:hover { color: #111827; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; }
.dl { font-size: 12px; color: #9ca3af; font-weight: 500; }
.dv { font-size: 14px; color: #111827; font-weight: 500; }
.sub-title { font-size: 16px; font-weight: 600; margin: 0 0 12px; }
.no-files { color: #9ca3af; font-size: 13px; text-align: center; padding: 20px; }
.file-list { display: flex; flex-direction: column; gap: 8px; }
.file-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; background: #f9fafb; border-radius: 8px; }
.f-name { font-size: 13px; font-weight: 500; color: #374151; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.25s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
