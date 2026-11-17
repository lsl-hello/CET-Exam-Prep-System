<template>
  <div class="msg-page">
    <el-card>
      <template #header>站内学习提醒消息</template>
      <el-table :data="msgList" border>
        <el-table-column label="标题" prop="title" />
        <el-table-column label="内容" prop="content" />
        <el-table-column label="状态">
          <template #default="scope">
            <el-tag :type="scope.row.isRead ? '' : 'warning'">
              {{ scope.row.isRead ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button v-if="!scope.row.isRead" size="small" @click="readMsg(scope.row.id)">标记已读</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMessageList, readMessage } from '@/api/student'

const msgList = ref([])
const loadList = async () => {
  const res = await getMessageList()
  if (res.code === 200) {
    // 后端返回的是分页对象 {records, total, ...}，取 records 才是真正的消息列表
    msgList.value = res.data?.records || res.data || []
  }
}
const readMsg = async (id) => {
  await readMessage(id)
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.msg-page { padding:20px; }
</style>