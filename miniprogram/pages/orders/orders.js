// pages/orders/orders.js
const api = require('../../utils/api')
const { assetUrl } = require('../../utils/asset')

// 只取到 月-日 时:分，卡片里不占地方
function shortTime(t) {
  if (!t) return ''
  const s = String(t).replace('T', ' ')
  return s.length >= 16 ? s.slice(5, 16) : s
}

/**
 * 把订单状态展开成时间轴节点。
 * 状态：0 待确认 / 1 已确认 / 2 已完成 / 3 已取消
 */
function buildTimeline(o) {
  const list = buildNodes(o)
  // 首尾节点不画外延的连接线，模板里直接读这两个标记，
  // 免得内层 wx:for 的 item 覆盖外层订单对象
  list.forEach((n, i) => {
    n.isFirst = i === 0
    n.isLast = i === list.length - 1
  })
  return list
}

function buildNodes(o) {
  if (o.status === 3) {
    return [
      { name: '下单', time: shortTime(o.createdAt), state: 'done' },
      { name: '已取消', time: shortTime(o.updatedAt || o.createdAt), state: 'cancel' }
    ]
  }

  const confirmed = o.status >= 1
  const finished = o.status === 2
  return [
    { name: '下单', time: shortTime(o.createdAt), state: 'done' },
    {
      name: '卖家确认',
      // 用后端记的 confirmedAt，没有就留空，不拿完成时间去顶替
      time: confirmed ? shortTime(o.confirmedAt || o.updatedAt) : '',
      state: confirmed ? 'done' : 'todo'
    },
    {
      name: '交易完成',
      time: finished ? shortTime(o.finishedAt || o.updatedAt) : '',
      state: finished ? 'done' : 'todo'
    }
  ]
}

