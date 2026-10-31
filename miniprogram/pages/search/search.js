// pages/search/search.js
const api = require('../../utils/api')

Page({
  data: {
    keyword: '',
    sort: 'newest',
    conditionMin: 0,
    cheap: false,
    categoryId: '',
    rows: [],
    total: 0,
    loading: false,
    isLogin: false,
    hot: [],
    history: []
  },

  onLoad(options) {
    // switchTab 不能带参数，首页把条件放进 globalData 由这里取走
    const app = getApp()
    const passed = app.globalData.searchParams || {}
    app.globalData.searchParams = null

    this.setData({
      keyword: passed.keyword || options.keyword || '',
      categoryId: passed.categoryId || options.categoryId || ''
    })
    this.loadHot()
    this.setData({ history: wx.getStorageSync('cb_search_history') || [] })
    this.load()
  },

  onShow() {
    const app = getApp()
    const isLogin = !!app.globalData.token
    if (isLogin !== this.data.isLogin) {
      this.setData({ isLogin })
      this.load()
    }
  },

  onInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  async loadHot() {
    try {
      this.setData({ hot: await api.hotKeywords() })
    } catch (e) { /* 热词失败不影响搜索 */ }
  },

  // 搜索历史存本地，最多留 8 条，重复的提到最前面
  saveHistory(word) {
    if (!word) return
    const list = (wx.getStorageSync('cb_search_history') || []).filter((w) => w !== word)
    list.unshift(word)
    const trimmed = list.slice(0, 8)
    wx.setStorageSync('cb_search_history', trimmed)
    this.setData({ history: trimmed })
  },

  clearHistory() {
    wx.removeStorageSync('cb_search_history')
    this.setData({ history: [] })
  },

  tapWord(e) {
    const word = e.currentTarget.dataset.word
    this.setData({ keyword: word })
    this.saveHistory(word)
    this.load()
  },

  onSearch() {
    this.saveHistory((this.data.keyword || '').trim())
    this.load()
  },

  onSort(e) {
    this.setData({ sort: e.currentTarget.dataset.sort })
    this.load()
  },

  toggleCondition() {
    this.setData({ conditionMin: this.data.conditionMin ? 0 : 4 })
    this.load()
  },

  toggleCheap() {
    this.setData({ cheap: !this.data.cheap })
    this.load()
  },

  async load() {
    this.setData({ loading: true })
    const app = getApp()
    // 空的筛选条件不要传给后端，否则 Long 转换会报错
    const params = {
      page: 1,
      size: 20,
      sort: this.data.sort,
      keyword: this.data.keyword || '',
      conditionMin: this.data.conditionMin || '',
      priceMax: this.data.cheap ? 20 : '',
      categoryId: this.data.categoryId || ''
    }
    try {
      const data = app.globalData.token
        ? await api.searchBooks(params)
        : await api.publicBooks(params)
      this.setData({ rows: data.list || [], total: data.total || 0 })
    } catch (e) {
      this.setData({ rows: [], total: 0 })
    } finally {
      this.setData({ loading: false })
    }
  },

  onBookTap(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.detail.book.id })
  }
})