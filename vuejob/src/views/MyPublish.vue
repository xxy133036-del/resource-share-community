<template>
  <div class="my-posts-page">
    <div class="page-header">
      <h2>我的发布</h2>
      <p>管理您上传的资源文件</p>
    </div>

    <div class="posts-container">
      <!-- Loading 状态 -->
      <div v-if="isLoading" class="loading-state">
        <div class="loading-spinner-large"></div>
        <p>正在加载...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="posts.length === 0" class="empty-state">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="empty-icon">
          <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
        </svg>
        <p>暂无发布的资源</p>
        <button class="create-btn" @click="goToUpload">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="btn-icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          发布第一个资源
        </button>
      </div>

      <!-- 列表 -->
      <div v-else class="posts-grid">
        <div v-for="post in posts" :key="post.id" class="post-card" @click="viewDetail(post)">
          <div class="post-status" :class="post.status">
            {{ post.status === 'approved' ? '已通过' : post.status === 'pending' ? '审核中' : '已下架' }}
          </div>

          <div class="post-content">
            <h3 class="post-title">{{ post.fileName }}</h3>
            <p class="post-desc">{{ post.description }}</p>

            <div class="post-stats">
              <div class="stat-item">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="stat-icon">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
                </svg>
                <span>{{ post.downloadsCount || 0 }}</span>
              </div>

              <div class="stat-item">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="stat-icon">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6.633 10.5c.806 0 1.533-.446 2.031-1.08a9.041 9.041 0 012.861-2.4c.723-.384 1.35-.956 1.653-1.715a4.498 4.498 0 00.322-1.672V3a.75.75 0 01.75-.75A2.25 2.25 0 0116.5 4.5c0 1.152-.26 2.247-.723 3.218-.266.558.107 1.282.725 1.282h3.126c1.026 0 1.945.694 2.054 1.715.045.422.068.85.068 1.285a11.95 11.95 0 01-2.649 7.521c-.388.482-.987.729-1.605.729H13.48c-.483 0-.964-.078-1.423-.23l-3.114-1.04a4.501 4.501 0 00-1.423-.23H5.904M14.25 9h2.25M5.904 18.75c.083.205.173.405.27.602.197.4-.078.898-.523.898h-.908c-.889 0-1.713-.518-1.972-1.368a12 12 0 01-.521-3.507c0-1.553.295-3.036.831-4.398C3.387 10.203 4.167 9.75 5 9.75h1.053c.472 0 .745.556.5.96a8.958 8.958 0 00-1.302 4.665c0 1.194.232 2.333.654 3.375z" />
                </svg>
                <span>{{ post.likesCount || 0 }}</span>
              </div>

              <div class="stat-item">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="stat-icon">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 01.865-.501 48.172 48.172 0 003.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" />
                </svg>
                <span>{{ post.commentsCount || 0 }}</span>
              </div>
            </div>

            <div class="post-time">
              发布于 {{ post.createTime }}
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="pagination">
        <button
            class="page-btn"
            :disabled="currentPage === 1"
            @click="goToPage(currentPage - 1)"
        >上一页</button>
        <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
        <button
            class="page-btn"
            :disabled="currentPage >= totalPages"
            @click="goToPage(currentPage + 1)"
        >下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { showToast } from '@/utils/toast'
import { formatTime } from '@/utils/format'
import { getAvatarUrl } from '@/utils/config'

const router = useRouter()

const posts = ref([])
const isLoading = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)
const pageSize = 12

onMounted(() => {
  fetchMyPosts()
})

const fetchMyPosts = async (page = 1) => {
  isLoading.value = true
  currentPage.value = page

  try {
    const res = await request.get('/myFiles', {
      params: {
        pageNum: page,
        pageSize: pageSize
      }
    })

    if (res.code === 200) {
      const pageData = res.data
      const records = pageData.records || []
      totalPages.value = pageData.pages || 1

      posts.value = records.map(item => ({
        id: item.id,
        fileName: item.fileName,
        description: item.description || '暂无描述',
        fileUrl: item.fileUrl,
        userid: item.userid,
        // 后端 status: 0=默认/待审核  1=下架  2=通过
        status: item.status === 2 ? 'approved' : item.status === 1 ? 'removed' : 'pending',
        downloadsCount: item.downloadsCount || 0,
        likesCount: item.likesCount || 0,
        commentsCount: item.commentsCount || 0,
        createTime: formatTime(item.createTime),
        authorName: item.authorName || '匿名用户',
        authorAvatar: item.authorAvatar,
        tags: item.tags ? item.tags.split(',') : []
      }))
    }
  } catch (error) {
    console.error('获取我的发布异常：', error)
    showToast('加载失败，请重试', 'error')
  } finally {
    isLoading.value = false
  }
}

const goToPage = (page) => {
  if (page < 1 || page > totalPages.value) return
  fetchMyPosts(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const goToUpload = () => {
  router.push('/sys/home')
  setTimeout(() => {
    showToast('请在首页点击"发布资源"按钮', 'success')
  }, 100)
}

const viewDetail = (post) => {
  router.push({
    name: 'ResourceDetail',
    params: { id: post.id },
    query: {
      fileName: post.fileName,
      fileUrl: post.fileUrl,
      author: post.authorName,
      avatar: getAvatarUrl(post.authorAvatar),
      authorId: post.userid,
      time: post.createTime,
      description: post.description,
      downloadsCount: post.downloadsCount,
      likesCount: post.likesCount,
      commentsCount: post.commentsCount,
      tags: post.tags ? post.tags.join(',') : ''
    }
  })
}
</script>

<style scoped>
.my-posts-page {
  min-height: calc(100vh - 120px);
}

.page-header {
  margin-bottom: 32px;
  text-align: left;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #111827;
}

.page-header p {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
}

.posts-container {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  min-height: 400px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  color: #9ca3af;
  gap: 16px;
}

.loading-spinner-large {
  width: 36px;
  height: 36px;
  border: 3px solid #e5e7eb;
  border-top-color: #111827;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.empty-state {
  padding: 80px 24px;
  text-align: center;
  color: #9ca3af;
}

.empty-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  opacity: 0.3;
}

.empty-state p {
  margin: 0 0 24px 0;
  font-size: 14px;
}

.create-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background-color: #111827;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.create-btn:hover {
  background-color: #374151;
}

.btn-icon {
  width: 16px;
  height: 16px;
}

.posts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  padding: 24px;
}

.post-card {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  transition: box-shadow 0.2s, transform 0.2s;
  position: relative;
  cursor: pointer;
}

.post-card:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.post-status {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.post-status.approved {
  background-color: #dcfce7;
  color: #166534;
}

.post-status.pending {
  background-color: #fef3c7;
  color: #92400e;
}

.post-status.removed {
  background-color: #f3f4f6;
  color: #6b7280;
}

.post-content {
  margin-top: 8px;
}

.post-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  line-height: 1.4;
  padding-right: 70px;
}

.post-desc {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: #6b7280;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #6b7280;
}

.stat-icon {
  width: 16px;
  height: 16px;
}

.post-time {
  font-size: 12px;
  color: #9ca3af;
}

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
  padding: 8px 20px;
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

.page-info {
  font-size: 14px;
  color: #6b7280;
}
</style>