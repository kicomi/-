// pages/login/login.js
const api = require('../../utils/api')

Page({
  data: {
    showAccounts: false,
    // 演示账号：code 会映射到 openid，对应种子数据里的真实用户
    accounts: [
      { code: 'demo-001', name: '买家视角', nick: '施雨桐 · 白银 89',
        desc: '在售 3 本、买过 3 单，有完整浏览记录' },
      { code: 'demo-079', name: '卖家视角', nick: '郑雨桐 · 青铜 78',
        desc: '在售 5 本，发布需审核、列表不优先' },
      { code: 'demo-010', name: '高信用用户', nick: '卫宇 · 黄金 97',
        desc: '发布免审核、首页优先，可对比青铜账号' },
      { code: 'demo-018', name: '新用户', nick: '孙文 · 80 分',
        desc: '冷启动状态，推荐走热门与分类兜底' }
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