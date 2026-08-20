use user_management;
set names utf8mb4;

drop temporary table if exists tmp_customer_seed_20260721;
create temporary table tmp_customer_seed_20260721 as
select
  tens.n * 10 + ones.n + 1 as n,
  concat('CUST20260721', lpad(tens.n * 10 + ones.n + 1, 4, '0')) as customer_no,
  case
    when (tens.n * 10 + ones.n + 1) % 10 = 2 then '21'
    when (tens.n * 10 + ones.n + 1) % 4 = 0 then '6'
    when (tens.n * 10 + ones.n + 1) % 4 = 1 then '4'
    when (tens.n * 10 + ones.n + 1) % 4 = 2 then '8'
    else '10'
  end as rating_level
from (
  select 0 n union all select 1 union all select 2 union all select 3 union all select 4
  union all select 5 union all select 6 union all select 7 union all select 8 union all select 9
) tens
join (
  select 0 n union all select 1 union all select 2 union all select 3 union all select 4
  union all select 5 union all select 6 union all select 7 union all select 8 union all select 9
) ones
where tens.n * 10 + ones.n + 1 <= 100;

drop temporary table if exists tmp_app_seed_20260721;
create temporary table tmp_app_seed_20260721 as
select
  s.n,
  s.customer_no,
  app_idx.n as app_idx,
  concat('APP20260721', lpad(s.n, 4, '0'), lpad(app_idx.n, 2, '0')) as application_no,
  case when app_idx.n = 1 then 'WORKING_CAPITAL_LOAN' else 'FIXED_ASSET_LOAN' end as product_type,
  case when app_idx.n = 1 then 600000.00 + s.n * 1000.00 else 800000.00 + s.n * 1000.00 end as apply_amount
from tmp_customer_seed_20260721 s
join (select 1 n union all select 2) app_idx;

drop temporary table if exists tmp_draw_seed_20260721;
create temporary table tmp_draw_seed_20260721 as
select
  a.n,
  a.customer_no,
  a.app_idx,
  draw_idx.n as draw_idx,
  concat('DRAW20260721', lpad(a.n, 4, '0'), lpad(a.app_idx, 2, '0'), lpad(draw_idx.n, 2, '0')) as drawdown_no,
  concat('DEBT20260721', lpad(a.n, 4, '0'), lpad(a.app_idx, 2, '0'), lpad(draw_idx.n, 2, '0')) as debt_no,
  concat('CON20260721', lpad(a.n, 4, '0'), lpad(a.app_idx, 2, '0')) as contract_no,
  a.application_no,
  a.product_type,
  round(case when a.app_idx = 1 then 240000.00 + a.n * 300.00 + draw_idx.n * 5000.00 else 320000.00 + a.n * 400.00 + draw_idx.n * 5000.00 end, 2) as actual_draw_amount,
  round(case when a.app_idx = 1 then 240000.00 + a.n * 300.00 + draw_idx.n * 5000.00 else 320000.00 + a.n * 400.00 + draw_idx.n * 5000.00 end * 0.18, 2) as repaid_principal_amount,
  round(case when a.app_idx = 1 then 240000.00 + a.n * 300.00 + draw_idx.n * 5000.00 else 320000.00 + a.n * 400.00 + draw_idx.n * 5000.00 end * 0.82, 2) as outstanding_principal_amount,
  case
    when a.app_idx = 1 and draw_idx.n = 1 and a.n % 10 = 0 then 120
    when a.app_idx = 1 and draw_idx.n = 1 and a.n % 3 = 0 then 35
    when a.app_idx = 1 and draw_idx.n = 1 then 8
    else 0
  end as overdue_days,
  case
    when a.app_idx = 1 and draw_idx.n = 1 and a.n % 10 = 1 then 'SUBSTANDARD'
    when a.n % 25 = 0 then 'SPECIAL_MENTION'
    else 'NORMAL'
  end as five_category,
  case
    when a.app_idx = 1 and draw_idx.n = 1 and a.n % 10 in (0, 1, 2) then 'HIGH'
    when a.app_idx = 1 and draw_idx.n = 1 and a.n % 3 = 0 then 'MEDIUM'
    else 'LOW'
  end as risk_level
from tmp_app_seed_20260721 a
join (select 1 n union all select 2) draw_idx;

