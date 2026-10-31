import { reactive } from 'vue'

const KEY_TOKEN = 'cb_student_token'
const KEY_USER = 'cb_student_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(KEY_USER) || 'null')
  } catch (e) {
    return null
  }
}

export const userStore = reactive({
  token: localStorage.getItem(KEY_TOKEN) || '',
  user: readUser(),

  get isLogin() {
    return !!this.token
  },

  save(token, user) {
    this.token = token
    this.user = user
    localStorage.setItem(KEY_TOKEN, token)
    localStorage.setItem(KEY_USER, JSON.stringify(user || {}))
  },

  update(user) {
    this.user = user
    localStorage.setItem(KEY_USER, JSON.stringify(user || {}))
  },

  logout() {
    this.token = ''
    this.user = null
    localStorage.removeItem(KEY_TOKEN)
    localStorage.removeItem(KEY_USER)
  }
})
