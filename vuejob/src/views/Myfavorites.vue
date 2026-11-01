<template>
  <div class="favorites-page">
    <div class="page-header">
      <h2>我的收藏</h2>
      <p>查看您收藏的优质资源</p>
    </div>

    <div class="favorites-container">
      <!-- 空状态 -->
      <div v-if="favorites.length === 0 && !isLoading && !apiUnavailable" class="empty-state">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="empty-icon">
          <path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" />
        </svg>
        <p>暂无收藏的资源</p>
        <button class="browse-btn" @click="goToHome">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="btn-icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
          </svg>
          去资源大厅浏览
        </button>
      </div>

      <!-- 收藏功能开发中提示 -->
      <div v-if="apiUnavailable && !isLoading" class="unavailable-state">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="unavailable-icon">
          <path stroke-linecap="round" stroke-linejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
        </svg>
        <p>收藏功能正在开发中，敬请期待</p>
        <p class="unavailable-sub">您可以先去资源大厅浏览和下载喜欢的资源</p>
        <button class="browse-btn" @click="goToHome">去资源大厅浏览</button>
      </div>

      <!-- Loading -->
      <div v-else-if="isLoading" class="loading-state">
        <div class="loading-spinner-large"></div>
        <p>正在加载收藏...</p>
      </div>

      <!-- 收藏列表 -->
      <div v-else class="favorites-list">
        <div v-for="fav in favorites" :key="fav.id" class="favorite-card">
          <div class="card-main">
            <div class="author-info">
              <img :src="fav.authorAvatar" alt="avatar" class="author-avatar" @error="handleAvatarError" />
              <div class="author-details">
                <span class="author-name">{{ fav.author }}</span>
                <span class="publish-date">{{ fav.collectTime }}</span>
              </div>
            </div>

            <h3 class="resource-title" @click="viewResource(fav)">{{ fav.title }}</h3>
            <p class="resource-desc">{{ fav.desc }}</p>

            <div class="resource-tags">
              <span class="tag" v-for="tag in fav.tags" :key="tag">{{ tag }}</span>
            </div>
          </div>

          <div class="card-actions">
            <button class="action-btn view-btn" @click="viewResource(fav)">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="icon">
                <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              查看详情
            </button>
            <button class="action-btn remove-btn" @click="removeFavorite(fav.id)">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="icon">
                <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
              </svg>
              取消收藏
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { showToast } from '@/utils/toast'
import { getAvatarUrl } from '@/utils/config'

const router = useRouter()
const isLoading = ref(false)
const favorites = ref([])
// 后端收藏接口尚未实现时显示开发中提示（待后端实现 /myFavorites 和 /removeFavorite 接口后移除）
const apiUnavailable = ref(false)

onMounted(() => {
  fetchFavorites()
})

const fetchFavorites = async () => {
  isLoading.value = true
  try {
    const res = await request.get('/myFavorites')
    if (res.code === 200) {
      const records = res.data.records || res.data || []
      favorites.value = records.map(item => ({
        id: item.id,
        title: item.fileName || item.title,
        desc: item.description || item.desc || '',
        author: item.authorName || item.author || '匿名用户',
        authorAvatar: getAvatarUrl(item.authorAvatar),
        fileUrl: item.fileUrl || '',
        tags: item.tags ? (Array.isArray(item.tags) ? item.tags : item.tags.split(',')) : [],
        collectTime: item.collectTime || item.createTime || '',
        authorId: item.userid || item.userId || '',
        downloadsCount: item.downloadsCount || 0,
        likesCount: item.likesCount || 0,
        commentsCount: item.commentsCount || 0
      }))
    }
  } catch (error) {
    // 后端接口未就绪时显示开发中提示
    console.error('获取收藏异常（后端接口可能未就绪）：', error)
    apiUnavailable.value = true
  } finally {
    isLoading.value = false
  }
}

const handleAvatarError = (event) => {
  event.target.src = getAvatarUrl(null)
}

const goToHome = () => {
  router.push('/sys/home')
}

const viewResource = (fav) => {
  router.push({
    name: 'ResourceDetail',
    params: { id: fav.id },
    query: {
      fileName: fav.title,
      fileUrl: fav.fileUrl,
      author: fav.author,
      avatar: fav.authorAvatar,
      authorId: fav.authorId,
      time: fav.collectTime,
      description: fav.desc,
      downloadsCount: fav.downloadsCount,
      likesCount: fav.likesCount,
      commentsCount: fav.commentsCount,
      tags: fav.tags ? fav.tags.join(',') : ''
    }
  })
}

const removeFavorite = async (id) => {
  try {
    // 使用 POST + params 方式，与后端 @RequestParam 风格保持一致
    const res = await request.post('/removeFavorite', null, { params: { fileId: id } })
    if (res.code === 200) {
      const index = favorites.value.findIndex(f => f.id === id)
      if (index !== -1) {
        const name = favorites.value[index].title
        favorites.value.splice(index, 1)
        showToast(`已取消收藏: ${name}`, 'success')
      }
    } else {
      showToast(res.message || '取消收藏失败', 'error')
    }
  } catch (error) {
    console.error('取消收藏异常：', error)
    showToast('取消收藏失败，请检查网络', 'error')
  }
}
</script>

<style scoped>
.favorites-page {
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

.favorites-container {
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

/* 功能开发中提示 */
.unavailable-state {
  padding: 80px 24px;
  text-align: center;
  color: #9ca3af;
}

.unavailable-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  color: #f59e0b;
  opacity: 0.6;
}

.unavailable-state p {
  margin: 0 0 8px 0;
  font-size: 15px;
  font-weight: 500;
  color: #6b7280;
}

.unavailable-sub {
  font-size: 13px !important;
  font-weight: 400 !important;
  color: #9ca3af !important;
  margin-bottom: 24px !important;
}

.browse-btn {
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

.browse-btn:hover {
  background-color: #374151;
}

.btn-icon {
  width: 16px;
  height: 16px;
}

.favorites-list {
  display: flex;
  flex-direction: column;
  padding: 24px;
  gap: 16px;
}

.favorite-card {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  gap: 24px;
  transition: box-shadow 0.2s, transform 0.2s;
}

.favorite-card:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.card-main {
  flex: 1;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.author-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #f3f4f6;
  object-fit: cover;
}

.author-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-name {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.publish-date {
  font-size: 12px;
  color: #9ca3af;
}

.resource-title {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  cursor: pointer;
  transition: color 0.2s;
}

.resource-title:hover {
  color: #4b5563;
}

.resource-desc {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: #6b7280;
  line-height: 1.6;
}

.resource-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag {
  background-color: #e5e7eb;
  color: #4b5563;
  padding: 4px 12px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 500;
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  white-space: nowrap;
}

.view-btn {
  background-color: #111827;
  color: #ffffff;
  border: none;
}

.view-btn:hover {
  background-color: #374151;
}

.remove-btn {
  background-color: transparent;
  color: #ef4444;
  border: 1px solid #fecaca;
}

.remove-btn:hover {
  background-color: #fef2f2;
}

.icon {
  width: 16px;
  height: 16px;
}

@media (max-width: 768px) {
  .favorite-card {
    flex-direction: column;
  }

  .card-actions {
    flex-direction: row;
  }
}
</style>