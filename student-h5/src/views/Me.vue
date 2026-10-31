<template>
  <div class="me">
    <div class="topbar">
      <h1>我的</h1>
      <div class="top-actions" v-if="user">
        <span class="bell" @click="go('/notice')">
          🔔
          <em v-if="noticeUnread > 0" class="bell-badge">{{ noticeUnread > 99 ? '99+' : noticeUnread }}</em>
        </span>
        <span class="gear" @click="go('/profile')">⚙</span>
      </div>
    </div>

    <div class="user-card" v-if="user">
      <div class="avatar" @click="go('/profile')">
        <img v-if="user.avatar" :src="user.avatar" />
        <span v-else>{{ (user.nickname || '书').slice(0, 1) }}</span>
      </div>
      <div class="u-main">
        <div class="u-row">
          <b>{{ user.nickname }}</b>
          <em :style="{ color: credit.color, borderColor: credit.color }">{{ credit.name }}</em>
        </div>
        <p>{{ user.majorName || '未填写专业' }} · {{ user.grade || '—' }} 级</p>
        <p class="u-sign">{{ user.signature || '还没有写个性签名' }}</p>
      </div>
    </div>

    <div class="user-card" v-else>
      <div class="avatar"><span>游</span></div>
      <div class="u-main">
        <div class="u-row"><b>未登录</b></div>
        <p>登录后可以发布、收藏和下单</p>
        <button class="login-btn" @click="go('/login')">立即登录</button>
      </div>
    </div>

    <div class="credit-card" :class="credit.theme" v-if="user" @click="go('/credit')">
      <!-- 等级光环：分数越高范围越大 -->
      <div class="halo" :style="{ background: 'radial-gradient(circle, ' + credit.color + '33 0%, transparent 68%)' }"></div>

      <div class="c-head">
        <span><i class="c-gem" :style="{ background: credit.color }"></i>信用分 <b :style="{ color: credit.color }">{{ user.creditScore }}</b></span>
        <small>{{ credit.next }} ›</small>
      </div>
      <div class="c-track"><i :style="{ width: credit.percent + '%', background: credit.color }"></i></div>
      <div class="c-steps">
        <span :class="{ on: credit.name === '青铜信用' }">青铜</span>
        <span :class="{ on: credit.name === '白银信用' }">白银</span>
        <span :class="{ on: credit.name === '黄金信用' }">黄金</span>
      </div>

      <!-- 特权清单：亮的已解锁 -->
      <div class="perks">
        <span class="perk" :class="{ on: score >= 90 }">免审核上架</span>
        <span class="perk" :class="{ on: score >= 90 }">首页优先</span>
        <span class="perk" :class="{ on: score >= 95 }">专属标识</span>
      </div>
    </div>

    <div class="page" style="padding-top: 12px">
      <div class="stats" v-if="user">
        <div class="stat"><b>{{ stats.selling }}</b><span>在售</span></div>
        <i></i>
        <div class="stat"><b>{{ stats.sold }}</b><span>已售</span></div>
        <i></i>
        <div class="stat"><b>{{ stats.fav }}</b><span>收藏</span></div>
        <i></i>
        <div class="stat">
          <b>{{ stats.views || 0 }}</b><span>被浏览</span>
        </div>
      </div>

      <div class="quick" v-if="user">
        <div class="q" @click="go('/chat')"><span>💬<em v-if="unread > 0" class="q-dot">{{ unread }}</em></span><small>消息</small></div>
        <div class="q" @click="go('/credit')"><span>🏅</span><small>信用规则</small></div>
        <div class="q" @click="go('/profile')"><span>⚙</span><small>资料</small></div>
      </div>

      <div class="tabs" v-if="user">
        <div class="tab" :class="{ on: tab === 'selling' }" @click="setTab('selling')">我的发布</div>
        <div class="tab" :class="{ on: tab === 'favorites' }" @click="setTab('favorites')">我的收藏</div>
        <div class="tab" :class="{ on: tab === 'history' }" @click="setTab('history')">浏览历史</div>
      </div>

      <template v-if="user">
        <div v-if="loading" class="loading">加载中…</div>
        <template v-else>
          <book-card v-for="b in list" :key="b.id" :book="b" @click="openBook(b)" />
          <div v-if="!list.length" class="empty">
            <div class="icon">{{ tab === 'selling' ? '📚' : '♡' }}</div>
            {{ tab === 'selling' ? '还没有发布过闲置' : tab === 'history' ? '还没有浏览记录' : '还没有收藏的书' }}
          </div>
        </template>
      </template>
      <div v-else class="empty">登录后查看你的闲置和收藏</div>

      <button class="logout" v-if="user" @click="logout">退出登录</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import BookCard from '../components/BookCard.vue'
import { myBooks, myFavorites, getMe, myStats, browseHistory,
         unreadCount as chatUnread, noticeUnread as noticeUnreadApi } from '../api'
import { userStore } from '../store/user'

