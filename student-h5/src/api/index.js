import axios from 'axios'
import { userStore } from '../store/user'

const request = axios.create({ baseURL: '/api', timeout: 15000 })

request.interceptors.request.use((config) => {
  if (userStore.token) config.headers.Authorization = 'Bearer ' + userStore.token
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code === 0) return body.data
    if (body && body.code === 403 && String(body.message || '').includes('封禁')) {
      userStore.logout()
      alert(body.message)
      location.hash = '#/login'
      return Promise.reject(new Error(body.message))
    }
    if (body && body.code === 401) userStore.logout()
    return Promise.reject(new Error((body && body.message) || '请求失败'))
  },
  (error) => {
    const status = error.response && error.response.status
    const body = error.response && error.response.data
    if (status === 401) userStore.logout()
    return Promise.reject(new Error((body && body.message) || error.message || '网络异常'))
  }
)

// ---------- 认证 ----------
export const wxLogin = (code) => request.post('/auth/wx-login', { code })
export const register = (data) => request.post('/auth/register', data)
export const passwordLogin = (data) => request.post('/auth/login', data)
export const getMe = () => request.get('/auth/me')
export const updateProfile = (data) => request.put('/users/me', data)

// ---------- 公开 ----------
export const getDict = () => request.get('/public/dict')
export const getBanners = (position) => request.get('/public/banners', { params: { position } })
export const bannerView = (id) => request.post('/public/banners/' + id + '/view')
export const publicBooks = (params) => request.get('/public/books', { params })
export const publicBook = (id) => request.get('/public/books/' + id)
export const publicHome = () => request.get('/public/home')
export const sellerProfile = (id) => request.get('/public/sellers/' + id)
export const hotKeywords = () => request.get('/public/hot-keywords', { params: { limit: 8 } })
export const similarBooks = (id, limit) => request.get('/public/similar/' + id, { params: { limit } })

// ---------- 需要登录 ----------
export const searchBooks = (params) => request.get('/books', { params })
export const bookDetail = (id) => request.get('/books/' + id)
export const recommendForUser = (limit) => request.get('/recommend/for-user', { params: { limit } })
export const suggestPrice = (data) => request.post('/price/suggest', data)
export const publishBook = (data) => request.post('/books', data)
export const myBooks = (type) => request.get('/books/my', { params: { type } })
export const myFavorites = () => request.get('/books/my/favorites')
export const browseHistory = (limit = 12) => request.get('/books/my/history', { params: { limit } })
export const toggleFavorite = (id, add) =>
  add ? request.post('/books/' + id + '/favorite') : request.delete('/books/' + id + '/favorite')
export const offlineBook = (id) => request.post('/books/' + id + '/offline')
export const createOrder = (data) => request.post('/orders', data)
export const myOrders = (role, page = 1, size = 10) =>
  request.get('/orders', { params: { role, page, size } })
export const confirmOrder = (id) => request.put('/orders/' + id + '/confirm')
export const completeOrder = (id) => request.put('/orders/' + id + '/complete')
export const cancelOrder = (id) => request.put('/orders/' + id + '/cancel')
export const createReview = (data) => request.post('/reviews', data)
export const bookReviews = (bookId) => request.get('/reviews/book/' + bookId)
export const reviewsPage = (bookId, page = 1, size = 10) =>
  request.get('/reviews/book/' + bookId + '/page', { params: { page, size } })
export const reviewsSummary = (bookId) => request.get('/reviews/book/' + bookId + '/summary')

// ---------- 聊天 ----------
export const sendMessage = (data) => request.post('/chat/send', data)
export const chatHistory = (peerId, bookId) =>
  request.get('/chat/history', { params: { peerId, bookId } })
export const conversations = () => request.get('/chat/conversations')
export const unreadCount = () => request.get('/chat/unread')

// ---------- 上传 ----------
export async function uploadImage(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
// 举报
export const submitReport = (data) => request.post('/reports', data)
// 通知
export const notifications = (page = 1) => request.get('/notifications', { params: { page, size: 30 } })
export const noticeUnread = () => request.get('/notifications/unread')
export const readNotice = (id) => request.put('/notifications/' + id + '/read')
export const readAllNotice = () => request.put('/notifications/read-all')
// 个人统计 / 评价维护
export const myStats = () => request.get('/users/me/stats')
export const reviewByOrder = (orderId) => request.get('/reviews/order/' + orderId)
export const updateReview = (id, data) => request.put('/reviews/' + id, data)
export const deleteReview = (id) => request.delete('/reviews/' + id)