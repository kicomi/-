import { reactive } from 'vue'

const KEY_TOKEN = 'campus_book_token'
const KEY_USER = 'campus_book_admin'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(KEY_USER) || 'null')
  } catch (e) {
    return null
  }
}

export const auth = reactive({
  token: localStorage.getItem(KEY_TOKEN) || '',
  user: readUser(),

  save(token, user) {
    this.token = token
    this.user = user
    localStorage.setItem(KEY_TOKEN, token)
    localStorage.setItem(KEY_USER, JSON.stringify(user || {}))
  },

  logout() {
    this.token = ''
    this.user = null
    localStorage.removeItem(KEY_TOKEN)
    localStorage.removeItem(KEY_USER)
  }
})
