<template>
  <div class="dash-page">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <h2>{{ stat.userCount }}</h2>
          <p>总用户数</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <h2>{{ stat.wordCount }}</h2>
          <p>单词总量</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <h2>{{ stat.paperCount }}</h2>
          <p>试卷总数</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <h2>{{ stat.questionCount }}</h2>
          <p>试题总数</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getStats } from '@/api/admin'
const stat = ref({})
const loadStat = async () => {
  const res = await getStats()
  if(res.code === 200) stat.value = res.data
}
onMounted(loadStat)
</script>

<style scoped>
.dash-page { padding:20px; }
.stat-card h2 { font-size:32px; color:#409eff; margin:10px 0; }
.stat-card p { color:#909399; }
</style>