<!-- src/App.vue -->
<template>
  <!-- 放入全局 Toast 组件 -->
  <GlobalToast />

  <router-view></router-view>
</template>

<script setup>
import GlobalToast from '@/components/GlobalToast.vue'
</script>

<style>
body {
  margin: 0;
  padding: 0;
}
</style>