drop temporary table if exists tmp_collateral_seed_20260721;
create temporary table tmp_collateral_seed_20260721 as
select
  s.n,
  s.customer_no,
  coll_idx.n as collateral_idx,
  concat('COLL20260721', lpad(s.n, 4, '0'), lpad(coll_idx.n, 2, '0')) as collateral_no,
  case when coll_idx.n = 1 then 'REAL_ESTATE' else 'EQUIPMENT' end as collateral_type,
  round(case when coll_idx.n = 1 then 1200000.00 + s.n * 5000.00 else 800000.00 + s.n * 3000.00 end, 2) as appraisal_value,
  case when coll_idx.n = 1 then 0.7000 else 0.6000 end as mortgage_rate
from tmp_customer_seed_20260721 s
join (select 1 n union all select 2) coll_idx;

insert ignore into corporate_customer (
  tenant_id, customer_no, customer_name, customer_short_name, unified_social_credit_code,
  company_type, industry_code, industry_name, registered_capital, capital_currency,
  established_date, registered_address, business_address, legal_representative_name,
  contact_name, contact_mobile, contact_email, bank_name, bank_account_name,
  customer_type, customer_level, source_channel, relationship_manager_id,
  relationship_manager_name, owner_org_id, owner_org_name, kyc_status, risk_level,
  blacklist_flag, status, remark, created_by, created_by_name
)
select
  1,
  s.customer_no,
  concat('Corporate Customer 20260721-', lpad(s.n, 3, '0'), ' Co., Ltd.'),
  concat('CC', lpad(s.n, 3, '0')),
  concat('91310001', lpad(s.n, 10, '0')),
  'LIMITED',
  concat('IND', lpad((s.n % 8) + 1, 2, '0')),
  case s.n % 5 when 0 then 'Manufacturing' when 1 then 'Wholesale' when 2 then 'Technology Service' when 3 then 'Construction' else 'Transportation' end,
  1000000.00 + s.n * 200000.00,
  'CNY',
  date_add('2017-01-01', interval s.n day),
  concat('Shanghai Demo Registered Road ', s.n),
  concat('Shanghai Demo Business Road ', s.n),
  concat('Legal Person ', lpad(s.n, 3, '0')),
  concat('Contact ', lpad(s.n, 3, '0')),
  concat('139', lpad(s.n, 8, '0')),
  concat('corp20260721_', lpad(s.n, 3, '0'), '@risk.local'),
  'Industrial and Commercial Bank of China',
  concat('Corporate Customer 20260721-', lpad(s.n, 3, '0'), ' Co., Ltd.'),
  case when s.n % 10 = 0 then 'VIP' else 'NORMAL' end,
  case s.n % 4 when 0 then 'A' when 1 then 'B' when 2 then 'C' else 'D' end,
  'SIMULATION',
  1000 + s.n % 8,
  concat('Manager ', (s.n % 8) + 1),
  200 + s.n % 5,
  concat('Corporate Banking Dept ', (s.n % 5) + 1),
  'APPROVED',
  case when s.n % 10 in (0, 1, 2) then 'HIGH' when s.n % 6 = 0 then 'MEDIUM' else 'LOW' end,
  0,
  'ACTIVE',
  'Full-chain credit simulation batch 20260721',
  1,
  'system'
from tmp_customer_seed_20260721 s;

insert ignore into corporate_rating (
  tenant_id, rating_no, customer_id, customer_no_snapshot, customer_name_snapshot,
  rating_type, rating_source, rating_model, quantitative_score, qualitative_score,
  rating_score, rating_level, outlook, rating_date, valid_from, valid_to,
  evaluator_id, evaluator_name, review_org_id, review_org_name, approval_no,
  rating_status, key_risk_factors, rating_basis, created_by, created_by_name
)
select
  1,
  concat('RATE20260721', lpad(s.n, 4, '0')),
  c.id,
  c.customer_no,
  c.customer_name,
  'REGULAR',
  'SYSTEM',
  'INTERNAL',
  case when s.rating_level = '21' then 45.00 else 78.00 - s.n % 12 end,
  case when s.rating_level = '21' then 40.00 else 80.00 - s.n % 10 end,
  case when s.rating_level = '21' then 42.00 else 79.00 - s.n % 10 end,
  s.rating_level,
  case when s.rating_level = '21' then 'NEGATIVE' else 'STABLE' end,
  '2026-07-21',
  '2026-07-21',
  '2027-07-20',
  300 + s.n % 5,
  concat('Rating User ', (s.n % 5) + 1),
  200 + s.n % 5,
  concat('Corporate Banking Dept ', (s.n % 5) + 1),
  concat('RAPPR20260721', lpad(s.n, 4, '0')),
  'APPROVED',
  case when s.rating_level = '21' then 'Rating level 21 default trigger sample' else 'Stable operation sample' end,
  'Simulation rating generated by rule',
  1,
  'system'
