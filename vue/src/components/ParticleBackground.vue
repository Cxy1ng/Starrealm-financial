<template>
  <div class="ambient-backdrop" aria-hidden="true">
    <!-- 动态流体极光光斑 (Aurora Orbs，跟随当前调色盘主题色实时渲染) -->
    <div class="aurora-orb orb-1"></div>
    <div class="aurora-orb orb-2"></div>
    <div class="aurora-orb orb-3"></div>
    <!-- 交互式星尘粒子连线画布 -->
    <canvas ref="canvasRef" class="particle-canvas"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  isDark: {
    type: Boolean,
    default: false
  },
  particleCount: {
    type: Number,
    default: 52
  }
})

const canvasRef = ref(null)
let animationFrameId = null
let particles = []
let mouse = { x: -1000, y: -1000, radius: 140 }

const createParticles = (width, height) => {
  const count = Math.min(props.particleCount, Math.floor((width * height) / 20000))
  particles = Array.from({ length: Math.max(28, count) }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    vx: (Math.random() - 0.5) * 0.48,
    vy: (Math.random() - 0.5) * 0.48,
    size: Math.random() * 2.2 + 1.1,
    alpha: Math.random() * 0.5 + 0.32
  }))
}

const handleMouseMove = (e) => {
  mouse.x = e.clientX
  mouse.y = e.clientY
}

const handleMouseLeave = () => {
  mouse.x = -1000
  mouse.y = -1000
}

let resizeHandler = null

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')

  const setSize = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
    createParticles(canvas.width, canvas.height)
  }
  setSize()

  resizeHandler = () => setSize()
  window.addEventListener('resize', resizeHandler)
  window.addEventListener('mousemove', handleMouseMove, { passive: true })
  window.addEventListener('mouseleave', handleMouseLeave)

  const render = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    const rootStyle = getComputedStyle(document.documentElement)
    const dark = document.documentElement.classList.contains('dark') || props.isDark

    // 读取当前调色盘对应的粒子 RGB（暗色模式下采用高亮清透色，如亮冰蓝/亮薄荷青/星云紫/鎏金蛋黄）
    const darkRgb = rootStyle.getPropertyValue('--theme-particle-dark').trim() || '125, 211, 252'
    const lightRgb = rootStyle.getPropertyValue('--theme-particle-light').trim() || '22, 119, 255'
    const activeRgb = dark ? darkRgb : lightRgb

    for (let i = 0; i < particles.length; i++) {
      const p = particles[i]
      p.x += p.vx
      p.y += p.vy

      if (p.x < 0 || p.x > canvas.width) p.vx *= -1
      if (p.y < 0 || p.y > canvas.height) p.vy *= -1

      // 鼠标轻推互动
      const dxMouse = p.x - mouse.x
      const dyMouse = p.y - mouse.y
      const distMouse = Math.sqrt(dxMouse * dxMouse + dyMouse * dyMouse)
      if (distMouse < mouse.radius && distMouse > 0) {
        const force = (mouse.radius - distMouse) / mouse.radius
        p.x += (dxMouse / distMouse) * force * 1.15
        p.y += (dyMouse / distMouse) * force * 1.15
      }

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
      const dotAlpha = dark ? Math.min(0.92, p.alpha + 0.22) : p.alpha
      ctx.fillStyle = `rgba(${activeRgb}, ${dotAlpha})`
      if (dark) {
        ctx.shadowBlur = 8
        ctx.shadowColor = `rgba(${activeRgb}, 0.65)`
      } else {
        ctx.shadowBlur = 0
      }
      ctx.fill()
      ctx.shadowBlur = 0

      // 粒子间连线
      for (let j = i + 1; j < particles.length; j++) {
        const p2 = particles[j]
        const dx = p.x - p2.x
        const dy = p.y - p2.y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 130) {
          const opacity = (1 - dist / 130) * (dark ? 0.26 : 0.14)
          ctx.beginPath()
          ctx.moveTo(p.x, p.y)
          ctx.lineTo(p2.x, p2.y)
          ctx.strokeStyle = `rgba(${activeRgb}, ${opacity})`
          ctx.lineWidth = dark ? 0.95 : 0.8
          ctx.stroke()
        }
      }

      // 与鼠标连线
      if (distMouse < mouse.radius) {
        const opacity = (1 - distMouse / mouse.radius) * (dark ? 0.42 : 0.28)
        ctx.beginPath()
        ctx.moveTo(p.x, p.y)
        ctx.lineTo(mouse.x, mouse.y)
        ctx.strokeStyle = `rgba(${activeRgb}, ${opacity})`
        ctx.lineWidth = 1.1
        ctx.stroke()
      }
    }

    animationFrameId = requestAnimationFrame(render)
  }

  render()
})

onBeforeUnmount(() => {
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mouseleave', handleMouseLeave)
})
</script>

<style scoped>
.ambient-backdrop {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.particle-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.aurora-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(84px);
  opacity: 0.56;
  transition: background 0.6s ease;
  animation: floatOrb 18s ease-in-out infinite alternate;
}

html.dark .aurora-orb {
  opacity: 0.36;
}

.orb-1 {
  width: 450px;
  height: 450px;
  background: radial-gradient(circle, var(--theme-orb-1), transparent 72%);
  top: -120px;
  left: 12%;
}

.orb-2 {
  width: 390px;
  height: 390px;
  background: radial-gradient(circle, var(--theme-orb-2), transparent 72%);
  bottom: -100px;
  right: 8%;
  animation-delay: -6s;
  animation-duration: 22s;
}

.orb-3 {
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, var(--theme-orb-3), transparent 72%);
  top: 42%;
  left: 55%;
  animation-delay: -11s;
  animation-duration: 25s;
}

@keyframes floatOrb {
  0% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(35px, -28px, 0) scale(1.08);
  }
  100% {
    transform: translate3d(-25px, 24px, 0) scale(0.95);
  }
}
</style>
