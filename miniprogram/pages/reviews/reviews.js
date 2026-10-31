// pages/reviews/reviews.js
// 某本书的全部评价，带分页
const api = require('../../utils/api')
const { assetUrl } = require('../../utils/asset')

Page({
  data: {
    bookId: null,
    title: '',
    summary: null,
    rows: [],
    page: 1,
    hasMore: true,
    loading: true,
    loadingMore: false,
    skeleton: true
  },

  onLoad(options) {
    this.setData({
      bookId: Number(options.id),
      title: decodeURIComponent(options.title || '')
    })
    if (this.data.title) {
      wx.setNavigationBarTitle({ title: this.data.title + ' · 评价' })
    }
    this.loadSummary()
    this.loadPage(1)
  },

  async loadSummary() {
    try {
      const s = await api.reviewsSummary(this.data.bookId)
      // 后端给的是 1-5 星各自数量，这里补上百分比给柱状条用
      const total = s.total || 0
      s.bars = (s.dist || []).map((n, i) => ({
        star: i + 1,
        count: n,
        percent: total ? Math.round(n * 100 / total) : 0
      })).reverse()
      this.setData({ summary: s })
    } catch (e) { /* 概况失败不影响列表 */ }
  },

  async loadPage(page) {
    page === 1 ? this.setData({ loading: true, skeleton: true }) : this.setData({ loadingMore: true })
    try {
      const data = await api.reviewsPage(this.data.bookId, page)
      const list = (data.list || []).map((r) => {
        let imgs = []
        try { imgs = (JSON.parse(r.images || '[]') || []).map(assetUrl) } catch (e) { imgs = [] }
        return Object.assign({}, r, {
          imgs,
          initial: (r.fromNickname || '同').slice(0, 1),
          timeText: r.createdAt ? String(r.createdAt).slice(0, 10) : ''
        })
      })
      this.setData({
        rows: page === 1 ? list : this.data.rows.concat(list),
        page,
        hasMore: list.length >= 10
      })
    } catch (e) {
      this.setData(page === 1 ? { rows: [] } : { hasMore: false })
    } finally {
      this.setData({ loading: false, loadingMore: false, skeleton: false })
    }
  },

  onReachBottom() {
    if (!this.data.hasMore || this.data.loadingMore) return
    this.loadPage(this.data.page + 1)
  },

  goUser(e) {
    const uid = e.currentTarget.dataset.uid
    if (uid) wx.navigateTo({ url: '/pages/user/user?id=' + uid })
  },

  preview(e) {
    const item = e.currentTarget.dataset.item
    wx.previewImage({ current: e.currentTarget.dataset.src, urls: item.imgs })
  }
})