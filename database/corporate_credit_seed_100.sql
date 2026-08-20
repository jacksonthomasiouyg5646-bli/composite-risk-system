use user_management;
set names utf8mb4;

drop temporary table if exists tmp_customer_seed;
create temporary table tmp_customer_seed as
select
  tens.n * 10 + ones.n + 1 as n,
  concat('CUST20260720', lpad(tens.n * 10 + ones.n + 1, 4, '0')) as customer_no
from (
  select 0 n union all select 1 union all select 2 union all select 3 union all select 4
  union all select 5 union all select 6 union all select 7 union all select 8 union all select 9
) tens
join (
  select 0 n union all select 1 union all select 2 union all select 3 union all select 4
  union all select 5 union all select 6 union all select 7 union all select 8 union all select 9
) ones
where tens.n * 10 + ones.n + 1 <= 100;

insert ignore into corporate_customer (
  tenant_id, customer_no, customer_name, customer_short_name, unified_social_credit_code,
  registration_no, taxpayer_no, company_type, industry_code, industry_name, registered_capital,
  capital_currency, established_date, business_term_start, business_term_end, registered_address,
  business_address, business_scope, legal_representative_name, legal_representative_id_type,
  legal_representative_id_no_enc, legal_representative_id_no_hash, contact_name, contact_title,
  contact_mobile, contact_phone, contact_email, bank_name, bank_branch_name, bank_account_name,
  bank_account_no_enc, bank_account_no_hash, customer_type, customer_level, source_channel,
  relationship_manager_id, relationship_manager_name, owner_org_id, owner_org_name, kyc_status,
  risk_level, blacklist_flag, compliance_remark, status, remark, created_by, created_by_name
)
select
  1,
  s.customer_no,
  concat('模拟对公客户', lpad(s.n, 3, '0'), '有限公司'),
  concat('客户', lpad(s.n, 3, '0')),
  concat('91310000', lpad(s.n, 10, '0')),
  concat('REG', lpad(s.n, 12, '0')),
  concat('TAX', lpad(s.n, 12, '0')),
  case s.n % 4 when 0 then '有限责任公司' when 1 then '股份有限公司' when 2 then '国有企业' else '民营企业' end,
  concat('IND', lpad((s.n % 8) + 1, 2, '0')),
  case s.n % 5 when 0 then '制造业' when 1 then '批发零售业' when 2 then '信息技术服务业' when 3 then '建筑业' else '交通运输业' end,
  1000000.00 + s.n * 250000.00,
  'CNY',
  date_add('2016-01-01', interval s.n day),
  date_add('2016-01-01', interval s.n day),
  date_add('2036-01-01', interval s.n day),
  concat('上海市浦东新区模拟路', s.n, '号'),
  concat('上海市浦东新区经营路', s.n, '号'),
  '企业经营、贸易服务、技术咨询及相关配套业务。',
  concat('法人', lpad(s.n, 3, '0')),
  'IDENTITY_CARD',
  concat('ENC_LEGAL_', sha2(concat('LEGAL', s.n), 256)),
  sha2(concat('LEGAL', s.n), 256),
  concat('联系人', lpad(s.n, 3, '0')),
  '财务负责人',
  concat('138', lpad(s.n, 8, '0')),
  concat('021-', lpad(60000000 + s.n, 8, '0')),
  concat('corp', lpad(s.n, 3, '0'), '@risk.local'),
  '中国工商银行',
  concat('上海模拟支行', s.n),
  concat('模拟对公客户', lpad(s.n, 3, '0'), '有限公司'),
  concat('ENC_BANK_', sha2(concat('BANK', s.n), 256)),
  sha2(concat('BANK', s.n), 256),
  case when s.n % 10 = 0 then 'VIP' when s.n % 15 = 0 then 'STRATEGIC' else 'NORMAL' end,
  case s.n % 4 when 0 then 'A' when 1 then 'B' when 2 then 'C' else 'D' end,
  'SIMULATION',
  1000 + s.n % 8,
  concat('客户经理', (s.n % 8) + 1),
  200 + s.n % 5,
  concat('对公业务部', (s.n % 5) + 1),
  'APPROVED',
  case when s.n % 20 = 0 then 'HIGH' when s.n % 6 = 0 then 'MEDIUM' else 'LOW' end,
  0,
  '模拟数据，已完成KYC准入。',
  'ACTIVE',
  '对公信贷模拟客户。',
  1,
  '系统管理员'
from tmp_customer_seed s;

