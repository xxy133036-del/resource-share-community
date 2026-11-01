<template>
  <div class="page">
    <div class="page-top">
      <div>
        <h1 class="page-title">管理员管理</h1>
        <p class="page-desc">管理系统管理员账号，分配角色和权限</p>
      </div>
      <button class="btn-primary" @click="openCreate">+ 新建管理员</button>
    </div>

    <!-- 搜索 & 筛选 -->
    <div class="filter-bar">
      <div class="search-wrap">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="search-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" /></svg>
        <input type="text" v-model="keyword" placeholder="搜索管理员账号..." @keyup.enter="doSearch" />
      </div>
      <input type="date" v-model="startDate" class="date-input" @change="doSearch" />
      <span class="date-sep">至</span>
      <input type="date" v-model="endDate" class="date-input" @change="doSearch" />
      <button class="search-btn" @click="doSearch">搜索</button>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <div v-if="isLoading" class="loading">加载中...</div>
      <table v-else>
        <thead>
          <tr>
            <th>账号</th>
            <th>角色</th>
            <th>权限</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in list" :key="a.id">
            <td class="name-cell">{{ a.adminName }}</td>
            <td>{{ a.roleName || '-' }}</td>
            <td>
              <div class="priv-list" v-if="a.privileges && a.privileges.length > 0">
                <span class="priv-tag" v-for="p in a.privileges" :key="p">{{ p }}</span>
              </div>
              <span v-else>-</span>
            </td>
            <td><span :class="['status-badge', a.status === 1 ? 'active' : 'frozen']">{{ a.status === 1 ? '正常' : '冻结' }}</span></td>
            <td class="time-cell">{{ a.createTime }}</td>
            <td class="action-cell">
              <span v-if="a.roleName === '超级管理员'" class="noop-text">不可操作</span>
              <template v-else>
                <button v-if="a.status === 1" class="btn-reject" @click="toggleStatus(a, 2)">冻结</button>
                <button v-else class="btn-approve" @click="toggleStatus(a, 1)">解冻</button>
              </template>
            </td>
          </tr>
          <tr v-if="list.length === 0">
            <td colspan="6" class="empty-row">暂无管理员</td>
          </tr>
        </tbody>
      </table>

      <div v-if="totalPages > 1" class="pagination">
        <button :disabled="pageNum === 1" @click="goPage(pageNum - 1)">上一页</button>
        <span>{{ pageNum }} / {{ totalPages }}</span>
        <button :disabled="pageNum >= totalPages" @click="goPage(pageNum + 1)">下一页</button>
      </div>
    </div>

    <!-- 新建弹窗 -->
    <Transition name="modal">
      <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal-card">
          <div class="modal-header">
            <h3>新建管理员</h3>
            <button class="modal-close" @click="closeModal">×</button>
          </div>
          <form @submit.prevent="submitCreate" class="admin-form">
            <div class="form-group">
              <label>管理员账号 <span class="req">*</span></label>
              <input type="text" v-model="createForm.adminName" required placeholder="仅限数字或字母" maxlength="20" />
            </div>
            <div class="form-group">
              <label>密码 <span class="req">*</span></label>
              <input type="password" v-model="createForm.password" required placeholder="至少6位 数字或字母" />
            </div>
            <div class="form-group">
              <label>角色名称</label>
              <input type="text" v-model="createForm.roleName" placeholder="如：超级管理员" maxlength="12" />
            </div>
            <div class="form-group">
              <label>权限分配</label>
              <div class="privilege-chips">
                <span
                  v-for="p in allPrivileges"
                  :key="p"
                  :class="['priv-chip', { selected: createForm.privileges.includes(p) }]"
                  @click="togglePrivilege(p)"
                >{{ p }}</span>
              </div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn-cancel" @click="closeModal">取消</button>
              <button type="submit" class="btn-primary" :disabled="submitting">{{ submitting ? '创建中...' : '确认创建' }}</button>
            </div>
          </form>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminRequest from '@/utils/adminRequest'
