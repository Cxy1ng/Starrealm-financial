<template>
  <div class="boss-progress-row" :class="{ 'is-large': large }">
    <div
      class="boss-bar-track"
      :class="[
        'stage-glow-' + activeStage,
        { 'is-500-max': displayPct >= 500 }
      ]"
      ref="trackRef"
    >
      <!-- 内部圆角裁剪容器：确保填充图层与柔光掠影严密贴合圆角，无任何多余间隙 -->
      <div class="boss-bar-clip">
        <!-- 第 1 层：系统主题蓝 (0% ~ 100%) -->
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

        <!-- 第 4 层：黄金流火 (300% ~ 400%, 3X) -->
        <div
          v-if="displayPct > 300"
          class="boss-bar-layer layer-gold-base"
          :style="{ width: stage4Width + '%' }"
        ></div>

        <!-- 第 5 层及以上：荒野乱斗究极彩晶流光 (400%+, 4X ~ 5X+) -->
        <div
          v-if="displayPct > 400"
          class="boss-bar-layer layer-chroma-base"
          :style="{ width: stage5Width + '%' }"
        ></div>
      </div>

      <!-- Canvas 真实物理粒子、双层共生电弧、正圆形突破爆发与白蓝彗星 -->
      <canvas ref="canvasRef" class="boss-fx-canvas"></canvas>
    </div>

    <!-- 右侧倍率徽章与百分比读数：5X 往后均延续顺时针流光徽章 -->
    <span class="boss-bar-label num-font" :class="'label-stage-' + activeStage">
      <span
        v-if="bonusMultiplier >= 1"
        class="stage-tag"
        :class="{ 'is-5x': bonusMultiplier >= 5 }"
      >{{ bonusMultiplier }}X</span>
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
const chargePaused = ref(false) // 跨 100% 边界时的蓄力停顿
const chargeVelocity = ref(0) // 当前推进瞬时速度，用于驱动彗星尾焰长度与亮度

// 当前视觉阶级：1=主题色(0~100%), 2=紫色(100~200%), 3=红色(200~300%), 4=金色(300~400%), 5=彩晶(400%+)
const activeStage = computed(() => {
  const p = displayPct.value
  if (p > 400) return 5
  if (p > 300) return 4
  if (p > 200) return 3
  if (p > 100) return 2
  return 1
})

// 倍率徽章：严格与整百进度对齐，500% 刚好蜕变为 5X，5X 往后延续 5X 顺时针流光样式
const bonusMultiplier = computed(() => {
  const p = Math.round(displayPct.value)
  if (p < 100) return 0
  return Math.floor(p / 100)
})

// 各层严格顺序宽度计算 (每阶 100%)
const stage1Width = computed(() => Math.min(Math.max(displayPct.value, 0), 100))
const stage2Width = computed(() => Math.min(Math.max(displayPct.value - 100, 0), 100))
const stage3Width = computed(() => Math.min(Math.max(displayPct.value - 200, 0), 100))
const stage4Width = computed(() => Math.min(Math.max(displayPct.value - 300, 0), 100))
const stage5Width = computed(() => Math.min(Math.max(displayPct.value - 400, 0), 100))

// 当前活跃层的已填充比例 (0 ~ 100)，即已充能区域右边界以及推进彗星头部所在百分比
const activeFillRatio = computed(() => {
  const p = displayPct.value
  if (p <= 0) return 0
  if (p >= 500) return 100
  const rem = p % 100
  return rem === 0 ? 100 : rem
})

