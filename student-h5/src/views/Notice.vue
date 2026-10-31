<template>
  <div class="notice">
    <header class="nav">
      <button class="back" @click="$router.back()">‹</button>
      <span>消息通知</span>
      <button class="readall" :class="{ off: unread === 0 }" @click="readAll">全部已读</button>
    </header>

    <div class="page">
      <p class="head" v-if="rows.length">{{ unread > 0 ? unread + ' 条未读' : '全部已读' }}</p>

      <div class="sk-card" v-for="i in 4" :key="'sk' + i" v-if="loading">
        <div class="sk-line w40"></div>
        <div class="sk-line w90"></div>
      </div>

      <template v-if="!loading">
        <div class="item" :class="{ read: r.readFlag === 1 }" v-for="r in rows" :key="r.id" @click="open(r)">
          <span class="dot" v-if="r.readFlag === 0"></span>
          <div class="item-top">
            <em class="tag" :style="{ color: r.typeColor, background: r.typeBg }">{{ r.typeLabel }}</em>
            <b>{{ r.title }}</b>
            <small>{{ r.timeText }}</small>
          </div>
          <p>{{ r.content }}</p>
        </div>

        <button v-if="hasMore && rows.length" class="more-btn" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
        <p v-if="!hasMore && rows.length" class="more-tip">没有更多了</p>

        <div v-if="!rows.length" class="empty">
          <div class="icon">🔔</div>
          还没有消息，订单状态变化、举报处理结果都会通知你
        </div>
      </template>
    </div>

    <div class="mask" v-if="detail" @click="detail = null"></div>
    <div class="sheet" v-if="detail">
      <div class="sheet-head">
        <em class="tag" :style="{ color: detail.typeColor, background: detail.typeBg }">{{ detail.typeLabel }}</em>
        <small>{{ detail.timeText }}</small>
      </div>
      <h3>{{ detail.title }}</h3>
      <p class="sheet-body">{{ detail.content }}</p>
      <div class="sheet-actions">
        <button class="plain" @click="detail = null">知道了</button>
        <button class="main" v-if="detail.linkType" @click="gotoLink">
          去看看{{ detail.linkType === 'order' ? '订单' : '这本书' }} ›
        </button>
        <button class="main" v-else-if="detail.type === 'system'" @click="detail = null; $router.push('/me')">
          去看我的资料 ›
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { notifications, readNotice, readAllNotice } from '../api'

const router = useRouter()
const rows = ref([])
const unread = ref(0)
const loading = ref(true)
const detail = ref(null)
const page = ref(1)
const hasMore = ref(true)
const loadingMore = ref(false)

const TYPE = {
  order: { label: '订单', color: '#2f7d5b', bg: '#eef7f2' },
  report: { label: '举报', color: '#c0563f', bg: '#fbeeeb' },
  book: { label: '书籍', color: '#5b7fa6', bg: '#eef2f8' },
  system: { label: '系统', color: '#8a7a5c', bg: '#f6f2e9' }
}

