<template>
  <div class="report-page">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>单词掌握统计</template>
          <div ref="wordChartRef" style="width:100%; height:300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>刷题错题分布(仅记录阅读和听力)</template>
          <div ref="qChartRef" style="width:100%; height:300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getWordStat, getWeakQuestionReport } from '@/api/student'

const wordChartRef = ref(null)
const qChartRef = ref(null)
let wordChart = null
let qChart = null

const initWordChart = async () => {
  try {
    const res = await getWordStat()
    await nextTick()
    if (!wordChartRef.value) return
    wordChart = echarts.init(wordChartRef.value)
    wordChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { data: ['熟练', '模糊', '遗忘'] },
      yAxis: { minInterval: 1 },
      series: [{
        type: 'bar',
        data: [
          res.data.know ?? 0,
          res.data.vague ?? 0,
          res.data.forget ?? 0
        ],
        itemStyle: { color: '#409eff' }
      }]
    })
  } catch (e) {
    console.error('单词统计图表初始化失败', e)
  }
}

const initQChart = async () => {
  try {
    const res = await getWeakQuestionReport()
    await nextTick()
    if (!qChartRef.value) return
    qChart = echarts.init(qChartRef.value)
    qChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '60%'],
        data: res.data.chartData || []
      }]
    })
  } catch (e) {
    console.error('错题分布图表初始化失败', e)
  }
}

const handleResize = () => {
  wordChart?.resize()
  qChart?.resize()
}

onMounted(() => {
  initWordChart()
  initQChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  wordChart?.dispose()
  qChart?.dispose()
})
</script>

<style scoped>
.report-page { padding:20px; }
</style>