from tmp_customer_seed_20260721 s
join corporate_customer c on c.customer_no = s.customer_no;

insert ignore into corporate_credit_limit (
  tenant_id, limit_no, customer_id, customer_no_snapshot, customer_name_snapshot,
  limit_type, currency, total_limit_amount, used_limit_amount, frozen_limit_amount,
  available_limit_amount, revolving_flag, secured_flag, guarantee_type, approval_no,
  approval_date, effective_date, expiry_date, credit_rating_level, risk_level,
  limit_status, manager_id, manager_name, owner_org_id, owner_org_name,
  limit_purpose, risk_mitigation, created_by, created_by_name
)
select
  1,
  concat('LIM20260721', lpad(s.n, 4, '0')),
  c.id,
  c.customer_no,
  c.customer_name,
  'COMPREHENSIVE',
  'CNY',
  3000000.00 + s.n * 20000.00,
  1500000.00 + s.n * 5000.00,
  0.00,
  1500000.00 + s.n * 15000.00,
  1,
  1,
  'COLLATERAL',
  concat('LAPPR20260721', lpad(s.n, 4, '0')),
  '2026-07-21',
  '2026-07-21',
  '2027-07-20',
  r.rating_level,
  c.risk_level,
  'ACTIVE',
  c.relationship_manager_id,
  c.relationship_manager_name,
  c.owner_org_id,
  c.owner_org_name,
  'Working capital and fixed asset credit line',
  'Collateral coverage and internal rating control',
  1,
  'system'
from tmp_customer_seed_20260721 s
join corporate_customer c on c.customer_no = s.customer_no
join corporate_rating r on r.customer_no_snapshot = c.customer_no and r.rating_status = 'APPROVED';

insert ignore into corporate_credit_application (
  tenant_id, application_no, customer_id, customer_no_snapshot, customer_name_snapshot,
  application_type, product_type, related_limit_id, currency, apply_amount,
  apply_term_months, apply_rate, repayment_method, fund_usage, guarantee_type,
  latest_rating_level, customer_risk_level, risk_assessment_result,
  application_status, submitted_at, approved_at, applicant_id, applicant_name,
  manager_id, manager_name, owner_org_id, owner_org_name, approval_no,
  approval_opinion, created_by, created_by_name
)
select
  1,
  a.application_no,
  c.id,
  c.customer_no,
  c.customer_name,
  'LOAN',
  a.product_type,
  l.id,
  'CNY',
  a.apply_amount,
  case when a.app_idx = 1 then 12 else 24 end,
  4.3500 + a.app_idx * 0.1500,
  'MONTHLY_INTEREST',
  case when a.app_idx = 1 then 'Working capital turnover' else 'Fixed asset purchase' end,
  'COLLATERAL',
  r.rating_level,
  c.risk_level,
  case when c.risk_level = 'HIGH' then 'CONDITIONAL_PASS' else 'PASS' end,
  'APPROVED',
  '2026-07-21 09:00:00',
  '2026-07-21 10:00:00',
  400 + a.n % 10,
  concat('Applicant ', (a.n % 10) + 1),
  c.relationship_manager_id,
  c.relationship_manager_name,
  c.owner_org_id,
  c.owner_org_name,
  concat('AAPPR20260721', lpad(a.n, 4, '0'), lpad(a.app_idx, 2, '0')),
  'Approved by simulation rule',
  1,
  'system'
from tmp_app_seed_20260721 a
join corporate_customer c on c.customer_no = a.customer_no
join corporate_credit_limit l on l.customer_no_snapshot = c.customer_no and l.limit_status = 'ACTIVE'
join corporate_rating r on r.customer_no_snapshot = c.customer_no and r.rating_status = 'APPROVED';

