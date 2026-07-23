<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-500 to-purple-600">
    <div class="bg-white rounded-2xl shadow-xl p-8 w-full max-w-md">
      <div class="text-center mb-8">
        <div class="w-16 h-16 bg-blue-500 rounded-full flex items-center justify-center mx-auto mb-4">
          <BookOpen class="w-8 h-8 text-white" />
        </div>
        <h1 class="text-2xl font-bold text-gray-800">图书管理系统</h1>
        <p class="text-gray-500 mt-2">欢迎登录</p>
      </div>

      <form @submit.prevent="handleLogin" class="space-y-6">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">用户名</label>
          <div class="relative">
            <User class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              v-model="form.account"
              type="text"
              placeholder="请输入用户名"
              class="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
              required
            />
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
          <div class="relative">
            <Lock class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              class="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
              required
            />
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">验证码</label>
          <div class="flex items-center gap-3">
            <div class="relative flex-1">
              <input
                v-model="form.captcha"
                type="text"
                placeholder="请输入验证码"
                class="w-full pl-4 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                required
              />
            </div>
            <div class="flex-shrink-0">
              <img
                :src="captchaImage"
                alt="验证码"
                @click="loadCaptcha"
                class="w-32 h-12 rounded-lg cursor-pointer hover:opacity-80 transition-opacity"
              />
            </div>
          </div>
        </div>

        <div v-if="error" class="text-red-500 text-sm text-center">{{ error }}</div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-blue-500 hover:bg-blue-600 text-white font-semibold py-3 px-4 rounded-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <span v-if="loading" class="flex items-center justify-center">
            <Loader2 class="w-5 h-5 animate-spin mr-2" />
            登录中...
          </span>
          <span v-else>登 录</span>
        </button>
      </form>

      <div class="mt-6 text-center">
        <span class="text-gray-500">还没有账号？</span>
        <router-link to="/register" class="text-blue-500 hover:text-blue-600 font-medium ml-1">立即注册</router-link>
      </div>

    
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { BookOpen, User, Lock, Loader2 } from 'lucide-vue-next'
import { login, getCaptcha } from '../services/userService'
import { setToken, setUser } from '../utils/auth'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const captchaImage = ref('')
const captchaKey = ref('')

const form = reactive({
  account: '',
  password: '',
  captcha: ''
})

async function loadCaptcha() {
  try {
    const response = await getCaptcha()
    if (response.code === 200) {
      captchaKey.value = response.data.captchaKey
      captchaImage.value = response.data.image
    }
  } catch (err) {
    console.error('获取验证码失败:', err)
  }
}

async function handleLogin() {
  loading.value = true
  error.value = ''
  
  if (!form.captcha.trim()) {
    error.value = '请输入验证码'
    loading.value = false
    return
  }
  
  try {
    const response = await login({
      ...form,
      captchaKey: captchaKey.value
    })
    if (response.code === 200) {
      const { token, ...user } = response.data
      setToken(token)
      setUser(user)
      router.push('/')
    } else {
      error.value = response.message || '登录失败'
      loadCaptcha()
    }
  } catch (err) {
    error.value = err.response?.data?.message || '登录失败，请检查网络'
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCaptcha()
})
</script>
