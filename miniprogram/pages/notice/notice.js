// pages/notice/notice.js
// 站内通知：订单状态、举报结果都在这里
const api = require('../../utils/api')

const TYPE = {
  order: { label: '订单', color: '#2f7d5b', bg: '#eef7f2' },
  report: { label: '举报', color: '#c0563f', bg: '#fbeeeb' },
  book: { label: '书籍', color: '#5b7fa6', bg: '#eef2f8' },
  system: { label: '系统', color: '#8a7a5c', bg: '#f6f2e9' }
}

Page({
  data: {
    rows: [],
    loading: true,
    unread: 0,
    detailOpen: false,
    detail: null,
    page: 1,
    hasMore: true,
    loadingMore: false
  },

  onShow() {
    this.load()
  },

  onReachBottom() {
    this.loadMore()
  },

  async loadMore() {
    if (!this.data.hasMore || this.data.loadingMore || this.data.loading) return
    this.setData({ loadingMore: true })
    try {
      const next = this.data.page + 1
      const data = await api.notifications(next)
      const more = (data.list || []).map((n) => this.decorate(n))
      this.setData({
        rows: this.data.rows.concat(more),
        page: next,
        hasMore: more.length >= 20
      })
    } catch (e) {
      this.setData({ hasMore: false })
    } finally {
      this.setData({ loadingMore: false })
    }
  },

  // 统一补上类型标签与相对时间
  decorate(n) {
    const t = TYPE[n.type] || TYPE.system
    return Object.assign({}, n, {
      typeLabel: t.label,
      typeColor: t.color,
      typeBg: t.bg,
      timeText: this.fmtTime(n.createdAt)
    })
  },

  async load() {
    this.setData({ loading: true, page: 1, hasMore: true })
    try {
      const data = await api.notifications(1)
      const rows = (data.list || []).map((n) => this.decorate(n))
      this.setData({
        rows,
        unread: rows.filter((r) => r.readFlag === 0).length,
        hasMore: rows.length >= 20
      })
    } catch (e) {
      this.setData({ rows: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 简单的相对时间，超过一天就显示日期
  fmtTime(t) {
    if (!t) return ''
    const s = String(t).replace('T', ' ')
    const d = new Date(s.replace(/-/g, '/'))
    const diff = (Date.now() - d.getTime()) / 1000
    if (diff < 60) return '刚刚'
    if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
    if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
    return s.slice(5, 16)
  },

  // 点击先看详情，而不是直接跳走；顺带标记已读
  async onTap(e) {
    const item = e.currentTarget.dataset.item
    if (item.readFlag === 0) {
      api.readNotice(item.id).catch(() => {})
      const rows = this.data.rows.map((r) => r.id === item.id ? Object.assign({}, r, { readFlag: 1 }) : r)
      this.setData({ rows, unread: Math.max(0, this.data.unread - 1) })
    }
    this.setData({ detailOpen: true, detail: item })
  },

  closeDetail() {
    this.setData({ detailOpen: false })
  },

  // 详情里再决定去不去对应页面
  gotoLink() {
    const d = this.data.detail
    if (!d) return
    this.setData({ detailOpen: false })
    if (d.linkType === 'order') {
      wx.switchTab({ url: '/pages/orders/orders' })
    } else if (d.linkType === 'book' && d.linkId) {
      wx.navigateTo({ url: '/pages/detail/detail?id=' + d.linkId })
    }
  },

  // 系统通知（封禁/禁言/改昵称）没有关联对象，给一个可执行的下一步
  gotoProfile() {
    this.setData({ detailOpen: false })
    wx.switchTab({ url: '/pages/me/me' })
  },

  async readAll() {
    if (this.data.unread === 0) return
    try {
      await api.readAllNotice()
      this.setData({
        rows: this.data.rows.map((r) => Object.assign({}, r, { readFlag: 1 })),
        unread: 0
      })
      wx.showToast({ title: '已全部标为已读', icon: 'none' })
    } catch (e) { /* 已提示 */ }
  }
})