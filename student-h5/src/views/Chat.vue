<template>
  <div class="chat-page">
    <header class="nav">
      <button class="back" @click="$router.back()">‹</button>
      <span class="title">{{ peerId ? (bookTitle ? '《' + bookTitle + '》' : peerName) : '消息' }}</span>
      <div style="width: 32px"></div>
    </header>

    <!-- 会话列表 -->
    <div v-if="!peerId" class="page">
      <div class="conv" v-for="c in list" :key="c.peerId" @click="open(c)">
        <div class="conv-avatar">{{ c.peerNickname.slice(0, 1) }}</div>
        <div class="conv-main">
          <div class="conv-top"><b>{{ c.peerNickname }}</b><small>{{ fmt(c.lastTime) }}</small></div>
          <div class="conv-bottom">
            <span class="last">{{ c.lastContent }}</span>
            <span v-if="c.unread > 0" class="badge">{{ c.unread }}</span>
          </div>
        </div>
      </div>
      <div v-if="!list.length" class="empty"><div class="icon">💬</div>还没有聊天记录</div>
    </div>

    <!-- 对话 -->
    <div v-else class="dialog">
      <div class="book-bar" v-if="bookTitle" @click="goBook">
        <span class="book-tag">在聊</span>
        <span class="book-name">《{{ bookTitle }}》</span>
        <span class="book-arrow">›</span>
      </div>

      <div class="msgs" ref="msgRef">
        <div v-for="m in messages" :key="m.id" class="msg-row" :class="m.fromUserId === myId ? 'mine' : 'theirs'">
          <!-- 只保留头像，不显示名字 -->
          <div class="who" v-if="m.fromUserId !== myId">
            <div class="avatar-sm">{{ peerName.slice(0, 1) }}</div>
          </div>

          <div class="msg-main">
            <div class="bubble">{{ m.content }}</div>
            <span class="time">{{ fmt(m.createdAt) }}</span>
          </div>

          <div class="who" v-if="m.fromUserId === myId">
            <div class="avatar-sm mine">{{ myName.slice(0, 1) }}</div>
          </div>
        </div>

        <div v-if="!messages.length" class="empty"><div class="icon">👋</div>发条消息打个招呼吧</div>
      </div>

      <div class="quick-bar" v-if="showQuick">
        <span v-for="q in quickPhrases" :key="q" @click="usePhrase(q)">{{ q }}</span>
      </div>

      <div class="input-bar">
        <button class="quick-toggle" @click="showQuick = !showQuick">≡</button>
        <input v-model="input" placeholder="说点什么…" @keyup.enter="send" />
        <button class="send" :disabled="sending" @click="send">发送</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { conversations, chatHistory, sendMessage } from '../api'
import { userStore } from '../store/user'

const route = useRoute()
const router = useRouter()

const peerId = ref(route.query.peerId ? Number(route.query.peerId) : null)
const peerName = ref(route.query.peerName || '对方')
const bookId = ref(route.query.bookId ? Number(route.query.bookId) : null)
const bookTitle = ref(route.query.bookTitle || '')
const myId = (userStore.user && userStore.user.id) || null
const myName = (userStore.user && userStore.user.nickname) || '我'

const list = ref([])
const messages = ref([])
const input = ref('')
const showQuick = ref(false)
// 校园场景话术高度重复
const quickPhrases = ['这本书还在吗？', '书况怎么样？', '可以便宜点吗？', '今晚方便见面吗？']

function usePhrase(t) {
  input.value = t
  showQuick.value = false
}
const sending = ref(false)
const msgRef = ref(null)

function fmt(t) {
  if (!t) return ''
  return String(t).slice(5, 16)
}

function goBook() {
  if (bookId.value) router.push('/book/' + bookId.value)
}

function open(c) {
  router.push({ path: '/chat', query: { peerId: c.peerId, peerName: c.peerNickname } })
}

async function loadConversations() {
  try {
    list.value = (await conversations()) || []
  } catch (e) {
    list.value = []
  }
}

async function loadHistory() {
  try {
    messages.value = (await chatHistory(peerId.value, bookId.value)) || []
    await nextTick()
    if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight
  } catch (e) { /* ignore */ }
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  sending.value = true
  try {
    await sendMessage({ toUserId: peerId.value, bookId: bookId.value, content: text })
    input.value = ''
    await loadHistory()
  } catch (e) {
    alert(e.message || '发送失败')
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  if (peerId.value) loadHistory()
  else loadConversations()
})
</script>

