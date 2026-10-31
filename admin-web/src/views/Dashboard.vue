<template>
  <div class="screen" v-loading="loading" element-loading-background="rgba(7,13,24,0.6)">
    <div class="screen-head">
      <div class="title-wrap">
        <span class="tag-live"><i></i>LIVE</span>
        <h2>校园二手书 · 运营数据大屏</h2>
        <span class="sub">供需匹配与智能定价系统</span>
      </div>
      <div class="head-meta">
        <span class="chip">数据源 MySQL 8.0</span>
        <span class="chip">模型 ItemCF + RF</span>
        <span class="chip accent">{{ updatedAt }}</span>
      </div>
    </div>

    <section class="kpi-row">
      <article class="kpi" v-for="k in kpis" :key="k.label" :style="{ '--accent': k.color }">
        <div class="kpi-label">{{ k.label }}</div>
        <div class="kpi-value">
          {{ k.value }}<span class="unit">{{ k.unit }}</span>
        </div>
        <div class="kpi-foot">{{ k.foot }}</div>
        <div class="kpi-bar"><i :style="{ width: k.percent + '%' }"></i></div>
      </article>
    </section>

    <section class="grid grid-a">
      <div class="panel-dark">
        <div class="panel-head">
          <span>图书分类分布</span>
          <em>
            {{ categoryTotal }} 本
            <a v-if="anyHidden" class="reset-link" @click="resetCategories">· 恢复全部</a>
          </em>
        </div>
        <e-chart ref="pieRef" :option="categoryOption" height="264px"
                 slice-toggle @legend-change="onLegendChange" />
        <div class="chart-hint">点击扇区可临时剔除该类，点图例恢复</div>
      </div>

      <div class="panel-dark">
        <div class="panel-head">
          <span>近 14 天新增趋势</span>
          <em>书籍 / 订单</em>
        </div>
        <e-chart :option="trendOption" height="286px" />
      </div>

      <div class="panel-dark">
        <div class="panel-head">
          <span>交易健康度</span>
          <em>实时</em>
        </div>
        <e-chart :option="gaugeOption" height="210px" />
        <div class="gauge-foot">
          <div>
            <b>{{ overview.pendingCount || 0 }}</b>
            <span>待审核</span>
          </div>
          <div>
            <b>{{ overview.completedCount || 0 }}</b>
            <span>已完成单</span>
          </div>
          <div>
            <b>¥{{ overview.avgPrice || 0 }}</b>
            <span>在售均价</span>
          </div>
        </div>
      </div>
    </section>

    <section class="grid grid-b">
      <div class="panel-dark">
        <div class="panel-head">
          <span>在售价位区间</span>
          <em>单位：本</em>
        </div>
        <e-chart :option="priceOption" height="236px" />
      </div>

      <div class="panel-dark">
        <div class="panel-head">
          <span>各专业在售书籍</span>
          <em>按数量排序</em>
        </div>
        <e-chart :option="majorOption" height="236px" />
      </div>

      <div class="panel-dark">
        <div class="panel-head">
          <span>关键指标</span>
          <em>累计</em>
        </div>
        <div class="metric-list">
          <div class="metric" v-for="m in metrics" :key="m.label">
            <div class="metric-top">
              <span>{{ m.label }}</span>
              <b>{{ m.value }}</b>
            </div>
            <div class="metric-track"><i :style="{ width: m.percent + '%', background: m.color }"></i></div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import EChart from '../components/EChart.vue'
import { getOverview, getCharts } from '../api'

const loading = ref(false)
const overview = ref({})
const charts = ref({})
const updatedAt = ref('')

// 分类图的临时剔除状态：由 ECharts 的 legendselectchanged 事件驱动
const pieRef = ref(null)
const catSelected = ref({})

function onLegendChange(selected) {
  catSelected.value = selected || {}
}

const anyHidden = computed(() => Object.values(catSelected.value).some((v) => v === false))

function resetCategories() {
  if (pieRef.value) pieRef.value.dispatch({ type: 'legendAllSelect' })
  // 程序化恢复不一定会回传事件，这里直接把本地状态清空，保证总数与图表一致
  catSelected.value = {}
}

