<template>
  <div class="detail-page">
    <div class="detail-card">
      <!-- 返回按钮 -->
      <button class="back-btn" @click="goBack">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="back-icon">
          <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
        </svg>
        返回列表
      </button>

      <!-- 资源头部信息 -->
      <div class="resource-header">
        <div class="author-section">
          <img :src="resourceInfo.avatar" alt="author" class="author-avatar" />
          <div class="author-details">
            <span class="author-name">{{ resourceInfo.author }}</span>
            <span class="publish-time">{{ resourceInfo.time }}</span>
          </div>
        </div>

        <h1 class="resource-title">{{ resourceInfo.fileName }}</h1>
      </div>

      <!-- 资源统计信息 -->
      <div class="resource-stats">
        <div class="stat-item">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="stat-icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
          </svg>
          <span>{{ resourceInfo.downloadsCount || 0 }} 次下载</span>
        </div>
        <div class="stat-item clickable" :class="{ liked: resourceInfo._liked }" @click="handleLike">
          <svg xmlns="http://www.w3.org/2000/svg" :fill="resourceInfo._liked ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="stat-icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" />
          </svg>
          <span>{{ resourceInfo.likesCount || 0 }} 点赞</span>
        </div>
        <div class="stat-item">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="stat-icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 01.865-.501 48.172 48.172 0 003.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" />
          </svg>
          <span>{{ resourceInfo.commentsCount || 0 }} 评论</span>
        </div>
      </div>

      <!-- 资源标签 -->
      <div v-if="resourceInfo.tags && resourceInfo.tags.length > 0" class="resource-tags">
        <span class="tag" v-for="tag in resourceInfo.tags" :key="tag">{{ tag }}</span>
      </div>

      <!-- 资源描述 -->
      <div class="resource-description">
        <h3 class="section-title">资源介绍</h3>
        <p class="description-text">{{ resourceInfo.description || '暂无详细描述' }}</p>
      </div>

      <!-- 下载按钮区域 -->
      <div class="download-section">
        <button class="download-btn" @click="handleDownload" :disabled="isDownloading">
          <svg v-if="!isDownloading" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="btn-icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
          </svg>
          <svg v-else class="loading-spinner" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" opacity="0.25"></circle>
            <path d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" fill="currentColor" opacity="0.75"></path>
          </svg>
          {{ isDownloading ? '下载中...' : '立即下载' }}
        </button>
        <p class="download-hint">下载将消耗 5 积分</p>
      </div>
    </div>

    <!-- 评论区 -->
    <div class="comments-section">
      <div class="comments-card">
        <h3 class="section-title">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="section-icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 01.865-.501 48.172 48.172 0 003.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" />
          </svg>
          评论 ({{ comments.length }})
        </h3>

        <!-- 评论表单 -->
        <div class="comment-form">
          <textarea
              v-model="newComment"
              placeholder="发表你的看法..."
              rows="3"
              maxlength="500"
              :disabled="isSubmittingComment"
          ></textarea>
          <div class="comment-form-footer">
            <span class="char-count">{{ newComment.length }}/500</span>
            <button
                class="submit-comment-btn"
                :disabled="!newComment.trim() || isSubmittingComment"
                @click="submitComment"
            >
              {{ isSubmittingComment ? '发送中...' : '发表评论' }}
            </button>
          </div>
        </div>

        <!-- 评论列表 -->
        <div class="comment-list">
          <!-- Loading -->
          <div v-if="isLoadingComments" class="loading-comments">
            <p>正在加载评论...</p>
          </div>

          <!-- 空状态 -->
          <div v-else-if="commentTree.length === 0" class="empty-comments">
            <p>暂无评论，快来发表第一条评论吧！</p>
          </div>

          <!-- 评论树 -->
          <template v-else v-for="comment in commentTree" :key="comment.id">
            <div class="comment-item" :id="'comment-' + comment.id">
              <img
                  :src="comment.authorAvatar
                    ? getAvatarUrl(comment.authorAvatar)
                    : getAvatarUrl(null)"
                  alt="avatar"
                  class="comment-avatar"
              />
              <div class="comment-body">
                <div class="comment-header">
                  <span class="comment-author">{{ comment.authorName || '匿名用户' }}</span>
                  <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                </div>
                <p class="comment-content">{{ comment.content }}</p>

                <button
                    class="reply-toggle-btn"
                    @click="toggleReplyBox(comment.id, comment.authorName)"
                >
                  💬 回复
                </button>

                <!-- 回复输入框 -->
                <div v-if="activeReplyId === comment.id" class="reply-form">
                  <textarea
                      v-model="replyContents[activeReplyId]"
                      :placeholder="'回复 ' + replyTargetName + '...'"
                      rows="2"
                      maxlength="300"
                  ></textarea>
                  <div class="reply-form-footer">
                    <button class="cancel-reply-btn" @click="cancelReply">取消</button>
                    <button
                        class="submit-reply-btn"
                        :disabled="!(replyContents[activeReplyId] || '').trim()"
                        @click="submitReply(comment.id)"
                    >回复</button>
                  </div>
                </div>

                <!-- 所有回复扁平展示（同一列对齐） -->
                <div v-if="getFlatReplies(comment).length > 0" class="replies">
                  <div
                      v-for="reply in getFlatReplies(comment)"
                      :key="reply.id"
                      :id="'comment-' + reply.id"
                      class="reply-item"
                  >
                    <img
                        :src="reply.authorAvatar
                          ? getAvatarUrl(reply.authorAvatar)
                          : getAvatarUrl(null)"
                        alt="avatar"
                        class="reply-avatar"
                    />
                    <div class="reply-body">
                      <div class="reply-header">
                        <span class="reply-author">{{ reply.authorName || '匿名用户' }}</span>
                        <span class="reply-to">
                          回复
                          <span class="reply-target">@{{ getAuthorNameById(reply.parentId) }}</span>
                        </span>
                        <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                      </div>
                      <p class="reply-content">{{ reply.content }}</p>

                      <button
                          class="reply-toggle-btn"
                          @click="toggleReplyBox(reply.id, reply.authorName)"
                      >
                        💬 回复
                      </button>

                      <!-- 回复输入框 -->
                      <div v-if="activeReplyId === reply.id" class="reply-form">
                        <textarea
                            v-model="replyContents[activeReplyId]"
                            :placeholder="'回复 ' + replyTargetName + '...'"
                            rows="2"
                            maxlength="300"
                        ></textarea>
                        <div class="reply-form-footer">
                          <button class="cancel-reply-btn" @click="cancelReply">取消</button>
                          <button
                              class="submit-reply-btn"
                              :disabled="!(replyContents[activeReplyId] || '').trim()"
                              @click="submitReply(reply.id)"
                          >回复</button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- 评论分页 -->
        <div v-if="commentTotalPages > 1" class="comment-pagination">
          <button
              class="page-btn"
              :disabled="commentPage === 1"
              @click="loadComments(commentPage - 1)"
          >上一页</button>
          <span class="page-info">{{ commentPage }} / {{ commentTotalPages }}</span>
          <button
              class="page-btn"
              :disabled="commentPage >= commentTotalPages"
              @click="loadComments(commentPage + 1)"
          >下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { showToast } from '@/utils/toast'
