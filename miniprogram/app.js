// app.js
const { request } = require('./utils/request')

App({
  globalData: {
    // 演示环境后端跑在本机；真机调试时改成电脑的局域网 IP
    baseUrl: 'http://127.0.0.1:8080',
    token: '',
    user: null
  },

  onLaunch() {
    const token = wx.getStorageSync('cb_token') || ''
    const user = wx.getStorageSync('cb_user') || null
    this.globalData.token = token
    this.globalData.user = user

    // 有 token 但没缓存用户时补拉一次，避免"已登录却显示未登录"
    if (token && !user) {
      request({ url: '/api/auth/me' })
        .then((u) => {
          this.globalData.user = u
          wx.setStorageSync('cb_user', u)
        })
        .catch(() => {
          this.globalData.token = ''
          wx.removeStorageSync('cb_token')
        })
    }
  }
})
