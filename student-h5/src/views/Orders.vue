<template>
  <div class="orders">
    <header class="nav">订单</header>

    <div class="tabs">
      <div class="tab" :class="{ on: role === 'buyer' }" @click="switchRole('buyer')">我买到的</div>
      <div class="tab" :class="{ on: role === 'seller' }" @click="switchRole('seller')">我卖出的</div>
    </div>

    <div class="page">
      <div v-if="loading" class="loading">加载中…</div>
      <template v-if="!loading">
        <div class="order-card" v-for="o in rows" :key="o.id">
          <div class="o-head">
            <span class="no">{{ o.orderNo }}</span>
            <span class="status" :class="statusClass(o.status)">{{ o.statusText }}</span>
          </div>

          <div class="o-body" @click="openBook(o)">
            <div class="o-cover">
              <img v-if="o.bookCover" :src="o.bookCover" />
              <span v-else>{{ (o.bookTitle || '书').slice(0, 1) }}</span>
            </div>
            <div class="o-info">
              <div class="o-title">{{ o.bookTitle }}</div>
              <div class="o-meta">{{ role === 'buyer' ? '卖家' : '买家' }}：{{ role === 'buyer' ? o.sellerNickname : o.buyerNickname }}</div>
              <div class="o-meta">{{ o.pickupPlace || '未指定地点' }}</div>
            </div>
            <div class="o-price"><span class="symbol">¥</span>{{ o.price }}</div>
          </div>

          <div class="countdown" :class="{ urgent: o.remainHours <= 2 }"
               v-if="o.status === 0 && o.remainHours !== null && o.remainHours !== undefined">
            {{ o.remainHours > 0 ? '卖家还有 ' + o.remainHours + ' 小时确认，超时自动取消' : '即将自动取消' }}
          </div>

          <div class="timeline">
            <div class="tl-node" v-for="t in o.timeline" :key="t.name">
              <div class="tl-line left" :class="{ hide: t.isFirst, red: t.state === 'cancel' }"></div>
              <div class="tl-dot" :class="t.state"></div>
              <div class="tl-line right" :class="{ hide: t.isLast, red: t.state === 'cancel' }"></div>
              <span class="tl-name" :class="t.state">{{ t.name }}</span>
              <span class="tl-time">{{ t.time || ' ' }}</span>
            </div>
          </div>

          <div class="o-actions">
            <button v-if="role === 'seller' && o.status === 0" class="mini primary" @click="confirm(o)">确认预约</button>
            <button v-if="o.status === 1" class="mini primary" @click="complete(o)">完成交易</button>
            <button v-if="o.status === 0 || o.status === 1" class="mini" @click="cancel(o)">取消</button>
            <button v-if="o.status !== 3" class="mini" @click="openAppeal(o)">申诉</button>
            <button v-if="role === 'buyer' && o.status === 2 && !o.reviewed" class="mini primary" @click="openReview(o)">评价</button>
            <button v-if="o.status === 2 && o.reviewed" class="mini" @click="openViewed(o)">查看评价</button>
          </div>
        </div>

        <button v-if="hasMore && rows.length" class="more-btn" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
        <div v-if="!hasMore && rows.length" class="more-tip">没有更多了</div>

        <div v-if="!rows.length" class="empty">
          <div class="icon">📦</div>
          {{ role === 'buyer' ? '还没有买过书' : '还没有卖出过书' }}
        </div>
      </template>
    </div>

    <transition name="fade">
      <div class="sheet" v-if="appealOpen">
        <div class="sheet-mask" @click="appealOpen = false"></div>
        <div class="sheet-body">
          <div class="sheet-title">这笔订单有问题</div>
          <p class="appeal-tip">提交后管理员会介入核实，处理结果会通知你</p>
          <textarea v-model="appealText" rows="3" placeholder="说明情况，例如：约好了时间卖家一直没来"></textarea>
          <button class="btn btn-primary btn-block" style="margin-top: 12px" @click="submitAppeal">提交申诉</button>
        </div>
      </div>
    </transition>

    <transition name="fade">
      <div class="sheet" v-if="viewOpen">
        <div class="sheet-mask" @click="viewOpen = false"></div>
        <div class="sheet-body">
          <div class="sheet-title">我的评价</div>
          <div class="view-stars">{{ viewed.reviewRating }} 星</div>
          <p class="view-content">{{ viewed.reviewContent || '当时没有填写评价内容' }}</p>
          <div class="view-imgs" v-if="viewedImgs.length">
            <img v-for="(img, i) in viewedImgs" :key="i" :src="img" @click="window.open(img)" />
          </div>
          <div class="view-actions">
            <button class="mini" @click="startEdit">修改</button>
            <button class="mini danger" @click="doDelete">删除</button>
            <button class="mini primary" @click="viewOpen = false">关闭</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="fade">
      <div class="sheet" v-if="reviewOpen">
        <div class="sheet-mask" @click="reviewOpen = false"></div>
        <div class="sheet-body">
          <div class="sheet-title">评价这次交易</div>
          <div class="stars">
            <span v-for="n in 5" :key="n" :class="{ on: n <= rating }" @click="rating = n">★</span>
          </div>
          <textarea v-model="content" rows="3" placeholder="书况和描述一致吗？沟通是否顺畅？"></textarea>

          <div class="review-photos">
            <div class="rp" v-for="(p, i) in reviewPreview" :key="p">
              <img :src="p" />
              <span class="del" @click="removeImage(i)">×</span>
            </div>
            <div class="rp-add" v-if="reviewImages.length < 3" @click="pickImage">
              {{ uploading ? '上传中' : '+ 配图' }}
            </div>
            <input ref="fileRef" type="file" accept="image/*" multiple style="display: none" @change="onFiles" />
          </div>
          <button class="btn btn-primary btn-block" style="margin-top: 12px" @click="submitReview">提交评价</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { myOrders, confirmOrder, completeOrder, cancelOrder, createReview,
         uploadImage, updateReview, deleteReview, submitReport } from '../api'

