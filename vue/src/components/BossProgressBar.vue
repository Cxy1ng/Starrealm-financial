<template>
  <div class="boss-progress-row" :class="{ 'is-large': large }">
    <div class="boss-bar-track" ref="trackRef">
      <!-- 第 1 层：系统主题色 (0% ~ 100%) -->
      <div
        class="boss-bar-layer layer-theme"
        :style="{ width: stage1Width + '%' }"
      ></div>

      <!-- 第 2 层：星云紫 (100% ~ 200%, 1X) -->
      <div
        v-if="displayPct > 100"
        class="boss-bar-layer layer-purple"
        :style="{ width: stage2Width + '%' }"
      ></div>

      <!-- 第 3 层：烈焰红 (200% ~ 300%, 2X) -->
      <div
        v-if="displayPct > 200"
        class="boss-bar-layer layer-red"
        :style="{ width: stage3Width + '%' }"
      ></div>

      <!-- 第 4 层及以上：流光金 (300%+, 3X / 4X / NX 循环充能叠层) -->
      <div
        v-if="displayPct > 300"
        class="boss-bar-layer layer-gold-base"
        :style="{ width: goldBaseWidth + '%' }"
      ></div>
      <div
        v-if="displayPct > 400 && goldTopWidth > 0"
        class="boss-bar-layer layer-gold-top"
        :style="{ width: goldTopWidth + '%' }"
      ></div>

      <!-- Canvas 真实物理粒子与仿真分形电弧 + 龟派气功彗星尾焰渲染层 -->
      <canvas ref="canvasRef" class="boss-fx-canvas"></canvas>
    </div>

    <!-- 右侧倍率徽章與百分比读数：紫色 1X，红色 2X，金色第一层 3X，金色第二层 4X，以此类推 -->
    <span class="boss-bar-label num-font" :class="'label-stage-' + activeStage">
      <span v-if="bonusMultiplier >= 1" class="stage-tag">{{ bonusMultiplier }}X</span>
      {{ Math.round(displayPct) }}%
    </span>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  percent: {
    type: Number,
    default: 0
  },
  large: {
    type: Boolean,
    default: false
  },
  animateTrigger: {
    type: Number,
    default: 0
  },
  fromPercent: {
    type: Number,
    default: 0
  }
})

const trackRef = ref(null)
const canvasRef = ref(null)
const displayPct = ref(Math.max(0, Number(props.percent) || 0))
const isCharging = ref(false)
const chargePaused = ref(false) // 跨 100% 边界时的满格蓄力停顿：让彗星先消散，再在下一层重新点燃
const chargeVelocity = ref(0) // 当前推进瞬时速度，用于驱动彗星尾焰长度与亮度

// 当前视觉阶级：1=主题色(0~100%), 2=紫色(100~200%), 3=红色(200~300%), 4=金色(300%+)
const activeStage = computed(() => {
  const p = displayPct.value
  if (p > 300) return 4
  if (p > 200) return 3
  if (p > 100) return 2
  return 1
})

// 倍率徽章：紫色(100%+)为 1X，红色(200%+)为 2X，金色第一层(300%+)为 3X，金色第二层(400%+)为 4X，以此类推
const bonusMultiplier = computed(() => {
  const p = displayPct.value
  if (p <= 100) return 0
  return Math.floor((p - 0.0001) / 100)
})

// 各层严格顺序宽度计算
const stage1Width = computed(() => Math.min(Math.max(displayPct.value, 0), 100))
const stage2Width = computed(() => {
  if (displayPct.value <= 100) return 0
  return Math.min(displayPct.value - 100, 100)
})
const stage3Width = computed(() => {
  if (displayPct.value <= 200) return 0
  return Math.min(displayPct.value - 200, 100)
})
// 金色底层（300~400% 逐渐涨满；超过 400% 后底层保持 100% 满格托底，新一轮金色在顶层推进）
const goldBaseWidth = computed(() => {
  const p = displayPct.value
  if (p <= 300) return 0
  if (p <= 400) return p - 300
  return 100
})
const goldTopWidth = computed(() => {
  const p = displayPct.value
  if (p <= 400) return 0
  const rem = (p - 300) % 100
  return rem === 0 ? 100 : rem
})

// 当前活跃层的填充百分比 (0 ~ 100)，即已充能区域右边界以及推进彗星头部所在百分比
const activeFillRatio = computed(() => {
  const p = displayPct.value
  if (p <= 0) return 0
  const rem = p % 100
  return rem === 0 ? 100 : rem
})

// ==================== 1. 逐层加速顺序充能物理引擎 ====================
let chargeRafId = null