Page({
  data: {
    role: 'buyer',
    rows: [],
    loading: true,
    isLogin: false,
    skeleton: true,
    page: 1,
    hasMore: true,
    loadingMore: false,
    reviewOpen: false,
    rating: 5,
    content: '',
    currentOrder: null,
    reviewImages: [],
    reviewPreview: [],
    uploading: false,
    viewOpen: false,
    viewOrder: null,
    appealOpen: false,
    appealOrder: null,
    appealText: ''
  },

  onShow() {
    const app = getApp()
    const isLogin = !!app.globalData.token
    this.setData({ isLogin })
    if (isLogin) this.load()
    else wx.navigateTo({ url: '/pages/login/login' })
  },

  switchRole(e) {
    this.setData({ role: e.currentTarget.dataset.role })
    this.load()
  },

  async load() {
    this.setData({ loading: true, skeleton: true, page: 1, hasMore: true })
    try {
      const data = await api.myOrders(this.data.role, 1)
      const rows = (data.list || []).map((o) => Object.assign({}, o, {
        // 订单接口返回的是相对路径，要拼成完整地址才能显示
        coverSrc: assetUrl(o.bookCover),
        timeline: buildTimeline(o)
      }))
      this.setData({
        rows,
        hasMore: (data.list || []).length >= 10
      })
    } catch (e) {
      this.setData({ rows: [] })
    } finally {
      this.setData({ loading: false, skeleton: false })
    }
  },

  // 触底加载下一页
  async onReachBottom() {
    if (!this.data.hasMore || this.data.loadingMore || this.data.loading) return
    this.setData({ loadingMore: true })
    try {
      const next = this.data.page + 1
      const data = await api.myOrders(this.data.role, next)
      const more = (data.list || []).map((o) => Object.assign({}, o, {
        coverSrc: assetUrl(o.bookCover),
        timeline: buildTimeline(o)
      }))
      this.setData({
        rows: this.data.rows.concat(more),
        page: next,
        hasMore: more.length >= 10
      })
    } catch (e) {
      this.setData({ hasMore: false })
    } finally {
      this.setData({ loadingMore: false })
    }
  },

  onBookTap(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id })
  },

  async onConfirm(e) {
    await api.confirmOrder(e.currentTarget.dataset.id)
    wx.showToast({ title: '已确认', icon: 'success' })
    this.load()
  },

  async onComplete(e) {
    await api.completeOrder(e.currentTarget.dataset.id)
    wx.showToast({ title: '交易完成', icon: 'success' })
    this.load()
  },

  async onCancel(e) {
    const id = e.currentTarget.dataset.id
    const res = await wx.showModal({ title: '提示', content: '确定取消这笔订单吗？' })
    if (!res.confirm) return
    await api.cancelOrder(id)
    wx.showToast({ title: '已取消', icon: 'none' })
    this.load()
  },

  // 已评价：点开看当时写了什么
  openReviewed(e) {
    const o = e.currentTarget.dataset.order
    let imgs = []
    try { imgs = (JSON.parse(o.reviewImages || '[]') || []).map(assetUrl) } catch (err) { imgs = [] }
    this.setData({ viewOpen: true, viewOrder: Object.assign({}, o, { imgs }) })
  },

  closeViewed() {
    this.setData({ viewOpen: false })
  },

  previewViewed(e) {
    const i = e.currentTarget.dataset.index
    wx.previewImage({ current: this.data.viewOrder.imgs[i], urls: this.data.viewOrder.imgs })
  },

  openAppeal(e) {
    this.setData({ appealOpen: true, appealOrder: e.currentTarget.dataset.order, appealText: '' })
  },

  closeAppeal() {
    this.setData({ appealOpen: false })
  },

  onAppealInput(e) {
    this.setData({ appealText: e.detail.value })
  },

  async submitAppeal() {
    const text = (this.data.appealText || '').trim()
    if (!text) {
      wx.showToast({ title: '请说明情况', icon: 'none' })
      return
    }
    try {
      await api.report({
        targetType: 'order',
        targetId: this.data.appealOrder.id,
        reason: '交易纠纷',
        detail: text
      })
      this.setData({ appealOpen: false })
      wx.showToast({ title: '已提交，等待管理员处理', icon: 'none' })
    } catch (e) { /* 已提示 */ }
  },

  openReview(e) {
    this.setData({
      currentOrder: e.currentTarget.dataset.order,
      reviewOpen: true,
      rating: 5,
      content: '',
      reviewImages: [],
      reviewPreview: []
    })
  },

  // 评价配图最多 3 张，复用发布页的上传逻辑
  chooseReviewImage() {
    const left = 3 - this.data.reviewImages.length
    if (left <= 0) {
      wx.showToast({ title: '最多 3 张', icon: 'none' })
      return
    }
    wx.chooseMedia({
      count: left,
      mediaType: ['image'],
      sizeType: ['compressed'],
      success: async (res) => {
        this.setData({ uploading: true })
        const paths = this.data.reviewImages.slice()
        for (const f of res.tempFiles) {
          try {
            const up = await api.uploadImage(f.tempFilePath)
            paths.push(up.url)
          } catch (e) { /* 单张失败不阻塞 */ }
        }
        this.setData({
          reviewImages: paths,
          reviewPreview: paths.map(assetUrl),
          uploading: false
        })
      },
      fail: () => this.setData({ uploading: false })
    })
  },

  removeReviewImage(e) {
    const i = e.currentTarget.dataset.index
    const paths = this.data.reviewImages.slice()
    paths.splice(i, 1)
    this.setData({ reviewImages: paths, reviewPreview: paths.map(assetUrl) })
  },

  previewReviewImage(e) {
    const i = e.currentTarget.dataset.index
    wx.previewImage({ current: this.data.reviewPreview[i], urls: this.data.reviewPreview })
  },

  closeReview() {
    this.setData({ reviewOpen: false })
  },

  setRating(e) {
    this.setData({ rating: Number(e.currentTarget.dataset.n) })
  },

  onContent(e) {
    this.setData({ content: e.detail.value })
  },

  async submitReview() {
    try {
      await api.createReview({
        orderId: this.data.currentOrder.id,
        rating: this.data.rating,
        content: this.data.content,
        images: JSON.stringify(this.data.reviewImages)
      })
      this.setData({ reviewOpen: false })
      wx.showToast({ title: '评价成功', icon: 'success' })
      this.load()
    } catch (e) { /* 已提示 */ }
  }
})