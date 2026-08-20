export const modules = [
  {
    key: 'riskRegisters',
    title: '风险台账',
    path: '/risks/registers',
    api: '/api/risks/registers',
    icon: 'Warning',
    permission: 'risk:manage',
    keyword: 'risk_name',
    fields: [
      { prop: 'risk_code', label: '风险编号', required: true },
      { prop: 'risk_name', label: '风险名称', required: true },
      { prop: 'category', label: '风险分类', type: 'select', options: ['战略风险', '运营风险', '财务风险', '合规风险', '信息安全风险'] },
      { prop: 'level', label: '风险等级', type: 'select', options: ['低', '中', '高', '重大'] },
      { prop: 'owner_department', label: '责任部门' },
      { prop: 'responsible_person', label: '责任人' },
      { prop: 'status', label: '状态', type: 'select', options: ['识别中', '评估中', '整改中', '监控中', '已关闭'] },
      { prop: 'identified_at', label: '识别日期' },
      { prop: 'due_date', label: '整改期限' },
      { prop: 'description', label: '风险描述', type: 'textarea' }
    ]
  },
  {
    key: 'riskLedgers',
    title: '椋庨櫓鍙拌处鏄庣粏',
    path: '/risks/ledgers',
    api: '/api/risks/ledgers',
    icon: 'Document',
    permission: 'risk:manage',
    keyword: 'customer_no',
    readOnly: true,
    fields: [
      { prop: 'customer_no', label: '瀹㈡埛缂栧彿' },
      { prop: 'customer_name_snapshot', label: '瀹㈡埛鍚嶇О' },
      { prop: 'rating_no', label: '璇勭骇缂栧彿' },
      { prop: 'rating_level', label: '璇勭骇绛夌骇' },
      { prop: 'limit_no', label: '棰濆害缂栧彿' },
      { prop: 'total_limit_amount', label: '棰濆害鎬婚' },
      { prop: 'available_limit_amount', label: '鍙敤棰濆害' },
      { prop: 'application_count', label: '涓氬姟鐢宠鏁?' },
      { prop: 'latest_application_no', label: '鏈€鏂扮敵璇风紪鍙?' },
      { prop: 'contract_count', label: '鍚堝悓鏁?' },
      { prop: 'latest_contract_no', label: '鏈€鏂板悎鍚岀紪鍙?' },
      { prop: 'drawdown_count', label: '鍊哄強鏀敤鏁?' },
      { prop: 'latest_drawdown_no', label: '鏈€鏂板悄鐢ㄧ紪鍙?' },
      { prop: 'outstanding_principal_amount_total', label: '鏈敖鏈噾鎬婚' },
      { prop: 'collateral_count', label: '鎶垫娂鍝佹暟' },
      { prop: 'latest_collateral_no', label: '鏈€鏂版姷鍝佺紪鍙?' },
      { prop: 'total_secured_amount', label: '鎶垫娂鎬婚' },
      { prop: 'exposure_count', label: '椋庨櫓鏇村彲鏁?' },
      { prop: 'latest_exposure_no', label: '鏈€鏂伴闄╂洜缂栧彿' },
      { prop: 'exposure_balance_total', label: '椋庨櫓鏆傚瓨鎬婚' },
      { prop: 'overdue_count', label: '閫炬湡鏁?' },
      { prop: 'latest_overdue_no', label: '鏈€鏂伴€炬湡缂栧彿' },
      { prop: 'overdue_max_days', label: '鏈€澶ч€炬湡澶╂暟' },
      { prop: 'debt_default_count', label: '杩濈害鍊哄姟鏁?' },
      { prop: 'latest_debt_default_no', label: '鏈€鏂拌繚绾︾紪鍙?' },
      { prop: 'highest_default_level', label: '杩濈害绛夌骇' },
      { prop: 'customer_default_no', label: '瀹㈡埛杩濈害缂栧彿' },
      { prop: 'customer_default_status', label: '瀹㈡埛杩濈害鐘舵€?' },
      { prop: 'customer_default_total_debt_count', label: '瀹㈡埛杩濈害鍊哄姟鎬绘暟' },
      { prop: 'ledger_risk_level', label: '鍙拌处椋庨櫓绛夌骇' }
    ]
  },
  {
    key: 'riskDefaultTrends',
    title: '违约趋势分析',
    path: '/risks/default-trends',
    api: '/api/risks/default-trends',
    icon: 'TrendCharts',
    permission: 'risk:manage',
    readOnly: true,
    fields: []
  },
  {
    key: 'riskLgdCenter',
    title: 'LGD 风险管理',
    path: '/risks/lgd-center',
    api: '/api/risks/lgd/overview',
    icon: 'DataAnalysis',
    permission: 'risk:manage',
    readOnly: true,
    fields: []
  },
  {
    key: 'riskPortfolioManagement',
    title: '组合限额与校准',
    path: '/risks/portfolio-management',
    api: '/api/risks/portfolio-management',
    icon: 'DataBoard',
    permission: 'risk:manage',
    readOnly: true,
    fields: []
  },
  {
    key: 'riskMonthEndAnalysis',
    title: '月末组合变动',
    path: '/risks/month-end-analysis',
    api: '/api/risks/month-end-analysis',
    icon: 'Calendar',
    permission: 'risk:manage',
    readOnly: true,
    fields: []
  },
  {
    key: 'creditDomainQueries',
    title: '信贷综合查询',
    path: '/risks/credit-domain-query',
    api: '/api/risks/credit-domain/query',
    icon: 'Search',
    permission: 'risk:manage',
    readOnly: true,
    fields: []
  },
  {
    key: 'riskScoringRules',
    title: '组合评分规则',
    path: '/risks/scoring-rules',
    api: '/api/risks/scoring-rules',
    icon: 'DataAnalysis',
    permission: 'risk:manage',
    readOnly: true,
    fields: []
  },
  {
    key: 'riskAlertSubscriptions',
    title: '预警订阅',
    path: '/risks/alert-subscriptions',
    api: '/api/risks/alert-subscriptions',
    icon: 'Bell',
    permission: 'risk:manage',
    readOnly: true,
    fields: []
  },
  {
    key: 'riskAiAssistant',
    title: '风险智能问答',
    path: '/risks/ai-assistant',
    api: '/api/risks/ai-chat/customer',
    icon: 'ChatDotRound',
    permission: 'risk:manage',
    readOnly: true,
    fields: []
  },
  {
    key: 'riskRelationshipGraph',
    title: '风险关系图谱',
    path: '/risks/relationship-graph',
    api: '/api/risks/relationship-graph',
    icon: 'Connection',
    permission: 'risk:manage',
    readOnly: true,
    fields: []
  },
  {
    key: 'riskAlertCases',
    title: '预警处置中心',
    path: '/risks/alert-cases',
    api: '/api/risks/alert-cases',
    icon: 'AlarmClock',
    permission: 'risk:manage',
    readOnly: true,
    fields: []
  },
  {
    key: 'riskDataGovernance',
    title: '数据治理',
    path: '/risks/data-governance',
    api: '/api/risks/data-governance',
    icon: 'DataLine',
    permission: 'risk:manage',
    readOnly: true,
    fields: []
  },
  {
    key: 'riskModelGovernance',
    title: '模型治理',
    path: '/risks/model-governance',
    api: '/api/risks/model-governance',
    icon: 'SetUp',
    permission: 'risk:manage',
    readOnly: true,
    fields: []
  },
  {
    key: 'riskModelMonitoring',
    title: '模型监控',
    path: '/risks/model-monitoring',
    api: '/api/risks/model-monitoring',
    icon: 'Monitor',
    permission: 'risk:manage',
    readOnly: true,
    fields: []
  },
  {
    key: 'riskManagementReports',
    title: '管理报表',
    path: '/risks/management-reports',
    api: '/api/risks/management-reports',
    icon: 'DataBoard',
    permission: 'risk:manage',
    readOnly: true,
    fields: []
  },
  {
    key: 'riskAssessments',
    title: '风险评估',
    path: '/risks/assessments',
    api: '/api/risks/assessments',
    icon: 'DataAnalysis',
    permission: 'risk:assess',
    keyword: 'risk_name',
    fields: [
      { prop: 'risk_code', label: '风险编号', required: true },
      { prop: 'risk_name', label: '风险名称', required: true },
      { prop: 'likelihood', label: '发生可能性', type: 'number' },
      { prop: 'impact', label: '影响程度', type: 'number' },
      { prop: 'inherent_level', label: '固有风险', type: 'select', options: ['低', '中', '高', '重大'] },
      { prop: 'residual_level', label: '剩余风险', type: 'select', options: ['低', '中', '高', '重大'] },
      { prop: 'assessor', label: '评估人' },
      { prop: 'assessed_at', label: '评估日期' },
      { prop: 'conclusion', label: '评估结论', type: 'textarea' }
    ]
  },
  {
    key: 'controlMeasures',
    title: '控制措施',
    path: '/risks/controls',
    api: '/api/risks/controls',
    icon: 'Lock',
    permission: 'risk:control',
    keyword: 'control_name',
    fields: [
      { prop: 'control_code', label: '控制编号', required: true },
      { prop: 'risk_code', label: '关联风险', required: true },
      { prop: 'control_name', label: '控制措施', required: true },
      { prop: 'control_type', label: '控制类型', type: 'select', options: ['预防性控制', '发现性控制', '纠正性控制'] },
      { prop: 'frequency', label: '执行频率', type: 'select', options: ['实时', '每日', '每周', '每月', '每季度'] },
      { prop: 'owner', label: '负责人' },
      { prop: 'effectiveness', label: '有效性', type: 'select', options: ['有效', '部分有效', '无效', '待验证'] },
      { prop: 'status', label: '状态', type: 'select', options: ['启用', '停用', '优化中'] }
    ]
  },
  {
    key: 'treatmentPlans',
    title: '整改任务',
    path: '/risks/treatments',
    api: '/api/risks/treatments',
    icon: 'Tickets',
    permission: 'risk:treat',
    keyword: 'action',
    fields: [
      { prop: 'plan_code', label: '任务编号', required: true },
      { prop: 'risk_code', label: '关联风险', required: true },
      { prop: 'action', label: '整改措施', required: true },
      { prop: 'owner', label: '负责人' },
      { prop: 'due_date', label: '截止日期' },
      { prop: 'progress', label: '进度%', type: 'number' },
      { prop: 'status', label: '状态', type: 'select', options: ['未开始', '进行中', '待复核', '已完成', '逾期'] }
    ]
  },
  {
    key: 'riskEvents',
    title: '风险事件',
    path: '/risks/events',
    api: '/api/risks/events',
    icon: 'Document',
    permission: 'risk:event',
    keyword: 'title',
    fields: [
      { prop: 'event_code', label: '事件编号', required: true },
      { prop: 'title', label: '事件标题', required: true },
      { prop: 'risk_code', label: '关联风险' },
      { prop: 'severity', label: '严重程度', type: 'select', options: ['低', '中', '高', '重大'] },
      { prop: 'occurred_at', label: '发生日期' },
      { prop: 'loss_amount', label: '损失金额', type: 'number' },
      { prop: 'owner', label: '负责人' },
      { prop: 'status', label: '状态', type: 'select', options: ['登记', '处理中', '已复盘', '已关闭'] },
      { prop: 'summary', label: '事件说明', type: 'textarea' }
    ]
  },
  {
    key: 'riskIndicators',
    title: '风险指标',
    path: '/risks/indicators',
    api: '/api/risks/indicators',
    icon: 'TrendCharts',
    permission: 'risk:indicator',
    keyword: 'name',
    fields: [
      { prop: 'indicator_code', label: '指标编号', required: true },
      { prop: 'name', label: '指标名称', required: true },
      { prop: 'threshold', label: '阈值', required: true },
      { prop: 'current_value', label: '当前值' },
      { prop: 'trend', label: '趋势', type: 'select', options: ['上升', '平稳', '下降'] },
      { prop: 'owner', label: '负责人' },
      { prop: 'status', label: '状态', type: 'select', options: ['正常', '预警', '超限'] }
    ]
  },
  {
    key: 'users',
    title: '用户与账号',
    path: '/users',
    api: '/api/users',
    icon: 'User',
    permission: 'user:manage',
    keyword: 'username',
    fields: [
      { prop: 'username', label: '用户名', required: true },
      { prop: 'password', label: '密码' },
      { prop: 'display_name', label: '姓名', required: true },
      { prop: 'email', label: '邮箱' },
      { prop: 'phone', label: '手机号' },
      { prop: 'department_id', label: '部门ID', type: 'number' },
      { prop: 'post_id', label: '岗位ID', type: 'number' },
      { prop: 'status', label: '状态', type: 'select', options: ['ENABLED', 'DISABLED'] }
    ]
  },
  {
    key: 'roles',
    title: '角色权限',
    path: '/roles',
    api: '/api/roles',
    icon: 'Key',
    permission: 'role:manage',
    keyword: 'name',
    fields: [
      { prop: 'name', label: '角色名称', required: true },
      { prop: 'code', label: '角色编码', required: true },
      { prop: 'description', label: '描述' },
      { prop: 'status', label: '状态', type: 'select', options: ['ENABLED', 'DISABLED'] }
    ]
  },
  {
    key: 'operationLogs',
    title: '审计日志',
    path: '/logs/operation',
    api: '/api/logs/operation',
    icon: 'Tickets',
    permission: 'log:view',
    keyword: 'action',
    readOnly: true,
    fields: [
      { prop: 'username', label: '用户名' },
      { prop: 'module', label: '模块' },
      { prop: 'action', label: '动作' },
      { prop: 'method', label: '方法' },
      { prop: 'status', label: '状态' },
      { prop: 'created_at', label: '时间' }
    ]
  },
  {
    key: 'configs',
    title: '风险参数',
    path: '/configs',
    api: '/api/configs',
    icon: 'Setting',
    permission: 'config:manage',
    keyword: 'config_key',
    fields: [
      { prop: 'config_key', label: '参数键', required: true },
      { prop: 'config_value', label: '参数值' },
      { prop: 'description', label: '说明' }
    ]
  },
  {
    key: 'notifications',
    title: '风险通知',
    path: '/notifications',
    api: '/api/notifications',
    icon: 'Bell',
    permission: 'notification:manage',
    keyword: 'title',
    fields: [
      { prop: 'title', label: '标题', required: true },
      { prop: 'content', label: '内容', type: 'textarea' },
      { prop: 'channel', label: '渠道', type: 'select', options: ['SYSTEM', 'EMAIL', 'SMS'] },
      { prop: 'recipients', label: '收件人' },
      { prop: 'target_type', label: '目标', type: 'select', options: ['ALL', 'ROLE', 'USER'] },
      { prop: 'status', label: '状态', type: 'select', options: ['DRAFT', 'PUBLISHED'] }
    ]
  }
]

