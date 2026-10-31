<template>
  <div class="publish">
    <header class="nav">
      <button class="back" @click="$router.back()">‹</button>
      <span>发布闲置</span>
      <div style="width: 32px"></div>
    </header>

    <div class="page">
      <div class="card">
        <div class="field">
          <label class="required">书名</label>
          <input v-model="form.title" placeholder="例如：数据结构（C语言版）" />
        </div>
        <div class="grid-2">
          <div class="field">
            <label>作者</label>
            <input v-model="form.author" placeholder="严蔚敏" />
          </div>
          <div class="field">
            <label>出版社</label>
            <input v-model="form.publisher" placeholder="清华大学出版社" />
          </div>
        </div>
        <div class="grid-2">
          <div class="field">
            <label class="required">原价</label>
            <input v-model.number="form.originalPrice" type="number" placeholder="45" />
          </div>
          <div class="field">
            <label>出版年份</label>
            <input v-model.number="form.publishYear" type="number" placeholder="2018" />
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-title">书况</div>
        <div class="field">
          <label>品相</label>
          <div class="seg">
            <button v-for="c in conditions" :key="c.v"
                    :class="{ on: form.conditionLevel === c.v }"
                    @click="form.conditionLevel = c.v">{{ c.label }}</button>
          </div>
        </div>
        <div class="field">
          <label>新旧程度</label>
          <div class="seg">
            <button v-for="c in wears" :key="c.v"
                    :class="{ on: form.wearLevel === c.v }"
                    @click="form.wearLevel = c.v">{{ c.label }}</button>
          </div>
        </div>
        <div class="field switch-row">
          <label style="margin: 0">含笔记 / 划线</label>
          <div class="switch" :class="{ on: form.noteFlag === 1 }" @click="form.noteFlag = form.noteFlag ? 0 : 1">
            <i></i>
          </div>
        </div>
        <div class="field">
          <label>实拍图（最多 6 张）</label>
          <div class="photos">
            <div class="photo" v-for="(p, i) in images" :key="p">
              <img :src="p" />
              <span class="del" @click="removeImage(i)">×</span>
            </div>
            <div class="photo-add" v-if="images.length < 6" @click="pick">
              {{ uploading ? '上传中' : '+ 拍照/相册' }}
            </div>
          </div>
          <input ref="fileRef" type="file" accept="image/*" multiple style="display: none" @change="onFiles" />
        </div>

        <div class="field" style="margin-bottom: 0">
          <label>补充描述</label>
          <textarea v-model="form.description" rows="3" placeholder="书页是否完整、有无笔记、交易地点等"></textarea>
        </div>
      </div>

      <!-- 智能定价 -->
      <div class="card price-card">
        <div class="price-head">
          <div class="card-title" style="margin: 0">智能定价</div>
          <button class="ai-btn" :disabled="pricing" @click="askPrice">
            {{ pricing ? '计算中…' : (suggest ? '重新估算' : '获取建议价') }}
          </button>
        </div>

        <div v-if="!suggest" class="price-empty">
          根据原价、品相、新旧和同款在售量，用回归模型给出参考价
        </div>

        <div v-else class="price-result">
          <div class="sug">
            <div class="sug-main">
              <span class="label">建议售价</span>
              <span class="value"><span class="symbol">¥</span>{{ suggest.suggestedPrice }}</span>
            </div>
            <div class="sug-range">区间 ¥{{ suggest.priceLow }} ~ ¥{{ suggest.priceHigh }}</div>
          </div>
          <div class="tips">
            <div v-for="(t, i) in suggest.tips" :key="i" class="tip-line">· {{ t }}</div>
          </div>
          <button class="btn btn-ghost btn-block" @click="form.price = suggest.suggestedPrice">
            采纳建议价 ¥{{ suggest.suggestedPrice }}
          </button>
          <div class="model-line">{{ suggest.model }} · {{ suggest.modelVersion }}</div>
        </div>

        <div class="field" style="margin: 14px 0 0">
          <label class="required">我的定价</label>
          <input v-model.number="form.price" type="number" placeholder="请输入出售价" />
        </div>
      </div>

      <button class="btn btn-primary btn-block submit" :disabled="saving" @click="submit">
        {{ saving ? '发布中…' : '发布' }}
      </button>
      <p class="notice">发布后即出现在学生端，管理员可对违规内容下架</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { suggestPrice, publishBook, uploadImage } from '../api'

const router = useRouter()

const conditions = [
  { v: 1, label: '四成新' }, { v: 2, label: '五成新' }, { v: 3, label: '七成新' },
  { v: 4, label: '九成新' }, { v: 5, label: '全新' }
]
const wears = [
  { v: 1, label: '磨损明显' }, { v: 2, label: '有折痕' }, { v: 3, label: '正常' },
  { v: 4, label: '接近崭新' }, { v: 5, label: '几乎全新' }
]

