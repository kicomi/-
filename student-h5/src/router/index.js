import { createRouter, createWebHashHistory } from 'vue-router'
import { userStore } from '../store/user'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/Home.vue'), meta: { title: '首页' } },
  { path: '/search', name: 'search', component: () => import('../views/Search.vue'), meta: { title: '找书' } },
  { path: '/book/:id', name: 'book-detail', component: () => import('../views/BookDetail.vue'), meta: { title: '书籍详情', auth: true } },
  { path: '/publish', name: 'publish', component: () => import('../views/Publish.vue'), meta: { title: '发布闲置', auth: true } },
  { path: '/orders', name: 'orders', component: () => import('../views/Orders.vue'), meta: { title: '订单', auth: true } },
  { path: '/me', name: 'me', component: () => import('../views/Me.vue'), meta: { title: '我的' } },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'register', component: () => import('../views/Register.vue'), meta: { title: '注册' } },
  { path: '/chat', name: 'chat', component: () => import('../views/Chat.vue'), meta: { title: '消息', auth: true } },
  { path: '/notice', name: 'notice', component: () => import('../views/Notice.vue'), meta: { title: '消息通知', auth: true } },
  { path: '/user/:id', name: 'user', component: () => import('../views/User.vue'), meta: { title: '个人主页' } },
  { path: '/reviews/:id', name: 'reviews', component: () => import('../views/Reviews.vue'), meta: { title: '全部评价' } },
  { path: '/credit', name: 'credit', component: () => import('../views/Credit.vue'), meta: { title: '信用分', auth: true } },
  { path: '/profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '编辑资料', auth: true } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  if (to.meta.auth && !userStore.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

router.afterEach((to) => {
  document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '校园二手书'
})

export default router