// 跨阶突破正圆形爆发队列 (一个条完成时的特效)
const shockwaves = []
const triggerBreakthroughBurst = (boundaryPct) => {
  const stage = Math.min(5, Math.max(1, Math.floor(boundaryPct / 100)))
  shockwaves.push({
    life: 0,
    maxLife: 36,
    stage,
    sparks: Array.from({ length: 16 }, (_, idx) => {
      const angle = (Math.PI * 2 * idx) / 16 + (Math.random() - 0.5) * 0.2
      const speed = 1.5 + Math.random() * 2.4
      return {
        x: 0,
        y: 0,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        r: 1.0 + Math.random() * 1.2
      }
    })
  })
}

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
  let vel = 0.12

  const step = () => {
    try {
      if (pauseFrames > 0) {
        pauseFrames--
        chargeVelocity.value = 0.14
        if (pauseFrames === 0) chargePaused.value = false
        chargeRafId = requestAnimationFrame(step)
        return
      }

      const currentFloor = Math.floor(current / 100) * 100
      const nextBoundary = currentFloor + 100
      const segmentTarget = Math.min(to, nextBoundary)
      const distInSegment = Math.max(0, current - segmentStart)

      // 渐进加速曲线
      const accel = 0.16 + Math.pow(distInSegment / 60, 1.5) * 1.45
      vel = Math.min(1.8, accel)
      chargeVelocity.value = vel

      current += vel

      if (current >= segmentTarget) {
        current = segmentTarget
        displayPct.value = current

        // 达成一个条完成的整百边界时，触发正圆形突破爆发光环
        if (current % 100 === 0 && current > 0) {
          triggerBreakthroughBurst(current)
        }

        if (current < to) {
          pauseFrames = 14
          chargePaused.value = true
          segmentStart = current
          vel = 0.1
          chargeRafId = requestAnimationFrame(step)
        } else {
          displayPct.value = to
          chargePaused.value = false
          isCharging.value = false
          chargeVelocity.value = 0
        }
      } else {
        displayPct.value = current
        chargeRafId = requestAnimationFrame(step)
      }
    } catch (err) {
      console.error('BossProgressBar charge step error:', err)
      displayPct.value = to
      chargePaused.value = false
      isCharging.value = false
      chargeVelocity.value = 0
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

// ==================== 2. Canvas 物理微粒 / 仿真分形电弧 / 白蓝彗星引擎 ====================
let fxRafId = null
let resizeObserver = null
const ambientParticles = []
const ambientBolts = []
const tailParticles = []
const cometArcs = []
let cometOpacity = 0
let moteSpawnAcc = 0
let moteSpawnAccPrev = 0

const PAD_X = 36
const PAD_Y = 22

// 电流调色盘（Stage 5: 纯白高亮芯 + 冰天蓝等离子外辉光）
const BOLT_PALETTE = {
  1: { core: '240, 249, 255', glow: '56, 189, 248' },
  2: { core: '245, 235, 255', glow: '168, 85, 247' },
  3: { core: '255, 232, 232', glow: '239, 68, 68' },
  4: { core: '255, 255, 255', glow: '250, 204, 21' },
  5: { core: '255, 255, 255', glow: '56, 189, 248' }
}

// 浮尘光晕调色盘（Stage 5: 白热晶核 + 冰电弧淡蓝外晕）
const MOTE_PALETTE = {
  1: { glow: '56, 189, 248', core: '240, 249, 255' },
  2: { glow: '192, 132, 252', core: '246, 239, 255' },
  3: { glow: '248, 113, 113', core: '255, 242, 242' },
  4: { glow: '253, 224, 71', core: '255, 253, 240' },
  5: { glow: '125, 211, 252', core: '255, 255, 255' }
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
  grad.addColorStop(0.18, `rgba(${coreRgb}, 0.92)`)
  grad.addColorStop(0.4, `rgba(${glowRgb}, 0.45)`)
  grad.addColorStop(0.7, `rgba(${glowRgb}, 0.12)`)
  grad.addColorStop(1, `rgba(${glowRgb}, 0)`)
  g.fillStyle = grad
  g.fillRect(0, 0, size, size)
  return cvs
}

const getMoteSprite = (stage) => {
  if (moteSpriteCache[stage]) return moteSpriteCache[stage]
  const pal = MOTE_PALETTE[stage] || MOTE_PALETTE[5]
  const sprite = buildMoteSprite(pal.glow, pal.core)
  moteSpriteCache[stage] = sprite
  return sprite
}

// 分形电弧生成
const createFractalBoltPoints = (x1, y1, x2, y2, displace, generations = 3) => {
  let pts = [{ x: x1, y: y1 }, { x: x2, y: y2 }]
  for (let g = 0; g < generations; g++) {
    const next = []
    for (let i = 0; i < pts.length - 1; i++) {
      const p1 = pts[i]
      const p2 = pts[i + 1]
      const midX = (p1.x + p2.x) * 0.5 + (Math.random() - 0.5) * displace * 0.42
      const midY = (p1.y + p2.y) * 0.5 + (Math.random() - 0.5) * displace
      next.push(p1, { x: midX, y: midY })
    }
    next.push(pts[pts.length - 1])
    pts = next
    displace *= 0.54
  }
  return pts
}

const createBolt = (x1, y1, x2, y2, displace, generations = 2, branchChance = 0) => {
  const main = createFractalBoltPoints(x1, y1, x2, y2, displace, generations)
  const branches = []
  if (branchChance > 0 && main.length > 5 && Math.random() < branchChance) {
    const from = main[2 + Math.floor(Math.random() * (main.length - 4))]
    const baseAngle = Math.atan2(y2 - y1, x2 - x1)
    const angle = baseAngle + (Math.random() - 0.5) * 1.8
    const len = Math.min(18, Math.max(6, Math.hypot(x2 - x1, y2 - y1) * (0.26 + Math.random() * 0.32)))
    branches.push(
      createFractalBoltPoints(
        from.x, from.y,
        from.x + Math.cos(angle) * len,
        from.y + Math.sin(angle) * len,
        displace * 0.52,
        Math.max(1, generations - 1)
      )
    )
  }
  return { main, branches }
}

const drawBolt = (ctx, bolt, palette, width, alpha) => {
  if (alpha <= 0.01) return
  ctx.save()
  ctx.globalCompositeOperation = 'lighter'
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'

  ctx.beginPath()
  bolt.main.forEach((pt, i) => (i ? ctx.lineTo(pt.x, pt.y) : ctx.moveTo(pt.x, pt.y)))
  bolt.branches.forEach((bp) => {
    bp.forEach((pt, i) => (i ? ctx.lineTo(pt.x, pt.y) : ctx.moveTo(pt.x, pt.y)))
  })
  ctx.strokeStyle = `rgba(${palette.glow}, ${alpha * 0.52})`
  ctx.lineWidth = width * 3.5
  ctx.shadowColor = `rgba(${palette.glow}, ${alpha * 0.9})`
  ctx.shadowBlur = 10
  ctx.stroke()

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
  taper(bolt.main, width, 0.78)
  bolt.branches.forEach((bp) => taper(bp, width * 0.55, 0.84))

  ctx.restore()
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
  try {
    const track = trackRef.value
    const cvs = canvasRef.value
    if (track && cvs) {
      const ctx = cvs.getContext('2d')
      if (!ctx) return
    const dpr = Math.min(window.devicePixelRatio || 1, 2)
    const trackW = cvs.width / dpr - PAD_X * 2
    const trackH = cvs.height / dpr - PAD_Y * 2
    const nowMs = performance.now()

    ctx.save()
    ctx.scale(dpr, dpr)
    ctx.clearRect(0, 0, trackW + PAD_X * 2, trackH + PAD_Y * 2)

    const stage = activeStage.value
    const fillRatio = activeFillRatio.value / 100
    const filledW = Math.max(0, Math.min(1, fillRatio)) * trackW
    const exposedW = Math.max(0, trackW - filledW)
    const prevStage = stage - 1
    const barCenterY = PAD_Y + trackH * 0.5

    // ---------- A. 双层共生粒子与电弧力场 (当前层 + 未满时露出的上一层对应区域) ----------
    const MOTE_CONFIG = {
      1: { min: 6, max: 18, density: 0.05, spawn: 0.25, rMin: 0.45, rMax: 0.9, lifeMin: 60, lifeMax: 120 },
      2: { min: 8, max: 22, density: 0.07, spawn: 0.35, rMin: 0.5, rMax: 1.0, lifeMin: 55, lifeMax: 110 },
      3: { min: 12, max: 34, density: 0.11, spawn: 0.65, rMin: 0.4, rMax: 0.85, lifeMin: 48, lifeMax: 95 },
      4: { min: 16, max: 50, density: 0.16, spawn: 1.0, rMin: 0.35, rMax: 0.75, lifeMin: 42, lifeMax: 88 },
      5: { min: 20, max: 64, density: 0.22, spawn: 1.25, rMin: 0.32, rMax: 0.8, lifeMin: 48, lifeMax: 96 }
    }

    // 1) 当前层粒子生成 (范围: [PAD_X, PAD_X + filledW])
    if (filledW > 6) {
      const cfg = MOTE_CONFIG[stage] || MOTE_CONFIG[1]
      const maxMotes = Math.max(cfg.min, Math.min(cfg.max, Math.round(filledW * cfg.density)))
      const currMotes = ambientParticles.filter((m) => m.stage === stage).length
      moteSpawnAcc += cfg.spawn
      while (moteSpawnAcc >= 1) {
        moteSpawnAcc -= 1
        if (currMotes >= maxMotes) break
        const r = cfg.rMin + Math.pow(Math.random(), 1.45) * (cfg.rMax - cfg.rMin)
        ambientParticles.push({
          x: PAD_X + Math.random() * filledW,
          y: barCenterY + (Math.random() - 0.5) * (trackH + 12),
          vx: 0.08 + Math.random() * 0.18,
          vy: (Math.random() - 0.5) * 0.22,
          r,
          phase: Math.random() * Math.PI * 2,
          life: 0,
          maxLife: cfg.lifeMin + Math.random() * (cfg.lifeMax - cfg.lifeMin),
          stage
        })
      }
    }

    // 2) 上一层露出来区域粒子生成 (范围: [PAD_X + filledW, PAD_X + trackW])
    if (stage >= 2 && prevStage >= 1 && exposedW > 8) {
      const cfgPrev = MOTE_CONFIG[prevStage]
      const maxMotesPrev = Math.max(4, Math.min(cfgPrev.max, Math.round(exposedW * cfgPrev.density)))
      const prevMotes = ambientParticles.filter((m) => m.stage === prevStage).length
      moteSpawnAccPrev += cfgPrev.spawn * 0.9
      while (moteSpawnAccPrev >= 1) {
        moteSpawnAccPrev -= 1
        if (prevMotes >= maxMotesPrev) break
        const r = cfgPrev.rMin + Math.pow(Math.random(), 1.45) * (cfgPrev.rMax - cfgPrev.rMin)
        ambientParticles.push({
          x: PAD_X + filledW + Math.random() * exposedW,
          y: barCenterY + (Math.random() - 0.5) * (trackH + 12),
          vx: 0.06 + Math.random() * 0.16,
          vy: (Math.random() - 0.5) * 0.2,
          r,
          phase: Math.random() * Math.PI * 2,
          life: 0,
          maxLife: cfgPrev.lifeMin + Math.random() * (cfgPrev.lifeMax - cfgPrev.lifeMin),
          stage: prevStage
        })
      }
    }

    // 3) 渲染与边界过滤所有浮尘微粒
    ctx.save()
    ctx.globalCompositeOperation = 'lighter'
    for (let i = ambientParticles.length - 1; i >= 0; i--) {
      const m = ambientParticles[i]
      m.life++
      m.x += m.vx
      m.y += m.vy + Math.sin((m.life + m.phase * 10) * 0.04) * 0.06

      // 当前层粒子：不能越出充能前沿
      if (m.stage === stage && m.x > PAD_X + filledW + 6) {
        ambientParticles.splice(i, 1)
        continue
      }
      // 上一层露出区域的粒子：若被推进的当前层覆盖吞没，或者漂移出轨道右边界
      if (m.stage === prevStage && (m.x < PAD_X + filledW - 4 || m.x > PAD_X + trackW + 6)) {
        ambientParticles.splice(i, 1)
        continue
      }
      // 其余非当前或上一层的历史粒子自然清除
      if (m.stage !== stage && m.stage !== prevStage) {
        ambientParticles.splice(i, 1)
        continue
      }
      if (m.life >= m.maxLife) {
        ambientParticles.splice(i, 1)
        continue
      }

      const progress = m.life / m.maxLife
      const alpha = Math.sin(progress * Math.PI) *
        (0.75 + 0.25 * Math.sin((m.life + m.phase * 12) * 0.16)) * 0.9
      if (alpha <= 0.01) continue

      const halo = m.r * 6.8
      ctx.globalAlpha = alpha
      ctx.drawImage(getMoteSprite(m.stage), m.x - halo, m.y - halo, halo * 2, halo * 2)
      ctx.globalAlpha = Math.min(1, alpha * 0.95)
      ctx.fillStyle = '#ffffff'
      ctx.beginPath()
      ctx.arc(m.x, m.y, Math.max(0.3, m.r * 0.62), 0, Math.PI * 2)
      ctx.fill()
    }
    ctx.restore()

    // 4) 双层电弧生成与渲染 (当前层 + 露出的上一层)
    // 4.1 当前层电弧
    if (stage >= 2 && filledW > 16) {
      const boltCfgCurr = stage === 2
        ? { chance: 0.12, max: 2, width: 0.75, branch: 0 }
        : stage === 3
          ? { chance: 0.22, max: 3, width: 0.95, branch: 0.3 }
          : stage === 4
            ? { chance: 0.35, max: 4, width: 1.15, branch: 0.5 }
            : { chance: 0.45, max: 5, width: 1.25, branch: 0.65 }

      const currBolts = ambientBolts.filter((b) => b.stage === stage).length
      if (currBolts < boltCfgCurr.max && Math.random() < boltCfgCurr.chance) {
        const spanMin = stage === 2 ? 14 : stage === 3 ? 20 : stage === 4 ? 28 : 34
        const spanMax = stage === 2 ? 28 : stage === 3 ? 45 : stage === 4 ? 76 : 92
        const segLen = Math.min(filledW, spanMin + Math.random() * (spanMax - spanMin))
        const startX = PAD_X + Math.random() * Math.max(1, filledW - segLen)
        const endX = startX + segLen
        const yOffset1 = (Math.random() - 0.5) * (trackH + 6)
        const yOffset2 = (Math.random() - 0.5) * (trackH + 6)
        const displace = stage === 2 ? 4 : stage === 3 ? 6.5 : stage === 4 ? 9 : 10.5

        ambientBolts.push({
          ...createBolt(
            startX, barCenterY + yOffset1,
            endX, barCenterY + yOffset2,
            displace, 3, boltCfgCurr.branch
          ),
          life: 0,
          maxLife: 8 + Math.floor(Math.random() * 7),
          width: boltCfgCurr.width * (0.85 + Math.random() * 0.25),
          phase: Math.random() * Math.PI * 2,
          stage
        })
      }
    }

    // 4.2 上一层露出来区域的专属电弧
    if (stage >= 2 && prevStage >= 2 && exposedW > 24) {
      const boltCfgPrev = prevStage === 2
        ? { chance: 0.12, max: 2, width: 0.75, branch: 0 }
        : prevStage === 3
          ? { chance: 0.20, max: 3, width: 0.95, branch: 0.3 }
          : { chance: 0.30, max: 3, width: 1.15, branch: 0.5 }

      const prevBolts = ambientBolts.filter((b) => b.stage === prevStage).length
      if (prevBolts < boltCfgPrev.max && Math.random() < boltCfgPrev.chance) {
        const spanMin = prevStage === 2 ? 14 : prevStage === 3 ? 20 : 26
        const spanMax = prevStage === 2 ? 28 : prevStage === 3 ? 42 : 68
        const segLen = Math.min(exposedW, spanMin + Math.random() * (spanMax - spanMin))
        const startX = PAD_X + filledW + Math.random() * Math.max(1, exposedW - segLen)
        const endX = startX + segLen
        const yOffset1 = (Math.random() - 0.5) * (trackH + 6)
        const yOffset2 = (Math.random() - 0.5) * (trackH + 6)
        const displace = prevStage === 2 ? 4 : prevStage === 3 ? 6.5 : 8.5

        ambientBolts.push({
          ...createBolt(
            startX, barCenterY + yOffset1,
            endX, barCenterY + yOffset2,
            displace, 3, boltCfgPrev.branch
          ),
          life: 0,
          maxLife: 8 + Math.floor(Math.random() * 7),
          width: boltCfgPrev.width * (0.85 + Math.random() * 0.25),
          phase: Math.random() * Math.PI * 2,
          stage: prevStage
        })
      }
    }

    for (let i = ambientBolts.length - 1; i >= 0; i--) {
      const b = ambientBolts[i]
      b.life++
      if (b.stage === prevStage && b.main[0].x < PAD_X + filledW - 4) {
        ambientBolts.splice(i, 1)
        continue
      }
      if (b.life >= b.maxLife) {
        ambientBolts.splice(i, 1)
        continue
      }
      const progress = b.life / b.maxLife
      const alpha = Math.sin(progress * Math.PI) *
        (0.8 + 0.2 * Math.sin(b.life * 2.0 + b.phase))
      drawBolt(ctx, b, BOLT_PALETTE[b.stage], b.width, alpha)
    }

    // ---------- B. 跨阶突破【正圆形】扩散光环与正圆形粒子爆发 (一个条完成时的特效) ----------
    if (shockwaves.length > 0) {
      const burstX = PAD_X + trackW
      const burstY = barCenterY
      ctx.save()
      ctx.globalCompositeOperation = 'lighter'
      for (let i = shockwaves.length - 1; i >= 0; i--) {
        const sw = shockwaves[i]
        sw.life++
        if (sw.life >= sw.maxLife) {
          shockwaves.splice(i, 1)
          continue
        }
        const p = sw.life / sw.maxLife
        const easeOut = 1 - Math.pow(1 - p, 3)
        const alpha = (1 - p) * 0.9
        const pal = BOLT_PALETTE[sw.stage] || BOLT_PALETTE[5]
        const radius = 5 + easeOut * 28

        // 外光环
        ctx.shadowColor = `rgba(${pal.glow}, ${alpha})`
        ctx.shadowBlur = 10
        ctx.strokeStyle = `rgba(${pal.glow}, ${alpha * 0.9})`
        ctx.lineWidth = 2.8 * (1 - p)
        ctx.beginPath()
        ctx.arc(burstX, burstY, radius, 0, Math.PI * 2)
        ctx.stroke()

        // 内高亮环
        ctx.shadowBlur = 0
        ctx.strokeStyle = `rgba(${pal.core}, ${alpha * 0.85})`
        ctx.lineWidth = 1.3 * (1 - p)
        ctx.beginPath()
        ctx.arc(burstX, burstY, radius * 0.72, 0, Math.PI * 2)
        ctx.stroke()

        // 16 颗发散火花微粒
        sw.sparks.forEach((sp) => {
          sp.x += sp.vx
          sp.y += sp.vy
          sp.vx *= 0.93
          sp.vy *= 0.93
          const px = burstX + sp.x
          const py = burstY + sp.y
          const halo = sp.r * 4.5
          ctx.globalAlpha = alpha
          ctx.drawImage(getMoteSprite(sw.stage), px - halo, py - halo, halo * 2, halo * 2)
          ctx.fillStyle = '#ffffff'
          ctx.beginPath()
          ctx.arc(px, py, sp.r * (1 - p * 0.45), 0, Math.PI * 2)
          ctx.fill()
        })
      }
      ctx.restore()
    }

    // ---------- C. 充能推进特效：彗星尾焰 + 头部奔涌电弧 ----------
    const cometTarget = isCharging.value && !chargePaused.value ? 1 : 0
    const cometFadeRate = cometTarget > cometOpacity ? 0.2 : (chargePaused.value ? 0.24 : 0.11)
    cometOpacity += (cometTarget - cometOpacity) * cometFadeRate
    if (Math.abs(cometOpacity - cometTarget) < 0.004) cometOpacity = cometTarget

    if (cometOpacity > 0.01 && filledW > 1) {
      const headX = PAD_X + filledW
      const headY = barCenterY
      const speedFactor = Math.min(1.8, Math.max(0.45, chargeVelocity.value / 0.8))
      const tailLen = Math.min(filledW, 36 + speedFactor * 26) * (0.3 + cometOpacity * 0.7)
      const headRadius = trackH * 0.64 * (0.95 + speedFactor * 0.14)
      const wobble = Math.sin(nowMs * 0.012) * trackH * 0.05 + Math.sin(nowMs * 0.024) * trackH * 0.03

      const mainRgb = stage === 5
        ? '224, 242, 254'
        : stage === 4 ? '253, 224, 71' : stage === 3 ? '248, 113, 113' : stage === 2 ? '192, 132, 252' : '100, 180, 255'
      const glowRgb = stage === 5
        ? '56, 189, 248'
        : stage === 4 ? '245, 158, 11' : stage === 3 ? '220, 38, 38' : stage === 2 ? '147, 51, 234' : '37, 99, 235'

      ctx.save()
      ctx.globalCompositeOperation = 'lighter'
      ctx.globalAlpha = cometOpacity

      const bloomR = headRadius * (2.8 + speedFactor * 0.6)
      const bloom = ctx.createRadialGradient(headX, headY, 0, headX, headY, bloomR)
      bloom.addColorStop(0, `rgba(${glowRgb}, 0.65)`)
      bloom.addColorStop(0.48, `rgba(${glowRgb}, 0.22)`)
      bloom.addColorStop(1, `rgba(${glowRgb}, 0)`)
      ctx.fillStyle = bloom
      ctx.beginPath()
      ctx.arc(headX, headY, bloomR, 0, Math.PI * 2)
      ctx.fill()

      const tailGrad = ctx.createLinearGradient(headX - tailLen, headY, headX, headY)
      tailGrad.addColorStop(0, `rgba(${glowRgb}, 0)`)
      tailGrad.addColorStop(0.38, `rgba(${glowRgb}, 0.35)`)
      tailGrad.addColorStop(0.78, `rgba(${mainRgb}, 0.75)`)
      tailGrad.addColorStop(0.92, `rgba(${mainRgb}, 0.95)`)
      tailGrad.addColorStop(1, 'rgba(255, 255, 255, 1)')

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

      const orbRadius = headRadius * 1.15
      const orbGrad = ctx.createRadialGradient(headX, headY, 0, headX, headY, orbRadius)
      orbGrad.addColorStop(0, 'rgba(255, 255, 255, 1)')
      orbGrad.addColorStop(0.4, 'rgba(255, 255, 255, 0.9)')
      orbGrad.addColorStop(0.75, `rgba(${glowRgb}, 0.5)`)
      orbGrad.addColorStop(1, `rgba(${glowRgb}, 0)`)
      ctx.fillStyle = orbGrad
      ctx.beginPath()
      ctx.arc(headX, headY, orbRadius, 0, Math.PI * 2)
      ctx.fill()
      ctx.restore()

      if (isCharging.value && !chargePaused.value) {
        const spawn = speedFactor > 1.2 ? 4 : 2
        for (let k = 0; k < spawn; k++) {
          tailParticles.push({
            x: headX - Math.random() * 4,
            y: headY + (Math.random() - 0.5) * trackH * 0.75,
            vx: -(0.6 + Math.random() * 1.8) * (0.6 + speedFactor * 0.5),
            vy: (Math.random() - 0.5) * 0.35,
            r: 0.6 + Math.random() * 1.1,
            life: 0,
            maxLife: 20 + Math.random() * 22,
            stage
          })
        }
      }

      ctx.save()
      ctx.globalCompositeOperation = 'lighter'
      for (let i = tailParticles.length - 1; i >= 0; i--) {
        const tp = tailParticles[i]
        tp.life++
        tp.vx *= 0.96
        tp.x += tp.vx
        tp.y += tp.vy
        if (tp.life >= tp.maxLife || tp.x < PAD_X - 4) {
          tailParticles.splice(i, 1)
          continue
        }
        const p = tp.life / tp.maxLife
        const a = (1 - p) * 0.95 * cometOpacity
        const halo = tp.r * 5.5
        ctx.globalAlpha = a
        ctx.drawImage(getMoteSprite(tp.stage), tp.x - halo, tp.y - halo, halo * 2, halo * 2)

        if (p < 0.5) {
          ctx.globalAlpha = a * (1 - p / 0.5)
          ctx.fillStyle = '#ffffff'
          ctx.beginPath()
          ctx.arc(tp.x, tp.y, Math.max(0.3, tp.r * 0.5), 0, Math.PI * 2)
          ctx.fill()
        }
      }
      ctx.restore()

      if (isCharging.value && !chargePaused.value && stage >= 2) {
        const arcCfg = stage === 2
          ? { chance: 0.35, max: 5, reach: 10, displace: 4, branch: 0 }
          : stage === 3
            ? { chance: 0.55, max: 8, reach: 14, displace: 6, branch: 0.35 }
            : stage === 4
              ? { chance: 0.8, max: 11, reach: 18, displace: 7.5, branch: 0.5 }
              : { chance: 0.9, max: 14, reach: 22, displace: 9, branch: 0.7 }

        if (cometArcs.length < arcCfg.max && Math.random() < arcCfg.chance) {
          const angle = (Math.random() * 2 - 1) * Math.PI
          const dist = 5 + Math.random() * arcCfg.reach
          const dx = Math.cos(angle) * dist + (speedFactor - 1) * 3
          const dy = Math.sin(angle) * dist * 0.65
          cometArcs.push({
            ...createBolt(
              (Math.random() - 0.5) * 4,
              (Math.random() - 0.5) * 2,
              dx, dy, arcCfg.displace, 2, arcCfg.branch
            ),
            life: 0,
            maxLife: 6 + Math.floor(Math.random() * 5),
            width: stage === 5 ? 1.35 : stage === 4 ? 1.15 : 0.95,
            phase: Math.random() * Math.PI * 2,
            stage
          })
        }

        for (let i = cometArcs.length - 1; i >= 0; i--) {
          const ca = cometArcs[i]
          ca.life++
          if (ca.life >= ca.maxLife) {
            cometArcs.splice(i, 1)
            continue
          }
          const progress = ca.life / ca.maxLife
          const alpha = Math.sin(progress * Math.PI) *
            (0.85 + 0.15 * Math.sin(ca.life * 2.3 + ca.phase)) * cometOpacity

          const shiftedBolt = {
            main: ca.main.map((pt) => ({ x: pt.x + headX, y: pt.y + headY })),
            branches: ca.branches.map((b) => b.map((pt) => ({ x: pt.x + headX, y: pt.y + headY })))
          }
          drawBolt(ctx, shiftedBolt, BOLT_PALETTE[ca.stage] || BOLT_PALETTE[5], ca.width, alpha)
        }
      }
    } else {
      tailParticles.length = 0
      cometArcs.length = 0
    }

    ctx.restore()
  }
  } catch (err) {
    console.error('BossProgressBar renderFxLoop error:', err)
  } finally {
    fxRafId = requestAnimationFrame(renderFxLoop)
  }
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
  gap: 12px;
  margin: 12px 0 8px;
  width: 100%;
}

.boss-progress-row.is-large {
  gap: 16px;
  margin: 14px 0 10px;
}

/* 进度条轨道：无外圈杂色粗边框，纯净内嵌质感 */
.boss-bar-track {
  position: relative;
  flex: 1;
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(8, 13, 26, 0.92) 0%, rgba(15, 23, 42, 0.78) 100%);
  overflow: visible;
  box-shadow:
    inset 0 2px 5px rgba(0, 0, 0, 0.75),
    inset 0 0 0 1px rgba(255, 255, 255, 0.08),
    0 1px 0 rgba(255, 255, 255, 0.05);
  transition: box-shadow 0.35s ease;
}

.boss-progress-row.is-large .boss-bar-track {
  height: 16px;
}

/* 内部圆角裁剪容器：确保所有填充层与掠影严格贴合胶囊圆角 */
.boss-bar-clip {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  overflow: hidden;
  z-index: 2;
}

.boss-bar-layer {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 999px;
  overflow: hidden;
  pointer-events: none;
  box-shadow:
    inset 0 1px 1px rgba(255, 255, 255, 0.3),
    inset 0 -1px 1.5px rgba(0, 0, 0, 0.25);
}

/*
 * 全层通用斜切柔光掠影 (第 1 ~ 5 层全部配备)
 * 从左端扫掠至右端，赋予所有进度层灵动通透的光泽感
 */
.boss-bar-layer::after {
  content: '';
  position: absolute;
  top: -40%;
  bottom: -40%;
  left: -34%;
  width: 30%;
  min-width: 72px;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0) 0%,
    rgba(255, 255, 255, 0.08) 22%,
    rgba(255, 255, 255, 0.42) 50%,
    rgba(255, 255, 255, 0.08) 78%,
    rgba(255, 255, 255, 0) 100%
  );
  transform: skewX(-22deg);
  animation: sheenSweepRight 6.8s cubic-bezier(0.38, 0.05, 0.24, 1) infinite;
  mix-blend-mode: overlay;
  pointer-events: none;
}