insert ignore into corporate_credit_contract (
  tenant_id, contract_no, contract_name, customer_id, customer_no_snapshot,
  customer_name_snapshot, application_id, application_no_snapshot, limit_id,
  limit_no_snapshot, contract_type, product_type, currency, contract_amount,
  available_draw_amount, used_draw_amount, frozen_amount, interest_rate,
  rate_type, repayment_method, loan_term_months, purpose, guarantee_type,
  party_a_name, party_b_name, sign_date, effective_date, expiry_date,
  approval_no, approval_date, contract_status, manager_id, manager_name,
  owner_org_id, owner_org_name, created_by, created_by_name
)
select
  1,
  concat('CON20260721', lpad(a.n, 4, '0'), lpad(a.app_idx, 2, '0')),
  concat('Credit Contract ', a.application_no),
  c.id,
  c.customer_no,
  c.customer_name,
  app.id,
  app.application_no,
  l.id,
  l.limit_no,
  'SINGLE',
  a.product_type,
  'CNY',
  a.apply_amount,
  round(a.apply_amount * 0.20, 2),
  round(a.apply_amount * 0.80, 2),
  0.00,
  app.apply_rate,
  'FIXED',
  app.repayment_method,
  app.apply_term_months,
  app.fund_usage,
  'COLLATERAL',
  'Risk Bank',
  c.customer_name,
  '2026-07-21',
  '2026-07-21',
  date_add('2026-07-21', interval app.apply_term_months month),
  concat('CAPPR20260721', lpad(a.n, 4, '0'), lpad(a.app_idx, 2, '0')),
  '2026-07-21',
  'EFFECTIVE',
  c.relationship_manager_id,
  c.relationship_manager_name,
  c.owner_org_id,
  c.owner_org_name,
  1,
  'system'
from tmp_app_seed_20260721 a
join corporate_customer c on c.customer_no = a.customer_no
join corporate_credit_application app on app.application_no = a.application_no
join corporate_credit_limit l on l.customer_no_snapshot = c.customer_no and l.limit_status = 'ACTIVE';

insert ignore into corporate_credit_drawdown (
  tenant_id, drawdown_no, debt_no, customer_id, customer_no_snapshot,
  customer_name_snapshot, contract_id, contract_no_snapshot, application_id,
  application_no_snapshot, limit_id, limit_no_snapshot, product_type,
  drawdown_type, currency, apply_draw_amount, approved_draw_amount,
  actual_draw_amount, repaid_principal_amount, outstanding_principal_amount,
  interest_receivable_amount, interest_paid_amount, overdue_principal_amount,
  overdue_interest_amount, interest_rate, rate_type, repayment_method,
  interest_payment_method, drawdown_date, value_date, maturity_date, term_days,
  fund_usage, guarantee_type, five_category, risk_level, overdue_days,
  drawdown_status, approval_no, approval_date, disbursement_voucher_no,
  manager_id, manager_name, owner_org_id, owner_org_name, created_by, created_by_name
)
select
  1,
  t.drawdown_no,
  t.debt_no,
  c.id,
  c.customer_no,
  c.customer_name,
  con.id,
  con.contract_no,
  app.id,
  app.application_no,
  l.id,
  l.limit_no,
  t.product_type,
  'LOAN',
  'CNY',
  t.actual_draw_amount,
  t.actual_draw_amount,
  t.actual_draw_amount,
  t.repaid_principal_amount,
  t.outstanding_principal_amount,
  round(t.actual_draw_amount * 0.035, 2),
  round(t.actual_draw_amount * 0.010, 2),
  case when t.app_idx = 1 and t.draw_idx = 1 then round(t.actual_draw_amount * 0.080, 2) else 0.00 end,
  case when t.app_idx = 1 and t.draw_idx = 1 then round(t.actual_draw_amount * 0.006, 2) else 0.00 end,
  4.3500 + t.app_idx * 0.1500,
  'FIXED',
  'MONTHLY_INTEREST',
  'MONTHLY',
  date_add('2026-07-21', interval t.draw_idx day),
  date_add('2026-07-21', interval t.draw_idx day),
  date_add('2026-07-21', interval 365 + t.app_idx * 180 day),
  365 + t.app_idx * 180,
  case when t.app_idx = 1 then 'Working capital turnover' else 'Fixed asset purchase' end,
  'COLLATERAL',
  t.five_category,
  t.risk_level,
  t.overdue_days,
  'DISBURSED',
  concat('DAPPR20260721', lpad(t.n, 4, '0'), lpad(t.app_idx, 2, '0'), lpad(t.draw_idx, 2, '0')),
  '2026-07-21',
  concat('VOUCH20260721', lpad(t.n, 4, '0'), lpad(t.app_idx, 2, '0'), lpad(t.draw_idx, 2, '0')),
  c.relationship_manager_id,
  c.relationship_manager_name,
  c.owner_org_id,
  c.owner_org_name,
  1,
  'system'