const startSequentialCharge = (startVal, targetVal) => {
  if (chargeRafId) cancelAnimationFrame(chargeRafId)
  const from = Math.max(0, Number(startVal) || 0)
  const to = Math.max(0, Number(targetVal) || 0)

  if (to <= from) {
    displayPct.value = to
    isCharging.value = false
    chargePaused.value = false
    chargeVelocity.value = 0
    return
  }

  displayPct.value = from
  isCharging.value = true
  chargePaused.value = false
  let current = from
  let segmentStart = from
  let pauseFrames = 0
  let vel = 0.08 // 初始慢速起步，随后在每一层内逐渐加速推进

  const step = () => {
    if (pauseFrames > 0) {
      pauseFrames--
      chargeVelocity.value = 0.15
      if (pauseFrames === 0) chargePaused.value = false
      chargeRafId = requestAnimationFrame(step)
      return
    }

    const currentFloor = Math.floor(current / 100) * 100
    const nextBoundary = currentFloor + 100
    const segmentTarget = Math.min(to, nextBoundary)
    const distInSegment = Math.max(0, current - segmentStart)

    // 逐渐加速曲线：起步柔和 (0.14%/frame)，随着推进距离呈二次方加速，最高可达 1.65%/frame
    const accel = 0.14 + Math.pow(distInSegment / 65, 1.55) * 1.35
    vel = Math.min(1.65, accel)
    chargeVelocity.value = vel

    current += vel

    if (current >= segmentTarget) {
      current = segmentTarget
      displayPct.value = current
      // 如果还没到最终目标（跨越了 100% 阶段边界），满格蓄力停顿 16 帧再从 0% 加速冲刺下一层
      if (current < to) {
        pauseFrames = 16
        chargePaused.value = true
        segmentStart = current
        vel = 0.08
        chargeRafId = requestAnimationFrame(step)
      } else {
        displayPct.value = to
        // 不再定格 380ms 后瞬间消失：交给 Canvas 的淡出包络自然收尾
        chargePaused.value = false
        isCharging.value = false
      }
    } else {
      displayPct.value = current
      chargeRafId = requestAnimationFrame(step)
    }
  }

  chargeRafId = requestAnimationFrame(step)
}

watch(
  () => props.percent,
  (newVal, oldVal) => {
    const target = Math.max(0, Number(newVal) || 0)
    const prev = oldVal !== undefined ? Math.max(0, Number(oldVal) || 0) : target
    if (!isCharging.value) {
      if (target > prev && prev >= 0 && props.animateTrigger > 0) {
        startSequentialCharge(props.fromPercent ?? prev, target)
      } else {
        displayPct.value = target
      }
    }
  }
)

watch(
  () => props.animateTrigger,
  (trig) => {
    if (trig > 0) {
      startSequentialCharge(props.fromPercent, props.percent)
    }
  }
)

// ==================== 2. Canvas 间歇性微粒 / 仿真分形闪电 / 龟派气功彗星尾焰引擎 ====================
let fxRafId = null
let resizeObserver = null
const ambientParticles = [] // 间歇性随机生成并消散的灰尘微粒/能量粒子
const ambientBolts = []     // 随机间歇性闪烁的分形小电弧/雷电束
const tailParticles = []    // 龟派气功彗星尾焰高速喷射流粒子
const cometArcs = []        // 彗星头部持续撕裂、短暂滞留的分形电弧簇
let cometOpacity = 0        // 彗星整体透明度：起步淡入、结束后缓慢消散，而非瞬间消失

const PAD_X = 36
const PAD_Y = 22

// 获取当前主题色 RGB
const getThemeColorHex = () => {
  if (typeof window === 'undefined') return '#409eff'
  const style = getComputedStyle(document.documentElement)
  return style.getPropertyValue('--el-color-primary').trim() || '#409eff'
}

// ---------- 丁达尔尘埃（Tyndall Dust）素材：按阶段预烘焙柔光晕贴图 ----------
// 每颗尘埃 = 一个极小的白色亮芯 + 一圈低透明度阶段色光晕（贴图缩放），
// 视觉上是「阳光里的浮尘」而不是会发光的 LED 圆点；贴图复用，避免逐粒子重建径向渐变。
const MOTE_PALETTE = {
  2: { glow: '192, 132, 252', core: '246, 239, 255' }, // 星云紫
  3: { glow: '248, 113, 113', core: '255, 242, 242' }, // 烈焰红
  4: { glow: '253, 224, 71', core: '255, 253, 240' }   // 黄金
}
const moteSpriteCache = {}
const buildMoteSprite = (glowRgb, coreRgb) => {
  const size = 64
  const cvs = document.createElement('canvas')
  cvs.width = size
  cvs.height = size
  const g = cvs.getContext('2d')
  const half = size / 2
  const grad = g.createRadialGradient(half, half, 0, half, half, half)
  grad.addColorStop(0, `rgba(${coreRgb}, 1)`)
  grad.addColorStop(0.16, `rgba(${coreRgb}, 0.9)`)
  grad.addColorStop(0.34, `rgba(${glowRgb}, 0.36)`)
  grad.addColorStop(0.62, `rgba(${glowRgb}, 0.09)`)
  grad.addColorStop(1, `rgba(${glowRgb}, 0)`)
  g.fillStyle = grad
  g.fillRect(0, 0, size, size)
  return cvs
}
const getMoteSprite = (stage) => {
  if (moteSpriteCache[stage]) return moteSpriteCache[stage]
  const pal = MOTE_PALETTE[stage] || MOTE_PALETTE[4]
  const sprite = buildMoteSprite(pal.glow, pal.core)
  moteSpriteCache[stage] = sprite
  return sprite
}

