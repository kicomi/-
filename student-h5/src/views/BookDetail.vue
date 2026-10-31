<template>
  <div class="detail">
    <header class="nav">
      <button class="back" @click="$router.back()">‹</button>
      <span>书籍详情</span>
      <button class="share" @click="share">分享</button>
    </header>

    <!-- 图片画廊 -->
    <div class="gallery" v-if="book">
      <div class="slides" v-if="gallery.length" ref="slides" @scroll="onScroll">
        <img v-for="(g, i) in gallery" :key="g" :src="g" @click="preview(i)" />
      </div>
      <div class="gallery-empty" v-else>
        <span class="ghost">{{ book.title.slice(0, 1) }}</span>
        <small>卖家暂未上传实拍图</small>
      </div>

      <span class="cond">{{ conditionText }}</span>
      <span class="count" v-if="gallery.length > 1">{{ index + 1 }} / {{ gallery.length }}</span>
    </div>

    <!-- 骨架屏 -->
    <div class="page" v-if="!book">
      <div class="sk-hero"></div>
      <div class="sk-block">
        <div class="sk-line w70"></div>
        <div class="sk-line w40"></div>
        <div class="sk-line w30"></div>
      </div>
      <div class="sk-block">
        <div class="sk-line w50"></div>
        <div class="sk-line w90"></div>
        <div class="sk-line w90"></div>
      </div>
    </div>

    <div class="page" v-else>
      <div class="card main-card">
        <h1>{{ book.title }}</h1>
        <p class="sub">{{ book.author }} · {{ book.publisher }} · {{ book.publishYear }}</p>
        <div class="price-row">
          <b>¥{{ book.price }}</b>
          <s>原价 ¥{{ book.originalPrice }}</s>
          <em v-if="saving > 0">省 {{ saving }}%</em>
        </div>
        <div class="tags">
          <span class="chip warm">{{ conditionText }}</span>
          <span class="chip" v-if="book.noteFlag === 1">含笔记划线</span>
          <span class="chip" v-if="book.courseName">{{ book.courseName }}</span>
        </div>
      </div>

      <div class="card">
        <h2>书况说明</h2>
        <div class="dim" v-for="d in dims" :key="d.label">
          <span class="dim-label">{{ d.label }}</span>
          <div class="dim-bar">
            <i v-for="n in 5" :key="n" :class="{ on: n <= d.value }"></i>
          </div>
          <span class="dim-text">{{ d.text }}</span>
        </div>
        <p class="selling">
          同款在售 <b>{{ book.listingCount }}</b> 本 · 近 30 天同类成交 <b>{{ book.soldCount30d || 0 }}</b> 笔
        </p>
      </div>

      <!-- 卖家画像 -->
      <div class="card" v-if="seller">
        <h2>卖家</h2>
        <div class="seller" @click="goSeller">
          <div class="avatar">
            <img v-if="seller.avatar" :src="seller.avatar" />
            <span v-else>{{ seller.nickname.slice(0, 1) }}</span>
          </div>
          <div class="seller-info">
            <b>{{ seller.nickname }} <em class="go-arrow">›</em></b>
            <p>{{ seller.majorName || '本校学生' }}<template v-if="seller.grade"> · {{ seller.grade }} 级</template></p>
          </div>
          <span class="credit">信用 {{ seller.creditScore }}</span>
        </div>

        <div class="seller-stats">
          <div><b>{{ seller.soldCount }}</b><small>已成交</small></div>
          <i></i>
          <div><b>{{ seller.goodRate }}%</b><small>好评率</small></div>
          <i></i>
          <div><b>{{ seller.onSaleCount }}</b><small>在售</small></div>
          <i></i>
          <div><b>{{ seller.ratingAvg }}</b><small>评分</small></div>
        </div>

        <div class="review" v-for="(rv, i) in seller.recentReviews" :key="i">
          <div class="review-head">
            <span>{{ rv.fromNickname }}</span>
            <em>{{ rv.rating }} 星</em>
          </div>
          <p>{{ rv.content || '买家没有填写评价内容' }}</p>
          <div class="review-imgs" v-if="reviewImgs(rv).length">
            <img v-for="(img, k) in reviewImgs(rv)" :key="k" :src="img" @click="zoomSrc = img" />
          </div>
        </div>
        <p v-if="!seller.recentReviews.length" class="review-empty">还没有收到评价</p>

        <button class="all-reviews" v-if="seller.reviewCount > 0" @click="goReviews">
          <span>共 {{ seller.reviewCount }} 条评价</span>
          <em>查看全部 ›</em>
        </button>

        <button class="contact" @click="contactSeller">私信卖家 · 问问书况</button>
      </div>

      <div class="card" v-if="book.description">
        <h2>卖家描述</h2>
        <p class="desc">{{ book.description }}</p>
      </div>

      <p class="report-entry" @click="openReport">举报这本书</p>

      <div class="card" v-if="sellerBooks.length">
        <h2>卖家其他在售（{{ sellerBooks.length }}）</h2>
        <div class="hscroll">
          <div class="mini" v-for="b in sellerBooks" :key="b.id" @click="$router.push('/book/' + b.id)">
            <div class="mini-cover">
              <img v-if="b.coverImg" :src="b.coverImg" />
              <span v-else>{{ b.title.slice(0, 1) }}</span>
            </div>
            <div class="mini-title">{{ b.title }}</div>
            <div class="mini-price">¥{{ b.price }}</div>
          </div>
        </div>
      </div>

      <div class="card" v-if="similar.length">
        <h2>相似的书</h2>
        <div class="hscroll">
          <div class="mini" v-for="b in similar" :key="b.id" @click="$router.push('/book/' + b.id)">
            <div class="mini-cover">
              <img v-if="b.coverImg" :src="b.coverImg" />
              <span v-else>{{ b.title.slice(0, 1) }}</span>
            </div>
            <div class="mini-title">{{ b.title }}</div>
            <div class="mini-price">¥{{ b.price }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="bar" v-if="book">
      <button class="fav" :class="{ on: favorited }" @click="toggleFav">{{ favorited ? '已收藏' : '收藏' }}</button>
      <button class="bargain" v-if="book.status === 1" @click="openBargain">还价</button>
      <button class="buy" v-if="book.status === 1" @click="openOrder">预约自提</button>
      <!-- 已售出不留死按钮，直接引导看同款 -->
      <button class="buy" v-else @click="findSimilar">已售出 · 看看同款</button>
    </div>

    <!-- 还价弹层 -->
    <div class="mask" v-if="bargainOpen" @click="bargainOpen = false"></div>
    <div class="sheet" v-if="bargainOpen">
      <h3>向卖家还价</h3>
      <p class="sheet-tip">标价 ¥{{ book.price }}，出价会作为一条消息发给卖家</p>
      <div class="bargain-input">
        <span>¥</span>
        <input v-model="bargainPrice" type="number" placeholder="请输入你的心理价" />
      </div>
      <button class="btn-primary" @click="sendBargain">发送出价</button>
    </div>

    <!-- 图片放大 -->
    <div class="zoom" v-if="zoomSrc" @click="zoomSrc = ''">
      <img :src="zoomSrc" />
    </div>

    <!-- 举报弹层 -->
    <div class="mask" v-if="reportOpen" @click="reportOpen = false"></div>
    <div class="sheet" v-if="reportOpen">
      <h3>举报这本书</h3>
      <p class="sheet-tip">请选择原因，管理员会核实处理</p>
      <div class="reasons">
        <span v-for="r in reportReasons" :key="r" class="reason" :class="{ on: reportReason === r }" @click="reportReason = r">{{ r }}</span>
      </div>
      <textarea v-model="reportDetail" rows="3" placeholder="补充说明（选填），写清楚问题更容易被处理" />
      <button class="btn-primary" @click="submitReportAction">提交举报</button>
    </div>

    <!-- 预约弹层 -->
    <div class="mask" v-if="orderOpen" @click="orderOpen = false"></div>
    <div class="sheet" v-if="orderOpen">
      <h3>预约自提</h3>
      <label class="field-label">交接地点</label>
      <select v-model="pickupPlace">
        <option v-for="p in places" :key="p" :value="p">{{ p }}</option>
      </select>
      <label class="field-label">给卖家留言</label>
      <textarea v-model="remark" rows="3" placeholder="例如：今晚七点在图书馆门口可以吗" />
      <button class="btn-primary" @click="submitOrder">确认预约</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  bookDetail, publicBook, similarBooks, toggleFavorite, sellerProfile,
  createOrder, sendMessage, submitReport
} from '../api'
import { userStore } from '../store/user'

