<template>
  <div class="user">
    <header class="nav">
      <button class="back" @click="$router.back()">‹</button>
      <span>{{ profile.nickname || '个人主页' }}</span>
      <div style="width: 32px"></div>
    </header>

    <template v-if="profile.userId">
      <div class="banner">
        <div class="avatar">
          <img v-if="profile.avatar" :src="profile.avatar" />
          <span v-else>{{ (profile.nickname || '同').slice(0, 1) }}</span>
        </div>
      </div>

      <div class="head">
        <div class="name-row">
          <b>{{ profile.nickname }}</b>
          <span class="lv">{{ levelName }}</span>
        </div>
        <small class="sub">{{ profile.majorName || '未填写专业' }}<template v-if="profile.grade"> · {{ profile.grade }} 级</template></small>

        <div class="credit-bar">
          <span class="cb-label">信用分</span>
          <b class="cb-score">{{ profile.creditScore }}</b>
          <div class="cb-track"><i :style="{ width: creditPercent + '%' }"></i></div>
        </div>
      </div>

      <div class="stats">
        <div class="stat"><b>{{ profile.soldCount }}</b><span>已成交</span></div>
        <i class="line"></i>
        <div class="stat"><b>{{ profile.goodRate }}%</b><span>好评率</span></div>
        <i class="line"></i>
        <div class="stat"><b>{{ profile.onSaleCount }}</b><span>在售</span></div>
        <i class="line"></i>
        <div class="stat"><b>{{ profile.ratingAvg }}</b><span>评分</span></div>
      </div>

      <div class="seg">
        <div class="seg-item" :class="{ on: tab === 'selling' }" @click="tab = 'selling'">在售 {{ books.length }}</div>
        <div class="seg-item" :class="{ on: tab === 'review' }" @click="tab = 'review'">评价 {{ reviews.length }}</div>
      </div>

      <div class="grid" v-if="tab === 'selling'">
        <div class="gcard" v-for="b in books" :key="b.id" @click="$router.push('/book/' + b.id)">
          <div class="gcover">
            <img v-if="b.coverImg" :src="b.coverImg" />
            <span v-else class="gletter">{{ b.title.slice(0, 1) }}</span>
          </div>
          <div class="gtitle">{{ b.title }}</div>
          <div class="gprice">¥{{ b.price }}</div>
        </div>
        <div class="empty" v-if="!books.length">TA 还没有在卖的书</div>
      </div>

      <div class="rlist" v-if="tab === 'review'">
        <div class="rv" v-for="r in reviews" :key="r.id">
          <div class="rv-head">
            <div class="rv-avatar">{{ (r.fromNickname || '同').slice(0, 1) }}</div>
            <span class="rv-name">{{ r.fromNickname || '同学' }}</span>
            <div class="rv-stars">
              <em v-for="n in 5" :key="n" :class="{ on: n <= r.rating }">★</em>
            </div>
          </div>
          <p>{{ r.content || '（没有填写内容）' }}</p>
        </div>
        <div class="empty" v-if="!reviews.length">TA 还没有收到评价</div>
      </div>

      <button class="fab" @click="$router.push({ path: '/chat', query: { peerId: profile.userId, peerName: profile.nickname } })">
        私信 TA
      </button>
    </template>

    <div class="empty" v-else-if="!loading">用户不存在或已注销</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { sellerProfile, publicBooks } from '../api'

const route = useRoute()
const uid = Number(route.params.id)
const profile = ref({})
const books = ref([])
const reviews = ref([])
const loading = ref(true)
const tab = ref('selling')

const levelName = computed(() => {
  const s = profile.value.creditScore == null ? 100 : profile.value.creditScore
  return s >= 95 ? '黄金信用' : s >= 85 ? '白银信用' : s >= 70 ? '青铜信用' : '待提升'
})

const creditPercent = computed(() => {
  const s = profile.value.creditScore == null ? 100 : profile.value.creditScore
  return Math.max(4, Math.min(100, s))
})

onMounted(async () => {
  try {
    profile.value = await sellerProfile(uid)
    reviews.value = profile.value.recentReviews || []
  } catch (e) { profile.value = {} }

  try {
    const d = await publicBooks({ sellerId: uid, size: 12 })
    books.value = d.list || []
  } catch (e) { /* 没有在售也正常 */ }

  loading.value = false
})
</script>

