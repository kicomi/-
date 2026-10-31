import axios from 'axios'
import { auth } from '../store/auth'
import request from './request'

export const adminLogin = (data) => request.post('/auth/admin-login', data)

export const getOverview = () => request.get('/admin/stats/overview')
export const getCharts = () => request.get('/admin/stats/charts')
export const getAlgoStatus = () => request.get('/admin/algo-status')

export const getBooks = (params) => request.get('/admin/books', { params })
export const auditBook = (id, data) => request.post('/admin/books/' + id + '/audit', data)

export const getUsers = (params) => request.get('/admin/users', { params })
export const resetNickname = (id, nickname) =>
  request.put('/admin/users/' + id + '/nickname', { nickname })
export const userDetail = (id) => request.get('/admin/users/' + id + '/detail')
export const adjustCredit = (id, creditScore, reason) =>
  request.put('/admin/users/' + id + '/credit', { creditScore, reason })

// 导出走 blob，响应拦截器会挡住非 {code} 结构，所以单独用裸 axios
export const exportFile = (kind) =>
  axios.get('/api/admin/export/' + kind, {
    responseType: 'blob',
    headers: { Authorization: 'Bearer ' + auth.token }
  }).then((r) => r.data)

// 封禁与禁言：days 传 0 表示永久
export const banUser = (id, days, reason) =>
  request.post('/admin/users/' + id + '/ban', null, { params: { days, reason } })
export const unbanUser = (id) => request.post('/admin/users/' + id + '/unban')
export const muteUser = (id, days, reason) =>
  request.post('/admin/users/' + id + '/mute', null, { params: { days, reason } })
export const unmuteUser = (id) => request.post('/admin/users/' + id + '/unmute')

export const getOrders = (params) => request.get('/admin/orders', { params })

// 广告位
export const getBanners = (position) => request.get('/admin/banners', { params: { position } })
export const saveBanner = (data) => request.post('/admin/banners', data)
export const setBannerStatus = (id, status) =>
  request.post('/admin/banners/' + id + '/status', null, { params: { status } })
export const deleteBanner = (id) => request.delete('/admin/banners/' + id)

// 运营助手
export const aiChat = (message) => request.post('/admin/ai/chat', { message })
export const aiQuickQuestions = () => request.get('/admin/ai/quick-questions')

// 举报
export const reportList = (params) => request.get('/admin/reports', { params })
export const reportHandle = (id, data) => request.put('/admin/reports/' + id, data)
export const reportDetail = (id) => request.get('/admin/reports/' + id)
export const reportPending = () => request.get('/admin/reports/pending')