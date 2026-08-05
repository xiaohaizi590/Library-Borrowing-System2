<template>
  <div>
    <PageHeader title="图书管理">
      <button
        @click="showCreateDialog = true"
        class="flex items-center space-x-1 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
      >
        <Plus class="w-4 h-4" />
        <span>添加图书</span>
      </button>
    </PageHeader>

    <!-- Excel批量导入按钮 - 固定在右下角 -->
    <button
      @click="handleBatchImport"
      class="fixed bottom-6 right-6 flex items-center space-x-2 px-5 py-3 bg-green-500 text-white rounded-lg hover:bg-green-600 shadow-lg z-40"
    >
      <Upload class="w-5 h-5" />
      <span>Excel文件批量导入图书</span>
    </button>

    <!-- 搜索栏 -->
    <div class="bg-white rounded-lg shadow-sm p-4 mb-6">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[200px]">
          <label class="block text-sm font-medium text-gray-700 mb-2">搜索类型</label>
          <select
            v-model="searchType"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="title">按书名</option>
            <option value="author">按作者</option>
            <option value="category">按分类</option>
          </select>
        </div>
        <div class="flex-1 min-w-[200px]">
          <label class="block text-sm font-medium text-gray-700 mb-2">搜索关键词</label>
          <div class="relative">
            <input
              v-model="searchKeyword"
              type="text"
              :placeholder="'输入' + (searchType === 'title' ? '书名' : searchType === 'author' ? '作者' : '分类') + '...'"
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
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">书名</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">作者</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ISBN</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">分类</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">库存</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">可借</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-gray-200">
          <tr v-for="book in books" :key="book.id" class="hover:bg-gray-50">
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ book.title }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ book.author }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ book.isbn || '-' }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ book.category || '未分类' }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ book.stock }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ book.available }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
              <button
                @click="handleEdit(book)"
                class="text-blue-600 hover:text-blue-900 mr-3 px-3 py-1 bg-blue-50 rounded hover:bg-blue-100"
              >
                编辑
              </button>
              <button
                @click="handleDelete(book)"
                :disabled="book.available !== book.stock"
                :class="book.available !== book.stock ? 'text-gray-400 cursor-not-allowed' : 'text-red-600 hover:text-red-900 px-3 py-1 bg-red-50 rounded hover:bg-red-100'"
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

    <Modal :visible="showCreateDialog || showEditDialog" :title="showEditDialog ? '编辑图书' : '添加图书'" @close="closeDialog">
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">书名 *</label>
          <input
            v-model="form.title"
            type="text"
            required
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          />
        </div>
        
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">作者 *</label>
          <input
            v-model="form.author"
            type="text"
            required
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          />
        </div>
        
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">ISBN *</label>
          <input
            v-model="form.isbn"
            type="text"
            required
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          />
        </div>
        
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">出版社</label>
          <input
            v-model="form.publisher"
            type="text"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          />
        </div>
        
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">分类</label>
          <input
            v-model="form.category"
            type="text"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          />
        </div>
        
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">库存 *</label>
          <input
            v-model.number="form.stock"
            type="number"
            min="1"
            required
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          />
        </div>
        
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">描述</label>
          <textarea
            v-model="form.description"
            rows="3"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          ></textarea>
        </div>
        
        <div v-if="submitError" class="text-red-500 text-sm">{{ submitError }}</div>
        
        <div class="flex justify-end space-x-3 pt-4">
          <button
            type="button"
            @click="closeDialog"
            class="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
          >
            取消
          </button>
          <button
            type="submit"
            :disabled="submitLoading"
            class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-50"
          >
            {{ submitLoading ? '提交中...' : '提交' }}
          </button>
        </div>
      </form>
    </Modal>

    <!-- 批量导入图书弹窗 -->
    <Modal :visible="showBatchImportModal" title="批量导入图书" @close="closeBatchImportModal">
      <div class="space-y-4">
        <div v-if="!importTaskId">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">选择 Excel 文件</label>
            <input
              ref="fileInputRef"
              type="file"
              accept=".xlsx,.xls"
              @change="handleFileChange"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            />
            <p v-if="selectedFile" class="mt-2 text-sm text-green-600">已选择: {{ selectedFile.name }}</p>
            <p class="mt-1 text-xs text-gray-400">支持 .xlsx / .xls 格式，大文件将自动使用异步导入</p>
          </div>
          <div v-if="batchImportError" class="text-red-500 text-sm">{{ batchImportError }}</div>
          <div class="flex justify-end space-x-3 pt-4">
            <button
              type="button"
              @click="closeBatchImportModal"
              class="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
            >
              取消
            </button>
            <button
              type="button"
              @click="confirmBatchImport"
              :disabled="!selectedFile || batchImportLoading"
              class="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 disabled:opacity-50"
            >
              {{ batchImportLoading ? '提交中...' : '开始导入' }}
            </button>
          </div>
        </div>

        <div v-else>
          <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm font-medium text-blue-800">导入进度</span>
              <span class="text-sm text-blue-600">{{ importStatus }}</span>
            </div>
            <div class="w-full bg-blue-200 rounded-full h-2.5">
              <div
                class="bg-blue-500 h-2.5 rounded-full transition-all duration-500"
                :style="{ width: importProgressPercent + '%' }"
              ></div>
            </div>
            <p class="mt-3 text-xs text-gray-500">任务ID: {{ importTaskId }}</p>
          </div>

          <div v-if="importDone" class="flex justify-end pt-4">
            <button
              type="button"
              @click="closeBatchImportModal"
              class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
            >
              完成
            </button>
          </div>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { Plus, Upload, Search, RotateCcw } from 'lucide-vue-next'
