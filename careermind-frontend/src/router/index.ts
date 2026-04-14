import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/HomeView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/tasks',
      name: 'Tasks',
      component: () => import('@/views/TasksView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/tasks/:id',
      name: 'TaskDetail',
      component: () => import('@/views/TaskView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/discussions/:taskId',
      name: 'Discussion',
      component: () => import('@/views/DiscussionView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/results/:taskId',
      name: 'Result',
      component: () => import('@/views/ResultView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/agents',
      name: 'Agents',
      component: () => import('@/views/AgentsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/settings',
      name: 'Settings',
      component: () => import('@/views/SettingsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/kb',
      name: 'KnowledgeBase',
      component: () => import('@/views/KbView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  // 只要有 token 就认为是登录状态（user 会在初始化时从 localStorage 恢复）
  const hasAuth = userStore.isLoggedIn || !!userStore.token

  if (to.meta.requiresAuth && !hasAuth) {
    ElMessage.warning('请先登录后再访问此页面')
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  } else if (to.meta.public && hasAuth) {
    next('/')
  } else {
    next()
  }
})

export default router
