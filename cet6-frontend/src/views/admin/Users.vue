<template>
  <div class="admin-user">
    <el-card>
      <template #header>
        <div class="header-row">
          <span>用户管理</span>
        </div>
      </template>
      <el-table :data="userList" border>
        <el-table-column label="用户名" prop="username" />
        <el-table-column label="昵称" prop="nickname" />
        <el-table-column label="手机号" prop="phone" />
        <el-table-column label="角色">
          <template #default="scope">
            <el-tag :type="scope.row.role==='admin'?'danger':'primary'">
              {{ scope.row.role === 'admin' ? '管理员' : '考生' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态">
            <template #default="scope">
                <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0"@change="switchStatus(scope.row)" />
            </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" @click="editUser(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="delUser(scope.row.id)">删除</el-button>
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
    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" title="编辑用户" width="450px">
      <el-form :model="form">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="form.role">
            <el-radio label="student">考生</el-radio>
            <el-radio label="admin">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSaveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, toRaw } from 'vue'
import { ElMessage } from 'element-plus'
// 接口别名规避重名
import { getUserList, saveUser as apiSaveUser, deleteUser } from '@/api/admin'

const userList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const form = reactive({ id: null, nickname:'', phone:'', role:'student', status:1 })

const loadList = async () => {
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value
  }
  const res = await getUserList(params)
  if(res.code === 200){
    userList.value = res.data.records
    total.value = res.data.total
  }
}
const editUser = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}
const handleSaveUser = async () => {
  await apiSaveUser(toRaw(form))
  dialogVisible.value = false
  ElMessage.success('保存成功')
  loadList()
}
const delUser = async (id) => {
  await deleteUser(id)
  ElMessage.success('删除完成')
  loadList()
}
const switchStatus = async (row) => {
  // row.status 已经是数字1/0，直接提交
  await apiSaveUser(toRaw(row))
  ElMessage.success('状态修改成功')
}

onMounted(loadList)
</script>

<style scoped>
.admin-user { padding:20px; }
.header-row { display:flex; justify-content:space-between; align-items:center; }
</style>