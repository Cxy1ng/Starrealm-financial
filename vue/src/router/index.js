import { createRouter, createWebHistory } from 'vue-router'

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
        { path: 'home', name: 'home', meta:{ title: '智汇账本 - 系统主页'}, component: () => import('../views/Home.vue')},
        { path: 'user', name: 'user', meta:{ title: '智汇账本 - 用户中心'}, component: () => import('../views/User.vue')},
        { path: 'admin', name: 'admin', meta:{ title: '智汇账本 - 管理中心'}, component: () => import('../views/Admin.vue')},
        { path: 'stats', name: 'stats', meta:{ title: '智汇账本 - 个人中心与安全设置'}, component: () => import('../views/Personal.vue')},
        { path: 'stats/password', name: 'password', redirect: { path: '/main/stats', query: { action: 'pwd' } }},
        { path: 'detail', name: 'detail', meta:{ title: '智汇账本 - 账户详情'}, component: () => import('../views/AccountDetail.vue')},
        { path: 'category', name: 'category', meta:{ title: '智汇账本 - 分类管理'}, component: () => import('../views/Category.vue')},
        { path: 'notice', name: 'notice', meta:{ title: '智汇账本 - 公告管理'}, component: () => import('../views/Notice.vue')},
        { path: 'siteConfig', name: 'siteConfig', meta:{ title: '智汇账本 - 网站公共配置'}, component: () => import('../views/SiteConfig.vue')},
        { path: 'notebook', name: 'notebook', meta:{ title: '智汇账本 - 记账日记'}, component: () => import('../views/Notebook.vue')},
        { path: 'plan', name: 'plan', meta:{ title: '智汇账本 - 存钱计划'}, component: () => import('../views/Plan.vue')},
        { path: 'planDetail', name: 'planDetail', meta:{ title: '智汇账本 - 存钱计划明细'}, component: () => import('../views/PlanDetail.vue')},
        { path: 'AIAnalysis', name: 'AIAnalysis', meta:{ title: '智汇账本 - 智能账单分析'}, component: () => import('../views/AIAnalysis.vue')},
        { path: 'bill', name: 'bill', meta:{ title: '智汇账本 - 账单管理'}, component: () => import('../views/Bill.vue')},
    ]},
    { path: '/login',name: 'login', meta:{ title: '智汇账本 - 登录'}, component: () => import('../views/Login.vue')},
    { path: '/register',name: 'register', meta:{ title: '智汇账本 - 注册'}, component: () => import('../views/Register.vue')},
    { path: '/404',name: 'NotFound', meta:{title: '页面迷路了...'}, component: () => import('../views/404.vue')},
    { path: '/:pathMatch(.*)', redirect: '/404' }
  ]
})

router.beforeEach((to, from, next) => {
  startRouteProgress()
  document.title = to.meta.title || '智汇账本'
  next()
})

router.afterEach(() => {
  finishRouteProgress()
})

export default router
