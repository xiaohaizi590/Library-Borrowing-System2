<template>
  <div>
    <PageHeader title="借阅管理">
      <div class="flex space-x-2">
        <button
          @click="viewMode = 'all'"
          :class="viewMode === 'all' ? 'bg-blue-500 text-white' : 'bg-gray-500 text-white'"
          class="px-4 py-2 rounded-lg hover:opacity-90"
        >
          全部记录
        </button>
        <button
          @click="viewMode = 'overdue'"
          :class="viewMode === 'overdue' ? 'bg-red-500 text-white' : 'bg-gray-500 text-white'"
          class="px-4 py-2 rounded-lg hover:opacity-90"
        >
          逾期记录
        </button>
      </div>
    </PageHeader>

    <LoadingSpinner v-if="loading" />

    <EmptyState
      v-else-if="records.length === 0"
      :message="viewMode === 'overdue' ? '暂无逾期记录' : '暂无借阅记录'"
      icon="ClipboardList"
    />

    <div v-else class="bg-white rounded-lg shadow-sm overflow-hidden">
      <table class="min-w-full divide-y divide-gray-200">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">图书信息</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">借阅人</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">借阅时间</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">应还时间</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">归还时间</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">续借次数</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-gray-200">
          <tr v-for="record in records" :key="record.id" class="hover:bg-gray-50">
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="text-sm font-medium text-gray-900">{{ record.bookTitle }}</div>
              <div class="text-sm text-gray-500">{{ record.bookAuthor }}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ record.userName }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ formatDateTime(record.borrowTime) }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm" :class="isOverdue(record) ? 'text-red-500 font-medium' : 'text-gray-500'">
              {{ formatDateTime(record.dueTime) }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ record.returnTime ? formatDateTime(record.returnTime) : '-' }}</td>
            <td class="px-6 py-4 whitespace-nowrap">
              <StatusBadge :type="record.status" :map="borrowStatusMap" />
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ record.renewCount }}</td>
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
import { ref, onMounted, watch } from 'vue'
import { getAllBorrowRecords, getOverdueRecords } from '../services/bookService'
import PageHeader from '../components/PageHeader.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import EmptyState from '../components/EmptyState.vue'
import Pagination from '../components/Pagination.vue'
import StatusBadge from '../components/StatusBadge.vue'

const borrowStatusMap = {
  BORROWED: { class: 'bg-yellow-100 text-yellow-800', text: '借阅中' },
  RETURNED: { class: 'bg-green-100 text-green-800', text: '已归还' },
  OVERDUE: { class: 'bg-red-100 text-red-800', text: '已逾期' }
}

const loading = ref(false)
const records = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const viewMode = ref('all')

async function fetchRecords(page = 0) {
  loading.value = true
  
  try {
    let response
    if (viewMode.value === 'overdue') {
      response = await getOverdueRecords(page, 10)
    } else {
      response = await getAllBorrowRecords(page, 10)
    }
    
    if (response.code === 200) {
      records.value = response.data.content
      totalPages.value = response.data.totalPages
      currentPage.value = response.data.number
    }
  } catch (err) {
    console.error('获取借阅记录失败:', err)
  } finally {
    loading.value = false
  }
}

function prevPage() {
  if (currentPage.value > 0) {
    fetchRecords(currentPage.value - 1)
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value - 1) {
    fetchRecords(currentPage.value + 1)
  }
}

function formatDateTime(dateTime) {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

function isOverdue(record) {
  if (record.status !== 'BORROWED') return false
  return new Date(record.dueTime) < new Date()
}

watch(viewMode, () => {
  fetchRecords(0)
})

onMounted(() => {
  fetchRecords()
})
</script>
