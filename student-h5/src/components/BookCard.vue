<template>
  <div class="bc" :class="{ 'is-priority': book.priority === 1 }" @click="$emit('click')">
    <div class="cover">
      <span v-if="!book.coverImg" class="cover-text">{{ firstChar }}</span>
      <img v-if="book.coverImg" :src="book.coverImg" class="cover-img" :class="{ loaded }" @load="loaded = true" />
    </div>

    <div class="info">
      <div class="title">{{ book.title }}</div>
      <div class="meta">{{ metaLine }}</div>

      <div class="price-row">
        <span class="p-cur"><span class="p-sym">¥</span>{{ book.price }}</span>
        <span class="p-old">¥{{ book.originalPrice }}</span>
        <span class="p-off" v-if="off > 0">{{ off }}%↓</span>
      </div>

      <div class="foot">
        <div class="seller">
          {{ book.sellerNickname || '同学' }}<span class="s-dot">·</span>{{ book.majorName || '本校' }}
        </div>
        <span class="match" v-if="book.matchScore">匹配 {{ book.matchScore }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  book: { type: Object, required: true }
})
defineEmits(['click'])

const loaded = ref(false)

const firstChar = computed(() => (props.book.title || '书').slice(0, 1))

const CONDITION = { 1: '一成新', 2: '三成新', 3: '五成新', 4: '七成新', 5: '九成新' }

// 课程 / 品相 / 笔记 合成一行，避免一排小标签
const metaLine = computed(() => {
  const b = props.book
  const bits = []
  if (b.courseName) bits.push(b.courseName)
  if (b.conditionLevel) bits.push(CONDITION[b.conditionLevel] || '')
  if (b.noteFlag === 1) bits.push('含笔记')
  return bits.filter(Boolean).join(' · ')
})

const off = computed(() => {
  const o = Number(props.book.originalPrice) || 0
  const p = Number(props.book.price) || 0
  return o > 0 && p > 0 && p < o ? Math.round((1 - p / o) * 100) : 0
})
</script>

<style scoped>
.bc {
  display: flex;
  background: #fff;
  border-radius: 10px;
  padding: 10px;
  margin-bottom: 8px;
  position: relative;
  transition: transform .15s ease;
}
.bc:active { transform: scale(.985); }

/* 高信用卖家：左侧细金线，不用文字标签 */
.bc.is-priority::before {
  content: '';
  position: absolute; left: 0; top: 13px; bottom: 13px; width: 2px;
  border-radius: 0 2px 2px 0;
  background: linear-gradient(180deg, #e8c67a, #c8901f);
}

.cover {
  width: 80px; height: 107px; border-radius: 6px; flex: none; margin-right: 11px;
  background: linear-gradient(150deg, #eef2ef, #e2e9e4);
  display: grid; place-items: center; overflow: hidden; position: relative;
}
.cover-img { width: 100%; height: 100%; object-fit: cover; opacity: 0; transition: opacity .3s ease; }
.cover-img.loaded { opacity: 1; }
.cover-text { font-size: 28px; color: rgba(47, 125, 91, .38); font-family: serif; }

.info { flex: 1; min-width: 0; display: flex; flex-direction: column; padding: 1px 0; }
.title {
  font-size: 14.5px; font-weight: 600; color: #1f262d; line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.meta {
  margin-top: 5px; font-size: 11px; color: #9aa3ad;
  overflow: hidden; white-space: nowrap; text-overflow: ellipsis;
}

.price-row { margin-top: auto; display: flex; align-items: baseline; }
.p-cur { font-size: 18px; font-weight: 700; color: var(--brand); line-height: 1; }
.p-sym { font-size: 12px; margin-right: 1px; }
.p-old { font-size: 11px; color: #b8b2a8; text-decoration: line-through; margin-left: 6px; }
.p-off { font-size: 10px; color: var(--danger); margin-left: 5px; }

.foot { display: flex; align-items: center; justify-content: space-between; margin-top: 5px; }
.seller {
  font-size: 10.5px; color: #a8a299; flex: 1; min-width: 0;
  overflow: hidden; white-space: nowrap; text-overflow: ellipsis;
}
.s-dot { margin: 0 4px; }
.match {
  font-size: 10px; color: var(--brand); background: var(--brand-soft);
  border-radius: 3px; padding: 1.5px 5px; flex: none; margin-left: 6px;
}
</style>
