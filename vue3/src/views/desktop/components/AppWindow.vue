<template>
  <section ref="winRef" class="app-window" :aria-label="`${title}工作区`" tabindex="-1" @keydown.esc="emit('close')">
    <h1 class="app-window__sr-title"><slot name="title">{{ title }}</slot></h1>
    <div class="app-window__body">
      <slot />
    </div>
  </section>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'

defineProps({
  title: { type: String, default: '' },
  width: { type: Number, default: 1160 },
  height: { type: Number, default: 720 }
})

const emit = defineEmits(['close', 'minimize'])
const winRef = ref(null)

onMounted(() => nextTick(() => winRef.value?.focus()))
</script>

<style scoped lang="scss">
.app-window {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f3f7fc;
  outline: none;
}

.app-window__sr-title {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.app-window__body {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  overflow: hidden;
  background: transparent;
}
</style>
