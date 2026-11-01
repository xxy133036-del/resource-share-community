<template>
  <div class="home-layout">
    <main class="main-content">

      <section class="hero-banner">
        <div class="banner-content">
          <h1>探索未知的代码世界</h1>
          <p>发现、分享、与顶尖开发者同行</p>
        </div>
      </section>

      <div class="grid-container">

        <section class="feed-section">
          <!-- 搜索栏 -->
          <div class="search-bar">
            <div class="search-input-wrap">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="search-icon">
                <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
              </svg>
              <input
                  type="text"
                  v-model="searchKeyword"
                  placeholder="搜索标题、标签或作者..."
                  @keyup.enter="doSearch"
              />
              <button v-if="searchKeyword" class="clear-input-btn" @click="searchKeyword = ''">×</button>
            </div>
            <div class="date-inputs">
              <input type="date" v-model="searchStartDate" title="开始日期" />
              <span class="date-sep">至</span>
              <input type="date" v-model="searchEndDate" title="结束日期" />
            </div>
            <button class="search-btn" @click="doSearch">搜索</button>
            <button v-if="isSearching" class="reset-btn" @click="resetSearch">重置</button>
          </div>

          <div class="feed-header">
            <div class="header-left">
              <h3 class="section-title">资源大厅</h3>
              <div class="sort-tabs">
                <button
                    :class="['tab-btn', { active: currentSort === 'latest' }]"
                    @click="switchSort('latest')"
                >最新发布</button>
                <button
                    :class="['tab-btn', { active: currentSort === 'popular' }]"
                    @click="switchSort('popular')"
                >热门推荐</button>
              </div>
            </div>

            <button class="publish-btn" @click="openUploadModal">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="btn-icon">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
              发布资源
            </button>
          </div>

          <!-- Loading 状态 -->
          <div v-if="isLoading" class="loading-state">
            <div class="loading-spinner-large"></div>
            <p>正在加载资源...</p>
          </div>

          <div v-else class="resource-list">
            <article class="resource-card" v-for="item in resourceList" :key="item.id">
              <div class="card-header">
                <img :src="item.avatar" alt="author" class="author-avatar" @click.stop="viewAuthor(item)" />
                <div class="author-info">
                  <span class="author-name" @click.stop="viewAuthor(item)">{{ item.author }}</span>
                  <span class="publish-time">{{ item.time || '刚刚' }}</span>
                </div>

                <div v-if="currentSort === 'popular'" class="hot-score-badge">
                  🔥 {{ item.hotScore || 0 }} 热度
                </div>
              </div>

              <div class="card-body" @click="viewDetail(item)">
                <h4 class="card-title">{{ item.fileName }}</h4>
                <p class="card-desc">{{ item.desc }}</p>
              </div>

              <div class="card-footer">
                <div class="tags">
                  <span class="tag" v-for="tag in item.tags" :key="tag">{{ tag }}</span>
                </div>
                <div class="card-actions">
                  <button class="download-btn" @click.stop="handleDownload(item)" :disabled="item._downloading">
                    <svg v-if="!item._downloading" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="btn-icon">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
                    </svg>
                    <svg v-else class="spin-icon" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" opacity="0.25"></circle>
                      <path d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" fill="currentColor" opacity="0.75"></path>
                    </svg>
                    下载
                  </button>
                </div>
              </div>
            </article>

            <div v-if="resourceList.length === 0 && !isLoading" class="empty-state">
              <p>暂无资源分享，快来发布第一篇吧！</p>
            </div>

            <!-- 分页 -->
            <div v-if="totalPages > 1" class="pagination">
              <button class="page-btn" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">上一页</button>
              <button
                  v-for="p in visiblePages"
                  :key="p"
                  :class="['page-btn', 'page-num', { active: p === currentPage }]"
                  @click="goToPage(p)"
              >{{ p }}</button>
              <button class="page-btn" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">下一页</button>
              <span class="page-info">共 {{ totalPages }} 页</span>
            </div>
          </div>

        </section>

        <aside class="sidebar">
          <div class="widget announcement-widget">
            <h3 class="widget-title">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="icon-small">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              系统公告
            </h3>
            <div v-if="isLoadingNotices" class="notice-loading">加载中...</div>
            <ul v-else-if="notices.length > 0" class="announcement-list">
              <li v-for="notice in notices" :key="notice.id">
                <span class="dot"></span>
                <a href="javascript:void(0)" class="notice-text" @click="showNoticeDetail(notice)">{{ notice.title }}</a>
                <span class="notice-date">{{ notice.date }}</span>
              </li>
            </ul>
            <div v-else class="notice-empty">暂无公告</div>
          </div>
        </aside>

      </div>
    </main>

    <!-- 上传弹窗 -->
    <Transition name="modal-fade">
      <div v-if="showUploadModal" class="modal-overlay" @click.self="closeUploadModal">
        <div class="modal-content">
          <div class="modal-header">
            <h3>发布新资源</h3>
            <button class="close-btn" @click="closeUploadModal">×</button>
          </div>

          <div class="upload-tips">
            <span class="tip-icon">💡</span>
            <span class="tip-text">上传资源获得 5 积分，热度越高可收获积分奖励越高</span>
          </div>

          <form @submit.prevent="submitUpload" class="upload-form">
            <div class="form-group">
              <label>资源标题 <span class="required">*</span></label>
              <input type="text" v-model="uploadForm.fileName" required placeholder="请输入资源名称（如：Spring Boot 学习笔记）" maxlength="50" />
            </div>

            <div class="form-group">
              <label>详细描述 <span class="required">*</span></label>
              <textarea v-model="uploadForm.description" required placeholder="简单介绍一下这个资源的亮点和用途..." rows="4"></textarea>
            </div>

            <div class="form-group">
              <label>资源标签 <span class="tag-hint">（可多选预设或自定义）</span></label>
              <div class="tag-selector">
                <span
                    v-for="tag in presetTags"
                    :key="tag"
                    :class="['tag-chip', { active: uploadForm.tags.includes(tag) }]"
                    @click="toggleTag(tag)"
                >{{ tag }}</span>
              </div>
              <div class="custom-tag-row">
                <input
                    type="text"
                    v-model="customTagInput"
                    placeholder="自定义标签，回车添加"
                    maxlength="10"
                    @keyup.enter.prevent="addCustomTag"
                />
              </div>
              <div v-if="uploadForm.tags.length > 0" class="selected-tags">
                <span v-for="tag in uploadForm.tags" :key="tag" class="selected-tag">
                  {{ tag }}
                  <button type="button" class="remove-tag-btn" @click="removeTag(tag)">×</button>
                </span>
              </div>
            </div>

            <div class="form-group">
              <label>上传文件 <span class="required">*</span></label>
              <div class="file-upload-box" @click="triggerFileInput">
                <input type="file" ref="fileInput" @change="handleFileChange" required style="display: none;" />
                <svg v-if="!selectedFile" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="upload-icon">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 16.5V9.75m0 0l3 3m-3-3l-3 3M6.75 19.5a4.5 4.5 0 01-1.41-8.775 5.25 5.25 0 0110.233-2.33 3 3 0 013.758 3.848A3.752 3.752 0 0118 19.5H6.75z" />
                </svg>
                <div v-if="!selectedFile" class="upload-text">点击选择本地文件 (最大 10MB)</div>
                <div v-else class="file-selected">
                  <span class="file-name">📄 {{ selectedFile.name }}</span>
                  <span class="file-size">({{ (selectedFile.size / 1024 / 1024).toFixed(2) }} MB)</span>
                </div>
              </div>
            </div>

            <div class="modal-footer">
              <button type="button" class="btn-cancel" @click="closeUploadModal">取消</button>
              <button type="submit" class="btn-submit" :disabled="isUploading">
                {{ isUploading ? '正在上传...' : '确认发布' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { showToast } from '@/utils/toast'
import { formatTime } from '@/utils/format'
import { getAvatarUrl } from '@/utils/config'

const router = useRouter()

// --- 列表状态 ---
const currentSort = ref('latest')
const resourceList = ref([])
const isLoading = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)
const pageSize = 7

// --- 搜索状态 ---
const searchKeyword = ref('')
const searchStartDate = ref('')
const searchEndDate = ref('')
const isSearching = ref(false)

const doSearch = () => {
  currentPage.value = 1
  isSearching.value = true
  fetchResourceList()
}

const resetSearch = () => {
  searchKeyword.value = ''
  searchStartDate.value = ''
  searchEndDate.value = ''
  isSearching.value = false
  currentPage.value = 1
  fetchResourceList()
}

// 计算可见页码（最多显示7个页码按钮）
const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const cur = currentPage.value
  let start = Math.max(1, cur - 3)
  let end = Math.min(total, cur + 3)
  if (end - start < 6) {
    if (start === 1) end = Math.min(total, start + 6)
    else start = Math.max(1, end - 6)
  }
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

// 同步标记：告知 MainLayout 首页已接管 /home 请求，无需兜底调用
window.__homePageActive = true

onMounted(() => {
  fetchResourceList()
})

const fetchResourceList = async () => {
  isLoading.value = true
  try {
    let res
    if (isSearching.value) {
      // 搜索模式：走 /searchResources
      res = await request.get('/searchResources', {
        params: {
          keyword: searchKeyword.value.trim(),
          startDate: searchStartDate.value || undefined,
          endDate: searchEndDate.value || undefined,
          pageNum: currentPage.value,
          pageSize: pageSize
        }
      })
    } else {
      // 普通模式：走 /home（包含用户信息、文件列表、公告）
      res = await request.get('/home', {
        params: {
          storage: currentSort.value === 'popular' ? 'hottest' : 'latest',
          pageNum: currentPage.value,
          pageSize: pageSize
        }
      })
    }

    if (res.code === 200) {
      // 搜索模式返回结构不同：普通模式 data.files，搜索模式 data 直接是 Page
      const pageData = isSearching.value ? res.data : res.data.files

      // 普通模式下传递用户信息 + 公告数据
      if (!isSearching.value && res.data.userAndCheckInStatus) {
        window.dispatchEvent(new CustomEvent('user-info-updated', {
          detail: {
            username: res.data.userAndCheckInStatus.user?.username,
            nickname: res.data.userAndCheckInStatus.user?.nickname,
            avatar: res.data.userAndCheckInStatus.user?.avatar,
            hasCheckedIn: res.data.userAndCheckInStatus.hasCheckedIn || false
          }
        }))
        // 从 /home 响应中直接取公告列表
        const noticesData = res.data.notices || []
        notices.value = noticesData.map(n => ({
          id: n.id,
          title: n.title,
          content: n.content,
          date: formatTime(n.createTime)
        }))
      }

      const files = pageData.records || []
      totalPages.value = pageData.pages || 1

      resourceList.value = files.map(item => ({
        id: item.id,
        fileName: item.fileName,
        desc: item.description || '',
        author: item.authorName || '匿名用户',
        authorId: item.userid,
        avatar: getAvatarUrl(item.authorAvatar),
        time: formatTime(item.createTime),
        fileUrl: item.fileUrl,
        tags: item.tags ? item.tags.split(',') : [],
        hotScore: item.hotScore || 0,
        downloadsCount: item.downloadsCount || 0,
        likesCount: item.likesCount || 0,
        commentsCount: item.commentsCount || 0,
        _liked: item.liked || false
      }))
    } else {
      showToast(res.message || '获取资源列表失败', 'error')
    }
  } catch (error) {
    console.error('获取资源列表异常：', error)
  } finally {
    isLoading.value = false
  }
  // 搜索模式下 /searchResources 不返回公告，需单独加载
  if (isSearching.value) {
    fetchNotices()
  }
}

const switchSort = (type) => {
  if (currentSort.value === type) return
  currentSort.value = type
  currentPage.value = 1
  isSearching.value = false
  searchKeyword.value = ''
  searchStartDate.value = ''
  searchEndDate.value = ''
  fetchResourceList()
}

const goToPage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchResourceList()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// --- 上传相关逻辑 ---
const showUploadModal = ref(false)
const isUploading = ref(false)
const fileInput = ref(null)
const selectedFile = ref(null)

// --- 标签相关 ---
const presetTags = ['Java', 'Spring', 'Vue', 'React', 'Python', 'Node.js', '前端', '后端', 'AI', '数据库', '算法', '面试', '工具', '教程', '项目实战']

const uploadForm = ref({
  fileName: '',
  description: '',
  tags: []
})

const customTagInput = ref('')

const toggleTag = (tag) => {
  const idx = uploadForm.value.tags.indexOf(tag)
  if (idx >= 0) {
    uploadForm.value.tags.splice(idx, 1)
  } else {
    if (uploadForm.value.tags.length >= 5) {
      showToast('最多选择5个标签', 'error')
      return
    }
    uploadForm.value.tags.push(tag)
  }
}

const addCustomTag = () => {
  const val = customTagInput.value.trim()
  if (!val) return
  if (uploadForm.value.tags.includes(val)) {
    showToast('标签已存在', 'error')
    customTagInput.value = ''
    return
  }
  if (uploadForm.value.tags.length >= 5) {
    showToast('最多选择5个标签', 'error')
    return
  }
  uploadForm.value.tags.push(val)
  customTagInput.value = ''
}

const removeTag = (tag) => {
  const idx = uploadForm.value.tags.indexOf(tag)
  if (idx >= 0) uploadForm.value.tags.splice(idx, 1)
}

const openUploadModal = () => {
  showUploadModal.value = true
}

const closeUploadModal = () => {
  showUploadModal.value = false
  uploadForm.value = { fileName: '', description: '', tags: [] }
  customTagInput.value = ''
  selectedFile.value = null
  if (fileInput.value) fileInput.value.value = ''
}

const triggerFileInput = () => {
  fileInput.value.click()
}

const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (!file) return

  if (file.size > 10 * 1024 * 1024) {
    showToast('文件过大！单次上传不能超过 10MB', 'error')
    event.target.value = ''
    return
  }
  selectedFile.value = file
}

