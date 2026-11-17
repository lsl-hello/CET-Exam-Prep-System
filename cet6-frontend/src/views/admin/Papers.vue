<template>
  <div class="admin-paper">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>试卷管理</span>
          <el-button type="primary" @click="openAdd">新增试卷</el-button>
        </div>
      </template>
      <el-table :data="paperList" border stripe>
        <el-table-column label="试卷名称" prop="paperName" />
        <el-table-column label="考试等级" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.level === 4 ? 'warning' : ''" size="small">
              {{ scope.row.level === 4 ? '四级' : '六级' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="考试时间" width="130" align="center">
          <template #default="scope">
            {{ scope.row.paperYear }}年{{ scope.row.paperMonth }}月
          </template>
        </el-table-column>
        <el-table-column label="总题数" prop="questionCount" align="center" width="90">
          <template #default="scope">
            <el-tag type="info" size="small">{{ scope.row.questionCount ?? 0 }} 题</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" @click="editPaper(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="delPaper(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑试卷' : '新增试卷'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="试卷名称">
          <el-input v-model="form.paperName" />
        </el-form-item>
        <el-form-item label="考试等级">
          <el-radio-group v-model="form.level">
            <el-radio :label="4">四级</el-radio>
            <el-radio :label="6">六级</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="考试年份">
          <el-input-number v-model="form.paperYear" :min="2000" :max="2100" />
        </el-form-item>
        <el-form-item label="考试月份">
          <el-select v-model="form.paperMonth" style="width: 120px">
            <el-option label="6月" :value="6" />
            <el-option label="12月" :value="12" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePaper">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, toRaw } from 'vue'
import { ElMessage } from 'element-plus'
// 接口别名防止命名冲突
import { getAdminPaperList, savePaper as apiSavePaper, deletePaper } from '@/api/admin'

const paperList = ref([])
const dialogVisible = ref(false)
const defaultForm = {
  id: null,
  paperName: '',
  paperYear: new Date().getFullYear(),
  paperMonth: 12,
  level: 6,
  status: 1
}
const form = reactive({ ...defaultForm })

// 新增：先重置表单（清掉 id），否则会复用上次编辑的 id 变成"修改"
const openAdd = () => {
  Object.assign(form, defaultForm)
  dialogVisible.value = true
}

const loadList = async () => {
  const res = await getAdminPaperList()
  if (res.code === 200) {
    // 兼容MyBatis‑Plus Page分页对象，取records数组
    if(res.data?.records){
      paperList.value = res.data.records
    }else{
      paperList.value = res.data
    }
  }
}

const editPaper = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSavePaper = async () => {
  await apiSavePaper(toRaw(form))
  dialogVisible.value = false
  ElMessage.success('保存成功')
  loadList()
}

const delPaper = async (id) => {
  await deletePaper(id)
  ElMessage.success('删除完成')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.admin-paper {
  padding: 20px;
}
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>