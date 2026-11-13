import { ref, onMounted, onBeforeUnmount } from 'vue'

/**
 * 实时时钟（HH:mm），用于桌面顶栏时间显示
 */
export function useClock() {
  const time = ref('')
  let timer = null

  function pad(n) {
    return n < 10 ? '0' + n : '' + n
  }

  function update() {
    const now = new Date()
    time.value = `${pad(now.getHours())}:${pad(now.getMinutes())}`
  }

  onMounted(() => {
    update()
    timer = setInterval(update, 1000)
  })

  onBeforeUnmount(() => {
    timer && clearInterval(timer)
  })

  return { time }
}
