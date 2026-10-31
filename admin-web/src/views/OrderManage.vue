<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>订单管理</h2>
        <p>共 {{ total }} 笔交易记录，包含已完成与已取消订单</p>
      </div>
      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 138px" @change="reload">
          <el-option label="待确认" :value="0" />
          <el-option label="已预约" :value="1" />
          <el-option label="已完成" :value="2" />
          <el-option label="已取消" :value="3" />
          <el-option label="爽约" :value="4" />
        </el-select>
        <el-button :icon="Refresh" @click="reload">刷新</el-button>
        <el-button type="success" plain :loading="exporting" @click="doExport">导出 Excel</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">订单明细</div>
      <el-table :data="rows" v-loading="loading" stripe size="small">
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="bookTitle" label="书籍" min-width="180" show-overflow-tooltip />
        <el-table-column prop="buyerNickname" label="买家" width="108" />
        <el-table-column prop="sellerNickname" label="卖家" width="108" />
        <el-table-column label="成交价" width="98">
          <template #default="{ row }">
            <strong class="money" style="color: #059669">¥{{ row.price }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" effect="light">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pickupPlace" label="自提点" width="132" />
        <el-table-column prop="createdAt" label="下单时间" width="158" />
      </el-table>

      <el-pagination style="margin-top: 16px; justify-content: flex-end"
                     layout="total, prev, pager, next"
                     :total="total" :page-size="query.size" :current-page="query.page"
                     @current-change="onPageChange" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getOrders, exportFile } from '../api'
import { saveBlob, today } from '../utils/download'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ status: null, page: 1, size: 10 })

function statusType(s) {
  if (s === 2) return 'success'
  if (s === 1) return 'primary'
  if (s === 3) return 'info'
  if (s === 4) return 'danger'
  return 'warning'
}

async function load() {
  loading.value = true
  try {
    const data = await getOrders({ status: query.status, page: query.page, size: query.size })
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

function onPageChange(p) {
  query.page = p
  load()
}

onMounted(load)

// ---------- 导出 ----------
const exporting = ref(false)

async function doExport() {
  exporting.value = true
  try {
    const blob = await exportFile('orders')
    saveBlob(blob, '订单列表_' + today() + '.xlsx')
    ElMessage.success('已导出')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}
</script>