import { ref, computed } from 'vue'
import { saveThemePreferencesToCookie, readThemePreferencesFromCookie } from './cookie.js'

export const THEME_PRESETS = [
  {
    key: 'blue',
    name: '极客蓝',
    desc: '经典拂晓科技蓝',
    primary: '#1677ff',
    darkPrimary: '#2f6feb',
    darkPrimaryHover: '#388bfd',
    secondary: '#06b6d4',
    rgb: '22, 119, 255',
    darkParticleRgb: '125, 211, 252', // 暗色下更清透亮眼的冰蓝色粒子
    lightParticleRgb: '22, 119, 255',
    lightBg: '#eaf2ff',
    lightPageBg: '#e2edff',
    darkBg: '#080e1c',
    darkPageBg: '#060a14',
    tableHeadLight: '#e4eefb',
    tableRowLight: '#f6f9fe',
    tableRowAltLight: '#eef4fc',
    tableHoverLight: '#dbe8fa',
    tableBorderLight: '#d3e1f4',
    tableHeadDark: '#121b2d',
    tableRowDark: '#0c1322',
    tableRowAltDark: '#101829',
    tableHoverDark: '#18243c',
    tableBorderDark: '#1e2c46',
    orb1: 'rgba(56, 189, 248, 0.42)',
    orb2: 'rgba(37, 99, 235, 0.34)',
    orb3: 'rgba(14, 165, 233, 0.28)'
  },
  {
    key: 'teal',
    name: '青翠绿',
    desc: '清新翡翠青绿',
    primary: '#0d9488',
    darkPrimary: '#0f766e',
    darkPrimaryHover: '#14b8a6',
    secondary: '#10b981',
    rgb: '13, 148, 136',
    darkParticleRgb: '94, 234, 212', // 暗色下明亮薄荷青粒子
    lightParticleRgb: '13, 148, 136',
    lightBg: '#e6f7f4',
    lightPageBg: '#dcf2ee',
    darkBg: '#061413',
    darkPageBg: '#040e0d',
    tableHeadLight: '#dff3ef',
    tableRowLight: '#f4fbf9',
    tableRowAltLight: '#ebf7f4',
    tableHoverLight: '#d3ede8',
    tableBorderLight: '#c9e6e0',
    tableHeadDark: '#0f2422',
    tableRowDark: '#091817',
    tableRowAltDark: '#0c1e1d',
    tableHoverDark: '#14302d',
    tableBorderDark: '#1a3a37',
    orb1: 'rgba(45, 212, 191, 0.42)',
    orb2: 'rgba(16, 185, 129, 0.34)',
    orb3: 'rgba(6, 182, 212, 0.28)'
  },
  {
    key: 'violet',
    name: '紫罗兰',
    desc: '优雅星云暗紫',
    primary: '#7c3aed',
    darkPrimary: '#6d28d9',
    darkPrimaryHover: '#8b5cf6',
    secondary: '#ec4899',
    rgb: '124, 58, 237',
    darkParticleRgb: '196, 181, 253', // 暗色下星云亮紫罗兰粒子
    lightParticleRgb: '124, 58, 237',
    lightBg: '#f1ebff',
    lightPageBg: '#e9e0ff',
    darkBg: '#100a1e',
    darkPageBg: '#0b0715',
    tableHeadLight: '#ece4fb',
    tableRowLight: '#f9f6fe',
    tableRowAltLight: '#f3eefc',
    tableHoverLight: '#e3d7f8',
    tableBorderLight: '#dcd0f2',
    tableHeadDark: '#1b142d',
    tableRowDark: '#120d20',
    tableRowAltDark: '#161026',
    tableHoverDark: '#241b3b',
    tableBorderDark: '#2c2146',
    orb1: 'rgba(167, 139, 250, 0.42)',
    orb2: 'rgba(217, 70, 239, 0.32)',
    orb3: 'rgba(99, 102, 241, 0.30)'
  },
  {
    key: 'amber',
    name: '蛋黄琥珀',
    desc: '温暖鎏金蛋黄',
    primary: '#d97706',
    darkPrimary: '#b45309',
    darkPrimaryHover: '#f59e0b',
    secondary: '#eab308',
    rgb: '217, 119, 6',
    darkParticleRgb: '253, 224, 71', // 暗色下明亮温暖蛋黄色粒子
    lightParticleRgb: '217, 119, 6',
    lightBg: '#fdf4e3',
    lightPageBg: '#faeccf',
    darkBg: '#161006',
    darkPageBg: '#100b04',
    tableHeadLight: '#f6ead3',
    tableRowLight: '#fcf8f0',
    tableRowAltLight: '#f8f1e4',
    tableHoverLight: '#efe0c2',
    tableBorderLight: '#e6d5b5',
    tableHeadDark: '#261d10',
    tableRowDark: '#19130a',
    tableRowAltDark: '#1f180d',
    tableHoverDark: '#332715',
    tableBorderDark: '#3d2f1a',
    orb1: 'rgba(251, 191, 36, 0.40)',
    orb2: 'rgba(245, 158, 11, 0.32)',
    orb3: 'rgba(234, 88, 12, 0.26)'
  }
]

