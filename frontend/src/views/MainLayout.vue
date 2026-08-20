<template>
  <el-container class="app-shell">
    <el-aside width="244px" class="sidebar">
      <div class="brand-lockup">
        <div class="brand-mark"><DataAnalysis /></div>
        <div class="brand-copy"><strong>组合风险系统</strong><span>风险运营工作台</span></div>
      </div>

      <nav class="risk-navigation" aria-label="主导航">
        <el-menu router :default-active="$route.path" class="side-menu">
          <el-menu-item index="/">
            <el-icon><DataBoard /></el-icon>
            <span>风险总览</span>
          </el-menu-item>
          <template v-for="group in groupedModules" :key="group.key">
            <li class="nav-group-label">{{ group.title }}</li>
            <el-menu-item v-for="item in group.items" :key="item.key" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </nav>

      <div class="sidebar-foot">
        <span class="status-dot"></span>
        <span class="brand-copy">系统服务正常</span>
      </div>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="topbar-context">
          <span class="context-kicker">风险运营</span>
          <div class="page-title">{{ currentTitle }}</div>
        </div>
        <div class="topbar-tools">
          <el-input
            v-model="globalKeyword"
            class="global-search"
            clearable
            :prefix-icon="Search"
            placeholder="搜索客户、合同或债项"
            @keyup.enter="openGlobalSearch"
          />
          <el-tooltip content="查询信贷域"><el-button :icon="Search" circle @click="openGlobalSearch" /></el-tooltip>
          <el-tooltip content="风险通知"><el-button :icon="Bell" circle @click="router.push('/notifications')" /></el-tooltip>
          <el-tooltip content="更新账户信息"><el-button :icon="Refresh" circle :loading="profileLoading" @click="refreshProfile" /></el-tooltip>
          <div class="user-chip">
            <el-avatar :size="28">{{ userInitial }}</el-avatar>
            <span>{{ auth.user?.display_name || auth.user?.username || '用户' }}</span>
          </div>
          <el-tooltip content="退出登录"><el-button :icon="SwitchButton" circle @click="logout" /></el-tooltip>
        </div>
      </el-header>
      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, Refresh, Search, SwitchButton } from '@element-plus/icons-vue'
import { modules } from '../modules'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const globalKeyword = ref('')
const profileLoading = ref(false)

const groupDefinitions = [
  { key: 'credit', title: '信贷风险', items: ['creditDomainQueries', 'riskLedgers', 'riskLgdCenter', 'riskPortfolioManagement', 'riskMonthEndAnalysis', 'riskDefaultTrends'] },
  { key: 'operations', title: '预警与处置', items: ['riskAlertCases', 'riskRegisters', 'riskAssessments', 'controlMeasures', 'treatmentPlans', 'riskEvents', 'riskIndicators'] },
  { key: 'intelligence', title: '智能与治理', items: ['riskAiAssistant', 'riskRelationshipGraph', 'riskDataGovernance', 'riskScoringRules', 'riskModelGovernance', 'riskModelMonitoring', 'riskAlertSubscriptions', 'riskManagementReports'] },
  { key: 'system', title: '系统管理', items: ['users', 'roles', 'departments', 'posts', 'tenants', 'menus', 'operationLogs', 'notifications', 'configs', 'securityPolicies'] }
]

onMounted(() => {
  if (auth.token && !auth.profile) {
    auth.loadProfile().catch(() => {})
  }
})

const visibleModules = computed(() => modules.filter((item) => auth.hasPermission(item.permission)))
const groupedModules = computed(() => groupDefinitions.map((group) => ({
  ...group,
  items: group.items
    .map((key) => visibleModules.value.find((item) => item.key === key))
    .filter(Boolean)
})).filter((group) => group.items.length))
const currentTitle = computed(() => modules.find((item) => item.path === route.path)?.title || '风险总览')
const userInitial = computed(() => String(auth.user?.display_name || auth.user?.username || 'U').slice(0, 1).toUpperCase())

function openGlobalSearch() {
  router.push({ path: '/risks/credit-domain-query', query: globalKeyword.value.trim() ? { keyword: globalKeyword.value.trim() } : {} })
}

async function refreshProfile() {
  profileLoading.value = true
  try {
    await auth.loadProfile()
  } finally {
    profileLoading.value = false
  }
}

async function logout() {
  await auth.logout()
  router.push('/login')
}
</script>