import { formatTime } from '@/utils/format'
import { getAvatarUrl } from '@/utils/config'

const route = useRoute()
const router = useRouter()
const isDownloading = ref(false)

// --- 资源信息 ---
const resourceInfo = ref({
  id: route.params.id,
  fileName: route.query.fileName || '',
  fileUrl: route.query.fileUrl || '',
  author: route.query.author || '匿名用户',
  authorId: route.query.authorId || '',
  avatar: route.query.avatar || 'https://api.dicebear.com/7.x/notionists/svg?seed=Felix',
  time: route.query.time || '',
  description: route.query.description || '',
  downloadsCount: parseInt(route.query.downloadsCount) || 0,
  likesCount: parseInt(route.query.likesCount) || 0,
  commentsCount: parseInt(route.query.commentsCount) || 0,
  tags: route.query.tags ? route.query.tags.split(',') : []
})

// --- 评论状态 ---
const comments = ref([])           // 保留兼容
const commentTree = ref([])        // 树形结构
const allCommentMap = ref({})      // ID → 评论映射（用于查找作者名）
const newComment = ref('')
const isSubmittingComment = ref(false)
const isLoadingComments = ref(false)
const commentPage = ref(1)
const commentTotalPages = ref(1)
const commentPageSize = 10

// 回复状态
const activeReplyId = ref(null)
const replyTargetName = ref('')
const replyContents = ref({})