import { showToast } from '@/utils/toast'

const keyword = ref('')
const startDate = ref('')
const endDate = ref('')
const list = ref([])
const isLoading = ref(false)
const pageNum = ref(1)
const totalPages = ref(1)

const showModal = ref(false)
const submitting = ref(false)
const createForm = ref({ adminName: '', password: '', roleName: '', privileges: [] })
const allPrivileges = ['管理员管理', '公告管理', '资源管理', '用户管理']

onMounted(() => fetchList())

const fetchList = async () => {
  isLoading.value = true
  try {
    const res = await adminRequest.get('/listAdmin', {
      params: {
        keyWord: keyword.value || undefined,
        startDate: startDate.value || undefined,
        endDate: endDate.value || undefined,
        pageNum: pageNum.value,
        pageSize: 10
      }
    })
    if (res.code === 200) {
      list.value = res.data?.records || []
      totalPages.value = res.data?.pages || 1
    } else {
      showToast(res.message || '获取管理员列表失败', 'error')
    }
  } catch (e) {
    console.error(e)
    showToast('加载管理员列表失败，请检查网络', 'error')
  }
  finally { isLoading.value = false }
}

const doSearch = () => { pageNum.value = 1; fetchList() }
const goPage = (p) => { pageNum.value = p; fetchList() }

const toggleStatus = async (admin, newStatus) => {
  const endpoint = newStatus === 1 ? '/thaw' : '/freeze'
  try {
    const res = await adminRequest.put(endpoint, null, { params: { id: admin.id } })
    if (res.code === 200) {
      admin.status = newStatus
      showToast(newStatus === 1 ? '已解冻' : '已冻结', 'success')
    } else { showToast(res.message || '操作失败', 'error') }
  } catch (e) { showToast('操作异常', 'error') }
}

const openCreate = () => {
  createForm.value = { adminName: '', password: '', roleName: '', privileges: [] }
  showModal.value = true
}

const closeModal = () => { showModal.value = false }

const togglePrivilege = (p) => {
  const idx = createForm.value.privileges.indexOf(p)
  if (idx >= 0) createForm.value.privileges.splice(idx, 1)
  else createForm.value.privileges.push(p)
}

const submitCreate = async () => {
  if (submitting.value) return
  submitting.value = true
  try {
    const res = await adminRequest.post('/insertAdmin', {
      adminName: createForm.value.adminName,
      password: createForm.value.password,
      roleName: createForm.value.roleName,
      privileges: createForm.value.privileges
    })
    if (res.code === 200) {
      showToast('管理员创建成功', 'success')
      closeModal()
      pageNum.value = 1
      await fetchList()
    } else { showToast(res.message || '创建失败', 'error') }
  } catch (e) {
    console.error('创建管理员异常:', e)
    showToast('创建异常，请检查控制台', 'error')
  }
  finally { submitting.value = false }
}
</script>

