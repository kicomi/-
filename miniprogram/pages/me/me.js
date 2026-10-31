// pages/me/me.js
const api = require('../../utils/api')
const { assetUrl } = require('../../utils/asset')

// 信用分映射成等级，比干巴巴一个数字更有感知
function creditLevel(score) {
  const s = score == null ? 100 : score
  // 每档配主题色、图标与卡片风格，让高低信用一眼能分出来
  if (s >= 95) {
    return {
      name: '黄金信用', icon: '👑', theme: 'tier-gold',
      next: '已满级，享受免审核直接上架', percent: 100, color: '#c8901f'
    }
  }
  if (s >= 85) {
    return {
      name: '白银信用', icon: '🛡', theme: 'tier-silver',
      next: '再得 ' + (95 - s) + ' 分升黄金，解锁免审核', percent: Math.round((s - 60) / 35 * 100), color: '#7c8da3'
    }
  }
  if (s >= 70) {
    return {
      name: '青铜信用', icon: '🌱', theme: 'tier-bronze',
      next: '再得 ' + (85 - s) + ' 分升白银', percent: Math.round((s - 40) / 45 * 100), color: '#b0805a'
    }
  }
  return {
    name: '待提升', icon: '⚠️', theme: 'tier-low',
    next: '完成交易、保持好评可提升信用', percent: Math.max(5, s), color: '#c0563f'
  }
}

// 信用分规则，数值与后端 OrderService / ReviewService 保持一致
const CREDIT_GAIN = [
  { title: '完成一笔交易（作为卖家）', delta: '+2' },
  { title: '完成一笔交易（作为买家）', delta: '+1' },
  { title: '收到一条好评', delta: '+1' },
  { title: '资料填写完整', delta: '+2' }
]

const CREDIT_LOSS = [
  { title: '取消已确认的订单', delta: '-2' },
  { title: '书况与描述严重不符被投诉', delta: '-5' },
  { title: '发布违规内容', delta: '-10' },
  { title: '被禁言期间仍然违规', delta: '-10' }
]

Page({
  data: {
    user: null,
    avatarSrc: '',
    initial: '书',
    credit: { name: '', next: '', percent: 0, color: '#2f7d5b' },
    tab: 'selling',
    list: [],
    stats: { selling: 0, sold: 0, fav: 0, views: 0 },
    unread: 0,
    loading: false,
    showRules: false,
    noticeUnread: 0,
    creditGain: CREDIT_GAIN,
    creditLoss: CREDIT_LOSS
  },

  onShow() {
    const app = getApp()
    const user = app.globalData.user
    this.applyUser(user)
    if (user) this.refresh()
    else this.setData({ list: [], stats: { selling: 0, sold: 0, fav: 0, views: 0 }, unread: 0 })
  },

  applyUser(user) {
    if (!user) {
      this.setData({ user: null, avatarSrc: '', initial: '游' })
      return
    }
    this.setData({
      user,
      // 头像存的是相对路径，展示前统一拼成完整地址
      avatarSrc: assetUrl(user.avatar),
      initial: (user.nickname || '书').slice(0, 1),
      credit: creditLevel(user.creditScore)
    })
  },

  async refresh() {
    try {
      const me = await api.getMe()
      getApp().globalData.user = me
      wx.setStorageSync('cb_user', me)
      this.applyUser(me)
    } catch (e) { /* 过期由 request 处理 */ }

    try {
      // 一次拿到在售/已售/收藏/被浏览，少发几个请求
      this.setData({ stats: await api.myStats() })
    } catch (e) { /* ignore */ }

    try {
      const u = await api.unread()
      this.setData({ unread: u.count || 0 })
    } catch (e) { /* ignore */ }

    try {
      const n = await api.noticeUnread()
      this.setData({ noticeUnread: n.count || 0 })
    } catch (e) { /* ignore */ }

    this.loadList()
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
    this.loadList()
  },

  async loadList() {
    if (!this.data.user) return
    this.setData({ loading: true })
    try {
      let list
      if (this.data.tab === 'selling') list = await api.myBooks('selling')
      else if (this.data.tab === 'history') list = await api.browseHistory(12)
      else list = await api.myFavorites()
      this.setData({ list })
    } catch (e) {
      this.setData({ list: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  onBookTap(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.detail.book.id })
  },

  goLogin() { wx.navigateTo({ url: '/pages/login/login' }) },
  goChat() { wx.navigateTo({ url: '/pages/chat/chat' }) },
  goNotice() {
    wx.navigateTo({
      url: '/pages/notice/notice',
      // 新加页面后开发者工具偶尔不认，这里给个明确提示，避免点了没反应
      fail: () => {
        wx.showModal({
          title: '页面未加载',
          content: '请在微信开发者工具中点「编译」重新构建，或关闭项目后重新打开',
          showCancel: false
        })
      }
    })
  },
  // 直接展开规则：不依赖新页面注册，点了就一定有反应
  toggleRules() {
    this.setData({ showRules: !this.data.showRules })
  },
  goCredit() {
    wx.navigateTo({ url: '/pages/credit/credit' })
  },

  goProfile() { wx.navigateTo({ url: '/pages/profile/profile' }) },
  goPublish() { wx.switchTab({ url: '/pages/publish/publish' }) },
  goOrders() { wx.switchTab({ url: '/pages/orders/orders' }) },

  logout() {
    const app = getApp()
    app.globalData.token = ''
    app.globalData.user = null
    wx.removeStorageSync('cb_token')
    wx.removeStorageSync('cb_user')
    this.setData({ user: null, avatarSrc: '', list: [], stats: { selling: 0, sold: 0, fav: 0 }, unread: 0 })
    wx.showToast({ title: '已退出', icon: 'none' })
  }
})