<template>
  <div class="word-study-container">
    <!-- 没有学习计划时显示提示 -->
    <el-card v-if="!planInfo.hasPlan" shadow="hover" class="no-plan-card">
      <el-empty description="您还没有制定背单词学习计划">
        <el-button type="primary" size="large" @click="goToStudyPlan">去制定学习计划</el-button>
      </el-empty>
    </el-card>

    <!-- 有学习计划时显示背单词内容 -->
    <template v-else>
    <!-- 顶部今日背诵进度 -->
    <el-card shadow="hover" class="progress-card">
      <div class="progress-header">
        <h3>
          背诵进度
          <el-tag v-if="isReviewMode" type="warning" size="small" style="margin-left:8px;">复习模糊/遗忘</el-tag>
        </h3>
        <div class="progress-right">
          <span>每日词数：<b>{{ planInfo.dailyLimit }}</b></span>
          <span style="margin-left:16px">预计 {{ planInfo.estimatedDays }} 天背完</span>
        </div>
      </div>
      <div style="margin-bottom:8px;font-size:13px;color:#909399">
        总词库 {{ planInfo.totalWords }} 词<span v-if="planInfo.hasPlan"> · 当前计划「{{ planInfo.planName }}」<span v-if="planInfo.bookName"> · 词书「{{ planInfo.bookName }}」</span></span>
      </div>
      <div style="margin-bottom:8px;display:flex;justify-content:space-between;align-items:center">
        <span>{{ finishNum }} / {{ targetNum }} 个单词<span v-if="wrongReviewCount > 0" style="color:#909399;font-size:12px;margin-left:8px">（计划 {{ planInfo.dailyLimit }} + 错题复习 {{ wrongReviewCount }}）</span></span>
      </div>
      <el-progress :percentage="progressPercent" status="success" />
    </el-card>

    <!-- 单词卡片区域 -->
    <el-card v-if="currentWord" shadow="hover" class="word-card">
      <div class="word-top">
        <h2 class="word-text">{{ currentWord.word }}</h2>
        <el-button circle @click="playAudio" title="播放发音">
          <el-icon><VideoPlay /></el-icon>
        </el-button>
        <el-button circle :type="manualMarked ? 'warning' : 'default'" @click="addToWrongBookManual" title="加入错题本">
          <el-icon><Star /></el-icon>
        </el-button>
      </div>
      <div class="item">音标：{{ currentWord.phonetic }}</div>
      <div class="item">词性：{{ currentWord.speech }}</div>
      <div class="item">释义：{{ currentWord.translate }}</div>
      <div class="item">例句：{{ currentWord.sentence }}</div>

      <div class="btn-group">
        <el-button type="success" size="large" @click="submitReview(1)">认识</el-button>
        <el-button type="warning" size="large" @click="submitReview(2)">模糊</el-button>
        <el-button type="danger" size="large" @click="submitReview(3)">遗忘</el-button>
      </div>
    </el-card>

    <!-- 空状态 -->
    <el-empty v-else>
      <template #description>
        <div v-if="wrongSet.length > 0" class="review-entry">
          <p>本轮有 <b>{{ wrongSet.length }}</b> 个模糊/遗忘单词，建议再过一遍加深记忆</p>
          <el-button type="primary" @click="startReviewWrong">再过一遍</el-button>
        </div>
        <div v-else>
          <p>今日单词已全部背完，明天再来复习吧！</p>
        </div>
      </template>
    </el-empty>

    <!-- 今日已背单词回顾（背完后展示） -->
    <el-card v-if="!currentWord && finishedList.length > 0" shadow="hover" class="chart-card">
      <h3>今日已背单词（{{ finishedList.length }} 个）</h3>
      <el-table :data="finishedList" stripe size="small" max-height="360">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="word" label="单词" min-width="120" />
        <el-table-column prop="phonetic" label="音标" min-width="140" />
        <el-table-column prop="speech" label="词性" width="80" />
        <el-table-column prop="translate" label="释义" min-width="200" show-overflow-tooltip />
        <el-table-column label="记忆结果" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.memoryResult === 1" type="success" size="small">认识</el-tag>
            <el-tag v-else-if="row.memoryResult === 2" type="warning" size="small">模糊</el-tag>
            <el-tag v-else-if="row.memoryResult === 3" type="danger" size="small">遗忘</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.reviewType === 3" type="info" size="small">错题复习</el-tag>
            <el-tag v-else-if="row.reviewType === 2" type="primary" size="small">复习</el-tag>
            <el-tag v-else size="small">新词</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 统计图表 -->
    <el-card shadow="hover" class="chart-card">
      <h3>本周单词掌握统计</h3>
      <div ref="chartRef" style="width:100%;height:300px;"></div>
    </el-card>

    <audio v-if="currentWord" ref="audioRef" :src="currentWord.audioUrl" />
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VideoPlay, Star } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { useUserStore } from '@/store/user'
import { getTodayWordList, getWordDetail, submitWordReview, getWordStat, addWrongWord,
  removeWrongWordByWordId, checkWrongWord, getWordPlanInfo, getWrongWordList } from '@/api/student'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
