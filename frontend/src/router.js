import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { getStoredToken } from './stores/session'
import LoginPage from './views/LoginPage.vue'
import MainLayout from './views/MainLayout.vue'
import DashboardPage from './views/DashboardPage.vue'
import CrudPage from './views/CrudPage.vue'
import CreditDomainQueryPage from './views/CreditDomainQueryPage.vue'
import CreditDefaultTrendPage from './views/CreditDefaultTrendPage.vue'
import RiskScoringRulePage from './views/RiskScoringRulePage.vue'
import RiskAlertSubscriptionPage from './views/RiskAlertSubscriptionPage.vue'
import RiskAiAssistantPage from './views/RiskAiAssistantPage.vue'
import RiskModelMonitoringPage from './views/RiskModelMonitoringPage.vue'
import RiskManagementReportPage from './views/RiskManagementReportPage.vue'
import RiskDataGovernancePage from './views/RiskDataGovernancePage.vue'
import RiskModelGovernancePage from './views/RiskModelGovernancePage.vue'
import RiskAlertCasePage from './views/RiskAlertCasePage.vue'
import RiskRelationshipGraphPage from './views/RiskRelationshipGraphPage.vue'
import RiskLgdCenterPage from './views/RiskLgdCenterPage.vue'
import RiskPortfolioManagementPage from './views/RiskPortfolioManagementPage.vue'
import RiskMonthEndAnalysisPage from './views/RiskMonthEndAnalysisPage.vue'
import { modules } from './modules'

const moduleComponents = {
  creditDomainQueries: CreditDomainQueryPage,
  riskDefaultTrends: CreditDefaultTrendPage,
  riskScoringRules: RiskScoringRulePage,
  riskAlertSubscriptions: RiskAlertSubscriptionPage,
  riskAiAssistant: RiskAiAssistantPage,
  riskModelMonitoring: RiskModelMonitoringPage,
  riskManagementReports: RiskManagementReportPage
  ,riskDataGovernance: RiskDataGovernancePage
  ,riskModelGovernance: RiskModelGovernancePage
  ,riskAlertCases: RiskAlertCasePage
  ,riskRelationshipGraph: RiskRelationshipGraphPage
  ,riskLgdCenter: RiskLgdCenterPage
  ,riskPortfolioManagement: RiskPortfolioManagementPage
  ,riskMonthEndAnalysis: RiskMonthEndAnalysisPage
}

const childRoutes = [
  { path: '', name: 'dashboard', component: DashboardPage },
  ...modules.map((module) => ({
    path: module.path.replace(/^\//, ''),
    name: module.key,
    component: moduleComponents[module.key] || CrudPage,
    meta: { moduleKey: module.key, permission: module.permission }
  }))
]

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginPage },
    {
      path: '/',
      component: MainLayout,
      children: childRoutes
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const token = getStoredToken()
  if (auth.token !== token) {
    auth.syncFromStorage()
  }

  if (to.name !== 'login' && !token) {
    return '/login'
  }
  if (to.name === 'login' && token) {
    return '/'
  }
  if (to.meta?.permission && !auth.hasPermission(to.meta.permission)) {
    return '/'
  }
  return true
})

export default router
