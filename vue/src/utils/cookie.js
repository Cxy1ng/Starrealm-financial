/**
 * 星界 (Star Realm) 浏览器 Cookie 统一管理工具
 * 用途：
 * 1. 登录态记住我（7天免登录会话凭证与上次登录账号/角色记忆，配合后端 HttpOnly Cookie 双轨鉴权）
 * 2. 主题色（5套随心配）、明暗模式与全局字号偏好的跨会话 Cookie 持久化（365天）
 * 3. 《星界用户服务协议》与《隐私保护政策》同意状态及版本号绑定（sr_legal_agreed=v2026.1，协议版本升级时自动重签）
 */

export const LEGAL_CONSENT_COOKIE = 'sr_legal_agreed'
export const LEGAL_CONSENT_VERSION = 'v2026.1'

export const THEME_PALETTE_COOKIE = 'sr_theme_palette'
export const DARK_MODE_COOKIE = 'sr_dark_mode'
export const FONT_SIZE_COOKIE = 'sr_font_size'

export const AUTH_SESSION_COOKIE = 'sr_auth_session'
export const LAST_USER_COOKIE = 'sr_last_username'
export const LAST_PERM_COOKIE = 'sr_last_perm'
export const REMEMBER_ME_COOKIE = 'sr_remember_me'

export function setCookie(name, value, options = {}) {
  if (typeof document === 'undefined') return
  const {
    days = 30,
    path = '/',
    sameSite = 'Lax'
  } = options

  let cookieStr = `${encodeURIComponent(name)}=${encodeURIComponent(String(value ?? ''))}`
  if (typeof days === 'number') {
    const maxAge = Math.floor(days * 24 * 60 * 60)
    cookieStr += `; Max-Age=${maxAge}`
    const expires = new Date(Date.now() + maxAge * 1000).toUTCString()
    cookieStr += `; Expires=${expires}`
  }
  if (path) cookieStr += `; Path=${path}`
  if (sameSite) cookieStr += `; SameSite=${sameSite}`
  document.cookie = cookieStr
}

export function getCookie(name) {
  if (typeof document === 'undefined' || !document.cookie) return null
  const target = `${encodeURIComponent(name)}=`
  const parts = document.cookie.split(';')
  for (let i = 0; i < parts.length; i++) {
    const part = parts[i].trim()
    if (part.indexOf(target) === 0) {
      try {
        return decodeURIComponent(part.substring(target.length))
      } catch {
        return part.substring(target.length)
      }
    }
  }
  return null
}

export function removeCookie(name, path = '/') {
  if (typeof document === 'undefined') return
  document.cookie = `${encodeURIComponent(name)}=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=${path}; SameSite=Lax`
}

/**
 * 检查用户是否已同意当前版本 (v2026.1) 的《星界用户服务协议》与《隐私保护政策》
 */
export function hasAgreedLegalTerms() {
  const cookieVal = getCookie(LEGAL_CONSENT_COOKIE)
  if (cookieVal === LEGAL_CONSENT_VERSION) {
    return true
  }
  return localStorage.getItem('xingjie_agreed_terms_ver') === LEGAL_CONSENT_VERSION
}

/**
 * 记录用户已同意当前版本的《星界用户服务协议》与《隐私保护政策》（有效期 180 天）
 */
export function setAgreedLegalTerms() {
  setCookie(LEGAL_CONSENT_COOKIE, LEGAL_CONSENT_VERSION, { days: 180 })
  localStorage.setItem('xingjie_agreed_terms', '1')
  localStorage.setItem('xingjie_agreed_terms_ver', LEGAL_CONSENT_VERSION)
}

/**
 * 保存主题色、明暗模式与全局字号偏好到 Cookie（有效期 365 天）
 */
export function saveThemePreferencesToCookie({ themeKey, isDark, fontSize } = {}) {
  if (themeKey) {
    setCookie(THEME_PALETTE_COOKIE, themeKey, { days: 365 })
  }
  if (typeof isDark === 'boolean') {
    setCookie(DARK_MODE_COOKIE, isDark ? '1' : '0', { days: 365 })
  }
  if (fontSize) {
    setCookie(FONT_SIZE_COOKIE, fontSize, { days: 365 })
  }
}

/**
 * 从 Cookie 读取主题色、明暗模式与全局字号偏好
 */
export function readThemePreferencesFromCookie() {
  const themeKey = getCookie(THEME_PALETTE_COOKIE) || null
  const darkRaw = getCookie(DARK_MODE_COOKIE)
  const isDark = darkRaw === '1' ? true : (darkRaw === '0' ? false : null)
  const fontSize = getCookie(FONT_SIZE_COOKIE) || null
  return { themeKey, isDark, fontSize }
}

/**
 * 从 Cookie 读取上次记住的登录账号与角色偏好
 */
export function readRememberedAccountFromCookie() {
  const username = getCookie(LAST_USER_COOKIE) || ''
  const perm = getCookie(LAST_PERM_COOKIE) || '1'
  const rememberMe = getCookie(REMEMBER_ME_COOKIE) === '1'
  return { username, perm, rememberMe }
}

/**
 * 保存登录会话与记住我偏好到 Cookie
 */
export function saveLoginSessionCookie(userData, rememberMe = true, fallbackPerm = '1') {
  if (!userData) return
  const days = rememberMe ? 7 : null
  try {
    // 仅存储轻量认证会话摘要，避免超出单个 Cookie 4KB 限制
    const slimSession = {
      id: userData.id,
      username: userData.username,
      nickname: userData.nickname,
      role: userData.role,
      perm: userData.perm ?? fallbackPerm,
      avatar: userData.avatar,
      hasApiKey: Boolean(userData.hasApiKey),
      needChangePwd: userData.needChangePwd ?? 0,
      token: userData.token
    }
    setCookie(AUTH_SESSION_COOKIE, JSON.stringify(slimSession), { days })
  } catch {
    // ignore
  }
  if (userData.username) {
    setCookie(LAST_USER_COOKIE, userData.username, { days: 30 })
  }
  const permToSave = userData.perm ?? fallbackPerm
  if (permToSave !== undefined && permToSave !== null) {
    setCookie(LAST_PERM_COOKIE, String(permToSave), { days: 30 })
  }
  setCookie(REMEMBER_ME_COOKIE, rememberMe ? '1' : '0', { days: 30 })
}

/**
 * 从 Cookie 恢复登录会话（当 localStorage 被清理或跨窗口免登录时自动恢复）
 */
export function getLoginSessionFromCookie() {
  const raw = getCookie(AUTH_SESSION_COOKIE)
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw)
    if (parsed && parsed.token) {
      return parsed
    }
  } catch {
    // ignore
  }
  return null
}

/**
 * 退出登录或凭证失效时清除登录会话 Cookie
 */
export function clearLoginSessionCookie() {
  removeCookie(AUTH_SESSION_COOKIE)
}
