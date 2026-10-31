<template>
  <div ref="chartEl" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, default: () => ({}) },
  height: { type: String, default: '320px' },
  // 打开后，点击扇区 / 柱子会临时把该数据项从图例中隐藏
  sliceToggle: { type: Boolean, default: false }
})

const emit = defineEmits(['legend-change'])

const chartEl = ref(null)
let chart = null

function syncLegend(e) {
  emit('legend-change', e.selected || {})
}

function bindEvents() {
  // 用户点图例触发 legendselectchanged；
  // 代码里 dispatchAction 触发的则是 legendselected，两个都要接，否则状态会不同步
  chart.on('legendselectchanged', syncLegend)
  chart.on('legendselected', syncLegend)
  if (props.sliceToggle) {
    chart.on('click', (params) => {
      if (params.componentType !== 'series' || !params.name) return
      // 借助图例的选中状态实现"点一下剔除、点图例恢复"
      chart.dispatchAction({ type: 'legendToggleSelect', name: params.name })
    })
  }
}

function render() {
  if (!chartEl.value) return
  if (!chart) {
    chart = echarts.init(chartEl.value)
    bindEvents()
  }
  if (props.option) chart.setOption(props.option, true)
}

function resize() {
  if (chart) chart.resize()
}

onMounted(() => {
  render()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})

watch(() => props.option, () => nextTick(render), { deep: true })

// 父组件可以通过 reset 之类的动作控制图表，比如恢复全部数据
defineExpose({
  dispatch: (action) => {
    if (chart) chart.dispatchAction(action)
  }
})
</script>