const cookiePrefs = readThemePreferencesFromCookie()
const savedKey = cookiePrefs.themeKey || (typeof localStorage !== 'undefined' ? (localStorage.getItem('dsh_accent_theme') || 'blue') : 'blue')
const activeThemeKey = ref(THEME_PRESETS.some(t => t.key === savedKey) ? savedKey : 'blue')

function mixHex(hex1, hex2, weight) {
  const d2h = (d) => d.toString(16).padStart(2, '0')
  const h2d = (h) => parseInt(h, 16)
  const c1 = hex1.replace('#', '')
  const c2 = hex2.replace('#', '')
  const r = Math.round(h2d(c1.slice(0, 2)) * (1 - weight) + h2d(c2.slice(0, 2)) * weight)
  const g = Math.round(h2d(c1.slice(2, 4)) * (1 - weight) + h2d(c2.slice(2, 4)) * weight)
  const b = Math.round(h2d(c1.slice(4, 6)) * (1 - weight) + h2d(c2.slice(4, 6)) * weight)
  return `#${d2h(r)}${d2h(g)}${d2h(b)}`
}

export function applyThemePalette(themeKey, isDarkMode = null) {
  const preset = THEME_PRESETS.find(t => t.key === themeKey) || THEME_PRESETS[0]
  activeThemeKey.value = preset.key
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('dsh_accent_theme', preset.key)
  }
  if (typeof document === 'undefined') return

  const root = document.documentElement
  const dark = isDarkMode !== null ? isDarkMode : root.classList.contains('dark')
  saveThemePreferencesToCookie({ themeKey: preset.key, isDark: dark })
  const baseColor = dark ? preset.darkPrimary : preset.primary
  const mixTarget = dark ? preset.darkBg : '#ffffff'

  root.setAttribute('data-accent', preset.key)
  root.style.setProperty('--theme-primary', preset.primary)
  root.style.setProperty('--theme-dark-primary', preset.darkPrimary)
  root.style.setProperty('--theme-dark-hover', preset.darkPrimaryHover)
  root.style.setProperty('--theme-secondary', preset.secondary)
  root.style.setProperty('--theme-rgb', preset.rgb)
  root.style.setProperty('--theme-particle-dark', preset.darkParticleRgb)
  root.style.setProperty('--theme-particle-light', preset.lightParticleRgb)
  root.style.setProperty('--theme-orb-1', preset.orb1)
  root.style.setProperty('--theme-orb-2', preset.orb2)
  root.style.setProperty('--theme-orb-3', preset.orb3)
  root.style.setProperty('--theme-bg-light', preset.lightBg)
  root.style.setProperty('--theme-bg-page-light', preset.lightPageBg)
  root.style.setProperty('--theme-bg-dark', preset.darkBg)
  root.style.setProperty('--theme-bg-page-dark', preset.darkPageBg)
  root.style.setProperty('--el-bg-color', dark ? preset.darkBg : preset.lightBg)
  root.style.setProperty('--el-bg-color-page', dark ? preset.darkPageBg : preset.lightPageBg)
  root.style.setProperty('--el-bg-color-overlay', dark ? preset.tableRowAltDark : preset.tableRowLight)
  root.style.setProperty('--el-card-bg-color', dark ? preset.tableRowDark : preset.tableRowLight)
  root.style.setProperty('--el-fill-color-light', dark ? preset.tableHoverDark : preset.tableRowAltLight)
  root.style.setProperty('--el-fill-color-blank', dark ? preset.tableRowDark : preset.tableRowLight)
  root.style.setProperty('--el-border-color', dark ? preset.tableBorderDark : preset.tableBorderLight)
  root.style.setProperty('--el-border-color-light', dark ? preset.tableBorderDark : preset.tableBorderLight)
  root.style.setProperty('--mica-bg-light', preset.tableRowLight)
  root.style.setProperty('--mica-bg-strong-light', preset.tableRowAltLight)
  root.style.setProperty('--mica-border-light', preset.tableBorderLight)
  root.style.setProperty('--mica-bg-dark', preset.tableRowDark)
  root.style.setProperty('--mica-bg-strong-dark', preset.tableRowAltDark)
  root.style.setProperty('--mica-border-dark', preset.tableBorderDark)
  root.style.setProperty('--table-head-bg', dark ? preset.tableHeadDark : preset.tableHeadLight)
  root.style.setProperty('--table-row-bg', dark ? preset.tableRowDark : preset.tableRowLight)
  root.style.setProperty('--table-row-alt-bg', dark ? preset.tableRowAltDark : preset.tableRowAltLight)
  root.style.setProperty('--table-hover-bg', dark ? preset.tableHoverDark : preset.tableHoverLight)
  root.style.setProperty('--table-border-color', dark ? preset.tableBorderDark : preset.tableBorderLight)

  // 动态同步 Element Plus 主色与阶梯色阶（暗色模式下降低按钮填充亮度，柔和不刺眼）
  root.style.setProperty('--el-color-primary', baseColor)
  root.style.setProperty('--el-color-primary-light-3', dark ? preset.darkPrimaryHover : mixHex(baseColor, mixTarget, 0.3))
  root.style.setProperty('--el-color-primary-light-5', mixHex(baseColor, mixTarget, 0.5))
  root.style.setProperty('--el-color-primary-light-7', mixHex(baseColor, mixTarget, 0.7))
  root.style.setProperty('--el-color-primary-light-8', mixHex(baseColor, mixTarget, 0.8))
  root.style.setProperty('--el-color-primary-light-9', mixHex(baseColor, mixTarget, 0.9))
  root.style.setProperty('--el-color-primary-dark-2', mixHex(baseColor, '#000000', 0.2))

  window.dispatchEvent(new CustomEvent('accent-theme-change', { detail: preset }))
}

