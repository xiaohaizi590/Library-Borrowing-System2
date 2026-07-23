<template>
  <div>
    <PageHeader title="用户管理" />

    <LoadingSpinner v-if="loading" />

    <div v-else class="bg-white rounded-lg shadow-sm overflow-hidden">
      <table class="min-w-full divide-y divide-gray-200">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">用户ID</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">用户名</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">邮箱</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">手机号</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">角色</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">注册时间</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-gray-200">
          <tr v-for="user in users" :key="user.id" class="hover:bg-gray-50">
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.id }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ user.username }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.email || '-' }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.phoneNumber }}</td>
            <td class="px-6 py-4 whitespace-nowrap">
              <StatusBadge :type="user.role" :map="roleMap" />
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ formatDateTime(user.createTime) }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
              <button
                @click="handleDelete(user)"
                :disabled="user.role === 'ADMIN'"
                :class="user.role === 'ADMIN' ? 'text-gray-400 cursor-not-allowed' : 'text-red-600 hover:text-red-900 px-3 py-1 bg-red-50 rounded hover:bg-red-100'"
              >
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <Pagination
      :current-page="currentPage"
      :total-pages="totalPages"
      @prev="prevPage"
      @next="nextPage"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAllUsers, deleteUser } from '../services/userService'
import PageHeader from '../components/PageHeader.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import Pagination from '../components/Pagination.vue'
import StatusBadge from '../components/StatusBadge.vue'

const roleMap = {
  ADMIN: { class: 'bg-red-100 text-red-800', text: '管理员' },
  USER: { class: 'bg-green-100 text-green-800', text: '普通用户' }
}

const loading = ref(false)
const users = ref([])
const currentPage = ref(0)
const totalPages = ref(0)

async function fetchUsers(page = 0) {
  loading.value = true
  try {
    const response = await getAllUsers(page, 10)
    
    if (response.code === 200) {
      users.value = response.data.content
      totalPages.value = response.data.totalPages
      currentPage.value = response.data.number
    }
  } catch (err) {
    console.error('获取用户失败:', err)
  } finally {
    loading.value = false
  }
}

function prevPage() {
  if (currentPage.value > 0) {
    fetchUsers(currentPage.value - 1)
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value - 1) {
    fetchUsers(currentPage.value + 1)
  }
}

function formatDateTime(dateTime) {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

function handleDelete(user) {
  if (user.role === 'ADMIN') return
  
  if (confirm(`确定要删除用户 "${user.username}" 吗？`)) {
    deleteUser(user.id).then(response => {
      if (response.code === 200) {
        fetchUsers(currentPage.value)
      }
    })
  }
}

onMounted(() => {
  fetchUsers()
})
</script>
