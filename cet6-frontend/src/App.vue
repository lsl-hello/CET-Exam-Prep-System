<template>
  <router-view />
  <ChangePasswordDialog />
</template>

<script setup>
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Microsoft YaHei', sans-serif; background: #f5f7fa; }
</style>
