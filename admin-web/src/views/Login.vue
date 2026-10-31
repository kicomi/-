<template>
  <div class="login">
    <div class="bg-grid"></div>
    <div class="glow glow-a"></div>
    <div class="glow glow-b"></div>

    <div class="stage">
      <div class="intro">
        <div class="logo">
          <span>书</span>
        </div>
        <h1>校园二手书</h1>
        <p class="slogan">供需匹配 · 智能定价 · 交易闭环</p>
        <ul class="points">
          <li><i></i>协同过滤 + 内容匹配双路推荐</li>
          <li><i></i>随机森林回归定价，给出 80% 价格区间</li>
          <li><i></i>微信小程序学生端 + Web 管理端</li>
        </ul>
      </div>

      <div class="card">
        <h2>管理员登录</h2>
        <p class="tip">请输入后台账号</p>

        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @keyup.enter="submit">
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" size="large" placeholder="admin">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" size="large" show-password placeholder="请输入密码">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
            登 录
          </el-button>
        </el-form>

        <div class="hint">默认账号 admin / admin123</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminLogin } from '../api'
import { auth } from '../store/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ username: 'admin', password: 'admin123' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  if (formRef.value) {
    const ok = await formRef.value.validate().catch(() => false)
    if (!ok) return
  }
  loading.value = true
  try {
    const data = await adminLogin({ username: form.username, password: form.password })
    auth.save(data.token, data.user)
    ElMessage.success('欢迎回来，' + (data.user.nickname || '管理员'))
    router.push('/dashboard')
  } catch (e) {
    // 统一在拦截器里提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login {
  position: relative;
  height: 100%;
  overflow: hidden;
  background: radial-gradient(1100px 620px at 15% 10%, #0f2a24 0%, transparent 60%),
    radial-gradient(900px 600px at 90% 90%, #131f3a 0%, transparent 62%),
    #060c15;
  display: grid;
  place-items: center;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.06) 1px, transparent 1px);
  background-size: 46px 46px;
  -webkit-mask-image: radial-gradient(circle at 50% 40%, #000 10%, transparent 72%);
  mask-image: radial-gradient(circle at 50% 40%, #000 10%, transparent 72%);
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.42;
}

.glow-a {
  width: 380px;
  height: 380px;
  background: #10b981;
  top: -120px;
  left: 8%;
}

.glow-b {
  width: 420px;
  height: 420px;
  background: #2563eb;
  bottom: -160px;
  right: 6%;
}

.stage {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 72px;
  align-items: center;
  max-width: 1080px;
  width: 100%;
  padding: 0 40px;
}

.intro {
  color: #e2e8f0;
}

.logo {
  width: 52px;
  height: 52px;
  border-radius: 15px;
  display: grid;
  place-items: center;
  font-size: 24px;
  font-weight: 700;
  color: #04120c;
  background: linear-gradient(135deg, #6ee7b7, #22d3ee);
  box-shadow: 0 14px 34px -14px rgba(52, 211, 153, 0.9);
  margin-bottom: 22px;
}

.intro h1 {
  margin: 0 0 10px;
  font-size: 38px;
  letter-spacing: 3px;
  background: linear-gradient(90deg, #f8fafc, #6ee7b7 70%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.slogan {
  margin: 0 0 30px;
  font-size: 14px;
  color: #64748b;
  letter-spacing: 1.4px;
}

.points {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.points li {
  font-size: 13px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 10px;
}

.points i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 8px #34d399;
}

.card {
  background: rgba(255, 255, 255, 0.045);
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 18px;
  padding: 30px 30px 24px;
  backdrop-filter: blur(14px);
  box-shadow: 0 30px 70px -40px rgba(0, 0, 0, 0.9);
}

.card h2 {
  margin: 0 0 6px;
  font-size: 19px;
  font-weight: 620;
  color: #f1f5f9;
}

.tip {
  margin: 0 0 22px;
  font-size: 12.5px;
  color: #64748b;
}

.card :deep(.el-form-item__label) {
  color: #94a3b8;
  font-size: 12.5px;
  padding-bottom: 4px;
}

.card :deep(.el-input__wrapper) {
  background: rgba(15, 23, 42, 0.6);
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.2) inset;
  border-radius: 10px;
}

.card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #10b981 inset;
}

.card :deep(.el-input__inner) {
  color: #e2e8f0;
}

.card :deep(.el-input__inner::placeholder) {
  color: #475569;
}

.card :deep(.el-form-item__error) {
  color: #f87171;
}

.submit {
  width: 100%;
  margin-top: 6px;
  height: 44px;
  font-size: 15px;
  letter-spacing: 4px;
  background: linear-gradient(90deg, #10b981, #0ea5a4);
  border: none;
}

.hint {
  margin-top: 18px;
  text-align: center;
  font-size: 11.5px;
  color: #475569;
}

@media (max-width: 960px) {
  .stage {
    grid-template-columns: 1fr;
    gap: 32px;
  }
  .intro {
    text-align: center;
  }
  .logo {
    margin: 0 auto 18px;
  }
  .points {
    display: none;
  }
}
</style>
