<template>
  <div class="desktop-hero">
    <!-- 涟漪光环 -->
    <span class="ripple ripple--1"></span>
    <span class="ripple ripple--2"></span>
    <span class="ripple ripple--3"></span>
    <!-- 品牌标志 -->
    <img src="/logo/logo_center.png" class="brand-mark" alt="brand" />
  </div>
</template>

<script setup></script>

<style scoped lang="scss">
.desktop-hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 420px;
  height: 420px;
}

.brand-mark {
  position: relative;
  z-index: 2;
  width: 180px;
  height: 140px;
  filter: drop-shadow(0 6px 18px rgba(75, 30, 158, 0.25));
}

.ripple {
  position: absolute;
  top: 50%;
  left: 50%;
  border-radius: 50%;
  border: 1.5px solid rgba(123, 63, 211, 0.18);
  transform: translate(-50%, -50%);
  animation: ripple-pulse 4s ease-in-out infinite;
}

.ripple--1 {
  width: 240px;
  height: 240px;
  animation-delay: 0s;
}

.ripple--2 {
  width: 330px;
  height: 330px;
  animation-delay: 0.6s;
  border-color: rgba(123, 63, 211, 0.12);
}

.ripple--3 {
  width: 420px;
  height: 420px;
  animation-delay: 1.2s;
  border-color: rgba(123, 63, 211, 0.07);
}

@keyframes ripple-pulse {
  0%,
  100% {
    opacity: 0.6;
    transform: translate(-50%, -50%) scale(0.96);
  }
  50% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.04);
  }
}
</style>
