<template>
  <div class="admin-wordbook">
    <el-card>
      <template #header>
        <div class="header-row">
          <span>词书管理</span>
          <el-button type="primary" @click="openAdd">新增词书</el-button>
        </div>
      </template>
      <el-table :data="bookList" border>
        <el-table-column label="词书名称" prop="bookName" />
        <el-table-column label="考试等级" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.level === 4 ? 'warning' : ''" size="small">
              {{ scope.row.level === 4 ? '四级' : '六级' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="简介" prop="bookDesc" />
        <el-table-column label="排序值" prop="sort" />
        <el-table-column label="状态">
          <template #default="scope">
            <el-tag :type="scope.row.status===1?'success':'danger'">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" @click="editBook(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="delBook(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialogVisible" title="词书编辑" width="500px">
      <el-form :model="form">
        <el-form-item label="词书名称">
          <el-input v-model="form.bookName" />
        </el-form-item>
        <el-form-item label="考试等级">
          <el-radio-group v-model="form.level">
            <el-radio :label="4">四级</el-radio>
            <el-radio :label="6">六级</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.bookDesc" type="textarea" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="saveBook">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, toRaw } from 'vue'
import { ElMessage } from 'element-plus'
// 接口别名规避重名风险
import { getWordBookList, saveWordBook as apiSaveBook, deleteWordBook } from '@/api/admin'

const bookList = ref([])
const dialogVisible = ref(false)
const form = reactive({ id:null, bookName:'', bookDesc:'', level:6, sort:0, status:1 })

const loadList = async () => {
  const res = await getWordBookList()
  if(res.code === 200){
    // 兼容MyBatis‑Plus Page分页对象，取records数组
    if(res.data?.records){
      bookList.value = res.data.records
    }else{
      bookList.value = res.data
    }
  }
}
const editBook = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}
// 新增时重置表单，避免残留上次编辑的 id 被当成修改
const openAdd = () => {
  Object.assign(form, { id:null, bookName:'', bookDesc:'', level:6, sort:0, status:1 })
  dialogVisible.value = true
}
// 本地函数名和导入接口区分开
const saveBook = async () => {
  await apiSaveBook(toRaw(form))
  dialogVisible.value = false
  ElMessage.success('保存成功')
  loadList()
}
const delBook = async (id) => {
  await deleteWordBook(id)
  ElMessage.success('删除完成')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.admin-wordbook { padding:20px; }
.header-row { display:flex; justify-content:space-between; align-items:center; }
</style>