const route = useRoute()
const router = useRouter()

const CONDITION = { 1: '四成新', 2: '五成新', 3: '七成新', 4: '九成新', 5: '全新' }
const WEAR = { 1: '磨损明显', 2: '有折痕', 3: '正常使用痕迹', 4: '接近崭新', 5: '几乎全新' }

const book = ref(null)
const seller = ref(null)
const similar = ref([])
const sellerBooks = ref([])
const favorited = ref(false)
const gallery = ref([])
const index = ref(0)
const slides = ref(null)

const places = ['图书馆一楼', '一食堂门口', '三号教学楼', '宿舍楼下']
const pickupPlace = ref(places[0])
const remark = ref('')

const zoomSrc = ref('')
const reportOpen = ref(false)
const reportReason = ref('')
const reportDetail = ref('')
const reportReasons = ['书况与描述不符', '疑似虚假信息', '价格异常', '涉嫌诈骗', '内容违规', '其他']

const bargainOpen = ref(false)
const bargainPrice = ref('')
const orderOpen = ref(false)

const conditionText = computed(() =>
  book.value ? (CONDITION[book.value.conditionLevel] || '七成新') : '')

const saving = computed(() => {
  if (!book.value) return 0
  const orig = Number(book.value.originalPrice) || 0
  const price = Number(book.value.price) || 0
  return orig > 0 ? Math.round((1 - price / orig) * 100) : 0
})

