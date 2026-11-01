// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import Index from '../views/Index.vue'
import Register from '../views/Register.vue'
import Login from '../views/Login.vue'
import Home from "@/views/Home.vue"
import Profile from "@/views/Profile.vue"
import MainLayout from '../layout/MainLayout.vue'
import AdminLayout from '../layout/AdminLayout.vue'
import MyMessage from '@/views/MyMessage.vue'
import AIService from '@/views/AIService.vue'
import MyPublish from '@/views/MyPublish.vue'
import MyFavorites from '@/views/Myfavorites.vue'
import ResourceDetail from '@/views/ResourceDetail.vue'
import NoticeDetail from '@/views/NoticeDetail.vue'
// 管理端页面
import AdminDashboard from '@/views/admin/AdminDashboard.vue'
import AdminResources from '@/views/admin/AdminResources.vue'
import AdminUsers from '@/views/admin/AdminUsers.vue'
import AdminNotices from '@/views/admin/AdminNotices.vue'
import AdminAdmins from '@/views/admin/AdminAdmins.vue'
import AdminResourceDetail from '@/views/admin/AdminResourceDetail.vue'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        // ==================== 公开页面 ====================
        {
            path: '/',
            name: 'Index',
            component: Index
        },
        {
            path: '/register',
            name: 'Register',
            component: Register
        },
        {
            path: '/login',
            name: 'Login',
            component: Login
        },

        // ==================== 用户端（需登录） ====================
        {
            path: '/sys',
            component: MainLayout,
            children: [
                { path: 'home', name: 'Home', component: Home },
                { path: 'profile', name: 'Profile', component: Profile },
                { path: 'messages', name: 'Messages', component: MyMessage },
                { path: 'ai-service', name: 'AIService', component: AIService },
                { path: 'my-posts', name: 'MyPosts', component: MyPublish },
                { path: 'my-favorites', name: 'MyFavorites', component: MyFavorites },
                { path: 'resource/:id', name: 'ResourceDetail', component: ResourceDetail },
                { path: 'notice-detail', name: 'NoticeDetail', component: NoticeDetail }
            ]
        },

        // ==================== 管理端（需管理员角色） ====================
        // 每个路由标记需要的权限，在 router.beforeEach 中做二次校验
        {
            path: '/admin',
            component: AdminLayout,
            redirect: '/admin/dashboard',
            children: [
                { path: 'dashboard', name: 'AdminDashboard', component: AdminDashboard, meta: { privilege: null } },
                { path: 'resources', name: 'AdminResources', component: AdminResources, meta: { privilege: '资源管理' } },
                { path: 'users', name: 'AdminUsers', component: AdminUsers, meta: { privilege: '用户管理' } },
                { path: 'notices', name: 'AdminNotices', component: AdminNotices, meta: { privilege: '公告管理' } },
                { path: 'admins', name: 'AdminAdmins', component: AdminAdmins, meta: { privilege: '管理员管理' } },
                { path: 'resource/:id', name: 'AdminResourceDetail', component: AdminResourceDetail, meta: { privilege: '资源管理' } }
            ]
        }
    ]
})

// 全局路由守卫
router.beforeEach((to, from) => {
    const token = localStorage.getItem('token')
    const role = localStorage.getItem('role')

    const isPublicPath = ['/', '/login', '/register'].includes(to.path)
    const isAdminPath = to.path.startsWith('/admin')
    const isUserPath = to.path.startsWith('/sys')

    // 未登录 → 仅允许访问公开页面
    if (!token && !isPublicPath) {
        return '/login'
    }

    // 已登录 → 不允许访问登录/注册/首页
    if (token && isPublicPath) {
        if (role === 'admin') return '/admin/dashboard'
        return '/sys/home'
    }

    // 角色越权拦截
    if (token) {
        if (isAdminPath && role !== 'admin') {
            return '/sys/home'
        }
        if (isUserPath && role === 'admin') {
            return '/admin/dashboard'
        }
    }

    // === 管理员页面权限二次校验 ===
    if (token && role === 'admin' && isAdminPath && to.meta?.privilege) {
        const privsStr = localStorage.getItem('adminPrivileges')
        // 权限尚未加载（AdminLayout/Dashboard 的 /home 调用还未返回）→ 放行，后端会做权限校验
        // 如果是 '[]' 也是同样处理（可能是旧版本登录时残留的空数组，或 /home 还没返回）
        if (!privsStr || privsStr === '[]') {
            return true
        }
        try {
            const privs = JSON.parse(privsStr)
            if (!privs.includes(to.meta.privilege)) {
                // 这里不能用 showToast（router 文件不依赖 toast），用 alert 兜底
                alert('无权限访问该页面')
                return '/admin/dashboard'
            }
        } catch (e) {
            // 放行
        }
    }

    return true
})

export default router
