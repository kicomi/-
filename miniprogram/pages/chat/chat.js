// pages/chat/chat.js
// 一个页面承担两件事：
//   - 不带 peerId：会话列表
//   - 带 peerId：与某个人的对话
const api = require('../../utils/api')
const { assetUrl } = require('../../utils/asset')

Page({
  data: {
    peerId: null,
    peerName: '',
    peerAvatar: '',
    peerInitial: '友',
    bookId: null,
    bookTitle: '',
    mode: 'list',
    conversations: [],
    messages: [],
    input: '',
    myId: null,
    myName: '',
    myAvatar: '',
    myInitial: '我',
    sending: false,
    // 校园场景话术高度重复，给几个快捷短语省打字
    quickPhrases: ['这本书还在吗？', '书况怎么样？', '可以便宜点吗？', '今晚方便见面吗？'],
    showQuick: false
  },

  onLoad(options) {
    const app = getApp()
    const my = app.globalData.user || {}
    this.setData({
      myId: my.id,
      myName: my.nickname || '我',
      myAvatar: assetUrl(my.avatar),
      myInitial: (my.nickname || '我').slice(0, 1)
    })

    if (options.peerId) {
      const peerName = decodeURIComponent(options.peerName || '对方')
      this.setData({
        mode: 'chat',
        peerId: Number(options.peerId),
        peerName,
        peerAvatar: assetUrl(decodeURIComponent(options.peerAvatar || '')),
        peerInitial: peerName.slice(0, 1),
        bookId: options.bookId ? Number(options.bookId) : null,
        bookTitle: decodeURIComponent(options.bookTitle || '')
      })
      // 标题优先显示书名：聊天时更关心"在聊哪本书"，对方是谁看头像就行
      wx.setNavigationBarTitle({ title: this.data.bookTitle ? '《' + this.data.bookTitle + '》' : peerName })
      this.loadHistory()
    } else {
      wx.setNavigationBarTitle({ title: '消息' })
      this.loadConversations()
    }
  },

  async loadConversations() {
    try {
      const list = await api.conversations()
      ;(list || []).forEach((c) => {
        c.avatarSrc = assetUrl(c.peerAvatar)
        c.initial = (c.peerNickname || '友').slice(0, 1)
      })
      this.setData({ conversations: list || [] })
    } catch (e) {
      this.setData({ conversations: [] })
    }
  },

  async loadHistory() {
    try {
      const list = await api.chatHistory(this.data.peerId, this.data.bookId)

      // 从会话列表点进来时没有 bookTitle，用消息里带的书名补上，
      // 保证任何入口进来标题都是书名
      if (!this.data.bookTitle) {
        const withBook = (list || []).find((m) => m.bookTitle)
        if (withBook) {
          this.setData({ bookTitle: withBook.bookTitle })
          wx.setNavigationBarTitle({ title: '《' + withBook.bookTitle + '》' })
        }
      }

      this.setData({ messages: list || [] })
      this.scrollToBottom()
    } catch (e) { /* 已提示 */ }
  },

  openConversation(e) {
    const item = e.currentTarget.dataset.item
    wx.navigateTo({
      url: '/pages/chat/chat?peerId=' + item.peerId +
           '&peerName=' + encodeURIComponent(item.peerNickname) +
           '&peerAvatar=' + encodeURIComponent(item.peerAvatar || '')
    })
  },

  onInput(e) {
    this.setData({ input: e.detail.value })
  },

  toggleQuick() {
    this.setData({ showQuick: !this.data.showQuick })
  },

  usePhrase(e) {
    const text = e.currentTarget.dataset.text
    this.setData({ input: text, showQuick: false })
  },

  async send() {
    const text = (this.data.input || '').trim()
    if (!text || this.data.sending) return
    this.setData({ sending: true })
    try {
      await api.sendMessage({
        toUserId: this.data.peerId,
        bookId: this.data.bookId,
        content: text
      })
      this.setData({ input: '' })
      await this.loadHistory()
    } catch (e) {
      // 被禁言时后端会返回提示，这里不清空输入框方便重试
    } finally {
      this.setData({ sending: false })
    }
  },

  scrollToBottom() {
    const n = this.data.messages.length
    if (n > 0) this.setData({ scrollTo: 'msg-' + (n - 1) })
  },

  goBook() {
    if (this.data.bookId) {
      wx.navigateTo({ url: '/pages/detail/detail?id=' + this.data.bookId })
    }
  }
})