const router = useRouter()
const tab = ref('selling')
const list = ref([])
const loading = ref(false)
const stats = ref({ selling: 0, sold: 0, fav: 0 })
// 聊天未读（数据条那一栏）与通知未读（铃铛角标）是两个数，别混用
const unread = ref(0)
const noticeUnread = ref(0)

const user = computed(() => userStore.user)

// 信用分映射等级，比单纯数字更直观
// 每档配主题、图标与特权说明
const score = computed(() => (user.value && user.value.creditScore != null ? user.value.creditScore : 100))

const credit = computed(() => {
  const s = user.value && user.value.creditScore != null ? user.value.creditScore : 100
  if (s >= 95) return { name: '黄金信用', icon: '👑', theme: 'tier-gold', next: '已满级，免审核直接上架', percent: 100, color: '#c8901f' }
  if (s >= 85) return { name: '白银信用', icon: '🛡', theme: 'tier-silver', next: '再得 ' + (95 - s) + ' 分升黄金，解锁免审核', percent: Math.round((s - 60) / 35 * 100), color: '#7c8da3' }
  if (s >= 70) return { name: '青铜信用', icon: '🌱', theme: 'tier-bronze', next: '再得 ' + (85 - s) + ' 分升白银', percent: Math.round((s - 40) / 45 * 100), color: '#b0805a' }
  return { name: '待提升', icon: '⚠️', theme: 'tier-low', next: '完成交易、保持好评可提升信用', percent: Math.max(5, s), color: '#c0563f' }
})

const go = (p) => router.push(p)
const openBook = (b) => router.push('/book/' + b.id)

