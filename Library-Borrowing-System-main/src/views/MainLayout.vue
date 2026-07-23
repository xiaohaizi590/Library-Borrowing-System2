<template>
  <div class="min-h-screen bg-gray-100 flex">
    <aside class="w-64 bg-white shadow-md flex flex-col fixed h-full">
      <div class="p-4 border-b border-gray-200">
        <div class="flex items-center">
          <BookOpen class="w-8 h-8 text-blue-500" />
          <span class="ml-3 text-xl font-bold text-gray-800">图书管理系统</span>
        </div>
      </div>

      <nav class="flex-1 p-4 space-y-1">
        <router-link
          to="/"
          class="nav-link"
          :class="{ 'bg-blue-50 text-blue-600': $route.name === 'BookList' }"
        >
          <BookMarked class="w-5 h-5" />
          <span>图书浏览</span>
        </router-link>

        <router-link
          to="/my-borrow"
          class="nav-link"
          :class="{ 'bg-blue-50 text-blue-600': $route.name === 'MyBorrow' }"
        >
          <BookOpenCheck class="w-5 h-5" />
          <span>我的借阅</span>
        </router-link>

        <router-link
          to="/profile"
          class="nav-link"
          :class="{ 'bg-blue-50 text-blue-600': $route.name === 'Profile' }"
        >
          <User class="w-5 h-5" />
          <span>个人信息</span>
        </router-link>

        <div v-if="isAdmin()" class="mt-6">
          <div class="text-xs font-semibold text-gray-400 uppercase tracking-wider px-3 py-2">管理</div>
          <router-link
            to="/book-manage"
            class="nav-link"
            :class="{ 'bg-blue-50 text-blue-600': $route.name === 'BookManage' }"
          >
            <Library class="w-5 h-5" />
            <span>图书管理</span>
          </router-link>

          <router-link
            to="/user-manage"
            class="nav-link"
            :class="{ 'bg-blue-50 text-blue-600': $route.name === 'UserManage' }"
          >
            <Users class="w-5 h-5" />
            <span>用户管理</span>
          </router-link>

          <router-link
            to="/borrow-manage"
            class="nav-link"
            :class="{ 'bg-blue-50 text-blue-600': $route.name === 'BorrowManage' }"
          >
            <ClipboardList class="w-5 h-5" />
            <span>借阅管理</span>
          </router-link>
        </div>
      </nav>

      <div class="p-4 border-t border-gray-200">
        <div class="flex items-center space-x-3">
          <div class="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
            <User class="w-5 h-5 text-blue-500" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-gray-800 truncate">{{ user?.username }}</p>
            <span
              :class="isAdmin() ? 'bg-red-100 text-red-600' : 'bg-blue-100 text-blue-600'"
              class="inline-block px-2 py-0.5 text-xs rounded-full"
            >
              {{ isAdmin() ? '管理员' : '用户' }}
            </span>
          </div>
        </div>
        <button
          @click="handleLogout"
          class="w-full mt-4 flex items-center justify-center space-x-2 px-3 py-2 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
        >
          <LogOut class="w-4 h-4" />
          <span>退出登录</span>
        </button>
      </div>
    </aside>

    <main class="flex-1 ml-64">
      <div class="p-6">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { BookOpen, BookMarked, BookOpenCheck, User, Library, Users, ClipboardList, LogOut } from 'lucide-vue-next'
import { getUser, isAdmin, removeToken, removeUser } from '../utils/auth'

const router = useRouter()
const user = computed(() => getUser())

function handleLogout() {
  removeToken()
  removeUser()
  router.push('/login')
}
</script>

<style scoped>
.nav-link {
  @apply flex items-center px-3 py-2.5 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded-lg transition-colors;
  gap: 0.75rem;
}
</style>