onMounted(() => {
  loadComments(1)
  fetchLikeStatus()
  // 从消息页跳转过来时，定位到对应评论
  const commentId = route.query.commentId
  if (commentId) {
    setTimeout(() => {
      const el = document.getElementById('comment-' + commentId)
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' })
        el.classList.add('comment-highlight')
        setTimeout(() => el.classList.remove('comment-highlight'), 2000)
      }
    }, 500)
  }
})

const fetchLikeStatus = async () => {
  try {
    const res = await request.get('/checkLike', {
      params: { fileId: resourceInfo.value.id }
    })
    if (res.code === 200) {
      resourceInfo.value._liked = res.data
    }
  } catch (error) {
    console.error('获取点赞状态异常：', error)
  }
}

const loadComments = async (page = 1) => {
  isLoadingComments.value = true
  commentPage.value = page

  try {
    const res = await request.get('/getCommentListWithAuthor', {
      params: {
        targetId: resourceInfo.value.id,
        pageNum: page,
        pageSize: commentPageSize
      }
    })

    if (res.code === 200) {
      const pageData = res.data
      const records = pageData.records || []
      commentTotalPages.value = pageData.pages || 1

      // 构建 ID → 评论 映射
      const idMap = {}
      records.forEach(item => {
        idMap[item.id] = { ...item, children: [] }
      })
      allCommentMap.value = idMap

      // 构建递归树
      const roots = []
      records.forEach(item => {
        const node = idMap[item.id]
        const parentId = item.parentId
        if (parentId && idMap[parentId]) {
          idMap[parentId].children.push(node)
        } else {
          roots.push(node)
        }
      })

      commentTree.value = roots
      comments.value = records  // 保留兼容
      resourceInfo.value.commentsCount = pageData.total || 0
    }
  } catch (error) {
    console.error('获取评论异常：', error)
  } finally {
    isLoadingComments.value = false
  }
}

// 根据 ID 查找作者名（用于显示"回复 @xxx"）
const getAuthorNameById = (id) => {
  const node = allCommentMap.value[id]
  return node ? (node.authorName || '匿名用户') : '未知用户'
}

// 扁平化某个顶级评论下的所有后代回复（BFS 按时间排序，同列对齐）
const getFlatReplies = (rootComment) => {
  const result = []
  const queue = [...(rootComment.children || [])]
  while (queue.length > 0) {
    const node = queue.shift()
    result.push(node)
    if (node.children && node.children.length > 0) {
      queue.push(...node.children)
    }
  }
  // 按创建时间排序
  result.sort((a, b) => (a.createTime || '').localeCompare(b.createTime || ''))
  return result
}

const submitComment = async () => {
  if (!newComment.value.trim() || isSubmittingComment.value) return

  isSubmittingComment.value = true
  try {
    const res = await request.post('/commentResources', {
      targetId: resourceInfo.value.id,
      content: newComment.value.trim(),
      parentId: 0
    })

    if (res.code === 200) {
      showToast('评论发表成功！', 'success')
      newComment.value = ''
      loadComments(1)  // 刷新评论列表
    } else {
      showToast(res.message || '评论发表失败', 'error')
    }
  } catch (error) {
    console.error('评论异常：', error)
    showToast('评论发表失败，请检查网络', 'error')
  } finally {
    isSubmittingComment.value = false
  }
}

const toggleReplyBox = (commentId, authorName) => {
  if (activeReplyId.value === commentId) {
    cancelReply()
    return
  }
  activeReplyId.value = commentId
  replyTargetName.value = authorName || '匿名用户'
  replyContents.value[commentId] = ''
}