// Stage 1 的彗星尾尘跟随主题色，主题切换时重新烘焙
let themeMoteSprite = null
let themeMoteHex = ''
const getThemeMoteSprite = () => {
  const hex = getThemeColorHex()
  if (!themeMoteSprite || themeMoteHex !== hex) {
    themeMoteSprite = buildMoteSprite(hexToRgb(hex), '255, 255, 255')
    themeMoteHex = hex
  }
  return themeMoteSprite
}

// 各阶段浮尘密度：紫 < 红 < 金；颗粒半径：紫 > 红 > 金（越来越细）
// density 为每像素已充能长度对应的目标颗粒数，min/max 兜底，保证任何条宽下都是金 > 红 > 紫。
// spawn 为每帧期望生成数，用累加器处理小数，保证低帧率下也不会漏生成。
const MOTE_CONFIG = {
  2: { min: 10, max: 30, density: 0.09, spawn: 0.5, rMin: 0.55, rMax: 1.1, lifeMin: 42, lifeMax: 88 },
  3: { min: 14, max: 50, density: 0.15, spawn: 0.95, rMin: 0.4, rMax: 0.85, lifeMin: 34, lifeMax: 74 },
  4: { min: 18, max: 74, density: 0.23, spawn: 1.55, rMin: 0.3, rMax: 0.72, lifeMin: 28, lifeMax: 66 }
}
let moteSpawnAcc = 0
let lastFxStage = 0

// 生成一条两点之间的随机分形锯齿闪电路径
const createFractalBoltPoints = (x1, y1, x2, y2, displace, generations = 3) => {
  let pts = [{ x: x1, y: y1 }, { x: x2, y: y2 }]
  for (let g = 0; g < generations; g++) {
    const next = []
    for (let i = 0; i < pts.length - 1; i++) {
      const p1 = pts[i]
      const p2 = pts[i + 1]
      const midX = (p1.x + p2.x) * 0.5 + (Math.random() - 0.5) * displace * 0.45
      const midY = (p1.y + p2.y) * 0.5 + (Math.random() - 0.5) * displace
      next.push(p1, { x: midX, y: midY })
    }
    next.push(pts[pts.length - 1])
    pts = next
    displace *= 0.55
  }
  return pts
}

// 电流配色：高亮芯线偏白，外辉光用阶段色
const BOLT_PALETTE = {
  2: { core: '240, 228, 255', glow: '168, 85, 247' },
  3: { core: '255, 228, 228', glow: '239, 68, 68' },
  4: { core: '255, 255, 255', glow: '250, 204, 21' }
}

// 在主干之外随机生出一根分叉，模拟真实放电的枝状末端
const createBolt = (x1, y1, x2, y2, displace, generations = 2, branchChance = 0) => {
  const main = createFractalBoltPoints(x1, y1, x2, y2, displace, generations)
  const branches = []
  if (branchChance > 0 && main.length > 5 && Math.random() < branchChance) {
    const from = main[2 + Math.floor(Math.random() * (main.length - 4))]
    const baseAngle = Math.atan2(y2 - y1, x2 - x1)
    const angle = baseAngle + (Math.random() - 0.5) * 1.9
    const len = Math.min(16, Math.max(5, Math.hypot(x2 - x1, y2 - y1) * (0.24 + Math.random() * 0.32)))
    branches.push(
      createFractalBoltPoints(
        from.x, from.y,
        from.x + Math.cos(angle) * len,
        from.y + Math.sin(angle) * len,
        displace * 0.55,
        Math.max(1, generations - 1)
      )
    )
  }
  return { main, branches }
}

