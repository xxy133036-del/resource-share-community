<template>
  <div class="ai-service-page">
    <div class="page-header">
      <h2>AI 智能客服</h2>
      <p>有任何问题都可以咨询我哦</p>
    </div>

    <div class="chat-container">
      <div class="chat-messages" ref="messagesContainer">
        <div
            v-for="(msg, index) in messages"
            :key="index"
            class="message-bubble"
            :class="msg.role"
        >
          <div class="message-avatar">
            <span v-if="msg.role === 'user'">👤</span>
            <span v-else>🤖</span>
          </div>
          <div class="message-text">{{ msg.content }}</div>
        </div>

        <div v-if="isTyping" class="message-bubble ai">
          <div class="message-avatar">🤖</div>
          <div class="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>

        <div v-if="errorMsg" class="error-tip">
          {{ errorMsg }}
          <button class="retry-btn" @click="retryLastMessage">重试</button>
        </div>
      </div>

      <div class="chat-input-area">
        <input
            v-model="inputMessage"
            @keyup.enter="sendMessage"
            type="text"
            placeholder="输入您的问题..."
            :disabled="isTyping"
            class="chat-input"
        />
        <button
            @click="sendMessage"
            :disabled="!inputMessage.trim() || isTyping"
            class="send-btn"
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="send-icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 12L3.269 3.126A59.768 59.768 0 0121.485 12 59.77 59.77 0 013.27 20.876L5.999 12zm0 0h7.5" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import request from '@/utils/request'

const messages = ref([
  {
    role: 'ai',
    content: '你好！我是 AI 客服助手，有什么可以帮助你的吗？😊'
  }
])

const inputMessage = ref('')
const isTyping = ref(false)
const errorMsg = ref('')
const messagesContainer = ref(null)
const lastQuestion = ref('')

/**
 * 会话 ID：聊天窗口打开时生成一次，整个会话期间保持不变
 * 后端 Redis 根据此 ID 存取对话历史，实现多轮上下文记忆
 */
const conversationId = ref('')

onMounted(() => {
  // 获取当前用户 ID（从 localStorage 的 token payload 或直接生成）
  const token = localStorage.getItem('token') || ''
  const userId = token ? token.substring(token.length - 8) : Date.now().toString(36)
  conversationId.value = userId + '_' + crypto.randomUUID().substring(0, 8)
})

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || isTyping.value) return

  const question = inputMessage.value.trim()
  messages.value.push({ role: 'user', content: question })
  lastQuestion.value = question

  inputMessage.value = ''
  errorMsg.value = ''
  await scrollToBottom()

  isTyping.value = true

  try {
    const params = new URLSearchParams()
    params.append('question', question)
    params.append('conversationId', conversationId.value)

    const res = await request.post('/chat/ask', params.toString(), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    })

    if (res.code === 200) {
      messages.value.push({ role: 'ai', content: res.data })
    } else {
      errorMsg.value = res.message || 'AI 暂时无法回答，请稍后重试'
    }
  } catch (e) {
    console.error('AI 请求异常：', e)
    errorMsg.value = '网络异常，请检查连接后重试'
  } finally {
    isTyping.value = false
    await scrollToBottom()
  }
}

const retryLastMessage = async () => {
  if (!lastQuestion.value) return
  // 移除上一条用户消息和错误提示，重新发送
  const lastIdx = messages.value.findLastIndex(m => m.role === 'user' && m.content === lastQuestion.value)
  if (lastIdx >= 0) {
    messages.value.splice(lastIdx)
  }
  errorMsg.value = ''
  inputMessage.value = lastQuestion.value
  lastQuestion.value = ''
  await sendMessage()
}
</script>

<style scoped>
.ai-service-page {
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

.chat-container {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e5e7eb;
  height: calc(100vh - 280px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-bubble {
  display: flex;
  gap: 12px;
  max-width: 80%;
  animation: fadeIn 0.3s ease;
}

.message-bubble.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-bubble.ai {
  align-self: flex-start;
}

.message-avatar {
  font-size: 24px;
  flex-shrink: 0;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  background-color: #f3f4f6;
  color: #111827;
  word-break: break-word;
}

.message-bubble.user .message-text {
  background-color: #111827;
  color: #ffffff;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background-color: #f3f4f6;
  border-radius: 12px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background-color: #9ca3af;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-8px); }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.error-tip {
  align-self: center;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background-color: #fef2f2;
  color: #991b1b;
  border-radius: 8px;
  font-size: 13px;
}

.retry-btn {
  padding: 4px 12px;
  background: #fecaca;
  color: #991b1b;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  font-family: inherit;
}

.retry-btn:hover { background: #fca5a5; }

.chat-input-area {
  padding: 16px 24px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 12px;
  background-color: #fafafa;
}

.chat-input {
  flex: 1;
  height: 44px;
  padding: 0 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.chat-input:focus { border-color: #111827; }
.chat-input:disabled { background-color: #f3f4f6; cursor: not-allowed; }

.send-btn {
  width: 44px;
  height: 44px;
  background-color: #111827;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.send-btn:hover:not(:disabled) { background-color: #374151; }
.send-btn:disabled { background-color: #d1d5db; cursor: not-allowed; }

.send-icon { width: 20px; height: 20px; color: #ffffff; }
</style>
