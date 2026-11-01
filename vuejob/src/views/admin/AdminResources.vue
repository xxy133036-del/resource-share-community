<template>
  <div class="page">
    <div class="page-top">
      <div>
        <h1 class="page-title">资源审核</h1>
        <p class="page-desc">管理用户上传的资源文件，审核、下架或下载</p>
      </div>
    </div>

    <!-- 搜索 & 筛选 -->
    <div class="filter-bar">
      <div class="search-wrap">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="search-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" /></svg>
        <input type="text" v-model="keyword" placeholder="搜索文件名、标签或作者..." @keyup.enter="doSearch" />
      </div>
      <select v-model="statusFilter" @change="doSearch" class="status-select">
        <option :value="null">全部状态</option>
        <option :value="0">待审核</option>
        <option :value="2">已通过</option>
        <option :value="1">已下架</option>
      </select>
      <select v-model="sortType" @change="doSearch" class="status-select">
        <option value="latest">最新发布</option>
        <option value="hottest">最热</option>
      </select>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <div v-if="isLoading" class="loading">加载中...</div>
      <table v-else>
        <thead>
          <tr>
            <th>文件名</th>
            <th>上传者</th>
            <th>标签</th>
            <th>上传时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td class="file-name-cell">
              <span class="file-name link" @click="viewDetail(item)">{{ item.fileName }}</span>
              <span class="file-desc" v-if="item.description">{{ item.description }}</span>
            </td>
            <td>{{ item.authorName || '-' }}</td>
            <td>
              <div class="tag-list" v-if="item.tags">
                <span class="tag" v-for="t in splitTags(item.tags)" :key="t">{{ t }}</span>
              </div>
              <span v-else>-</span>
            </td>
            <td class="time-cell">{{ item.createTime }}</td>
            <td><span :class="['status-badge', statusClass(item.status)]">{{ statusText(item.status) }}</span></td>
            <td class="action-cell">
              <button v-if="item.status !== 2" class="btn-approve" @click="updateStatus(item, 2)">通过</button>
              <button v-if="item.status !== 1" class="btn-reject" @click="updateStatus(item, 1)">下架</button>
              <button class="btn-download" @click="downloadFile(item)">下载</button>
            </td>
          </tr>
          <tr v-if="list.length === 0">
            <td colspan="6" class="empty-row">暂无数据</td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="pagination">
        <button :disabled="pageNum === 1" @click="goPage(pageNum - 1)">上一页</button>
        <span>{{ pageNum }} / {{ totalPages }}</span>
        <button :disabled="pageNum >= totalPages" @click="goPage(pageNum + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminRequest from '@/utils/adminRequest'
import { showToast } from '@/utils/toast'
import { useRouter } from 'vue-router'

const router = useRouter()

const keyword = ref('')
const statusFilter = ref(null)
const sortType = ref('latest')
const list = ref([])
const isLoading = ref(false)
const pageNum = ref(1)
const totalPages = ref(1)

onMounted(() => fetchList())

const fetchList = async () => {
  isLoading.value = true
  try {
    const res = await adminRequest.get('/resourcesList', {
      params: { keyword: keyword.value || undefined, status: statusFilter.value ?? undefined, sortType: sortType.value, pageNum: pageNum.value, pageSize: 10 }
    })
    if (res.code === 200) {
      list.value = res.data?.records || []
      totalPages.value = res.data?.pages || 1
    } else {
      showToast(res.message || '获取资源列表失败', 'error')
    }
  } catch (e) {
    console.error(e)
    showToast('加载资源列表失败', 'error')
  }
  finally { isLoading.value = false }
}

const doSearch = () => { pageNum.value = 1; fetchList() }
const goPage = (p) => { pageNum.value = p; fetchList() }

const updateStatus = async (item, status) => {
  try {
    const res = await adminRequest.put('/updateStatus', null, { params: { fileUrl: item.fileUrl, status } })
    // 后端现在返回标准 CommonResult JSON（如 { code: 200, message: '已审核' }）
    if (res && res.code === 200) {
      showToast(status === 2 ? '已通过审核' : '已下架', 'success')
      item.status = status
    } else {
      console.error('审核操作返回异常格式:', res)
      showToast(res?.message || '操作失败', 'error')
    }
  } catch (e) {
    console.error('审核操作异常:', e)
    showToast('操作异常，请打开浏览器控制台查看详情', 'error')
  }
}

