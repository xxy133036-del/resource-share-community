<template>
  <div class="messages-page">

    <!-- ========== 摘要视图 ========== -->
    <template v-if="viewMode === 'summary'">
      <div class="page-header">
        <h2>我的消息</h2>
        <p>查看点赞、评论和系统通知</p>
      </div>

      <div v-if="isLoadingSummary" class="loading-state">
        <div class="loading-spinner-large"></div>
        <p>正在加载...</p>
      </div>

      <div v-else class="summary-cards">
        <div
            v-for="item in summaryList"
            :key="item.type"
            class="summary-card"
            :class="{ 'has-unread': item.unreadCount > 0 }"
            @click="openDetail(item)"
        >
          <div class="card-icon">
            <span v-if="item.type === 1">👍</span>
            <span v-else-if="item.type === 2">💬</span>
            <span v-else>📢</span>
          </div>

          <div class="card-body">
            <div class="card-title-row">
              <h3 class="card-title">{{ item.typeName }}</h3>
              <span v-if="item.unreadCount > 0" class="unread-badge">{{ item.unreadCount }}</span>
            </div>
            <p class="card-preview">{{ getPreviewText(item) }}</p>
          </div>

          <div class="card-right">
            <span class="total-badge">{{ item.total }}条</span>
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="arrow-icon">
              <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
            </svg>
          </div>
        </div>
      </div>
    </template>

    <!-- ========== 详情视图 ========== -->
    <template v-else-if="viewMode === 'detail'">
      <div class="detail-header">
        <button class="back-btn" @click="backToSummary">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="back-icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
          </svg>
          返回
        </button>
        <h2 class="detail-title">{{ currentTypeName }} ({{ detailTotal }})</h2>
      </div>

      <div v-if="isLoadingDetail" class="loading-state">
        <div class="loading-spinner-large"></div>
        <p>正在加载消息...</p>
      </div>

      <div v-else-if="messages.length === 0" class="empty-state">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="empty-icon">
          <path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.913A2.25 2.25 0 012.25 6.996V6.75" />
        </svg>
        <p>暂无此类消息</p>
      </div>

      <div v-else class="message-list">
        <div
            v-for="msg in messages"
            :key="msg.id"
            class="message-card"
            :class="{
              unread: msg.status === 0,
              clickable: msg.type === 3 || msg.type === 2,
              expanded: msg._expanded
            }"
            @click="handleMsgClick(msg)"
        >
          <div class="msg-avatar">
            <span v-if="msg.type === 1">👍</span>
            <span v-else-if="msg.type === 2">💬</span>
            <span v-else>📢</span>
          </div>

          <div class="msg-content">
            <div class="msg-header">
              <span class="msg-sender">{{ msg.senderName || '系统' }}</span>
              <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
            </div>
            <!-- 系统公告有 title + content -->
            <p v-if="msg.type === 3 && msg.title" class="msg-title">{{ msg.title }}</p>
            <!-- 正文：未展开时限两行 -->
            <p
                class="msg-text"
                :class="{ 'text-clamped': !msg._expanded }"
            >{{ msg.content }}</p>
            <!-- 展开提示 -->
            <span v-if="msg.type === 3 && msg.content && msg.content.length > 80 && !msg._expanded" class="expand-hint">点击查看详情</span>
            <span v-else-if="msg.type === 3 && msg._expanded" class="expand-hint">点击收起</span>
            <span v-else-if="msg.type === 2" class="expand-hint jump-hint">点击跳转到该资源</span>
          </div>

          <div v-if="msg.status === 0" class="unread-dot"></div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="detailPages > 1" class="pagination">
        <button class="page-btn" :disabled="detailPage === 1" @click="loadMessages(detailPage - 1)">上一页</button>
        <span class="page-info">{{ detailPage }} / {{ detailPages }}</span>
        <button class="page-btn" :disabled="detailPage >= detailPages" @click="loadMessages(detailPage + 1)">下一页</button>
      </div>
    </template>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { showToast } from '@/utils/toast'
import { formatTime } from '@/utils/format'

const router = useRouter()