from tmp_draw_seed_20260721 t
join corporate_customer c on c.customer_no = t.customer_no
join corporate_credit_contract con on con.contract_no = t.contract_no
join corporate_credit_application app on app.application_no = t.application_no
join corporate_credit_limit l on l.customer_no_snapshot = c.customer_no and l.limit_status = 'ACTIVE';

insert ignore into corporate_credit_collateral (
  tenant_id, collateral_no, customer_no, customer_name_snapshot, collateral_name,
  collateral_type, ownership_type, owner_name, ownership_cert_no, currency,
  original_value, appraisal_value, confirmed_value, mortgage_rate,
  available_secured_amount, appraisal_org_name, appraisal_date,
  appraisal_expiry_date, registration_flag, registration_no, registration_date,
  custody_org_name, insurance_flag, insurance_policy_no, insurance_expiry_date,
  location_desc, risk_level, collateral_status, created_by, created_by_name
)
select
  1,
  cs.collateral_no,
  c.customer_no,
  c.customer_name,
  concat('Collateral ', lpad(cs.n, 4, '0'), '-', cs.collateral_idx),
  cs.collateral_type,
  'OWNED',
  c.customer_name,
  concat('OWNCERT20260721', lpad(cs.n, 4, '0'), lpad(cs.collateral_idx, 2, '0')),
  'CNY',
  round(cs.appraisal_value * 1.05, 2),
  cs.appraisal_value,
  cs.appraisal_value,
  cs.mortgage_rate,
  round(cs.appraisal_value * cs.mortgage_rate, 2),
  'Demo Appraisal Org',
  '2026-07-21',
  '2027-07-20',
  1,
  concat('REGCOLL20260721', lpad(cs.n, 4, '0'), lpad(cs.collateral_idx, 2, '0')),
  '2026-07-21',
  'Risk Bank Custody Center',
  1,
  concat('POLICY20260721', lpad(cs.n, 4, '0'), lpad(cs.collateral_idx, 2, '0')),
  '2027-07-20',
  concat('Collateral location ', cs.n, '-', cs.collateral_idx),
  case when cs.n % 10 in (0, 1, 2) then 'MEDIUM' else 'LOW' end,
  'ACTIVE',
  1,
  'system'
from tmp_collateral_seed_20260721 cs
join corporate_customer c on c.customer_no = cs.customer_no;

insert ignore into corporate_credit_contract_collateral (
  tenant_id, relation_no, contract_no, collateral_no, customer_no,
  secured_amount, pledge_rate, relation_type, priority_order,
  effective_date, expiry_date, relation_status, created_by, created_by_name
)
select
  1,
  concat('REL20260721', lpad(a.n, 4, '0'), lpad(a.app_idx, 2, '0'), lpad(cs.collateral_idx, 2, '0')),
  con.contract_no,
  cs.collateral_no,
  c.customer_no,
  round(con.contract_amount * case when cs.collateral_idx = 1 then 0.45 else 0.35 end, 2),
  case when cs.collateral_idx = 1 then 0.7000 else 0.6000 end,
  case when cs.collateral_idx = 1 then 'MORTGAGE' else 'PLEDGE' end,
  cs.collateral_idx,
  con.effective_date,
  con.expiry_date,
  'ACTIVE',
  1,
  'system'
