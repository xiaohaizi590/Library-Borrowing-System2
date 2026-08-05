<template>
  <div>
    <PageHeader title="用户管理" />

    <!-- 搜索栏 -->
    <div class="bg-white rounded-lg shadow-sm p-4 mb-6">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[200px]">
          <label class="block text-sm font-medium text-gray-700 mb-2">搜索类型</label>
          <select
            v-model="searchType"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="username">按用户名</option>
           <!--  <option value="email">按邮箱</option>-->
            <!--  <option value="phone">按手机号</option>-->
          </select>
        </div>
        <div class="flex-1 min-w-[200px]">
          <label class="block text-sm font-medium text-gray-700 mb-2">搜索关键词</label>
          <div class="relative">
            <input
              v-model="searchKeyword"
              type="text"
              :placeholder="'输入' + (searchType === 'username' ? '用户名' : searchType === 'email' ? '邮箱' : '手机号') + '...'"
              class="w-full pl-4 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              @keyup.enter="handleSearch"
            />
          </div>
        </div>
        <div class="flex items-end gap-2">
          <button
            @click="handleSearch"
            class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
          >
            <Search class="w-4 h-4" />
          </button>
          <button
            @click="handleReset"
            class="px-4 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 transition-colors"
          >
            <RotateCcw class="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>

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
                @click="openResetPwdModal(user)"
                :disabled="user.role === 'ADMIN'"
                :class="user.role === 'ADMIN' ? 'text-gray-400 cursor-not-allowed' : 'text-blue-600 hover:text-blue-900 px-3 py-1 bg-blue-50 rounded hover:bg-blue-100 mr-2'"
              >
                重置密码
              </button>
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

    <Modal :visible="showResetPwdModal" :title="'重置密码 - ' + resetPwdUser?.username" @close="showResetPwdModal = false">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">新密码</label>
          <input v-model="resetPwdForm.newPassword" type="password"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            placeholder="请输入新密码（至少6位）" />
        </div>
        <div class="flex justify-end space-x-3 pt-2">
          <button @click="showResetPwdModal = false"
            class="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200">取消</button>
          <button @click="handleResetPassword"
            class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-50"
            :disabled="resettingPwd">确认</button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, RotateCcw } from 'lucide-vue-next'
import { getAllUsers, deleteUser, resetPassword, getUserByUsername } from '../services/userService'
import Modal from '../components/Modal.vue'
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

// 搜索相关
const searchKeyword = ref('')
const searchType = ref('username') // username | email | phone

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

function handleSearch() {
  if (!searchKeyword.value.trim()) {
    fetchUsers(0)
    return
  }

  if (searchType.value === 'username') {
    // 支持按用户名精确搜索（已有 API）
    getUserByUsername(searchKeyword.value.trim()).then(response => {
      if (response.code === 200) {
        users.value = [response.data]
        currentPage.value = 0
        totalPages.value = 1
      } else {
        alert(response.message || '未找到该用户')
      }
    }).catch(err => {
      alert(err.response?.data?.message || '查询失败')
    })
  } else {
    // TODO: email/phone 搜索待后端 API 实现
    alert('邮箱和手机号搜索功能待后端 API 对接后生效，当前仅支持用户名搜索')
    fetchUsers(0)
  }
}

function handleReset() {
  searchKeyword.value = ''
  searchType.value = 'username'
  fetchUsers(0)
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

const showResetPwdModal = ref(false)
const resettingPwd = ref(false)
const resetPwdUser = ref(null)
const resetPwdForm = ref({ newPassword: '' })

function openResetPwdModal(user) {
  resetPwdUser.value = user
  resetPwdForm.value = { newPassword: '' }
  showResetPwdModal.value = true
}

async function handleResetPassword() {
  const { newPassword } = resetPwdForm.value

  if (!newPassword) {
    alert('请输入新密码')
    return
  }
  if (newPassword.length < 6) {
    alert('新密码长度至少6位')
    return
  }

  resettingPwd.value = true
  try {
    const res = await resetPassword(resetPwdUser.value.id, { newPassword })
    if (res.code === 200) {
      alert('密码重置成功')
      showResetPwdModal.value = false
    } else {
      alert(res.message || '重置失败')
    }
  } catch (err) {
    alert(err.response?.data?.message || '重置失败，请检查网络')
  } finally {
    resettingPwd.value = false
  }
}
</script>