<template>
  <div class="translate-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>中英互译</span>
          <el-tag v-if="directionLabel" type="info" size="small">{{ directionLabel }}</el-tag>
        </div>
      </template>
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="6"
        resize="vertical"
        maxlength="450"
        show-word-limit
        placeholder="输入单词或句子：中文自动翻译成英文，英文自动翻译成中文（Ctrl + Enter 快速翻译）"
        @keydown.ctrl.enter="handleTranslate"
      />
      <div class="actions">
        <el-button type="primary" :loading="loading" :disabled="!inputText.trim()" @click="handleTranslate">翻译</el-button>
        <el-button @click="handleClear">清空</el-button>
      </div>
    </el-card>

    <el-card v-if="result" class="result-card">
      <template #header>
        <div class="card-header">
          <span>翻译结果</span>
          <el-button link type="primary" @click="copyResult">复制</el-button>
        </div>
      </template>
      <div class="result-main">
        <el-tag v-if="result.speech" size="small" type="success" class="speech-tag">{{ result.speech }}</el-tag>
        <span class="result-text">{{ result.text }}</span>
      </div>
      <div v-if="result.relatedWords && result.relatedWords.length" class="related">
        <span class="related-label">关联词：</span>
        <el-tag
          v-for="w in result.relatedWords"
          :key="w.word"
          class="related-tag"
          @click="useRelated(w.word)"
        >{{ w.word }}<span v-if="w.speech" class="related-speech"> {{ w.speech }}</span></el-tag>
      </div>
    </el-card>

    <el-card class="history-card">
      <template #header>
        <div class="card-header">
          <span>翻译记录</span>
          <el-button v-if="records.length" link type="danger" @click="handleClearRecords">
            <el-icon><Delete /></el-icon>
            <span>清空</span>
          </el-button>
        </div>
      </template>
      <div v-if="records.length">
        <div v-for="r in records" :key="r.id" class="history-item">
          <div class="history-content" @click="restore(r)">
            <p class="history-source">{{ r.sourceText }}</p>
            <p class="history-result">
              <el-tag v-if="r.speech" size="small" type="success" class="speech-tag">{{ r.speech }}</el-tag>
              <span class="history-result-text">{{ r.resultText }}</span>
            </p>
          </div>
          <el-button link type="danger" class="history-delete" @click.stop="handleDelete(r.id)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
      <el-empty v-else description="暂无翻译记录" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  translateText,
  getTranslateRecords,
  deleteTranslateRecord,
  clearTranslateRecords
} from '@/api/student'

const inputText = ref('')
const result = ref(null)
const records = ref([])
const loading = ref(false)

// 输入包含中文字符 → 中译英，否则视为英译中
const hasChinese = computed(() => /[\u4e00-\u9fa5]/.test(inputText.value))
const directionLabel = computed(() => {
  if (!inputText.value.trim()) return ''
  return hasChinese.value ? '中文 → 英文' : '英文 → 中文'
})

const loadRecords = async () => {
  const res = await getTranslateRecords()
  records.value = res.data || []
}

const handleTranslate = async () => {
  const text = inputText.value.trim()
  if (!text) return ElMessage.warning('请输入需要翻译的内容')
  loading.value = true
  try {
    const res = await translateText({ text })
    const { result: translated, speech, relatedWords, record } = res.data
    result.value = { text: translated, speech, relatedWords: relatedWords || [] }
    if (record) {
      // 新记录置顶，最多保留 50 条
      records.value = [record, ...records.value.filter(r => r.id !== record.id)].slice(0, 50)
    }
  } catch (e) {
    // 错误提示已由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

// 点击关联词，直接翻译该词
const useRelated = word => {
  inputText.value = word
  handleTranslate()
}

// 点击记录，回填输入框并展示译文
const restore = r => {
  inputText.value = r.sourceText
  result.value = { text: r.resultText, speech: r.speech, relatedWords: [] }
}

const handleDelete = async id => {
  await deleteTranslateRecord(id)
  records.value = records.value.filter(r => r.id !== id)
  ElMessage.success('已删除')
}

const handleClearRecords = async () => {
  try {
    await ElMessageBox.confirm('确定清空全部翻译记录吗？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await clearTranslateRecords()
  records.value = []
  ElMessage.success('已清空')
}

const handleClear = () => {
  inputText.value = ''
  result.value = null
}

const copyResult = async () => {
  try {
    await navigator.clipboard.writeText(result.value.text)
    ElMessage.success('已复制到剪贴板')
  } catch (e) {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}

onMounted(loadRecords)
</script>

<style scoped>
.translate-page { max-width: 900px; margin: 0 auto; padding: 20px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.actions { margin-top: 16px; display: flex; gap: 12px; }
.result-card { margin-top: 20px; }
.result-main { display: flex; align-items: flex-start; gap: 8px; }
.result-text { color: #303133; font-size: 15px; line-height: 1.8; white-space: pre-wrap; word-break: break-word; }
.speech-tag { flex-shrink: 0; }
.related { margin-top: 14px; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.related-label { color: #909399; font-size: 13px; }
.related-tag { cursor: pointer; }
.related-speech { color: #909399; }
.history-card { margin-top: 20px; }
.history-item { display: flex; align-items: center; padding: 12px 0; border-bottom: 1px solid #ebeef5; }
.history-item:last-child { border-bottom: none; }
.history-content { flex: 1; cursor: pointer; min-width: 0; }
.history-source { color: #303133; font-weight: 600; margin-bottom: 6px; word-break: break-word; }
.history-result { color: #606266; font-size: 14px; line-height: 1.6; word-break: break-word; }
.history-result-text { margin-left: 4px; }
.history-delete { flex-shrink: 0; }
</style>
