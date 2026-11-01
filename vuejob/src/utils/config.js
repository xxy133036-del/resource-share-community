/**
 * 全局前端配置
 * 所有环境相关的 URL 集中管理，方便切换开发/生产环境
 */

// 头像服务基础路径（通过 Vite 代理 → user8011 的 /head/** 静态资源映射）
export const AVATAR_BASE = '/api/head/'

// 默认头像（DiceBear 生成器，无需后端支持）
export const DEFAULT_AVATAR = 'https://api.dicebear.com/7.x/notionists/svg?seed=Fallback'

/**
 * 获取头像完整 URL
 * @param {string|null|undefined} avatar - 后端返回的头像文件名
 * @returns {string} 可用的头像 URL
 */
export const getAvatarUrl = (avatar) => {
  if (avatar && avatar.trim()) {
    return `${AVATAR_BASE}${avatar}`
  }
  return DEFAULT_AVATAR
}

// 管理员 API 基础路径（通过 Vite 代理 → admin8012）
export const ADMIN_API_BASE = '/admin-api'
