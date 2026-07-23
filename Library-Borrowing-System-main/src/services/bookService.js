import api from '../utils/api'

export function createBook(data) {
  return api.post('/books/create', data)
}

export function getBookById(id) {
  return api.get(`/books/getById/${id}`)
}

export function getAllBooks(page, size) {
  return api.get('/books/getAll', { params: { page, size } })
}

export function searchByTitle(title, page, size) {
  return api.get('/books/searchByTitle', { params: { title, page, size } })
}

export function searchByAuthor(author, page, size) {
  return api.get('/books/searchByAuthor', { params: { author, page, size } })
}

export function searchByCategory(category, page, size) {
  return api.get('/books/searchByCategory', { params: { category, page, size } })
}

export function updateBook(id, data) {
  return api.put(`/books/update/${id}`, data)
}

export function deleteBook(id) {
  return api.delete(`/books/delete/${id}`)
}

export function borrowBook(data) {
  return api.post('/book-records/borrow', data)
}

export function returnBook(recordId) {
  return api.post(`/book-records/return/${recordId}`)
}

export function renewBook(recordId) {
  return api.post(`/book-records/renew/${recordId}`)
}

export function getBorrowRecordsByUser(userId, page, size) {
  return api.get(`/book-records/borrowRecords/user/${userId}`, { params: { page, size } })
}

export function getAllBorrowRecords(page, size) {
  return api.get('/book-records/borrowRecords/all', { params: { page, size } })
}

export function getOverdueRecords(page, size) {
  return api.get('/book-records/overdue', { params: { page, size } })
}
