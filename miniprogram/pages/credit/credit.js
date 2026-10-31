// pages/credit/credit.js
// 信用分说明页：让用户知道现在多少分、怎么涨、怎么会掉
const api = require('../../utils/api')

// 涨分项。数值和后端保持一致，改规则时两边要一起改
const GAIN = [
  { title: '完成一笔交易（作为卖家）', delta: '+2', desc: '按时交付、书况与描述一致' },
  { title: '完成一笔交易（作为买家）', delta: '+1', desc: '如约取书，不无故失联' },
  { title: '收到一条好评', delta: '+1', desc: '评价 4 星及以上才算' },
  { title: '资料填写完整', delta: '+2', desc: '头像、专业、签名都填上（一次性）' }
]

// 违规扣分按举报原因分级，和后端 ReportService 里的映射一一对应
const LOSS = [
  { title: '涉嫌诈骗', delta: '-20', desc: '收款后不发货、货不对板' },
  { title: '内容违规', delta: '-15', desc: '盗版、涉黄涉政等' },
  { title: '疑似虚假信息', delta: '-10', desc: '冒用书号、图文不符' },
  { title: '交易纠纷', delta: '-8', desc: '约好不来、临时加价' },
  { title: '书况与描述不符', delta: '-5', desc: '品相、笔记情况和描述差太多' },
  { title: '价格异常', delta: '-3', desc: '明显偏离市场价' }
]

// 等级特权：分数不只是数字，直接换权益
const PERKS = [
  { level: '青铜 70', items: ['正常发布与交易', '参与评价与举报'] },
  { level: '白银 85', items: ['搜索排队略微靠前', '专属白银标识'] },
  { level: '黄金 95', items: ['发布免审核，立即上架', '首页优先展示', '专属黄金标识'] }
]

function creditLevel(score) {
  const s = score == null ? 80 : score
  if (s >= 95) return { name: '黄金信用', next: '已满级，发布免审核、首页优先', percent: 100, color: '#c8901f' }
  if (s >= 85) return { name: '白银信用', next: '再得 ' + (95 - s) + ' 分升黄金', percent: Math.round((s - 60) / 35 * 100), color: '#8a9bb0' }
  if (s >= 70) return { name: '青铜信用', next: '再得 ' + (85 - s) + ' 分升白银', percent: Math.round((s - 40) / 45 * 100), color: '#b0805a' }
  return { name: '待提升', next: '完成交易可提升信用', percent: Math.max(5, s), color: '#c0563f' }
}

Page({
  data: {
    score: 80,
    credit: { name: '', next: '', percent: 0, color: '#2f7d5b' },
    gain: GAIN,
    loss: LOSS,
    loading: true
  },

  onShow() {
    this.load()
  },

  async load() {
    try {
      const u = await api.getMe()
      const score = u.creditScore == null ? 80 : u.creditScore
      this.setData({ score, credit: creditLevel(score), loading: false })
    } catch (e) {
      this.setData({ loading: false })
    }
  }
})