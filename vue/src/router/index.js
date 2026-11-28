import { createRouter, createWebHistory } from 'vue-router'
import { getStoredLoginUser } from '@/utils/auth.js'

// 轻量顶部路由跳转流光加载进度条
let progressEl = null
let progressTimer = null

function startRouteProgress() {
  if (typeof document === 'undefined') return
  if (!progressEl) {
    progressEl = document.createElement('div')
    progressEl.id = 'route-progress-bar'
    document.body.appendChild(progressEl)
  }
  clearTimeout(progressTimer)
  progressEl.style.opacity = '1'
  progressEl.style.width = '18%'
  requestAnimationFrame(() => {
    if (progressEl) progressEl.style.width = '72%'
  })
}

function finishRouteProgress() {
  if (!progressEl) return
  progressEl.style.width = '100%'
  progressTimer = setTimeout(() => {
    if (progressEl) {
      progressEl.style.opacity = '0'
      setTimeout(() => {
        if (progressEl) progressEl.style.width = '0%'
      }, 260)
    }
  }, 180)
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login'},
    { path: '/main' ,redirect: '/main/home', component: () => import('../views/MainFrame.vue'), children: [
        { path: 'home', name: 'home', meta:{ title: '星界 - 系统主页'}, component: () => import('../views/Home.vue')},
        { path: 'user', name: 'user', meta:{ title: '星界 - 用户中心'}, component: () => import('../views/User.vue')},
        { path: 'admin', name: 'admin', meta:{ title: '星界 - 管理中心'}, component: () => import('../views/Admin.vue')},
        { path: 'stats', name: 'stats', meta:{ title: '星界 - 个人中心与安全设置'}, component: () => import('../views/Personal.vue')},
        { path: 'stats/password', name: 'password', redirect: { path: '/main/stats', query: { action: 'pwd' } }},
        { path: 'detail', name: 'detail', meta:{ title: '星界 - 账户详情'}, component: () => import('../views/AccountDetail.vue')},
        { path: 'category', name: 'category', meta:{ title: '星界 - 分类管理'}, component: () => import('../views/Category.vue')},
        { path: 'notice', name: 'notice', meta:{ title: '星界 - 公告管理'}, component: () => import('../views/Notice.vue')},
        { path: 'siteConfig', name: 'siteConfig', meta:{ title: '星界 - 网站公共配置'}, component: () => import('../views/SiteConfig.vue')},
        { path: 'notebook', name: 'notebook', meta:{ title: '星界 - 记账日记'}, component: () => import('../views/Notebook.vue')},
        { path: 'plan', name: 'plan', meta:{ title: '星界 - 存钱计划'}, component: () => import('../views/Plan.vue')},
        { path: 'planDetail', name: 'planDetail', meta:{ title: '星界 - 存钱计划明细'}, component: () => import('../views/PlanDetail.vue')},
        { path: 'AIAnalysis', name: 'AIAnalysis', meta:{ title: '星界 - 智能账单分析'}, component: () => import('../views/AIAnalysis.vue')},
        { path: 'bill', name: 'bill', meta:{ title: '星界 - 账单管理'}, component: () => import('../views/Bill.vue')},
    ]},
    { path: '/login',name: 'login', meta:{ title: '星界 - 登录'}, component: () => import('../views/Login.vue')},
    { path: '/register',name: 'register', meta:{ title: '星界 - 注册'}, component: () => import('../views/Register.vue')},
    { path: '/404',name: 'NotFound', meta:{title: '页面迷路了...'}, component: () => import('../views/404.vue')},
    { path: '/:pathMatch(.*)', redirect: '/404' }
  ]
})

router.beforeEach((to, from, next) => {
  startRouteProgress()
  document.title = to.meta.title || '星界'

  // 读取并校验 JWT 有效期（过期或无效会被就地清理并返回空对象）
  const user = getStoredLoginUser()
  const loggedIn = Boolean(user && user.token)

  // 未登录或 token 已失效时直接拦截受保护页面，避免先渲染主页再跳回登录页
  if (to.path.startsWith('/main') && !loggedIn) {
    return next({ path: '/login', replace: true })
  }

  // 在已持有有效登录态的情况下进入登录/注册页面，自动重定向到系统主页
  if ((to.path === '/login' || to.path === '/register') && loggedIn) {
    return next('/main/home')
  }

  // 强制改密全局路由守卫（防 F12 绕过）：若用户处于初始密码待重置状态，限制在主页完成改密，禁止跳转到其他业务页面
  if (loggedIn && user.needChangePwd === 1 && to.path !== '/main/home' && to.path !== '/login') {
    return next('/main/home')
  }

  next()
})

router.afterEach(() => {
  finishRouteProgress()
})

export default router
