<template>
  <div class="home">
    <header class="hero">
      <div class="hero-top">
        <div class="brand">
          <h1>校园二手书</h1>
          <span>同校同专业 · 教材流转</span>
        </div>
        <div class="stat-pill">
          <b>{{ stats.onSale }}</b>
          <small>本在售</small>
        </div>
      </div>
      <div class="search" @click="go('/search')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" />
        </svg>
        <span>搜索教材、课程、ISBN</span>
        <em>搜索</em>
      </div>
    </header>

    <!-- 首屏骨架 -->
    <div class="page" v-if="skeleton">
      <div class="sk-banner"></div>
      <div class="sk-cats">
        <div class="sk-cat" v-for="i in 8" :key="i"></div>
      </div>
      <div class="sk-card" v-for="i in 3" :key="i">
        <div class="sk-cover"></div>
        <div class="sk-lines">
          <div class="sk-line w70"></div>
          <div class="sk-line w50"></div>
          <div class="sk-line w30"></div>
        </div>
      </div>
    </div>

    <div class="page" v-else>
      <div class="banners" v-if="banners.length">
        <div class="banner" :class="'b' + (i % 3)" v-for="(b, i) in banners" :key="b.id" @click="openBanner(b)">
          <b>{{ b.title }}</b>
          <small>{{ b.subtitle }}</small>
        </div>
      </div>

      <div class="cats">
        <div class="cat" v-for="c in categories" :key="c.id" @click="goCategory(c)">
          <div class="cat-dot">{{ c.name.slice(0, 1) }}</div>
          <span>{{ shortName(c) }}</span>
        </div>
      </div>

      <template v-if="sameMajor.length">
        <div class="floor"><i></i>同专业在售
          <span class="refresh" @click="refreshFloor('major')">换一批</span>
          <a @click="go('/search')">更多 ›</a>
        </div>
        <div class="hscroll">
          <div class="mini" v-for="b in sameMajor" :key="b.id" @click="openBook(b)">
            <div class="mini-cover">
              <img v-if="b.coverImg" :src="b.coverImg" />
              <span v-else>{{ b.title.slice(0, 1) }}</span>
            </div>
            <div class="mini-title">{{ b.title }}</div>
            <div class="mini-price">¥{{ b.price }}</div>
          </div>
        </div>
      </template>

      <template v-if="cheap.length">
        <div class="floor warm"><i></i>20 元以内
          <span class="refresh" @click="refreshFloor('cheap')">换一批</span>
          <a @click="go('/search')">更多 ›</a>
        </div>
        <div class="hscroll">
          <div class="mini" v-for="b in cheap" :key="b.id" @click="openBook(b)">
            <div class="mini-cover cheap">
              <img v-if="b.coverImg" :src="b.coverImg" />
              <span v-else>{{ b.title.slice(0, 1) }}</span>
            </div>
            <div class="mini-title">{{ b.title }}</div>
            <div class="mini-price">¥{{ b.price }}</div>
          </div>
        </div>
      </template>

      <template v-if="sold.length">
        <div class="floor blue"><i></i>刚刚成交
          <span class="refresh" @click="refreshFloor('sold')">换一批</span>
          <span class="sub">最近买走的书</span>
        </div>
        <div class="sold-list">
          <div class="sold-item" v-for="b in sold" :key="b.id" @click="openBook(b)">
            <span class="sold-title">{{ b.title }}</span>
            <span class="sold-price">¥{{ b.price }}</span>
            <span class="sold-flag">已出</span>
          </div>
        </div>
      </template>

      <div class="floor"><i></i>{{ userStore.isLogin ? '猜你想找' : '热门在售' }}<a @click="go('/search')">全部 ›</a></div>
      <div v-if="loadingRec" class="loading">正在挑选…</div>
      <book-card v-for="b in recommended" :key="b.id" :book="b" @click="openBook(b)" />

      <div class="floor"><i></i>最新上架</div>
      <book-card v-for="b in latest" :key="b.id" :book="b" @click="openBook(b)" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BookCard from '../components/BookCard.vue'
import { getBanners, bannerView, publicHome, publicBooks, recommendForUser, getDict } from '../api'
import { userStore } from '../store/user'

const router = useRouter()

const banners = ref([])
const categories = ref([])
const recommended = ref([])
const latest = ref([])
const sameMajor = ref([])
const cheap = ref([])
const sold = ref([])
const stats = ref({ onSale: 0 })
const loadingRec = ref(true)
const skeleton = ref(true)

