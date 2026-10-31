<template>
  <div class="credit">
    <header class="nav">
      <button class="back" @click="$router.back()">‹</button>
      <span>信用分</span>
      <div style="width: 32px"></div>
    </header>

    <div class="hero">
      <p class="label">当前信用分</p>
      <div class="score" :style="{ color: credit.color }">{{ score }}</div>
      <span class="level" :style="{ color: credit.color, borderColor: credit.color }">{{ credit.name }}</span>

      <div class="track"><i :style="{ width: credit.percent + '%', background: credit.color }"></i></div>
      <div class="steps"><span>青铜 70</span><span>白银 85</span><span>黄金 95</span></div>
      <p class="next">{{ credit.next }}</p>
    </div>

    <div class="page">
      <div class="card">
        <div class="card-title"><i class="g"></i>怎么提高</div>
        <div class="rule" v-for="r in GAIN" :key="r.title">
          <div><b>{{ r.title }}</b><small>{{ r.desc }}</small></div>
          <em class="plus">{{ r.delta }}</em>
        </div>
      </div>

      <div class="card">
        <div class="card-title"><i class="g"></i>等级特权</div>
        <div class="perk" v-for="p in PERKS" :key="p.level">
          <b>{{ p.level }}</b>
          <div class="perk-items">
            <span v-for="it in p.items" :key="it">· {{ it }}</span>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-title"><i class="r"></i>什么情况会扣分</div>
        <div class="rule" v-for="r in LOSS" :key="r.title">
          <div><b>{{ r.title }}</b><small>{{ r.desc }}</small></div>
          <em class="minus">{{ r.delta }}</em>
        </div>
      </div>

      <p class="footer">信用分 0-100，低于 60 分会被限制发布新书</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMe } from '../api'
import { userStore } from '../store/user'

// 涨分/扣分规则，数值与后端保持一致
const GAIN = [
  { title: '完成一笔交易（作为卖家）', delta: '+2', desc: '按时交付、书况与描述一致' },
  { title: '完成一笔交易（作为买家）', delta: '+1', desc: '如约取书，不无故失联' },
  { title: '收到一条好评', delta: '+1', desc: '评价 4 星及以上才算' },
  { title: '资料填写完整', delta: '+2', desc: '头像、专业、签名都填上（一次性）' }
]
const LOSS = [
  { title: '涉嫌诈骗', delta: '-20', desc: '收款后不发货、货不对板' },
  { title: '内容违规', delta: '-15', desc: '盗版、涉黄涉政等' },
  { title: '疑似虚假信息', delta: '-10', desc: '冒用书号、图文不符' },
  { title: '交易纠纷', delta: '-8', desc: '约好不来、临时加价' },
  { title: '书况与描述不符', delta: '-5', desc: '品相、笔记情况和描述差太多' },
  { title: '价格异常', delta: '-3', desc: '明显偏离市场价' }
]

// 等级特权
const PERKS = [
  { level: '青铜 70', items: ['正常发布与交易', '参与评价与举报'] },
  { level: '白银 85', items: ['搜索排队略微靠前', '专属白银标识'] },
  { level: '黄金 95', items: ['发布免审核，立即上架', '首页优先展示', '专属黄金标识'] }
]

const score = ref(80)

const credit = computed(() => {
  const s = score.value
  if (s >= 95) return { name: '黄金信用', next: '已是最高等级', percent: 100, color: '#d9a441' }
  if (s >= 85) return { name: '白银信用', next: '再得 ' + (95 - s) + ' 分升黄金', percent: Math.round((s - 60) / 35 * 100), color: '#8a9bb0' }
  if (s >= 70) return { name: '青铜信用', next: '再得 ' + (85 - s) + ' 分升白银', percent: Math.round((s - 40) / 45 * 100), color: '#b0805a' }
  return { name: '待提升', next: '完成交易可提升信用', percent: Math.max(5, s), color: '#c0563f' }
})

onMounted(async () => {
  try {
    const u = await getMe()
    score.value = u.creditScore == null ? 80 : u.creditScore
    userStore.update(u)
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.credit { min-height: 100vh; background: #f7f4ef; }
.nav {
  height: 48px; display: flex; align-items: center; justify-content: space-between;
  padding: 0 12px; font-size: 14.5px; font-weight: 600;
  background: rgba(247, 244, 239, .94); backdrop-filter: blur(10px);
}
.back { border: none; background: none; font-size: 24px; width: 32px; text-align: left; line-height: 1; }

.hero {
  padding: 20px 16px 18px; text-align: center;
  background: radial-gradient(240px 130px at 82% -10%, #dcefe4 0%, transparent 65%),
              linear-gradient(180deg, #f2f8f5, #f7f4ef);
}
.label { margin: 0; font-size: 11.5px; color: #8f9a94; }
.score { font-size: 46px; font-weight: 700; line-height: 1.1; margin: 5px 0 4px; letter-spacing: -1px; }
.level { font-size: 10.5px; border: 1px solid currentColor; border-radius: 4px; padding: 1px 6px; }
.track { height: 6px; border-radius: 3px; background: #e8e4dc; overflow: hidden; margin: 14px 0 6px; }
.track i { display: block; height: 100%; border-radius: 3px; transition: width .5s; }
.steps { display: flex; justify-content: space-between; font-size: 9.5px; color: #b9b4ab; }
.next { margin: 7px 0 0; font-size: 10.5px; color: var(--ink-3); }

.note {
  background: var(--brand-soft); border-radius: 12px; padding: 11px 12px;
  font-size: 12px; color: #4a7a63; line-height: 1.7; margin: 0 0 12px;
}
.note b { color: var(--brand); }

.card { background: #fff; border-radius: 14px; padding: 13px 14px; margin-bottom: 12px; }
.card-title { display: flex; align-items: center; font-size: 14px; font-weight: 650; margin-bottom: 10px; }
.card-title i { width: 3px; height: 12px; border-radius: 2px; margin-right: 7px; }
.card-title i.g { background: var(--brand); }
.card-title i.r { background: var(--danger); }

.rule { display: flex; align-items: center; padding: 9px 0; border-bottom: 1px solid #f4f1eb; }
.rule:last-child { border-bottom: none; }
.rule > div { flex: 1; min-width: 0; }
.rule b { display: block; font-size: 12.5px; font-weight: 500; }
.rule small { display: block; font-size: 10.5px; color: var(--ink-3); margin-top: 3px; line-height: 1.5; }
.rule em { font-style: normal; font-size: 14px; font-weight: 700; margin-left: 10px; }
.plus { color: var(--brand); }
.minus { color: var(--danger); }

.perk { display: flex; padding: 9px 0; border-bottom: 1px solid #f2eee8; }
.perk:last-child { border-bottom: none; }
.perk b { width: 70px; flex: none; font-size: 12px; color: var(--brand); }
.perk-items { flex: 1; }
.perk-items span { display: block; font-size: 11.5px; color: var(--ink-2); line-height: 1.85; }
.card-title i.g { background: #c8901f; }

.footer { text-align: center; font-size: 10.5px; color: #b9b4ab; padding: 4px 0 20px; }
</style>