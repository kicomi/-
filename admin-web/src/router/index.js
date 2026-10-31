import { createRouter, createWebHashHistory } from 'vue-router'
import { auth } from '../store/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '数据大屏' }
      },
      {
        path: 'books',
        name: 'books',
        component: () => import('../views/BookAudit.vue'),
        meta: { title: '书籍审核' }
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('../views/UserManage.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('../views/OrderManage.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'reports',
        name: 'reports',
        component: () => import('../views/ReportManage.vue'),
        meta: { title: '举报处理' }
      },
      {
        path: 'banners',
        name: 'banners',
        component: () => import('../views/BannerManage.vue'),
        meta: { title: '广告管理' }
      },
      {
        path: 'ai',
        name: 'ai',
        component: () => import('../views/AiAssistant.vue'),
        meta: { title: '运营助手' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  // 用 hash 模式，打包后直接双击 index.html 也能打开，答辩现场省事
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  if (!auth.token) return { path: '/login' }
  return true
})

export default router