import { ref } from 'vue'
import Cookies from 'js-cookie'
import { useRoute, useRouter } from 'vue-router'
import { getCodeImg } from '@/api/login'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import useUserStore from '@/store/modules/user'

/**
 * 登录鉴权逻辑（保留若依账号密码 + 验证码 + 记住密码能力）
 * 仅负责数据与流程，UI 由页面组件呈现，便于复用与维护。
 */
export function useLogin() {
  const userStore = useUserStore()
  const route = useRoute()
  const router = useRouter()

  const loginForm = ref({
    username: 'admin',
    password: 'admin123',
    rememberMe: false,
    code: '',
    uuid: '',
    // 云端 LoginBody 必填项：标识登录设备（后端按 macAddress 查终端，查不到不影响登录）
    macAddress: 'PC-WEB',
    deviceTypeCode: 'PC'
  })

  const loading = ref(false)
  const codeUrl = ref('')
  // 验证码开关（由后端返回决定，默认开启以兼容若依）
  const captchaEnabled = ref(true)
  const redirect = ref(undefined)

  // 监听重定向参数
  function bindRedirect() {
    redirect.value = route.query && route.query.redirect
  }

  // 获取验证码
  function getCode() {
    return getCodeImg().then(res => {
      captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
      if (captchaEnabled.value) {
        codeUrl.value = 'data:image/gif;base64,' + res.img
        loginForm.value.uuid = res.uuid
      }
    })
  }

  // 读取记住的账号密码
  function loadRememberedAccount() {
    const username = Cookies.get('username')
    const password = Cookies.get('password')
    const rememberMe = Cookies.get('rememberMe')
    loginForm.value = {
      ...loginForm.value,
      username: username === undefined ? loginForm.value.username : username,
      password: password === undefined ? loginForm.value.password : decrypt(password),
      rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
    }
  }

  // 持久化 / 清除记住密码
  function persistRememberMe() {
    if (loginForm.value.rememberMe) {
      Cookies.set('username', loginForm.value.username, { expires: 30 })
      Cookies.set('password', encrypt(loginForm.value.password), { expires: 30 })
      Cookies.set('rememberMe', loginForm.value.rememberMe, { expires: 30 })
    } else {
      Cookies.remove('username')
      Cookies.remove('password')
      Cookies.remove('rememberMe')
    }
  }

  // 执行登录
  function handleLogin() {
    loading.value = true
    persistRememberMe()
    return userStore
      .login(loginForm.value)
      .then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== 'redirect') {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        // noRedirect 是后端父级菜单「不跳转」哨兵，并非真实地址；遇到则回首页，避免 404 死循环
        const target = (!redirect.value || /noredirect/i.test(redirect.value)) ? '/index' : redirect.value
        router.push({ path: target, query: otherQueryParams })
      })
      .catch(() => {
        loading.value = false
        if (captchaEnabled.value) {
          getCode()
        }
      })
  }

  return {
    loginForm,
    loading,
    codeUrl,
    captchaEnabled,
    bindRedirect,
    getCode,
    loadRememberedAccount,
    handleLogin
  }
}