<style scoped>
.chat-page { display: flex; flex-direction: column; height: 100vh; }
.nav {
  height: 48px; display: flex; align-items: center; justify-content: space-between;
  padding: 0 12px; font-size: 15px; font-weight: 600;
  background: #f7f4ef;
  border-bottom: 1px solid rgba(0, 0, 0, .04);
}
.back { border: none; background: none; font-size: 24px; width: 32px; text-align: left; line-height: 1; }
.title { max-width: 70%; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }

.conv {
  display: flex; align-items: center; gap: 12px; background: #fff;
  border-radius: 14px; padding: 12px; margin-bottom: 8px;
}
.conv-avatar {
  width: 44px; height: 44px; border-radius: 50%; background: var(--brand-soft);
  color: var(--brand); display: grid; place-items: center; font-weight: 600; flex: none;
}
.conv-main { flex: 1; min-width: 0; }
.conv-top { display: flex; justify-content: space-between; align-items: baseline; }
.conv-top b { font-size: 14px; }
.conv-top small { font-size: 11px; color: var(--ink-3); }
.conv-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 3px; }
.last { font-size: 12.5px; color: var(--ink-3); overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.badge {
  min-width: 18px; height: 18px; border-radius: 9px; background: var(--danger);
  color: #fff; font-size: 10.5px; display: grid; place-items: center; padding: 0 5px;
}

.dialog { flex: 1; display: flex; flex-direction: column; min-height: 0; }

.book-bar {
  display: flex; align-items: center; gap: 6px; padding: 9px 16px;
  background: #fff; border-bottom: 1px solid #f0ebe3; flex: none;
}
.book-tag {
  font-size: 10px; color: var(--brand); background: var(--brand-soft);
  border-radius: 4px; padding: 1px 5px;
}
.book-name { flex: 1; font-size: 12.5px; color: #3c444d; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.book-arrow { color: #b9b4ab; }

.msgs { flex: 1; overflow-y: auto; padding: 16px 12px 8px; }
.msg-row { display: flex; align-items: flex-start; margin-bottom: 16px; }
.msg-row.theirs { justify-content: flex-start; }
.msg-row.mine { justify-content: flex-end; }

.who { display: flex; flex-direction: column; align-items: center; width: 32px; flex: none; }
.avatar-sm {
  width: 32px; height: 32px; border-radius: 50%; overflow: hidden;
  background: var(--brand-soft); color: var(--brand);
  display: grid; place-items: center; font-size: 13px;
}

.msg-main { display: flex; flex-direction: column; max-width: 250px; margin: 0 8px; }
.mine .msg-main { align-items: flex-end; }

.bubble {
  padding: 11px 14px; border-radius: 10px;
  font-size: 16px; font-weight: 500; letter-spacing: 0.2px;
  line-height: 1.5; word-break: break-word;
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
}
.mine .bubble { background: var(--brand); color: #fff; border-bottom-right-radius: 3px; }
.mine .bubble { -webkit-font-smoothing: antialiased; }
.theirs .bubble {
  background: #fff; color: #14171a; border-bottom-left-radius: 3px;
  border: 1px solid #ece7df;
}
.time { font-size: 11.5px; color: #9aa3ad; margin-top: 5px; }

.input-bar {
  display: flex; gap: 10px; padding: 10px 14px calc(10px + env(safe-area-inset-bottom));
  background: #fff; border-top: 1px solid var(--line); flex: none;
}
.quick-bar { display: flex; flex-wrap: wrap; gap: 6px; padding: 8px 14px 2px; background: #fff; }
.quick-bar span {
  font-size: 11.5px; color: var(--ink-2); background: #f5f2ec;
  border-radius: 10px; padding: 4px 10px; cursor: pointer;
}
.quick-toggle {
  width: 28px; border: none; background: none; color: var(--ink-3); font-size: 16px;
}

.input-bar input { flex: 1; height: 40px; border-radius: 20px; padding: 0 15px; background: #f5f2ec; border: none; font-size: 15px; }
.send {
  height: 38px; padding: 0 18px; border-radius: 19px; border: none;
  background: var(--brand); color: #fff; font-size: 13.5px; font-weight: 600;
}
.send:disabled { background: #b9cdc3; }
</style>