export const FONT_SIZE_PRESETS = [
  { key: '14px', label: '标准 (14px)', size: '14px', scale: 1 },
  { key: '15px', label: '舒适中号 (15px)', size: '15px', scale: 1.07 },
  { key: '16px', label: '大字护眼 (16px)', size: '16px', scale: 1.15 },
  { key: '18px', label: '特大关怀 (18px)', size: '18px', scale: 1.25 }
]

const savedFontSize = cookiePrefs.fontSize || (typeof localStorage !== 'undefined' ? (localStorage.getItem('star_realm_font_size') || '14px') : '14px')
export const activeFontSize = ref(FONT_SIZE_PRESETS.some(f => f.key === savedFontSize) ? savedFontSize : '14px')

export function applyGlobalFontSize(sizeKey = activeFontSize.value) {
  const preset = FONT_SIZE_PRESETS.find(f => f.key === sizeKey) || FONT_SIZE_PRESETS[0]
  activeFontSize.value = preset.key
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('star_realm_font_size', preset.key)
  }
  saveThemePreferencesToCookie({ fontSize: preset.key })
  if (typeof document === 'undefined') return
  const root = document.documentElement
  const px = parseInt(preset.size, 10) || 14
  root.setAttribute('data-font-size', preset.key)
  root.style.setProperty('--app-base-font-size', `${px}px`)
  const scale = Number(preset.scale || (px / 14)).toFixed(3)
  root.style.setProperty('--app-font-scale', scale)
  root.style.setProperty('--el-font-size-base', `${px}px`)
  root.style.setProperty('--el-font-size-small', `${Math.max(12, px - 1)}px`)
  root.style.setProperty('--el-font-size-extra-small', `${Math.max(11, px - 2)}px`)
  root.style.setProperty('--el-font-size-medium', `${px + 2}px`)
  root.style.setProperty('--el-font-size-large', `${px + 4}px`)
  root.style.setProperty('--el-font-size-extra-large', `${px + 6}px`)

  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('font-size-change', { detail: { sizeKey: preset.key, scale: Number(scale), px } }))
  }
}

export function getChartFontScale() {
  if (typeof document === 'undefined') return 1
  const s = parseFloat(document.documentElement.style.getPropertyValue('--app-font-scale'))
  return !isNaN(s) && s > 0 ? s : (FONT_SIZE_PRESETS.find(f => f.key === activeFontSize.value)?.scale || 1)
}

if (typeof document !== 'undefined') {
  applyGlobalFontSize(activeFontSize.value)
}

export function useThemePalette() {
  const currentPreset = computed(() => THEME_PRESETS.find(t => t.key === activeThemeKey.value) || THEME_PRESETS[0])
  return {
    presets: THEME_PRESETS,
    activeThemeKey,
    currentPreset,
    applyThemePalette,
    fontSizePresets: FONT_SIZE_PRESETS,
    activeFontSize,
    applyGlobalFontSize,
    getChartFontScale
  }
}