// ==================== 视图状态 ====================
const viewMode = ref('summary')
const currentType = ref(null)
const currentTypeName = ref('')

// ==================== 摘要 ====================
const summaryList = ref([])
const isLoadingSummary = ref(false)

onMounted(() => {
  fetchSummary()
})

const fetchSummary = async () => {
  isLoadingSummary.value = true
  try {
    const res = await request.get('/messageSummary')
    if (res.code === 200) {
      summaryList.value = res.data || []
    }
  } catch (error) {
    console.error('获取消息摘要异常：', error)
    showToast('加载消息失败', 'error')
  } finally {
    isLoadingSummary.value = false
  }
}

const getPreviewText = (item) => {
  if (item.total === 0) return '暂无消息'
  const prefix = item.firstSender || '用户'
  switch (item.type) {
    case 1:
      return `${prefix}等${item.total}位用户给您点赞`
    case 2:
      return `${prefix}等${item.total}位用户评论了您`
    case 3:
      return `管理员发布了${item.total}条公告`
    default:
      return `${item.total}条消息`
  }
}

// ==================== 详情 ====================
const messages = ref([])
const isLoadingDetail = ref(false)
const detailPage = ref(1)
const detailPages = ref(1)
const detailTotal = ref(0)
const pageSize = 10

const openDetail = async (item) => {
  currentType.value = item.type
  currentTypeName.value = item.typeName
  viewMode.value = 'detail'

  if (item.unreadCount > 0) {
    try {
      await request.put('/markMessageReadByType', null, { params: { type: item.type } })
      const s = summaryList.value.find(s => s.type === item.type)
      if (s) s.unreadCount = 0
      // 通知 MainLayout 刷新顶部未读徽标
      window.__refreshUnreadCount?.()
    } catch (e) {
      console.error('标记已读失败：', e)
    }
  }

  loadMessages(1)
}

const loadMessages = async (page = 1) => {
  isLoadingDetail.value = true
  detailPage.value = page

  try {
    const res = await request.get('/myMessages', {
      params: { type: currentType.value, pageNum: page, pageSize: pageSize }
    })

    if (res.code === 200) {
      const pageData = res.data
      messages.value = (pageData.records || []).map(m => ({
        ...m,
        _expanded: false  // 每条消息独立的展开/收起
      }))
      detailPages.value = pageData.pages || 1
      detailTotal.value = pageData.total || 0
    }
  } catch (error) {
    console.error('获取消息详情异常：', error)
    showToast('加载消息失败', 'error')
  } finally {
    isLoadingDetail.value = false
  }
}

// ==================== 三种点击行为 ====================
const handleMsgClick = (msg) => {
  // 标记已读
  if (msg.status === 0) {
    msg.status = 1
    request.put(`/markMessageRead/${msg.id}`).catch(() => {})
    // 通知 MainLayout 刷新顶部未读徽标
    window.__refreshUnreadCount?.()
  }

  switch (msg.type) {
    case 3:
      // 系统公告：展开/收起
      msg._expanded = !msg._expanded
      break
    case 2:
      // 评论消息：跳转到资源详情，带评论 ID 定位
      if (msg.targetId) {
        router.push({
          name: 'ResourceDetail',
          params: { id: msg.targetId },
          query: { commentId: msg.commentId }
        })
      }
      break
    case 1:
      // 点赞消息：不可点击
      break
  }
}

const backToSummary = () => {
  viewMode.value = 'summary'
  fetchSummary()
}

</script>

<style scoped>
.messages-page { min-height: calc(100vh - 120px); }