<style scoped>
.page { max-width: 1200px; }
.page-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; }
.page-title { margin: 0; font-size: 28px; font-weight: 700; color: #111827; letter-spacing: -0.5px; }
.page-desc { margin: 6px 0 0; font-size: 14px; color: #6b7280; }

.btn-primary { padding: 12px 24px; background: #111827; color: #fff; border: none; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; font-family: inherit; white-space: nowrap; }
.btn-primary:hover { background: #1f2937; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(17,24,39,0.2); }
.btn-primary:disabled { background: #9ca3af; cursor: not-allowed; transform: none; box-shadow: none; }

.filter-bar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.search-wrap { flex: 1; display: flex; align-items: center; background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 0 14px; }
.search-wrap:focus-within { border-color: #111827; box-shadow: 0 0 0 3px rgba(17,24,39,0.06); }
.search-icon { width: 18px; height: 18px; color: #9ca3af; flex-shrink: 0; }
.search-wrap input { flex: 1; border: none; padding: 12px 10px; font-size: 14px; outline: none; background: transparent; }
.date-input { padding: 12px 10px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 13px; outline: none; color: #374151; }
.date-input:focus { border-color: #111827; }
.date-sep { font-size: 13px; color: #9ca3af; }
.search-btn { padding: 12px 20px; background: #111827; color: #fff; border: none; border-radius: 10px; font-size: 14px; font-weight: 500; cursor: pointer; transition: background 0.2s; font-family: inherit; }
.search-btn:hover { background: #1f2937; }

.table-card { background: #fff; border-radius: 14px; border: 1px solid rgba(229,231,235,0.5); overflow: hidden; }
.loading { text-align: center; padding: 60px; color: #9ca3af; }
table { width: 100%; border-collapse: collapse; }
thead th { padding: 14px 20px; font-size: 12px; font-weight: 600; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid #f3f4f6; text-align: left; background: #fafafa; }
tbody td { padding: 16px 20px; font-size: 14px; color: #374151; border-bottom: 1px solid #f9fafb; }
tbody tr:hover { background: #fafbfc; }
.name-cell { font-weight: 600; color: #111827; }
.time-cell { white-space: nowrap; font-size: 13px; color: #6b7280; }
.status-badge { display: inline-block; padding: 4px 12px; border-radius: 14px; font-size: 12px; font-weight: 600; }
.status-badge.active { background: #dcfce7; color: #166534; }
.status-badge.frozen { background: #fef2f2; color: #991b1b; }
.priv-list { display: flex; gap: 4px; flex-wrap: wrap; }
.priv-tag { padding: 2px 8px; background: #f3f4f6; border-radius: 10px; font-size: 11px; color: #6b7280; }
.action-cell { display: flex; gap: 8px; }
.noop-text { font-size: 12px; color: #9ca3af; padding: 6px 0; }
.btn-approve, .btn-reject {
  padding: 6px 14px; border-radius: 8px; font-size: 12px; font-weight: 500; border: none; cursor: pointer; transition: all 0.15s; font-family: inherit;
}
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

/* 弹窗 */
.modal-overlay { position: fixed; inset: 0; background: rgba(17,24,39,0.5); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal-card { background: #fff; border-radius: 16px; width: 90%; max-width: 500px; padding: 32px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.modal-header h3 { margin: 0; font-size: 20px; font-weight: 600; }
.modal-close { background: none; border: none; font-size: 22px; cursor: pointer; color: #9ca3af; }
.modal-close:hover { color: #111827; }
.admin-form { display: flex; flex-direction: column; gap: 20px; }
.form-group { display: flex; flex-direction: column; gap: 8px; }
.form-group label { font-size: 14px; font-weight: 500; color: #374151; }
.req { color: #ef4444; }
.form-group input {
  width: 100%; padding: 12px 16px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 14px; font-family: inherit; outline: none; box-sizing: border-box; background: #f9fafb; transition: border-color 0.2s;
}
.form-group input:focus { border-color: #111827; background: #fff; box-shadow: 0 0 0 3px rgba(17,24,39,0.06); }
.privilege-chips { display: flex; gap: 8px; flex-wrap: wrap; }
.priv-chip {
  padding: 6px 14px; background: #f3f4f6; border: 1px solid #e5e7eb; border-radius: 14px; font-size: 13px; color: #6b7280; cursor: pointer; transition: all 0.2s; user-select: none;
}
.priv-chip:hover { border-color: #9ca3af; }
.priv-chip.selected { background: #111827; color: #fff; border-color: #111827; }
.modal-footer { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { padding: 10px 20px; background: transparent; border: 1px solid #d1d5db; border-radius: 10px; font-size: 14px; cursor: pointer; color: #374151; font-family: inherit; transition: all 0.2s; }
.btn-cancel:hover { background: #f3f4f6; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.25s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