// 用 userId 隔离进度数据：同账号退出再登录可恢复，切换账号互不干扰
// 用 localStorage 而非 sessionStorage：关闭浏览器/退出登录后仍保留
const uid = () => userStore.userInfo?.id || 0
const progressKey = () => `wordStudy.progress.u${uid()}`
const wrongSetKey = () => `wordStudy.wrongSet.u${uid()}`
const planLimitKey = () => `wordStudy.planLimit.u${uid()}`

const audioRef = ref(null)
const chartRef = ref(null)
let chartInstance = null

const wordList = ref([])
const currentIndex = ref(0)
const currentWord = ref(null)

// 本轮标记为 模糊/遗忘 的单词（用于"再过一遍"）
const wrongSet = ref([])
// 当前是否处于复习模式（再过一遍）
const isReviewMode = ref(false)
// 用户手动点星标过 → 视觉反馈
const manualMarked = ref(false)
// 今日已背单词列表（背完后回顾用）
const finishedList = ref([])
// 今日是否已背完（独立标志，不依赖 currentIndex/wordList.length 比较，
// 因为 wordList 每次从后端动态拉取，长度会变，旧位置在新列表里可能仍然有效）
const todayCompleted = ref(false)
// 当前学习模式：'normal' 正常计划 | 'wrongBook' 错题本重背
const studyMode = ref('normal')

// 持久化本轮 wrongSet，避免切页/路由切换后"再过一遍"按钮丢失
const saveWrongSet = () => {
  const key = wrongSetKey()
  if (wrongSet.value.length > 0) {
    localStorage.setItem(key, JSON.stringify(wrongSet.value))
  } else {
    localStorage.removeItem(key)
  }
}
const loadWrongSetFromStorage = () => {
  try {
    const saved = localStorage.getItem(wrongSetKey())
    if (saved) wrongSet.value = JSON.parse(saved)
  } catch (e) {
    console.error('恢复 wrongSet 失败', e)
  }
}
// 持久化今日进度（currentIndex / finishNum / finishedList / isReviewMode / todayCompleted / studyMode）
const saveProgress = () => {
  localStorage.setItem(progressKey(), JSON.stringify({
    currentIndex: currentIndex.value,
    finishNum: finishNum.value,
    finishedList: finishedList.value,
    isReviewMode: isReviewMode.value,
    todayCompleted: todayCompleted.value,
    studyMode: studyMode.value,
    // 存日期，第二天进来时 todayCompleted 失效，重新背新词
    studyDate: new Date().toDateString(),
    savedAt: Date.now()
  }))
}
const loadProgressFromStorage = () => {
  try {
    const saved = localStorage.getItem(progressKey())
    if (saved) {
      const p = JSON.parse(saved)
      // 只有今天的记录才恢复 todayCompleted/studyMode，避免第二天仍显示"已背完"
      const isToday = p.studyDate === new Date().toDateString()
      todayCompleted.value = isToday && (p.todayCompleted === true)
      studyMode.value = isToday ? (p.studyMode || 'normal') : 'normal'
      currentIndex.value = p.currentIndex || 0
      finishNum.value = p.finishNum || 0
      finishedList.value = p.finishedList || []
      isReviewMode.value = p.isReviewMode || false
    }
  } catch (e) {
    console.error('恢复进度失败', e)
  }
}

