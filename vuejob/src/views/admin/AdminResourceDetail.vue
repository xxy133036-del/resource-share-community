<template>
  <div class="detail-page">
    <button class="back-btn" @click="goBack">
      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="back-icon">
        <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
      </svg>
      返回审核列表
    </button>

    <!-- 加载中 -->
    <div v-if="isLoading" class="loading-state">加载中...</div>

    <!-- 资源详情 -->
    <div class="detail-card" v-else-if="resource">
      <h1 class="resource-title">{{ resource.fileName }}</h1>

      <div class="meta-row">
        <span class="meta-item">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="meta-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" /></svg>
          {{ resource.authorName || '匿名用户' }}
        </span>
        <span class="meta-item">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="meta-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
          {{ resource.createTime }}
        </span>
      </div>

      <!-- 统计 + 状态 -->
      <div class="stats-row">
        <span class="stat-item">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="stat-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" /></svg>
          {{ resource.downloadsCount || 0 }} 次下载
        </span>
        <span class="stat-item">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="stat-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" /></svg>
          {{ resource.likesCount || 0 }} 点赞
        </span>
        <span class="stat-item">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="stat-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 01.865-.501 48.172 48.172 0 003.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" /></svg>
          {{ resource.commentsCount || 0 }} 评论
        </span>
        <span :class="['status-badge', statusClass(resource.status)]">{{ statusText(resource.status) }}</span>
      </div>

      <!-- 标签 -->
      <div v-if="resource.tags" class="tags-row">
        <span class="tag" v-for="t in splitTags(resource.tags)" :key="t">{{ t }}</span>
      </div>

      <!-- 描述 -->
      <div class="desc-section">
        <h3 class="section-title">资源介绍</h3>
        <p class="desc-text">{{ resource.description || '暂无描述' }}</p>
      </div>
    </div>

    <!-- 加载失败 -->
    <div v-else class="loading-state">未找到该资源</div>

    <!-- 评论区（只读） -->
    <div class="comments-card" v-if="resource">
      <h3 class="section-title">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="section-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 01.865-.501 48.172 48.172 0 003.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" /></svg>
        评论 ({{ comments.length }})
      </h3>

      <div v-if="isLoadingComments" class="loading-comments">加载评论中...</div>
      <div v-else-if="comments.length === 0" class="empty-comments">暂无评论</div>
      <template v-else>
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <div class="comment-header">
            <span class="comment-author">{{ c.authorName || '匿名用户' }}</span>
            <span class="comment-time">{{ c.createTime }}</span>
          </div>
          <p class="comment-content">{{ c.content }}</p>

          <!-- 回复 -->
          <div v-if="c.replies && c.replies.length > 0" class="replies">
            <div v-for="r in c.replies" :key="r.id" class="reply-item">
              <div class="reply-header">
                <span class="reply-author">{{ r.authorName || '匿名用户' }}</span>
                <span class="reply-time">{{ r.createTime }}</span>
              </div>
              <p class="reply-content">{{ r.content }}</p>
            </div>
          </div>
        </div>
      </template>

      <div v-if="commentTotalPages > 1" class="pagination">
        <button :disabled="commentPageNum === 1" @click="goCommentPage(commentPageNum - 1)">上一页</button>
        <span>{{ commentPageNum }} / {{ commentTotalPages }}</span>
        <button :disabled="commentPageNum >= commentTotalPages" @click="goCommentPage(commentPageNum + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import adminRequest from '@/utils/adminRequest'

const route = useRoute()
const router = useRouter()

const resource = ref(null)
const isLoading = ref(true)

// 评论
const comments = ref([])
const isLoadingComments = ref(false)
const commentPageNum = ref(1)
const commentTotalPages = ref(1)

onMounted(async () => {
  const fileUrl = route.query.fileUrl
  if (fileUrl) {
    try {
      const res = await adminRequest.get('/resourceDetail', { params: { fileUrl } })
      if (res.code === 200 && res.data) {
        resource.value = res.data
      }
    } catch (e) {
      console.error('获取资源详情失败', e)
    }
  }

  // query 参数兜底（兼容未传 fileUrl 的旧导航方式）
  if (!resource.value) {
    resource.value = {
      id: route.params.id,
      fileName: route.query.fileName || '',
      fileUrl: route.query.fileUrl || '',
      authorName: route.query.author || '',
      createTime: route.query.time || '',
      description: route.query.description || '',
      tags: route.query.tags || '',
      downloadsCount: parseInt(route.query.downloadsCount) || 0,
      likesCount: parseInt(route.query.likesCount) || 0,
      commentsCount: parseInt(route.query.commentsCount) || 0,
      status: parseInt(route.query.status) ?? 0
    }
  }

  isLoading.value = false

  if (resource.value && resource.value.id) {
    loadComments(1)
  }
})