const downloadFile = async (item) => {
  try {
    const response = await adminRequest.get(`/downloadResource/${encodeURIComponent(item.fileUrl)}`, { responseType: 'blob' })
    const blob = new Blob([response.data], { type: response.headers['content-type'] || 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = item.fileName || 'download'; a.click()
    window.URL.revokeObjectURL(url)
  } catch (e) { showToast('下载失败', 'error') }
}

const splitTags = (tags) => typeof tags === 'string' ? tags.split(',') : (tags || [])
const statusClass = (s) => s === 2 ? 'approved' : s === 1 ? 'rejected' : s === 0 ? 'pending' : ''
const statusText = (s) => s === 2 ? '已通过' : s === 1 ? '已下架' : s === 0 ? '待审核' : '未知'

/** 跳转管理员资源详情页 */
const viewDetail = (item) => {
  router.push({ path: `/admin/resource/${item.id}`, query: { fileUrl: item.fileUrl } })
}
</script>

<style scoped>
.page { max-width: 1200px; }
.page-top { margin-bottom: 28px; }
.page-title { margin: 0; font-size: 28px; font-weight: 700; color: #111827; letter-spacing: -0.5px; }
.page-desc { margin: 6px 0 0; font-size: 14px; color: #6b7280; }

/* 筛选栏 */
.filter-bar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.search-wrap { flex: 1; display: flex; align-items: center; background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 0 14px; }
.search-wrap:focus-within { border-color: #111827; box-shadow: 0 0 0 3px rgba(17,24,39,0.06); }
.search-icon { width: 18px; height: 18px; color: #9ca3af; flex-shrink: 0; }
.search-wrap input { flex: 1; border: none; padding: 12px 10px; font-size: 14px; outline: none; background: transparent; }
.status-select { padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 14px; color: #374151; background: #fff; outline: none; cursor: pointer; }
.status-select:focus { border-color: #111827; }

/* 表格 */
.table-card { background: #fff; border-radius: 14px; border: 1px solid rgba(229,231,235,0.5); overflow: hidden; }
.loading { text-align: center; padding: 60px; color: #9ca3af; }
table { width: 100%; border-collapse: collapse; }
thead th { padding: 14px 20px; font-size: 12px; font-weight: 600; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid #f3f4f6; text-align: left; background: #fafafa; }
tbody td { padding: 16px 20px; font-size: 14px; color: #374151; border-bottom: 1px solid #f9fafb; }
tbody tr:hover { background: #fafbfc; }
.file-name-cell { max-width: 240px; }
.file-name { display: block; font-weight: 600; color: #111827; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-name.link { cursor: pointer; color: #2563eb; }
.file-name.link:hover { color: #1d4ed8; text-decoration: underline; }
.file-desc { display: block; font-size: 12px; color: #9ca3af; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.time-cell { white-space: nowrap; font-size: 13px; color: #6b7280; }
.tag-list { display: flex; gap: 4px; flex-wrap: wrap; }
.tag { padding: 2px 8px; background: #f3f4f6; border-radius: 10px; font-size: 11px; color: #6b7280; }

/* 状态标签 */
.status-badge { display: inline-block; padding: 4px 12px; border-radius: 14px; font-size: 12px; font-weight: 600; }
.status-badge.approved { background: #dcfce7; color: #166534; }
.status-badge.pending { background: #fef3c7; color: #92400e; }
.status-badge.rejected { background: #f3f4f6; color: #6b7280; }

/* 操作按钮 */
.action-cell { display: flex; gap: 8px; }
.btn-approve, .btn-reject, .btn-download {
  padding: 6px 14px; border-radius: 8px; font-size: 12px; font-weight: 500; border: none; cursor: pointer; transition: all 0.15s; font-family: inherit;
}
.btn-approve { background: #dcfce7; color: #166534; }
.btn-approve:hover { background: #bbf7d0; }
.btn-reject { background: #fef2f2; color: #991b1b; }
.btn-reject:hover { background: #fecaca; }
.btn-download { background: #f3f4f6; color: #374151; }
.btn-download:hover { background: #e5e7eb; }

.empty-row { text-align: center; padding: 48px !important; color: #9ca3af; }

/* 分页 */
.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; padding: 20px; border-top: 1px solid #f3f4f6; }
.pagination button { padding: 8px 18px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 13px; color: #374151; cursor: pointer; transition: all 0.2s; font-family: inherit; }
.pagination button:hover:not(:disabled) { border-color: #111827; }
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.pagination span { font-size: 13px; color: #6b7280; }
</style>
