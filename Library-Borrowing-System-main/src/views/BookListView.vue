<template>
  <div>
    <PageHeader title="图书浏览" />

    <div class="bg-white rounded-lg shadow-sm p-4 mb-6">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[200px]">
          <label class="block text-sm font-medium text-gray-700 mb-2">按书名搜索</label>
          <div class="relative">
            <Search class="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              v-model="searchTitle"
              type="text"
              placeholder="输入书名..."
              class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              @keyup.enter="handleSearch"
            />
          </div>
        </div>
        <div class="flex-1 min-w-[200px]">
          <label class="block text-sm font-medium text-gray-700 mb-2">按作者搜索</label>
          <div class="relative">
            <Search class="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              v-model="searchAuthor"
              type="text"
              placeholder="输入作者..."
              class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              @keyup.enter="handleSearch"
            />
          </div>
        </div>
        <div class="flex-1 min-w-[200px]">
          <label class="block text-sm font-medium text-gray-700 mb-2">按分类搜索</label>
          <div class="relative">
            <Search class="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              v-model="searchCategory"
              type="text"
              placeholder="输入分类..."
              class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
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

    <div v-else-if="books.length === 0">
      <EmptyState message="暂无图书" icon="BookX" />
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      <div
        v-for="book in books"
        :key="book.id"
        class="bg-white rounded-lg shadow-sm p-4 hover:shadow-md transition-shadow"
      >
        <div class="h-48 bg-gray-100 rounded-lg flex items-center justify-center mb-4">
          <BookOpen class="w-16 h-16 text-gray-400" />
        </div>
        <h3 class="text-lg font-semibold text-gray-800 truncate mb-1">{{ book.title }}</h3>
        <p class="text-sm text-gray-600 mb-1">作者: {{ book.author }}</p>
        <p class="text-sm text-gray-500 mb-1">分类: {{ book.category || '未分类' }}</p>
        <p class="text-sm text-gray-500 mb-3">库存: {{ book.stock }} / 可借: {{ book.available }}</p>
        <button
          @click="handleBorrow(book)"
          :disabled="book.available <= 0 || borrowLoading"
          class="w-full py-2 px-4 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 disabled:cursor-not-allowed text-white rounded-lg transition-colors"
        >
          {{ book.available > 0 ? '借阅' : '暂不可借' }}
        </button>
      </div>
    </div>

    <Pagination
      :current-page="currentPage"
      :total-pages="totalPages"
      @prev="prevPage"
      @next="nextPage"
    />

    <Modal :visible="showBorrowDialog" title="借阅图书" @close="showBorrowDialog = false">
      <p class="text-gray-600 mb-2">书名: {{ selectedBook?.title }}</p>
      <p class="text-gray-600 mb-4">作者: {{ selectedBook?.author }}</p>
      <div class="mb-4">
        <label class="block text-sm font-medium text-gray-700 mb-2">借阅天数</label>
        <input
          v-model.number="borrowDays"
          type="number"
          min="1"
          max="30"
          class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
      </div>
      <div v-if="borrowError" class="text-red-500 text-sm mb-4">{{ borrowError }}</div>
      <div class="flex justify-end space-x-3">
        <button
          @click="showBorrowDialog = false"
          class="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
        >
          取消
        </button>
        <button
          @click="confirmBorrow"
          :disabled="borrowLoading"
          class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-50"
        >
          {{ borrowLoading ? '借阅中...' : '确认借阅' }}
        </button>
      </div>
    </Modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, RotateCcw, BookOpen } from 'lucide-vue-next'
import { getAllBooks, searchByTitle, searchByAuthor, searchByCategory, borrowBook } from '../services/bookService'
import PageHeader from '../components/PageHeader.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import EmptyState from '../components/EmptyState.vue'
import Pagination from '../components/Pagination.vue'
import Modal from '../components/Modal.vue'

const loading = ref(false)
const borrowLoading = ref(false)
const books = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const showBorrowDialog = ref(false)
const selectedBook = ref(null)
const borrowDays = ref(7)
const borrowError = ref('')

const searchTitle = ref('')
const searchAuthor = ref('')
const searchCategory = ref('')

async function fetchBooks(page = 0) {
  loading.value = true
  try {
    let response
    if (searchTitle.value) {
      response = await searchByTitle(searchTitle.value, page, 10)
    } else if (searchAuthor.value) {
      response = await searchByAuthor(searchAuthor.value, page, 10)
    } else if (searchCategory.value) {
      response = await searchByCategory(searchCategory.value, page, 10)
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
  searchTitle.value = ''
  searchAuthor.value = ''
  searchCategory.value = ''
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

function handleBorrow(book) {
  selectedBook.value = book
  borrowDays.value = 7
  borrowError.value = ''
  showBorrowDialog.value = true
}

async function confirmBorrow() {
  if (!selectedBook.value || borrowDays.value <= 0) return
  
  borrowLoading.value = true
  borrowError.value = ''
  
  try {
    const response = await borrowBook({
      bookId: selectedBook.value.id,
      borrowDays: borrowDays.value
    })
    
    if (response.code === 200) {
      showBorrowDialog.value = false
      fetchBooks(currentPage.value)
    } else {
      borrowError.value = response.message || '借阅失败'
    }
  } catch (err) {
    borrowError.value = err.response?.data?.message || '借阅失败'
  } finally {
    borrowLoading.value = false
  }
}

onMounted(() => {
  fetchBooks()
})
</script>