async function loadTab() {
  if (!userStore.isLogin) { list.value = []; return }
  loading.value = true
  try {
    if (tab.value === 'selling') list.value = await myBooks('selling')
    else if (tab.value === 'history') list.value = await browseHistory(12)
    else list.value = await myFavorites()
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function setTab(t) {
  tab.value = t
  loadTab()
}

function logout() {
  userStore.logout()
  list.value = []
  stats.value = { selling: 0, sold: 0, fav: 0 }
  unread.value = 0
  router.push('/')
}

watch(() => userStore.token, () => loadTab())

onMounted(async () => {
  if (!userStore.isLogin) return
  try { userStore.update(await getMe()) } catch (e) { /* ignore */ }
  try { stats.value = await myStats() } catch (e) { /* ignore */ }
  try { unread.value = (await chatUnread()).count || 0 } catch (e) { /* ignore */ }
  try { noticeUnread.value = (await noticeUnreadApi()).count || 0 } catch (e) { /* ignore */ }
  await loadTab()
})
</script>

<style scoped>
.me {
  background:
    radial-gradient(260px 150px at 88% -6%, #dcefe4 0%, transparent 62%),
    linear-gradient(180deg, #f2f8f5 0%, #f7f4ef 160px);
  min-height: 100%;
}
.topbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px 4px;
}
.topbar h1 { margin: 0; font-size: 19px; font-weight: 700; letter-spacing: 0.5px; }
.top-actions { display: flex; align-items: center; gap: 8px; }
.bell {
  position: relative; width: 32px; height: 32px; border-radius: 50%;
  background: rgba(255,255,255,.8); display: grid; place-items: center;
  font-size: 14px; cursor: pointer;
}
.bell-badge {
  position: absolute; top: -3px; right: -3px; min-width: 15px; height: 15px;
  border-radius: 8px; background: var(--danger); color: #fff;
  font-size: 9px; font-style: normal; line-height: 15px; text-align: center; padding: 0 3px;
}
.gear {
  width: 32px; height: 32px; border-radius: 50%; background: rgba(255,255,255,.8);
  color: var(--ink-2); display: grid; place-items: center; font-size: 15px; cursor: pointer;
}

.user-card { display: flex; align-items: center; gap: 12px; padding: 12px 16px 4px; }
.avatar {
  width: 68px; height: 68px; border-radius: 50%; overflow: hidden; flex: none;
  background: #e3f0e8; border: 2px solid #fff;
  box-shadow: 0 14px 34px -18px rgba(47, 125, 91, 0.8);
  display: grid; place-items: center;
}
.avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }
.avatar span { font-size: 26px; color: var(--brand); font-weight: 600; }
.u-main { flex: 1; min-width: 0; }
.u-row { display: flex; align-items: center; gap: 7px; }
.u-row b { font-size: 18px; font-weight: 700; }
.u-row em {
  font-style: normal; font-size: 9.5px; border: 1px solid currentColor;
  border-radius: 4px; padding: 1px 5px;
}
.u-main p { margin: 4px 0 0; font-size: 11.5px; color: #8f9a94; }
.u-main .u-sign {
  color: #6b756f; line-height: 1.5;
  overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}
.login-btn {
  margin-top: 7px; height: 30px; padding: 0 17px; border-radius: 15px;
  border: none; background: var(--brand); color: #fff; font-size: 12.5px;
}

.credit-card { position: relative; overflow: hidden; }
.halo {
  position: absolute; top: -40px; right: -30px; width: 160px; height: 160px;
  pointer-events: none;
}
/* 等级宝石：纯 CSS 菱形，替代 emoji */
.c-gem {
  display: inline-block; width: 8px; height: 8px; border-radius: 1.5px;
  transform: rotate(45deg); margin-right: 7px;
}

.perks { display: flex; gap: 6px; margin-top: 8px; flex-wrap: wrap; }
.perk {
  font-size: 9.5px; color: #b9b4ab; background: #f4f1ec;
  border-radius: 9px; padding: 3px 8px;
}
.perk.on { color: var(--brand); background: var(--brand-soft); }

/* 香槟金：静态渐变，克制不抢眼 */
.tier-gold {
  background:
    radial-gradient(120% 90% at 100% 0%, rgba(255, 236, 190, .55) 0%, transparent 58%),
    linear-gradient(160deg, #fffdf7 0%, #fdf6e3 46%, #fffefb 100%) !important;
  border: 1px solid #ecd9a8 !important;
  box-shadow: 0 10px 30px -26px rgba(178, 126, 32, .7);
}
.tier-gold::after {
  content: '';
  position: absolute; left: 0; right: 0; top: 0; height: 1px;
  background: linear-gradient(90deg, transparent, rgba(226, 183, 90, .9), transparent);
}
.tier-silver {
  background: linear-gradient(135deg, #fbfcfe 0%, #eef2f7 60%, #ffffff 100%) !important;
  border: 1px solid #e2e8f0 !important;
}
.tier-bronze {
  background: linear-gradient(135deg, #fffaf5 0%, #f7ece2 60%, #ffffff 100%) !important;
  border: 1px solid #eee0d2 !important;
}
.tier-low {
  background: linear-gradient(135deg, #fff8f7 0%, #fdeceb 60%, #ffffff 100%) !important;
  border: 1px solid #f2d3ce !important;
}

.credit-card {
  cursor: pointer;
  margin: 10px 16px 0; background: #fff; border-radius: 14px; padding: 13px 14px 11px;
  box-shadow: 0 10px 28px -24px rgba(27, 31, 35, 0.6);
}
.c-head { display: flex; align-items: center; justify-content: space-between; }
.c-left { display: flex; align-items: baseline; }
.c-label { font-size: 12px; color: #8f9a94; }
.c-left b { font-size: 27px; font-weight: 700; margin-left: 5px; line-height: 1; }
.c-level {
  font-style: normal; font-size: 10.5px; border: 1px solid currentColor;
  border-radius: 4px; padding: 1px 6px;
}
.c-track { height: 6px; border-radius: 3px; background: #f0ebe3; overflow: hidden; margin: 11px 0 6px; }
.c-track i { display: block; height: 100%; border-radius: 3px; transition: width .4s; }
.c-steps { display: flex; justify-content: space-between; }
.c-steps span { font-size: 10px; color: #b9b4ab; }
.c-steps span.on { color: var(--brand); font-weight: 600; }
.c-foot {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 10px; padding-top: 9px; border-top: 1px solid #f4f1eb;
}
.c-foot small { font-size: 10.5px; color: var(--ink-3); }
.c-foot strong { font-size: 11px; color: var(--brand); font-weight: 600; }

.stats {
  display: flex; align-items: center; background: #fff; border-radius: 14px;
  padding: 13px 0; margin-bottom: 10px;
  box-shadow: 0 8px 24px -20px rgba(27, 31, 35, 0.5);
}
.stat { flex: 1; display: flex; flex-direction: column; align-items: center; }
.stats i { width: 1px; height: 22px; background: #f0ebe3; }
.stat b { font-size: 19px; font-weight: 700; position: relative; }
.stat span { font-size: 11px; color: var(--ink-3); margin-top: 2px; }
.dot {
  position: absolute; top: 0; right: -6px; width: 6px; height: 6px;
  border-radius: 50%; background: var(--danger);
}

.quick { display: flex; background: #fff; border-radius: 14px; padding: 12px 0; margin-bottom: 12px; }
.q { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; cursor: pointer; }
.q span { font-size: 17px; position: relative; }
.q-dot {
  position: absolute; top: -4px; right: -10px; min-width: 14px; height: 14px;
  border-radius: 7px; background: var(--danger); color: #fff;
  font-size: 9px; font-style: normal; line-height: 14px; text-align: center; padding: 0 3px;
}
.q small { font-size: 11px; color: var(--ink-2); }

.tabs { display: flex; gap: 18px; margin-bottom: 10px; }
.tab { font-size: 14px; color: var(--ink-3); padding-bottom: 5px; position: relative; cursor: pointer; }
.tab.on { color: var(--ink); font-weight: 650; }
.tab.on::after {
  content: ''; position: absolute; left: 50%; bottom: 0; transform: translateX(-50%);
  width: 20px; height: 3px; border-radius: 2px; background: var(--brand);
}

.logout {
  margin-top: 16px; width: 100%; height: 44px; border-radius: 14px;
  border: none; background: #fff; color: var(--danger); font-size: 14px;
}
</style>