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

export function getDeletedUsers(page, size) {
  return api.get('/users/recycleBin', { params: { page, size } })
}

export function restoreUser(id) {
  return api.put(`/users/restore/${id}`)
}

export function cleanExpiredUsers(retentionDays = 30) {
  return api.delete('/users/cleanExpired', { params: { retentionDays } })
}
// 用户自己改密码
export function changePassword(data) {
  return api.put('/users/password', data)
}

// 管理员重置密码
export function resetPassword(id, data) {
  return api.put(`/users/${id}/password`, data)
}