// 进度按实际单词数动态计算
const finishNum = ref(0)
// 计划词数（不含错题本），从 wordList 里统计
const plannedCount = computed(() => wordList.value.filter(w => w.reviewType !== 3).length)
// 错题复习词数
const wrongReviewCount = computed(() => wordList.value.filter(w => w.reviewType === 3).length)
// 进度条目标 = 计划词数 + 错题复习词数 = wordList.length
// 背完后保持 target = finishNum，进度条稳定显示 100%
const targetNum = computed(() =>
  todayCompleted.value ? finishNum.value : wordList.value.length
)
const progressPercent = computed(() => {
  if (todayCompleted.value) return 100
  return targetNum.value ? Math.floor(finishNum.value / targetNum.value * 100) : 0
})

// 学习计划信息：每日词数 + 预计天数
const planInfo = reactive({
  totalWords: 0,
  dailyLimit: 0,
  estimatedDays: 0,
  hasPlan: false,
  planName: '',
  bookName: ''
})
const loadPlanInfo = async () => {
  const res = await getWordPlanInfo()
  if (res.code === 200 && res.data) {
    Object.assign(planInfo, res.data)
  }
}
const goToStudyPlan = () => {
  router.push('/study-plan')
}

// 加载错题本所有单词，映射成 wordList 可背格式（id 用 wordId 对应 cet6_word.id）
const loadWrongBookWords = async () => {
  try {
    const res = await getWrongWordList({ pageNum: 1, pageSize: 1000 })
    if (res.code === 200 && res.data?.list) {
      wordList.value = res.data.list.map(r => ({
        id: r.wordId,
        word: r.word,
        phonetic: r.phonetic,
        speech: r.speech,
        translate: r.translate,
        sentence: r.sentence,
        audioUrl: r.audioUrl,
        reviewType: 3 // 标记为错题复习
      }))
    } else {
      wordList.value = []
    }
  } catch (e) {
    console.error('加载错题本失败', e)
    wordList.value = []
  }
}

const loadWordList = async () => {
  loadWrongSetFromStorage()
  loadProgressFromStorage()

  // 错题本"重背"模式：从错题本页点重背跳来，只背错题本所有单词，不混入计划新词
  if (route.query.mode === 'wrongBook') {
    studyMode.value = 'wrongBook'
    todayCompleted.value = false
    currentIndex.value = 0
    finishNum.value = 0
    // 保留 finishedList：今日已背单词是今天背过的所有单词（含正常计划 + 错题本重背），
    // submitReview 会按 id 去重 push，不会重复
    isReviewMode.value = true
    await loadWrongBookWords()
    if (wordList.value.length === 0) {
      ElMessage.warning('错题本是空的，去背单词积累错题吧')
      currentWord.value = null
    } else {
      refreshCurrentWord()
    }
    saveProgress()
    // 用完即清 query，避免刷新页面重复触发
    router.replace({ path: route.path })
    return
  }

  // 切走再回来恢复错题本模式（studyMode 持久化恢复，没背完就重新加载错题本单词）
  if (studyMode.value === 'wrongBook' && !todayCompleted.value) {
    await loadWrongBookWords()
    if (currentIndex.value >= wordList.value.length) {
      currentWord.value = null
      manualMarked.value = false
    } else {
      refreshCurrentWord()
    }
    return
  }

  // 检测计划每日词数是否变更
  // - 增加 + 今日已背完 → 重置完成状态，补拉新词继续背
  // - 减少 → 保持原页面不动（已背完就保持背完，没背完就保持当前进度）
  const oldLimit = Number(localStorage.getItem(planLimitKey()) || 0)
  const newLimit = planInfo.dailyLimit || 0
  if (newLimit > oldLimit && todayCompleted.value) {
    todayCompleted.value = false
    currentIndex.value = 0
    finishNum.value = 0
    finishedList.value = []
    isReviewMode.value = false
  }
  localStorage.setItem(planLimitKey(), String(newLimit))

  // 今日已背完：直接保持背完状态，不再拉新列表
  if (todayCompleted.value) {
    currentWord.value = null
    manualMarked.value = false
    return
  }

  // 正常计划模式
  const res = await getTodayWordList({ limit: planInfo.dailyLimit || 30 })
  if (res.code === 200) {
    wordList.value = res.data || []
    if (finishNum.value > 0 && currentIndex.value >= wordList.value.length) {
      // 保持背完状态
      currentWord.value = null
      manualMarked.value = false
    } else if (currentIndex.value >= wordList.value.length) {
      // 第二天或首次进入：重置
      currentIndex.value = 0
      finishNum.value = 0
      finishedList.value = []
      if (wordList.value.length > 0) {
        refreshCurrentWord()
      } else {
        currentWord.value = null
        manualMarked.value = false
      }
    } else {
      // 恢复到中间位置
      refreshCurrentWord()
    }
  }
}

