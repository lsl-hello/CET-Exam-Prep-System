<template>
  <div class="admin-question">
    <el-card>
      <template #header>
        <div class="header-row">
          <span>试题管理</span>
          <div class="btn-group">
            <el-button type="primary" @click="openAdd">新增试题</el-button>
            <el-upload ref="uploadRef" :http-request="uploadExcel" accept=".xlsx,.xls" show-file-list>
              <el-button type="success">Excel批量导入试题</el-button>
            </el-upload>
          </div>
        </div>
      </template>
      <!-- 查询栏 -->
      <el-row :gutter="16" style="margin-bottom:16px">
        <el-col :span="6">
          <el-select v-model="query.paperId" placeholder="选择试卷" clearable @change="loadList">
            <el-option v-for="p in paperOpts" :key="p.id" :label="p.paperName" :value="p.id" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-input v-model="query.keyword" placeholder="搜索题干" @keyup.enter="loadList" />
        </el-col>
      </el-row>
      <el-table :data="qList" border>
        <el-table-column label="题干" prop="title" show-overflow-tooltip />
        <el-table-column label="题型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.questionType)" size="small">{{ typeLabel(row.questionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属试卷" prop="paperName" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" @click="editQ(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="delQ(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        @current-change="loadList"
        @size-change="loadList"
        style="margin-top:16px"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogAdd" title="试题编辑" width="650px" @close="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="题型">
          <el-select v-model="form.questionType" @change="onTypeChange">
            <el-option label="听力" :value="1" />
            <el-option label="阅读" :value="2" />
            <el-option label="翻译" :value="3" />
            <el-option label="写作" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属试卷">
          <el-select v-model="form.paperId">
            <el-option v-for="p in paperOpts" :key="p.id" :label="p.paperName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干">
          <el-input v-model="form.title" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item v-if="form.questionType === 1 || form.questionType === 2" label="选项（每行一个）">
          <el-input v-model="form.options" type="textarea" :rows="5" placeholder="每行一个选项，例如：&#10;A. 选项内容&#10;B. 选项内容" />
        </el-form-item>
        <el-form-item label="标准答案">
          <el-input v-model="form.standardAnswer" :placeholder="form.questionType === 1 || form.questionType === 2 ? '填写选项字母，如 A' : '参考答案文本'" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item v-if="form.questionType === 1" label="听力音频地址">
          <el-input v-model="form.audioUrl" placeholder="听力题音频 URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogAdd = false">取消</el-button>
        <el-button type="primary" @click="handleSaveQ">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, toRaw } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminQuestionList, saveQuestion as apiSaveQ, deleteQuestion, importQuestions, getAdminPaperList } from '@/api/admin'

const qList = ref([])
const paperOpts = ref([])
const uploadRef = ref(null)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogAdd = ref(false)

const query = reactive({ paperId: null, keyword: '' })

// 表单默认值，和数据库tinyint匹配
const defaultForm = {
  id: null,
  paperId: null,
  questionType: 2,
  subType: 201, // 数据库 sub_type 非空，必须给默认值；阅读=201
  difficult: 1,
  sort: 1,
  title: '',
  options: '',
  standardAnswer: '',
  analysis: '',
  audioUrl: '',
  status: 1 // 默认启用，学生端 status=1 过滤才能查到
}
const form = reactive({ ...defaultForm })

// 题型中文标签 + tag 颜色
const typeLabel = t => ({ 1: '听力', 2: '阅读', 3: '翻译', 4: '写作' }[t] || '未知')
const typeTag = t => ({ 1: 'success', 2: '', 3: 'warning', 4: 'danger' }[t] || 'info')
// 题型 → 子题型默认值（DB sub_type 非空，必须随题型联动设值）
const defaultSubType = t => ({ 1: 101, 2: 201, 3: 301, 4: 401 }[t] || 201)

// 题型切换时清理无关字段 + 联动子题型：翻译/写作清空选项，非听力清空音频
const onTypeChange = () => {
  form.subType = defaultSubType(form.questionType)
  if (form.questionType !== 1 && form.questionType !== 2) {
    form.options = ''
  }
  if (form.questionType !== 1) {
    form.audioUrl = ''
  }
}

// 重置表单（弹窗关闭、新增时调用）
const resetForm = () => {
  Object.assign(form, defaultForm)
}
// 打开新增
const openAdd = () => {
  resetForm()
  dialogAdd.value = true
}

// 加载试卷下拉（来源就是"试卷管理"里的试卷列表）
const loadPaperOpts = async () => {
  const res = await getAdminPaperList()
  if (res.code === 200) {
    // 后端返回 MyBatis-Plus 分页对象 {records, total, ...}，取 records 数组
    paperOpts.value = res.data?.records || res.data || []
  }
}

// 试题列表
const loadList = async () => {
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    ...query
  }
  const res = await getAdminQuestionList(params)
  if (res.code === 200) {
    qList.value = res.data.records
    total.value = res.data.total
  }
}

// 编辑回显
const editQ = (row) => {
  Object.assign(form, row)
  // options 后端存的是 JSON 数组字符串（如 '["A. xxx","B. yyy"]'）
  // 解析成换行分隔的可读字符串放进 textarea，方便编辑
  const opts = row.options
  if (Array.isArray(opts)) {
    form.options = opts.join('\n')
  } else if (typeof opts === 'string' && opts.trim()) {
    try {
      const parsed = JSON.parse(opts)
      form.options = Array.isArray(parsed) ? parsed.join('\n') : opts
    } catch (e) {
      // 不是 JSON（可能是旧的逗号分隔格式），按逗号拆成换行
      form.options = opts.split(/[,，]/).map(s => s.trim()).filter(Boolean).join('\n')
    }
  } else {
    form.options = ''
  }
  dialogAdd.value = true
}

// 保存：前端 textarea 字符串 → JSON 数组字符串传给后端
// 后端 Cet6Question.options 是 String 字段，存 JSON 数组字符串
const handleSaveQ = async () => {
  const submitData = toRaw(form)
  // 必填校验：所属试卷、题干不能为空；sub_type 数据库非空，兜底补默认值
  if (!submitData.paperId) {
    ElMessage.warning('请先选择所属试卷')
    return
  }
  if (!submitData.title || !submitData.title.trim()) {
    ElMessage.warning('题干不能为空')
    return
  }
  if (submitData.subType == null) {
    submitData.subType = defaultSubType(submitData.questionType)
  }
  // 统一处理 options：兼容数组/字符串/JSON 字符串，最终序列化成 JSON 字符串
  let opts = submitData.options
  if (opts == null) {
    opts = []
  } else if (Array.isArray(opts)) {
    // 已经是数组（理论不会走到这，但兜底）
  } else if (typeof opts === 'string') {
    const trimmed = opts.trim()
    if (trimmed === '') {
      opts = []
    } else if (trimmed.startsWith('[')) {
      // 用户直接粘贴了 JSON 数组字符串，尝试解析
      try {
        const parsed = JSON.parse(trimmed)
        opts = Array.isArray(parsed) ? parsed : []
      } catch (e) {
        opts = []
      }
    } else {
      // 普通文本：按换行或中英文逗号分隔
      opts = trimmed.split(/[\n,，]/).map(s => s.trim()).filter(Boolean)
    }
  } else {
    opts = []
  }
  submitData.options = JSON.stringify(opts)
  // 兜底 status：编辑时如果 row.status 为 null，也补成 1
  if (submitData.status == null) submitData.status = 1
  await apiSaveQ(submitData)
  dialogAdd.value = false
  ElMessage.success('保存成功')
  loadList()
}

const delQ = async (id) => {
  await deleteQuestion(id)
  ElMessage.success('删除完成')
  loadList()
}

// Excel导入
const uploadExcel = async (opt) => {
  const file = opt.file
  const paperId = query.paperId
  // 必须先选试卷：后端 @RequestParam Long paperId 收到字符串 "null" 会转换失败
  if (!paperId) {
    ElMessage.warning('请先在查询栏选择试卷，再导入试题')
    uploadRef.value && uploadRef.value.clearFiles()
    return
  }
  const res = await importQuestions(file, paperId)
  if (res.code === 200) {
    ElMessage.success(`导入成功 ${res.data.count ?? res.data.successNum ?? 0} 条`)
    uploadRef.value.clearFiles()
    loadList()
  }
}

onMounted(() => {
  loadPaperOpts()
  loadList()
})
</script>

<style scoped>
.admin-question {
  padding: 20px;
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.btn-group {
  display: flex;
  gap: 12px;
}
</style>