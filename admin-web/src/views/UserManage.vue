<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>用户管理</h2>
        <p>共 {{ total }} 位注册用户。封禁立即生效，禁言在小程序聊天上线后生效</p>
      </div>
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="昵称 / 学号 / 手机号 / 用户ID" clearable
                  style="width: 240px" :prefix-icon="Search" @keyup.enter="reload" />
        <el-button type="primary" :icon="Search" @click="reload">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="success" plain :loading="exporting" @click="doExport">导出 Excel</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">用户列表</div>
      <el-table :data="rows" v-loading="loading" stripe size="small">
        <el-table-column prop="id" label="ID" width="86" sortable="custom" :sort-orders="['ascending','descending']"
                         @sort-change="onSortChange" />
        <el-table-column prop="nickname" label="昵称" width="112" />
        <el-table-column prop="studentNo" label="学号" width="126" />
        <el-table-column prop="majorName" label="专业" min-width="160" />
        <el-table-column prop="grade" label="年级" width="72" />
        <el-table-column label="信用分" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.creditScore >= 90 ? 'success' : 'warning'" effect="light">
              {{ row.creditScore }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" min-width="170">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" size="small" type="success" effect="light">正常</el-tag>
            <el-tag v-else size="small" type="danger" effect="light">
              {{ row.banUntil ? '封禁至 ' + fmtDate(row.banUntil) : '永久封禁' }}
            </el-tag>
            <el-tag v-if="row.muteStatus === 1" size="small" type="warning" effect="light" style="margin-left: 6px">
              {{ row.muteUntil ? '禁言至 ' + fmtDate(row.muteUntil) : '永久禁言' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="390" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
            <el-button type="primary" link size="small" @click="openCredit(row)">信用分</el-button>
            <el-button type="primary" link size="small" @click="openRename(row)">重置昵称</el-button>
            <el-button v-if="row.status === 1" type="danger" link size="small"
                       @click="openAction(row, 'ban')">封禁</el-button>
            <el-button v-else type="success" link size="small" @click="unban(row)">解除封禁</el-button>

            <el-button v-if="row.muteStatus !== 1" type="warning" link size="small"
                       @click="openAction(row, 'mute')">禁言</el-button>
            <el-button v-else type="success" link size="small" @click="unmute(row)">解除禁言</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination style="margin-top: 16px; justify-content: flex-end"
                     layout="total, prev, pager, next"
                     :total="total" :page-size="query.size" :current-page="query.page"
                     @current-change="onPageChange" />
    </div>

    <el-drawer v-model="detailOpen" size="760px" :title="'用户详情 #' + (detail.user ? detail.user.id : '')">
      <div v-loading="detailLoading" v-if="detail.user" class="d-wrap">
        <!-- 基本信息 -->
        <div class="d-card">
          <div class="d-head">
            <div class="d-avatar">{{ (detail.user.nickname || '书').slice(0, 1) }}</div>
            <div class="d-main">
              <div class="d-name">
                {{ detail.user.nickname }}
                <el-tag size="small" :type="detail.user.status === 1 ? 'success' : 'danger'">
                  {{ detail.user.status === 1 ? '正常' : '封禁' }}
                </el-tag>
                <el-tag v-if="detail.user.muteStatus === 1" size="small" type="warning">禁言中</el-tag>
              </div>
              <p>{{ detail.user.majorName || '未填专业' }} · {{ detail.user.grade || '—' }} 级
                 · 信用 {{ detail.user.creditScore }}</p>
              <p class="d-sign">{{ detail.user.signature || '没有个性签名' }}</p>
            </div>
          </div>

          <div class="d-kv">
            <span>学号</span><b>{{ detail.user.studentNo || '—' }}</b>
            <span>手机号</span><b>{{ detail.user.phone || '—' }}</b>
            <span>真实姓名</span><b class="warn">{{ detail.user.realName || '未登记' }}</b>
            <span>注册时间</span><b>{{ fmt(detail.user.createdAt) }}</b>
          </div>
          <p v-if="detail.user.banReason" class="d-warn">
            封禁原因：{{ detail.user.banReason }}
            <template v-if="detail.user.banUntil">（至 {{ fmt(detail.user.banUntil) }}）</template>
          </p>
          <p v-if="detail.user.muteReason" class="d-warn">
            禁言原因：{{ detail.user.muteReason }}
          </p>
        </div>

        <!-- 统计 -->
        <div class="d-stats">
          <div><b>{{ detail.stats.bookCount }}</b><small>发布</small></div>
          <i></i>
          <div><b>{{ detail.stats.soldCount }}</b><small>卖出</small></div>
          <i></i>
          <div><b>{{ detail.stats.buyCount }}</b><small>买入</small></div>
          <i></i>
          <div><b>{{ detail.stats.reviewCount }}</b><small>收到评价</small></div>
          <i></i>
          <div><b :class="{ danger: detail.stats.reportAgainst > 0 }">{{ detail.stats.reportAgainst }}</b><small>被举报</small></div>
        </div>

        <!-- 发布的书 -->
        <div class="d-card">
          <div class="d-title">最近发布</div>
          <div v-if="!detail.books.length" class="d-empty">还没有发布过闲置</div>
          <div class="d-row" v-for="b in detail.books" :key="b.id">
            <span class="d-row-main">{{ b.title }}</span>
            <span class="d-muted">¥{{ b.price }}</span>
            <el-tag size="small" :type="b.status === 1 ? 'success' : b.status === 2 ? 'info' : 'danger'">
              {{ bookStatus(b.status) }}
            </el-tag>
            <span class="d-muted">{{ fmt(b.createdAt) }}</span>
          </div>
        </div>

        <!-- 订单 -->
        <div class="d-card">
          <div class="d-title">最近订单</div>
          <div v-if="!detail.orders.length" class="d-empty">还没有交易记录</div>
          <div class="d-row" v-for="o in detail.orders" :key="o.id">
            <span class="d-row-main">{{ o.bookTitle }}</span>
            <span class="d-muted">{{ o.role === 'buyer' ? '买入' : '卖出' }}</span>
            <span class="d-muted">¥{{ o.price }}</span>
            <el-tag size="small" type="info">{{ orderStatus(o.status) }}</el-tag>
            <span class="d-muted">{{ fmt(o.createdAt) }}</span>
          </div>
        </div>

        <!-- 评价 -->
        <div class="d-card">
          <div class="d-title">收到的评价</div>
          <div v-if="!detail.reviews.length" class="d-empty">还没有收到评价</div>
          <div class="d-review" v-for="(r, i) in detail.reviews" :key="i">
            <span class="d-star">{{ r.rating }} 星</span>
            <span class="d-muted">《{{ r.bookTitle }}》</span>
            <p>{{ r.content || '未填写内容' }}</p>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="creditOpen" title="调整信用分" width="460px">
      <p class="dialog-tip">
        用于纠正误判。修改后会发站内通知告知本人，并说明原因。
      </p>
      <div class="credit-row">
        <span>{{ creditTarget.nickname }}</span>
        <b :class="{ danger: creditValue < 60 }">{{ creditValue }}</b>
      </div>
      <el-slider v-model="creditValue" :min="0" :max="100" :step="1" show-input />
      <el-input v-model="creditReason" type="textarea" :rows="2" placeholder="调整原因（会展示给用户）"
                style="margin-top: 14px" />
      <template #footer>
        <el-button @click="creditOpen = false">取消</el-button>
        <el-button type="primary" @click="submitCredit">确定调整</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="renameOpen" title="重置用户昵称" width="440px">
      <p class="dialog-tip">
        用于处理含联系方式、广告或违规词的昵称。修改后系统会发一条站内通知告知本人。
      </p>
      <el-input v-model="renameValue" maxlength="20" show-word-limit placeholder="输入新昵称" />
      <template #footer>
        <el-button @click="renameOpen = false">取消</el-button>
        <el-button type="primary" @click="submitRename">确定修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px">
      <el-form label-width="72px">
        <el-form-item label="时长">
          <el-radio-group v-model="form.days">
            <el-radio-button :value="1">1 天</el-radio-button>
            <el-radio-button :value="3">3 天</el-radio-button>
            <el-radio-button :value="7">7 天</el-radio-button>
            <el-radio-button :value="30">30 天</el-radio-button>
            <el-radio-button :value="0">永久</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原因">
          <el-select v-model="form.reason" filterable allow-create default-first-option
                     placeholder="选择常用原因，或直接输入" style="width: 100%">
            <el-option v-for="r in reasonOptions" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="dialog-tip">
        对象：{{ current ? current.nickname : '' }}
        <span v-if="form.days > 0"> · {{ form.days }} 天后自动解除</span>
        <span v-else> · 需要手动解除</span>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmAction">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getUsers, banUser, unbanUser, muteUser, unmuteUser, resetNickname, userDetail,
         exportFile, adjustCredit } from '../api'
import { saveBlob, today } from '../utils/download'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({
  sort: 'idDesc', keyword: '', page: 1, size: 10 })

const dialogVisible = ref(false)
const actionType = ref('ban')
const current = ref(null)
const form = reactive({ days: 3, reason: '' })

const dialogTitle = computed(() => (actionType.value === 'ban' ? '封禁账号' : '禁言账号'))

const reasonOptions = computed(() =>
  actionType.value === 'ban'
    ? ['发布违规书籍信息', '售卖盗版或非正版书籍', '多次爽约不履行交易', '恶意骚扰其他同学', '账号信息造假']
    : ['聊天中使用不当言论', '刷屏或发布广告', '恶意议价骚扰卖家', '泄露他人隐私']
)

function fmtDate(v) {
  return v ? String(v).slice(0, 10) : ''
}

// 详情抽屉里统一用这个：日期 + 时分
function fmt(v) {
  return v ? String(v).replace('T', ' ').slice(0, 16) : '—'
}

async function load() {
  loading.value = true
  try {
    const data = await getUsers({
      keyword: query.keyword || undefined,
      sort: query.sort || undefined,
      page: query.page,
      size: query.size
    })
    rows.value = (data && data.list) || []
    total.value = (data && data.total) || 0
  } finally {
    loading.value = false
  }
}

// ---------- 详情 ----------
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref({})

const BOOK_STATUS = { 0: '待审核', 1: '在售中', 2: '已售出', 3: '已下架', 4: '已驳回' }
const ORDER_STATUS = { 0: '待确认', 1: '已确认', 2: '已完成', 3: '已取消', 4: '爽约' }
const bookStatus = (s) => BOOK_STATUS[s] || '未知'
const orderStatus = (s) => ORDER_STATUS[s] || '未知'

async function openDetail(row) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = {}
  try {
    detail.value = await userDetail(row.id)
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    detailLoading.value = false
  }
}

// ---------- 导出 ----------
const exporting = ref(false)

async function doExport() {
  exporting.value = true
  try {
    const blob = await exportFile('users')
    saveBlob(blob, '用户列表_' + today() + '.xlsx')
    ElMessage.success('已导出')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

// ---------- 信用分 ----------
const creditOpen = ref(false)
const creditTarget = ref({})
const creditValue = ref(100)
const creditReason = ref('')

function openCredit(row) {
  creditTarget.value = row
  creditValue.value = row.creditScore == null ? 100 : row.creditScore
  creditReason.value = ''
  creditOpen.value = true
}

async function submitCredit() {
  try {
    await adjustCredit(creditTarget.value.id, creditValue.value, creditReason.value)
    ElMessage.success('已调整并通知用户')
    creditOpen.value = false
    load()
  } catch (e) {
    ElMessage.error(e.message || '调整失败')
  }
}

// ---------- 重置昵称 ----------
const renameOpen = ref(false)
const renameValue = ref('')
const renameTarget = ref(null)

/** 默认名：违规名称 + 4 位随机数，避免和正常用户撞名 */
function genDefaultName() {
  return '违规名称' + Math.floor(1000 + Math.random() * 9000)
}

function openRename(row) {
  renameTarget.value = row
  renameValue.value = genDefaultName()
  renameOpen.value = true
}

async function submitRename() {
  const name = (renameValue.value || '').trim()
  if (!name) return ElMessage.warning('昵称不能为空')
  try {
    await resetNickname(renameTarget.value.id, name)
    ElMessage.success('已重置，并已通知该用户')
    renameOpen.value = false
    load()
  } catch (e) {
    ElMessage.error(e.message || '重置失败')
  }
}

function onSortChange({ prop, order }) {
  if (prop !== 'id') return
  query.sort = order === 'ascending' ? 'idAsc' : 'idDesc'
  query.page = 1
  load()
}

function reload() {
  query.page = 1
  load()
}

function resetQuery() {
  query.keyword = ''
  query.page = 1
  load()
}

function onPageChange(p) {
  query.page = p
  load()
}

function openAction(row, type) {
  current.value = row
  actionType.value = type
  form.days = 3
  form.reason = ''
  dialogVisible.value = true
}

async function confirmAction() {
  if (!current.value) return
  saving.value = true
  try {
    const call = actionType.value === 'ban' ? banUser : muteUser
    await call(current.value.id, form.days, form.reason || undefined)
    ElMessage.success(actionType.value === 'ban' ? '已封禁' : '已禁言')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function unban(row) {
  await unbanUser(row.id)
  ElMessage.success('已解除封禁')
  load()
}

async function unmute(row) {
  await unmuteUser(row.id)
  ElMessage.success('已解除禁言')
  load()
}

onMounted(load)
</script>

<style scoped>
.credit-row {
  display: flex; justify-content: space-between; align-items: baseline;
  margin-bottom: 6px; font-size: 13px; color: #475569;
}
.credit-row b { font-size: 22px; color: #0f766e; }
.credit-row b.danger { color: #dc2626; }

.d-wrap { padding: 0 4px; }
.d-card { background: #f8fafc; border-radius: 10px; padding: 14px; margin-bottom: 14px; }
.d-head { display: flex; gap: 12px; align-items: center; }
.d-avatar {
  width: 52px; height: 52px; border-radius: 50%; background: #ccfbf1; color: #0f766e;
  display: grid; place-items: center; font-size: 20px; font-weight: 600; flex: none;
}
.d-name { font-size: 16px; font-weight: 650; display: flex; align-items: center; gap: 6px; }
.d-main p { margin: 4px 0 0; font-size: 12.5px; color: #64748b; }
.d-sign { color: #94a3b8 !important; }
.d-kv {
  display: grid; grid-template-columns: 76px 1fr 76px 1fr; gap: 6px 8px;
  margin-top: 12px; font-size: 12.5px;
}
.d-kv span { color: #94a3b8; }
.d-kv b { color: #1e293b; font-weight: 600; }
.d-kv .warn { color: #b45309; }
.d-warn { margin: 10px 0 0; font-size: 12.5px; color: #b91c1c; background: #fef2f2; padding: 7px 9px; border-radius: 7px; }

.d-stats { display: flex; align-items: center; background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 12px 0; margin-bottom: 14px; }
.d-stats div { flex: 1; display: flex; flex-direction: column; align-items: center; }
.d-stats b { font-size: 18px; }
.d-stats b.danger { color: #dc2626; }
.d-stats small { font-size: 11px; color: #94a3b8; margin-top: 2px; }
.d-stats i { width: 1px; height: 20px; background: #e2e8f0; }

.d-title { font-size: 13px; font-weight: 650; margin-bottom: 9px; color: #334155; }
.d-row { display: flex; align-items: center; gap: 10px; padding: 7px 0; border-bottom: 1px dashed #e2e8f0; font-size: 12.5px; }
.d-row:last-child { border-bottom: none; }
.d-row-main { flex: 1; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.d-muted { color: #94a3b8; font-size: 12px; }
.d-empty { font-size: 12.5px; color: #94a3b8; padding: 6px 0; }
.d-review { padding: 7px 0; border-bottom: 1px dashed #e2e8f0; }
.d-review:last-child { border-bottom: none; }
.d-star { font-size: 12px; color: #d97706; margin-right: 6px; }
.d-review p { margin: 3px 0 0; font-size: 12.5px; color: #475569; }

.dialog-tip {
  font-size: 12px;
  color: #94a3b8;
  padding-left: 4px;
}
</style>