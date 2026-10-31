<template>
  <div class="profile">
    <header class="nav">
      <button class="back" @click="$router.back()">‹</button>
      <span>编辑资料</span>
      <div style="width: 32px"></div>
    </header>

    <div class="page">
      <div class="card avatar-card">
        <div class="avatar-wrap" @click="pickAvatar">
          <img v-if="form.avatar" :src="form.avatar" class="avatar-img" />
          <div v-else class="avatar-txt">{{ (form.nickname || '书').slice(0, 1) }}</div>
          <div class="avatar-mask">{{ uploading ? '上传中…' : '换头像' }}</div>
        </div>
        <input ref="fileRef" type="file" accept="image/*" style="display: none" @change="onFile" />
      </div>

      <div class="card">
        <div class="field">
          <label>昵称</label>
          <input v-model="form.nickname" placeholder="取个好记的名字" />
        </div>
        <div class="field">
          <label>个性签名</label>
          <textarea v-model="form.signature" rows="2" placeholder="一句话介绍自己" />
        </div>
        <div class="field">
          <label>专业（可直接输入）</label>
          <input v-model="form.customMajor" placeholder="例如：数据科学与大数据技术" />
          <div class="major-chips">
            <span v-for="m in majors" :key="m.id" class="major-chip" @click="form.customMajor = m.name">{{ m.name }}</span>
          </div>
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

      <button class="btn btn-primary btn-block" :disabled="saving" @click="save">
        {{ saving ? '保存中…' : '保存' }}
      </button>
      <p class="tip">真实姓名与学号属于实名信息，注册后不可自行修改，仅管理员可见</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMe, updateProfile, getDict, uploadImage } from '../api'
import { userStore } from '../store/user'

const router = useRouter()
const fileRef = ref(null)
const majors = ref([])
const genders = ['男', '女', '保密']
const saving = ref(false)
const uploading = ref(false)

const form = reactive({
  nickname: '', signature: '', avatar: '', customMajor: '', grade: '', gender: '保密'
})

function pickAvatar() {
  fileRef.value && fileRef.value.click()
}

async function onFile(e) {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  uploading.value = true
  try {
    const up = await uploadImage(file)
    form.avatar = up.url
  } catch (err) {
    alert(err.message || '上传失败')
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

async function save() {
  if (!form.nickname.trim()) return alert('昵称不能为空')
  saving.value = true
  try {
    const u = await updateProfile({ ...form })
    userStore.update(u)
    alert('已保存')
    router.back()
  } catch (e) {
    alert(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const dict = await getDict()
    majors.value = dict.majors || []
  } catch (e) { /* ignore */ }

  try {
    const u = await getMe()
    Object.assign(form, {
      nickname: u.nickname || '',
      signature: u.signature || '',
      avatar: u.avatar || '',
      customMajor: u.majorName || u.customMajor || '',
      grade: u.grade || '',
      gender: u.gender || '保密'
    })
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
.avatar-card { display: flex; justify-content: center; padding: 22px; }
.avatar-wrap {
  position: relative; width: 92px; height: 92px; border-radius: 50%;
  overflow: hidden; background: var(--brand-soft); cursor: pointer;
}
.avatar-img { width: 100%; height: 100%; object-fit: cover; display: block; }
.avatar-txt {
  width: 100%; height: 100%; display: grid; place-items: center;
  font-size: 32px; color: var(--brand); font-weight: 600;
}
.avatar-mask {
  position: absolute; left: 0; right: 0; bottom: 0; height: 24px;
  line-height: 24px; text-align: center; font-size: 10.5px;
  color: #fff; background: rgba(0, 0, 0, 0.38);
}
.major-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.major-chip {
  font-size: 11px; color: var(--brand); background: var(--brand-soft);
  border-radius: 10px; padding: 3px 9px; cursor: pointer;
}
.seg { display: flex; gap: 8px; }
.seg button {
  flex: 1; height: 36px; border-radius: 9px; border: 1px solid var(--line);
  background: #fff; font-size: 13px; color: var(--ink-2);
}
.seg button.on { border-color: var(--brand); background: var(--brand-soft); color: var(--brand); font-weight: 600; }
.tip { text-align: center; font-size: 11px; color: var(--ink-3); margin-top: 14px; line-height: 1.6; }
</style>