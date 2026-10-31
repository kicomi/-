<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>广告管理</h2>
        <p>投放给小程序学生端的首页轮播，可控制位置、排序与投放时间</p>
      </div>
      <div class="filter-bar">
        <el-select v-model="position" placeholder="全部位置" clearable style="width: 160px" @change="load">
          <el-option label="首页轮播" value="home_banner" />
          <el-option label="分类页" value="category_banner" />
          <el-option label="详情页" value="detail_banner" />
        </el-select>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建广告</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">广告位列表（共 {{ rows.length }} 条）</div>
      <el-table :data="rows" v-loading="loading" stripe size="small">
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="广告" min-width="220">
          <template #default="{ row }">
            <div class="ad-cell">
              <div class="ad-thumb" :style="row.imageUrl ? { backgroundImage: 'url(' + row.imageUrl + ')' } : {}">
                <span v-if="!row.imageUrl">{{ row.title.slice(0, 1) }}</span>
              </div>
              <div class="ad-text">
                <b>{{ row.title }}</b>
                <small>{{ row.subtitle || '—' }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="位置" width="120">
          <template #default="{ row }">{{ positionText(row.position) }}</template>
        </el-table-column>
        <el-table-column label="跳转" width="150">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" type="info">{{ linkText(row.linkType) }}</el-tag>
            <span style="color: #94a3b8; margin-left: 6px; font-size: 12px">{{ row.linkValue || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="曝光 / 点击" width="120">
          <template #default="{ row }">
            <span class="money">{{ row.viewCount }}</span>
            <span style="color: #cbd5e1"> / </span>
            <span class="money" style="color: #059669">{{ row.clickCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="投放状态" width="100">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" size="small"
                       @change="(val) => toggle(row, val)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑广告' : '新建广告'" width="560px">
      <el-form :model="form" label-width="88px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="100" placeholder="例如：开学季教材专场" />
        </el-form-item>
        <el-form-item label="推广语">
          <el-input v-model="form.subtitle" maxlength="200" placeholder="一句话说明卖点" />
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="form.imageUrl" placeholder="留空则显示纯色文字卡片" />
        </el-form-item>
        <el-form-item label="投放位置">
          <el-select v-model="form.position" style="width: 100%">
            <el-option label="首页轮播" value="home_banner" />
            <el-option label="分类页" value="category_banner" />
            <el-option label="详情页" value="detail_banner" />
          </el-select>
        </el-form-item>
        <el-form-item label="跳转类型">
          <el-select v-model="form.linkType" style="width: 100%">
            <el-option label="书籍详情" value="book" />
            <el-option label="分类列表" value="category" />
            <el-option label="搜索关键词" value="search" />
            <el-option label="小程序页面 / 外链" value="url" />
            <el-option label="不跳转" value="none" />
          </el-select>
        </el-form-item>
        <el-form-item label="跳转目标">
          <el-input v-model="form.linkValue" placeholder="书籍ID / 分类ID / 关键词 / 路径" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
          <span class="form-tip">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="投放时间">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="开始（可空）"
                          value-format="YYYY-MM-DD HH:mm:ss" style="width: 46%" />
          <span style="margin: 0 8px; color: #cbd5e1">至</span>
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="结束（可空）"
                          value-format="YYYY-MM-DD HH:mm:ss" style="width: 46%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getBanners, saveBanner, setBannerStatus, deleteBanner } from '../api'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const position = ref('')

const dialogVisible = ref(false)
const form = reactive({
  id: null,
  title: '',
  subtitle: '',
  imageUrl: '',
  linkType: 'book',
  linkValue: '',
  position: 'home_banner',
  sort: 0,
  status: 1,
  startTime: null,
  endTime: null
})

function positionText(p) {
  return { home_banner: '首页轮播', category_banner: '分类页', detail_banner: '详情页' }[p] || p
}

function linkText(t) {
  return { book: '书籍', category: '分类', search: '搜索', url: '跳转', none: '无' }[t] || t
}

async function load() {
  loading.value = true
  try {
    rows.value = (await getBanners(position.value || undefined)) || []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: null, title: '', subtitle: '', imageUrl: '',
    linkType: 'book', linkValue: '', position: 'home_banner',
    sort: 0, status: 1, startTime: null, endTime: null
  })
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写广告标题')
    return
  }
  saving.value = true
  try {
    await saveBanner({ ...form })
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggle(row, enabled) {
  await setBannerStatus(row.id, enabled ? 1 : 0)
  ElMessage.success(enabled ? '已开始投放' : '已暂停投放')
  load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除广告「' + row.title + '」？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteBanner(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.ad-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ad-thumb {
  width: 54px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #10b981, #22d3ee);
  background-size: cover;
  background-position: center;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 700;
  flex: none;
}

.ad-text {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
  min-width: 0;
}

.ad-text b {
  font-size: 13px;
  color: #0f172a;
}

.ad-text small {
  font-size: 11.5px;
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
