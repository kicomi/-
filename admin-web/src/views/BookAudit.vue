<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>书籍审核</h2>
        <p>审核通过后书籍才会出现在小程序学生端，驳回需填写原因</p>
      </div>
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="书名 / 作者 / ISBN" clearable
                  style="width: 240px" :prefix-icon="Search" @keyup.enter="reload" />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 132px" @change="reload">
          <el-option label="待审核" :value="0" />
          <el-option label="在售中" :value="1" />
          <el-option label="已售出" :value="2" />
          <el-option label="已下架" :value="3" />
          <el-option label="已驳回" :value="4" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="reload">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">共 {{ total }} 本书籍</div>
      <el-table :data="rows" v-loading="loading" stripe size="small">
        <el-table-column prop="id" label="ID" width="66" />
        <el-table-column label="封面" width="72">
          <template #default="{ row }">
            <div class="thumb">
              <img v-if="row.coverImg" :src="row.coverImg" />
              <span v-else>{{ (row.title || '书').slice(0, 1) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="书名" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.priority === 1" size="small" type="success" effect="plain" style="margin-right: 5px">优先</el-tag>
            {{ row.title }}
          </template>
        </el-table-column>
        <el-table-column prop="sellerNickname" label="卖家" width="110" />
        <el-table-column label="原价 / 售价" width="132">
          <template #default="{ row }">
            <span class="money" style="color: #94a3b8">¥{{ row.originalPrice }}</span>
            <span style="color: #cbd5e1; margin: 0 3px">/</span>
            <strong class="money" style="color: #059669">¥{{ row.price }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="品相" width="82">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" type="info">{{ row.conditionLevel }} 成</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发布时间" width="158" />
        <el-table-column label="操作" width="196" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="success" link size="small" @click="approve(row)">通过</el-button>
            <el-button v-if="row.status === 0 || row.status === 1" type="danger" link size="small"
                       @click="openReject(row)">驳回</el-button>
            <el-button v-if="row.status === 1" type="warning" link size="small" @click="offline(row)">下架</el-button>
            <el-button v-if="row.status === 4" type="primary" link size="small" @click="approve(row)">恢复上架</el-button>
            <span v-if="row.status === 2 || row.status === 3" style="color: #cbd5e1; font-size: 12px">—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination style="margin-top: 16px; justify-content: flex-end"
                     layout="total, prev, pager, next, sizes"
                     :total="total" :page-size="query.size" :current-page="query.page"
                     :page-sizes="[10, 20, 50]"
                     @current-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <el-dialog v-model="rejectVisible" title="驳回书籍" width="480px">
      <el-form label-width="82px">
        <el-form-item label="驳回原因">
          <el-select v-model="rejectReason" filterable allow-create default-first-option
                     placeholder="选择常用原因，或直接输入" style="width: 100%">
            <el-option v-for="r in rejectReasons" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="rejectRemark" type="textarea" :rows="2" placeholder="选填，会一并推送给卖家" />
        </el-form-item>
      </el-form>
      <div class="dialog-tip">即将驳回《{{ currentBook ? currentBook.title : '' }}》</div>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getBooks, auditBook } from '../api'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ status: null, keyword: '', page: 1, size: 10 })

const rejectVisible = ref(false)
const rejectReason = ref('')
const rejectRemark = ref('')
const currentBook = ref(null)

// 常见驳回原因，管理员下拉直接选；想写别的也能输入（allow-create）
const rejectReasons = [
  '图片与实物不符',
  '书籍信息填写不完整',
  '价格明显高于市场价',
  '疑似盗版或非正版教材',
  '描述含联系方式等违规内容',
  '重复发布同一本书',
  '书况描述与实际差距较大'
]

const STATUS_MAP = { 0: '待审核', 1: '在售中', 2: '已售出', 3: '已下架', 4: '已驳回' }
const statusText = (s) => STATUS_MAP[s] || '未知'

function statusType(s) {
  if (s === 1) return 'success'
  if (s === 2) return 'info'
  if (s === 4) return 'danger'
  if (s === 3) return 'warning'
  return 'primary'
}

async function load() {
  loading.value = true
  try {
    const data = await getBooks({
      status: query.status,
      keyword: query.keyword || undefined,
      page: query.page,
      size: query.size
    })
    rows.value = (data && data.list) || []
    total.value = (data && data.total) || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  query.page = 1
  load()
}

function resetQuery() {
  query.status = null
  query.keyword = ''
  query.page = 1
  load()
}

function onPageChange(p) {
  query.page = p
  load()
}

function onSizeChange(s) {
  query.size = s
  query.page = 1
  load()
}

async function approve(row) {
  await auditBook(row.id, { action: 'approve' })
  ElMessage.success('已通过：《' + row.title + '》')
  load()
}

function openReject(row) {
  currentBook.value = row
  rejectReason.value = ''
  rejectRemark.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  const reason = (rejectReason.value || '').trim()
  if (!reason) {
    ElMessage.warning('请选择或填写驳回原因')
    return
  }
  const remark = (rejectRemark.value || '').trim()
  const full = remark ? reason + '：' + remark : reason
  await auditBook(currentBook.value.id, { action: 'reject', reason: full })
  ElMessage.success('已驳回')
  rejectVisible.value = false
  load()
}

async function offline(row) {
  try {
    await ElMessageBox.confirm('确定下架《' + row.title + '》吗？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await auditBook(row.id, { action: 'offline', reason: '管理员手动下架' })
  ElMessage.success('已下架')
  load()
}

onMounted(load)
</script>

<style scoped>
.thumb {
  width: 40px; height: 54px; border-radius: 4px; overflow: hidden;
  background: #eef2f0; display: grid; place-items: center;
  color: #8aa79a; font-size: 15px; font-family: serif;
}
.thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }

.dialog-tip {
  font-size: 12px;
  color: #94a3b8;
  padding-left: 4px;
}
</style>