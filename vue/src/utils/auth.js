import {
  REMEMBER_ME_COOKIE,
  getCookie,
  saveLoginSessionCookie,
  getLoginSessionFromCookie,
  clearLoginSessionCookie
} from './cookie.js'

const STORAGE_KEY = 'loginToken'
const EXPIRY_SKEW_MS = 5000

/**
 * 解析并校验 JWT 载荷中的 exp 过期时间（仅在格式完整且尚未过期时返回 true）
 */
export function isJwtTokenValid(token, skewMs = EXPIRY_SKEW_MS) {
  if (!token || typeof token !== 'string') return false
  const parts = token.split('.')
  if (parts.length !== 3 || !parts[1]) return false
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
    const payloadJson = decodeURIComponent(
      atob(padded)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    const payload = JSON.parse(payloadJson)
    if (typeof payload.exp !== 'number') return false
    return payload.exp * 1000 > Date.now() + skewMs
  } catch {
    return false
  }
}

function safeParseJson(raw) {
  if (!raw) return null
  try {
    const obj = JSON.parse(raw)
    return obj && typeof obj === 'object' ? obj : null
  } catch {
    return null
  }
}

function getTargetStorage(rememberMe) {
  if (typeof window === 'undefined') return null
  return rememberMe ? window.localStorage : window.sessionStorage
}

/**
 * 清理全部本地登录态（localStorage / sessionStorage / 会话 Cookie）
 */
export function clearLoginSession() {
  if (typeof window !== 'undefined') {
    try {
      window.localStorage.removeItem(STORAGE_KEY)
    } catch {
      // ignore
    }
    try {
      window.sessionStorage.removeItem(STORAGE_KEY)
    } catch {
      // ignore
    }
  }
  clearLoginSessionCookie()
}

/**
 * 保存登录态：勾选“7天内自动登录”时写入 localStorage + 7天 Cookie；否则仅写入当前浏览器会话的 sessionStorage + 会话 Cookie
 */
export function saveLoginUser(userData, rememberMe = false, fallbackPerm = '1') {
  if (!userData || !userData.token) return
  if (typeof window !== 'undefined') {
    try {
      window.localStorage.removeItem(STORAGE_KEY)
      window.sessionStorage.removeItem(STORAGE_KEY)
      getTargetStorage(Boolean(rememberMe))?.setItem(STORAGE_KEY, JSON.stringify(userData))
    } catch {
      // ignore
    }
  }
  saveLoginSessionCookie(userData, Boolean(rememberMe), fallbackPerm)
}

/**
 * 读取当前有效登录用户信息；若发现本地缓存或 Cookie 中的 JWT 已过期/无效，立即清理并返回空对象
 */
export function getStoredLoginUser() {
  if (typeof window === 'undefined') return {}

  const sessionObj = safeParseJson(window.sessionStorage?.getItem(STORAGE_KEY))
  if (sessionObj && sessionObj.token) {
    if (isJwtTokenValid(sessionObj.token)) {
      return sessionObj
    }
    clearLoginSession()
    return {}
  }

  const localObj = safeParseJson(window.localStorage?.getItem(STORAGE_KEY))
  if (localObj && localObj.token) {
    // 若未勾选记住我且浏览器已重建会话导致会话 Cookie 丢失，清理残留的持久化缓存
    if (getCookie(REMEMBER_ME_COOKIE) === '0' && !getLoginSessionFromCookie()) {
      clearLoginSession()
      return {}
    }
    if (isJwtTokenValid(localObj.token)) {
      return localObj
    }
    clearLoginSession()
    return {}
  }

  const cookieSession = getLoginSessionFromCookie()
  if (cookieSession && cookieSession.token) {
    if (isJwtTokenValid(cookieSession.token)) {
      const rememberMe = getCookie(REMEMBER_ME_COOKIE) === '1'
      try {
        getTargetStorage(rememberMe)?.setItem(STORAGE_KEY, JSON.stringify(cookieSession))
      } catch {
        // ignore
      }
      return cookieSession
    }
    clearLoginSession()
  }

  return {}
}

/**
 * 局部更新已登录用户的本地资料缓存（保持当前会话存储策略与 Cookie 同步）
 */
export function updateStoredLoginUser(patch = {}) {
  const current = getStoredLoginUser()
  if (!current || !current.token) return {}
  const merged = { ...current, ...patch }
  const useLocal = typeof window !== 'undefined' && Boolean(window.localStorage.getItem(STORAGE_KEY))
  const rememberMe = useLocal || getCookie(REMEMBER_ME_COOKIE) === '1'
  try {
    getTargetStorage(rememberMe)?.setItem(STORAGE_KEY, JSON.stringify(merged))
  } catch {
    // ignore
  }
  saveLoginSessionCookie(merged, rememberMe, String(merged.perm ?? '1'))
  return merged
}
