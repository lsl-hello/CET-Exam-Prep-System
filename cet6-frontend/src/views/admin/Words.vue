<template>
  <div class="admin-word">
    <el-card>
      <template #header>
        <div class="header-row">
          <span>单词管理</span>
          <div class="btn-group">
            <el-button type="primary" @click="dialogAdd=true">新增单词</el-button>
            <el-button type="warning" @click="openBatchDialog">批量文本导入</el-button>
            <el-upload
              ref="uploadRef"
              :http-request="uploadExcel"
              show-file-list
              accept=".xlsx,.xls"
            >
              <el-button type="success">Excel批量导入</el-button>
            </el-upload>
          </div>
        </div>
      </template>
      <!-- 查询栏 -->
      <el-row :gutter="16" style="margin-bottom:16px">
        <el-col :span="6">
          <el-select v-model="query.bookId" placeholder="选择词书" clearable @change="loadList">
            <el-option v-for="b in bookOpts" :key="b.id" :label="b.bookName" :value="b.id" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-input v-model="query.word" placeholder="搜索单词" @keyup.enter="loadList" />
        </el-col>
      </el-row>
      <el-table :data="wordList" border>
        <el-table-column label="单词" prop="word" />
        <el-table-column label="音标" prop="phonetic" />
        <el-table-column label="词性" prop="speech" />
        <el-table-column label="释义" prop="translate" />
        <el-table-column label="所属词书" prop="bookId" :formatter="formatBookName" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" @click="editWord(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="delWord(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        @current-change="loadList"
        @size-change="loadList"
      />
    </el-card>
    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogAdd" title="单词编辑" width="600px">
      <el-form :model="form">
        <el-form-item label="所属词书">
          <el-select v-model="form.bookId">
            <el-option v-for="b in bookOpts" :key="b.id" :label="b.bookName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单词">
          <el-input v-model="form.word" />
        </el-form-item>
        <el-form-item label="音标">
          <el-input v-model="form.phonetic" />
        </el-form-item>
        <el-form-item label="词性">
          <el-input v-model="form.speech" />
        </el-form-item>
        <el-form-item label="释义">
          <el-input v-model="form.translate" type="textarea" />
        </el-form-item>
        <el-form-item label="例句">
          <el-input v-model="form.sentence" type="textarea" />
        </el-form-item>
        <el-form-item label="音频地址">
          <el-input v-model="form.audioUrl" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogAdd=false">取消</el-button>
        <el-button type="primary" @click="handleSaveWord">保存</el-button>
      </template>
    </el-dialog>
    <!-- 批量文本导入弹窗 -->
    <el-dialog v-model="dialogBatch" title="批量文本导入" width="800px" :close-on-click-modal="false">
      <el-alert type="info" :closable="false" style="margin-bottom:12px">
        <template #title>
          <div style="line-height:1.6">
            每行一个单词，支持 4 种分隔符：<b>|</b>、Tab（制表符）、<b>,</b>、<b>，</b>（中文逗号）<br>
            字段顺序：<b>单词 | 音标 | 词性 | 释义 | 例句</b>（例句可省略，至少要有单词）<br>
            示例：<code>ambitious|/æmˈbɪʃəs/|adj.|有雄心的；有抱负的|He is an ambitious young man.</code><br>
            <b>从 PDF 复制</b>：在 PDF 阅读器里选中文字→复制→粘贴到下面文本框，自动按行解析
          </div>
        </template>
      </el-alert>
      <el-form :model="batchForm">
        <el-form-item label="所属词书">
          <el-select v-model="batchForm.bookId" placeholder="选择词书" style="width:300px">
            <el-option v-for="b in bookOpts" :key="b.id" :label="b.bookName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单词文本">
          <el-input
            v-model="batchForm.text"
            type="textarea"
            :rows="14"
            placeholder="在此粘贴单词文本，每行一个，例如：&#10;ambitious|/æmˈbɪʃəs/|adj.|有雄心的；有抱负的&#10;substantial|/səbˈstænʃl/|adj.|大量的；实质的"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogBatch=false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="handleBatchImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, toRaw } from 'vue'
import { ElMessage } from 'element-plus'
// 接口起别名，防止和本地保存函数重名
import { getWordList, saveWord as apiSaveWord, deleteWord, importWords, batchTextWords, getWordBookList } from '@/api/admin'

const wordList = ref([])
const bookOpts = ref([])
const uploadRef = ref(null)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogAdd = ref(false)
const query = reactive({ bookId: null, word: '' })
const form = reactive({
  id: null, bookId: '', word:'', phonetic:'', speech:'', translate:'', sentence:'', audioUrl:'', sort:0
})

const loadBookOpt = async () => {
  const res = await getWordBookList()
  if(res.code === 200) bookOpts.value = res.data.records || res.data || []
}

// 表格"所属词书"列：根据 row.bookId 在 bookOpts 里查 bookName 显示
// 不依赖后端 join 返回，前端自己关联
const formatBookName = (row) => {
  if (row.bookId == null) return '未关联'
  const b = bookOpts.value.find(x => x.id === row.bookId)
  return b ? b.bookName : '加载中...'
}
const loadList = async () => {
  // 修复分页：ref变量必须 .value 解包
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    ...query
  }
  const res = await getWordList(params)
  if(res.code === 200){
    wordList.value = res.data.records
    total.value = res.data.total
  }
}
const editWord = (row) => {
  Object.assign(form, row)
  dialogAdd.value = true
}
// 调用别名接口 apiSaveWord
const handleSaveWord = async () => {
  await apiSaveWord(toRaw(form))
  dialogAdd.value = false
  ElMessage.success('保存成功')
  loadList()
}
const delWord = async (id) => {
  await deleteWord(id)
  ElMessage.success('删除完成')
  loadList()
}
// Excel导入
const uploadExcel = async (opt) => {
  const file = opt.file
  const bookId = form.bookId
  const res = await importWords(file, bookId)
  if(res.code === 200){
    ElMessage.success(`导入成功${res.data.successNum}条，失败${res.data.failNum}条`)
    uploadRef.value.clearFiles()
    loadList()
  }
}

// 批量文本导入
const dialogBatch = ref(false)
const batchLoading = ref(false)
const batchForm = reactive({ bookId: null, text: '' })
const openBatchDialog = () => {
  batchForm.bookId = query.bookId || (bookOpts.value[0]?.id ?? null)
  batchForm.text = ''
  dialogBatch.value = true
}
const handleBatchImport = async () => {
  if (!batchForm.bookId) {
    ElMessage.warning('请选择词书')
    return
  }
  if (!batchForm.text.trim()) {
    ElMessage.warning('请粘贴单词文本')
    return
  }
  batchLoading.value = true
  try {
    const res = await batchTextWords(toRaw(batchForm))
    if (res.code === 200) {
      const { success, fail, failMsg } = res.data
      ElMessage.success(`导入成功 ${success} 条${fail ? '，失败 ' + fail + ' 条' + (failMsg ? '：' + failMsg : '') : ''}`)
      dialogBatch.value = false
      loadList()
    }
  } finally {
    batchLoading.value = false
  }
}

onMounted(()=>{
  loadBookOpt()
  loadList()
})
</script>

<style scoped>
.admin-word { padding:20px; }
.header-row { display:flex; justify-content:space-between; align-items:center; }
.btn-group { display:flex; gap:12px; }
</style>