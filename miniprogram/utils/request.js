// utils/request.js
// 统一封装 wx.request：带 token、拼 baseUrl、处理 { code, message, data }
const app = () => getApp()

/**
 * 去掉 undefined / null / 空串。
 * 小程序的 GET 请求会把 data 直接拼成 query，值为 undefined 时会变成字符串
 * "undefined"，后端 Long 类型转换就会报错（踩过一次：categoryId=undefined）。
 */
function cleanParams(data) {
  const out = {}
  Object.keys(data || {}).forEach((k) => {
    const v = data[k]
    if (v === undefined || v === null || v === '') return
    out[k] = v
  })
  return out
}

function request(options) {
  const { url, method = 'GET', data = {}, header = {} } = options
  const base = app().globalData.baseUrl
  const token = app().globalData.token
  const payload = method === 'GET' ? cleanParams(data) : data

  return new Promise((resolve, reject) => {
    wx.request({
      url: base + url,
      method,
      data: payload,
      header: Object.assign(
        { 'content-type': 'application/json' },
        token ? { Authorization: 'Bearer ' + token } : {},
        header
      ),
      success(res) {
        const body = res.data || {}
        if (body.code === 0) {
          resolve(body.data)
          return
        }
        // 被封禁：清掉登录态，避免用户拿着失效身份反复试
        if (body.code === 403 && String(body.message || '').indexOf('封禁') >= 0) {
          app().globalData.token = ''
          app().globalData.user = null
          wx.removeStorageSync('cb_token')
          wx.removeStorageSync('cb_user')
          wx.showModal({
            title: '账号已被封禁',
            content: body.message,
            showCancel: false,
            success: () => wx.reLaunch({ url: '/pages/login/login' })
          })
          reject(new Error(body.message))
          return
        }
        if (body.code === 401) {
          app().globalData.token = ''
          app().globalData.user = null
          wx.removeStorageSync('cb_token')
          wx.removeStorageSync('cb_user')
        }
        wx.showToast({ title: body.message || '请求失败', icon: 'none' })
        reject(new Error(body.message || '请求失败'))
      },
      fail(err) {
        wx.showToast({ title: '网络异常，请确认后端已启动', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = { request, cleanParams }