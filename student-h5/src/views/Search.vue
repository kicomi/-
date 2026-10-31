<template>
  <div class="search-page">
    <header class="head">
      <div class="bar">
        <div class="input-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" />
          </svg>
          <input v-model="query.keyword" placeholder="书名 / 作者 / ISBN" @keyup.enter="reload" />
        </div>
        <button class="go" @click="reload">搜索</button>
      </div>

      <div class="filters">
        <div class="chips">
          <span class="chip" :class="{ on: !query.sort || query.sort === 'newest' }" @click="setSort('newest')">最新</span>
          <span class="chip" :class="{ on: query.sort === 'hot' }" @click="setSort('hot')">热门</span>
          <span class="chip" :class="{ on: query.sort === 'priceAsc' }" @click="setSort('priceAsc')">价格低</span>
          <span class="chip" :class="{ on: query.sort === 'priceDesc' }" @click="setSort('priceDesc')">价格高</span>
          <span class="chip" :class="{ on: query.conditionMin === 4 }" @click="toggleCondition">九成新以上</span>
          <span class="chip" :class="{ on: query.priceMax === 20 }" @click="toggleCheap">20 元内</span>
        </div>
      </div>
    </header>

    <!-- 没输入关键词时展示历史与热词 -->
    <div class="suggest" v-if="!query.keyword">
      <template v-if="history.length">
        <div class="sug-head">
          <b>最近搜索</b>
          <span class="clear" @click="clearHistory">清空</span>
        </div>
        <div class="his-row">
          <span class="his-chip" v-for="h in history" :key="h" @click="pickWord(h)">{{ h }}</span>
        </div>
      </template>

      <template v-if="hot.length">
        <div class="sug-head">
          <b>大家都在找</b>
          <small>按在售书籍的热度</small>
        </div>
        <div class="hot-list">
          <div class="hot-row" v-for="(w, i) in hot" :key="w.keyword" @click="pickWord(w.keyword)">
            <em class="rank" :class="{ top: i < 3 }">{{ i + 1 }}</em>
            <span class="hot-name">{{ w.keyword }}</span>
            <small>{{ w.count }} 本</small>
          </div>
        </div>
      </template>
    </div>

    <div class="page">
      <div class="result-head">
        <span>共 {{ total }} 本</span>
        <span v-if="userStore.isLogin" class="hint">已按匹配度加权</span>
      </div>

      <div v-if="loading" class="loading">搜索中…</div>
      <template v-else>
        <book-card v-for="b in rows" :key="b.id" :book="b" @click="openBook(b)" />
        <div v-if="!rows.length && !loading" class="no-result">
          <b>没有找到「{{ query.keyword }}」相关的书</b>
          <small>换个词试试，或者看看大家都在找什么</small>
          <div class="nr-chips">
            <span v-for="w in hot" :key="w.keyword" @click="pickWord(w.keyword)">{{ w.keyword }}</span>
          </div>
        </div>

        <div v-if="false" class="empty">
          <div class="icon">🔍</div>
          没有找到相关的书，换个关键词试试
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BookCard from '../components/BookCard.vue'
import { searchBooks, publicBooks, hotKeywords } from '../api'
import { userStore } from '../store/user'

const route = useRoute()
const router = useRouter()

const rows = ref([])
const total = ref(0)
const loading = ref(false)

const history = ref([])
const hot = ref([])

function loadHistory() {
  try {
    history.value = JSON.parse(localStorage.getItem('cb_search_history') || '[]')
  } catch (e) { history.value = [] }
}

// 搜索历史最多留 8 条，重复的提到最前
function saveHistory(word) {
  if (!word) return
  const list = history.value.filter((w) => w !== word)
  list.unshift(word)
  history.value = list.slice(0, 8)
  localStorage.setItem('cb_search_history', JSON.stringify(history.value))
}

function clearHistory() {
  history.value = []
  localStorage.removeItem('cb_search_history')
}

function pickWord(word) {
  query.keyword = word
  saveHistory(word)
  reload()
}

const query = reactive({
  keyword: '',
  sort: 'newest',
  conditionMin: null,
  priceMax: null,
  categoryId: null,
  page: 1,
  size: 20
})

function setSort(s) {
  query.sort = s
  reload()
}

function toggleCondition() {
  query.conditionMin = query.conditionMin === 4 ? null : 4
  reload()
}

function toggleCheap() {
  query.priceMax = query.priceMax === 20 ? null : 20
  reload()
}

function openBook(b) {
  router.push('/book/' + b.id)
}

async function load() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size,
      sort: query.sort,
      keyword: query.keyword || undefined,
      conditionMin: query.conditionMin || undefined,
      priceMax: query.priceMax || undefined,
      categoryId: query.categoryId || undefined
    }
    // 登录后用带匹配度的接口，未登录走公开接口
    const data = userStore.isLogin ? await searchBooks(params) : await publicBooks(params)
    rows.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function reload() {
  query.page = 1
  saveHistory((query.keyword || '').trim())
  load()
}

watch(() => route.query, (q) => {
  query.keyword = q.keyword || ''
  query.categoryId = q.categoryId ? Number(q.categoryId) : null
  query.sort = q.sort || 'newest'
  reload()
}, { immediate: true })

onMounted(async () => {
  loadHistory()
  try { hot.value = (await hotKeywords()) || [] } catch (e) { hot.value = [] }
  if (!route.query.keyword && !route.query.categoryId) reload()
})
</script>

<style scoped>
.head {
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--bg);
  padding: 12px 16px 8px;
  border-bottom: 1px solid transparent;
}

.bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 12px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 19px;
}

.input-wrap svg {
  width: 16px;
  height: 16px;
  color: var(--ink-3);
  flex: none;
}

.input-wrap input {
  border: none;
  padding: 0;
  background: transparent;
  font-size: 13.5px;
}

.go {
  border: none;
  background: none;
  color: var(--brand);
  font-size: 13.5px;
  font-weight: 600;
  padding: 0 2px;
}

.filters {
  margin-top: 10px;
}

.chips {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: none;
}

.chips::-webkit-scrollbar {
  display: none;
}

.chip {
  flex: none;
  border: 1px solid var(--line);
  background: #fff;
  color: var(--ink-2);
  cursor: pointer;
}

.chip.on {
  background: var(--brand-soft);
  border-color: var(--brand);
  color: var(--brand);
}

.result-head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--ink-3);
  padding: 12px 2px 8px;
}

.hint {
  color: var(--brand);
}
</style>