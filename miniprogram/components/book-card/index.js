// 书籍卡片组件，首页/检索/我的页面共用
const { assetUrl } = require('../../utils/asset')

const CONDITION = { 1: '四成新', 2: '五成新', 3: '七成新', 4: '九成新', 5: '全新' }

Component({
  properties: {
    book: {
      type: Object,
      value: {},
      observer(val) {
        if (!val) return
        // 折扣角标：直接标出省了多少，比"原价划线"更直观
        const orig = Number(val.originalPrice) || 0
        const price = Number(val.price) || 0
        const off = orig > 0 && price > 0 && price < orig
          ? Math.round((1 - price / orig) * 100)
          : 0

        // 课程 / 品相 / 笔记 合并成一行文字
        const bits = []
        if (val.courseName) bits.push(val.courseName)
        if (val.conditionLevel) bits.push(CONDITION[val.conditionLevel] || '')
        if (val.noteFlag === 1) bits.push('含笔记')
        const metaLine = bits.filter(Boolean).join(' · ')

        this.setData({
          metaLine,
          off,
          conditionText: CONDITION[val.conditionLevel] || '七成新',
          // 封面是相对路径，必须拼成完整地址才能加载
          coverSrc: assetUrl(val.coverImg),
          initial: (val.title || '书').slice(0, 1)
        })
      }
    }
  },

  data: {
    off: 0,
    imgLoaded: false,
    conditionText: '七成新',
    coverSrc: '',
    initial: '书'
  },

  methods: {
    // 图片加载完再淡入，避免慢网下先闪一块空白
    onImgLoad() {
      this.setData({ imgLoaded: true })
    },

    onTap() {
      this.triggerEvent('tap', { book: this.data.book })
    }
  }
})