const PALETTE = ['#22d3ee', '#a78bfa', '#34d399', '#fbbf24', '#f472b6', '#60a5fa', '#fb923c', '#4ade80']

const TOOLTIP = {
  backgroundColor: 'rgba(15,23,42,0.94)',
  borderColor: 'rgba(148,163,184,0.22)',
  borderWidth: 1,
  textStyle: { color: '#e2e8f0', fontSize: 12 },
  extraCssText: 'border-radius:8px;box-shadow:0 8px 24px rgba(0,0,0,.4);'
}

const AXIS_LABEL = { color: '#64748b', fontSize: 11 }

const kpis = computed(() => {
  const o = overview.value
  return [
    {
      label: '注册用户',
      value: o.userCount || 0,
      unit: ' 人',
      foot: '在售书籍来自 ' + (o.onSaleCount || 0) + ' 个卖家',
      color: '#22d3ee',
      percent: 100
    },
    {
      label: '书籍总数',
      value: o.bookCount || 0,
      unit: ' 本',
      foot: '在售 ' + (o.onSaleCount || 0) + ' · 已售 ' + (o.soldCount || 0),
      color: '#a78bfa',
      percent: Math.min(100, Math.round(((o.soldCount || 0) / Math.max(1, o.bookCount || 1)) * 100) * 3)
    },
    {
      label: '累计订单',
      value: o.orderCount || 0,
      unit: ' 单',
      foot: '已完成 ' + (o.completedCount || 0) + ' 单',
      color: '#34d399',
      percent: Number(o.conversionRate || 0)
    },
    {
      label: '成交总额',
      value: '¥' + (o.gmv || 0),
      unit: '',
      foot: '在售均价 ¥' + (o.avgPrice || 0),
      color: '#fbbf24',
      percent: Number(o.soldRate || 0) * 3
    }
  ]
})

const metrics = computed(() => {
  const o = overview.value
  return [
    { label: '书籍售出率', value: (o.soldRate || 0) + '%', percent: Math.min(100, o.soldRate || 0), color: 'linear-gradient(90deg,#22d3ee,#34d399)' },
    { label: '订单成交转化率', value: (o.conversionRate || 0) + '%', percent: Math.min(100, o.conversionRate || 0), color: 'linear-gradient(90deg,#a78bfa,#60a5fa)' },
    { label: '在售书籍占比', value: Math.round(((o.onSaleCount || 0) / Math.max(1, o.bookCount || 1)) * 100) + '%', percent: Math.round(((o.onSaleCount || 0) / Math.max(1, o.bookCount || 1)) * 100), color: 'linear-gradient(90deg,#fbbf24,#fb923c)' },
    { label: '待审核占比', value: Math.round(((o.pendingCount || 0) / Math.max(1, o.bookCount || 1)) * 100) + '%', percent: Math.round(((o.pendingCount || 0) / Math.max(1, o.bookCount || 1)) * 100), color: 'linear-gradient(90deg,#f472b6,#fb7185)' }
  ]
})

// 被剔除的分类不计入总数，这样"暂时从总里拿掉"的语义才成立
const categoryTotal = computed(() => {
  const list = charts.value.categoryDist || []
  const sel = catSelected.value
  return list.reduce((s, x) => {
    const name = x.name || '未分类'
    if (sel[name] === false) return s
    return s + Number(x.cnt || 0)
  }, 0)
})

const categoryOption = computed(() => {
  const list = charts.value.categoryDist || []
  return {
    color: PALETTE,
    tooltip: Object.assign({ trigger: 'item', formatter: '{b}<br/>{c} 本 ({d}%)' }, TOOLTIP),
    legend: {
      bottom: 4,
      icon: 'circle',
      itemWidth: 7,
      itemHeight: 7,
      itemGap: 10,
      textStyle: { color: '#94a3b8', fontSize: 10.5 }
    },
    series: [
      {
        type: 'pie',
        radius: ['48%', '72%'],
        center: ['50%', '43%'],
        itemStyle: { borderColor: '#0a1424', borderWidth: 3, borderRadius: 5 },
        label: { show: false },
        // 剔除某类后，强调标签会残留成"本 (0%)"的空值，这里只保留放大效果，
        // 具体数值交给 tooltip 展示
        emphasis: {
          scale: true,
          scaleSize: 5,
          label: { show: false }
        },
        data: list.map((x) => ({ name: x.name || '未分类', value: Number(x.cnt) }))
      }
    ]
  }
})

