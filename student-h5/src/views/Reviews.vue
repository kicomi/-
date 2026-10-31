<template>
  <div class="reviews">
    <header class="nav">
      <button class="back" @click="$router.back()">‹</button>
      <span class="title">{{ title ? title + ' · 评价' : '全部评价' }}</span>
      <div style="width: 32px"></div>
    </header>

    <div class="page">
      <div class="summary" v-if="summary">
        <div class="sum-left">
          <b>{{ summary.avg || '—' }}</b>
          <small>{{ summary.total }} 条评价</small>
        </div>
        <div class="sum-bars">
          <div class="bar-row" v-for="b in bars" :key="b.star">
            <span>{{ b.star }} 星</span>
            <div class="track"><i :style="{ width: b.percent + '%' }"></i></div>
            <em>{{ b.count }}</em>
          </div>
        </div>
      </div>

      <!-- 骨架屏 -->
      <div class="sk" v-for="i in 3" :key="'sk' + i" v-if="skeleton">
        <div class="sk-line w30"></div>
        <div class="sk-line w90"></div>
        <div class="sk-line w60"></div>
      </div>

      <template v-if="!skeleton">
        <div class="item" v-for="r in rows" :key="r.id">
          <div class="item-head">
            <div class="avatar">{{ (r.fromNickname || '同').slice(0, 1) }}</div>
            <div class="head-main">
              <span class="nick">{{ r.fromNickname || '同学' }}</span>
              <div class="stars">
                <em v-for="n in 5" :key="n" :class="{ on: n <= r.rating }">★</em>
              </div>
            </div>
            <small class="time">{{ (r.createdAt || '').slice(0, 10) }}</small>
          </div>

          <p class="content">{{ r.content || '买家没有填写评价内容' }}</p>

          <div class="imgs" v-if="imgsOf(r).length">
            <img v-for="(img, i) in imgsOf(r)" :key="i" :src="img" @click="zoom(img)" />
          </div>
        </div>

        <button class="more-btn" v-if="hasMore && rows.length" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
        <p class="more-tip" v-if="!hasMore && rows.length">没有更多了</p>

        <div class="empty" v-if="!rows.length">
          <div class="empty-art">
            <div class="art-book"></div>
            <div class="art-line"></div>
            <div class="art-line short"></div>
          </div>
          <b>还没有人评价</b>
          <small>买下这本书并完成交易后，可以留下你的评价</small>
        </div>
      </template>
    </div>

    <!-- 图片放大 -->
    <div class="zoom" v-if="zoomSrc" @click="zoomSrc = ''">
      <img :src="zoomSrc" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { reviewsPage, reviewsSummary } from '../api'

const route = useRoute()
const bookId = Number(route.params.id)
const title = route.query.title || ''

const summary = ref(null)
const rows = ref([])
const page = ref(1)
const hasMore = ref(true)
const skeleton = ref(true)
const loadingMore = ref(false)
const zoomSrc = ref('')

const bars = computed(() => {
  const s = summary.value
  if (!s || !s.dist) return []
  const total = s.total || 0
  return s.dist.map((n, i) => ({
    star: i + 1,
    count: n,
    percent: total ? Math.round((n * 100) / total) : 0
  })).reverse()
})