const form = reactive({
  title: '',
  author: '',
  publisher: '',
  publishYear: 2018,
  originalPrice: null,
  price: null,
  conditionLevel: 3,
  wearLevel: 3,
  noteFlag: 0,
  categoryId: 1,
  description: ''
})

const suggest = ref(null)
const pricing = ref(false)
const saving = ref(false)
const images = ref([])
const uploading = ref(false)
const fileRef = ref(null)

function pick() {
  fileRef.value && fileRef.value.click()
}

async function onFiles(e) {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  uploading.value = true
  try {
    for (const f of files) {
      if (images.value.length >= 6) break
      const up = await uploadImage(f)
      images.value.push(up.url)
    }
  } catch (err) {
    alert(err.message || '上传失败')
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

function removeImage(i) {
  images.value.splice(i, 1)
}

async function askPrice() {
  if (!form.originalPrice) {
    alert('请先填写原价')
    return
  }
  pricing.value = true
  try {
    suggest.value = await suggestPrice({
      originalPrice: form.originalPrice,
      conditionLevel: form.conditionLevel,
      wearLevel: form.wearLevel,
      noteFlag: form.noteFlag,
      publishYear: form.publishYear,
      listingCount: 2,
      soldCount30d: 1,
      sellerHistory: Number(form.originalPrice) * 0.4,
      categoryId: 1
    })
  } catch (e) {
    alert(e.message || '定价服务不可用')
  } finally {
    pricing.value = false
  }
}

async function submit() {
  if (!form.title.trim()) return alert('请填写书名')
  if (!form.originalPrice) return alert('请填写原价')
  if (!form.price) return alert('请填写售价')

  saving.value = true
  try {
    await publishBook({
      ...form,
      coverImg: images.value[0] || '',
      images: JSON.stringify(images.value),
      suggestedPrice: suggest.value ? suggest.value.suggestedPrice : null,
      priceLow: suggest.value ? suggest.value.priceLow : null,
      priceHigh: suggest.value ? suggest.value.priceHigh : null
    })
    alert('发布成功')
    router.push('/me')
  } catch (e) {
    alert(e.message || '发布失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 10;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background: rgba(247, 244, 239, 0.92);
  backdrop-filter: blur(10px);
  font-size: 14.5px;
  font-weight: 600;
}

.back {
  border: none;
  background: none;
  font-size: 24px;
  width: 32px;
  text-align: left;
  line-height: 1;
}

.card {
  margin-bottom: 12px;
}

.card-title {
  font-size: 13.5px;
  font-weight: 650;
  margin-bottom: 12px;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.seg {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.seg button {
  flex: 1;
  min-width: 58px;
  height: 34px;
  border-radius: 9px;
  border: 1px solid var(--line);
  background: #fff;
  font-size: 12.5px;
  color: var(--ink-2);
}

.seg button.on {
  border-color: var(--brand);
  background: var(--brand-soft);
  color: var(--brand);
  font-weight: 600;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.switch {
  width: 46px;
  height: 26px;
  border-radius: 13px;
  background: #e6e1d8;
  position: relative;
  transition: background 0.18s;
}

.switch.on {
  background: var(--brand);
}

.switch i {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.18s;
}

.switch.on i {
  transform: translateX(20px);
}

.price-card {
  background: linear-gradient(160deg, #f2f8f4, #ffffff 60%);
  border: 1px solid #dcebe2;
}

.price-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ai-btn {
  border: none;
  background: var(--brand);
  color: #fff;
  font-size: 12px;
  padding: 6px 12px;
  border-radius: 14px;
  font-weight: 600;
}

.price-empty {
  margin-top: 10px;
  font-size: 12px;
  color: var(--ink-3);
  line-height: 1.6;
}

.price-result {
  margin-top: 12px;
}

.sug {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
}

.sug-main {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.sug-main .label {
  font-size: 12.5px;
  color: var(--ink-2);
}

.sug-main .value {
  font-size: 26px;
  font-weight: 700;
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}

.sug-main .symbol {
  font-size: 14px;
}

.sug-range {
  font-size: 12px;
  color: var(--ink-3);
}

.tips {
  margin: 10px 0 12px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid var(--line);
}

.tip-line {
  font-size: 12px;
  color: var(--ink-2);
  line-height: 1.75;
}

.model-line {
  margin-top: 8px;
  text-align: center;
  font-size: 10.5px;
  color: var(--ink-3);
}

.submit {
  margin-top: 6px;
}

.photos {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.photo {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
}

.photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.photo .del {
  position: absolute;
  top: 0;
  right: 0;
  width: 18px;
  height: 18px;
  line-height: 16px;
  text-align: center;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 13px;
  border-bottom-left-radius: 6px;
  cursor: pointer;
}

.photo-add {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  border: 1px dashed #d6d0c6;
  background: #fff;
  display: grid;
  place-items: center;
  font-size: 11px;
  color: #b9b4ab;
  text-align: center;
  line-height: 1.3;
  cursor: pointer;
}

.notice {
  text-align: center;
  font-size: 11px;
  color: var(--ink-3);
  margin: 10px 0 0;
}
</style>