from tmp_app_seed_20260721 a
join corporate_customer c on c.customer_no = a.customer_no
join corporate_credit_contract con on con.contract_no = concat('CON20260721', lpad(a.n, 4, '0'), lpad(a.app_idx, 2, '0'))
join tmp_collateral_seed_20260721 cs on cs.customer_no = a.customer_no;

insert ignore into corporate_risk_exposure (
  tenant_id, exposure_no, customer_no, customer_name_snapshot, contract_no,
  debt_no, drawdown_no, product_type, currency, exposure_balance, ead_amount,
  undrawn_amount, collateral_value, guaranteed_amount, lgd_avg, lgd_downturn,
  pd, el_avg, el_downturn, rating_result, product_recovery_rate, recovery_source,
  five_category, risk_level, overdue_days, default_flag, measurement_date,
  data_date, model_name, model_version, scenario_type, exposure_status,
  created_by, created_by_name
)
select
  1,
  concat('EXPO20260721', lpad(t.n, 4, '0'), lpad(t.app_idx, 2, '0'), lpad(t.draw_idx, 2, '0')),
  c.customer_no,
  c.customer_name,
  d.contract_no_snapshot,
  d.debt_no,
  d.drawdown_no,
  d.product_type,
  'CNY',
  d.outstanding_principal_amount,
  round(d.outstanding_principal_amount * 1.05, 2),
  case when t.draw_idx = 1 then 20000.00 else 0.00 end,
  coalesce((select sum(rel.secured_amount) from corporate_credit_contract_collateral rel where rel.contract_no = d.contract_no_snapshot), 0.00),
  0.00,
  case when t.n % 10 in (0, 1, 2) and t.app_idx = 1 and t.draw_idx = 1 then 0.550000 else 0.350000 end,
  case when t.n % 10 in (0, 1, 2) and t.app_idx = 1 and t.draw_idx = 1 then 0.700000 else 0.450000 end,
  case
    when r.rating_level = '21' and t.app_idx = 1 and t.draw_idx = 1 then 0.650000
    when t.overdue_days > 90 then 0.250000
    when t.five_category in ('SUBSTANDARD', 'DOUBTFUL', 'LOSS') then 0.180000
    else 0.030000
  end,
  round(d.outstanding_principal_amount * case
    when r.rating_level = '21' and t.app_idx = 1 and t.draw_idx = 1 then 0.357500
    when t.overdue_days > 90 then 0.137500
    when t.five_category in ('SUBSTANDARD', 'DOUBTFUL', 'LOSS') then 0.099000
    else 0.010500
  end, 6),
  round(d.outstanding_principal_amount * case
    when r.rating_level = '21' and t.app_idx = 1 and t.draw_idx = 1 then 0.455000
    when t.overdue_days > 90 then 0.175000
    when t.five_category in ('SUBSTANDARD', 'DOUBTFUL', 'LOSS') then 0.126000
    else 0.013500
  end, 6),
  r.rating_level,
  case when t.n % 10 in (0, 1, 2) and t.app_idx = 1 and t.draw_idx = 1 then 0.450000 else 0.650000 end,
  'MODEL_RULE',
  t.five_category,
  t.risk_level,
  t.overdue_days,
  case when t.app_idx = 1 and t.draw_idx = 1 and t.n % 10 in (0, 1, 2) then 1 else 0 end,
  '2026-07-21',
  '2026-07-21',
  'Corporate Risk Exposure Model',
  'v1.0',
  'BASE',
  'ACTIVE',
  1,
  'system'
from tmp_draw_seed_20260721 t
join corporate_credit_drawdown d on d.drawdown_no = t.drawdown_no
join corporate_customer c on c.customer_no = t.customer_no
join corporate_rating r on r.customer_no_snapshot = c.customer_no and r.rating_status = 'APPROVED';

