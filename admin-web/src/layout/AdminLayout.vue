<template>
  <el-container class="shell">
    <el-aside width="224px" class="aside">
      <div class="brand">
        <div class="brand-mark">书</div>
        <div class="brand-text">
          <strong>校园二手书</strong>
          <small>供需匹配 · 智能定价</small>
        </div>
      </div>

      <div class="menu-label">运营</div>
      <el-menu :default-active="activeMenu" router class="menu" :collapse="false">
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon><span>数据大屏</span>
        </el-menu-item>
        <el-menu-item index="/books">
          <el-icon><Reading /></el-icon><span>书籍审核</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><Tickets /></el-icon><span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/reports">
          <el-icon><Warning /></el-icon><span>举报处理</span>
        </el-menu-item>
        <el-menu-item index="/banners">
          <el-icon><PictureFilled /></el-icon><span>广告管理</span>
        </el-menu-item>
        <el-menu-item index="/ai">
          <el-icon><MagicStick /></el-icon><span>运营助手</span>
        </el-menu-item>
      </el-menu>

      <div class="aside-foot">
        <div class="algo" :class="algo.alive ? 'on' : 'off'">
          <span class="pulse"></span>
          <div class="algo-text">
            <b>{{ algo.alive ? '算法服务在线' : '算法服务离线' }}</b>
            <small>{{ algo.alive ? 'ItemCF + 回归定价' : '已降级为规则估算' }}</small>
          </div>
        </div>
      </div>
    </el-aside>

    <el-container class="right">
      <el-header class="header" :class="{ 'header-dark': isDashboard }">
        <div class="header-left">
          <span class="eyebrow">管理后台</span>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="header-right">
          <div class="clock">{{ clock }}</div>
          <el-dropdown @command="onCommand">
            <div class="user">
              <div class="avatar">管</div>
              <div class="user-meta">
                <b>{{ userName }}</b>
                <small>系统管理员</small>
              </div>
              <el-icon class="caret"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main :class="['main', isDashboard ? 'main-screen' : '']">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auth } from '../store/auth'
import { getAlgoStatus } from '../api'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '管理后台')
const userName = computed(() => (auth.user && auth.user.nickname) || '管理员')

// 大屏页要铺满深色，其他页面保持浅色工作区
const isDashboard = computed(() => route.path === '/dashboard')

const algo = ref({ alive: false })
const clock = ref('')

let timer = null
function tick() {
  const d = new Date()
  const pad = (n) => (n < 10 ? '0' + n : '' + n)
  clock.value =
    d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
    ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds())
}

onMounted(async () => {
  tick()
  timer = setInterval(tick, 1000)
  try {
    algo.value = await getAlgoStatus()
  } catch (e) {
    algo.value = { alive: false }
  }
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})

async function onCommand(cmd) {
  if (cmd !== 'logout') return
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  auth.logout()
  ElMessage.success('已退出')
  router.push('/login')
}
</script>

<style scoped>
.shell {
  height: 100%;
}

/* ---------------- 侧栏 ---------------- */

.aside {
  background: linear-gradient(180deg, #0b1526 0%, #0f1e33 45%, #0b1526 100%);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(148, 163, 184, 0.12);
  position: relative;
  overflow: hidden;
}

.aside::after {
  content: '';
  position: absolute;
  top: -60px;
  right: -70px;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.22), transparent 70%);
  pointer-events: none;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 20px 18px 18px;
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 17px;
  color: #04120c;
  background: linear-gradient(135deg, #34d399, #22d3ee);
  box-shadow: 0 6px 16px -6px rgba(52, 211, 153, 0.9);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.brand-text strong {
  color: #e8eef7;
  font-size: 15px;
  letter-spacing: 0.4px;
}

.brand-text small {
  color: #64748b;
  font-size: 10.5px;
  letter-spacing: 0.6px;
}

.menu-label {
  padding: 6px 20px 8px;
  font-size: 10.5px;
  letter-spacing: 1.6px;
  color: #475569;
}

.menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 0 10px;
}

.menu :deep(.el-menu-item) {
  height: 44px;
  border-radius: 10px;
  margin-bottom: 4px;
  color: #94a3b8;
  font-size: 13.5px;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(148, 163, 184, 0.09);
  color: #e2e8f0;
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(16, 185, 129, 0.18), rgba(16, 185, 129, 0.04));
  color: #6ee7b7;
  box-shadow: inset 2px 0 0 #10b981;
}

.menu :deep(.el-menu-item.is-active .el-icon) {
  color: #34d399;
}

.aside-foot {
  padding: 14px;
}

.algo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 11px;
  background: rgba(148, 163, 184, 0.07);
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.algo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.algo-text b {
  font-size: 12px;
  font-weight: 600;
}

.algo-text small {
  font-size: 10.5px;
  color: #64748b;
}

.algo.on .algo-text b {
  color: #6ee7b7;
}

.algo.off .algo-text b {
  color: #fca5a5;
}

.pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex: none;
}

.algo.on .pulse {
  background: #34d399;
  box-shadow: 0 0 0 0 rgba(52, 211, 153, 0.7);
  animation: pulse 2s infinite;
}

.algo.off .pulse {
  background: #f87171;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(52, 211, 153, 0.55); }
  70% { box-shadow: 0 0 0 8px rgba(52, 211, 153, 0); }
  100% { box-shadow: 0 0 0 0 rgba(52, 211, 153, 0); }
}

/* ---------------- 顶栏 ---------------- */

.right {
  background: var(--canvas);
}

.header {
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid var(--line);
  padding: 0 24px;
}

.header-dark {
  background: rgba(7, 13, 24, 0.92);
  border-bottom-color: rgba(148, 163, 184, 0.14);
}

.header-left .eyebrow {
  display: block;
  font-size: 10.5px;
  letter-spacing: 1.6px;
  color: #94a3b8;
  margin-bottom: 2px;
}

.header-left h1 {
  margin: 0;
  font-size: 17px;
  font-weight: 650;
  color: var(--ink-900);
  letter-spacing: 0.3px;
}

.header-dark .header-left h1 {
  color: #e2e8f0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.clock {
  font-size: 12.5px;
  font-variant-numeric: tabular-nums;
  color: #94a3b8;
  padding: 5px 12px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: #f8fafc;
}

.header-dark .clock {
  color: #94a3b8;
  border-color: rgba(148, 163, 184, 0.18);
  background: rgba(148, 163, 184, 0.06);
}

.user {
  display: flex;
  align-items: center;
  gap: 9px;
  cursor: pointer;
  padding: 5px 8px 5px 5px;
  border-radius: 10px;
  transition: background 0.15s;
}

.user:hover {
  background: #f1f5f9;
}

.header-dark .user:hover {
  background: rgba(148, 163, 184, 0.1);
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #04120c;
  background: linear-gradient(135deg, #6ee7b7, #34d399);
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.user-meta b {
  font-size: 13px;
  color: var(--ink-900);
}

.header-dark .user-meta b {
  color: #e2e8f0;
}

.user-meta small {
  font-size: 10.5px;
  color: #94a3b8;
}

.caret {
  color: #94a3b8;
  font-size: 12px;
}

/* ---------------- 内容区 ---------------- */

.main {
  padding: 20px 24px;
  overflow-y: auto;
}

.main-screen {
  padding: 0;
  /* 大屏内容比一屏高时允许纵向滚动，避免底部被裁掉 */
  overflow-y: auto;
  overflow-x: hidden;
  background: var(--screen-bg);
}
</style>
