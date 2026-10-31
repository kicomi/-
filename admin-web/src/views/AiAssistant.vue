<template>
  <div class="page ai-page">
    <div class="page-head">
      <div>
        <h2>运营助手</h2>
        <p>基于平台实时数据回答问题；配置大模型后会自动切换为模型回答</p>
      </div>
      <el-tag :type="modelTagType" effect="light" size="small">{{ modelLabel }}</el-tag>
    </div>

    <div class="ai-layout">
      <div class="panel chat-panel">
        <div class="chat-body" ref="bodyRef">
          <div class="msg" v-for="(m, i) in messages" :key="i" :class="m.role">
            <div class="bubble">
              <div class="bubble-text">{{ m.content }}</div>
              <div class="bubble-meta" v-if="m.role === 'assistant' && m.meta">{{ m.meta }}</div>
            </div>
          </div>
          <div class="msg assistant" v-if="thinking">
            <div class="bubble typing"><i></i><i></i><i></i></div>
          </div>
        </div>

        <div class="chat-input">
          <el-input v-model="input" type="textarea" :rows="2" resize="none"
                    placeholder="例如：最近成交情况怎么样？  回车发送"
                    @keydown.enter.prevent="send" />
          <el-button type="primary" :loading="thinking" @click="send">发送</el-button>
        </div>
      </div>

      <div class="panel side-panel">
        <div class="panel-title">可以这样问</div>
        <div class="quick-list">
          <div class="quick" v-for="q in quick" :key="q" @click="ask(q)">
            {{ q }}
          </div>
        </div>

        <div class="panel-title" style="margin-top: 20px">能力说明</div>
        <ul class="ability">
          <li>读取实时统计：用户、书籍、订单、成交额、转化率</li>
          <li>解读推荐算法指标（Recall@10、相对热门基线提升）</li>
          <li>给出可执行的运营建议</li>
        </ul>
        <div class="hint-box">
          当前为<b>本地规则分析</b>模式。在 <code>application.yml</code> 的
          <code>app.ai</code> 下填好 base-url 与 api-key 即可接入大模型，调用失败会自动回退。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { aiChat, aiQuickQuestions } from '../api'

const messages = ref([])
const input = ref('')
const thinking = ref(false)
const quick = ref([])
const bodyRef = ref(null)
const currentModel = ref('')

const modelLabel = computed(() => currentModel.value || '本地规则分析')
const modelTagType = computed(() => (currentModel.value && currentModel.value.indexOf('大模型') >= 0 ? 'success' : 'info'))

async function scrollBottom() {
  await nextTick()
  if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
}

async function ask(q) {
  input.value = q
  await send()
}

async function send() {
  const text = input.value.trim()
  if (!text || thinking.value) return

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  thinking.value = true
  await scrollBottom()

  try {
    const data = await aiChat(text)
    currentModel.value = data.model
    messages.value.push({
      role: 'assistant',
      content: data.reply,
      meta: data.model + ' · ' + data.elapsedMs + 'ms'
    })
  } catch (e) {
    ElMessage.error('助手暂时不可用')
    messages.value.push({ role: 'assistant', content: '抱歉，服务暂时不可用，请稍后再试。' })
  } finally {
    thinking.value = false
    await scrollBottom()
  }
}

onMounted(async () => {
  try {
    quick.value = (await aiQuickQuestions()) || []
  } catch (e) {
    quick.value = []
  }
  messages.value.push({
    role: 'assistant',
    content: '你好，我是运营助手。可以问我成交情况、价位分布、推荐算法效果，或者直接要一份运营建议。',
    meta: '系统'
  })
})
</script>

<style scoped>
.ai-page {
  height: 100%;
}

.ai-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 22px;
  background: #fbfcfd;
  min-height: 320px;
}

.msg {
  display: flex;
  margin-bottom: 14px;
}

.msg.user {
  justify-content: flex-end;
}

.bubble {
  max-width: 76%;
  padding: 11px 14px;
  border-radius: 12px;
  font-size: 13.5px;
  line-height: 1.7;
}

.msg.assistant .bubble {
  background: #fff;
  border: 1px solid #e9edf3;
  color: #334155;
  border-top-left-radius: 4px;
}

.msg.user .bubble {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  border-top-right-radius: 4px;
}

.bubble-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-meta {
  margin-top: 6px;
  font-size: 11px;
  color: #a8b3c0;
}

.typing {
  display: flex;
  gap: 4px;
  align-items: center;
}

.typing i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #cbd5e1;
  animation: bounce 1.2s infinite;
}

.typing i:nth-child(2) { animation-delay: 0.15s; }
.typing i:nth-child(3) { animation-delay: 0.3s; }

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.chat-input {
  display: flex;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid #eef2f7;
  background: #fff;
}

.chat-input .el-button {
  height: auto;
  padding: 0 22px;
}

.side-panel {
  display: flex;
  flex-direction: column;
}

.quick-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick {
  font-size: 12.5px;
  color: #475569;
  background: #f5f8fb;
  border: 1px solid #e9edf3;
  border-radius: 9px;
  padding: 9px 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.quick:hover {
  border-color: #10b981;
  color: #059669;
  background: #f0fdf7;
}

.ability {
  margin: 0;
  padding-left: 18px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.9;
}

.hint-box {
  margin-top: 18px;
  font-size: 11.5px;
  color: #94a3b8;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  border-radius: 9px;
  padding: 10px 12px;
  line-height: 1.7;
}

.hint-box b {
  color: #059669;
}

.hint-box code {
  background: #eef2f7;
  border-radius: 4px;
  padding: 0 4px;
  font-size: 11px;
}
</style>