const riskLedgerModule = modules.find((item) => item.key === 'riskLedgers')
if (riskLedgerModule) {
  riskLedgerModule.title = '风险台账明细'
  const riskLedgerLabels = {
    customer_no: '客户编号',
    customer_name_snapshot: '客户名称',
    rating_no: '评级编号',
    rating_level: '评级等级',
    limit_no: '额度编号',
    total_limit_amount: '额度总额',
    available_limit_amount: '可用额度',
    application_count: '业务申请数',
    latest_application_no: '最新申请编号',
    contract_count: '合同数',
    latest_contract_no: '最新合同编号',
    drawdown_count: '债项支用数',
    latest_drawdown_no: '最新支用编号',
    outstanding_principal_amount_total: '未结本金总额',
    collateral_count: '押品数',
    latest_collateral_no: '最新押品编号',
    total_secured_amount: '押品担保总额',
    exposure_count: '敞口数',
    latest_exposure_no: '最新敞口编号',
    exposure_balance_total: '敞口余额总额',
    overdue_count: '逾期数',
    latest_overdue_no: '最新逾期编号',
    overdue_max_days: '最大逾期天数',
    debt_default_count: '债项违约数',
    latest_debt_default_no: '最新债项违约编号',
    highest_default_level: '违约等级',
    customer_default_no: '客户违约编号',
    customer_default_status: '客户违约状态',
    customer_default_total_debt_count: '客户违约债项总数',
    ledger_risk_level: '台账风险等级'
  }
  riskLedgerModule.fields.forEach((field) => {
    field.label = riskLedgerLabels[field.prop] || field.label
  })
}

export function findModule(key) {
  return modules.find((item) => item.key === key)
}
