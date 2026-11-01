<template>
  <div class="page">
    <div class="page-top">
      <div>
        <h1 class="page-title">公告管理</h1>
        <p class="page-desc">发布和管理系统公告，公告将通过 Redis 实时推送给在线用户</p>
      </div>
      <button class="btn-primary" @click="openCreate">+ 新建公告</button>
    </div>

    <!-- 搜索 -->
    <div class="filter-bar">
      <div class="search-wrap">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="search-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" /></svg>
        <input type="text" v-model="keyword" placeholder="搜索公告标题..." @keyup.enter="doSearch" />
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
            <th>标题</th>
            <th>内容摘要</th>
            <th>发布时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="n in list" :key="n.id">
            <td class="title-cell">{{ n.title }}</td>
            <td class="content-cell">{{ n.content?.substring(0, 60) }}{{ n.content?.length > 60 ? '...' : '' }}</td>
            <td class="time-cell">{{ n.createTime }}</td>
            <td class="action-cell">
              <button class="btn-edit" @click="openEdit(n)">编辑</button>
              <button class="btn-delete" @click="deleteNotice(n)">删除</button>
            </td>
          </tr>
          <tr v-if="list.length === 0">
            <td colspan="4" class="empty-row">暂无公告</td>
          </tr>
        </tbody>
      </table>

      <div v-if="totalPages > 1" class="pagination">
        <button :disabled="pageNum === 1" @click="goPage(pageNum - 1)">上一页</button>
        <span>{{ pageNum }} / {{ totalPages }}</span>
        <button :disabled="pageNum >= totalPages" @click="goPage(pageNum + 1)">下一页</button>
      </div>
    </div>

    <!-- 新建/编辑弹窗 -->
    <Transition name="modal">
      <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal-card">
          <div class="modal-header">
            <h3>{{ editingNotice ? '编辑公告' : '新建公告' }}</h3>
            <button class="modal-close" @click="closeModal">×</button>
          </div>
          <form @submit.prevent="submitForm" class="notice-form">
            <div class="form-group">
              <label>公告标题 <span class="req">*</span></label>
              <input type="text" v-model="form.title" required placeholder="输入公告标题" maxlength="100" />
            </div>
            <div class="form-group">
              <label>公告内容 <span class="req">*</span></label>
              <textarea v-model="form.content" required placeholder="输入公告正文..." rows="6" maxlength="2000"></textarea>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn-cancel" @click="closeModal">取消</button>
              <button type="submit" class="btn-primary" :disabled="submitting">{{ submitting ? '保存中...' : '保存发布' }}</button>
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

// 弹窗
const showModal = ref(false)
const editingNotice = ref(null)
const submitting = ref(false)
const form = ref({ title: '', content: '' })

onMounted(() => fetchList())

const fetchList = async () => {
  isLoading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: 10 }
    if (keyword.value || startDate.value || endDate.value) {
      Object.assign(params, { keyword: keyword.value || undefined, startDate: startDate.value || undefined, endDate: endDate.value || undefined })
    }
    const res = await adminRequest.get(keyword.value || startDate.value || endDate.value ? '/queryNoticeByAddition' : '/queryNoticeList', { params })
    if (res.code === 200) {
      list.value = res.data?.records || []
      totalPages.value = res.data?.pages || 1
    } else {
      showToast(res.message || '获取公告列表失败', 'error')
    }
  } catch (e) {
    console.error(e)
    showToast('加载公告列表失败，请检查网络', 'error')
  }
  finally { isLoading.value = false }
}

const doSearch = () => { pageNum.value = 1; fetchList() }
const goPage = (p) => { pageNum.value = p; fetchList() }

const openCreate = () => {
  editingNotice.value = null
  form.value = { title: '', content: '' }
  showModal.value = true
}

const openEdit = (notice) => {
  editingNotice.value = notice
  form.value = { title: notice.title, content: notice.content }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingNotice.value = null
}