insert ignore into corporate_rating (
  tenant_id, rating_no, customer_id, customer_no_snapshot, customer_name_snapshot,
  rating_type, rating_source, rating_model, quantitative_score, qualitative_score,
  rating_score, rating_level, outlook, rating_date, valid_from, valid_to, evaluator_id,
  evaluator_name, review_org_id, review_org_name, approval_no, rating_status,
  key_risk_factors, rating_basis, rating_detail_json, remark, created_by, created_by_name
)
select
  1,
  concat('RATE20260720', lpad(s.n, 4, '0')),
  c.id,
  c.customer_no,
  c.customer_name,
  'REGULAR',
  'SYSTEM',
  'INTERNAL',
  60 + s.n % 35,
  58 + s.n % 32,
  60 + s.n % 35,
  case when s.n % 10 in (0, 1) then 'AA' when s.n % 10 in (2, 3, 4) then 'A' when s.n % 10 in (5, 6, 7) then 'BBB' else 'BB' end,
  case when s.n % 12 = 0 then 'NEGATIVE' when s.n % 9 = 0 then 'POSITIVE' else 'STABLE' end,
  date_add('2026-01-01', interval s.n day),
  date_add('2026-01-01', interval s.n day),
  date_add('2027-01-01', interval s.n day),
  3000 + s.n % 5,
  concat('评级员', (s.n % 5) + 1),
  200 + s.n % 5,
  concat('对公业务部', (s.n % 5) + 1),
  concat('RAPPR', lpad(s.n, 8, '0')),
  'APPROVED',
  '经营稳定，现金流覆盖授信需求；关注行业周期波动。',
  '基于财务指标、交易流水、行业风险和历史履约表现综合评级。',
  json_object('model', 'corporate-rating-v1', 'scoreRank', s.n, 'industry', c.industry_name),
  '有效评级模拟数据。',
  1,
  '系统管理员'
from tmp_customer_seed s
join corporate_customer c on c.customer_no = s.customer_no;

insert ignore into corporate_credit_limit (
  tenant_id, limit_no, customer_id, customer_no_snapshot, customer_name_snapshot, limit_type,
  currency, total_limit_amount, used_limit_amount, frozen_limit_amount, available_limit_amount,
  revolving_flag, secured_flag, guarantee_type, approval_no, approval_date, effective_date,
  expiry_date, credit_rating_level, risk_level, limit_status, manager_id, manager_name,
  owner_org_id, owner_org_name, limit_purpose, risk_mitigation, remark, created_by, created_by_name
)
select
  1,
  concat('LIM20260720', lpad(s.n, 4, '0')),
  c.id,
  c.customer_no,
  c.customer_name,
  'COMPREHENSIVE',
  'CNY',
  1000000.00 + s.n * 50000.00,
  0.00,
  0.00,
  1000000.00 + s.n * 50000.00,
  1,
  case when s.n % 3 = 0 then 1 else 0 end,
  case s.n % 4 when 0 then 'MORTGAGE' when 1 then 'GUARANTEE' when 2 then 'PLEDGE' else 'CREDIT' end,
  concat('LAPPR', lpad(s.n, 8, '0')),
  date_add('2026-03-01', interval s.n day),
  date_add('2026-04-01', interval s.n day),
  date_add('2027-04-01', interval s.n day),
  r.rating_level,
  c.risk_level,
  'ACTIVE',
  c.relationship_manager_id,
  c.relationship_manager_name,
  c.owner_org_id,
  c.owner_org_name,
  '用于日常经营周转、采购结算和项目履约。',
  '结合客户评级、担保措施、资金流向监控和贷后检查进行风险缓释。',
  '有效额度模拟数据。',
  1,
  '系统管理员'
from tmp_customer_seed s
join corporate_customer c on c.customer_no = s.customer_no
join corporate_rating r on r.customer_no_snapshot = c.customer_no and r.rating_status = 'APPROVED';

drop temporary table if exists tmp_app_seed;
create temporary table tmp_app_seed as
select
  s.n,
  app_idx.n as app_idx,
  concat('APP20260720', lpad(s.n, 4, '0'), lpad(app_idx.n, 2, '0')) as application_no
from tmp_customer_seed s
join (select 1 as n union all select 2 as n) app_idx;

