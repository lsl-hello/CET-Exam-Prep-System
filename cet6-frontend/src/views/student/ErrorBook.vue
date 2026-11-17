<template>
  <div class="error-page">
    <el-card>
      <template #header><span>我的错题本</span></template>

      <el-row :gutter="20">
        <!-- 左栏：题目错题本 -->
        <el-col :xs="24" :lg="12">
          <div class="col-block">
            <div class="col-header">
              <h3>题目错题本</h3>
              <el-tag type="info" size="small">共 {{ total }} 题</el-tag>
            </div>

            <el-table :data="errorList" border stripe size="small">
              <el-table-column label="题干" prop="title" min-width="200" show-overflow-tooltip />
              <el-table-column label="题型" width="80">
                <template #default="scope">
                  <el-tag :type="errorTypeTag(scope.row.questionType)" size="small">{{ scope.row.typeName || '—' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="正确答案" prop="standardAnswer" width="110" show-overflow-tooltip />
              <el-table-column label="我的作答" prop="userAnswer" width="110" show-overflow-tooltip />
              <el-table-column label="操作" width="160" fixed="right">
                <template #default="scope">
                  <el-button size="small" link type="primary" @click="reviewQ(scope.row.questionId)">重做</el-button>
                  <el-button size="small" link type="success" @click="handleMarkMaster(scope.row.id)">已掌握</el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-pagination
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              :total="total"
              layout="total, prev, pager, next"
              @current-change="loadList"
              style="margin-top:12px"
              small
            />
          </div>
        </el-col>

        <!-- 右栏：单词错题本 -->
        <el-col :xs="24" :lg="12">
          <div class="col-block">
            <div class="col-header">
              <h3>单词错题本</h3>
              <el-tag type="info" size="small">共 {{ wrongTotal }} 个</el-tag>
            </div>

            <div class="word-filter">
              <el-radio-group v-model="reasonFilter" size="small" @change="loadWrongWords">
                <el-radio-button label="">全部</el-radio-button>
                <el-radio-button label="vague">模糊</el-radio-button>
                <el-radio-button label="forget">遗忘</el-radio-button>
                <el-radio-button label="manual">手动</el-radio-button>
              </el-radio-group>
            </div>

            <el-table :data="wrongWordList" border stripe size="small">
              <el-table-column label="单词" prop="word" width="110" />
              <el-table-column label="释义" prop="translate" min-width="160" show-overflow-tooltip />
              <el-table-column label="来源" width="80">
                <template #default="scope">
                  <el-tag :type="reasonTagType(scope.row.reason)" size="small">
                    {{ reasonLabel(scope.row.reason) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="170" fixed="right">
                <template #default="scope">
                  <el-button size="small" link @click="playWrongAudio(scope.row)">发音</el-button>
                  <el-button size="small" link type="primary" @click="restudyWrong">重背</el-button>
                  <el-button size="small" link type="danger" @click="removeWrong(scope.row.id)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-pagination
              v-model:current-page="wrongPageNum"
              v-model:page-size="wrongPageSize"
              :total="wrongTotal"
              layout="total, prev, pager, next"
              @current-change="loadWrongWords"
              style="margin-top:12px"
              small
            />
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 用于播放发音的隐藏 audio -->
    <audio ref="wrongAudioRef" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getErrorList, markMaster, getWrongWordList, removeWrongWord } from '@/api/student'

const router = useRouter()

// ===== 题目错题本 =====
const errorList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadList = async () => {
  try {
    const res = await getErrorList({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) {
      errorList.value = res.data.records
      total.value = res.data.total
    }
  } catch (e) {
    errorList.value = []
    total.value = 0
  }
}
const reviewQ = (id) => {
  router.push({ path: '/question-practice', query: { qId: id } })
}
const errorTypeTag = t => ({ 1: 'success', 2: '', 3: 'warning', 4: 'danger' }[t] || 'info')
const handleMarkMaster = async (id) => {
  await markMaster(id)
  ElMessage.success('已标记掌握')
  loadList()
}

// ===== 单词错题本 =====
const wrongWordList = ref([])
const wrongPageNum = ref(1)
const wrongPageSize = ref(10)
const wrongTotal = ref(0)
const reasonFilter = ref('')
const wrongAudioRef = ref(null)

const loadWrongWords = async () => {
  try {
    const params = {
      pageNum: wrongPageNum.value,
      pageSize: wrongPageSize.value
    }
    // 把 reason 过滤传给后端，让后端按 reason 过滤 + 返回正确的 total
    if (reasonFilter.value) {
      params.reason = reasonFilter.value
    }
    const res = await getWrongWordList(params)
    if (res.code === 200) {
      wrongWordList.value = res.data.list || []
      wrongTotal.value = res.data.total || 0
    }
  } catch (e) {
    // 表不存在 / 接口异常时，不阻塞页面，显示空状态
    wrongWordList.value = []
    wrongTotal.value = 0
  }
}

const reasonLabel = (r) => ({ vague: '模糊', forget: '遗忘', manual: '手动' }[r] || r)
const reasonTagType = (r) => ({ vague: 'warning', forget: 'danger', manual: 'info' }[r] || 'info')

const playWrongAudio = (row) => {
  if (!row.audioUrl) {
    ElMessage.warning('该单词暂无音频')
    return
  }
  wrongAudioRef.value.src = row.audioUrl
  wrongAudioRef.value.play().catch(() => ElMessage.error('音频播放失败'))
}

const restudyWrong = () => {
  // 重背整个错题本：只背错题本里的单词，不混入计划新词
  router.push({ path: '/word-study', query: { mode: 'wrongBook' } })
  ElMessage.success('已跳转，将重背错题本所有单词')
}

const removeWrong = async (id) => {
  try {
    await ElMessageBox.confirm('确认从错题本移除该单词？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await removeWrongWord(id)
  ElMessage.success('已移除')
  loadWrongWords()
}

onMounted(() => {
  // 左右两栏同时显示，并行加载
  loadList()
  loadWrongWords()
})
</script>

<style scoped>
.error-page { padding:20px; }
.col-block {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  height: 100%;
}
.col-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.col-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}
.word-filter { margin-bottom: 12px; }

/* 大屏左右分栏，小屏自动堆叠 */
@media (max-width: 1199px) {
  .col-block { margin-bottom: 16px; }
}
</style>