// 绘制放电路径：先铺一层宽而淡的外辉光，再逐段收细高亮芯线（尾端自然变尖）
const drawBolt = (ctx, bolt, palette, width, alpha) => {
  if (alpha <= 0.01) return
  ctx.save()
  ctx.globalCompositeOperation = 'lighter'
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'

  // 外辉光：整条路径一次描边，成本低且边缘柔和
  ctx.beginPath()
  bolt.main.forEach((pt, i) => (i ? ctx.lineTo(pt.x, pt.y) : ctx.moveTo(pt.x, pt.y)))
  bolt.branches.forEach((bp) => {
    bp.forEach((pt, i) => (i ? ctx.lineTo(pt.x, pt.y) : ctx.moveTo(pt.x, pt.y)))
  })
  ctx.strokeStyle = `rgba(${palette.glow}, ${alpha * 0.42})`
  ctx.lineWidth = width * 3.4
  ctx.shadowColor = `rgba(${palette.glow}, ${alpha * 0.9})`
  ctx.shadowBlur = 9
  ctx.stroke()

  // 高亮芯线：逐段衰减线宽，头部粗、尾端细，避免均匀线条的“塑料感”
  ctx.shadowBlur = 0
  ctx.strokeStyle = `rgba(${palette.core}, ${alpha})`
  const taper = (pts, baseWidth, decay) => {
    for (let i = 0; i < pts.length - 1; i++) {
      const k = i / (pts.length - 1)
      ctx.beginPath()
      ctx.moveTo(pts[i].x, pts[i].y)
      ctx.lineTo(pts[i + 1].x, pts[i + 1].y)
      ctx.lineWidth = Math.max(0.3, baseWidth * (1 - k * decay))
      ctx.stroke()
    }
  }
  taper(bolt.main, width, 0.8)
  bolt.branches.forEach((bp) => taper(bp, width * 0.55, 0.85))

  ctx.restore()
}

// 主题色 hex -> rgb（Stage 1 的彗星跟随主题色，需要带 alpha 的 rgba 渐变）
const hexToRgb = (hex) => {
  let h = String(hex || '').trim().replace('#', '')
  if (h.length === 3) h = h.split('').map((c) => c + c).join('')
  const n = parseInt(h, 16)
  if (!Number.isFinite(n)) return '64, 158, 255'
  return `${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}`
}

const setupCanvas = () => {
  const track = trackRef.value
  const cvs = canvasRef.value
  if (!track || !cvs) return
  const rect = track.getBoundingClientRect()
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  const w = Math.max(100, rect.width)
  const h = Math.max(8, rect.height)
  cvs.width = (w + PAD_X * 2) * dpr
  cvs.height = (h + PAD_Y * 2) * dpr
  cvs.style.width = `${w + PAD_X * 2}px`
  cvs.style.height = `${h + PAD_Y * 2}px`
  cvs.style.left = `-${PAD_X}px`
  cvs.style.top = `-${PAD_Y}px`
}