import { getAllBooks, createBook, updateBook, deleteBook, batchImportFromExcel, batchImportFromExcelAsync, getImportProgress, searchByTitle, searchByAuthor, searchByCategory } from '../services/bookService'
import PageHeader from '../components/PageHeader.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import Pagination from '../components/Pagination.vue'
import Modal from '../components/Modal.vue'

const loading = ref(false)
const submitLoading = ref(false)
const books = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const submitError = ref('')
const editingBookId = ref(null)

const form = reactive({
  title: '',
  author: '',
  isbn: '',
  publisher: '',
  category: '',
  stock: 1,
  description: ''
})

// 搜索相关
const searchKeyword = ref('')
const searchType = ref('title') // title | author | category

async function fetchBooks(page = 0) {
  loading.value = true
  try {
    let response
    if (searchKeyword.value.trim()) {
      const keyword = searchKeyword.value.trim()
      switch (searchType.value) {
        case 'author':
          response = await searchByAuthor(keyword, page, 10)
          break
        case 'category':
          response = await searchByCategory(keyword, page, 10)
          break
        default:
          response = await searchByTitle(keyword, page, 10)
      }
    } else {
      response = await getAllBooks(page, 10)
    }
    
    if (response.code === 200) {
      books.value = response.data.content
      totalPages.value = response.data.totalPages
      currentPage.value = response.data.number
    }
  } catch (err) {
    console.error('获取图书失败:', err)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchBooks(0)
}

function handleReset() {
  searchKeyword.value = ''
  searchType.value = 'title'
  fetchBooks(0)
}

function prevPage() {
  if (currentPage.value > 0) {
    fetchBooks(currentPage.value - 1)
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value - 1) {
    fetchBooks(currentPage.value + 1)
  }
}

function handleEdit(book) {
  editingBookId.value = book.id
  form.title = book.title
  form.author = book.author
  form.isbn = book.isbn || ''
  form.publisher = book.publisher || ''
  form.category = book.category || ''
  form.stock = book.stock
  form.description = book.description || ''
  showEditDialog.value = true
  submitError.value = ''
}

function handleDelete(book) {
  if (book.available !== book.stock) return
  
  if (confirm(`确定要删除图书 "${book.title}" 吗？`)) {
    deleteBook(book.id).then(response => {
      if (response.code === 200) {
        fetchBooks(currentPage.value)
      }
    })
  }
}

function closeDialog() {
  showCreateDialog.value = false
  showEditDialog.value = false
  editingBookId.value = null
  submitError.value = ''
  Object.assign(form, {
    title: '',
    author: '',
    isbn: '',
    publisher: '',
    category: '',
    stock: 1,
    description: ''
  })
}

async function handleSubmit() {
  submitLoading.value = true
  submitError.value = ''
  
  try {
    const requestData = {
      title: form.title,
      author: form.author,
      isbn: form.isbn,
      publisher: form.publisher,
      category: form.category,
      stock: form.stock,
      description: form.description
    }
    
    let response
    if (showEditDialog.value && editingBookId.value) {
      response = await updateBook(editingBookId.value, requestData)
    } else {
      response = await createBook(requestData)
    }
    
    if (response.code === 200) {
      closeDialog()
      fetchBooks(currentPage.value)
    } else {
      submitError.value = response.message || '操作失败'
    }
  } catch (err) {
    submitError.value = err.response?.data?.message || '操作失败'
  } finally {
    submitLoading.value = false
  }
}

const showBatchImportModal = ref(false)
const selectedFile = ref(null)
const fileInputRef = ref(null)
const batchImportLoading = ref(false)
const batchImportError = ref('')

const importTaskId = ref(null)
const importStatus = ref('')
const importProgressPercent = ref(0)
const importDone = ref(false)
let progressTimer = null

function handleBatchImport() {
  resetImportState()
  selectedFile.value = null
  batchImportError.value = ''
  showBatchImportModal.value = true
}

function closeBatchImportModal() {
  stopProgressPolling()
  resetImportState()
  showBatchImportModal.value = false
}

function resetImportState() {
  importTaskId.value = null
  importStatus.value = ''
  importProgressPercent.value = 0
  importDone.value = false
}

function stopProgressPolling() {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

function handleFileChange(event) {
  selectedFile.value = event.target.files[0] || null
}

async function confirmBatchImport() {
  if (!selectedFile.value) return
  batchImportLoading.value = true
  batchImportError.value = ''

  try {
    const response = await batchImportFromExcelAsync(selectedFile.value)
    if (response.code === 200 && response.data && response.data.taskId) {
      importTaskId.value = response.data.taskId
      importStatus.value = response.data.message || '任务已提交'
      startProgressPolling(response.data.taskId)
    } else {
      batchImportError.value = response.message || '导入失败'
    }
  } catch (err) {
    batchImportError.value = err.response?.data?.message || '导入失败，请检查文件格式'
  } finally {
    batchImportLoading.value = false
  }
}

function startProgressPolling(taskId) {
  importProgressPercent.value = 10
  let pollCount = 0
  const maxPolls = 120

  progressTimer = setInterval(async () => {
    pollCount++
    if (pollCount > maxPolls) {
      stopProgressPolling()
      importStatus.value = '导入超时，请稍后刷新列表查看'
      importDone.value = true
      return
    }

    try {
      const res = await getImportProgress(taskId)
      if (res.code === 200 && res.data) {
        const statusText = res.data.status || ''
        importStatus.value = statusText

        if (statusText.includes('解析中')) {
          importProgressPercent.value = 20
        } else if (statusText.includes('开始导入') || statusText.includes('解析完成')) {
          importProgressPercent.value = 50
        } else if (statusText.includes('成功导入')) {
          importProgressPercent.value = 100
          importDone.value = true
          stopProgressPolling()
          fetchBooks(currentPage.value)
        } else if (statusText.includes('失败') || statusText.includes('错误')) {
          importProgressPercent.value = 0
          importDone.value = true
          stopProgressPolling()
        }
      }
    } catch (err) {
      console.error('查询进度失败:', err)
    }
  }, 2000)
}

onMounted(() => {
  fetchBooks()
})

onUnmounted(() => {
  stopProgressPolling()
})
</script>