/**
 * 把指定 wordId 的单词定位到当前光标。
 * 如果该单词在今日列表中 → 直接跳到那个位置
 * 如果不在 → 调接口拿详情，插入到列表头部，currentIndex=0
 */
const focusWord = async (wordId) => {
  const idx = wordList.value.findIndex(w => w.id === wordId)
  if (idx >= 0) {
    currentIndex.value = idx
    refreshCurrentWord()
    ElMessage.success(`已定位到单词：${currentWord.value?.word}`)
    return
  }
  try {
    const res = await getWordDetail(wordId)
    if (res.code === 200 && res.data) {
      wordList.value.unshift(res.data)
      currentIndex.value = 0
      refreshCurrentWord()
      ElMessage.success(`已加入今日列表：${res.data.word}`)
    } else {
      refreshCurrentWord()
    }
  } catch {
    refreshCurrentWord()
  }
}

const refreshCurrentWord = () => {
  if (currentIndex.value < wordList.value.length) {
    currentWord.value = wordList.value[currentIndex.value]
    // 检查当前单词是否已在错题本，决定星星按钮初始高亮状态
    // 不管 reason 是 vague/forget/manual，只要在错题本里就亮
    checkWrongWordStatus(currentWord.value.id)
  } else {
    currentWord.value = null
    manualMarked.value = false
  }
}

// 检查当前单词是否已在错题本，更新 manualMarked 状态
const checkWrongWordStatus = async (wordId) => {
  if (!wordId) {
    manualMarked.value = false
    return
  }
  try {
    const res = await checkWrongWord(wordId)
    if (res.code === 200) {
      manualMarked.value = res.data.inWrong === true
    }
  } catch (e) {
    // 接口失败默认不亮
    manualMarked.value = false
  }
}

const playAudio = () => {
  if (audioRef.value) audioRef.value.play()
}

const submitReview = async (memoryResult) => {
  if (!currentWord.value) return
  const word = currentWord.value

  // 先推进 UI（进度条 + 切换单词），避免后端接口抖动导致进度条卡死
  finishNum.value += 1
  // 把当前单词记录到今日已背列表（用于背完后回顾）
  // 按单词 id 去重：如果之前背过（比如复习模式再背一次），只更新记忆结果
  const existIdx = finishedList.value.findIndex(w => w.id === word.id)
  if (existIdx >= 0) {
    finishedList.value[existIdx].memoryResult = memoryResult
  } else {
    finishedList.value.push({ ...word, memoryResult })
  }
  currentIndex.value += 1
  refreshCurrentWord()
  // 背完本轮全部单词（currentIndex 走到列表末尾）→ 标记今日已完成
  if (currentIndex.value >= wordList.value.length) {
    todayCompleted.value = true
  }
  // 持久化进度，切到其他页面再回来不会重置
  saveProgress()

  // 模糊 / 遗忘 → 先收集到本轮 wrongSet（同步，不依赖网络）
  if (memoryResult === 2 || memoryResult === 3) {
    if (!wrongSet.value.find(w => w.id === word.id)) {
      wrongSet.value.push(word)
      saveWrongSet()
    }
  }

  // 再异步提交后端，失败不影响 UI 流程
  try {
    await submitWordReview({ wordId: word.id, memoryResult })
    ElMessage.success('状态已记录')
    // 模糊 / 遗忘 → 自动加入错题本（后端 upsertWrongWord 幂等，重复加不会报错）
    // 星星状态由 refreshCurrentWord 的 checkWrongWordStatus 自动维护：下次遇到该单词会亮
    if (memoryResult === 2 || memoryResult === 3) {
      const reason = memoryResult === 2 ? 'vague' : 'forget'
      try {
        await addWrongWord(word.id, reason)
      } catch (e) {
        console.error('加入错题本失败', e)
      }
    }
  } catch (e) {
    // axios 拦截器已经弹过错误提示，这里不再重复
    console.error('submitReview 失败', e)
  }
}

