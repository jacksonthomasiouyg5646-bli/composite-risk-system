SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
USE user_management;

CREATE TABLE IF NOT EXISTS risk_data_lineage (
  id bigint primary key auto_increment,
  domain_name varchar(80) not null,
  entity_name varchar(120) not null,
  source_table varchar(120) not null,
  business_key varchar(120) not null,
  key_fields varchar(500) not null,
  freshness_sla_hours int not null default 24,
  sensitivity_level varchar(32) not null default 'INTERNAL',
  enabled tinyint not null default 1,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_risk_data_lineage_table (source_table),
  key idx_risk_data_lineage_domain (domain_name, enabled)
);

CREATE TABLE IF NOT EXISTS risk_data_quality_snapshot (
  id bigint primary key auto_increment,
  snapshot_date date not null,
  quality_score decimal(6,2) not null default 0.00,
  check_total int not null default 0,
  pass_count int not null default 0,
  warning_count int not null default 0,
  failed_count int not null default 0,
  issue_total int not null default 0,
  detail_json json,
  captured_at datetime not null default current_timestamp,
  unique key uk_risk_data_quality_snapshot_date (snapshot_date),
  key idx_risk_data_quality_captured_at (captured_at)
);

CREATE TABLE IF NOT EXISTS risk_model_version (
  id bigint primary key auto_increment,
  version_code varchar(64) not null,
  version_name varchar(160) not null,
  status varchar(32) not null,
  baseline_version_id bigint,
  rule_count int not null default 0,
  created_by varchar(100) not null,
  submitted_by varchar(100),
  approved_by varchar(100),
  approval_comment varchar(1000),
  simulation_summary json,
  simulated_at datetime,
  submitted_at datetime,
  approved_at datetime,
  published_at datetime,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_risk_model_version_code (version_code),
  key idx_risk_model_version_status (status, created_at)
);

CREATE TABLE IF NOT EXISTS risk_model_version_rule (
  id bigint primary key auto_increment,
  version_id bigint not null,
  rule_code varchar(80) not null,
  rule_name varchar(160) not null,
  metric_key varchar(80) not null,
  operator_type varchar(16) not null,
  threshold_value decimal(18,6) not null,
  effect_type varchar(16) not null,
  score_value int not null,
  risk_tag varchar(120),
  reason_template varchar(500),
  enabled tinyint not null default 1,
  sort_order int not null default 100,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_risk_model_version_rule (version_id, rule_code),
  key idx_risk_model_version_rule_version (version_id, sort_order),
  constraint fk_risk_model_version_rule_version foreign key (version_id) references risk_model_version(id) on delete cascade
);

CREATE TABLE IF NOT EXISTS risk_model_approval_log (
  id bigint primary key auto_increment,
  version_id bigint not null,
  action_type varchar(32) not null,
  decision varchar(32) not null,
  operator varchar(100) not null,
  comment varchar(1000),
  created_at datetime not null default current_timestamp,
  key idx_risk_model_approval_version (version_id, created_at),
  constraint fk_risk_model_approval_version foreign key (version_id) references risk_model_version(id) on delete cascade
);

CREATE TABLE IF NOT EXISTS risk_alert_case (
  id bigint primary key auto_increment,
  customer_no varchar(32) not null,
  risk_code varchar(80) not null,
  priority varchar(16) not null,
  risk_score int not null,
  alert_state varchar(32) not null,
  owner varchar(120),
  sla_due_at datetime not null,
  escalation_level int not null default 0,
  first_detected_at datetime not null default current_timestamp,
  last_detected_at datetime not null default current_timestamp,
  started_at datetime,
  closed_at datetime,
  closure_comment varchar(1000),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_risk_alert_case_customer (customer_no),
  key idx_risk_alert_case_state_due (alert_state, sla_due_at),
  key idx_risk_alert_case_priority (priority, last_detected_at)
);

CREATE TABLE IF NOT EXISTS risk_stress_test_run (
  id bigint primary key auto_increment,
  scenario_code varchar(64) not null,
  scenario_name varchar(160) not null,
  parameter_json json not null,
  summary_json json not null,
  run_by varchar(100) not null,
  created_at datetime not null default current_timestamp,
  key idx_risk_stress_test_run_created (created_at)
);

INSERT INTO risk_data_lineage (domain_name, entity_name, source_table, business_key, key_fields, freshness_sla_hours, sensitivity_level, enabled)
VALUES
  ('CORPORATE_CREDIT', '客户主数据', 'corporate_customer', 'customer_no', 'customer_no,customer_name,industry_name,owner_org_name', 24, 'CONFIDENTIAL', 1),
  ('CORPORATE_CREDIT', '客户评级', 'corporate_rating', 'rating_no', 'rating_no,customer_no_snapshot,rating_level,rating_status', 24, 'CONFIDENTIAL', 1),
  ('CORPORATE_CREDIT', '授信额度', 'corporate_credit_limit', 'limit_no', 'limit_no,customer_no_snapshot,total_limit_amount,limit_status', 24, 'CONFIDENTIAL', 1),
  ('CORPORATE_CREDIT', '信贷申请', 'corporate_credit_application', 'application_no', 'application_no,customer_no_snapshot,apply_amount,application_status', 24, 'CONFIDENTIAL', 1),
  ('CORPORATE_CREDIT', '信贷合同', 'corporate_credit_contract', 'contract_no', 'contract_no,application_no_snapshot,customer_no_snapshot,contract_amount', 24, 'CONFIDENTIAL', 1),
  ('CORPORATE_CREDIT', '债项支用', 'corporate_credit_drawdown', 'drawdown_no', 'drawdown_no,debt_no,contract_no_snapshot,customer_no_snapshot', 24, 'CONFIDENTIAL', 1),
  ('CORPORATE_CREDIT', '押品信息', 'corporate_credit_collateral', 'collateral_no', 'collateral_no,customer_no,collateral_type,confirmed_value', 48, 'CONFIDENTIAL', 1),
  ('CORPORATE_CREDIT', '合同押品关系', 'corporate_credit_contract_collateral', 'contract_no,collateral_no', 'contract_no,collateral_no,customer_no,secured_amount', 24, 'CONFIDENTIAL', 1),
  ('RISK_MEASUREMENT', '风险敞口', 'corporate_risk_exposure', 'exposure_no', 'exposure_no,customer_no,contract_no,debt_no,pd,lgd_avg,ead_amount', 24, 'CONFIDENTIAL', 1),
  ('RISK_EVENT', '贷款逾期', 'corporate_credit_overdue', 'overdue_no', 'overdue_no,customer_no,contract_no,debt_no,overdue_days', 24, 'CONFIDENTIAL', 1),
  ('RISK_EVENT', '债项违约', 'corporate_debt_default', 'debt_default_no', 'debt_default_no,customer_no,contract_no,debt_no,default_level', 24, 'CONFIDENTIAL', 1),
  ('RISK_EVENT', '客户违约', 'corporate_customer_default', 'customer_default_no', 'customer_default_no,customer_no,default_status,default_date', 24, 'CONFIDENTIAL', 1)
ON DUPLICATE KEY UPDATE
  entity_name = values(entity_name), business_key = values(business_key), key_fields = values(key_fields),
  freshness_sla_hours = values(freshness_sla_hours), sensitivity_level = values(sensitivity_level), enabled = values(enabled);