const trendOption = computed(() => {
  const books = charts.value.dailyBooks || []
  const orders = charts.value.dailyOrders || []
  const labels = Array.from(
    new Set([].concat(books.map((x) => x.name), orders.map((x) => x.name)))
  ).sort()
  const bm = {}
  books.forEach((x) => (bm[x.name] = Number(x.cnt)))
  const om = {}
  orders.forEach((x) => (om[x.name] = Number(x.cnt)))

  const area = (from, to) => ({
    type: 'linear',
    x: 0,
    y: 0,
    x2: 0,
    y2: 1,
    colorStops: [
      { offset: 0, color: from },
      { offset: 1, color: to }
    ]
  })

  return {
    tooltip: Object.assign({ trigger: 'axis', axisPointer: { type: 'line', lineStyle: { color: 'rgba(148,163,184,.3)' } } }, TOOLTIP),
    legend: {
      top: 0,
      right: 0,
      icon: 'roundRect',
      itemWidth: 10,
      itemHeight: 4,
      textStyle: { color: '#94a3b8', fontSize: 11 }
    },
    grid: { left: 36, right: 14, top: 38, bottom: 24 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: { lineStyle: { color: 'rgba(148,163,184,0.2)' } },
      axisTick: { show: false },
      axisLabel: AXIS_LABEL
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.1)' } },
      axisLabel: AXIS_LABEL
    },
    series: [
      {
        name: '新增书籍',
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 2, color: '#22d3ee' },
        areaStyle: { color: area('rgba(34,211,238,0.34)', 'rgba(34,211,238,0)') },
        data: labels.map((l) => bm[l] || 0)
      },
      {
        name: '新增订单',
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 2, color: '#a78bfa' },
        areaStyle: { color: area('rgba(167,139,250,0.28)', 'rgba(167,139,250,0)') },
        data: labels.map((l) => om[l] || 0)
      }
    ]
  }
})

const gaugeOption = computed(() => {
  const conv = Number(overview.value.conversionRate || 0)
  const sold = Number(overview.value.soldRate || 0)
  const base = {
    type: 'gauge',
    startAngle: 210,
    endAngle: -30,
    // 半径按面板高度算，两个环并排时圆心距离是面板宽的 48%，
    // 半径给到 62% 才不互相压住（原来 92% 直接叠在一起）
    radius: '62%',
    center: ['50%', '58%'],
    progress: { show: true, width: 9, roundCap: true },
    axisLine: { lineStyle: { width: 9, color: [[1, 'rgba(148,163,184,0.16)']] } },
    pointer: { show: false },
    axisTick: { show: false },
    splitLine: { show: false },
    axisLabel: { show: false },
    anchor: { show: false }
  }
  return {
    series: [
      Object.assign({}, base, {
        name: '成交转化',
        center: ['26%', '58%'],
        progress: { show: true, width: 9, roundCap: true, itemStyle: { color: '#22d3ee' } },
        detail: {
          valueAnimation: true,
          offsetCenter: [0, '2%'],
          fontSize: 20,
          fontWeight: 700,
          color: '#e2e8f0',
          formatter: '{value}%'
        },
        title: { offsetCenter: [0, '32%'], color: '#64748b', fontSize: 11 },
        data: [{ value: conv, name: '成交转化率' }]
      }),
      Object.assign({}, base, {
        name: '售出率',
        center: ['74%', '58%'],
        progress: { show: true, width: 9, roundCap: true, itemStyle: { color: '#a78bfa' } },
        detail: {
          valueAnimation: true,
          offsetCenter: [0, '2%'],
          fontSize: 20,
          fontWeight: 700,
          color: '#e2e8f0',
          formatter: '{value}%'
        },
        title: { offsetCenter: [0, '32%'], color: '#64748b', fontSize: 11 },
        data: [{ value: sold, name: '书籍售出率' }]
      })
    ]
  }
})