const renderFxLoop = () => {
  const track = trackRef.value
  const cvs = canvasRef.value
  if (track && cvs) {
    const ctx = cvs.getContext('2d')
    const dpr = Math.min(window.devicePixelRatio || 1, 2)
    const trackW = cvs.width / dpr - PAD_X * 2
    const trackH = cvs.height / dpr - PAD_Y * 2

    ctx.save()
    ctx.scale(dpr, dpr)
    ctx.clearRect(0, 0, trackW + PAD_X * 2, trackH + PAD_Y * 2)

    const stage = activeStage.value
    const fillRatio = activeFillRatio.value / 100
    const filledW = Math.max(0, Math.min(1, fillRatio)) * trackW
    const barCenterY = PAD_Y + trackH * 0.5

    // ---------- A. 已充能区域：丁达尔浮尘 + 仿真电弧 (Stage >= 2) ----------
    if (stage >= 2 && filledW > 6) {
      // 阶段切换时丢弃旧色浮尘与旧电弧，避免紫/红/金颗粒混色
      if (stage !== lastFxStage) {
        ambientParticles.length = 0
        ambientBolts.length = 0
        moteSpawnAcc = 0
        lastFxStage = stage
      }

      const cfg = MOTE_CONFIG[stage]
      // 短血条按面积折减，长血条保持满密度，避免小卡片里挤成一片光斑
      const maxMotes = Math.max(cfg.min, Math.min(cfg.max, Math.round(filledW * cfg.density)))

      // 累加式生成：金最多、红次之、紫最少，且不受帧率波动影响
      moteSpawnAcc += cfg.spawn
      while (moteSpawnAcc >= 1) {
        moteSpawnAcc -= 1
        if (ambientParticles.length >= maxMotes) break
        // 半径向小值偏置（pow 1.5），整体更接近真实微尘而非彩色光点
        const r = cfg.rMin + Math.pow(Math.random(), 1.5) * (cfg.rMax - cfg.rMin)
        ambientParticles.push({
          x: PAD_X + Math.random() * filledW,
          // 允许少量浮尘溢出血条上下边缘，像悬浮在空气里的灰尘
          y: barCenterY + (Math.random() - 0.5) * (trackH + 14),
          vx: (Math.random() - 0.5) * 0.22,
          vy: (Math.random() - 0.5) * 0.34,
          r,
          phase: Math.random() * Math.PI * 2,
          life: 0,
          maxLife: cfg.lifeMin + Math.random() * (cfg.lifeMax - cfg.lifeMin),
          stage
        })
      }

      // 更新并绘制浮尘：正弦呼吸包络 + 轻微闪烁，缓慢漂浮后消散
      ctx.save()
      ctx.globalCompositeOperation = 'lighter'
      for (let i = ambientParticles.length - 1; i >= 0; i--) {
        const m = ambientParticles[i]
        m.life++
        // 轻微横向摆动 + 缓慢上下悬浮，模拟空气扰动
        m.x += m.vx + Math.sin((m.life + m.phase * 10) * 0.035) * 0.08
        m.y += m.vy
        if (
          m.life >= m.maxLife ||
          m.x > PAD_X + filledW + 6 ||
          m.y < barCenterY - trackH * 0.5 - 12 ||
          m.y > barCenterY + trackH * 0.5 + 12
        ) {
          ambientParticles.splice(i, 1)
          continue
        }

        // 正弦包络实现呼吸式浮现与消散，再叠一层缓慢闪烁
        const progress = m.life / m.maxLife
        const alpha = Math.sin(progress * Math.PI) *
          (0.72 + 0.28 * Math.sin((m.life + m.phase * 12) * 0.21)) * 0.9
        if (alpha <= 0.01) continue

        // 低透明度阶段色柔光晕 + 微型白色亮芯，叠加后呈现「光穿过浮尘」的体积感
        const halo = m.r * 7
        ctx.globalAlpha = alpha
        ctx.drawImage(getMoteSprite(m.stage), m.x - halo, m.y - halo, halo * 2, halo * 2)
        ctx.globalAlpha = Math.min(1, alpha * 0.95)
        ctx.fillStyle = '#ffffff'
        ctx.beginPath()
        ctx.arc(m.x, m.y, Math.max(0.3, m.r * 0.62), 0, Math.PI * 2)
        ctx.fill()
      }
      ctx.restore()

      // 间歇性随机迸发的仿真分形电弧（Stage 2 细微紫电，Stage 3 跃动红电，Stage 4 强电金色雷霆）
      const boltCfg = stage === 2
        ? { chance: 0.16, max: 2, width: 0.8, branch: 0 }
        : stage === 3
          ? { chance: 0.3, max: 4, width: 1.0, branch: 0.35 }
          : { chance: 0.5, max: 6, width: 1.25, branch: 0.6 }
      if (ambientBolts.length < boltCfg.max && Math.random() < boltCfg.chance) {
        const spanMin = stage === 2 ? 12 : stage === 3 ? 18 : 26
        const spanMax = stage === 2 ? 26 : stage === 3 ? 42 : Math.min(85, Math.max(32, filledW * 0.45))
        const segLen = Math.min(filledW, spanMin + Math.random() * (spanMax - spanMin))
        const startX = PAD_X + Math.random() * Math.max(1, filledW - segLen)
        const endX = startX + segLen
        const yOffset1 = (Math.random() - 0.5) * (trackH + (stage === 4 ? 8 : 4))
        const yOffset2 = (Math.random() - 0.5) * (trackH + (stage === 4 ? 8 : 4))
        const displace = stage === 2 ? 4.5 : stage === 3 ? 7.5 : 11

        ambientBolts.push({
          ...createBolt(
            startX, barCenterY + yOffset1,
            endX, barCenterY + yOffset2,
            displace, 3, boltCfg.branch
          ),
          life: 0,
          maxLife: stage === 2
            ? (7 + Math.floor(Math.random() * 5))
            : stage === 3
              ? (8 + Math.floor(Math.random() * 6))
              : (9 + Math.floor(Math.random() * 7)),
          width: boltCfg.width * (0.85 + Math.random() * 0.3),
          phase: Math.random() * Math.PI * 2,
          stage
        })
      }

      for (let i = ambientBolts.length - 1; i >= 0; i--) {
        const b = ambientBolts[i]
        b.life++
        if (b.life >= b.maxLife) {
          ambientBolts.splice(i, 1)
          continue
        }
        const progress = b.life / b.maxLife
        // 两端归零的正弦包络 + 轻微闪烁：出现与消失都不再是硬切
        const alpha = Math.sin(progress * Math.PI) *
          (0.82 + 0.18 * Math.sin(b.life * 2.1 + b.phase))
        drawBolt(ctx, b, BOLT_PALETTE[b.stage], b.width, alpha)
      }
    } else {
      ambientParticles.length = 0
      ambientBolts.length = 0
      moteSpawnAcc = 0
      lastFxStage = stage
    }

    // ---------- B. 存一笔即时推进特效：彗星尾焰 + 前端奔涌电弧 ----------
    // 透明度包络：起步快速淡入；推进结束后低速淡出，不再“定格 → 突然消失”
    const cometTarget = isCharging.value && !chargePaused.value ? 1 : 0
    // 跨层蓄力时快速熄灭（避免白核停在满格末端），正常结束则缓慢消散
    const cometFadeRate = cometTarget > cometOpacity ? 0.2 : (chargePaused.value ? 0.24 : 0.11)
    cometOpacity += (cometTarget - cometOpacity) * cometFadeRate
    if (Math.abs(cometOpacity - cometTarget) < 0.004) cometOpacity = cometTarget

    if (cometOpacity > 0.01 && filledW > 1) {
      const headX = PAD_X + filledW
      const headY = barCenterY
      const speedFactor = Math.min(1.8, Math.max(0.45, chargeVelocity.value / 0.8))
      const now = performance.now()
      // 尾焰随速度伸长；淡出时明显回缩，避免整条尾焰“定格”后消失
      const tailLen = Math.min(filledW, 30 + speedFactor * 24) * (0.3 + cometOpacity * 0.7)
      const headRadius = trackH * 0.62 * (0.95 + speedFactor * 0.12)
      const wobble = Math.sin(now * 0.011) * trackH * 0.05 + Math.sin(now * 0.023) * trackH * 0.03

      const mainRgb = stage === 1
        ? hexToRgb(getThemeColorHex())
        : stage === 2 ? '192, 132, 252' : stage === 3 ? '248, 113, 113' : '253, 224, 71'
      const glowRgb = stage === 1
        ? mainRgb
        : stage === 2 ? '147, 51, 234' : stage === 3 ? '220, 38, 38' : '245, 158, 11'

      ctx.save()
      ctx.globalCompositeOperation = 'lighter'
      ctx.globalAlpha = cometOpacity

      // 1) 头部外泛光：亮核与背景之间加一层空气散射，不再是硬边白球
      const bloomR = headRadius * (2.6 + speedFactor * 0.5)
      const bloom = ctx.createRadialGradient(headX, headY, 0, headX, headY, bloomR)
      bloom.addColorStop(0, `rgba(${glowRgb}, 0.5)`)
      bloom.addColorStop(0.45, `rgba(${glowRgb}, 0.16)`)
      bloom.addColorStop(1, `rgba(${glowRgb}, 0)`)
      ctx.fillStyle = bloom
      ctx.beginPath()
      ctx.arc(headX, headY, bloomR, 0, Math.PI * 2)
      ctx.fill()

      // 2) 尾焰：尾部完全透明，向头部过渡到白热，上下边缘带轻微波动
      const tailGrad = ctx.createLinearGradient(headX - tailLen, headY, headX, headY)
      tailGrad.addColorStop(0, `rgba(${glowRgb}, 0)`)
      tailGrad.addColorStop(0.42, `rgba(${glowRgb}, 0.3)`)
      tailGrad.addColorStop(0.8, `rgba(${mainRgb}, 0.7)`)
      tailGrad.addColorStop(0.94, `rgba(${mainRgb}, 0.95)`)
      tailGrad.addColorStop(1, 'rgba(255, 255, 255, 0.98)')

      ctx.fillStyle = tailGrad
      ctx.beginPath()
      ctx.moveTo(headX - tailLen, headY)
      ctx.bezierCurveTo(
        headX - tailLen * 0.7, headY - headRadius * 0.4 + wobble,
        headX - tailLen * 0.3, headY - headRadius,
        headX, headY - headRadius
      )
      ctx.arc(headX, headY, headRadius, -Math.PI / 2, Math.PI / 2)
      ctx.bezierCurveTo(
        headX - tailLen * 0.3, headY + headRadius,
        headX - tailLen * 0.7, headY + headRadius * 0.4 + wobble,
        headX - tailLen, headY
      )
      ctx.closePath()
      ctx.fill()

      // 3) 白热核心：比原版更小，外部由泛光过渡，避免“大白色圆球”
      const orbRadius = headRadius * 1.12
      const orbGrad = ctx.createRadialGradient(headX, headY, 0, headX, headY, orbRadius)
      orbGrad.addColorStop(0, 'rgba(255, 255, 255, 1)')
      orbGrad.addColorStop(0.34, 'rgba(255, 255, 255, 0.82)')
      orbGrad.addColorStop(0.68, `rgba(${mainRgb}, 0.4)`)
      orbGrad.addColorStop(1, `rgba(${mainRgb}, 0)`)
      ctx.fillStyle = orbGrad
      ctx.beginPath()
      ctx.arc(headX, headY, orbRadius, 0, Math.PI * 2)
      ctx.fill()
      ctx.restore()

      // 4) 尾焰向后流散的星尘：带阻尼地减速、缓慢冷却，不再糊成一条白色硬线
      if (isCharging.value && !chargePaused.value) {
        const spawn = speedFactor > 1.2 ? 3 : 2
        for (let k = 0; k < spawn; k++) {
          tailParticles.push({
            x: headX - Math.random() * 3,
            y: headY + (Math.random() - 0.5) * trackH * 0.72,
            vx: -(0.5 + Math.random() * 1.5) * (0.55 + speedFactor * 0.5),
            vy: (Math.random() - 0.5) * 0.3,
            r: 0.55 + Math.random() * 0.95,
            life: 0,
            maxLife: 18 + Math.random() * 20,
            stage
          })
        }
      }

      ctx.save()
      ctx.globalCompositeOperation = 'lighter'
      // 同一帧内复用主题色贴图，避免每颗粒子都去读一次 CSS 变量
      let wakeThemeSprite = null
      for (let i = tailParticles.length - 1; i >= 0; i--) {
        const tp = tailParticles[i]
        tp.life++
        tp.vx *= 0.965
        tp.x += tp.vx
        tp.y += tp.vy + Math.sin((tp.life + tp.x) * 0.2) * 0.03
        if (tp.life >= tp.maxLife || tp.x < PAD_X - 4) {
          tailParticles.splice(i, 1)
          continue
        }
        const p = tp.life / tp.maxLife
        const a = (1 - p) * 0.9 * cometOpacity
        const halo = tp.r * 5
        const sprite = tp.stage === 1
          ? (wakeThemeSprite || (wakeThemeSprite = getThemeMoteSprite()))
          : getMoteSprite(tp.stage)
        ctx.globalAlpha = a
        ctx.drawImage(sprite, tp.x - halo, tp.y - halo, halo * 2, halo * 2)
        // 刚喷出时保留一点白热芯，随后冷却成阶段色
        if (p < 0.5) {
          ctx.globalAlpha = a * (1 - p / 0.5) * 0.9
          ctx.fillStyle = '#ffffff'
          ctx.beginPath()
          ctx.arc(tp.x, tp.y, Math.max(0.3, tp.r * 0.5), 0, Math.PI * 2)
          ctx.fill()
        }
      }
      ctx.restore()

      // 5) 头部电弧：每一束都有独立寿命，锚定在喷出位置并淡入淡出，避免逐帧随机闪烁
      if (isCharging.value && !chargePaused.value && stage >= 2) {
        const arcCfg = stage === 2
          ? { chance: 0.34, max: 6, reach: 9, displace: 4, branch: 0 }
          : stage === 3
            ? { chance: 0.55, max: 10, reach: 14, displace: 6, branch: 0.35 }
            : { chance: 0.85, max: 14, reach: 20, displace: 8, branch: 0.55 }
        if (cometArcs.length < arcCfg.max && Math.random() < arcCfg.chance) {
          const angle = (Math.random() * 2 - 1) * Math.PI
          const dist = 4 + Math.random() * arcCfg.reach
          // 存相对坐标：电弧始终跟随彗星头部，而不是被高速推进甩在原地
          const dx = Math.cos(angle) * dist + (speedFactor - 1) * 3
          const dy = Math.sin(angle) * dist * 0.62
          cometArcs.push({
            ...createBolt(
              (Math.random() - 0.5) * 4,
              (Math.random() - 0.5) * 2,
              dx, dy, arcCfg.displace, 2, arcCfg.branch
            ),
            life: 0,
            maxLife: 5 + Math.floor(Math.random() * 5),
            width: stage === 2 ? 0.8 : stage === 3 ? 1.0 : 1.2,
            phase: Math.random() * Math.PI * 2,
            stage
          })
        }
      }

      for (let i = cometArcs.length - 1; i >= 0; i--) {
        const arc = cometArcs[i]
        arc.life++
        if (arc.life >= arc.maxLife) {
          cometArcs.splice(i, 1)
          continue
        }
        const alpha = Math.sin((arc.life / arc.maxLife) * Math.PI) *
          (0.7 + 0.3 * Math.sin(now * 0.06 + arc.phase)) * cometOpacity
        ctx.save()
        ctx.translate(headX, headY)
        drawBolt(ctx, arc, BOLT_PALETTE[arc.stage], arc.width, alpha)
        ctx.restore()
      }
    } else {
      tailParticles.length = 0
      cometArcs.length = 0
    }

    ctx.restore()
  }
  fxRafId = requestAnimationFrame(renderFxLoop)
}