const cancelReply = () => {
  if (activeReplyId.value) {
    delete replyContents.value[activeReplyId.value]
  }
  activeReplyId.value = null
  replyTargetName.value = ''
}

const submitReply = async (parentCommentId) => {
  const text = (replyContents.value[activeReplyId.value] || '').trim()
  if (!text) return

  try {
    const res = await request.post('/commentComment', {
      targetId: resourceInfo.value.id,
      content: text,
      parentId: parentCommentId
    })

    if (res.code === 200) {
      showToast('回复成功！', 'success')
      cancelReply()
      loadComments(commentPage.value)  // 刷新当前页评论
    } else {
      showToast(res.message || '回复失败', 'error')
    }
  } catch (error) {
    console.error('回复异常：', error)
    showToast('回复失败，请检查网络', 'error')
  }
}

const handleLike = async () => {
  const wasLiked = resourceInfo.value._liked
  resourceInfo.value._liked = !wasLiked
  resourceInfo.value.likesCount = (resourceInfo.value.likesCount || 0) + (wasLiked ? -1 : 1)

  try {
    const url = wasLiked ? '/removeLike' : '/addLike'
    await request.post(url, {}, { params: { fileId: resourceInfo.value.id } })
  } catch (error) {
    resourceInfo.value._liked = wasLiked
    resourceInfo.value.likesCount = (resourceInfo.value.likesCount || 0) + (wasLiked ? 1 : -1)
    console.error('点赞操作异常：', error)
  }
}

const goBack = () => {
  router.back()
}

const handleDownload = async () => {
  if (!resourceInfo.value.fileUrl) {
    showToast('文件地址不存在', 'error')
    return
  }

  if (isDownloading.value) return

  isDownloading.value = true

  try {
    // 使用 axios 发起下载请求（自动携带 Authorization header）
    // 注意：interceptor 对 blob 类型返回完整的 axios response 对象
    const response = await request.get(`/downloadFile/${resourceInfo.value.fileUrl}`, {
      responseType: 'blob'
    })

    // 从响应头获取文件名
    const contentDisposition = response.headers?.['content-disposition']
    let downloadFileName = resourceInfo.value.fileName || 'download'
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

    showToast('下载成功！', 'success')
    resourceInfo.value.downloadsCount++
  } catch (error) {
    console.error('下载异常：', error)
    // 错误消息已由 interceptor 处理并 toast，这里做静默处理
  } finally {
    isDownloading.value = false
  }
}
</script>

<style scoped>
.detail-page {
  min-height: calc(100vh - 120px);
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
}

.detail-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
  margin-bottom: 32px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 24px;
}

.back-btn:hover {
  background-color: #f3f4f6;
  border-color: #d1d5db;
}

.back-icon {
  width: 16px;
  height: 16px;
}

.resource-header {
  margin-bottom: 32px;
}

.author-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.author-avatar {
  width: 40px;
  height: 40px;
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

.publish-time {
  font-size: 12px;
  color: #9ca3af;
}

.resource-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  line-height: 1.3;
}

.resource-stats {
  display: flex;
  gap: 24px;
  padding: 20px 0;
  border-top: 1px solid #f3f4f6;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 32px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #6b7280;
}

.stat-icon {
  width: 18px;
  height: 18px;
}