const priceOption = computed(() => {
  const list = charts.value.priceRange || []
  return {
    tooltip: Object.assign({ trigger: 'axis', axisPointer: { type: 'shadow' } }, TOOLTIP),
    grid: { left: 36, right: 14, top: 18, bottom: 26 },
    xAxis: {
      type: 'category',
      data: list.map((x) => x.name),
      axisLine: { lineStyle: { color: 'rgba(148,163,184,0.2)' } },
      axisTick: { show: false },
      axisLabel: AXIS_LABEL
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.1)' } },
      axisLabel: AXIS_LABEL
    },
    series: [
      {
        type: 'bar',
        barWidth: '42%',
        itemStyle: {
          borderRadius: [5, 5, 0, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#34d399' },
              { offset: 1, color: 'rgba(52,211,153,0.18)' }
            ]
          }
        },
        data: list.map((x) => Number(x.cnt))
      }
    ]
  }
})

const majorOption = computed(() => {
  const list = (charts.value.majorDist || []).slice(0, 5).reverse()
  return {
    tooltip: Object.assign({ trigger: 'axis', axisPointer: { type: 'shadow' } }, TOOLTIP),
    // 专业名最长有 9 个字（数据科学与大数据技术），左边距不够会被截断
    grid: { left: 118, right: 34, top: 12, bottom: 28 },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.1)' } },
      axisLabel: AXIS_LABEL
    },
    yAxis: {
      type: 'category',
      data: list.map((x) => x.name || '未填写'),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    series: [
      {
        type: 'bar',
        barWidth: '54%',
        label: { show: true, position: 'right', color: '#94a3b8', fontSize: 11 },
        itemStyle: {
          borderRadius: [0, 5, 5, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: 'rgba(96,165,250,0.25)' },
              { offset: 1, color: '#60a5fa' }
            ]
          }
        },
        data: list.map((x) => Number(x.cnt))
      }
    ]
  }
})