onMounted(() => {
  setupCanvas()
  if (typeof ResizeObserver !== 'undefined' && trackRef.value) {
    resizeObserver = new ResizeObserver(() => setupCanvas())
    resizeObserver.observe(trackRef.value)
  }
  fxRafId = requestAnimationFrame(renderFxLoop)
})

onBeforeUnmount(() => {
  if (chargeRafId) cancelAnimationFrame(chargeRafId)
  if (fxRafId) cancelAnimationFrame(fxRafId)
  if (resizeObserver) resizeObserver.disconnect()
})
</script>

<style scoped>
.boss-progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 12px 0 8px;
}

.boss-progress-row.is-large {
  gap: 12px;
  margin: 8px 0 4px;
}

.boss-bar-track {
  position: relative;
  flex: 1;
  height: 10px;
  border-radius: 999px;
  background-color: rgba(148, 163, 184, 0.22);
  overflow: visible;
}

.boss-progress-row.is-large .boss-bar-track {
  height: 12px;
}

html.dark .boss-bar-track {
  background-color: rgba(255, 255, 255, 0.1);
}

.boss-bar-layer {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 999px;
  pointer-events: none;
}

/*
 * 各层渐变一律以本阶段的「饱和主色」收尾，不再使用 #fff / #f3e8ff / #fee2e2 之类的近白停靠点。
 * 原因：渐变会随层宽一起拉伸，近白端点 = 该层满格后右端永久留一块白斑。
 * 推进中的高亮头部交给 Canvas 彗星核心球（仅在 isCharging 时存在），充能结束后自然消失，
 * 满格时血条末端就是干净的阶段色。
 */

