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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from 'lucide-vue-next'
import { getAllBooks, createBook, updateBook, deleteBook } from '../services/bookService'
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

async function fetchBooks(page = 0) {
  loading.value = true
  try {
    const response = await getAllBooks(page, 10)
    
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

onMounted(() => {
  fetchBooks()
})
</script>