const startReviewWrong = () => {
  if (wrongSet.value.length === 0) return
  // 把本轮模糊/遗忘单词作为新的待背列表
  wordList.value = wrongSet.value.slice()
  // 清空 wrongSet，便于复习模式下重新收集（如果复习后仍有遗忘，可继续再过一遍）
  wrongSet.value = []
  saveWrongSet() // 同步清掉 storage，避免下次进页面又看到旧的"再过一遍"
  // 重置进度（复习模式从头开始），但保留 finishedList（今天背过的所有单词）
  currentIndex.value = 0
  finishNum.value = 0
  isReviewMode.value = true
  manualMarked.value = false
  todayCompleted.value = false // 复习模式重新开始，清除完成状态
  saveProgress()
  refreshCurrentWord()
}

// 星星按钮 toggle 行为：
// - 当前不亮 → 加入错题本（reason='manual'）→ 变亮
// - 当前亮 → 从错题本删除（按 wordId）→ 变不亮
const addToWrongBookManual = async () => {
  if (!currentWord.value) return
  const wordId = currentWord.value.id
  if (manualMarked.value) {
    // 当前已亮 → 取消，从错题本删除
    try {
      await removeWrongWordByWordId(wordId)
      manualMarked.value = false
      ElMessage.success('已从错题本移除')
    } catch (e) {
      ElMessage.error('移除失败')
    }
  } else {
    // 当前不亮 → 加入错题本
    try {
      await addWrongWord(wordId, 'manual')
      manualMarked.value = true
      ElMessage.success('已加入错题本')
    } catch (e) {
      ElMessage.error('加入失败')
    }
  }
}

const initChart = async () => {
  try {
    const res = await getWordStat()
    await nextTick()
    if (chartInstance) chartInstance.dispose()
    chartInstance = echarts.init(chartRef.value)
    chartInstance.setOption({
      tooltip: {},
      xAxis: { type: 'category', data: ['认识', '模糊', '遗忘'] },
      yAxis: { type: 'value' },
      series: [{
        data: [res.data.know ?? 0, res.data.vague ?? 0, res.data.forget ?? 0],
        type: 'bar'
      }]
    })
  } catch (err) {
    console.error('图表初始化异常', err)
  }
}

const handleResize = () => chartInstance?.resize()

onMounted(async () => {
  await loadPlanInfo()       // 先加载计划信息，拿到 dailyLimit + hasPlan
  if (planInfo.hasPlan) {
    await loadWordList()     // 有计划才加载今日单词
    initChart()
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  chartInstance?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.word-study-container{
  padding:20px;
  display:flex;
  flex-direction:column;
  gap:20px;
}
.progress-header{
  display:flex;
  justify-content:space-between;
  align-items:center;
  margin-bottom:12px;
}
.word-top{
  display:flex;
  align-items:center;
  gap:16px;
}
.word-text{
  font-size:36px;
  font-weight:bold;
  margin:0;
}
.item{
  font-size:16px;
  margin:10px 0;
  line-height:1.8;
}
.btn-group{
  margin-top:30px;
  display:flex;
  gap:20px;
  justify-content:center;
}
.review-entry{
  display:flex;
  flex-direction:column;
  align-items:center;
  gap:16px;
}
.review-entry p{ margin:0; }
</style>
