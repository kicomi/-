<template>
  <div class="reg">
    <header class="nav">
      <button class="back" @click="$router.back()">‹</button>
      <span>注册账号</span>
      <div style="width: 32px"></div>
    </header>

    <div class="page">
      <div class="card">
        <div class="field">
          <label class="required">手机号 / 学号</label>
          <input v-model="form.account" placeholder="作为登录账号" />
        </div>
        <div class="field">
          <label class="required">密码</label>
          <input v-model="form.password" type="password" placeholder="至少 6 位" />
        </div>
        <div class="field">
          <label class="required">昵称</label>
          <input v-model="form.nickname" placeholder="同学你好，怎么称呼" />
        </div>
      </div>

      <div class="card">
        <div class="card-title">实名信息<span class="sub">仅管理员可见</span></div>
        <div class="field">
          <label>真实姓名</label>
          <input v-model="form.realName" placeholder="用于线下交易核验" />
        </div>
        <div class="field">
          <label>学号</label>
          <input v-model="form.studentNo" placeholder="2023xxxxxx" />
        </div>
        <div class="field">
          <label>专业</label>
          <select v-model="form.majorId">
            <option :value="null">请选择专业</option>
            <option v-for="m in majors" :key="m.id" :value="m.id">{{ m.name }}</option>
          </select>
        </div>
        <div class="field">
          <label>年级</label>
          <input v-model="form.grade" placeholder="2024" />
        </div>
        <div class="field" style="margin-bottom: 0">
          <label>性别</label>
          <div class="seg">
            <button v-for="g in genders" :key="g" :class="{ on: form.gender === g }" @click="form.gender = g">{{ g }}</button>
          </div>
        </div>
      </div>

      <button class="btn btn-primary btn-block" :disabled="saving" @click="submit">
        {{ saving ? '注册中…' : '注册并登录' }}
      </button>
      <p class="foot">已有账号？<a @click="$router.replace('/login')">返回登录</a></p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { register, getDict } from '../api'
import { userStore } from '../store/user'

const router = useRouter()
const majors = ref([])
const genders = ['男', '女', '保密']
const saving = ref(false)

const form = reactive({
  account: '', password: '', nickname: '',
  realName: '', studentNo: '', majorId: null, grade: '2024', gender: '保密'
})

async function submit() {
  if (!form.account) return alert('请填写手机号或学号')
  if (!form.password || form.password.length < 6) return alert('密码至少 6 位')
  if (!form.nickname) return alert('请填写昵称')

  saving.value = true
  try {
    const data = await register({ ...form })
    userStore.save(data.token, data.user)
    alert('注册成功')
    router.replace('/')
  } catch (e) {
    alert(e.message || '注册失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const dict = await getDict()
    majors.value = dict.majors || []
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.nav {
  position: sticky; top: 0; z-index: 10;
  height: 48px; display: flex; align-items: center; justify-content: space-between;
  padding: 0 12px; background: rgba(247, 244, 239, 0.94);
  backdrop-filter: blur(10px); font-size: 14.5px; font-weight: 600;
}
.back { border: none; background: none; font-size: 24px; width: 32px; text-align: left; line-height: 1; }
.card { margin-bottom: 12px; }
.card-title { font-size: 13.5px; font-weight: 650; margin-bottom: 14px; display: flex; align-items: baseline; }
.sub { font-size: 11px; color: #9aa3ad; font-weight: 400; margin-left: 8px; }
.seg { display: flex; gap: 8px; }
.seg button {
  flex: 1; height: 36px; border-radius: 9px; border: 1px solid var(--line);
  background: #fff; font-size: 13px; color: var(--ink-2);
}
.seg button.on { border-color: var(--brand); background: var(--brand-soft); color: var(--brand); font-weight: 600; }
.foot { text-align: center; font-size: 12px; color: var(--ink-3); margin-top: 14px; }
.foot a { color: var(--brand); }
</style>
