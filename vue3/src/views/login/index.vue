<template>
  <div class="login-page">
    <canvas ref="particleCanvas" class="particle-bg"></canvas>

    <div class="login-shell">
      <!-- 时钟 -->
      <div class="login-time">{{ currentTime }}</div>
      <div class="login-date">{{ currentDate }}</div>

      <!-- 锁屏登录卡片（毛玻璃） -->
      <div class="lock-card" :class="{ 'is-expanded': expanded }">
        <!-- 头像 -->
        <div class="lock-card__avatar" @click="toggleExpand">
          <svg viewBox="0 0 100 100" class="avatar-icon" aria-hidden="true">
            <circle cx="50" cy="50" r="46" fill="none" stroke="#1f1f1f" stroke-width="4" />
            <circle cx="50" cy="40" r="15" fill="none" stroke="#1f1f1f" stroke-width="4" />
            <path
              d="M22 78 a28 26 0 0 1 56 0"
              fill="none"
              stroke="#1f1f1f"
              stroke-width="4"
              stroke-linecap="round"
            />
          </svg>
        </div>

        <!-- 账号名 -->
        <div class="lock-card__name">{{ loginForm.username }}</div>

        <!-- 展开后的输入区 -->
        <el-form
          v-show="expanded"
          ref="loginRef"
          :model="loginForm"
          :rules="loginRules"
          class="lock-card__form"
          @submit.prevent
        >
          <el-form-item prop="password">
            <el-input
              ref="passwordRef"
              v-model="loginForm.password"
              type="password"
              size="large"
              auto-complete="off"
              placeholder="密码"
              show-password
              @keyup.enter="submit"
            >
              <template #prefix>
                <svg-icon icon-class="password" class="input-icon" />
              </template>
            </el-input>
          </el-form-item>

          <el-form-item v-if="captchaEnabled" prop="code" class="captcha-item">
            <el-input
              v-model="loginForm.code"
              size="large"
              auto-complete="off"
              placeholder="验证码"
              class="captcha-input"
              @keyup.enter="submit"
            >
              <template #prefix>
                <svg-icon icon-class="validCode" class="input-icon" />
              </template>
            </el-input>
            <div class="captcha-img" @click="getCode">
              <img :src="codeUrl" alt="验证码" />
            </div>
          </el-form-item>

          <el-checkbox v-model="loginForm.rememberMe" class="remember-me">记住密码</el-checkbox>
        </el-form>

        <!-- 登录按钮 -->
        <el-button
          class="lock-card__submit"
          :loading="loading"
          size="large"
          @click="onSubmitClick"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
      </div>
    </div>

    <!-- 底部版权 -->
    <div v-if="footerContent" class="login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useLogin } from './composables/useLogin'
import defaultSettings from '@/settings'

const { proxy } = getCurrentInstance()
const footerContent = defaultSettings.footerVisible ? defaultSettings.footerContent : ''

const {
  loginForm,
  loading,
  codeUrl,
  captchaEnabled,
  bindRedirect,
  getCode,
  loadRememberedAccount,
  handleLogin
} = useLogin()

// 卡片是否展开（展开后显示密码输入区）
const expanded = ref(false)
const passwordRef = ref(null)

const loginRules = {
  password: [{ required: true, trigger: 'blur', message: '请输入您的密码' }],
  code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
}

// 点击头像：切换展开
function toggleExpand() {
  expanded.value ? collapse() : expand()
}

function expand() {
  expanded.value = true
  nextTick(() => passwordRef.value && passwordRef.value.focus())
}

function collapse() {
  expanded.value = false
}

// 点击登录按钮：未展开先展开，已展开则提交
function onSubmitClick() {
  if (!expanded.value) {
    expand()
    return
  }
  submit()
}

function submit() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      handleLogin()
    }
  })
}

// 时钟 + 粒子背景
const currentTime = ref('')
const currentDate = ref('')
const particleCanvas = ref(null)
let timer = null
let animationId = null
let particles = []
let resizeHandler = null

function startClock() {
  const update = () => {
    const now = new Date()
    const pad = n => String(n).padStart(2, '0')
    currentTime.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
    const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    currentDate.value = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${days[now.getDay()]}`
  }
  update()
  timer = setInterval(update, 1000)
}

function initParticles() {
  const canvas = particleCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  resizeHandler = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  resizeHandler()
  window.addEventListener('resize', resizeHandler)

  particles = Array.from({ length: 88 }, () => ({
    x: Math.random() * canvas.width,
    y: Math.random() * canvas.height,
    r: Math.random() * 2 + 1,
    dx: (Math.random() - 0.5) * 0.55,
    dy: (Math.random() - 0.5) * 0.55,
    alpha: Math.random() * 0.5 + 0.18
  }))

  const draw = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    particles.forEach(p => {
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(255,255,255,${p.alpha})`
      ctx.fill()
      p.x += p.dx
      p.y += p.dy
      if (p.x < 0 || p.x > canvas.width) p.dx *= -1
      if (p.y < 0 || p.y > canvas.height) p.dy *= -1
    })
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const a = particles[i]
        const b = particles[j]
        const dist = Math.hypot(a.x - b.x, a.y - b.y)
        if (dist < 125) {
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          ctx.strokeStyle = `rgba(255,255,255,${0.14 * (1 - dist / 125)})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    }
    animationId = requestAnimationFrame(draw)
  }
  draw()
}

bindRedirect()
getCode()
loadRememberedAccount()

onMounted(() => {
  startClock()
  initParticles()
})

onBeforeUnmount(() => {
  clearInterval(timer)
  cancelAnimationFrame(animationId)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
})
</script>

<style scoped lang="scss">
@use './styles/login.scss';
</style>
