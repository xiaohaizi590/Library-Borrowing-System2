<template>
  <div>
    <PageHeader title="回收站">
      <button
        @click="handleCleanExpired"
        class="flex items-center space-x-1 px-4 py-2 bg-orange-500 text-white rounded-lg hover:bg-orange-600"
      >
        <Trash2 class="w-4 h-4" />
        <span>清理30天前记录</span>
      </button>
    </PageHeader>

    <div class="mb-6">
      <div class="flex space-x-2">
        <button
          @click="activeTab = 'books'"
          :class="activeTab === 'books' ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'"
          class="px-4 py-2 rounded-lg font-medium"
        >
          已删除图书 ({{ bookCount }})
        </button>
        <button
          @click="activeTab = 'users'"
          :class="activeTab === 'users' ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'"
          class="px-4 py-2 rounded-lg font-medium"
        >
          已删除用户 ({{ userCount }})
        </button>
      </div>
    </div>

    <LoadingSpinner v-if="loading" />

    <template v-else>
      <div v-if="activeTab === 'books'">
        <EmptyState v-if="books.length === 0" title="暂无已删除图书" description="图书删除后会显示在这里" />
        
        <div v-else class="bg-white rounded-lg shadow-sm overflow-hidden">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">书名</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">作者</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ISBN</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">分类</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">删除时间</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr v-for="book in books" :key="book.id" class="hover:bg-gray-50">
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ book.title }}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ book.author }}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ book.isbn || '-' }}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ book.category || '未分类' }}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ formatDateTime(book.deleteTime) }}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <button
                    @click="handleRestoreBook(book)"
                    class="text-green-600 hover:text-green-900 px-3 py-1 bg-green-50 rounded hover:bg-green-100"
                  >
                    恢复
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <Pagination
          v-if="totalBookPages > 1"
          :current-page="bookPage"
          :total-pages="totalBookPages"
          @prev="prevBookPage"
          @next="nextBookPage"
        />
      </div>

      <div v-if="activeTab === 'users'">
        <EmptyState v-if="users.length === 0" title="暂无已删除用户" description="用户删除后会显示在这里" />
        
        <div v-else class="bg-white rounded-lg shadow-sm overflow-hidden">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">用户ID</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">用户名</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">邮箱</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">手机号</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">删除时间</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr v-for="user in users" :key="user.id" class="hover:bg-gray-50">
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.id }}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ user.username }}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.email || '-' }}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.phoneNumber }}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ formatDateTime(user.deleteTime) }}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <button
                    @click="handleRestoreUser(user)"
                    class="text-green-600 hover:text-green-900 px-3 py-1 bg-green-50 rounded hover:bg-green-100"
                  >
                    恢复
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <Pagination
          v-if="totalUserPages > 1"
          :current-page="userPage"
          :total-pages="totalUserPages"
          @prev="prevUserPage"
          @next="nextUserPage"
        />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Trash2 } from 'lucide-vue-next'
import { getDeletedBooks, restoreBook, cleanExpiredBooks } from '../services/bookService'
import { getDeletedUsers, restoreUser, cleanExpiredUsers } from '../services/userService'
import PageHeader from '../components/PageHeader.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import Pagination from '../components/Pagination.vue'
import EmptyState from '../components/EmptyState.vue'

const loading = ref(false)
const activeTab = ref('books')

const books = ref([])
const bookPage = ref(0)
const totalBookPages = ref(0)
const bookCount = ref(0)

const users = ref([])
const userPage = ref(0)
const totalUserPages = ref(0)
const userCount = ref(0)

async function fetchBooks(page = 0) {
  try {
    const response = await getDeletedBooks(page, 10)
    if (response.code === 200) {
      books.value = response.data.content
      totalBookPages.value = response.data.totalPages
      bookPage.value = response.data.number
      bookCount.value = response.data.totalElements
    }
  } catch (err) {
    console.error('获取已删除图书失败:', err)
  }
}

async function fetchUsers(page = 0) {
  try {
    const response = await getDeletedUsers(page, 10)
    if (response.code === 200) {
      users.value = response.data.content
      totalUserPages.value = response.data.totalPages
      userPage.value = response.data.number
      userCount.value = response.data.totalElements
    }
  } catch (err) {
    console.error('获取已删除用户失败:', err)
  }
}

function prevBookPage() {
  if (bookPage.value > 0) {
    fetchBooks(bookPage.value - 1)
  }
}

function nextBookPage() {
  if (bookPage.value < totalBookPages.value - 1) {
    fetchBooks(bookPage.value + 1)
  }
}

function prevUserPage() {
  if (userPage.value > 0) {
    fetchUsers(userPage.value - 1)
  }
}

function nextUserPage() {
  if (userPage.value < totalUserPages.value - 1) {
    fetchUsers(userPage.value + 1)
  }
}

function formatDateTime(dateTime) {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

function handleRestoreBook(book) {
  if (confirm(`确定要恢复图书 "${book.title}" 吗？`)) {
    restoreBook(book.id).then(response => {
      if (response.code === 200) {
        fetchBooks(bookPage.value)
      }
    })
  }
}

function handleRestoreUser(user) {
  if (confirm(`确定要恢复用户 "${user.username}" 吗？`)) {
    restoreUser(user.id).then(response => {
      if (response.code === 200) {
        fetchUsers(userPage.value)
      }
    })
  }
}

function handleCleanExpired() {
  if (confirm('确定要清理30天前的所有删除记录吗？此操作不可恢复！')) {
    Promise.all([
      cleanExpiredBooks(30),
      cleanExpiredUsers(30)
    ]).then(() => {
      fetchBooks(0)
      fetchUsers(0)
    })
  }
}

onMounted(() => {
  fetchBooks()
  fetchUsers()
})
</script>