<style scoped>
.user { min-height: 100vh; background: #f7f4ef; padding-bottom: 90px; }
.nav {
  height: 48px; display: flex; align-items: center; justify-content: space-between;
  padding: 0 12px; font-size: 14.5px; font-weight: 600;
  position: absolute; top: 0; left: 0; right: 0; z-index: 5;
}
.back { border: none; background: none; font-size: 24px; width: 32px; text-align: left; line-height: 1; }

.banner {
  position: relative; height: 100px;
  background: linear-gradient(150deg, #dcece2 0%, #eef4ef 60%, #f7f4ef 100%);
}
.avatar {
  position: absolute; left: 16px; bottom: -26px;
  width: 70px; height: 70px; border-radius: 50%;
  border: 3px solid #f7f4ef; background: #fff; overflow: hidden;
  display: grid; place-items: center; font-size: 25px; color: var(--brand);
  box-shadow: 0 4px 12px -6px rgba(27, 31, 35, .28);
}
.avatar img { width: 100%; height: 100%; object-fit: cover; }

.head { padding: 34px 16px 0; }
.name-row { display: flex; align-items: center; gap: 7px; }
.name-row b { font-size: 19px; font-weight: 700; color: #1f262d; }
.lv { font-size: 10px; color: #a8761c; background: #fdf4e0; border-radius: 4px; padding: 2px 7px; }
.sub { display: block; font-size: 11.5px; color: var(--ink-3); margin-top: 5px; }

.credit-bar {
  display: flex; align-items: center; margin-top: 11px;
  background: #fff; border-radius: 9px; padding: 9px 10px;
}
.cb-label { font-size: 11px; color: var(--ink-3); }
.cb-score { font-size: 15px; color: var(--brand); margin-left: 6px; }
.cb-track { flex: 1; height: 4px; border-radius: 2px; background: #f0ebe3; margin-left: 10px; overflow: hidden; }
.cb-track i { display: block; height: 100%; border-radius: 2px; background: linear-gradient(90deg, #7fb79a, #2f7d5b); }

.stats { display: flex; align-items: center; background: #fff; border-radius: 11px; margin: 11px 16px 0; padding: 13px 0; }
.stat { flex: 1; display: flex; flex-direction: column; align-items: center; }
.stat b { font-size: 16px; color: #1f262d; }
.stat span { font-size: 10px; color: var(--ink-3); margin-top: 4px; }
.line { width: 1px; height: 20px; background: #f0ebe3; }

.seg { display: flex; padding: 17px 16px 10px; }
.seg-item { font-size: 13.5px; color: var(--ink-3); margin-right: 20px; padding-bottom: 5px; position: relative; cursor: pointer; }
.seg-item.on { color: #1f262d; font-weight: 650; }
.seg-item.on::after {
  content: ''; position: absolute; left: 50%; bottom: 0; transform: translateX(-50%);
  width: 18px; height: 3px; border-radius: 2px; background: var(--brand);
}

.grid { display: flex; flex-wrap: wrap; gap: 8px; padding: 0 16px; }
.gcard { width: calc(50% - 4px); background: #fff; border-radius: 9px; overflow: hidden; cursor: pointer; }
.gcover { width: 100%; aspect-ratio: 3 / 4; background: #eef2ef; display: grid; place-items: center; }
.gcover img { width: 100%; height: 100%; object-fit: cover; }
.gletter { font-size: 34px; color: rgba(47, 125, 91, .35); font-family: serif; }
.gtitle {
  font-size: 12.5px; color: #1f262d; line-height: 1.4; padding: 7px 8px 0;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.gprice { font-size: 15px; font-weight: 700; color: var(--brand); padding: 5px 8px 9px; }

.rlist { padding: 0 16px; }
.rv { background: #fff; border-radius: 9px; padding: 12px; margin-bottom: 8px; }
.rv-head { display: flex; align-items: center; gap: 7px; margin-bottom: 6px; }
.rv-avatar {
  width: 26px; height: 26px; border-radius: 50%; background: var(--brand-soft);
  color: var(--brand); font-size: 11.5px; display: grid; place-items: center;
}
.rv-name { font-size: 12px; color: var(--ink-2); flex: 1; }
.rv-stars em { font-style: normal; font-size: 10.5px; color: #e6e1d8; margin-left: 1px; }
.rv-stars em.on { color: #e2a33c; }
.rv p { margin: 0; font-size: 13px; color: #3c444d; line-height: 1.65; }

.empty { width: 100%; text-align: center; padding: 35px 0; font-size: 12px; color: #b9b4ab; }

.fab {
  position: fixed; left: 16px; right: 16px; bottom: calc(12px + env(safe-area-inset-bottom));
  height: 44px; border-radius: 22px; border: none;
  background: linear-gradient(135deg, #3f936c, #2f7d5b);
  color: #fff; font-size: 14.5px; font-weight: 600;
  box-shadow: 0 6px 15px -6px rgba(47, 125, 91, .6);
}
</style>
