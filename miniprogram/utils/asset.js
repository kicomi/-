// utils/asset.js
/**
 * 后端返回的图片是相对路径（/uploads/202610/xxx.png），
 * 小程序没有开发服务器代理，直接用相对路径是加载不出来的，
 * 必须拼上后端地址。这里统一处理，顺便兼容将来改成 CDN 的绝对地址。
 */
function assetUrl(path) {
  if (!path) return ''
  if (path.indexOf('http://') === 0 || path.indexOf('https://') === 0) return path
  const base = getApp().globalData.baseUrl || ''
  return base + (path.charAt(0) === '/' ? path : '/' + path)
}

module.exports = { assetUrl }
