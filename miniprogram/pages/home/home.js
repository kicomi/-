// pages/home/home.js
const api = require('../../utils/api')
const { assetUrl } = require('../../utils/asset')

// 楼层里的封面是相对路径，必须补上服务地址，否则小程序加载不出来（白块）
function withCover(list) {
  return (list || []).map((b) => Object.assign({}, b, { coverImg: assetUrl(b.coverImg) }))
}

Page({
  data: {
    banners: [],
    categories: [],
    recommended: [],
    latest: [],
    sameMajor: [],
    cheap: [],
    sold: [],
    stats: { onSale: 0 },
    nickname: '',
    loadingRec: true,
    skeleton: true
  },

  onShow() {
    // 首次进入先亮骨架屏，减少白屏感
    if (!this._loaded) setTimeout(() => this.setData({ skeleton: false }), 900)
    const app = getApp()
    const user = app.globalData.user
    this.setData({ nickname: (user && user.nickname) || '' })

    if (!this._loaded) {
      this._loaded = true
      this.loadBase()
      this.loadFloors()
    }
    this.loadRecommend()
  },

  async loadBase() {
    try {
      const dict = await api.getDict()
      this.setData({ categories: (dict.categories || []).slice(0, 8) })
    } catch (e) { /* 字典失败不影响首页 */ }

    try {
      this.setData({ banners: await api.getBanners() })
    } catch (e) { /* 没广告也能展示 */ }

    try {
      const home = await api.publicHome()
      this.setData({ latest: home.latest || [], 'stats.onSale': (home.latest || []).length })
    } catch (e) { /* ignore */ }

    try {
      const all = await api.publicBooks({ page: 1, size: 1 })
      this.setData({ 'stats.onSale': all.total || 0 })
    } catch (e) { /* ignore */ }
  },

  // 三个运营楼层：同专业、低价、刚成交
  async loadFloors() {
    const app = getApp()
    const user = app.globalData.user || {}

    try {
      const params = { page: 1, size: 8, sort: 'hot' }
      if (user.majorId) params.majorId = user.majorId
      const d = await api.publicBooks(params)
      this.setData({ sameMajor: withCover(d.list) })
    } catch (e) { /* ignore */ }

    try {
      const d = await api.publicBooks({ page: 1, size: 8, priceMax: 20, sort: 'priceAsc' })
      this.setData({ cheap: withCover(d.list) })
    } catch (e) { /* ignore */ }

    try {
      // status=2 是已售出，用来做"刚刚成交"的信任感展示
      const d = await api.publicBooks({ page: 1, size: 8, status: 2 })
      this.setData({ sold: withCover(d.list) })
    } catch (e) { /* ignore */ }
  },

  async loadRecommend() {
    const app = getApp()
    this.setData({ loadingRec: true })
    try {
      if (app.globalData.token) {
        const rec = await api.recommend(6)
        this.setData({ recommended: rec.items || [] })
      } else {
        const hot = await api.publicBooks({ page: 1, size: 6, sort: 'hot' })
        this.setData({ recommended: hot.list || [] })
      }
    } catch (e) {
      this.setData({ recommended: [] })
    } finally {
      this.setData({ loadingRec: false })
    }
  },

  onBookTap(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.detail.book.id })
  },

  onMiniBookTap(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id })
  },

  goSearchWith(params) {
    getApp().globalData.searchParams = params || {}
    wx.switchTab({ url: '/pages/search/search' })
  },

  onCategoryTap(e) {
    this.goSearchWith({ categoryId: e.currentTarget.dataset.id })
  },

  onBannerTap(e) {
    const b = e.currentTarget.dataset.banner
    api.bannerView(b.id).catch(() => {})
    if (b.linkType === 'category') this.goSearchWith({ categoryId: b.linkValue })
    else if (b.linkType === 'search') this.goSearchWith({ keyword: b.linkValue })
    else this.goSearchWith({})
  },

  // 楼层换一批：换一页数据，给用户"还有更多"的感觉
  refreshFloor(e) {
    const which = e.currentTarget.dataset.which
    const page = 1 + Math.floor(Math.random() * 3)
    const user = (getApp().globalData.user) || {}
    wx.showLoading({ title: '换一批', mask: true })

    const done = () => wx.hideLoading()
    if (which === 'major') {
      const params = { page, size: 8, sort: 'hot' }
      if (user.majorId) params.majorId = user.majorId
      api.publicBooks(params).then((d) => {
        this.setData({ sameMajor: withCover(d.list) })
        done()
      }).catch(done)
    } else if (which === 'cheap') {
      api.publicBooks({ page, size: 8, priceMax: 20, sort: 'priceAsc' }).then((d) => {
        this.setData({ cheap: withCover(d.list) })
        done()
      }).catch(done)
    } else {
      api.publicBooks({ page, size: 8, status: 2 }).then((d) => {
        this.setData({ sold: withCover(d.list) })
        done()
      }).catch(done)
    }
  },

  goSearch() {
    this.goSearchWith({})
  },

  goCheap() {
    this.goSearchWith({ cheap: 1 })
  }
})