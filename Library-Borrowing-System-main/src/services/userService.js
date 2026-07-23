import api from '../utils/api'

export function login(data) {
  return api.post('/users/login', data)
}

export function register(data) {
  return api.post('/users/register', data)
}

export function getProfile() {
  return api.get('/users/profile')
}

export function getAllUsers(page, size) {
  return api.get('/users/getAllUsers', { params: { page, size } })
}

export function getUserById(id) {
  return api.get(`/users/getUserById/${id}`)
}

export function getUserByUsername(username) {
  return api.get(`/users/getUserByUsername/${username}`)
}

export function updateUser(id, data) {
  return api.put(`/users/updateUser/${id}`, data)
}

export function deleteUser(id) {
  return api.delete(`/users/deleteUser/${id}`)
}

export function getCaptcha() {
  return api.get('/users/captcha')
}

export function verifyCaptcha(data) {
  return api.post('/users/captcha/verify', data)
}