/* ==================== 摘要 ==================== */
.page-header { margin-bottom: 32px; text-align: left; }
.page-header h2 { margin: 0 0 8px 0; font-size: 24px; font-weight: 600; color: #111827; }
.page-header p { margin: 0; font-size: 14px; color: #6b7280; }

.loading-state {
  display: flex; flex-direction: column; align-items: center;
  padding: 80px 24px; color: #9ca3af; gap: 16px;
}
.loading-spinner-large {
  width: 36px; height: 36px;
  border: 3px solid #e5e7eb; border-top-color: #111827;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.summary-cards { display: flex; flex-direction: column; gap: 12px; }
.summary-card {
  display: flex; align-items: center; gap: 16px; padding: 24px;
  background: #ffffff; border-radius: 12px; border: 1px solid #e5e7eb;
  cursor: pointer; transition: all 0.2s;
}
.summary-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.06); border-color: #d1d5db; transform: translateY(-1px); }
.summary-card.has-unread { border-left: 3px solid #111827; background-color: #fafafa; }

.card-icon {
  font-size: 32px; width: 56px; height: 56px;
  display: flex; align-items: center; justify-content: center;
  background: #f9fafb; border-radius: 12px; flex-shrink: 0;
}
.card-body { flex: 1; min-width: 0; }
.card-title-row { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.card-title { margin: 0; font-size: 16px; font-weight: 600; color: #111827; }
.unread-badge {
  min-width: 20px; height: 20px; padding: 0 6px;
  background-color: #ef4444; color: #ffffff; font-size: 11px;
  font-weight: 600; border-radius: 10px;
  display: inline-flex; align-items: center; justify-content: center;
}
.card-preview {
  margin: 0; font-size: 14px; color: #6b7280;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.card-right { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.total-badge { font-size: 13px; color: #9ca3af; font-weight: 500; }
.arrow-icon { width: 18px; height: 18px; color: #d1d5db; }

/* ==================== 详情 ==================== */
.detail-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.back-btn {
  display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px;
  background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 8px;
  font-size: 14px; color: #374151; cursor: pointer; transition: all 0.2s;
}
.back-btn:hover { background: #f3f4f6; border-color: #d1d5db; }
.back-icon { width: 16px; height: 16px; }
.detail-title { margin: 0; font-size: 20px; font-weight: 600; color: #111827; }

.empty-state {
  padding: 80px 24px; text-align: center; color: #9ca3af;
  background: #ffffff; border-radius: 12px; border: 1px solid #e5e7eb;
}
.empty-icon { width: 48px; height: 48px; margin: 0 auto 12px; opacity: 0.3; }
.empty-state p { margin: 0; font-size: 14px; }

.message-list {
  background: #ffffff; border-radius: 12px; border: 1px solid #e5e7eb;
  overflow: hidden; display: flex; flex-direction: column;
}

.message-card {
  display: flex; align-items: flex-start; gap: 14px; padding: 18px 24px;
  border-bottom: 1px solid #f3f4f6; transition: background-color 0.2s;
  position: relative;
}
.message-card:last-child { border-bottom: none; }
.message-card.unread { background-color: #fafafa; }
.message-card.clickable { cursor: pointer; }
.message-card.clickable:hover { background-color: #f9fafb; }
.message-card.expanded { background-color: #f3f4f6; }

.msg-avatar {
  font-size: 22px; width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  background: #f9fafb; border-radius: 10px; flex-shrink: 0;
}
.msg-content { flex: 1; min-width: 0; }

.msg-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 6px; gap: 12px;
}
.msg-sender { font-size: 14px; font-weight: 600; color: #111827; }
.msg-time { font-size: 12px; color: #9ca3af; flex-shrink: 0; }

/* 系统公告标题 */
.msg-title {
  margin: 0 0 4px 0; font-size: 15px; font-weight: 600; color: #111827;
}

/* 正文：默认限两行 */
.msg-text {
  margin: 0; font-size: 14px; color: #4b5563; line-height: 1.6;
  white-space: pre-wrap; word-break: break-word;
}
.text-clamped {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 提示文字 */
.expand-hint {
  display: inline-block; margin-top: 6px;
  font-size: 12px; color: #9ca3af;
}
.jump-hint { color: #3b82f6; }

.unread-dot {
  position: absolute; top: 22px; right: 24px;
  width: 8px; height: 8px; background-color: #111827; border-radius: 50%;
}

/* 分页 */
.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 24px; }
.page-btn {
  padding: 8px 20px; background: #ffffff; border: 1px solid #e5e7eb;
  border-radius: 8px; font-size: 14px; color: #374151; cursor: pointer; transition: all 0.2s;
}
.page-btn:hover:not(:disabled) { border-color: #111827; color: #111827; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 14px; color: #6b7280; }
</style>
