// pages/publish/publish.js
const api = require('../../utils/api')
const { assetUrl } = require('../../utils/asset')

Page({
  data: {
    form: {
      title: '',
      author: '',
      publisher: '',
      publishYear: '',
      originalPrice: '',
      price: '',
      conditionLevel: 3,
      wearLevel: 3,
      noteFlag: 0,
      description: ''
    },
    conditions: [
      { v: 1, label: '四成新' }, { v: 2, label: '五成新' }, { v: 3, label: '七成新' },
      { v: 4, label: '九成新' }, { v: 5, label: '全新' }
    ],
    wears: [
      { v: 1, label: '磨损明显' }, { v: 2, label: '有折痕' }, { v: 3, label: '正常' },
      { v: 4, label: '接近崭新' }, { v: 5, label: '几乎全新' }
    ],
    suggest: null,
    pricing: false,
    saving: false,
    canPublish: false,
    images: [],
    previewImages: [],
    uploading: false
  },

  onShow() {
    const app = getApp()
    if (!app.globalData.token) {
      wx.navigateTo({ url: '/pages/login/login' })
    }
  },

  onField(e) {
    const key = e.currentTarget.dataset.key
    // 回调里用箭头函数，避免 this 丢失（真实环境和模拟环境行为不完全一致）
    this.setData({ ['form.' + key]: e.detail.value }, () => this.checkReady())
  },

  onCondition(e) {
    this.setData({ 'form.conditionLevel': e.currentTarget.dataset.v }, () => this.checkReady())
  },

  onWear(e) {
    this.setData({ 'form.wearLevel': e.currentTarget.dataset.v })
  },

  toggleNote() {
    this.setData({ 'form.noteFlag': this.data.form.noteFlag ? 0 : 1 })
  },

  checkReady() {
    const f = this.data.form
    this.setData({ canPublish: !!f.title && !!f.originalPrice && !!f.price })
  },

  // 拍照 / 从相册选图，逐张上传后拿到可访问的 url
  chooseImage() {
    const left = 6 - this.data.images.length
    if (left <= 0) {
      wx.showToast({ title: '最多 6 张', icon: 'none' })
      return
    }
    wx.chooseMedia({
      count: left,
      mediaType: ['image'],
      sizeType: ['compressed'],
      success: async (res) => {
        this.setData({ uploading: true })
        const uploaded = this.data.images.slice()
        for (const f of res.tempFiles) {
          try {
            const up = await api.uploadImage(f.tempFilePath)
            uploaded.push(up.url)
          } catch (e) { /* 单张失败不影响其它 */ }
        }
        this.setData({ images: uploaded, previewImages: uploaded.map(assetUrl), uploading: false })
      },
      fail: () => this.setData({ uploading: false })
    })
  },

  removeImage(e) {
    const i = e.currentTarget.dataset.index
    const list = this.data.images.slice()
    list.splice(i, 1)
    this.setData({ images: list, previewImages: list.map(assetUrl) })
  },

  previewImage(e) {
    const i = e.currentTarget.dataset.index
    wx.previewImage({ current: this.data.previewImages[i], urls: this.data.previewImages })
  },

  // 调后端定价模型，拿到建议价与区间
  async askPrice() {
    const f = this.data.form
    if (!f.originalPrice) {
      wx.showToast({ title: '请先填写原价', icon: 'none' })
      return
    }
    this.setData({ pricing: true })
    try {
      const suggest = await api.suggestPrice({
        originalPrice: Number(f.originalPrice),
        conditionLevel: f.conditionLevel,
        wearLevel: f.wearLevel,
        noteFlag: f.noteFlag,
        publishYear: Number(f.publishYear) || 2018,
        listingCount: 2,
        soldCount30d: 1,
        categoryId: 1
      })
      this.setData({ suggest })
    } catch (e) {
      // 已提示
    } finally {
      this.setData({ pricing: false })
    }
  },

  adoptPrice() {
    if (!this.data.suggest) return
    this.setData({ 'form.price': String(this.data.suggest.suggestedPrice) }, () => this.checkReady())
    wx.showToast({ title: '已采纳建议价', icon: 'none' })
  },

  async submit() {
    const f = this.data.form
    if (!f.title) { wx.showToast({ title: '请填写书名', icon: 'none' }); return }
    if (!f.originalPrice) { wx.showToast({ title: '请填写原价', icon: 'none' }); return }
    if (!f.price) { wx.showToast({ title: '请填写售价', icon: 'none' }); return }

    this.setData({ saving: true })
    try {
      await api.publishBook({
        title: f.title,
        author: f.author,
        publisher: f.publisher,
        publishYear: Number(f.publishYear) || null,
        originalPrice: Number(f.originalPrice),
        price: Number(f.price),
        conditionLevel: f.conditionLevel,
        wearLevel: f.wearLevel,
        noteFlag: f.noteFlag,
        description: f.description,
        categoryId: 1,
        coverImg: this.data.images[0] || '',
        images: JSON.stringify(this.data.images),
        suggestedPrice: this.data.suggest ? this.data.suggest.suggestedPrice : null,
        priceLow: this.data.suggest ? this.data.suggest.priceLow : null,
        priceHigh: this.data.suggest ? this.data.suggest.priceHigh : null
      })
      wx.showToast({ title: '发布成功', icon: 'success' })
      setTimeout(() => wx.switchTab({ url: '/pages/me/me' }), 700)
    } catch (e) {
      // 已提示
    } finally {
      this.setData({ saving: false })
    }
  }
})