const router = useRouter()

const role = ref('buyer')
const rows = ref([])
const loading = ref(false)

// 分页
const page = ref(1)
const hasMore = ref(true)
const loadingMore = ref(false)

// 评价配图
const viewOpen = ref(false)
const viewed = ref({})
const viewedImgs = ref([])

const reviewImages = ref([])
const reviewPreview = ref([])
const uploading = ref(false)
const fileRef = ref(null)

const reviewOpen = ref(false)
const rating = ref(5)
const content = ref('')
const currentOrder = ref(null)

function statusClass(s) {
  if (s === 2) return 'ok'
  if (s === 3) return 'cancel'
  if (s === 1) return 'doing'
  return 'wait'
}

// 只保留 月-日 时:分
function shortTime(t) {
  if (!t) return ''
  const s = String(t).replace('T', ' ')
  return s.length >= 16 ? s.slice(5, 16) : s
}

/** 订单状态展开成时间轴节点：0 待确认 / 1 已确认 / 2 完成 / 3 取消 */
function buildTimeline(o) {
  let list
  if (o.status === 3) {
    list = [
      { name: '下单', time: shortTime(o.createdAt), state: 'done' },
      { name: '已取消', time: shortTime(o.updatedAt || o.createdAt), state: 'cancel' }
    ]
  } else {
    const confirmed = o.status >= 1
    const finished = o.status === 2
    list = [
      { name: '下单', time: shortTime(o.createdAt), state: 'done' },
      { name: '卖家确认', time: confirmed ? shortTime(o.confirmedAt || o.updatedAt) : '', state: confirmed ? 'done' : 'todo' },
      { name: '交易完成', time: finished ? shortTime(o.finishedAt || o.updatedAt) : '', state: finished ? 'done' : 'todo' }
    ]
  }
  list.forEach((n, i) => {
    n.isFirst = i === 0
    n.isLast = i === list.length - 1
  })
  return list
}

