<template>
  <div class="admin-log">
    <el-card>
      <template #header><span>后台操作日志</span></template>
      <!-- 查询栏 -->
      <el-row :gutter="16" style="margin-bottom:16px">
        <el-col :span="6">
          <el-input v-model="query.username" placeholder="操作人账号" clearable @keyup.enter="loadList" />
        </el-col>
        <el-col :span="6">
          <el-input v-model="query.operation" placeholder="操作内容" clearable @keyup.enter="loadList" />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="loadList">查询</el-button>
        </el-col>
      </el-row>
      <el-table :data="logList" border>
        <el-table-column label="ID" prop="id" width="80"/>
        <el-table-column label="操作人ID" prop="adminId"/>
        <el-table-column label="操作类型" prop="operateType"/>
        <el-table-column label="操作内容" prop="operateDesc"/>
        <el-table-column label="IP地址" prop="ipAddr"/>
        <el-table-column label="操作时间" prop="createTime"/>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getLogList } from '@/api/admin'

const logList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const query = reactive({
  username: '',
  operation: ''
})

const loadList = async () => {
  // 核心修复：ref变量必须 .value 取出原生数字，避免序列化变成"[object Object]"
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    ...query
  }
  const res = await getLogList(params)
  if (res.code === 200) {
    logList.value = res.data.records
    total.value = res.data.total
  }
}

onMounted(loadList)
</script>

<style scoped>
.admin-log {
  padding: 20px;
}
</style>