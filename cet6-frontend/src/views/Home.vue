<template>
  <div class="home">
    <el-row :gutter="24">
      <el-col :span="16">
        <el-card class="hero-card">
          <h1>{{ homeData.title }}</h1>
          <p class="intro">{{ homeData.intro }}</p>
          <el-alert v-if="!userStore.isLoggedIn" title="游客模式：您当前只能浏览首页介绍，登录后可刷题背单词" type="info" show-icon :closable="false" style="margin-top:16px" />
          <div v-if="userStore.isStudent || userStore.isAdmin" class="actions">
            <el-button type="primary" size="large" @click="$router.push('/word-study')">开始背单词</el-button>
            <el-button type="success" size="large" @click="$router.push('/question-practice')">开始刷题</el-button>
          </div>
        </el-card>
        <el-card style="margin-top:24px">
          <template #header><span>四六级考试介绍</span></template>
          <el-descriptions :column="2" border v-if="homeData.examInfo">
            <el-descriptions-item label="考试名称">{{ homeData.examInfo.name }}</el-descriptions-item>
            <el-descriptions-item label="考试时间">{{ homeData.examInfo.frequency }}</el-descriptions-item>
            <el-descriptions-item label="考试时长">{{ homeData.examInfo.duration }}</el-descriptions-item>
            <el-descriptions-item label="考试板块">{{ homeData.examInfo.sections?.join('、') }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span>系统公告</span></template>
          <div v-for="n in homeData.notices" :key="n.id" class="notice-item">
            <h4>{{ n.title }}</h4>
            <p>{{ n.content }}</p>
          </div>
          <el-empty v-if="!homeData.notices?.length" description="暂无公告" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getHomeData } from '@/api/auth'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const homeData = ref({ title: '', intro: '', examInfo: {}, notices: [] })

onMounted(async () => {
  const res = await getHomeData()
  homeData.value = res.data
})
</script>

<style scoped>
.home { max-width: 1200px; margin: 0 auto; }
.hero-card h1 { font-size: 28px; color: #303133; margin-bottom: 16px; }
.intro { color: #606266; line-height: 1.8; font-size: 15px; }
.actions { margin-top: 24px; display: flex; gap: 16px; }
.notice-item { padding: 12px 0; border-bottom: 1px solid #ebeef5; }
.notice-item h4 { color: #303133; margin-bottom: 6px; }
.notice-item p { color: #909399; font-size: 13px; line-height: 1.6; }
</style>
