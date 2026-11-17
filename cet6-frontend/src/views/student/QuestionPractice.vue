<template>
  <div class="practice-page">
    <!-- 顶部：试卷选择 -->
    <el-card class="top-bar">
      <div class="bar-row">
        <span class="page-title">刷题中心</span>
        <el-select
          v-model="levelFilter"
          placeholder="考试等级"
          style="width: 130px"
          @change="onLevelChange"
        >
          <el-option label="全部等级" :value="0" />
          <el-option label="四级" :value="4" />
          <el-option label="六级" :value="6" />
        </el-select>
        <el-select
          v-model="selectedPaperId"
          placeholder="选择试卷"
          @change="onPaperChange"
          style="width: 300px"
        >
          <el-option v-for="p in paperList" :key="p.id" :label="p.paperName" :value="p.id" />
        </el-select>
        <el-button v-if="route.query.qId" link type="info" @click="clearJump">退出错题重做</el-button>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane
        v-for="t in tabs"
        :key="t.value"
        :label="t.label"
        :name="String(t.value)"
      >
        <div v-loading="loading" class="tab-body">
          <el-empty v-if="!loading && list.length === 0" description="暂无该类型题目，请先选择试卷或联系管理员添加" />
          <el-card
            v-for="q in list"
            :key="q.id"
            :id="'q-' + q.id"
            class="q-card"
            :class="{ 'highlight': highlightId === q.id }"
            shadow="hover"
          >
            <div class="q-head">
              <el-tag :type="typeTag(q.questionType)" size="small">{{ typeLabel(q.questionType) }}</el-tag>
              <el-button
                class="fav-btn"
                :class="{ 'fav-active': favSet.has(q.id) }"
                link
                @click="toggleFav(q)"
                :title="favSet.has(q.id) ? '已加入错题本' : '加入错题本'"
              >
                <el-icon><StarFilled v-if="favSet.has(q.id)" /><Star v-else /></el-icon>
              </el-button>
            </div>
            <div class="q-title">{{ q.title }}</div>

            <!-- 听力音频：点击按钮播放 -->
            <div v-if="q.questionType === 1 && q.audioUrl" class="audio-box">
              <el-button type="primary" size="small" @click="toggleAudio(q)">
                {{ playingId === q.id ? '⏸ 暂停音频' : '▶ 播放音频' }}
              </el-button>
              <audio
                :ref="el => setAudioRef(q.id, el)"
                :src="q.audioUrl"
                @ended="playingId = null"
              />
            </div>

            <!-- 客观题选项：听力 / 阅读可点击选择答案 -->
            <div v-if="hasOptions(q)" class="options-wrapper">
              <el-radio-group v-model="answers[q.id]">
                <div v-for="(opt, idx) in parseOptions(q.options)" :key="idx" class="option-item">
                  <el-radio :label="opt">
                    <span class="option-label">{{ String.fromCharCode(65 + idx) }}.</span> {{ opt }}
                  </el-radio>
                </div>
              </el-radio-group>
            </div>

            <!-- 操作：四个类都有"查看答案解析" -->
            <div class="btn-group">
              <el-button type="primary" @click="revealAnswer(q)">查看答案解析</el-button>
            </div>

            <!-- 答案解析展示 -->
            <div v-if="revealed[q.id]" class="reveal-box">
              <div class="reveal-row">
                <span class="label">参考答案：</span>
                <span class="answer">{{ revealed[q.id].standardAnswer || '—' }}</span>
                <el-tag v-if="revealed[q.id].isCorrect === true" type="success" size="small">回答正确</el-tag>
                <el-tag v-else-if="revealed[q.id].isCorrect === false" type="danger" size="small">回答错误，已加入错题本</el-tag>
              </div>
              <div class="reveal-row">
                <span class="label">解析：</span>
                <span>{{ revealed[q.id].analysis || '—' }}</span>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { getPaperList, getQuestionList, submitAnswer, getQuestionReveal, getQuestionDetail, getErrorQuestionIds, favoriteQuestion } from '@/api/student'