.stat-item.clickable {
  cursor: pointer; transition: color 0.2s;
}
.stat-item.clickable:hover { color: #e11d48; }
.stat-item.liked { color: #e11d48; }

/* 标签 */
.resource-tags { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 24px; }
.resource-tags .tag { background-color: #f3f4f6; color: #4b5563; padding: 4px 12px; border-radius: 14px; font-size: 12px; font-weight: 500; }

/* 评论高亮（从消息页跳转过来） */
.comment-highlight {
  animation: commentFlash 2s ease;
  background-color: #fef3c7 !important;
  border-radius: 8px;
}
@keyframes commentFlash {
  0% { background-color: #fef3c7; }
  100% { background-color: transparent; }
}

.resource-description {
  margin-bottom: 32px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  width: 22px;
  height: 22px;
}

.description-text {
  font-size: 15px;
  color: #4b5563;
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
}

.download-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 32px;
  background-color: #f9fafb;
  border-radius: 12px;
  border: 1px dashed #e5e7eb;
}

.download-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 48px;
  background-color: #111827;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 200px;
}

.download-btn:hover:not(:disabled) {
  background-color: #374151;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.download-btn:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
}

.btn-icon {
  width: 20px;
  height: 20px;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.download-hint {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

/* ===================== 评论区样式 ===================== */
.comments-section {
  margin-top: 0;
}

.comments-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
}

/* 评论表单 */
.comment-form {
  margin-bottom: 32px;
}

.comment-form textarea {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
  background-color: #f9fafb;
}

.comment-form textarea:focus {
  border-color: #111827;
  background-color: #ffffff;
  box-shadow: 0 0 0 1px #111827;
}

.comment-form textarea:disabled {
  background-color: #f3f4f6;
  cursor: not-allowed;
}

.comment-form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.char-count {
  font-size: 12px;
  color: #9ca3af;
}

.submit-comment-btn {
  padding: 8px 20px;
  background-color: #111827;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.submit-comment-btn:hover:not(:disabled) {
  background-color: #374151;
}

.submit-comment-btn:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
}

/* 评论列表 */
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.loading-comments, .empty-comments {
  text-align: center;
  padding: 40px 24px;
  color: #9ca3af;
  font-size: 14px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 20px 0;
  border-bottom: 1px solid #f3f4f6;
}

.comment-item:first-child {
  padding-top: 0;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #f3f4f6;
  flex-shrink: 0;
  object-fit: cover;
}

.comment-body {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.comment-author {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.comment-time {
  font-size: 12px;
  color: #9ca3af;
}

.comment-content {
  margin: 0;
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
  white-space: pre-wrap;
}

.reply-toggle-btn {
  margin-top: 8px;
  padding: 0;
  background: none;
  border: none;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  transition: color 0.2s;
}

.reply-toggle-btn:hover {
  color: #111827;
}

/* 回复列表 */
.replies {
  margin-top: 12px;
  padding-left: 16px;
  border-left: 2px solid #f3f4f6;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply-item {
  display: flex;
  gap: 10px;
}

.reply-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: #f3f4f6;
  flex-shrink: 0;
  object-fit: cover;
}

.reply-body {
  flex: 1;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.reply-author {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.reply-time {
  font-size: 11px;
  color: #9ca3af;
  margin-left: auto;
}

/* 回复对象标签 */
.reply-to {
  font-size: 12px;
  color: #9ca3af;
  margin-left: 6px;
  flex-shrink: 0;
}

.reply-target {
  color: #4b5563;
  font-weight: 500;
}

.reply-content {
  margin: 0;
  font-size: 13px;
  color: #4b5563;
  line-height: 1.5;
}

/* 回复表单 */
.reply-form {
  margin-top: 10px;
}

.reply-form textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  font-family: inherit;
  resize: vertical;
  outline: none;
  box-sizing: border-box;
  background-color: #f9fafb;
  transition: border-color 0.2s;
}

.reply-form textarea:focus {
  border-color: #111827;
  background-color: #ffffff;
}

.reply-form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.cancel-reply-btn {
  padding: 6px 14px;
  background: transparent;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-reply-btn:hover {
  background-color: #f3f4f6;
}

.submit-reply-btn {
  padding: 6px 14px;
  background-color: #111827;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.submit-reply-btn:hover:not(:disabled) {
  background-color: #374151;
}

.submit-reply-btn:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
}

/* 评论分页 */
.comment-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f3f4f6;
}

.page-btn {
  padding: 6px 16px;
  background-color: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
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
  font-size: 13px;
  color: #6b7280;
}

@media (max-width: 768px) {
  .detail-card, .comments-card {
    padding: 24px;
  }

  .resource-title {
    font-size: 22px;
  }

  .resource-stats {
    flex-direction: column;
    gap: 12px;
  }
}
</style>