async function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  loadingMore.value = true
  try {
    const next = page.value + 1
    const data = await myOrders(role.value, next, 10)
    const more = data.list || []
    more.forEach((o) => { o.timeline = buildTimeline(o) })
    rows.value = rows.value.concat(more)
    page.value = next
    hasMore.value = more.length >= 10
  } catch (e) {
    hasMore.value = false
  } finally {
    loadingMore.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const data = await myOrders(role.value, 1, 10)
    const list = data.list || []
    list.forEach((o) => { o.timeline = buildTimeline(o) })
    rows.value = list
    hasMore.value = list.length >= 10
    page.value = 1
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function switchRole(r) {
  role.value = r
  load()
}

function openBook(o) {
  router.push('/book/' + o.bookId)
}

async function confirm(o) {
  await confirmOrder(o.id)
  load()
}

async function complete(o) {
  await completeOrder(o.id)
  load()
}

async function cancel(o) {
  if (!confirm('确定取消这笔订单吗？')) return
  await cancelOrder(o.id)
  load()
}

// 修改评价：复用原来的评价弹层，预填内容
function startEdit() {
  const o = viewed.value
  currentOrder.value = o
  rating.value = o.reviewRating || 5
  content.value = o.reviewContent || ''
  reviewImages.value = [...viewedImgs.value]
  reviewPreview.value = [...viewedImgs.value]
  viewOpen.value = false
  reviewOpen.value = true
}

async function doDelete() {
  if (!confirm('确定删除这条评价吗？')) return
  try {
    await deleteReview(viewed.value.reviewId)
    alert('已删除')
    viewOpen.value = false
    load()
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

function openViewed(o) {
  viewed.value = o
  try {
    const arr = JSON.parse(o.reviewImages || '[]')
    viewedImgs.value = Array.isArray(arr) ? arr : []
  } catch (e) {
    viewedImgs.value = []
  }
  viewOpen.value = true
}

// ---------- 订单申诉 ----------
const appealOpen = ref(false)
const appealOrder = ref(null)
const appealText = ref('')

function openAppeal(o) {
  appealOrder.value = o
  appealText.value = ''
  appealOpen.value = true
}

async function submitAppeal() {
  const t = (appealText.value || '').trim()
  if (!t) return alert('请说明情况')
  try {
    await submitReport({ targetType: 'order', targetId: appealOrder.value.id, reason: '交易纠纷', detail: t })
    appealOpen.value = false
    alert('已提交，等待管理员处理')
  } catch (e) {
    alert(e.message || '提交失败')
  }
}

function pickImage() {
  fileRef.value && fileRef.value.click()
}

async function onFiles(e) {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  uploading.value = true
  try {
    for (const f of files) {
      if (reviewImages.value.length >= 3) break
      const up = await uploadImage(f)
      reviewImages.value.push(up.url)
      reviewPreview.value.push(up.url)
    }
  } catch (err) {
    alert(err.message || '上传失败')
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

function removeImage(i) {
  reviewImages.value.splice(i, 1)
  reviewPreview.value.splice(i, 1)
}

function openReview(o) {
  currentOrder.value = o
  rating.value = 5
  content.value = ''
  reviewImages.value = []
  reviewPreview.value = []
  reviewOpen.value = true
}

async function submitReview() {
  try {
    const payload = {
      rating: rating.value,
      content: content.value,
      images: JSON.stringify(reviewImages.value)
    }
    if (currentOrder.value.reviewed) {
      await updateReview(currentOrder.value.reviewId, payload)
    } else {
      await createReview({ ...payload, orderId: currentOrder.value.id })
    }
    reviewOpen.value = false
    load()
  } catch (e) {
    alert(e.message || '评价失败')
  }
}

onMounted(load)
</script>

<style scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 10;
  height: 48px;
  line-height: 48px;
  text-align: center;
  font-size: 15px;
  font-weight: 650;
  background: var(--bg);
}

.tabs {
  display: flex;
  padding: 0 16px 8px;
  gap: 18px;
}

.tab {
  font-size: 14px;
  color: var(--ink-3);
  padding-bottom: 6px;
  position: relative;
}

.tab.on {
  color: var(--ink);
  font-weight: 650;
}

.tab.on::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 0;
  width: 20px;
  height: 3px;
  border-radius: 2px;
  background: var(--brand);
}

.order-card {
  background: #fff;
  border-radius: var(--radius);
  padding: 12px 14px;
  margin-bottom: 10px;
}

.o-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11.5px;
  color: var(--ink-3);
  padding-bottom: 10px;
  border-bottom: 1px solid var(--line);
}

.status.wait { color: #c9962e; }
.status.doing { color: var(--brand); }
.status.ok { color: var(--brand); }
.status.cancel { color: var(--ink-3); }

.o-body {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  align-items: center;
}

.o-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }

.o-cover {
  width: 46px;
  height: 62px;
  border-radius: 7px;
  background: linear-gradient(150deg, #dfe9e2, #cfdcd2);
  display: grid;
  place-items: center;
  color: rgba(47, 125, 91, 0.5);
  font-family: 'Songti SC', 'SimSun', serif;
  font-size: 18px;
  flex: none;
}

.o-info {
  flex: 1;
  min-width: 0;
}

.o-title {
  font-size: 13.5px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.o-meta {
  font-size: 11.5px;
  color: var(--ink-3);
  margin-top: 2px;
}

.o-price {
  font-size: 16px;
  font-weight: 700;
  color: var(--brand);
}

.o-price .symbol {
  font-size: 11px;
}

.review-photos { display: flex; flex-wrap: wrap; gap: 7px; margin-top: 9px; }
.rp { position: relative; width: 62px; height: 62px; border-radius: 8px; overflow: hidden; }
.rp img { width: 100%; height: 100%; object-fit: cover; display: block; }
.rp .del {
  position: absolute; top: 0; right: 0; width: 17px; height: 17px;
  line-height: 15px; text-align: center; background: rgba(0,0,0,.5); color: #fff;
  font-size: 12px; border-bottom-left-radius: 6px; cursor: pointer;
}
.rp-add {
  width: 62px; height: 62px; border-radius: 8px; border: 1px dashed #d6d0c6;
  background: #fff; display: grid; place-items: center; font-size: 10.5px;
  color: #b9b4ab; cursor: pointer;
}
.appeal-tip { margin: 0 0 10px; font-size: 11.5px; color: var(--ink-3); line-height: 1.6; }

.view-actions { display: flex; gap: 8px; margin-top: 12px; }
.view-actions .mini { flex: 1; height: 38px; border-radius: 19px; border: 1px solid var(--line); background: #fff; font-size: 12.5px; color: var(--ink-2); }
.view-actions .mini.primary { background: var(--brand); color: #fff; border-color: var(--brand); }
.view-actions .mini.danger { color: var(--danger); border-color: #e8c4bb; }

.view-stars { font-size: 17px; color: #e2a33c; margin-bottom: 8px; }
.view-content {
  margin: 0; font-size: 13.5px; color: #3c444d; line-height: 1.7;
  background: #fff; border-radius: 10px; padding: 10px 12px;
}
.view-imgs { display: flex; flex-wrap: wrap; gap: 7px; margin-top: 9px; }
.view-imgs img { width: 74px; height: 74px; border-radius: 8px; object-fit: cover; display: block; }

.more-tip { text-align: center; font-size: 11px; color: #b9b4ab; padding: 12px 0 4px; }
@keyframes sk-shine {
  0% { opacity: .55; }
  50% { opacity: 1; }
  100% { opacity: .55; }
}
.sk-card { background: #fff; border-radius: 14px; padding: 13px; margin-bottom: 9px; }
.sk-line, .sk-cover { background: #eef1f0; border-radius: 4px; animation: sk-shine 1.4s ease-in-out infinite; }
.sk-line { height: 12px; margin-bottom: 7px; }
.sk-line.w90 { width: 90%; }
.sk-line.w70 { width: 70%; }
.sk-line.w50 { width: 50%; }
.sk-line.w40 { width: 40%; }
.sk-line.w30 { width: 30%; }
.sk-row { display: flex; gap: 10px; margin-top: 8px; }
.sk-cover { width: 46px; height: 62px; border-radius: 7px; flex: none; }
.sk-lines { flex: 1; }

.more-btn {
  display: block; width: 100%; height: 40px; margin: 4px 0 8px;
  border-radius: 20px; border: 1px solid var(--line);
  background: #fff; color: var(--brand); font-size: 12.5px; font-weight: 500;
}
.more-btn:disabled { color: var(--ink-3); }

.countdown {
  font-size: 10.5px; color: #b8822a; background: #fdf6e9;
  border-radius: 5px; padding: 4px 8px; margin-top: 7px;
}
.countdown.urgent { color: var(--danger); background: #fbeeeb; }

.timeline {
  display: flex;
  padding: 11px 0 3px;
  border-top: 1px solid #f4f1eb;
  margin-top: 2px;
}
.tl-node { flex: 1; display: flex; flex-direction: column; align-items: center; position: relative; }
.tl-dot { width: 8px; height: 8px; border-radius: 50%; background: #e2ddd4; margin-bottom: 5px; z-index: 2; }
.tl-dot.done { background: var(--brand); }
.tl-dot.done::after {
  content: ''; position: absolute; top: 0; width: 8px; height: 8px; border-radius: 50%;
  background: rgba(47, 125, 91, .25); transform: scale(1.8);
}
.tl-dot.cancel { background: var(--danger); }
.tl-line { position: absolute; top: 3px; height: 1px; background: #e2ddd4; width: 50%; z-index: 1; }
.tl-line.left { left: 0; }
.tl-line.right { right: 0; }
.tl-line.hide { display: none; }
.tl-line.red { background: #e6c4bc; }
.tl-name { font-size: 10.5px; color: #b9b4ab; }
.tl-name.done { color: var(--ink); font-weight: 600; }
.tl-name.cancel { color: var(--danger); font-weight: 600; }
.tl-time { font-size: 9.5px; color: #c4bfb7; margin-top: 2px; }

.o-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid var(--line);
}

.mini {
  height: 30px;
  padding: 0 14px;
  border-radius: 15px;
  border: 1px solid var(--line);
  background: #fff;
  font-size: 12.5px;
  color: var(--ink-2);
}

.mini.primary {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}

.done {
  font-size: 12px;
  color: var(--ink-3);
  align-self: center;
}

.sheet {
  z-index: 21;
  position: fixed;
  inset: 0;
  z-index: 30;
}

.sheet-mask {
  position: absolute;
  inset: 0;
  background: rgba(20, 24, 28, 0.42);
}

.sheet-body {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 430px;
  background: var(--bg);
  border-radius: 18px 18px 0 0;
  padding: 18px 16px calc(18px + env(safe-area-inset-bottom, 0px));
}

.sheet-title {
  font-size: 15px;
  font-weight: 650;
  margin-bottom: 12px;
}

.stars {
  font-size: 26px;
  color: #e2e1dc;
  letter-spacing: 4px;
  margin-bottom: 12px;
}

.stars .on {
  color: #e2a33c;
}
</style>