const route = useRoute()
const router = useRouter()

// 四个类
const tabs = [
  { value: 1, label: '听力' },
  { value: 2, label: '阅读' },
  { value: 3, label: '翻译' },
  { value: 4, label: '写作' }
]
const typeLabel = t => ({ 1: '听力', 2: '阅读', 3: '翻译', 4: '写作' }[t] || '未知')
const typeTag = t => ({ 1: 'success', 2: '', 3: 'warning', 4: 'danger' }[t] || 'info')

const paperList = ref([])
const levelFilter = ref(0)   // 0=全部 4=四级 6=六级
const selectedPaperId = ref(null)
const activeTab = ref('1')
const list = ref([])
const loading = ref(false)

// 每题作答 + 解析展示
const answers = reactive({})       // { [questionId]: 选中选项 }
const revealed = reactive({})      // { [questionId]: { standardAnswer, analysis, isCorrect } }

// 音频播放
const playingId = ref(null)
const audioRefs = {}
const setAudioRef = (id, el) => { if (el) audioRefs[id] = el }

// 错题重做高亮
const highlightId = ref(null)

// 收藏错题本：当前用户已收藏的题目ID集合
const favSet = reactive(new Set())
const loadFavIds = async () => {
  try {
    const res = await getErrorQuestionIds()
    if (res.code === 200) {
      favSet.clear()
      ;(res.data || []).forEach(id => favSet.add(id))
    }
  } catch (e) {}
}
const toggleFav = async (q) => {
  try {
    await favoriteQuestion(q.id)
    if (favSet.has(q.id)) {
      favSet.delete(q.id)
      ElMessage.success('已取消收藏')
    } else {
      favSet.add(q.id)
      ElMessage.success('已加入错题本')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 选项解析：后端 options 是 JSON 数组字符串
const parseOptions = (opts) => {
  if (!opts) return []
  if (Array.isArray(opts)) return opts
  try {
    const parsed = JSON.parse(opts)
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    return String(opts).split(/[\n,，]/).map(s => s.trim()).filter(Boolean)
  }
}
const hasOptions = (q) => parseOptions(q.options).length > 0

// 加载试卷列表（按考试等级过滤）
const loadPapers = async () => {
  try {
    const res = await getPaperList(levelFilter.value || undefined)
    if (res.code === 200) paperList.value = res.data || []
  } catch (e) {
    paperList.value = []
  }
}

// 切换等级：重新拉取试卷并默认选中第一张
const onLevelChange = async () => {
  await loadPapers()
  selectedPaperId.value = paperList.value.length ? paperList.value[0].id : null
  list.value = []
  if (selectedPaperId.value) loadQuestions()
}

// 加载当前 tab + 试卷 的题目
const loadQuestions = async () => {
  list.value = []
  if (!selectedPaperId.value) return
  loading.value = true
  try {
    const res = await getQuestionList({
      paperId: selectedPaperId.value,
      questionType: Number(activeTab.value),
      pageNum: 1,
      pageSize: 200
    })
    if (res.code === 200) {
      list.value = res.data.records || []
    }
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

const onPaperChange = () => {
  loadQuestions()
}
const onTabChange = () => {
  loadQuestions()
}

// 播放/暂停音频
const toggleAudio = (q) => {
  const audio = audioRefs[q.id]
  if (!audio) return
  if (playingId.value === q.id) {
    audio.pause()
    playingId.value = null
  } else {
    // 停掉其它正在播放的
    Object.values(audioRefs).forEach(a => { try { a.pause() } catch (e) {} })
    audio.play().catch(() => ElMessage.error('音频播放失败'))
    playingId.value = q.id
  }
}

// 查看答案解析
const revealAnswer = async (q) => {
  const isObjective = q.questionType === 1 || q.questionType === 2
  const userAns = answers[q.id]
  // 客观题且已选答案：提交判分（记录作答 + 错题本），返回答案解析 + 对错
  if (isObjective && userAns) {
    try {
      const res = await submitAnswer({ questionId: q.id, userAnswer: userAns })
      if (res.code === 200) {
        revealed[q.id] = {
          standardAnswer: res.data.standardAnswer,
          analysis: res.data.analysis,
          isCorrect: res.data.isCorrect
        }
        ElMessage[res.data.isCorrect ? 'success' : 'warning'](
          res.data.isCorrect ? '回答正确' : '回答错误，已加入错题本'
        )
      }
    } catch (e) {
      ElMessage.error('提交失败')
    }
    return
  }
  // 主观题 / 未作答客观题：直接展示答案解析
  try {
    const res = await getQuestionReveal(q.id)
    if (res.code === 200) {
      revealed[q.id] = {
        standardAnswer: res.data.standardAnswer,
        analysis: res.data.analysis,
        isCorrect: null
      }
    }
  } catch (e) {
    ElMessage.error('加载解析失败')
  }
}

// 错题本重做跳转：route.query.qId
const handleJump = async () => {
  const qId = route.query.qId
  if (!qId) return
  try {
    const res = await getQuestionDetail(qId)
    if (res.code === 200 && res.data) {
      const q = res.data
      if (q.paperId) {
        selectedPaperId.value = q.paperId
      }
      if (q.questionType) {
        activeTab.value = String(q.questionType)
      }
      await loadQuestions()
      // 滚动并高亮该题
      highlightId.value = Number(qId)
      await nextTick()
      const el = document.getElementById('q-' + qId)
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      setTimeout(() => { highlightId.value = null }, 3000)
    }
  } catch (e) {
    ElMessage.error('加载错题失败')
  }
}

const clearJump = () => {
  router.replace({ path: '/question-practice' })
  highlightId.value = null
}

onMounted(async () => {
  await loadPapers()
  loadFavIds()
  if (route.query.qId) {
    await handleJump()
  } else {
    // 默认选第一张试卷 + 听力 tab
    if (paperList.value.length) {
      selectedPaperId.value = paperList.value[0].id
      loadQuestions()
    }
  }
})
</script>

<style scoped>
.practice-page {
  padding: 20px;
  max-width: 960px;
  margin: 0 auto;
}
.bar-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.tab-body {
  min-height: 200px;
}
.q-card {
  margin-bottom: 18px;
  transition: box-shadow 0.3s, border-color 0.3s;
}
.q-card.highlight {
  border: 2px solid #409eff;
  box-shadow: 0 0 12px rgba(64, 158, 255, 0.5);
}
.q-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.fav-btn {
  margin-left: auto;
  font-size: 18px;
  color: #c0c4cc;
}
.fav-btn.fav-active {
  color: #f7ba2a;
}
.q-num {
  font-size: 13px;
  color: #909399;
}
.q-title {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 14px;
  white-space: pre-wrap;
}
.audio-box {
  margin-bottom: 14px;
}
.options-wrapper {
  padding: 8px 0;
  margin-bottom: 8px;
}
.option-item {
  margin-bottom: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background-color 0.2s;
}
.option-item:hover {
  background-color: #f5f7fa;
}
.option-label {
  font-weight: 600;
  color: #409eff;
  margin-right: 6px;
}
.btn-group {
  margin-top: 14px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.reveal-box {
  margin-top: 14px;
  padding: 14px 16px;
  background: #f0f9ff;
  border-left: 4px solid #409eff;
  border-radius: 6px;
  line-height: 1.9;
  font-size: 14px;
}
.reveal-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.reveal-row .label {
  color: #606266;
  font-weight: 600;
}
.reveal-row .answer {
  color: #e6a23c;
  font-weight: 600;
}

:deep(.el-radio) {
  display: flex;
  align-items: center;
  margin-right: 0;
  white-space: normal;
  word-break: break-word;
}
:deep(.el-radio__label) {
  white-space: normal;
  word-break: break-word;
  line-height: 1.6;
}
</style>
