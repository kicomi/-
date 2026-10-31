<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>举报处理</h2>
        <p>买卖纠纷与违规内容，通过后会自动下架被举报的书籍</p>
      </div>
      <div class="filter-bar">
        <el-radio-group v-model="status" @change="reload">
          <el-radio-button :value="0">待处理</el-radio-button>
          <el-radio-button :value="1">已处理</el-radio-button>
          <el-radio-button :value="2">已驳回</el-radio-button>
          <el-radio-button :value="''">全部</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="card">
      <div class="card-head">
        <span class="chip" v-if="pending > 0">待处理 {{ pending }} 条</span>
        <span class="chip ghost" v-else>暂无待处理</span>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="64" />
        <el-table-column prop="reporterNickname" label="举报人" width="120" />
        <el-table-column label="举报对象" min-width="210">
          <template #default="{ row }">
            <el-tag size="small" :type="row.targetType === 'book' ? 'success' : 'warning'" effect="plain">
              {{ row.targetType === 'book' ? '书籍' : '用户' }}
            </el-tag>
            <span style="margin-left: 6px">{{ row.targetTitle }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" width="140" />
        <el-table-column label="补充说明" min-width="200">
          <template #default="{ row }">
            <span class="muted">{{ row.detail || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 0 ? 'warning' : row.status === 1 ? 'success' : 'info'">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">查看详情</el-button>
            <template v-if="row.status === 0">
              <el-button size="small" type="success" @click="handle(row, 1)">通过</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total"
                       layout="total, prev, pager, next" @current-change="reload" />
      </div>
    </div>

    <!-- 举报详情抽屉 -->
    <el-drawer v-model="drawerOpen" size="720px" :title="'举报 #' + (detail.report ? detail.report.id : '')">
      <div v-loading="detailLoading" class="detail-wrap" v-if="detail.report">
        <!-- 左侧：被举报的书 -->
        <div class="d-col">
          <div class="d-block">
            <div class="d-title">被举报的{{ detail.report.targetType === 'book' ? '书籍' : '用户' }}</div>
            <div class="book-card" v-if="detail.book">
              <div class="book-cover">
                <img v-if="detail.book.coverImg" :src="detail.book.coverImg" />
                <span v-else>{{ (detail.book.title || '书').slice(0, 1) }}</span>
              </div>
              <div class="book-info">
                <b>{{ detail.book.title }}</b>
                <p>{{ detail.book.author }}</p>
                <div class="book-price">
                  <em>¥{{ detail.book.price }}</em>
                  <s>原价 ¥{{ detail.book.originalPrice }}</s>
                </div>
                <div class="book-tags">
                  <el-tag size="small" type="success" effect="plain">{{ conditionText(detail.book.conditionLevel) }}</el-tag>
                  <el-tag size="small" v-if="detail.book.noteFlag === 1" effect="plain">含笔记</el-tag>
                  <el-tag size="small" :type="detail.book.status === 1 ? 'success' : 'info'" effect="plain">
                    {{ bookStatusText(detail.book.status) }}
                  </el-tag>
                </div>
              </div>
            </div>
            <p class="book-desc">{{ detail.book ? (detail.book.description || '卖家没有填写描述') : '书籍已删除' }}</p>
          </div>

          <div class="d-block" v-if="detail.seller">
            <div class="d-title">卖家</div>
            <div class="kv">
              <span>昵称</span><b>{{ detail.seller.nickname }}</b>
            </div>
            <div class="kv"><span>信用分</span><b>{{ detail.seller.creditScore }}</b></div>
            <div class="kv"><span>历史成交</span><b>{{ detail.seller.soldCount }} 笔</b></div>
            <div class="kv">
              <span>被举报成立</span>
              <b :class="{ danger: detail.seller.reportedCount > 0 }">{{ detail.seller.reportedCount }} 次</b>
            </div>
          </div>

          <div class="d-block" v-if="detail.reviews && detail.reviews.length">
            <div class="d-title">这本书的评价</div>
            <div class="review" v-for="(rv, i) in detail.reviews" :key="i">
              <span class="star">{{ rv.rating }} 星</span>
              <p>{{ rv.content || '无评价内容' }}</p>
            </div>
          </div>
        </div>

        <!-- 右侧：举报信息与核实 -->
        <div class="d-col">
          <div class="d-block">
            <div class="d-title">举报内容</div>
            <div class="kv"><span>举报人</span>
              <b>{{ detail.report.reporter.nickname }}
                <em class="sub">信用 {{ detail.report.reporter.creditScore }}</em>
              </b>
            </div>
            <div class="kv"><span>举报原因</span><b>{{ detail.report.reason }}</b></div>
            <div class="kv"><span>提交时间</span><b>{{ fmt(detail.report.createdAt) }}</b></div>
            <p class="d-detail">{{ detail.report.detail || '没有填写补充说明' }}</p>
          </div>

          <div class="d-block">
            <div class="d-title">核实依据</div>
            <ul class="evidence">
              <li :class="detail.evidence && detail.evidence.hasOrder ? 'ok' : 'warn'">
                <span class="ico">{{ detail.evidence && detail.evidence.hasOrder ? '✓' : '!' }}</span>
                {{ detail.evidence && detail.evidence.hasOrder
                   ? '举报人购买过这本书（订单已' + orderStatusText(detail.evidence.orderStatus) + '）'
                   : '举报人没有购买过这本书' }}
              </li>
              <li :class="detail.evidence && detail.evidence.chatted ? 'ok' : 'warn'">
                <span class="ico">{{ detail.evidence && detail.evidence.chatted ? '✓' : '!' }}</span>
                {{ detail.evidence && detail.evidence.chatted ? '双方有过聊天记录' : '双方没有聊天记录' }}
              </li>
              <li :class="detail.report.reporter.reportCount - detail.report.reporter.validCount >= 2 ? 'warn' : 'ok'">
                <span class="ico">·</span>
                该用户历史举报 {{ detail.report.reporter.reportCount }} 次，成立 {{ detail.report.reporter.validCount }} 次
              </li>
            </ul>
          </div>

          <div class="d-block" v-if="detail.report.status === 0">
            <div class="d-title">处理</div>
            <el-input v-model="note" type="textarea" :rows="3" placeholder="处理说明（会留给举报人看）" />
            <div class="d-actions">
              <el-button @click="doHandle(2)">驳回</el-button>
              <el-button type="success" @click="doHandle(1)">通过并下架</el-button>
            </div>
          </div>
          <div class="d-block" v-else>
            <div class="d-title">处理结果</div>
            <p class="d-detail">{{ detail.report.handleNote || '已处理' }}</p>
            <p class="muted">处理时间：{{ fmt(detail.report.handledAt) }}</p>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="dialogOpen" title="处理举报" width="440px">
      <p class="dialog-tip">
        {{ action === 1
           ? '通过后将下架该书，并扣卖家信用分 ' + penaltyOf(current.reason) + ' 分（按原因分级）'
           : '驳回后举报记录仍会保留' }}
      </p>
      <el-input v-model="note" type="textarea" :rows="3" placeholder="处理说明（选填）" />
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button :type="action === 1 ? 'success' : 'primary'" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { reportList, reportHandle, reportPending, reportDetail } from '../api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref(0)
const loading = ref(false)
const pending = ref(0)

const drawerOpen = ref(false)
const detailLoading = ref(false)
const detail = ref({})

const COND = { 1: '四成新', 2: '五成新', 3: '七成新', 4: '九成新', 5: '全新' }
const BOOK_STATUS = { 0: '待审核', 1: '在售中', 2: '已售出', 3: '已下架', 4: '已驳回' }
const ORDER_STATUS = { 0: '待确认', 1: '已确认', 2: '已完成', 3: '已取消' }

function conditionText(l) { return COND[l] || '七成新' }
function bookStatusText(s) { return BOOK_STATUS[s] || '未知' }
function orderStatusText(s) { return ORDER_STATUS[s] || '未知' }

async function openDetail(row) {
  drawerOpen.value = true
  detailLoading.value = true
  note.value = ''
  try {
    detail.value = await reportDetail(row.id)
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function doHandle(status) {
  try {
    await reportHandle(detail.value.report.id, { status, note: note.value })
    ElMessage.success('已处理')
    drawerOpen.value = false
    load()
    loadPending()
  } catch (e) {
    ElMessage.error(e.message || '处理失败')
  }
}

const dialogOpen = ref(false)
const action = ref(1)
const note = ref('')
const current = ref(null)

// 与后端 ReportService.PENALTY_BY_REASON 保持一致
const PENALTY = {
  '涉嫌诈骗': 20, '内容违规': 15, '疑似虚假信息': 10,
  '交易纠纷': 8, '书况与描述不符': 5, '价格异常': 3
}
function penaltyOf(reason) {
  return PENALTY[reason] || 5
}

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '—'
}

async function load() {
  loading.value = true
  try {
    const data = await reportList({
      status: status.value === '' ? undefined : status.value,
      page: page.value,
      size: size.value
    })
    rows.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadPending() {
  try {
    const d = await reportPending()
    pending.value = d.count || 0
  } catch (e) { /* ignore */ }
}

function reload() {
  page.value = 1
  load()
}

function handle(row, act) {
  current.value = row
  action.value = act
  note.value = ''
  dialogOpen.value = true
}

async function submit() {
  try {
    await reportHandle(current.value.id, { status: action.value, note: note.value })
    ElMessage.success('已处理')
    dialogOpen.value = false
    load()
    loadPending()
  } catch (e) {
    ElMessage.error(e.message || '处理失败')
  }
}

onMounted(() => {
  load()
  loadPending()
})
</script>

<style scoped>
.detail-wrap { display: flex; gap: 18px; }
.d-col { flex: 1; min-width: 0; }
.d-block { margin-bottom: 18px; }
.d-title { font-size: 13px; font-weight: 650; color: #334155; margin-bottom: 10px; }

.book-card { display: flex; gap: 12px; }
.book-cover {
  width: 78px; height: 104px; border-radius: 8px; overflow: hidden; flex: none;
  background: #eef2f0; display: grid; place-items: center;
  color: #8aa79a; font-size: 24px; font-family: serif;
}
.book-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.book-info b { font-size: 14px; color: #1e293b; }
.book-info p { margin: 3px 0 6px; font-size: 12px; color: #94a3b8; }
.book-price { display: flex; align-items: baseline; gap: 6px; }
.book-price em { font-style: normal; font-size: 17px; font-weight: 700; color: #0f766e; }
.book-price s { font-size: 11.5px; color: #cbd5e1; }
.book-tags { display: flex; gap: 5px; margin-top: 6px; flex-wrap: wrap; }
.book-desc {
  margin: 10px 0 0; font-size: 12.5px; color: #475569; line-height: 1.65;
  background: #f8fafc; border-radius: 8px; padding: 10px;
}

.kv { display: flex; justify-content: space-between; font-size: 12.5px; padding: 5px 0; }
.kv span { color: #94a3b8; }
.kv b { color: #1e293b; font-weight: 600; }
.kv .sub { font-style: normal; font-weight: 400; color: #94a3b8; margin-left: 4px; }
.kv .danger { color: #dc2626; }

.d-detail {
  margin: 8px 0 0; font-size: 12.5px; color: #475569; line-height: 1.7;
  background: #f8fafc; border-radius: 8px; padding: 10px;
}

.evidence { list-style: none; padding: 0; margin: 0; }
.evidence li {
  display: flex; align-items: flex-start; gap: 7px;
  font-size: 12.5px; line-height: 1.6; padding: 7px 10px; border-radius: 8px; margin-bottom: 6px;
}
.evidence li.ok { background: #f0fdf4; color: #15803d; }
.evidence li.warn { background: #fef9ee; color: #b45309; }
.evidence .ico { font-weight: 700; }

.review { background: #f8fafc; border-radius: 8px; padding: 8px 10px; margin-bottom: 6px; }
.review .star { font-size: 11.5px; color: #d97706; }
.review p { margin: 3px 0 0; font-size: 12px; color: #475569; }

.d-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 10px; }
.muted { color: #94a3b8; font-size: 12px; }

.card-head { margin-bottom: 12px; }
.chip {
  display: inline-block;
  font-size: 12.5px;
  padding: 3px 12px;
  border-radius: 12px;
  background: #fef3e2;
  color: #b45309;
}
.chip.ghost { background: #f1f5f9; color: #64748b; }
.muted { color: #94a3b8; font-size: 12.5px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.dialog-tip { margin: 0 0 12px; font-size: 12.5px; color: #64748b; line-height: 1.6; }
</style>