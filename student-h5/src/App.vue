<template>
  <div class="app-shell">
    <router-view />
    <tab-bar v-if="showTab" />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import TabBar from './components/TabBar.vue'
import { getMe } from './api'
import { userStore } from './store/user'

const route = useRoute()

// 刷新页面后 localStorage 里有 token 但没有用户对象，
// 这里补拉一次，避免"已登录却显示未登录"
onMounted(async () => {
  if (userStore.token && !userStore.user) {
    try {
      userStore.update(await getMe())
    } catch (e) {
      userStore.logout()
    }
  }
})
// 详情页、发布页、登录页不显示底部导航
const HIDE = ['book-detail', 'publish', 'login', 'register', 'chat', 'profile', 'credit', 'notice', 'reviews', 'user']
const showTab = computed(() => !HIDE.includes(route.name))
</script>