import axios from 'axios'
import { getToken, removeToken, removeUser } from './auth'
import router from '../router'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        removeToken()
        removeUser()
        router.push('/login')
      }
    }
    return Promise.reject(error)
  }
)

export default api
