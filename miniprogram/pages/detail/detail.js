// pages/detail/detail.js
const api = require('../../utils/api')
const { assetUrl } = require('../../utils/asset')

const CONDITION = { 1: '四成新', 2: '五成新', 3: '七成新', 4: '九成新', 5: '全新' }
const WEAR = { 1: '磨损明显', 2: '有折痕', 3: '正常使用痕迹', 4: '接近崭新', 5: '几乎全新' }

Page({
  data: {
    id: '',
    book: null,
    seller: null,
    similar: [],
    favorited: false,
    sellerLetter: '同',
    conditionText: '',
    saving: 0,
    dims: [],
    gallery: [],
    sellerBooks: [],
    galleryIndex: 0,
    sheetOpen: false,
    pickupPlace: '图书馆一楼',
    places: ['图书馆一楼', '一食堂门口', '三号教学楼', '宿舍楼下'],
    remark: '',
    bargainOpen: false,
    bargainPrice: '',
    reportOpen: false,
    reportReason: '',
    reportDetail: '',
    reportReasons: ['书况与描述不符', '疑似虚假信息', '价格异常', '涉嫌诈骗', '内容违规', '其他']
  },

  onLoad(options) {
    this.setData({ id: options.id })
    this.load()
  },

  async load() {
    const app = getApp()
    let book = null
    try {
      book = app.globalData.token
        ? await api.bookDetail(this.data.id)
        : await api.publicBook(this.data.id)
    } catch (e) {
      return
    }

    // 图片：coverImg + images(JSON 字符串) 去重合并
    const imgs = []
    if (book.coverImg) imgs.push(assetUrl(book.coverImg))
    if (book.images) {
      try {
        const arr = JSON.parse(book.images)
        if (Array.isArray(arr)) {
          arr.forEach((u) => {
            const full = assetUrl(u)
            if (full && imgs.indexOf(full) < 0) imgs.push(full)
          })
        }
      } catch (e) { /* 老数据可能不是 JSON，忽略 */ }
    }

    const orig = Number(book.originalPrice) || 0
    const price = Number(book.price) || 0
    const saving = orig > 0 ? Math.round((1 - price / orig) * 100) : 0

    const dims = [
      { label: '品相', value: book.conditionLevel || 3, text: CONDITION[book.conditionLevel] || '七成新' },
      { label: '新旧', value: book.wearLevel || 3, text: WEAR[book.wearLevel] || '正常使用痕迹' },
      { label: '笔记', value: book.noteFlag === 1 ? 4 : 5, text: book.noteFlag === 1 ? '有少量笔记' : '无笔记划线' },
      { label: '热度', value: Math.min(5, (book.soldCount30d || 0) + 1), text: '同类近30天成交 ' + (book.soldCount30d || 0) }
    ]

    this.setData({
      book,
      gallery: imgs,
      sellerLetter: (book.sellerNickname || '同').slice(0, 1),
      saving,
      dims,
      conditionText: CONDITION[book.conditionLevel] || '七成新',
      favorited: !!book.favorited
    })

    // 卖家画像：成交数、好评率、最近评价
    if (book.sellerId) {
      try {
        const s = await api.sellerProfile(book.sellerId)
        s.avatarSrc = assetUrl(s.avatar)
        // 评价里的图片是 JSON 字符串，解析成数组并拼上完整地址
        ;(s.recentReviews || []).forEach((r) => {
          try {
            r.imgs = (JSON.parse(r.images || '[]') || []).map(assetUrl)
          } catch (e) { r.imgs = [] }
        })
        this.setData({ seller: s })
      } catch (e) { /* 失败就不展示这一块 */ }
    }

    // 卖家其他在售：想凑单买两本时不用回搜索页
    if (book.sellerId) {
      try {
        const d = await api.publicBooks({ sellerId: book.sellerId, size: 8 })
        const others = (d.list || []).filter((x) => x.id !== book.id).map((x) => {
          x.coverImg = assetUrl(x.coverImg)
          return x
        })
        this.setData({ sellerBooks: others })
      } catch (e) { /* ignore */ }
    }

    try {
      const sim = await api.similar(this.data.id, 6)
      sim.forEach((x) => { x.coverImg = assetUrl(x.coverImg) })
      this.setData({ similar: sim })
    } catch (e) { /* ignore */ }
  },

  onSwiperChange(e) {
    this.setData({ galleryIndex: e.detail.current })
  },

  previewGallery(e) {
    const i = e.currentTarget.dataset.index
    if (!this.data.gallery.length) return
    wx.previewImage({ current: this.data.gallery[i], urls: this.data.gallery })
  },

  // 已售出时引导：优先用课程名搜同款，没有就按书名
  findSimilar() {
    const b = this.data.book
    const keyword = b.courseName || (b.title || '').replace(/（.*?）/g, '')
    getApp().globalData.searchParams = { keyword }
    wx.switchTab({ url: '/pages/search/search' })
  },

  // 点卖家头像/昵称看他的主页
  goSeller() {
    const b = this.data.book
    if (!b || !b.sellerId) return
    wx.navigateTo({ url: '/pages/user/user?id=' + b.sellerId })
  },

  goReviews() {
    const b = this.data.book
    if (!b) return
    wx.navigateTo({
      url: '/pages/reviews/reviews?id=' + b.id + '&title=' + encodeURIComponent(b.title || '')
    })
  },

  previewReview(e) {
    const src = e.currentTarget.dataset.src
    const imgs = []
    ;(this.data.seller.recentReviews || []).forEach((r) => {
      (r.imgs || []).forEach((i) => imgs.push(i))
    })
    wx.previewImage({ current: src, urls: imgs })
  },

  onSimilarTap(e) {
    wx.redirectTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id })
  },

  async toggleFav() {
    const app = getApp()
    if (!app.globalData.token) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    try {
      const now = await api.favorite(this.data.id, !this.data.favorited)
      this.setData({ favorited: !!now })
      wx.showToast({ title: now ? '已收藏' : '已取消', icon: 'none' })
    } catch (e) { /* 已提示 */ }
  },

  contactSeller() {
    const app = getApp()
    if (!app.globalData.token) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    const b = this.data.book
    if (!b) return
    if (b.sellerId === (app.globalData.user || {}).id) {
      wx.showToast({ title: '这是你自己发布的', icon: 'none' })
      return
    }
    wx.navigateTo({
      url: '/pages/chat/chat?peerId=' + b.sellerId +
           '&peerName=' + encodeURIComponent(b.sellerNickname || '卖家') +
           '&peerAvatar=' + encodeURIComponent(b.sellerAvatar || '') +
           '&bookId=' + b.id +
           '&bookTitle=' + encodeURIComponent(b.title || '')
    })
  },

  // 还价：把价格作为一条消息发给卖家，复用现有聊天通道
  openBargain() {
    const app = getApp()
    if (!app.globalData.token) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    const b = this.data.book
    // 默认给一个比标价低 10% 的整数
    const suggest = Math.max(1, Math.round(Number(b.price) * 0.9))
    this.setData({ bargainOpen: true, bargainPrice: String(suggest) })
  },

  closeBargain() {
    this.setData({ bargainOpen: false })
  },

  onBargainInput(e) {
    this.setData({ bargainPrice: e.detail.value })
  },

  async sendBargain() {
    const price = Number(this.data.bargainPrice)
    const b = this.data.book
    if (!price || price <= 0) {
      wx.showToast({ title: '请输入价格', icon: 'none' })
      return
    }
    if (price >= Number(b.price)) {
      wx.showToast({ title: '还价要低于标价', icon: 'none' })
      return
    }
    try {
      await api.sendMessage({
        toUserId: b.sellerId,
        bookId: b.id,
        content: '我对《' + b.title + '》出价 ¥' + price + '，可以吗？'
      })
      this.setData({ bargainOpen: false })
      wx.showToast({ title: '已发给卖家', icon: 'success' })
    } catch (e) { /* 已提示 */ }
  },

  openReport() {
    const app = getApp()
    if (!app.globalData.token) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    this.setData({ reportOpen: true, reportReason: '', reportDetail: '' })
  },

  closeReport() {
    this.setData({ reportOpen: false })
  },

  pickReason(e) {
    this.setData({ reportReason: e.currentTarget.dataset.reason })
  },

  onReportDetail(e) {
    this.setData({ reportDetail: e.detail.value })
  },

  async submitReport() {
    if (!this.data.reportReason) {
      wx.showToast({ title: '请选择举报原因', icon: 'none' })
      return
    }
    try {
      await api.report({
        targetType: 'book',
        targetId: Number(this.data.id),
        reason: this.data.reportReason,
        detail: this.data.reportDetail
      })
      this.setData({ reportOpen: false })
      wx.showToast({ title: '已提交，管理员会尽快处理', icon: 'none' })
    } catch (e) { /* 已提示 */ }
  },

  // 转发给同学
  onShareAppMessage() {
    const b = this.data.book || {}
    return {
      title: b.title || '校园二手书',
      path: '/pages/detail/detail?id=' + this.data.id,
      imageUrl: (this.data.gallery || [])[0] || ''
    }
  },

  openSheet() {
    const app = getApp()
    if (!app.globalData.token) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    this.setData({ sheetOpen: true })
  },

  closeSheet() {
    this.setData({ sheetOpen: false })
  },

  onPlaceChange(e) {
    this.setData({ pickupPlace: this.data.places[e.detail.value] })
  },

  onRemarkInput(e) {
    this.setData({ remark: e.detail.value })
  },

  async submitOrder() {
    try {
      await api.createOrder({
        bookId: Number(this.data.id),
        pickupPlace: this.data.pickupPlace,
        remark: this.data.remark
      })
      this.setData({ sheetOpen: false })
      const b = this.data.book
      // 预约后买家最想干的是跟卖家约时间，直接给入口
      wx.showModal({
        title: '预约成功',
        content: '要现在跟卖家约个交接时间吗？',
        confirmText: '去聊天',
        cancelText: '稍后',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({
              url: '/pages/chat/chat?peerId=' + b.sellerId +
                   '&peerName=' + encodeURIComponent(b.sellerNickname || '卖家') +
                   '&bookId=' + b.id + '&bookTitle=' + encodeURIComponent(b.title || '')
            })
          } else {
            wx.switchTab({ url: '/pages/orders/orders' })
          }
        }
      })
    } catch (e) { /* 已提示 */ }
  }
})