onMounted(async () => {
  loading.value = true
  try {
    const [ov, ch] = await Promise.all([getOverview(), getCharts()])
    overview.value = ov || {}
    charts.value = ch || {}
    updatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.screen {
  position: relative;
  min-height: 100%;
  padding: 20px 24px 24px;
  background:
    radial-gradient(820px 460px at 12% -8%, rgba(34, 211, 238, 0.13), transparent 62%),
    radial-gradient(820px 460px at 88% -6%, rgba(167, 139, 250, 0.13), transparent 62%),
    linear-gradient(180deg, #070d18 0%, #070d18 100%);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.screen::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.055) 1px, transparent 1px);
  background-size: 44px 44px;
  -webkit-mask-image: radial-gradient(circle at 50% -10%, #000 0%, transparent 72%);
  mask-image: radial-gradient(circle at 50% -10%, #000 0%, transparent 72%);
  pointer-events: none;
}

.screen-head {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 4px;
}

.title-wrap {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.title-wrap h2 {
  margin: 0;
  font-size: 19px;
  font-weight: 650;
  letter-spacing: 1px;
  background: linear-gradient(90deg, #e2e8f0, #6ee7b7 60%, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.sub {
  font-size: 11.5px;
  color: #475569;
  letter-spacing: 0.5px;
}

.tag-live {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  letter-spacing: 1.4px;
  color: #34d399;
  border: 1px solid rgba(52, 211, 153, 0.32);
  background: rgba(52, 211, 153, 0.08);
  padding: 2px 8px;
  border-radius: 20px;
}

.tag-live i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #34d399;
  animation: blink 1.6s infinite;
}

.reset-link {
  color: #22d3ee;
  cursor: pointer;
  margin-left: 4px;
}

.reset-link:hover {
  text-decoration: underline;
}

.chart-hint {
  text-align: center;
  font-size: 10.5px;
  color: #475569;
  padding: 2px 0 4px;
  letter-spacing: 0.3px;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.25; }
}

.head-meta {
  display: flex;
  gap: 8px;
}

.chip {
  font-size: 11px;
  color: #64748b;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(148, 163, 184, 0.06);
  padding: 3px 10px;
  border-radius: 20px;
}

.chip.accent {
  color: #22d3ee;
  border-color: rgba(34, 211, 238, 0.28);
  background: rgba(34, 211, 238, 0.08);
}

/* ---------------- KPI ---------------- */

.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.kpi {
  position: relative;
  overflow: hidden;
  padding: 16px 18px 14px;
  border-radius: 14px;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.025));
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.kpi::after {
  content: '';
  position: absolute;
  top: -40px;
  right: -30px;
  width: 130px;
  height: 130px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--accent), transparent 68%);
  opacity: 0.16;
  pointer-events: none;
}

.kpi-label {
  font-size: 12px;
  color: #94a3b8;
  letter-spacing: 0.6px;
}

.kpi-value {
  margin: 8px 0 4px;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  color: #f1f5f9;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.5px;
}

.kpi-value .unit {
  font-size: 12px;
  font-weight: 400;
  color: #64748b;
  margin-left: 4px;
}

.kpi-foot {
  font-size: 11px;
  color: #64748b;
}

.kpi-bar {
  margin-top: 12px;
  height: 3px;
  border-radius: 3px;
  background: rgba(148, 163, 184, 0.14);
  overflow: hidden;
}

.kpi-bar i {
  display: block;
  height: 100%;
  border-radius: 3px;
  background: var(--accent);
  box-shadow: 0 0 10px var(--accent);
}

/* ---------------- 图表面板 ---------------- */

.grid {
  display: grid;
  gap: 14px;
}

.grid-a {
  grid-template-columns: 1.02fr 1.5fr 1.02fr;
}

.grid-b {
  grid-template-columns: 1fr 1fr 1fr;
}

.panel-dark {
  position: relative;
  border-radius: 14px;
  padding: 14px 16px 10px;
  background: linear-gradient(170deg, rgba(255, 255, 255, 0.055), rgba(255, 255, 255, 0.02));
  border: 1px solid rgba(148, 163, 184, 0.13);
  backdrop-filter: blur(6px);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.panel-head span {
  font-size: 13px;
  font-weight: 600;
  color: #cbd5e1;
  letter-spacing: 0.4px;
}

.panel-head span::before {
  content: '';
  display: inline-block;
  width: 3px;
  height: 12px;
  margin-right: 8px;
  border-radius: 2px;
  vertical-align: -1px;
  background: linear-gradient(180deg, #22d3ee, #34d399);
}

.panel-head em {
  font-style: normal;
  font-size: 10.5px;
  color: #64748b;
  letter-spacing: 0.5px;
}

.gauge-foot {
  display: flex;
  justify-content: space-around;
  padding: 2px 0 6px;
  border-top: 1px solid rgba(148, 163, 184, 0.1);
  margin-top: 2px;
}

.gauge-foot div {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.gauge-foot b {
  font-size: 15px;
  color: #e2e8f0;
  font-variant-numeric: tabular-nums;
}

.gauge-foot span {
  font-size: 10.5px;
  color: #64748b;
}

/* ---------------- 指标列表 ---------------- */

.metric-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 10px 4px 6px;
}

.metric-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 7px;
}

.metric-top span {
  font-size: 12px;
  color: #94a3b8;
}

.metric-top b {
  font-size: 15px;
  color: #e2e8f0;
  font-variant-numeric: tabular-nums;
}

.metric-track {
  height: 6px;
  border-radius: 6px;
  background: rgba(148, 163, 184, 0.12);
  overflow: hidden;
}

.metric-track i {
  display: block;
  height: 100%;
  border-radius: 6px;
  transition: width 0.6s ease;
}

@media (max-width: 1500px) {
  .kpi-value {
    font-size: 24px;
  }
}
</style>