const loadComments = async (page) => {
  isLoadingComments.value = true
  commentPageNum.value = page
  try {
    const res = await adminRequest.get('/resourceComments', {
      params: { targetId: resource.value.id, pageNum: page, pageSize: 10 }
    })
    if (res.code === 200) {
      const records = res.data?.records || []
      commentTotalPages.value = res.data?.pages || 1

      // 构建树形结构（顶级评论 + 回复）
      const map = {}
      records.forEach(item => { map[item.id] = { ...item, replies: [] } })
      const roots = []
      records.forEach(item => {
        if (item.parentId && map[item.parentId]) {
          map[item.parentId].replies.push(map[item.id])
        } else if (!item.parentId || item.parentId === 0) {
          roots.push(map[item.id])
        }
      })
      comments.value = roots
    }
  } catch (e) {
    console.error('加载评论失败', e)
  } finally {
    isLoadingComments.value = false
  }
}

const goCommentPage = (p) => loadComments(p)
const goBack = () => router.push('/admin/resources')

const splitTags = (tags) => {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  if (typeof tags === 'string') return tags.split(',').filter(t => t.trim())
  return []
}
const statusClass = (s) => s === 2 ? 'approved' : s === 1 ? 'rejected' : s === 0 ? 'pending' : ''
const statusText = (s) => s === 2 ? '已通过' : s === 1 ? '已下架' : s === 0 ? '待审核' : '未知'
</script>

<style scoped>
.detail-page {
  max-width: 900px;
  padding: 24px 0;
}

.back-btn {
  display: inline-flex; align-items: center; gap: 8px; padding: 8px 16px;
  background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 8px;
  font-size: 14px; color: #374151; cursor: pointer; font-family: inherit;
  margin-bottom: 24px; transition: all 0.2s;
}
.back-btn:hover { background: #f3f4f6; border-color: #d1d5db; }
.back-icon { width: 16px; height: 16px; }

.loading-state { text-align: center; padding: 60px; color: #9ca3af; font-size: 14px; }

.detail-card, .comments-card {
  background: #fff; border-radius: 14px; padding: 32px;
  border: 1px solid rgba(229,231,235,0.5); margin-bottom: 24px;
}

.resource-title { margin: 0 0 16px; font-size: 26px; font-weight: 700; color: #111827; }

.meta-row { display: flex; gap: 24px; margin-bottom: 20px; flex-wrap: wrap; }
.meta-item { display: flex; align-items: center; gap: 6px; font-size: 14px; color: #6b7280; }
.meta-icon { width: 16px; height: 16px; }

.stats-row { display: flex; gap: 24px; padding: 16px 0; border-top: 1px solid #f3f4f6; border-bottom: 1px solid #f3f4f6; margin-bottom: 20px; flex-wrap: wrap; align-items: center; }
.stat-item { display: flex; align-items: center; gap: 6px; font-size: 14px; color: #6b7280; }
.stat-icon { width: 18px; height: 18px; }

.status-badge { display: inline-block; padding: 4px 12px; border-radius: 14px; font-size: 12px; font-weight: 600; margin-left: auto; }
.status-badge.approved { background: #dcfce7; color: #166534; }
.status-badge.pending { background: #fef3c7; color: #92400e; }
.status-badge.rejected { background: #f3f4f6; color: #6b7280; }

.tags-row { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 24px; }
.tag { padding: 4px 12px; background: #f3f4f6; border-radius: 14px; font-size: 12px; color: #6b7280; }

.desc-section { margin-bottom: 0; }
.section-title { font-size: 17px; font-weight: 600; color: #111827; margin: 0 0 12px; display: flex; align-items: center; gap: 8px; }
.section-icon { width: 20px; height: 20px; }
.desc-text { font-size: 15px; color: #4b5563; line-height: 1.8; margin: 0; white-space: pre-wrap; }

/* 评论 */
.loading-comments, .empty-comments { text-align: center; padding: 40px; color: #9ca3af; font-size: 14px; }
.comment-item { padding: 16px 0; border-bottom: 1px solid #f3f4f6; }
.comment-item:first-child { padding-top: 0; }
.comment-header { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.comment-author { font-size: 14px; font-weight: 600; color: #374151; }
.comment-time { font-size: 12px; color: #9ca3af; }
.comment-content { margin: 0; font-size: 14px; color: #4b5563; line-height: 1.6; }

.replies { margin-top: 12px; padding-left: 16px; border-left: 2px solid #f3f4f6; display: flex; flex-direction: column; gap: 10px; }
.reply-item { }
.reply-header { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.reply-author { font-size: 13px; font-weight: 600; color: #374151; }
.reply-time { font-size: 11px; color: #9ca3af; }
.reply-content { margin: 0; font-size: 13px; color: #4b5563; line-height: 1.5; }

.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; padding: 20px 0 0; border-top: 1px solid #f3f4f6; }
.pagination button { padding: 8px 18px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 13px; color: #374151; cursor: pointer; font-family: inherit; transition: all 0.2s; }
.pagination button:hover:not(:disabled) { border-color: #111827; }
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.pagination span { font-size: 13px; color: #6b7280; }
</style>
