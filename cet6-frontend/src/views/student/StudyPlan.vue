<template>
  <div class="plan-page">
    <el-card>
      <template #header>
        <div class="plan-header">
          <span>我的学习计划</span>
          <el-button type="primary" @click="openAddDialog">新建计划</el-button>
        </div>
      </template>
      <el-table :data="planList" border>
        <el-table-column label="计划名称" prop="planName" />
        <el-table-column label="类型">
          <template #default="scope">{{ scope.row.planType ===1 ? '背单词' : '刷题' }}</template>
        </el-table-column>
        <el-table-column label="关联内容">
          <template #default="scope">
            <span v-if="scope.row.planType === 1">{{ getBookName(scope.row.bookId) }}</span>
            <span v-else>{{ getPaperName(scope.row.paperId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="每日目标">
          <template #default="scope">
            <span v-if="scope.row.planType === 1">{{ scope.row.dailyTargetNum }} 词/天</span>
            <span v-else>整卷试题</span>
          </template>
        </el-table-column>
        <el-table-column label="提醒时间" prop="remindTime" />
        <el-table-column label="状态">
          <template #default="scope">
            <el-tag :type="scope.row.status===1?'success':'danger'">
              {{ scope.row.status===1 ? '启用' : '暂停' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" @click="editPlan(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="delPlan(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑计划' : '新建计划'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="计划名称">
          <el-input v-model="form.planName" placeholder="请输入计划名称" />
        </el-form-item>
        <el-form-item label="计划类型">
          <el-radio-group v-model="form.planType" @change="onTypeChange">
            <el-radio :label="1">背单词</el-radio>
            <el-radio :label="2">刷题</el-radio>
          </el-radio-group>
        </el-form-item>
        <!-- 背单词：选单词书 + 每日目标词数 -->
        <el-form-item v-if="form.planType === 1" label="单词书">
          <el-select v-model="form.bookId" placeholder="请选择单词书" style="width:100%">
            <el-option v-for="b in bookOpts" :key="b.id" :value="b.id" :label="bookLabel(b)" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.planType === 1" label="每日目标">
          <el-input-number v-model="form.dailyTargetNum" :min="1" />
          <span style="margin-left:8px;color:#909399">词/天</span>
        </el-form-item>
        <!-- 刷题：选择试题（关联试卷） -->
        <el-form-item v-if="form.planType === 2" label="选择试题">
          <el-select v-model="form.paperId" placeholder="请选择试卷" style="width:100%">
            <el-option v-for="p in paperOpts" :key="p.id" :value="p.id" :label="p.paperName" />
          </el-select>
        </el-form-item>
        <el-form-item label="提醒时间">
          <el-time-picker v-model="form.remindTime" format="HH:mm" value-format="HH:mm" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSavePlan">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getPlanList, savePlan, deletePlan, getWordBooks, getPaperList } from '@/api/student'

const planList = ref([])
const bookOpts = ref([])
const paperOpts = ref([])
const dialogVisible = ref(false)
const form = reactive({
  id: null,
  planName: '',
  planType: 1,
  dailyTargetNum: 30,
  remindTime: '20:00',
  bookId: null,
  paperId: null
})

const getBookName = (bookId) => {
  const b = bookOpts.value.find(b => b.id === bookId)
  return b ? b.bookName : '-'
}
// 词书下拉显示考试等级，便于区分四级/六级
const bookLabel = (b) => {
  const lv = b.level === 4 ? '四级' : (b.level === 6 ? '六级' : '')
  return lv ? `${b.bookName}（${lv}）` : b.bookName
}
const getPaperName = (paperId) => {
  const p = paperOpts.value.find(p => p.id === paperId)
  return p ? p.paperName : '-'
}

// 切换计划类型时清理对方字段，避免脏数据
const onTypeChange = () => {
  if (form.planType === 1) {
    form.paperId = null
  } else {
    form.bookId = null
  }
}

// 新建的时候重置表单
const openAddDialog = () => {
  form.id = null
  form.planName = ''
  form.planType = 1
  form.dailyTargetNum = 30
  form.remindTime = '20:00'
  form.bookId = null
  form.paperId = null
  dialogVisible.value = true
}

const loadList = async () => {
  const res = await getPlanList()
  if (res.code === 200) planList.value = res.data
}

const loadBooks = async () => {
  const res = await getWordBooks()
  if (res.code === 200) bookOpts.value = res.data || []
}

const loadPapers = async () => {
  const res = await getPaperList()
  if (res.code === 200) paperOpts.value = res.data || []
}

const handleSavePlan = async () => {
  if (!form.planName?.trim()) {
    return ElMessage.warning('请填写计划名称')
  }
  // 按类型校验关联内容
  if (form.planType === 1 && !form.bookId) {
    return ElMessage.warning('请选择单词书')
  }
  if (form.planType === 2 && !form.paperId) {
    return ElMessage.warning('请选择试卷')
  }
  await savePlan(form)
  dialogVisible.value = false
  ElMessage.success('保存成功')
  loadList()
}

const editPlan = (row) => {
  Object.assign(form, row)
  // 兜底：编辑时补全 paperId 字段（旧数据可能没有）
  if (form.paperId == null) form.paperId = null
  dialogVisible.value = true
}

const delPlan = async (id) => {
  await deletePlan(id)
  ElMessage.success('删除完成')
  loadList()
}

onMounted(() => {
  loadList()
  loadBooks()
  loadPapers()
})
</script>

<style scoped>
.plan-page { padding:20px; }
.plan-header { display:flex; justify-content:space-between; align-items:center; }
</style>
