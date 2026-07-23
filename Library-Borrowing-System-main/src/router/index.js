import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated, isAdmin } from '../utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/RegisterView.vue')
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'BookList',
        component: () => import('../views/BookListView.vue')
      },
      {
        path: 'my-borrow',
        name: 'MyBorrow',
        component: () => import('../views/MyBorrowView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/ProfileView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'book-manage',
        name: 'BookManage',
        component: () => import('../views/BookManageView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'user-manage',
        name: 'UserManage',
        component: () => import('../views/UserManageView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'borrow-manage',
        name: 'BorrowManage',
        component: () => import('../views/BorrowManageView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    next('/login')
  } else if (to.meta.requiresAdmin && !isAdmin()) {
    next('/')
  } else {
    next()
  }
})

export default router