const SHORT_NAMES = { 1: '计算机', 2: '数学', 3: '外语', 4: '经管', 5: '政治法律', 6: '文学', 7: '物理工程', 8: '考研' }
const shortName = (c) => SHORT_NAMES[c.id] || c.name.slice(0, 4)

// 换一批：随机换一页数据
async function refreshFloor(which) {
  const p = 1 + Math.floor(Math.random() * 3)
  try {
    if (which === 'major') {
      const params = { page: p, size: 8, sort: 'hot' }
      const uid = userStore.user && userStore.user.majorId
      if (uid) params.majorId = uid
      sameMajor.value = (await publicBooks(params)).list || []
    } else if (which === 'cheap') {
      cheap.value = (await publicBooks({ page: p, size: 8, priceMax: 20, sort: 'priceAsc' })).list || []
    } else {
      sold.value = (await publicBooks({ page: p, size: 8, status: 2 })).list || []
    }
  } catch (e) { /* ignore */ }
}

const go = (p) => router.push(p)
const goCategory = (c) => router.push({ path: '/search', query: { categoryId: c.id } })
const openBook = (b) => router.push('/book/' + b.id)

async function openBanner(b) {
  try { bannerView(b.id) } catch (e) { /* ignore */ }
  if (b.linkType === 'category') router.push({ path: '/search', query: { categoryId: b.linkValue } })
  else if (b.linkType === 'search') router.push({ path: '/search', query: { keyword: b.linkValue } })
  else router.push('/search')
}

onMounted(async () => {
  setTimeout(() => { skeleton.value = false }, 700)

  try {
    const dict = await getDict()
    categories.value = (dict.categories || []).slice(0, 8)
  } catch (e) { /* ignore */ }

  try { banners.value = (await getBanners('home_banner')) || [] } catch (e) { /* ignore */ }
  try {
    const home = await publicHome()
    latest.value = home.latest || []
  } catch (e) { /* ignore */ }

  try {
    const all = await publicBooks({ page: 1, size: 1 })
    stats.value.onSale = all.total || 0
  } catch (e) { /* ignore */ }

  const user = userStore.user || {}
  try {
    const params = { page: 1, size: 8, sort: 'hot' }
    if (user.majorId) params.majorId = user.majorId
    sameMajor.value = (await publicBooks(params)).list || []
  } catch (e) { /* ignore */ }

  try {
    cheap.value = (await publicBooks({ page: 1, size: 8, priceMax: 20, sort: 'priceAsc' })).list || []
  } catch (e) { /* ignore */ }

  try {
    sold.value = (await publicBooks({ page: 1, size: 6, status: 2 })).list || []
  } catch (e) { /* ignore */ }

  try {
    if (userStore.isLogin) {
      recommended.value = (await recommendForUser(6)).items || []
    } else {
      recommended.value = (await publicBooks({ page: 1, size: 6, sort: 'hot' })).list || []
    }
  } catch (e) {
    recommended.value = []
  } finally {
    loadingRec.value = false
  }
})
</script>

