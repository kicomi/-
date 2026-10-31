<template>
  <nav class="tabbar">
    <div v-for="t in tabs" :key="t.path" class="tab" :class="{ active: isActive(t) }" @click="go(t)">
      <span class="ico" v-html="t.icon"></span>
      <span class="label">{{ t.label }}</span>
    </div>
  </nav>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 用内联 SVG，避免引外部图标库
const tabs = [
  {
    path: '/', label: '首页',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3 10.5 12 3l9 7.5"/><path d="M5 9.5V21h14V9.5"/></svg>'
  },
  {
    path: '/search', label: '找书',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>'
  },
  {
    path: '/publish', label: '发布',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 5v14M5 12h14"/></svg>'
  },
  {
    path: '/orders', label: '订单',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="4" y="4" width="16" height="16" rx="3"/><path d="M8 9h8M8 13h6M8 17h4"/></svg>'
  },
  {
    path: '/me', label: '我的',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="8.5" r="3.6"/><path d="M4.5 20c1.4-3.6 4-5.2 7.5-5.2s6.1 1.6 7.5 5.2"/></svg>'
  }
]

function isActive(t) {
  return route.path === t.path
}

function go(t) {
  if (route.path === t.path) return
  router.push(t.path)
}
</script>

<style scoped>
.tabbar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 430px;
  height: calc(56px + env(safe-area-inset-bottom, 0px));
  padding-bottom: env(safe-area-inset-bottom, 0px);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(12px);
  border-top: 1px solid var(--line);
  display: flex;
  z-index: 20;
}

.tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  color: var(--ink-3);
  cursor: pointer;
}

.tab.active {
  color: var(--brand);
}

.ico {
  width: 21px;
  height: 21px;
  display: block;
}

.ico :deep(svg) {
  width: 100%;
  height: 100%;
}

.label {
  font-size: 10.5px;
  letter-spacing: 0.3px;
}
</style>
