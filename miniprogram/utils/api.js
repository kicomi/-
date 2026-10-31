// utils/api.js  接口集中管理，页面里只关心业务
const { request } = require('./request')

const BASE = () => getApp().globalData.baseUrl

module.exports = {
  // 认证
  wxLogin: (code) => request({ url: '/api/auth/wx-login', method: 'POST', data: { code } }),
  register: (data) => request({ url: '/api/auth/register', method: 'POST', data }),
  passwordLogin: (data) => request({ url: '/api/auth/login', method: 'POST', data }),
  getMe: () => request({ url: '/api/auth/me' }),
  updateProfile: (data) => request({ url: '/api/users/me', method: 'PUT', data }),
  myStats: () => request({ url: '/api/users/me/stats' }),
  reviewByOrder: (orderId) => request({ url: '/api/reviews/order/' + orderId }),
  updateReview: (id, data) => request({ url: '/api/reviews/' + id, method: 'PUT', data }),
  deleteReview: (id) => request({ url: '/api/reviews/' + id, method: 'DELETE' }),

  // 公开
  getDict: () => request({ url: '/api/public/dict' }),
  getBanners: () => request({ url: '/api/public/banners?position=home_banner' }),
  bannerView: (id) => request({ url: '/api/public/banners/' + id + '/view', method: 'POST' }),
  publicHome: () => request({ url: '/api/public/home' }),
  publicBooks: (params) => request({ url: '/api/public/books', data: params }),
  publicBook: (id) => request({ url: '/api/public/books/' + id }),

  // 书籍
  searchBooks: (params) => request({ url: '/api/books', data: params }),
  bookDetail: (id) => request({ url: '/api/books/' + id }),
  recommend: (limit) => request({ url: '/api/recommend/for-user?limit=' + (limit || 6) }),
  reviewsPage: (bookId, page) =>
    request({ url: '/api/reviews/book/' + bookId + '/page?page=' + (page || 1) + '&size=10' }),
  reviewsSummary: (bookId) => request({ url: '/api/reviews/book/' + bookId + '/summary' }),
  similar: (id, limit) => request({ url: '/api/public/similar/' + id + '?limit=' + (limit || 6) }),
  sellerProfile: (id) => request({ url: '/api/public/sellers/' + id }),
  hotKeywords: () => request({ url: '/api/public/hot-keywords?limit=8' }),
  report: (data) => request({ url: '/api/reports', method: 'POST', data }),

  // 通知
  notifications: (page) => request({ url: '/api/notifications?page=' + (page || 1) + '&size=30' }),
  noticeUnread: () => request({ url: '/api/notifications/unread' }),
  readNotice: (id) => request({ url: '/api/notifications/' + id + '/read', method: 'PUT' }),
  readAllNotice: () => request({ url: '/api/notifications/read-all', method: 'PUT' }),
  suggestPrice: (data) => request({ url: '/api/price/suggest', method: 'POST', data }),
  publishBook: (data) => request({ url: '/api/books', method: 'POST', data }),
  myBooks: (type) => request({ url: '/api/books/my?type=' + type }),
  myFavorites: () => request({ url: '/api/books/my/favorites' }),
  browseHistory: (limit) => request({ url: '/api/books/my/history?limit=' + (limit || 12) }),
  favorite: (id, add) =>
    request({ url: '/api/books/' + id + '/favorite', method: add ? 'POST' : 'DELETE' }),

  // 订单
  createOrder: (data) => request({ url: '/api/orders', method: 'POST', data }),
  myOrders: (role, page) =>
    request({ url: '/api/orders?role=' + role + '&page=' + (page || 1) + '&size=10' }),
  confirmOrder: (id) => request({ url: '/api/orders/' + id + '/confirm', method: 'PUT' }),
  completeOrder: (id) => request({ url: '/api/orders/' + id + '/complete', method: 'PUT' }),
  cancelOrder: (id) => request({ url: '/api/orders/' + id + '/cancel', method: 'PUT' }),
  createReview: (data) => request({ url: '/api/reviews', method: 'POST', data }),

  // 聊天
  sendMessage: (data) => request({ url: '/api/chat/send', method: 'POST', data }),
  chatHistory: (peerId, bookId) =>
    request({ url: '/api/chat/history', data: { peerId, bookId } }),
  conversations: () => request({ url: '/api/chat/conversations' }),
  unread: () => request({ url: '/api/chat/unread' }),

  /**
   * 图片上传。wx.uploadFile 和 wx.request 不是一套 API，单独封装。
   */
  /**
   * 上传前先压缩。手机原图动辄 10MB+，网络慢时还容易超时，
   * 压到 80% 质量、最长边 1600 后通常只剩几百 KB。
   */
  compressImage(filePath) {
    return new Promise((resolve) => {
      wx.compressImage({
        src: filePath,
        quality: 80,
        success: (res) => resolve(res.tempFilePath || filePath),
        // 压缩失败就用原图，不要挡住上传
        fail: () => resolve(filePath)
      })
    })
  },

  uploadImage: async (filePath) => {
    const path = await module.exports.compressImage(filePath)
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: BASE() + '/api/upload',
        filePath: path,
        name: 'file',
        header: { Authorization: 'Bearer ' + getApp().globalData.token },
        success(res) {
          try {
            const body = JSON.parse(res.data)
            if (body.code === 0) resolve(body.data)
            else {
              wx.showToast({ title: body.message || '上传失败', icon: 'none' })
              reject(new Error(body.message))
            }
          } catch (e) {
            reject(e)
          }
        },
        fail(err) {
          wx.showToast({ title: '上传失败，检查网络后重试', icon: 'none' })
          reject(err)
        }
      })
    })
  }
}