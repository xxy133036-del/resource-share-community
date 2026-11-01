<template>
  <div class="index-wrapper">
    <nav class="top-nav">
      <div class="brand">
        <span class="logo-mark"></span>
        <span class="brand-name">Nexus</span>
      </div>
    </nav>

    <div class="hero-container">
      <div class="hero-section">
        <div class="badge fade-in-1">
          <span class="badge-dot"></span>
          弘创工作室 · 内部资源分享平台
        </div>

        <h1 class="title fade-in-2">欢迎探索未知的代码世界</h1>
        <p class="subtitle fade-in-3">在这里发现、分享，与顶尖开发者同行。极简、高效、纯粹的现代化系统。</p>

        <div class="action-buttons fade-in-4">
          <button class="btn btn-secondary" @click="goToLogin">登录账号</button>
          <button class="btn btn-primary" @click="goToRegister">
            立即注册
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="btn-icon">
              <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {useRouter} from 'vue-router'

const router = useRouter()

const goToRegister = () => {
  router.push('/register')
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
/* 包含科技感网格的背景 */
.index-wrapper {
  min-height: 100vh;
  background-color: #fafafa;
  background-image: linear-gradient(to right, rgba(0, 0, 0, 0.03) 1px, transparent 1px),
  linear-gradient(to bottom, rgba(0, 0, 0, 0.03) 1px, transparent 1px);
  background-size: 40px 40px; /* 网格大小 */
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
  display: flex;
  flex-direction: column;
}

/* 顶部导航简版 */
.top-nav {
  padding: 24px 40px;
  display: flex;
  align-items: center;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-mark {
  width: 24px;
  height: 24px;
  background-color: #111827;
  border-radius: 6px;
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.5px;
}

/* 核心内容区 */
.hero-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 24px 60px;
  max-width: 1000px;
  margin: 0 auto;
}

.hero-section {
  text-align: center;
  margin-bottom: 80px;
}

/* Badge 标签 */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background-color: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #4b5563;
  margin-bottom: 24px;
}

.badge-dot {
  width: 6px;
  height: 6px;
  background-color: #111827;
  border-radius: 50%;
}

.title {
  font-size: 48px;
  font-weight: 800;
  color: #111827;
  letter-spacing: -1.5px;
  margin: 0 0 20px 0;
  line-height: 1.2;
}

.subtitle {
  font-size: 18px;
  color: #6b7280;
  margin: 0 auto 40px;
  max-width: 600px;
  line-height: 1.6;
}
.action-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 48px;
  padding: 0 32px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.btn-icon {
  width: 18px;
  height: 18px;
  transition: transform 0.2s;
}

.btn-primary {
  background-color: #111827;
  color: white;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.btn-primary:hover {
  background-color: #374151;
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.btn-primary:hover .btn-icon {
  transform: translateX(4px);
}

.btn-secondary {
  background-color: #ffffff;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover {
  background-color: #f9fafb;
  border-color: #9ca3af;
  transform: translateY(-2px);
}

/* 交错淡入动画 */
.fade-in-1 {
  animation: fadeInUp 0.6s ease-out 0.1s both;
}

.fade-in-2 {
  animation: fadeInUp 0.6s ease-out 0.2s both;
}

.fade-in-3 {
  animation: fadeInUp 0.6s ease-out 0.3s both;
}

.fade-in-4 {
  animation: fadeInUp 0.6s ease-out 0.4s both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式适配 */
@media (max-width: 768px) {
  .title {
    font-size: 36px;
  }

  .top-nav {
    padding: 20px;
  }

  .hero-container {
    padding: 0 20px 40px;
  }
}
</style>