function fmt(t) {
  if (!t) return ''
  const s = String(t).replace('T', ' ')
  const d = new Date(s.replace(/-/g, '/'))
  const diff = (Date.now() - d.getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  return s.slice(5, 16)
}

async function load() {
  loading.value = true
  try {
    const data = await notifications(1)
    const list = (data.list || []).map((n) => {
      const t = TYPE[n.type] || TYPE.system
      return { ...n, typeLabel: t.label, typeColor: t.color, typeBg: t.bg, timeText: fmt(n.createdAt) }
    })
    rows.value = list
    unread.value = list.filter((r) => r.readFlag === 0).length
    hasMore.value = list.length >= 30
    page.value = 1
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

// 点击先展开详情，标记已读但不跳走
async function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  loadingMore.value = true
  try {
    const next = page.value + 1
    const data = await notifications(next)
    const more = (data.list || []).map((n) => {
      const t = TYPE[n.type] || TYPE.system
      return { ...n, typeLabel: t.label, typeColor: t.color, typeBg: t.bg, timeText: fmt(n.createdAt) }
    })
    rows.value = rows.value.concat(more)
    page.value = next
    hasMore.value = more.length >= 30
  } catch (e) {
    hasMore.value = false
  } finally {
    loadingMore.value = false
  }
}

async function open(r) {
  if (r.readFlag === 0) {
    readNotice(r.id).catch(() => {})
    r.readFlag = 1
    unread.value = Math.max(0, unread.value - 1)
  }
  detail.value = r
}

function gotoLink() {
  const d = detail.value
  if (!d) return
  detail.value = null
  if (d.linkType === 'order') router.push('/orders')
  else if (d.linkType === 'book' && d.linkId) router.push('/book/' + d.linkId)
}

async function readAll() {
  if (unread.value === 0) return
  try {
    await readAllNotice()
    rows.value.forEach((r) => { r.readFlag = 1 })
    unread.value = 0
  } catch (e) { /* ignore */ }
}

onMounted(load)
</script>

<style scoped>
.notice { min-height: 100vh; background: #f7f4ef; }
.nav {
  height: 48px; display: flex; align-items: center; justify-content: space-between;
  padding: 0 12px; font-size: 15px; font-weight: 600;
  background: #f7f4ef; border-bottom: 1px solid rgba(0, 0, 0, .04);
}
.back { border: none; background: none; font-size: 24px; width: 32px; text-align: left; line-height: 1; }
.readall {
  border: none; background: none; font-size: 12.5px; color: var(--brand);
  width: auto; padding: 0; text-align: right;
}
.readall.off { color: #c4bfb7; }

.head { margin: 13px 0 8px; font-size: 12.5px; font-weight: 650; color: #3c444d; }

.item {
  position: relative; background: #fff; border-radius: 12px;
  padding: 12px 13px; margin-bottom: 8px; cursor: pointer;
}
.item.read { background: #fbfaf7; }
.dot {
  position: absolute; top: 15px; right: 12px;
  width: 7px; height: 7px; border-radius: 50%; background: var(--danger);
}
.item-top { display: flex; align-items: center; gap: 6px; margin-bottom: 5px; }
.tag {
  font-style: normal; font-size: 9.5px; border-radius: 3px; padding: 1px 5px; flex: none;
}
.item-top b { flex: 1; font-size: 13.5px; font-weight: 600; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.item-top small { font-size: 10px; color: #b9b4ab; flex: none; }
.item p { margin: 0; font-size: 12px; color: var(--ink-2); line-height: 1.6; padding-right: 10px; }
.item.read b { color: var(--ink-2); font-weight: 500; }
.item.read p { color: #8f9a94; }

.more-btn {
  display: block; width: 100%; height: 38px; margin: 6px 0 4px;
  border-radius: 19px; border: 1px solid var(--line);
  background: #fff; color: var(--brand); font-size: 12.5px;
}
.more-btn:disabled { color: var(--ink-3); }
.more-tip { text-align: center; font-size: 10.5px; color: #b9b4ab; padding: 8px 0 4px; margin: 0; }

@keyframes sk-shine2 {
  0% { opacity: .55; }
  50% { opacity: 1; }
  100% { opacity: .55; }
}
.sk-card { background: #fff; border-radius: 12px; padding: 12px 13px; margin-bottom: 8px; }
.sk-line { height: 11px; border-radius: 4px; background: #eef1f0; margin-bottom: 7px; animation: sk-shine2 1.4s ease-in-out infinite; }
.sk-line.w90 { width: 90%; }
.sk-line.w40 { width: 40%; }

.loading { text-align: center; padding: 40px 0; color: var(--ink-3); font-size: 12.5px; }
.mask { position: fixed; inset: 0; background: rgba(20,24,28,.42); z-index: 20; }
.sheet {
  position: fixed; left: 18px; right: 18px; top: 50%; transform: translateY(-50%);
  background: #fff; border-radius: 16px; padding: 20px 18px 16px; z-index: 21;
  max-height: 70vh; display: flex; flex-direction: column;
}
.sheet-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.sheet-head small { font-size: 10.5px; color: #b9b4ab; }
.sheet h3 { margin: 0 0 10px; font-size: 16px; font-weight: 650; line-height: 1.4; }
.sheet-body { margin: 0; font-size: 13.5px; color: #4a525c; line-height: 1.8; overflow-y: auto; }
.sheet-actions { display: flex; gap: 9px; margin-top: 18px; }
.sheet-actions button { height: 42px; border-radius: 21px; font-size: 13.5px; border: none; }
.sheet-actions .plain { flex: 1; background: #fff; border: 1px solid var(--line); color: var(--ink-2); }
.sheet-actions .main { flex: 1.4; background: var(--brand); color: #fff; font-weight: 600; }

.empty .icon { font-size: 30px; opacity: .5; margin-bottom: 8px; }
</style>