insert ignore into corporate_credit_overdue (
  tenant_id, overdue_no, customer_no, customer_name_snapshot, contract_no,
  debt_no, drawdown_no, installment_no, product_type, currency, due_date,
  overdue_date, overdue_days, grace_days, grace_due_date, legal_holiday_flag,
  holiday_calendar_code, legal_holiday_name, principal_due_amount,
  interest_due_amount, overdue_principal_amount, overdue_interest_amount,
  penalty_interest_amount, compound_interest_amount, fee_overdue_amount,
  total_overdue_amount, repaid_overdue_amount, remaining_overdue_amount,
  overdue_stage, collection_status, repayment_status, default_flag,
  extension_flag, five_category, risk_level, manager_id, manager_name,
  owner_org_id, owner_org_name, data_date, created_by, created_by_name
)
select
  1,
  concat('OVD20260721', lpad(t.n, 4, '0')),
  c.customer_no,
  c.customer_name,
  d.contract_no_snapshot,
  d.debt_no,
  d.drawdown_no,
  concat('INST20260721', lpad(t.n, 4, '0')),
  d.product_type,
  'CNY',
  date_add('2026-03-01', interval t.n day),
  date_add(date_add('2026-03-01', interval t.n day), interval 1 day),
  t.overdue_days,
  3,
  date_add(date_add('2026-03-01', interval t.n day), interval 4 day),
  case when t.n % 7 = 0 then 1 else 0 end,
  'CN_PUBLIC_HOLIDAY',
  case when t.n % 7 = 0 then 'PUBLIC_HOLIDAY_SAMPLE' else null end,
  round(d.actual_draw_amount * 0.080, 2),
  round(d.actual_draw_amount * 0.006, 2),
  d.overdue_principal_amount,
  d.overdue_interest_amount,
  round(d.actual_draw_amount * 0.002, 2),
  round(d.actual_draw_amount * 0.001, 2),
  0.00,
  round(d.overdue_principal_amount + d.overdue_interest_amount + d.actual_draw_amount * 0.003, 2),
  round(d.actual_draw_amount * 0.001, 2),
  round(d.overdue_principal_amount + d.overdue_interest_amount + d.actual_draw_amount * 0.002, 2),
  case when t.overdue_days > 90 then 'M4' when t.overdue_days > 30 then 'M2' else 'M1' end,
  case when t.overdue_days > 90 then 'LEGAL_COLLECTION' else 'PENDING' end,
  'OVERDUE',
  case when t.n % 10 in (0, 1, 2) then 1 else 0 end,
  0,
  t.five_category,
  t.risk_level,
  c.relationship_manager_id,
  c.relationship_manager_name,
  c.owner_org_id,
  c.owner_org_name,
  '2026-07-21',
  1,
  'system'
from tmp_draw_seed_20260721 t
join corporate_credit_drawdown d on d.drawdown_no = t.drawdown_no
join corporate_customer c on c.customer_no = t.customer_no
where t.app_idx = 1
  and t.draw_idx = 1;

insert ignore into corporate_debt_default (
  tenant_id, debt_default_no, customer_no, customer_name_snapshot, contract_no,
  debt_no, drawdown_no, exposure_no, product_type, currency, default_level,
  default_date, default_recognition_date, overdue_days, overdue_date,
  five_category, rating_result, overdue_default_flag, five_category_default_flag,
  rating_default_flag, default_principal_amount, default_interest_amount,
  default_exposure_amount, recovered_amount, loss_amount, default_reason,
  default_source, default_status, manager_id, manager_name, owner_org_id,
  owner_org_name, data_date, created_by, created_by_name
)
select
  1,
  concat('DDEF20260721', lpad(t.n, 4, '0')),
  c.customer_no,
  c.customer_name,
  d.contract_no_snapshot,
  d.debt_no,
  d.drawdown_no,
  concat('EXPO20260721', lpad(t.n, 4, '0'), lpad(t.app_idx, 2, '0'), lpad(t.draw_idx, 2, '0')),
  d.product_type,
  'CNY',
  case when t.n % 10 = 0 then 'A' when t.n % 10 = 1 then 'B' else 'C' end,
  '2026-07-21',
  '2026-07-21',
  t.overdue_days,
  date_add(date_add('2026-03-01', interval t.n day), interval 1 day),
  t.five_category,
  r.rating_level,
  case when t.n % 10 = 0 then 1 else 0 end,
  case when t.n % 10 = 1 then 1 else 0 end,
  case when t.n % 10 = 2 then 1 else 0 end,
  d.overdue_principal_amount,
  d.overdue_interest_amount,
  d.outstanding_principal_amount,
  0.00,
  round(d.outstanding_principal_amount * 0.35, 2),
  case
    when t.n % 10 = 0 then 'A: overdue days greater than 90'
    when t.n % 10 = 1 then 'B: five-category classification is substandard'
    else 'C: rating level is 21'
  end,
  'SYSTEM_RULE',
  'ACTIVE',
  c.relationship_manager_id,
  c.relationship_manager_name,
  c.owner_org_id,
  c.owner_org_name,
  '2026-07-21',
  1,
  'system'