const submitForm = async () => {
  if (submitting.value) return
  submitting.value = true
  try {
    let res
    if (editingNotice.value) {
      res = await adminRequest.put('/updateNotice', null, { params: { noticeId: editingNotice.value.id, title: form.value.title, content: form.value.content } })
    } else {
      res = await adminRequest.post('/insertNotice', { title: form.value.title, content: form.value.content })
    }
    if (res.code === 200) {
      showToast(editingNotice.value ? '公告已更新' : '公告已发布', 'success')
      closeModal()
      fetchList()
    } else { showToast(res.message || '操作失败', 'error') }
  } catch (e) { showToast('操作异常', 'error') }
  finally { submitting.value = false }
}

const deleteNotice = async (notice) => {
  if (!confirm(`确定要删除公告「${notice.title}」吗？`)) return
  try {
    const res = await adminRequest.delete('/deleteNotice', { params: { noticeId: notice.id } })
    if (res.code === 200) {
      showToast('已删除', 'success')
      fetchList()
    } else { showToast(res.message || '删除失败', 'error') }
  } catch (e) { showToast('删除异常', 'error') }
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

/* 筛选 */
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

/* 表格 */
.table-card { background: #fff; border-radius: 14px; border: 1px solid rgba(229,231,235,0.5); overflow: hidden; }
.loading { text-align: center; padding: 60px; color: #9ca3af; }
table { width: 100%; border-collapse: collapse; }
thead th { padding: 14px 20px; font-size: 12px; font-weight: 600; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid #f3f4f6; text-align: left; background: #fafafa; }
tbody td { padding: 16px 20px; font-size: 14px; color: #374151; border-bottom: 1px solid #f9fafb; }
tbody tr:hover { background: #fafbfc; }
.title-cell { font-weight: 600; color: #111827; max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.content-cell { color: #6b7280; font-size: 13px; max-width: 300px; }
.time-cell { white-space: nowrap; font-size: 13px; color: #6b7280; }
.action-cell { display: flex; gap: 8px; }
.btn-edit, .btn-delete {
  padding: 6px 14px; border-radius: 8px; font-size: 12px; font-weight: 500; border: none; cursor: pointer; transition: all 0.15s; font-family: inherit;
}
.btn-edit { background: #eff6ff; color: #1d4ed8; }
.btn-edit:hover { background: #dbeafe; }
.btn-delete { background: #fef2f2; color: #991b1b; }
.btn-delete:hover { background: #fecaca; }
.empty-row { text-align: center; padding: 48px !important; color: #9ca3af; }

.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; padding: 20px; border-top: 1px solid #f3f4f6; }
.pagination button { padding: 8px 18px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 13px; color: #374151; cursor: pointer; transition: all 0.2s; font-family: inherit; }
.pagination button:hover:not(:disabled) { border-color: #111827; }
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.pagination span { font-size: 13px; color: #6b7280; }

/* 弹窗 */
.modal-overlay { position: fixed; inset: 0; background: rgba(17,24,39,0.5); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal-card { background: #fff; border-radius: 16px; width: 90%; max-width: 560px; padding: 32px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.modal-header h3 { margin: 0; font-size: 20px; font-weight: 600; }
.modal-close { background: none; border: none; font-size: 22px; cursor: pointer; color: #9ca3af; }
.modal-close:hover { color: #111827; }
.notice-form { display: flex; flex-direction: column; gap: 20px; }
.form-group { display: flex; flex-direction: column; gap: 8px; }
.form-group label { font-size: 14px; font-weight: 500; color: #374151; }
.req { color: #ef4444; }
.form-group input, .form-group textarea {
  width: 100%; padding: 12px 16px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 14px; font-family: inherit; outline: none; transition: border-color 0.2s; box-sizing: border-box; background: #f9fafb;
}
.form-group input:focus, .form-group textarea:focus { border-color: #111827; background: #fff; box-shadow: 0 0 0 3px rgba(17,24,39,0.06); }
.form-group textarea { resize: vertical; }
.modal-footer { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { padding: 10px 20px; background: transparent; border: 1px solid #d1d5db; border-radius: 10px; font-size: 14px; cursor: pointer; color: #374151; font-family: inherit; transition: all 0.2s; }
.btn-cancel:hover { background: #f3f4f6; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.25s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