insert ignore into corporate_credit_application (
  tenant_id, application_no, customer_id, customer_no_snapshot, customer_name_snapshot,
  application_type, product_type, related_limit_id, currency, apply_amount, apply_term_months,
  apply_rate, repayment_method, fund_usage, guarantee_type, collateral_desc, guarantor_desc,
  latest_rating_level, customer_risk_level, risk_assessment_result, risk_assessment_remark,
  application_status, submitted_at, approved_at, applicant_id, applicant_name, manager_id,
  manager_name, owner_org_id, owner_org_name, approval_no, approval_opinion, document_list_json,
  application_detail_json, remark, created_by, created_by_name
)
select
  1,
  a.application_no,
  c.id,
  c.customer_no,
  c.customer_name,
  case when a.app_idx = 1 then 'DRAWDOWN' else 'ADJUST_LIMIT' end,
  case when a.app_idx = 1 then '流动资金贷款' else '银行承兑汇票' end,
  l.id,
  'CNY',
  round(l.total_limit_amount * case when a.app_idx = 1 then 0.35 else 0.25 end, 2),
  case when a.app_idx = 1 then 12 else 6 end,
  case when a.app_idx = 1 then 3.8500 else 3.6500 end,
  case when a.app_idx = 1 then '按月付息到期还本' else '到期一次性兑付' end,
  case when a.app_idx = 1 then '采购原材料及日常经营周转。' else '贸易结算及票据支付。' end,
  l.guarantee_type,
  case when l.secured_flag = 1 then '提供厂房、设备或应收账款作为抵质押。' else null end,
  case when l.guarantee_type = 'GUARANTEE' then '由关联企业提供连带责任保证。' else null end,
  r.rating_level,
  c.risk_level,
  'PASS',
  '客户评级、额度余额、资金用途和担保措施满足准入要求。',
  'APPROVED',
  date_add('2026-05-01 09:00:00', interval (a.n * 2 + a.app_idx) day),
  date_add('2026-05-03 17:00:00', interval (a.n * 2 + a.app_idx) day),
  4000 + a.n % 10,
  concat('申请人', (a.n % 10) + 1),
  c.relationship_manager_id,
  c.relationship_manager_name,
  c.owner_org_id,
  c.owner_org_name,
  concat('AAPPR', lpad(a.n, 4, '0'), lpad(a.app_idx, 2, '0')),
  '审批通过，同意按申请金额办理业务。',
  json_array('营业执照', '财务报表', '交易合同', '授信用途说明'),
  json_object('source', 'simulation', 'appIndex', a.app_idx),
  '模拟业务申请数据。',
  1,
  '系统管理员'
from tmp_app_seed a
join corporate_customer c on c.customer_no = concat('CUST20260720', lpad(a.n, 4, '0'))
join corporate_credit_limit l on l.customer_no_snapshot = c.customer_no and l.limit_status = 'ACTIVE'
join corporate_rating r on r.customer_no_snapshot = c.customer_no and r.rating_status = 'APPROVED';

insert ignore into corporate_credit_contract (
  tenant_id, contract_no, contract_name, customer_id, customer_no_snapshot,
  customer_name_snapshot, application_id, application_no_snapshot, limit_id, limit_no_snapshot,
  contract_type, product_type, currency, contract_amount, available_draw_amount, used_draw_amount,
  frozen_amount, interest_rate, rate_type, repayment_method, loan_term_months, purpose,
  guarantee_type, collateral_desc, guarantor_desc, party_a_name, party_b_name, signatory_a,
  signatory_b, sign_method, sign_channel, sign_date, effective_date, expiry_date, approval_no,
  approval_date, contract_status, manager_id, manager_name, owner_org_id, owner_org_name,
  contract_file_url, contract_file_hash, contract_detail_json, remark, created_by, created_by_name
)
select
  1,
  concat('CON20260720', lpad(s.n, 4, '0'), lpad(s.app_idx, 2, '0')),
  concat(app.customer_name_snapshot, app.product_type, '合同'),
  app.customer_id,
  app.customer_no_snapshot,
  app.customer_name_snapshot,
  app.id,
  app.application_no,
  app.related_limit_id,
  l.limit_no,
  case when s.app_idx = 1 then 'LOAN' else 'BILL' end,
  app.product_type,
  app.currency,
  app.apply_amount,
  round(app.apply_amount * 0.30, 2),
  round(app.apply_amount * 0.70, 2),
  0.00,
  app.apply_rate,
  'FIXED',
  app.repayment_method,
  app.apply_term_months,
  app.fund_usage,
  app.guarantee_type,
  app.collateral_desc,
  app.guarantor_desc,
  '风险管理银行上海分行',
  app.customer_name_snapshot,
  concat('银行签署人', s.app_idx),
  concat('企业签署人', s.n),
  'ONLINE',
  'SYSTEM',
  date_add(date(app.approved_at), interval 1 day),
  date_add(date(app.approved_at), interval 2 day),
  date_add(date(app.approved_at), interval 2 + app.apply_term_months * 30 day),
  app.approval_no,
  date(app.approved_at),
  'EFFECTIVE',
  app.manager_id,
  app.manager_name,
  app.owner_org_id,
  app.owner_org_name,
  concat('/contracts/', app.application_no, '.pdf'),
  sha2(concat('CONTRACT', app.application_no), 256),
  json_object('source', 'simulation', 'applicationNo', app.application_no),
  '模拟合同数据。',
  1,
  '系统管理员'
