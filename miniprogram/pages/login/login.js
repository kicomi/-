// pages/login/login.js
const api = require('../../utils/api')

Page({
  data: {
    showAccounts: false,
    accounts: [
      { code: 'demo-002', name: '买家视角', desc: '有浏览记录，推荐更准' },
      { code: 'demo-010', name: '卖家视角', desc: '已发布多本闲置' },
      { code: 'demo-030', name: '新用户', desc: '冷启动，看热门与分类' }
    ]
  },

  toggleAccounts() {
    this.setData({ showAccounts: !this.data.showAccounts })
  },

  // 真实环境是 wx.login 拿 code 换 openid，演示模式直接映射到种子账号
  quickLogin() {
    this.onLogin({ currentTarget: { dataset: { code: 'demo-002' } } })
  },

  async onLogin(e) {
    const code = e.currentTarget.dataset.code
    try {
      const data = await api.wxLogin(code)
      const app = getApp()
      app.globalData.token = data.token
      app.globalData.user = data.user
      wx.setStorageSync('cb_token', data.token)
      wx.setStorageSync('cb_user', data.user)
      wx.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => wx.switchTab({ url: '/pages/home/home' }), 700)
    } catch (err) {
      // 已提示
    }
  },

  goRegister() {
    wx.navigateTo({ url: '/pages/register/register' })
  }
})
