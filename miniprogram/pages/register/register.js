// pages/register/register.js
const api = require('../../utils/api')

Page({
  data: {
    majors: [],
    majorIndex: 0,
    form: {
      account: '',
      password: '',
      nickname: '',
      realName: '',
      studentNo: '',
      grade: '2024',
      gender: '保密',
      customMajor: ''
    },
    genders: ['男', '女', '保密'],
    genderIndex: 2,
    submitting: false
  },

  async onLoad() {
    try {
      const dict = await api.getDict()
      this.setData({ majors: dict.majors || [] })
    } catch (e) { /* 字典失败时仍可注册，专业留空 */ }
  },

  onField(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ ['form.' + key]: e.detail.value })
  },

  onMajor(e) {
    this.setData({ majorIndex: Number(e.detail.value) })
  },

  pickMajor(e) {
    this.setData({ 'form.customMajor': e.currentTarget.dataset.name })
  },

  onGender(e) {
    const i = Number(e.detail.value)
    this.setData({ genderIndex: i, 'form.gender': this.data.genders[i] })
  },

  async submit() {
    const f = this.data.form
    if (!f.account) return wx.showToast({ title: '请填写手机号或学号', icon: 'none' })
    if (!f.password || f.password.length < 6) return wx.showToast({ title: '密码至少 6 位', icon: 'none' })
    if (!f.nickname) return wx.showToast({ title: '请填写昵称', icon: 'none' })

    this.setData({ submitting: true })
    try {
      const major = this.data.majors[this.data.majorIndex]
      const data = await api.register({
        account: f.account,
        password: f.password,
        nickname: f.nickname,
        realName: f.realName,
        studentNo: f.studentNo,
        majorId: f.customMajor ? null : (major ? major.id : null),
        customMajor: f.customMajor,
        grade: f.grade,
        gender: f.gender
      })
      const app = getApp()
      app.globalData.token = data.token
      app.globalData.user = data.user
      wx.setStorageSync('cb_token', data.token)
      wx.setStorageSync('cb_user', data.user)
      wx.showToast({ title: '注册成功', icon: 'success' })
      setTimeout(() => wx.switchTab({ url: '/pages/home/home' }), 700)
    } catch (e) {
      // 已提示
    } finally {
      this.setData({ submitting: false })
    }
  },

  goLogin() {
    wx.navigateBack({ delta: 1 })
  }
})