from tmp_app_seed s
join corporate_credit_application app on app.application_no = s.application_no
join corporate_credit_limit l on l.id = app.related_limit_id;

drop temporary table if exists tmp_draw_seed;
create temporary table tmp_draw_seed as
select ct.id as contract_id, draw_idx.n as draw_idx
from corporate_credit_contract ct
join (select 1 as n union all select 2 as n) draw_idx
where ct.contract_no like 'CON20260720%';

insert ignore into corporate_credit_drawdown (
  tenant_id, drawdown_no, debt_no, customer_id, customer_no_snapshot, customer_name_snapshot,
  contract_id, contract_no_snapshot, application_id, application_no_snapshot, limit_id,
  limit_no_snapshot, product_type, drawdown_type, currency, apply_draw_amount,
  approved_draw_amount, actual_draw_amount, repaid_principal_amount, outstanding_principal_amount,
  interest_receivable_amount, interest_paid_amount, overdue_principal_amount, overdue_interest_amount,
  interest_rate, rate_type, repayment_method, interest_payment_method, drawdown_date, value_date,
  maturity_date, term_days, fund_usage, payee_name, payee_bank_name, payee_account_no_enc,
  payee_account_no_hash, loan_account_no_enc, loan_account_no_hash, guarantee_type,
  collateral_desc, guarantor_desc, five_category, risk_level, overdue_days, drawdown_status,
  approval_no, approval_date, disbursement_voucher_no, manager_id, manager_name, owner_org_id,
  owner_org_name, drawdown_detail_json, remark, created_by, created_by_name
)
select
  1,
  concat('DD20260720', lpad(ct.id, 6, '0'), lpad(d.draw_idx, 2, '0')),
  concat('DEBT20260720', lpad(ct.id, 6, '0'), lpad(d.draw_idx, 2, '0')),
  ct.customer_id,
  ct.customer_no_snapshot,
  ct.customer_name_snapshot,
  ct.id,
  ct.contract_no,
  ct.application_id,
  ct.application_no_snapshot,
  ct.limit_id,
  ct.limit_no_snapshot,
  ct.product_type,
  case when ct.contract_type = 'BILL' then 'BILL' else 'LOAN' end,
  ct.currency,
  round(ct.contract_amount * case when d.draw_idx = 1 then 0.40 else 0.30 end, 2),
  round(ct.contract_amount * case when d.draw_idx = 1 then 0.40 else 0.30 end, 2),
  round(ct.contract_amount * case when d.draw_idx = 1 then 0.40 else 0.30 end, 2),
  round(ct.contract_amount * case when d.draw_idx = 1 then 0.08 else 0.00 end, 2),
  round(ct.contract_amount * case when d.draw_idx = 1 then 0.32 else 0.30 end, 2),
  round(ct.contract_amount * case when d.draw_idx = 1 then 0.010 else 0.008 end, 2),
  round(ct.contract_amount * case when d.draw_idx = 1 then 0.006 else 0.000 end, 2),
  0.00,
  0.00,
  ct.interest_rate,
  ct.rate_type,
  ct.repayment_method,
  '按月付息',
  date_add(ct.effective_date, interval d.draw_idx * 5 day),
  date_add(ct.effective_date, interval d.draw_idx * 5 day),
  date_add(ct.effective_date, interval 180 + d.draw_idx * 90 day),
  180 + d.draw_idx * 90,
  ct.purpose,
  concat(ct.customer_name_snapshot, '供应商', d.draw_idx),
  '中国工商银行上海分行',
  concat('ENC_PAYEE_', sha2(concat('PAYEE', ct.contract_no, d.draw_idx), 256)),
  sha2(concat('PAYEE', ct.contract_no, d.draw_idx), 256),
  concat('ENC_LOAN_', sha2(concat('LOAN', ct.contract_no, d.draw_idx), 256)),
  sha2(concat('LOAN', ct.contract_no, d.draw_idx), 256),
  ct.guarantee_type,
  ct.collateral_desc,
  ct.guarantor_desc,
  'NORMAL',
  case when ct.contract_amount >= 3000000 then 'MEDIUM' else 'LOW' end,
  0,
  case when d.draw_idx = 1 then 'PARTIAL_REPAID' else 'DISBURSED' end,
  ct.approval_no,
  ct.approval_date,
  concat('VOUCHER', lpad(ct.id, 6, '0'), lpad(d.draw_idx, 2, '0')),
  ct.manager_id,
  ct.manager_name,
  ct.owner_org_id,
  ct.owner_org_name,
  json_object('source', 'simulation', 'contractNo', ct.contract_no, 'drawIndex', d.draw_idx),
  '模拟债项支用数据。',
  1,
  '系统管理员'
from tmp_draw_seed d
join corporate_credit_contract ct on ct.id = d.contract_id;
