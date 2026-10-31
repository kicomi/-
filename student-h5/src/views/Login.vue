<template>
  <div class="login">
    <div class="brand">
      <div class="logo">书</div>
      <h1>校园二手书</h1>
      <p>同校同专业，把闲置教材传给下一个人</p>
    </div>

    <div class="actions">
      <button class="btn-main" @click="quickLogin">微信一键登录</button>
      <small class="hint">首次使用会自动创建账号，从 80 分信用起步</small>

      <div class="other">
        <span @click="goRegister">注册新账号</span>
        <i>|</i>
        <span @click="showAccounts = !showAccounts">用演示账号体验</span>
      </div>
    </div>

    <div class="demo" v-if="showAccounts">
      <p class="demo-tip">答辩演示用，三种角色对应不同数据状态</p>
      <div class="acc" v-for="a in accounts" :key="a.code" @click="login(a.code)">
        <div class="acc-left">
          <b>{{ a.name }}</b>
          <small>{{ a.desc }}</small>
        </div>
        <span class="go">进入 ›</span>
      </div>
    </div>

    <p class="foot">登录即表示同意校园二手书的使用规范</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { wxLogin } from '../api'
import { userStore } from '../store/user'

const router = useRouter()
const showAccounts = ref(false)

const accounts = [
  { code: 'demo-002', name: '买家视角', desc: '有浏览记录，推荐更准' },
  { code: 'demo-010', name: '卖家视角', desc: '已发布多本闲置' },
  { code: 'demo-030', name: '新用户', desc: '冷启动，看热门与分类' }
]

function quickLogin() {
  login('demo-002')
}

async function login(code) {
  try {
    const data = await wxLogin(code)
    userStore.save(data.token, data.user)
    router.replace('/')
  } catch (e) {
    alert(e.message || '登录失败')
  }
}

function goRegister() {
  router.push('/register')
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  padding: 0 24px;
  box-sizing: border-box;
  background:
    radial-gradient(280px 170px at 78% -8%, #dcefe4 0%, transparent 62%),
    linear-gradient(180deg, #f2f8f5 0%, #f7f4ef 230px);
}

.brand { padding: 60px 0 32px; }
.logo {
  width: 52px; height: 52px; border-radius: 14px;
  background: linear-gradient(140deg, #3f9d74, #2f7d5b);
  color: #fff; display: grid; place-items: center;
  font-size: 23px; font-family: 'Songti SC', serif;
  box-shadow: 0 16px 36px -18px rgba(47, 125, 91, .8);
}
.brand h1 { margin: 16px 0 0; font-size: 23px; font-weight: 700; letter-spacing: 1px; }
.brand p { margin: 6px 0 0; font-size: 12.5px; color: #8f9a94; }

.actions { margin-top: 10px; }
.btn-main {
  width: 100%; height: 48px; border-radius: 24px; border: none;
  background: var(--brand); color: #fff; font-size: 15px; font-weight: 600;
  box-shadow: 0 16px 34px -20px rgba(47, 125, 91, .9);
}
.hint { display: block; margin-top: 9px; text-align: center; font-size: 11px; color: var(--ink-3); }

.other {
  margin-top: 17px; display: flex; justify-content: center; align-items: center;
  gap: 11px; font-size: 12.5px; color: var(--brand);
}
.other i { font-style: normal; color: #d6d0c6; font-size: 11px; }
.other span { cursor: pointer; }

.demo { margin-top: 28px; }
.demo-tip { margin: 0 0 8px; text-align: center; font-size: 10.5px; color: #b9b4ab; }
.acc {
  display: flex; align-items: center; background: #fff; border-radius: 10px;
  padding: 11px 12px; margin-bottom: 7px; cursor: pointer;
}
.acc-left { flex: 1; }
.acc-left b { font-size: 13.5px; }
.acc-left small { display: block; font-size: 10.5px; color: var(--ink-3); margin-top: 2px; }
.go { font-size: 12px; color: var(--brand); }

.foot { margin-top: 30px; text-align: center; font-size: 10px; color: #c4bfb7; }
</style>