from tmp_draw_seed_20260721 t
join corporate_credit_drawdown d on d.drawdown_no = t.drawdown_no
join corporate_customer c on c.customer_no = t.customer_no
join corporate_rating r on r.customer_no_snapshot = c.customer_no and r.rating_status = 'APPROVED'
where t.app_idx = 1
  and t.draw_idx = 1
  and t.n % 10 in (0, 1, 2);

-- Normalize the simulated lifecycle so its dates match credit-business dependencies.
update corporate_rating
set rating_date = '2025-09-15', valid_from = '2025-09-15', valid_to = '2026-09-14'
where rating_no like 'RATE20260721%' and created_by_name = 'system';

update corporate_credit_limit
set approval_date = '2025-10-01', effective_date = '2025-10-01', expiry_date = '2026-10-01'
where limit_no like 'LIM20260721%' and created_by_name = 'system';

update corporate_credit_application
set submitted_at = date_add('2025-10-15 09:00:00', interval cast(substring(application_no, 12, 4) as unsigned) day),
    approved_at = date_add('2025-10-15 10:00:00', interval cast(substring(application_no, 12, 4) as unsigned) day)
where application_no like 'APP20260721%' and created_by_name = 'system';

update corporate_credit_contract ct
join corporate_credit_application app on app.application_no = ct.application_no_snapshot
set ct.sign_date = date_add(date(app.submitted_at), interval 1 day),
    ct.effective_date = date_add(date(app.submitted_at), interval 1 day),
    ct.approval_date = date(app.approved_at),
    ct.expiry_date = date_add(date_add(date(app.submitted_at), interval 1 day), interval ct.loan_term_months month)
where ct.contract_no like 'CON20260721%' and ct.created_by_name = 'system';

update corporate_credit_drawdown d
join corporate_credit_contract ct on ct.contract_no = d.contract_no_snapshot
set d.drawdown_date = date_add(ct.effective_date, interval cast(right(d.drawdown_no, 2) as unsigned) day),
    d.value_date = date_add(ct.effective_date, interval cast(right(d.drawdown_no, 2) as unsigned) day),
    d.approval_date = date_add(ct.effective_date, interval cast(right(d.drawdown_no, 2) as unsigned) day),
    d.maturity_date = date_add(date_add(ct.effective_date, interval cast(right(d.drawdown_no, 2) as unsigned) day), interval d.term_days day)
where d.drawdown_no like 'DRAW20260721%' and d.created_by_name = 'system';

update corporate_credit_overdue o
join corporate_credit_drawdown d on d.debt_no = o.debt_no and d.deleted_flag = 0
set o.due_date = date_sub(date_sub(o.data_date, interval o.overdue_days day), interval 1 day),
    o.overdue_date = date_sub(o.data_date, interval o.overdue_days day),
    o.grace_due_date = date_add(date_sub(o.data_date, interval o.overdue_days day), interval o.grace_days day)
where o.overdue_no like 'OVD20260721%' and o.created_by_name = 'system';

update corporate_debt_default dd
join corporate_credit_overdue o on o.debt_no = dd.debt_no and o.deleted_flag = 0
set dd.overdue_date = o.overdue_date,
    dd.default_date = date_add(o.overdue_date, interval 1 day),
    dd.default_recognition_date = date_add(o.overdue_date, interval 1 day)
where dd.debt_default_no like 'DDEF20260721%' and dd.created_by_name = 'system';

update corporate_customer_default cd
join (
  select customer_no, min(default_date) as first_default_date, max(default_date) as latest_default_date
  from corporate_debt_default
  where deleted_flag = 0 and default_status = 'ACTIVE'
  group by customer_no
) defaults_by_customer on defaults_by_customer.customer_no = cd.customer_no
set cd.first_default_date = defaults_by_customer.first_default_date,
    cd.latest_default_date = defaults_by_customer.latest_default_date
where cd.created_by_name = 'system';