<style scoped>
.sk-banner, .sk-cat, .sk-cover, .sk-line {
  background: #eaeeec;
  animation: sk-shine 1.4s ease-in-out infinite;
}
@keyframes sk-shine {
  0% { opacity: .55; }
  50% { opacity: 1; }
  100% { opacity: .55; }
}
.sk-banner { height: 100px; border-radius: 14px; margin-top: 14px; }
.sk-cats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px 8px; margin: 16px 0; }
.sk-cat { width: 42px; height: 42px; border-radius: 13px; margin: 0 auto; }
.sk-card { display: flex; gap: 12px; background: #fff; border-radius: 14px; padding: 12px; margin-bottom: 10px; }
.sk-cover { width: 76px; height: 100px; border-radius: 10px; flex: none; }
.sk-lines { flex: 1; }
.sk-line { height: 12px; border-radius: 4px; margin-bottom: 9px; }
.sk-line.w70 { width: 70%; }
.sk-line.w50 { width: 50%; }
.sk-line.w30 { width: 30%; }

.hero {
  padding: 18px 16px 14px;
  background: linear-gradient(160deg, #eef7f2 0%, #f7f4ef 72%);
}
.hero-top { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 12px; }
.brand h1 { margin: 0; font-size: 21px; font-weight: 700; letter-spacing: 1px; }
.brand span { font-size: 11px; color: #8f9a94; }
.stat-pill {
  display: flex; align-items: baseline; gap: 3px;
  background: #fff; border-radius: 15px; padding: 4px 10px;
  box-shadow: 0 6px 18px -10px rgba(47, 125, 91, 0.55);
}
.stat-pill b { font-size: 15px; color: var(--brand); }
.stat-pill small { font-size: 10px; color: var(--ink-3); }

.search {
  height: 42px; background: #fff; border-radius: 21px;
  display: flex; align-items: center; gap: 8px; padding: 0 6px 0 14px;
  box-shadow: 0 8px 24px -16px rgba(27, 31, 35, 0.35);
  color: var(--ink-3); font-size: 13px;
}
.search svg { width: 15px; height: 15px; flex: none; }
.search span { flex: 1; }
.search em {
  font-style: normal; height: 32px; line-height: 32px; padding: 0 15px;
  border-radius: 16px; background: var(--brand); color: #fff; font-size: 12.5px; font-weight: 600;
}

.banners { display: flex; gap: 10px; overflow-x: auto; padding: 14px 0 4px; scrollbar-width: none; }
.banners::-webkit-scrollbar { display: none; }
.banner {
  flex: 0 0 82%; min-height: 78px; border-radius: 14px; padding: 14px; color: #fff;
  display: flex; flex-direction: column; justify-content: center;
}
.banner b { font-size: 14.5px; margin-bottom: 4px; }
.banner small { font-size: 11px; opacity: 0.86; }
.b0 { background: linear-gradient(135deg, #3f9d74, #2f7d5b); }
.b1 { background: linear-gradient(135deg, #c68a52, #a8622f); }
.b2 { background: linear-gradient(135deg, #5b7fa6, #3d5a78); }

.cats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px 8px; margin-top: 16px; }
.cat { display: flex; flex-direction: column; align-items: center; gap: 6px; font-size: 11px; color: var(--ink-2); }
.cat-dot {
  width: 42px; height: 42px; border-radius: 13px; background: var(--brand-soft);
  color: var(--brand); display: grid; place-items: center; font-size: 15px; font-weight: 600;
  font-family: 'Songti SC', 'SimSun', serif;
}

.floor {
  display: flex; align-items: center; gap: 7px;
  font-size: 15.5px; font-weight: 650; margin: 20px 0 10px;
}
.floor i { width: 3px; height: 13px; border-radius: 2px; background: var(--brand); }
.floor.warm i { background: var(--warm); }
.floor.blue i { background: #5b7fa6; }
.refresh {
  margin-left: auto; font-size: 10.5px; color: var(--brand); background: var(--brand-soft);
  border-radius: 8px; padding: 2px 7px; margin-right: 8px; cursor: pointer; font-weight: 400;
}
.floor a, .floor .sub { margin-left: auto; font-size: 12px; font-weight: 400; color: var(--ink-3); }

.hscroll { display: flex; gap: 10px; overflow-x: auto; scrollbar-width: none; padding-bottom: 2px; }
.hscroll::-webkit-scrollbar { display: none; }
.mini { flex: 0 0 82px; }
.mini-cover {
  width: 82px; height: 106px; border-radius: 10px; overflow: hidden;
  background: linear-gradient(150deg, #dfe9e2, #cfdcd2);
  display: grid; place-items: center; color: rgba(47, 125, 91, 0.45);
  font-family: 'Songti SC', 'SimSun', serif; font-size: 24px;
}
.mini-cover.cheap { background: linear-gradient(150deg, #fbeedd, #f3ddc0); color: #c07a43; }
.mini-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.mini-title {
  font-size: 11.5px; line-height: 1.35; height: 31px; overflow: hidden;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; margin-top: 5px;
}
.mini-price { font-size: 13px; font-weight: 700; color: var(--brand); }

.sold-list { background: #fff; border-radius: 14px; padding: 4px 12px; }
.sold-item { display: flex; align-items: center; padding: 10px 0; border-bottom: 1px solid #f3f0ea; }
.sold-item:last-child { border-bottom: none; }
.sold-title { flex: 1; font-size: 12.5px; color: #3c444d; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.sold-price { font-size: 12.5px; color: var(--brand); font-weight: 600; margin: 0 8px; }
.sold-flag { font-size: 10px; color: var(--ink-3); background: #f3f0ea; border-radius: 5px; padding: 1px 6px; }
</style>