function imgsOf(r) {
  try {
    const arr = JSON.parse(r.images || '[]')
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

function zoom(src) {
  zoomSrc.value = src
}

async function load(p) {
  if (p === 1) skeleton.value = true
  else loadingMore.value = true
  try {
    const data = await reviewsPage(bookId, p, 10)
    const list = data.list || []
    rows.value = p === 1 ? list : rows.value.concat(list)
    page.value = p
    hasMore.value = list.length >= 10
  } catch (e) {
    if (p === 1) rows.value = []
    hasMore.value = false
  } finally {
    skeleton.value = false
    loadingMore.value = false
  }
}

function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  load(page.value + 1)
}

onMounted(async () => {
  try { summary.value = await reviewsSummary(bookId) } catch (e) { /* ignore */ }
  load(1)
})
</script>

<style scoped>
.reviews { min-height: 100vh; background: #f7f4ef; }
.nav {
  height: 48px; display: flex; align-items: center; justify-content: space-between;
  padding: 0 12px; font-size: 14.5px; font-weight: 600;
  background: #f7f4ef; border-bottom: 1px solid rgba(0, 0, 0, .04);
}
.back { border: none; background: none; font-size: 24px; width: 32px; text-align: left; line-height: 1; }
.title { max-width: 72%; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }

.summary { display: flex; align-items: center; background: #fff; border-radius: 14px; padding: 14px 13px; margin-bottom: 10px; }
.sum-left {
  width: 96px; display: flex; flex-direction: column; align-items: center;
  border-right: 1px solid #f0ebe3;
}
.sum-left b { font-size: 30px; color: var(--brand); line-height: 1; }
.sum-left small { font-size: 10.5px; color: var(--ink-3); margin-top: 5px; }
.sum-bars { flex: 1; padding-left: 13px; }
.bar-row { display: flex; align-items: center; gap: 7px; margin-bottom: 4px; }
.bar-row span { font-size: 10px; color: #b9b4ab; width: 26px; }
.track { flex: 1; height: 5px; border-radius: 3px; background: #f0ebe3; overflow: hidden; }
.track i { display: block; height: 100%; border-radius: 3px; background: #e2a33c; }
.bar-row em { font-style: normal; font-size: 10px; color: #b9b4ab; width: 18px; text-align: right; }

.item { background: #fff; border-radius: 14px; padding: 13px; margin-bottom: 9px; }
.item-head { display: flex; align-items: center; gap: 9px; margin-bottom: 7px; }
.avatar {
  width: 32px; height: 32px; border-radius: 50%; background: var(--brand-soft);
  color: var(--brand); display: grid; place-items: center; font-size: 13px; flex: none;
}
.head-main { flex: 1; }
.nick { font-size: 12.5px; color: #3c444d; }
.stars { display: flex; gap: 1px; margin-top: 2px; }
.stars em { font-style: normal; font-size: 11px; color: #e6e1d8; }
.stars em.on { color: #e2a33c; }
.time { font-size: 10px; color: #c4bfb7; }
.content { margin: 0; font-size: 13.5px; color: #3c444d; line-height: 1.65; }
.imgs { display: flex; gap: 6px; margin-top: 7px; flex-wrap: wrap; }
.imgs img { width: 72px; height: 72px; border-radius: 7px; object-fit: cover; cursor: zoom-in; background: #eee9e1; }

.more-btn {
  display: block; width: 100%; height: 40px; margin: 4px 0;
  border-radius: 20px; border: 1px solid var(--line); background: #fff;
  color: var(--brand); font-size: 12.5px;
}
.more-tip { text-align: center; font-size: 10.5px; color: #b9b4ab; padding: 8px 0; }

/* 骨架屏 */
@keyframes rv-shine { 0% { opacity: .55 } 50% { opacity: 1 } 100% { opacity: .55 } }
.sk { background: #fff; border-radius: 14px; padding: 13px; margin-bottom: 9px; }
.sk-line { height: 11px; border-radius: 4px; background: #eef1f0; margin-bottom: 7px; animation: rv-shine 1.4s ease-in-out infinite; }
.sk-line.w90 { width: 90%; }
.sk-line.w60 { width: 60%; }
.sk-line.w30 { width: 30%; }

/* 空状态插图 */
.empty { display: flex; flex-direction: column; align-items: center; padding: 50px 20px; }
.empty-art { position: relative; width: 80px; height: 80px; margin-bottom: 15px; }
.art-book {
  position: absolute; left: 17px; top: 10px; width: 46px; height: 60px;
  border: 2px solid #d9d3c8; border-radius: 5px; background: #fff;
}
.art-book::after {
  content: ''; position: absolute; left: 8px; top: 10px; width: 20px; height: 2px;
  background: #e6e1d8; box-shadow: 0 8px 0 #e6e1d8, 0 16px 0 #e6e1d8;
}
.art-line { position: absolute; left: 10px; bottom: 3px; width: 60px; height: 3px; border-radius: 2px; background: #e6e1d8; }
.art-line.short { width: 35px; left: 22px; bottom: -7px; }
.empty b { font-size: 13.5px; color: #8f9a94; }
.empty small { font-size: 11px; color: #b9b4ab; margin-top: 5px; text-align: center; line-height: 1.6; }

.zoom {
  position: fixed; inset: 0; z-index: 60; background: rgba(0, 0, 0, .92);
  display: grid; place-items: center; cursor: zoom-out;
}
.zoom img { max-width: 96vw; max-height: 92vh; object-fit: contain; }
</style>
