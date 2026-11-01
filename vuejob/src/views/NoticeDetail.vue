<template>
  <div class="notice-detail-page">
    <button class="back-link" @click="$router.back()">
      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="back-icon">
        <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
      </svg>
      返回
    </button>

    <article class="notice-article">
      <header class="notice-header">
        <span class="notice-badge">系统公告</span>
        <h1 class="notice-title">{{ notice.title }}</h1>
        <time class="notice-time">{{ notice.date }}</time>
      </header>

      <div class="divider"></div>

      <div class="notice-body">
        <p>{{ notice.content }}</p>
      </div>
    </article>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const notice = ref({
  title: '',
  content: '',
  date: ''
})

onMounted(() => {
  notice.value.title = route.query.title || '公告详情'
  notice.value.content = route.query.content || '暂无内容'
  notice.value.date = route.query.date || ''

  // 无效访问则返回首页
  if (!route.query.title && !route.query.content) {
    router.replace('/sys/home')
  }
})
</script>

<style scoped>
.notice-detail-page {
  max-width: 780px;
  margin: 0 auto;
  padding: 40px 24px 80px;
}

/* 返回按钮 */
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 0;
  background: none;
  border: none;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  transition: color 0.2s;
  font-family: inherit;
  margin-bottom: 48px;
}
.back-link:hover { color: #111827; }
.back-icon { width: 18px; height: 18px; }

/* 文章卡片 */
.notice-article {
  text-align: center;
}

/* 头部 */
.notice-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.notice-badge {
  display: inline-block;
  padding: 6px 20px;
  background: #f3f4f6;
  color: #6b7280;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.notice-title {
  margin: 0;
  font-size: 36px;
  font-weight: 700;
  color: #111827;
  line-height: 1.3;
  letter-spacing: -0.5px;
}

.notice-time {
  font-size: 14px;
  color: #9ca3af;
}

/* 分割线 */
.divider {
  width: 60px;
  height: 2px;
  background: #e5e7eb;
  margin: 40px auto;
}

/* 正文 */
.notice-body {
  text-align: left;
  padding: 0 20px;
}

.notice-body p {
  margin: 0;
  font-size: 16px;
  line-height: 2;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 暗色适配 */
@media (prefers-color-scheme: dark) {
  .notice-title { color: #f9fafb; }
  .notice-badge { background: #1f2937; color: #9ca3af; }
  .notice-time { color: #6b7280; }
  .notice-body p { color: #d1d5db; }
  .divider { background: #374151; }
  .back-link { color: #9ca3af; }
  .back-link:hover { color: #f9fafb; }
}

@media (max-width: 640px) {
  .notice-title { font-size: 26px; }
  .notice-body { padding: 0; }
  .notice-body p { font-size: 15px; }
}
</style>