const dims = computed(() => {
  const b = book.value
  if (!b) return []
  return [
    { label: '品相', value: b.conditionLevel || 3, text: CONDITION[b.conditionLevel] || '七成新' },
    { label: '新旧', value: b.wearLevel || 3, text: WEAR[b.wearLevel] || '正常使用痕迹' },
    { label: '笔记', value: b.noteFlag === 1 ? 4 : 5, text: b.noteFlag === 1 ? '有少量笔记' : '无笔记划线' },
    { label: '热度', value: Math.min(5, (b.soldCount30d || 0) + 1), text: '同类近30天成交 ' + (b.soldCount30d || 0) }
  ]
})

function onScroll(e) {
  const el = e.target
  if (!el.clientWidth) return
  index.value = Math.round(el.scrollLeft / el.clientWidth)
}

function preview(i) {
  window.open(gallery.value[i])
}

function share() {
  if (navigator.clipboard) navigator.clipboard.writeText(location.href)
  alert('链接已复制')
}

async function toggleFav() {
  if (!userStore.isLogin) return router.push('/login')
  try {
    const now = await toggleFavorite(book.value.id, !favorited.value)
    favorited.value = !!now
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

function contactSeller() {
  if (!userStore.isLogin) return router.push('/login')
  const b = book.value
  router.push({
    path: '/chat',
    query: {
      peerId: b.sellerId,
      peerName: b.sellerNickname || '卖家',
      bookId: b.id,
      bookTitle: b.title
    }
  })
}

// 评价图片是 JSON 字符串
function reviewImgs(rv) {
  try {
    const arr = JSON.parse(rv.images || '[]')
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

function goReviews() {
  router.push({ path: '/reviews/' + book.value.id, query: { title: book.value.title } })
}

function openReport() {
  if (!userStore.isLogin) return router.push('/login')
  reportReason.value = ''
  reportDetail.value = ''
  reportOpen.value = true
}

async function submitReportAction() {
  if (!reportReason.value) return alert('请选择举报原因')
  try {
    await submitReport({
      targetType: 'book',
      targetId: book.value.id,
      reason: reportReason.value,
      detail: reportDetail.value
    })
    reportOpen.value = false
    alert('已提交，管理员会尽快处理')
  } catch (e) {
    alert(e.message || '提交失败')
  }
}

function openBargain() {
  if (!userStore.isLogin) return router.push('/login')
  bargainPrice.value = String(Math.max(1, Math.round(Number(book.value.price) * 0.9)))
  bargainOpen.value = true
}

async function sendBargain() {
  const price = Number(bargainPrice.value)
  if (!price || price <= 0) return alert('请输入价格')
  if (price >= Number(book.value.price)) return alert('还价要低于标价')
  try {
    await sendMessage({
      toUserId: book.value.sellerId,
      bookId: book.value.id,
      content: '我对《' + book.value.title + '》出价 ¥' + price + '，可以吗？'
    })
    bargainOpen.value = false
    alert('已发给卖家')
  } catch (e) {
    alert(e.message || '发送失败')
  }
}

function goSeller() {
  if (book.value && book.value.sellerId) router.push('/user/' + book.value.sellerId)
}

function findSimilar() {
  const kw = book.value.courseName || (book.value.title || '').replace(/（.*?）/g, '')
  router.push({ path: '/search', query: { keyword: kw } })
}

function openOrder() {
  if (!userStore.isLogin) return router.push('/login')
  orderOpen.value = true
}

async function submitOrder() {
  try {
    await createOrder({ bookId: book.value.id, pickupPlace: pickupPlace.value, remark: remark.value })
    orderOpen.value = false
    // 预约后买家最想做的是跟卖家约时间
    if (confirm('预约成功，要现在跟卖家约个交接时间吗？')) {
      router.push({
        path: '/chat',
        query: {
          peerId: book.value.sellerId,
          peerName: book.value.sellerNickname || '卖家',
          bookId: book.value.id,
          bookTitle: book.value.title
        }
      })
    } else {
      router.push('/orders')
    }
  } catch (e) {
    alert(e.message || '预约失败')
  }
}

onMounted(async () => {
  const id = route.params.id
  try {
    book.value = userStore.isLogin ? await bookDetail(id) : await publicBook(id)
  } catch (e) {
    alert(e.message || '加载失败')
    return
  }

  // 图片：coverImg + images(JSON 字符串)
  const imgs = []
  if (book.value.coverImg) imgs.push(book.value.coverImg)
  try {
    const arr = JSON.parse(book.value.images || '[]')
    if (Array.isArray(arr)) arr.forEach((u) => { if (u && !imgs.includes(u)) imgs.push(u) })
  } catch (e) { /* ignore */ }
  gallery.value = imgs

  favorited.value = !!book.value.favorited

  try { seller.value = await sellerProfile(book.value.sellerId) } catch (e) { /* ignore */ }

  // 卖家其他在售：凑单时不用回搜索页
  try {
    const d = await publicBooks({ sellerId: book.value.sellerId, size: 8 })
    sellerBooks.value = (d.list || []).filter((x) => x.id !== book.value.id)
  } catch (e) { /* ignore */ }
  try { similar.value = (await similarBooks(id, 6)) || [] } catch (e) { /* ignore */ }
})
</script>

<style scoped>
@keyframes sk-shine3 {
  0% { opacity: .55; }
  50% { opacity: 1; }
  100% { opacity: .55; }
}
.sk-hero { height: 300px; background: #eaeeec; animation: sk-shine3 1.4s ease-in-out infinite; }
.sk-block { background: #fff; border-radius: 14px; padding: 14px; margin: 10px 12px; }
.sk-line { height: 12px; border-radius: 4px; background: #eef1f0; margin-bottom: 9px; animation: sk-shine3 1.4s ease-in-out infinite; }
.sk-line.w90 { width: 90%; }
.sk-line.w70 { width: 70%; }
.sk-line.w50 { width: 50%; }
.sk-line.w40 { width: 40%; }
.sk-line.w30 { width: 30%; }

.detail { padding-bottom: 76px; background: #f7f4ef; min-height: 100vh; }
.nav {
  position: sticky; top: 0; z-index: 10;
  height: 48px; display: flex; align-items: center; justify-content: space-between;
  padding: 0 12px; background: #f7f4ef; border-bottom: 1px solid rgba(0, 0, 0, .04);
  font-size: 15px; font-weight: 600;
}
.back { border: none; background: none; font-size: 24px; width: 32px; text-align: left; line-height: 1; }
.share { border: none; background: none; font-size: 13px; color: var(--brand); }

.gallery { position: relative; background: #f0ece4; }
.slides {
  display: flex; overflow-x: auto; scroll-snap-type: x mandatory; scrollbar-width: none;
}
.slides::-webkit-scrollbar { display: none; }
.slides img {
  width: 100%; height: 300px; object-fit: contain; flex: 0 0 100%;
  scroll-snap-align: start; background: #f0ece4;
}
.gallery-empty {
  height: 300px; display: flex; flex-direction: column; align-items: center; justify-content: center;
  background: linear-gradient(150deg, #dfe9e2, #cfdcd2);
}
.gallery-empty .ghost { font-size: 64px; color: rgba(47, 125, 91, .35); font-family: 'Songti SC', serif; line-height: 1; }
.gallery-empty small { margin-top: 8px; font-size: 11px; color: rgba(47, 125, 91, .55); }
.cond {
  position: absolute; left: 12px; bottom: 12px; background: rgba(47, 125, 91, .92);
  color: #fff; font-size: 10.5px; padding: 2px 9px; border-radius: 10px;
}
.count {
  position: absolute; right: 12px; bottom: 12px; background: rgba(0, 0, 0, .45);
  color: #fff; font-size: 10.5px; padding: 2px 8px; border-radius: 10px;
}

.card { margin: 0 12px 10px; background: #fff; border-radius: 14px; padding: 14px; }
.main-card { margin-top: -14px; position: relative; z-index: 2; }
.card h2 { font-size: 13.5px; font-weight: 650; margin: 0 0 10px; }
.card h1 { font-size: 17px; font-weight: 650; margin: 0 0 5px; line-height: 1.35; }
.sub { margin: 0 0 8px; font-size: 11.5px; color: var(--ink-3); }
.price-row { display: flex; align-items: baseline; gap: 7px; flex-wrap: wrap; }
.price-row b { font-size: 24px; color: var(--brand); font-weight: 700; }
.price-row s { font-size: 11.5px; color: #b9b4ab; }
.price-row em { font-style: normal; font-size: 10.5px; color: var(--danger); background: #fbeeeb; border-radius: 4px; padding: 1px 6px; }
.tags { margin-top: 9px; display: flex; gap: 6px; flex-wrap: wrap; }
.chip { font-size: 10.5px; color: var(--ink-2); background: #f5f2ec; border-radius: 4px; padding: 2px 7px; }
.chip.warm { color: var(--brand); background: var(--brand-soft); }

.dim { display: flex; align-items: center; margin-bottom: 9px; }
.dim-label { width: 34px; font-size: 12.5px; color: var(--ink-2); }
.dim-bar { display: flex; gap: 3px; }
.dim-bar i { width: 16px; height: 5px; border-radius: 3px; background: #eee9e1; }
.dim-bar i.on { background: var(--brand); }
.dim-text { margin-left: auto; font-size: 11px; color: var(--ink-3); }
.selling { margin: 9px 0 0; padding-top: 9px; border-top: 1px dashed #eee9e1; font-size: 11.5px; color: var(--ink-3); }
.selling b { color: var(--brand); }

.seller { cursor: pointer; }
.go-arrow { font-style: normal; color: #c8c2b8; font-size: 13px; }

.seller { display: flex; align-items: center; gap: 10px; }
.avatar {
  width: 42px; height: 42px; border-radius: 50%; overflow: hidden; flex: none;
  background: var(--brand-soft); color: var(--brand);
  display: grid; place-items: center; font-weight: 600;
}
.avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }
.seller-info { flex: 1; min-width: 0; }
.seller-info b { font-size: 14.5px; }
.seller-info p { margin: 2px 0 0; font-size: 11px; color: var(--ink-3); }
.credit { font-size: 10.5px; color: var(--brand); background: var(--brand-soft); border-radius: 4px; padding: 2px 7px; }

.seller-stats {
  display: flex; align-items: center; background: #f9f7f3; border-radius: 9px;
  padding: 10px 0; margin-top: 10px;
}
.seller-stats div { flex: 1; display: flex; flex-direction: column; align-items: center; }
.seller-stats b { font-size: 15px; }
.seller-stats small { font-size: 10px; color: var(--ink-3); margin-top: 2px; }
.seller-stats i { width: 1px; height: 18px; background: #ece7df; }

.review { margin-top: 10px; padding: 9px 10px; background: #f9f7f3; border-radius: 8px; }
.review-head { display: flex; justify-content: space-between; margin-bottom: 4px; }
.review-head span { font-size: 11.5px; color: var(--ink-2); }
.review-head em { font-style: normal; font-size: 10.5px; color: #d9a441; }
.review-imgs { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 7px; }
.review-imgs img {
  width: 64px; height: 64px; border-radius: 6px; object-fit: cover;
  display: block; cursor: pointer; background: #eee9e1;
}
.review p { margin: 0; font-size: 12px; color: #3c444d; line-height: 1.6; }
.review-empty { margin: 9px 0 0; text-align: center; font-size: 11.5px; color: #b9b4ab; }

.contact {
  margin-top: 11px; width: 100%; height: 38px; border-radius: 19px;
  border: 1px solid var(--brand); background: #f4faf7;
  color: var(--brand); font-size: 13px; font-weight: 600;
}
.desc { margin: 0; font-size: 13px; color: var(--ink-2); line-height: 1.7; }

.hscroll { display: flex; gap: 10px; overflow-x: auto; scrollbar-width: none; }
.hscroll::-webkit-scrollbar { display: none; }
.mini { flex: 0 0 78px; }
.mini-cover {
  width: 78px; height: 102px; border-radius: 9px; overflow: hidden;
  background: linear-gradient(150deg, #dfe9e2, #cfdcd2);
  display: grid; place-items: center; color: rgba(47, 125, 91, .45);
  font-family: 'Songti SC', serif; font-size: 22px;
}
.mini-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.mini-title {
  font-size: 11px; line-height: 1.35; height: 30px; overflow: hidden;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; margin-top: 4px;
}
.mini-price { font-size: 12.5px; font-weight: 700; color: var(--brand); }

.bar {
  position: fixed; left: 0; right: 0; bottom: 0; height: 64px;
  background: #fff; border-top: 1px solid var(--line);
  display: flex; align-items: center; gap: 8px; padding: 0 12px;
  padding-bottom: env(safe-area-inset-bottom);
}
.fav {
  width: 64px; height: 44px; border-radius: 22px; border: 1px solid var(--line);
  background: #fff; font-size: 12.5px; color: var(--ink-2);
}
.fav.on { color: var(--brand); border-color: var(--brand); background: var(--brand-soft); }
.bargain {
  width: 64px; height: 44px; border-radius: 22px; border: 1px solid #e2a33c;
  background: #fdf6e9; color: #b8822a; font-size: 12.5px;
}
.buy {
  flex: 1; height: 44px; border-radius: 22px; border: none;
  background: var(--brand); color: #fff; font-size: 14px; font-weight: 600;
}
.buy.disabled { background: #d9d5cd; }

.mask { position: fixed; inset: 0; background: rgba(20, 24, 28, .42); z-index: 20; }
.sheet {
  position: fixed; left: 0; right: 0; bottom: 0; z-index: 21;
  background: #f7f4ef; border-radius: 18px 18px 0 0; padding: 18px 16px 26px;
}
.sheet h3 { margin: 0 0 10px; font-size: 15px; }
.sheet-tip { margin: 0 0 11px; font-size: 11.5px; color: var(--ink-3); }
.bargain-input {
  display: flex; align-items: center; gap: 6px; background: #fff;
  border: 1px solid var(--line); border-radius: 10px; padding: 0 12px; height: 48px; margin-bottom: 14px;
}
.bargain-input span { font-size: 17px; color: var(--brand); font-weight: 700; }
.bargain-input input { flex: 1; border: none; font-size: 16px; background: transparent; }
.field-label { display: block; font-size: 12px; color: var(--ink-2); margin: 10px 0 5px; }
.sheet select, .sheet textarea {
  width: 100%; border: 1px solid var(--line); border-radius: 10px;
  padding: 10px 12px; font-size: 13.5px; background: #fff; font-family: inherit;
}
.all-reviews {
  display: flex; justify-content: space-between; align-items: center;
  width: 100%; margin-top: 8px; padding: 9px 10px;
  background: #f9f7f3; border: none; border-radius: 7px;
  font-size: 12px; color: var(--ink-2);
}
.all-reviews em { font-style: normal; color: var(--brand); }
.zoom {
  position: fixed; inset: 0; z-index: 60; background: rgba(0, 0, 0, .92);
  display: grid; place-items: center; cursor: zoom-out;
}
.zoom img { max-width: 96vw; max-height: 92vh; object-fit: contain; }

.report-entry {
  text-align: center; font-size: 11.5px; color: #b9b4ab;
  text-decoration: underline; padding: 6px 0 16px; margin: 0; cursor: pointer;
}
.reasons { display: flex; flex-wrap: wrap; gap: 7px; margin-bottom: 12px; }
.reason {
  font-size: 12px; color: var(--ink-2); background: #fff;
  border: 1px solid var(--line); border-radius: 13px; padding: 5px 12px; cursor: pointer;
}
.reason.on { color: var(--danger); background: #fbeeeb; border-color: #e8c4bb; }

.btn-primary {
  margin-top: 16px; width: 100%; height: 46px; border-radius: 23px; border: none;
  background: var(--brand); color: #fff; font-size: 14.5px; font-weight: 600;
}
</style>