const submitUpload = async () => {
  if (!selectedFile.value) {
    showToast('请先选择要上传的资源文件', 'error')
    return
  }

  isUploading.value = true
  const formData = new FormData()

  formData.append('fileName', uploadForm.value.fileName)
  formData.append('description', uploadForm.value.description)
  formData.append('file', selectedFile.value)
  if (uploadForm.value.tags.length > 0) {
    formData.append('tags', uploadForm.value.tags.join(','))
  }

  try {
    const res = await request.post('/uploadFile', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    if (res.code === 200) {
      showToast('资源上传成功！', 'success')
      closeUploadModal()

      currentSort.value = 'latest'
      currentPage.value = 1
      fetchResourceList()
    } else {
      showToast(res.message || '上传失败', 'error')
    }
  } catch (error) {
    console.error('上传异常：', error)
    showToast('上传失败，请检查网络或联系管理员', 'error')
  } finally {
    isUploading.value = false
  }
}

// --- 交互事件 ---
const viewAuthor = (item) => {
  // 跳转到作者发布的资源列表（暂跳转到资源详情）
  showToast(`作者: ${item.author}`, 'success')
}

const viewDetail = (item) => {
  router.push({
    name: 'ResourceDetail',
    params: { id: item.id },
    query: {
      fileName: item.fileName,
      fileUrl: item.fileUrl,
      author: item.author,
      authorId: item.authorId,
      avatar: item.avatar,
      time: item.time,
      description: item.desc,
      downloadsCount: item.downloadsCount,
      likesCount: item.likesCount,
      commentsCount: item.commentsCount,
      tags: item.tags ? item.tags.join(',') : ''
    }
  })
}

const handleDownload = async (item) => {
  if (!item.fileUrl) {
    showToast('文件地址不存在', 'error')
    return
  }

  if (item._downloading) return
  item._downloading = true

  try {
    // 使用 axios 发起下载请求（自动携带 Authorization header）
    // 注意：interceptor 对 blob 类型返回完整的 axios response 对象
    const response = await request.get(`/downloadFile/${item.fileUrl}`, {
      responseType: 'blob'
    })

    // 从响应头获取文件名
    const contentDisposition = response.headers?.['content-disposition']
    let downloadFileName = item.fileName || 'download'
    if (contentDisposition) {
      const match = contentDisposition.match(/filename\*?=['"]?(?:UTF-8'')?([^;\r\n"']*)/i)
      if (match) {
        downloadFileName = decodeURIComponent(match[1])
      }
    }

    // 创建 Blob 并触发下载
    const blob = new Blob([response.data], { type: response.headers['content-type'] || 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = downloadFileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    showToast(`下载成功: ${item.fileName}`, 'success')
    item.downloadsCount++
  } catch (error) {
    console.error('下载异常：', error)
    // 错误消息已由 interceptor 处理并 toast，这里做静默处理
  } finally {
    item._downloading = false
  }
}

// --- 公告 ---
const notices = ref([])
const isLoadingNotices = ref(false)

const fetchNotices = async () => {
  isLoadingNotices.value = true
  try {
    const res = await request.get('/queryNoticeList')
    if (res.code === 200) {
      const noticesData = res.data || []
      notices.value = noticesData.map(n => ({
        id: n.id,
        title: n.title,
        content: n.content,
        date: formatTime(n.createTime)
      }))
    }
  } catch (e) {
    console.error('获取公告列表异常：', e)
  } finally {
    isLoadingNotices.value = false
  }
}

const showNoticeDetail = (notice) => {
  router.push({
    name: 'NoticeDetail',
    query: {
      title: notice.title,
      content: notice.content,
      date: notice.date
    }
  })
}
</script>

<style scoped>
.home-layout { min-height: 100vh; background-color: #f3f4f6; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; color: #111827; }
.main-content { max-width: 1200px; margin: 0 auto; padding: 24px; }
.hero-banner { height: 240px; border-radius: 16px; background: linear-gradient(135deg, #111827 0%, #374151 100%); display: flex; align-items: center; padding: 0 48px; margin-bottom: 32px; box-shadow: 0 10px 20px -5px rgba(0, 0, 0, 0.1); }
.banner-content h1 { color: #ffffff; font-size: 32px; margin: 0 0 12px 0; font-weight: 700; letter-spacing: -1px; }
.banner-content p { color: #9ca3af; font-size: 16px; margin: 0; }
.grid-container { display: grid; grid-template-columns: 1fr 300px; gap: 32px; }

/* 搜索栏 */
.search-bar {
  display: flex; align-items: center; gap: 12px;
  background: #ffffff; border-radius: 12px; padding: 16px;
  border: 1px solid #e5e7eb; margin-bottom: 20px; flex-wrap: wrap;
}
.search-input-wrap {
  flex: 1; min-width: 200px; display: flex; align-items: center;
  background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 8px;
  padding: 0 12px; transition: border-color 0.2s;
}
.search-input-wrap:focus-within { border-color: #111827; }
.search-icon { width: 18px; height: 18px; color: #9ca3af; flex-shrink: 0; }
.search-input-wrap input {
  flex: 1; border: none; background: transparent; padding: 10px 8px;
  font-size: 14px; outline: none; color: #111827;
}
.search-input-wrap input::placeholder { color: #9ca3af; }
.clear-input-btn {
  background: none; border: none; color: #9ca3af; font-size: 18px;
  cursor: pointer; padding: 0; line-height: 1;
}
.clear-input-btn:hover { color: #111827; }
.date-inputs { display: flex; align-items: center; gap: 6px; }
.date-inputs input[type="date"] {
  padding: 8px 10px; border: 1px solid #e5e7eb; border-radius: 8px;
  font-size: 13px; color: #374151; outline: none; background: #f9fafb;
}
.date-inputs input[type="date"]:focus { border-color: #111827; }
.date-sep { font-size: 13px; color: #9ca3af; }
.search-btn {
  padding: 10px 20px; background: #111827; color: #ffffff;
  border: none; border-radius: 8px; font-size: 14px; font-weight: 500;
  cursor: pointer; transition: background-color 0.2s;
}
.search-btn:hover { background-color: #374151; }
.reset-btn {
  padding: 10px 16px; background: transparent; color: #6b7280;
  border: 1px solid #e5e7eb; border-radius: 8px; font-size: 14px;
  cursor: pointer; transition: all 0.2s;
}
.reset-btn:hover { border-color: #9ca3af; color: #111827; }

/* 头部带发布按钮的样式 */
.feed-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header-left { display: flex; align-items: center; gap: 24px; }
.section-title { font-size: 18px; font-weight: 600; margin: 0; }
.sort-tabs { display: flex; background-color: #e5e7eb; padding: 4px; border-radius: 8px; }
.tab-btn { padding: 6px 16px; border: none; background: transparent; color: #6b7280; font-size: 14px; font-weight: 500; border-radius: 6px; cursor: pointer; transition: all 0.2s ease; }
.tab-btn:hover { color: #111827; }
.tab-btn.active { background-color: #ffffff; color: #111827; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1); }

/* 发布按钮 */
.publish-btn { display: flex; align-items: center; gap: 6px; padding: 8px 16px; background-color: #111827; color: #ffffff; border: none; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }
.publish-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
.publish-btn .btn-icon { width: 16px; height: 16px; }

/* Loading */
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 24px; background: #ffffff; border-radius: 12px; color: #9ca3af; gap: 16px; }
.loading-spinner-large { width: 36px; height: 36px; border: 3px solid #e5e7eb; border-top-color: #111827; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* 资源列表与卡片 */
.resource-list { display: flex; flex-direction: column; gap: 20px; }
.resource-card { background: #ffffff; border-radius: 12px; padding: 24px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02); border: 1px solid rgba(229, 231, 235, 0.4); transition: box-shadow 0.2s, transform 0.2s; overflow: hidden; }
.resource-card:hover { box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05); transform: translateY(-2px); }
.card-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.author-avatar { width: 40px; height: 40px; border-radius: 50%; background-color: #f3f4f6; object-fit: cover; cursor: pointer; transition: transform 0.2s; }
.author-avatar:hover { transform: scale(1.05); }
.author-info { display: flex; flex-direction: column; }
.author-name { font-size: 14px; font-weight: 600; color: #374151; cursor: pointer; max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.author-name:hover { text-decoration: underline; }
.publish-time { font-size: 12px; color: #9ca3af; margin-top: 2px; }
.hot-score-badge { margin-left: auto; background-color: #fff1f2; color: #e11d48; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 600; white-space: nowrap; flex-shrink: 0; }
.card-body { cursor: pointer; }
.card-title { font-size: 18px; font-weight: 600; margin: 0 0 8px 0; transition: color 0.2s; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-body:hover .card-title { color: #4b5563; }
.card-desc { font-size: 14px; color: #6b7280; line-height: 1.6; margin: 0 0 16px 0; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; padding-top: 16px; border-top: 1px solid #f3f4f6; }
.tags { display: flex; gap: 8px; flex-wrap: wrap; overflow: hidden; max-height: 28px; }
.tag { background-color: #f3f4f6; color: #4b5563; padding: 4px 12px; border-radius: 14px; font-size: 12px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100px; }
.download-btn { display: flex; align-items: center; gap: 6px; padding: 8px 16px; background-color: #111827; color: #ffffff; border: none; border-radius: 8px; font-size: 13px; font-weight: 500; cursor: pointer; transition: background-color 0.2s; }
.download-btn:hover:not(:disabled) { background-color: #374151; }
.download-btn:disabled { background-color: #9ca3af; cursor: not-allowed; }
.spin-icon { width: 14px; height: 14px; animation: spin 0.8s linear infinite; }
.btn-icon { width: 16px; height: 16px; }

.card-actions { display: flex; align-items: center; gap: 10px; }
.empty-state { text-align: center; padding: 40px; color: #9ca3af; background: #ffffff; border-radius: 12px; border: 1px dashed #e5e7eb; }

/* 侧边栏 */
.widget { background: #ffffff; border-radius: 12px; padding: 24px; border: 1px solid rgba(229, 231, 235, 0.4); }
.widget-title { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; margin: 0 0 16px 0; }
.icon-small { width: 20px; height: 20px; color: #111827; }
.announcement-list { list-style: none; padding: 0; margin: 0; }
.announcement-list li { display: flex; align-items: flex-start; gap: 12px; padding: 12px 0; border-bottom: 1px solid #f3f4f6; }
.announcement-list li:last-child { border-bottom: none; padding-bottom: 0; }
.dot { width: 6px; height: 6px; background-color: #111827; border-radius: 50%; margin-top: 8px; flex-shrink: 0; }
.notice-loading, .notice-empty { text-align: center; padding: 20px 0; font-size: 13px; color: #9ca3af; }
.notice-text { flex: 1; font-size: 14px; color: #4b5563; text-decoration: none; line-height: 1.5; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; min-width: 0; }
.notice-text:hover { color: #111827; text-decoration: underline; }
.notice-date { font-size: 12px; color: #9ca3af; white-space: nowrap; }

/* 上传弹窗样式 */
.modal-overlay { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background-color: rgba(17, 24, 39, 0.6); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal-content { background: #ffffff; width: 100%; max-width: 500px; border-radius: 16px; padding: 32px; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04); }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.modal-header h3 { margin: 0; font-size: 20px; font-weight: 600; color: #111827; }
.close-btn { background: none; border: none; font-size: 24px; color: #9ca3af; cursor: pointer; transition: color 0.2s; line-height: 1; }
.close-btn:hover { color: #111827; }

.upload-tips {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background-color: #fef3c7;
  border: 1px solid #fbbf24;
  border-radius: 8px;
  margin-bottom: 20px;
}
.tip-icon { font-size: 18px; }
.tip-text { font-size: 14px; color: #92400e; font-weight: 500; }

.upload-form { display: flex; flex-direction: column; gap: 20px; }
.form-group { display: flex; flex-direction: column; gap: 8px; }
.form-group label { font-size: 14px; font-weight: 500; color: #374151; }
.required { color: #ef4444; }
.form-group input[type="text"], .form-group textarea { width: 100%; padding: 12px 16px; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 14px; font-family: inherit; outline: none; transition: border-color 0.2s; box-sizing: border-box; background-color: #f9fafb; }
.form-group input[type="text"]:focus, .form-group textarea:focus { border-color: #111827; background-color: #ffffff; box-shadow: 0 0 0 1px #111827; }
.form-group textarea { resize: vertical; }
.tag-hint { font-weight: 400; font-size: 12px; color: #9ca3af; }
.tag-selector { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-chip { padding: 4px 12px; background-color: #f3f4f6; border: 1px solid #e5e7eb; border-radius: 14px; font-size: 12px; color: #4b5563; cursor: pointer; transition: all 0.2s; user-select: none; }
.tag-chip:hover { border-color: #9ca3af; }
.tag-chip.active { background-color: #111827; color: #ffffff; border-color: #111827; }
.custom-tag-row { margin-top: 4px; }
.custom-tag-row input { width: 100%; padding: 8px 12px; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 13px; outline: none; box-sizing: border-box; background-color: #f9fafb; }
.custom-tag-row input:focus { border-color: #111827; }
.selected-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.selected-tag { display: flex; align-items: center; gap: 4px; padding: 4px 10px; background-color: #111827; color: #ffffff; border-radius: 14px; font-size: 12px; }
.remove-tag-btn { background: none; border: none; color: #9ca3af; font-size: 14px; cursor: pointer; padding: 0; line-height: 1; }
.remove-tag-btn:hover { color: #ffffff; }

.file-upload-box { border: 2px dashed #d1d5db; border-radius: 8px; padding: 32px; text-align: center; cursor: pointer; background-color: #f9fafb; transition: all 0.2s; display: flex; flex-direction: column; align-items: center; gap: 8px; }
.file-upload-box:hover { border-color: #9ca3af; background-color: #f3f4f6; }
.upload-icon { width: 32px; height: 32px; color: #9ca3af; }
.upload-text { font-size: 14px; color: #6b7280; }
.file-selected { display: flex; flex-direction: column; gap: 4px; color: #111827; font-weight: 500; }
.file-size { font-size: 12px; color: #6b7280; font-weight: normal; }

.modal-footer { display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; }
.btn-cancel { padding: 10px 20px; background: transparent; border: 1px solid #d1d5db; border-radius: 8px; color: #374151; font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.2s; }
.btn-cancel:hover { background-color: #f3f4f6; }
.btn-submit { padding: 10px 24px; background: #111827; border: none; border-radius: 8px; color: #ffffff; font-size: 14px; font-weight: 500; cursor: pointer; transition: background-color 0.2s; }
.btn-submit:hover:not(:disabled) { background-color: #374151; }
.btn-submit:disabled { background-color: #9ca3af; cursor: not-allowed; }

/* 弹窗动画 */
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.3s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-active .modal-content, .modal-fade-leave-active .modal-content { transition: transform 0.3s ease; }
.modal-fade-enter-from .modal-content, .modal-fade-leave-to .modal-content { transform: scale(0.95) translateY(20px); }

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 24px;
  border-top: 1px solid #f3f4f6;
}

.page-btn {
  padding: 8px 16px;
  background-color: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: #111827;
  color: #111827;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-num {
  min-width: 40px;
  text-align: center;
}

.page-num.active {
  background-color: #111827;
  color: #ffffff;
  border-color: #111827;
}

.page-info {
  font-size: 14px;
  color: #6b7280;
  margin-left: 8px;
}

@media (max-width: 768px) {
  .grid-container { grid-template-columns: 1fr; }
  .hero-banner { padding: 0 24px; height: 180px; }
  .banner-content h1 { font-size: 24px; }
}
</style>