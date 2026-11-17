<template>
  <div class="admin-notice">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>系统公告管理</span>
          <el-button type="primary" @click="dialogVisible = true">新增公告</el-button>
        </div>
      </template>
      <el-table :data="noticeList" border stripe>
        <el-table-column label="公告标题" prop="title" />
        <el-table-column label="公告内容" prop="content" show-overflow-tooltip />
        <el-table-column label="发布时间" prop="createTime" />
        <el-table-column label="状态">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" @click="editNotice(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="delNotice(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="公告编辑" width="600px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="公告标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="公告内容">
          <el-input v-model="form.content" type="textarea" rows="6" />
        </el-form-item>
        <el-form-item label="发布状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">直接发布</el-radio>
            <el-radio :label="0">保存草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveNotice">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, toRaw } from 'vue'
import { ElMessage } from 'element-plus'
import { getNoticeList, saveNotice as apiSaveNotice, deleteNotice } from '@/api/admin'

const noticeList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const query = reactive({ title: '' })
// 默认status=1直接发布
const form = reactive({ id: null, title: '', content: '', status: 1 })

const loadList = async () => {
  // 关键修复 .value取出数字，禁止直接传ref对象
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    ...query
  }
  const res = await getNoticeList(params)
  if(res.code === 200){
    noticeList.value = res.data.records
    total.value = res.data.total
  }
}

const editNotice = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSaveNotice = async () => {
  await apiSaveNotice(toRaw(form))
  dialogVisible.value = false
  ElMessage.success('保存成功')
  loadList()
}

const delNotice = async (id) => {
  await deleteNotice(id)
  ElMessage.success('删除完成')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.admin-notice {
  padding: 20px;
}
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>