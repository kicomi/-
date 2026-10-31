// pages/profile/profile.js
const api = require('../../utils/api')
const { assetUrl } = require('../../utils/asset')

Page({
  data: {
    majors: [],
    genders: ['男', '女', '保密'],
    genderIndex: 2,
    // customMajor 手动填写优先；majorId 只用于字典联想
    form: { nickname: '', signature: '', avatar: '', grade: '', gender: '保密', customMajor: '' },
    previewAvatar: '',
    initial: '书',
    saving: false,
    uploading: false
  },

  async onLoad() {
    const app = getApp()
    const u = app.globalData.user || {}

    try {
      const dict = await api.getDict()
      this.setData({ majors: dict.majors || [] })
    } catch (e) { /* ignore */ }

    const gi = Math.max(0, this.data.genders.indexOf(u.gender || '保密'))
    this.setData({
      genderIndex: gi,
      form: {
        nickname: u.nickname || '',
        signature: u.signature || '',
        avatar: u.avatar || '',
        grade: u.grade || '',
        gender: u.gender || '保密',
        // 老用户可能只有 majorId，回填时换成名字方便直接编辑
        customMajor: u.majorName || u.customMajor || ''
      },
      previewAvatar: assetUrl(u.avatar),
      initial: (u.nickname || '书').slice(0, 1)
    })
  },

  onField(e) {
    const key = e.currentTarget.dataset.key
    const patch = { ['form.' + key]: e.detail.value }
    if (key === 'nickname') {
      patch.initial = (e.detail.value || '书').slice(0, 1)
    }
    this.setData(patch)
  },

  onGender(e) {
    const i = Number(e.detail.value)
    this.setData({ genderIndex: i, 'form.gender': this.data.genders[i] })
  },

  // 点字典里的专业直接填进去，仍然可以手动改
  pickMajor(e) {
    const name = e.currentTarget.dataset.name
    this.setData({ 'form.customMajor': name })
  },

  chooseAvatar() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sizeType: ['compressed'],
      success: async (res) => {
        const path = res.tempFiles[0].tempFilePath
        this.setData({ uploading: true })
        try {
          const up = await api.uploadImage(path)
          // 存相对路径，展示用完整地址
          this.setData({
            'form.avatar': up.url,
            previewAvatar: assetUrl(up.url)
          })
          wx.showToast({ title: '头像已更新', icon: 'none' })
        } catch (e) { /* 已提示 */ } finally {
          this.setData({ uploading: false })
        }
      }
    })
  },

  async save() {
    const f = this.data.form
    if (!f.nickname.trim()) {
      wx.showToast({ title: '昵称不能为空', icon: 'none' })
      return
    }
    this.setData({ saving: true })
    try {
      const u = await api.updateProfile({
        nickname: f.nickname,
        signature: f.signature,
        avatar: f.avatar,
        grade: f.grade,
        gender: f.gender,
        customMajor: f.customMajor || ''
      })
      const app = getApp()
      app.globalData.user = u
      wx.setStorageSync('cb_user', u)
      wx.showToast({ title: '已保存', icon: 'success' })
      setTimeout(() => wx.navigateBack({ delta: 1 }), 600)
    } catch (e) { /* 已提示 */ } finally {
      this.setData({ saving: false })
    }
  }
})