/* 第 1 层：系统主题色 (0% ~ 100%) */
.layer-theme {
  background: linear-gradient(
    90deg,
    var(--el-color-primary-dark-2, var(--el-color-primary)) 0%,
    var(--el-color-primary) 58%,
    var(--el-color-primary) 100%
  );
  box-shadow: 0 0 8px rgba(var(--theme-rgb, 22, 119, 255), 0.4);
  z-index: 1;
}

/* 第 2 层：星云紫 (100% ~ 200%, 1X) */
.layer-purple {
  background: linear-gradient(90deg, #6b21a8 0%, #9333ea 55%, #c084fc 100%);
  box-shadow: 0 0 12px rgba(168, 85, 247, 0.6);
  z-index: 2;
}

/* 第 3 层：烈焰红 (200% ~ 300%, 2X) */
.layer-red {
  background: linear-gradient(90deg, #991b1b 0%, #dc2626 55%, #f87171 100%);
  box-shadow: 0 0 14px rgba(239, 68, 68, 0.72);
  z-index: 3;
}

/* 第 4 层及以上：流光金 (300%+, 3X / 4X / NX) */
.layer-gold-base {
  background: linear-gradient(90deg, #b45309 0%, #f59e0b 50%, #fde047 100%);
  box-shadow: 0 0 16px rgba(245, 158, 11, 0.85);
  z-index: 4;
}

.layer-gold-top {
  background: linear-gradient(90deg, #d97706 0%, #fbbf24 45%, #fef08a 100%);
  box-shadow: 0 0 20px rgba(250, 204, 21, 0.95), 0 0 10px rgba(255, 236, 150, 0.6);
  z-index: 5;
}

.boss-fx-canvas {
  position: absolute;
  pointer-events: none;
  z-index: 10;
}

.boss-bar-label {
  font-size: 13px;
  font-weight: 700;
  min-width: 64px;
  text-align: right;
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 5px;
  white-space: nowrap;
}

.boss-progress-row.is-large .boss-bar-label {
  font-size: 14px;
  min-width: 70px;
}

.stage-tag {
  font-size: 10px;
  font-weight: 800;
  padding: 1px 5px;
  border-radius: 4px;
  color: #fff;
  line-height: 1.25;
  letter-spacing: 0.3px;
}

.label-stage-1 {
  color: var(--el-color-primary);
}

.label-stage-2 {
  color: #a855f7;
}
.label-stage-2 .stage-tag {
  background: linear-gradient(90deg, #7e22ce, #a855f7);
  box-shadow: 0 0 6px rgba(168, 85, 247, 0.45);
}

.label-stage-3 {
  color: #ef4444;
}
.label-stage-3 .stage-tag {
  background: linear-gradient(90deg, #b91c1c, #ef4444);
  box-shadow: 0 0 6px rgba(239, 68, 68, 0.45);
}

.label-stage-4 {
  color: #f59e0b;
  text-shadow: 0 0 6px rgba(245, 158, 11, 0.35);
}
.label-stage-4 .stage-tag {
  background: linear-gradient(90deg, #d97706, #f59e0b);
  box-shadow: 0 0 8px rgba(245, 158, 11, 0.55);
}
</style>
