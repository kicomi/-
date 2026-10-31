// pages/user/user.js
// 别人点开我的主页：看信用、看口碑、看他还在卖什么
const api = require('../../utils/api')
const { assetUrl } = require('../../utils/asset')

Page({
  data: {
    uid: null,
    profile: null,
    books: [],
    loading: true,
    tab: 'selling',
    levelName: '',
    creditPercent: 0
  },

  onLoad(options) {
    this.setData({ uid: Number(options.id) })
    this.load()
  },

  async load() {
    const id = this.data.uid
    try {
      const p = await api.sellerProfile(id)
      p.avatar = assetUrl(p.avatar)
      p.reviews = (p.recentReviews || []).map((r) => Object.assign({}, r, {
        initial: (r.fromNickname || '同').slice(0, 1)
      }))
      const score = p.creditScore == null ? 100 : p.creditScore
      this.setData({
        profile: p,
        levelName: score >= 95 ? '黄金信用' : score >= 85 ? '白银信用' : score >= 70 ? '青铜信用' : '待提升',
        creditPercent: Math.max(4, Math.min(100, score))
      })
      wx.setNavigationBarTitle({ title: p.nickname || '个人主页' })
    } catch (e) {
      this.setData({ profile: null })
    }

    try {
      const d = await api.publicBooks({ sellerId: id, size: 12 })
      this.setData({
        books: (d.list || []).map((b) => Object.assign({}, b, { coverImg: assetUrl(b.coverImg) }))
      })
    } catch (e) { /* 没有在售也正常 */ }

    this.setData({ loading: false })
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
  },

  goBook(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id })
  },

  goChat() {
    const p = this.data.profile
    wx.navigateTo({
      url: '/pages/chat/chat?peerId=' + p.userId + '&peerName=' + encodeURIComponent(p.nickname || '同学')
    })
  },

  goReviews() {
    const p = this.data.profile
    const first = this.data.books[0]
    // 评价页是按书聚合的，这里没有书就退化成个人页的最近评价
    if (first) {
      wx.navigateTo({ url: '/pages/reviews/reviews?id=' + first.id })
    } else {
      wx.showToast({ title: '暂无在售书籍', icon: 'none' })
    }
  }
})