/* 第 1 层：系统主题蓝 (0% ~ 100%) */
.layer-theme {
  background: linear-gradient(90deg, #1d4ed8 0%, #2563eb 52%, #0ea5e9 82%, #38bdf8 100%);
  z-index: 1;
}
.boss-bar-track.stage-glow-1 {
  box-shadow:
    inset 0 2px 5px rgba(0, 0, 0, 0.7),
    0 0 14px rgba(56, 189, 248, 0.32);
}

/* 第 2 层：星云紫 (100% ~ 200%, 1X) */
.layer-purple {
  background: linear-gradient(90deg, #581c87 0%, #7e22ce 45%, #a855f7 80%, #e879f9 100%);
  z-index: 2;
}
.boss-bar-track.stage-glow-2 {
  box-shadow:
    inset 0 2px 5px rgba(0, 0, 0, 0.7),
    0 0 16px rgba(168, 85, 247, 0.48);
}

/* 第 3 层：烈焰红 (200% ~ 300%, 2X) */
.layer-red {
  background: linear-gradient(90deg, #7f1d1d 0%, #dc2626 48%, #f97316 84%, #fda4af 100%);
  z-index: 3;
}
.boss-bar-track.stage-glow-3 {
  box-shadow:
    inset 0 2px 5px rgba(0, 0, 0, 0.7),
    0 0 18px rgba(239, 68, 68, 0.58);
}

/* 第 4 层：黄金流火 (300% ~ 400%, 3X) */
.layer-gold-base {
  background: linear-gradient(90deg, #92400e 0%, #d97706 38%, #f59e0b 72%, #fef08a 100%);
  z-index: 4;
}
.boss-bar-track.stage-glow-4 {
  box-shadow:
    inset 0 2px 5px rgba(0, 0, 0, 0.7),
    0 0 22px rgba(245, 158, 11, 0.7);
}

/* 第 5 层及以上：荒野乱斗究极彩晶流光 (400%+, 4X ~ 5X+) */
.layer-chroma-base {
  background: linear-gradient(
    90deg,
    #34d399 0%,
    #38bdf8 10%,
    #818cf8 20%,
    #c084fc 30%,
    #f472b6 40%,
    #fde047 46%,
    #34d399 50%,
    #38bdf8 60%,
    #818cf8 70%,
    #c084fc 80%,
    #f472b6 90%,
    #fde047 96%,
    #34d399 100%
  );
  background-size: 200% 100%;
  background-repeat: repeat-x;
  animation: chromaFlowRight 7.5s linear infinite;
  box-shadow:
    inset 0 1.5px 1px rgba(255, 255, 255, 0.45),
    inset 0 -1.5px 2px rgba(15, 23, 42, 0.3);
  z-index: 5;
}

.boss-bar-track.stage-glow-5 {
  box-shadow:
    0 0 18px rgba(56, 189, 248, 0.5),
    0 0 30px rgba(192, 132, 252, 0.32),
    inset 0 2px 5px rgba(0, 0, 0, 0.65);
}

.boss-bar-track.is-500-max {
  box-shadow:
    0 0 24px rgba(56, 189, 248, 0.65),
    0 0 42px rgba(244, 114, 182, 0.45),
    inset 0 2px 4px rgba(0, 0, 0, 0.6);
}

@keyframes chromaFlowRight {
  0% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

@keyframes sheenSweepRight {
  0% { left: -34%; opacity: 0; }
  8% { opacity: 1; }
  82% { left: 104%; opacity: 1; }
  92%, 100% { left: 108%; opacity: 0; }
}

.boss-fx-canvas {
  position: absolute;
  pointer-events: none;
  z-index: 12;
}

/* ==================== 读数与倍率徽章 ==================== */
.boss-bar-label {
  font-size: 13px;
  font-weight: 700;
  min-width: 78px;
  text-align: right;
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  white-space: nowrap;
}

.boss-progress-row.is-large .boss-bar-label {
  font-size: 15px;
  min-width: 88px;
}

.stage-tag {
  font-size: 11px;
  font-weight: 800;
  padding: 1.5px 7px;
  border-radius: 999px;
  color: #fff;
  line-height: 1.25;
  letter-spacing: 0.3px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.label-stage-1 { color: #38bdf8; }
.label-stage-1 .stage-tag {
  background: linear-gradient(135deg, #0284c7, #38bdf8);
  box-shadow: 0 0 8px rgba(56, 189, 248, 0.45);
}

.label-stage-2 { color: #c084fc; }
.label-stage-2 .stage-tag {
  background: linear-gradient(135deg, #7e22ce, #c084fc);
  box-shadow: 0 0 8px rgba(168, 85, 247, 0.48);
}

.label-stage-3 { color: #f87171; }
.label-stage-3 .stage-tag {
  background: linear-gradient(135deg, #b91c1c, #fb7185);
  box-shadow: 0 0 8px rgba(239, 68, 68, 0.5);
}

.label-stage-4 { color: #fde047; }
.label-stage-4 .stage-tag {
  background: linear-gradient(135deg, #b45309, #f59e0b, #fde047);
  color: #1e1b4b;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.45);
  box-shadow: 0 0 10px rgba(245, 158, 11, 0.65);
}

/* 第 5 层专属彩晶徽章 (4X 充能阶段) */
.label-stage-5 {
  background: linear-gradient(90deg, #7dd3fc 0%, #f472b6 55%, #fde047 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 0 8px rgba(56, 189, 248, 0.45));
}
.label-stage-5 .stage-tag {
  -webkit-text-fill-color: #ffffff;
  background: linear-gradient(
    90deg,
    #34d399 0%,
    #38bdf8 12%,
    #c084fc 25%,
    #f472b6 38%,
    #fde047 46%,
    #34d399 50%,
    #38bdf8 62%,
    #c084fc 75%,
    #f472b6 88%,
    #fde047 96%,
    #34d399 100%
  );
  background-size: 200% 100%;
  animation: chromaFlowRight 7.5s linear infinite;
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.72), 0 0 6px rgba(244, 114, 182, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.55);
  text-shadow: 0 1px 2px rgba(15, 23, 42, 0.75);
}

/* 5X 及往后专属（5X, 6X... 纯净顺时针旋转流光色环，内嵌白色高光段，无多余图标） */
.stage-tag.is-5x {
  position: relative;
  padding: 2.5px 10px;
  border-radius: 999px;
  color: #ffffff;
  -webkit-text-fill-color: #ffffff;
  font-weight: 900;
  text-shadow: 0 1px 2px rgba(9, 13, 28, 0.95), 0 0 8px rgba(56, 189, 248, 0.85);
  overflow: hidden;
  background: transparent;
  border: none;
  box-shadow:
    0 0 14px rgba(56, 189, 248, 0.8),
    0 0 24px rgba(244, 114, 182, 0.55);
  z-index: 1;
  isolation: isolate;
}

/* 5X+ 徽章外环：顺时针旋转完整流彩色环 + 内嵌一小段纯白高光 (0deg~28deg) */
.stage-tag.is-5x::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 120px;
  height: 120px;
  margin-top: -60px;
  margin-left: -60px;
  background: conic-gradient(
    from 0deg,
    #ffffff 0deg,
    #e0f2fe 18deg,
    #38bdf8 48deg,
    #818cf8 105deg,
    #c084fc 160deg,
    #f472b6 220deg,
    #fde047 280deg,
    #34d399 330deg,
    #ffffff 360deg
  );
  animation: spinClockwise 6.5s linear infinite;
  z-index: -2;
}

/* 5X+ 徽章内胆：留出 2px 鲜明顺时针流光外环 */
.stage-tag.is-5x::after {
  content: '';
  position: absolute;
  inset: 2px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.82) 0%, rgba(30, 27, 75, 0.78) 100%);
  box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.25);
  z-index: -1;
}

@keyframes spinClockwise {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
