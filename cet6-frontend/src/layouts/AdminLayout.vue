<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="aside-title">管理后台</div>
      <el-menu router :default-active="$route.path" background-color="#304156" text-color="#bfcbd9" active-text-color="#409eff">
        <el-menu-item index="/admin/dashboard"><el-icon><DataAnalysis /></el-icon>数据统计</el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
        <el-menu-item index="/admin/notices"><el-icon><Bell /></el-icon>公告管理</el-menu-item>
        <el-menu-item index="/admin/wordbooks"><el-icon><Collection /></el-icon>词书管理</el-menu-item>
        <el-menu-item index="/admin/words"><el-icon><Notebook /></el-icon>单词库</el-menu-item>
        <el-menu-item index="/admin/papers"><el-icon><Document /></el-icon>试卷管理</el-menu-item>
        <el-menu-item index="/admin/questions"><el-icon><EditPen /></el-icon>试题管理</el-menu-item>
        <el-menu-item index="/admin/logs"><el-icon><List /></el-icon>操作日志</el-menu-item>
        <el-menu-item index="/"><el-icon><Back /></el-icon>返回前台</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">英语四六级备考系统管理后台</el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout { min-height: 100vh; }
.aside { background: #304156; }
.aside-title { color: #fff; text-align: center; padding: 20px 0; font-size: 16px; font-weight: bold; }
.admin-header { background: #fff; display: flex; align-items: center; box-shadow: 0 1px 4px rgba(0,0,0,.08); font-weight: bold; }
</style>
