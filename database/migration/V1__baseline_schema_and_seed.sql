/*!40101 SET NAMES utf8mb4 */;
create database if not exists user_management default character set utf8mb4 collate utf8mb4_0900_ai_ci;
use user_management;

drop table if exists sys_role_permission;
drop table if exists sys_user_role;
drop table if exists sys_user;
drop table if exists sys_role;
drop table if exists sys_permission;
drop table if exists sys_department;
drop table if exists sys_post;
drop table if exists sys_menu;
drop table if exists sys_login_log;
drop table if exists sys_operation_log;
drop table if exists sys_error_log;
drop table if exists sys_notification;
drop table if exists sys_config;
drop table if exists sys_security_policy;
drop table if exists sys_tenant;
drop table if exists corporate_credit_contract_collateral;
drop table if exists corporate_customer_default;
drop table if exists corporate_debt_default;
drop table if exists corporate_risk_exposure;
drop table if exists corporate_credit_overdue;
drop table if exists corporate_credit_drawdown;
drop table if exists corporate_credit_collateral;
drop table if exists corporate_credit_contract;
drop table if exists corporate_credit_application;
drop table if exists corporate_credit_limit;
drop table if exists corporate_rating;
drop table if exists corporate_customer;
drop table if exists risk_month_end_quality_issue;
drop table if exists risk_month_end_source_manifest;
drop table if exists risk_month_end_reconciliation;
drop table if exists risk_month_end_change_detail;
drop table if exists risk_month_end_summary;
drop table if exists risk_month_end_customer_snapshot;
drop table if exists risk_month_end_exposure_snapshot;
drop table if exists risk_month_end_batch;
drop table if exists risk_alert_effectiveness_snapshot;
drop table if exists risk_user_workbench_preference;
drop table if exists risk_customer_group_member;
drop table if exists risk_customer_group;
drop table if exists risk_portfolio_stress_result;
drop table if exists risk_stress_scenario_definition;
drop table if exists risk_alert_case_link;
drop table if exists risk_portfolio_limit_snapshot;
drop table if exists risk_portfolio_limit;
drop table if exists risk_model_backtest_run;
drop table if exists risk_indicator;
drop table if exists risk_alert_subscription;
drop table if exists risk_scoring_rule;
drop table if exists risk_lgd_stress_test_run;
drop table if exists risk_lgd_calculation_run;
drop table if exists risk_lgd_segment_parameter;
drop table if exists risk_lgd_model_version;
drop table if exists risk_stress_test_run;
drop table if exists risk_alert_case;
drop table if exists risk_model_approval_log;
drop table if exists risk_model_version_rule;
drop table if exists risk_model_version;
drop table if exists risk_data_quality_snapshot;
drop table if exists risk_data_lineage;
drop table if exists risk_model_monitor_snapshot;
drop table if exists risk_ai_chat_log;
drop table if exists risk_external_data_access_log;
drop table if exists risk_event;
drop table if exists risk_treatment_plan;
drop table if exists risk_control_measure;
drop table if exists risk_assessment;
drop table if exists risk_register;

create table sys_tenant (
  id bigint primary key auto_increment,
  name varchar(100) not null,
  code varchar(60) not null unique,
  contact_name varchar(60),
  contact_phone varchar(30),
  status varchar(20) not null default 'ENABLED',
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table sys_department (
  id bigint primary key auto_increment,
  tenant_id bigint not null default 1,
  parent_id bigint default 0,
  name varchar(100) not null,
  code varchar(60) not null,
  leader varchar(60),
  phone varchar(30),
  sort_order int not null default 0,
  status varchar(20) not null default 'ENABLED',
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table sys_post (
  id bigint primary key auto_increment,
  tenant_id bigint not null default 1,
  name varchar(100) not null,
  code varchar(60) not null,
  sort_order int not null default 0,
  status varchar(20) not null default 'ENABLED',
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table sys_user (
  id bigint primary key auto_increment,
  tenant_id bigint not null default 1,
  department_id bigint,
  post_id bigint,
  username varchar(60) not null unique,
  password_hash varchar(128) not null,
  display_name varchar(100) not null,
  email varchar(120),
  phone varchar(30),
  avatar varchar(255),
  status varchar(20) not null default 'ENABLED',
  last_login_at datetime,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table sys_role (
  id bigint primary key auto_increment,
  tenant_id bigint not null default 1,
  name varchar(100) not null,
  code varchar(60) not null unique,
  description varchar(255),
  status varchar(20) not null default 'ENABLED',
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table sys_permission (
  id bigint primary key auto_increment,
  name varchar(100) not null,
  code varchar(100) not null unique,
  module varchar(60) not null,
  description varchar(255),
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table sys_user_role (
  user_id bigint not null,
  role_id bigint not null,
  primary key (user_id, role_id)
);

create table sys_role_permission (
  role_id bigint not null,
  permission_id bigint not null,
  primary key (role_id, permission_id)
);

create table sys_menu (
  id bigint primary key auto_increment,
  parent_id bigint default 0,
  title varchar(100) not null,
  path varchar(160) not null,
  component varchar(160),
  icon varchar(60),
  permission_code varchar(100),
  sort_order int not null default 0,
  visible tinyint not null default 1,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table sys_login_log (
  id bigint primary key auto_increment,
  username varchar(60),
  ip_address varchar(60),
  user_agent varchar(255),
  status varchar(20),
  message varchar(255),
  created_at datetime not null default current_timestamp
);

create table sys_operation_log (
  id bigint primary key auto_increment,
  username varchar(60),
  module varchar(60),
  action varchar(120),
  method varchar(20),
  request_uri varchar(255),
  status varchar(20),
  created_at datetime not null default current_timestamp
);

create table sys_error_log (
  id bigint primary key auto_increment,
  service_name varchar(80),
  trace_id varchar(80),
  message varchar(500),
  stack_trace text,
  created_at datetime not null default current_timestamp
);

create table sys_notification (
  id bigint primary key auto_increment,
  title varchar(160) not null,
  content text,
  channel varchar(40) not null default 'SYSTEM',
  target_type varchar(40) not null default 'ALL',
  status varchar(20) not null default 'DRAFT',
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table sys_config (
  id bigint primary key auto_increment,
  config_key varchar(120) not null unique,
  config_value varchar(500),
  description varchar(255),
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table sys_security_policy (
  id bigint primary key auto_increment,
  name varchar(100) not null,
  policy_key varchar(120) not null unique,
  policy_value varchar(500) not null,
  enabled tinyint not null default 1,
  description varchar(255),
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table corporate_customer (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  customer_no varchar(32) not null,
  customer_name varchar(200) not null,
  customer_short_name varchar(100),
  unified_social_credit_code varchar(18),
  registration_no varchar(64),
  taxpayer_no varchar(64),
  company_type varchar(50),
  industry_code varchar(32),
  industry_name varchar(100),
  registered_capital decimal(18,2),
  capital_currency varchar(8) default 'CNY',
  established_date date,
  business_term_start date,
  business_term_end date,
  registered_address varchar(300),
  business_address varchar(300),
  business_scope text,
  legal_representative_name varchar(100),
  legal_representative_id_type varchar(32),
  legal_representative_id_no_enc varchar(512),
  legal_representative_id_no_hash varchar(128),
  contact_name varchar(100),
  contact_title varchar(100),
  contact_mobile varchar(32),
  contact_phone varchar(32),
  contact_email varchar(128),
  bank_name varchar(150),
  bank_branch_name varchar(150),
  bank_account_name varchar(200),
  bank_account_no_enc varchar(512),
  bank_account_no_hash varchar(128),
  customer_type varchar(32) default 'NORMAL',
  customer_level varchar(32),
  source_channel varchar(64),
  relationship_manager_id bigint unsigned,
  relationship_manager_name varchar(100),
  owner_org_id bigint unsigned,
  owner_org_name varchar(150),
  kyc_status varchar(32) not null default 'PENDING',
  risk_level varchar(32) default 'LOW',
  blacklist_flag tinyint not null default 0,
  compliance_remark varchar(500),
  status varchar(32) not null default 'ACTIVE',
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  primary key (id),
  unique key uk_corporate_customer_no (customer_no),
  unique key uk_corporate_customer_credit_code (unified_social_credit_code),
  key idx_corporate_customer_name (customer_name),
  key idx_corporate_customer_mobile (contact_mobile),
  key idx_corporate_customer_legal_hash (legal_representative_id_no_hash),
  key idx_corporate_customer_bank_hash (bank_account_no_hash),
  key idx_corporate_customer_kyc (kyc_status),
  key idx_corporate_customer_risk (risk_level),
  key idx_corporate_customer_status (status),
  key idx_corporate_customer_org (owner_org_id),
  key idx_corporate_customer_created_at (created_at)
);

create table corporate_credit_collateral (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  collateral_no varchar(32) not null,
  customer_no varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  collateral_name varchar(200) not null,
  collateral_type varchar(64) not null,
  ownership_type varchar(32) default 'OWNED',
  owner_name varchar(200),
  ownership_cert_no varchar(128),
  currency varchar(8) not null default 'CNY',
  original_value decimal(18,2),
  appraisal_value decimal(18,2) not null default 0.00,
  confirmed_value decimal(18,2) not null default 0.00,
  mortgage_rate decimal(8,4),
  available_secured_amount decimal(18,2) not null default 0.00,
  appraisal_org_name varchar(200),
  appraisal_date date,
  appraisal_expiry_date date,
  registration_flag tinyint not null default 0,
  registration_no varchar(128),
  registration_date date,
  custody_org_name varchar(200),
  insurance_flag tinyint not null default 0,
  insurance_policy_no varchar(128),
  insurance_expiry_date date,
  location_desc varchar(500),
  risk_level varchar(32) default 'LOW',
  collateral_status varchar(32) not null default 'ACTIVE',
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  primary key (id),
  unique key uk_corporate_credit_collateral_no (collateral_no),
  key idx_corporate_credit_collateral_customer_no (customer_no),
  key idx_corporate_credit_collateral_type (collateral_type),
  key idx_corporate_credit_collateral_status (collateral_status),
  key idx_corporate_credit_collateral_risk (risk_level),
  constraint fk_corporate_credit_collateral_customer_no
    foreign key (customer_no) references corporate_customer (customer_no),
  constraint ck_corporate_credit_collateral_amount
    check (
      appraisal_value >= 0
      and confirmed_value >= 0
      and available_secured_amount >= 0
    )
);

create table corporate_rating (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  rating_no varchar(32) not null,
  customer_id bigint unsigned not null,
  customer_no_snapshot varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  rating_type varchar(32) not null default 'REGULAR',
  rating_source varchar(32) not null default 'MANUAL',
  rating_model varchar(32) not null default 'INTERNAL',
  quantitative_score decimal(8,2),
  qualitative_score decimal(8,2),
  rating_score decimal(8,2),
  rating_level varchar(20) not null,
  outlook varchar(20) default 'STABLE',
  rating_date date not null,
  valid_from date,
  valid_to date,
  evaluator_id bigint unsigned,
  evaluator_name varchar(100),
  review_org_id bigint unsigned,
  review_org_name varchar(150),
  approval_no varchar(64),
  rating_status varchar(32) not null default 'DRAFT',
  key_risk_factors text,
  rating_basis text,
  rating_detail_json json,
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  active_customer_no varchar(32) generated always as (
    case when rating_status = 'APPROVED' and deleted_flag = 0 then customer_no_snapshot else null end
  ) stored,
  primary key (id),
  unique key uk_corporate_rating_no (rating_no),
  unique key uk_corporate_rating_active_customer (active_customer_no),
  key idx_corporate_rating_customer_date (customer_id, rating_date),
  key idx_corporate_rating_customer_no (customer_no_snapshot),
  key idx_corporate_rating_level (rating_level),
  key idx_corporate_rating_status (rating_status),
  key idx_corporate_rating_valid_to (valid_to),
  key idx_corporate_rating_tenant (tenant_id),
  constraint fk_corporate_rating_customer
    foreign key (customer_id) references corporate_customer (id),
  constraint fk_corporate_rating_customer_no
    foreign key (customer_no_snapshot) references corporate_customer (customer_no)
);

create table corporate_credit_limit (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  limit_no varchar(32) not null,
  customer_id bigint unsigned not null,
  customer_no_snapshot varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  limit_type varchar(32) not null default 'COMPREHENSIVE',
  currency varchar(8) not null default 'CNY',
  total_limit_amount decimal(18,2) not null default 0.00,
  used_limit_amount decimal(18,2) not null default 0.00,
  frozen_limit_amount decimal(18,2) not null default 0.00,
  available_limit_amount decimal(18,2) not null default 0.00,
  revolving_flag tinyint not null default 1,
  secured_flag tinyint not null default 0,
  guarantee_type varchar(64),
  approval_no varchar(64),
  approval_date date,
  effective_date date not null,
  expiry_date date not null,
  credit_rating_level varchar(20),
  risk_level varchar(32) default 'LOW',
  limit_status varchar(32) not null default 'ACTIVE',
  manager_id bigint unsigned,
  manager_name varchar(100),
  owner_org_id bigint unsigned,
  owner_org_name varchar(150),
  limit_purpose varchar(300),
  risk_mitigation text,
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  active_customer_no varchar(32) generated always as (
    case when limit_status = 'ACTIVE' and deleted_flag = 0 then customer_no_snapshot else null end
  ) stored,
  primary key (id),
  unique key uk_corporate_credit_limit_no (limit_no),
  unique key uk_corporate_credit_limit_active_customer (active_customer_no),
  key idx_corporate_credit_limit_customer (customer_id),
  key idx_corporate_credit_limit_customer_no (customer_no_snapshot),
  key idx_corporate_credit_limit_status (limit_status),
  key idx_corporate_credit_limit_expiry (expiry_date),
  key idx_corporate_credit_limit_risk (risk_level),
  key idx_corporate_credit_limit_org (owner_org_id),
  key idx_corporate_credit_limit_tenant (tenant_id),
  constraint fk_corporate_credit_limit_customer
    foreign key (customer_id) references corporate_customer (id),
  constraint fk_corporate_credit_limit_customer_no
    foreign key (customer_no_snapshot) references corporate_customer (customer_no),
  constraint ck_corporate_credit_limit_amount
    check (
      total_limit_amount >= 0
      and used_limit_amount >= 0
      and frozen_limit_amount >= 0
      and available_limit_amount >= 0
    )
);

create table corporate_credit_application (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  application_no varchar(32) not null,
  customer_id bigint unsigned not null,
  customer_no_snapshot varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  application_type varchar(32) not null default 'NEW_LIMIT',
  product_type varchar(64) not null,
  related_limit_id bigint unsigned,
  currency varchar(8) not null default 'CNY',
  apply_amount decimal(18,2) not null default 0.00,
  apply_term_months int,
  apply_rate decimal(8,4),
  repayment_method varchar(64),
  fund_usage varchar(300),
  guarantee_type varchar(64),
  collateral_desc varchar(500),
  guarantor_desc varchar(500),
  latest_rating_level varchar(20),
  customer_risk_level varchar(32),
  risk_assessment_result varchar(32),
  risk_assessment_remark text,
  application_status varchar(32) not null default 'DRAFT',
  submitted_at datetime,
  approved_at datetime,
  rejected_at datetime,
  applicant_id bigint unsigned,
  applicant_name varchar(100),
  manager_id bigint unsigned,
  manager_name varchar(100),
  owner_org_id bigint unsigned,
  owner_org_name varchar(150),
  approval_no varchar(64),
  approval_opinion text,
  document_list_json json,
  application_detail_json json,
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  primary key (id),
  unique key uk_corporate_credit_application_no (application_no),
  key idx_corporate_credit_application_customer (customer_id),
  key idx_corporate_credit_application_customer_no (customer_no_snapshot),
  key idx_corporate_credit_application_limit (related_limit_id),
  key idx_corporate_credit_application_status (application_status),
  key idx_corporate_credit_application_type (application_type),
  key idx_corporate_credit_application_org (owner_org_id),
  key idx_corporate_credit_application_created_at (created_at),
  constraint fk_corporate_credit_application_customer
    foreign key (customer_id) references corporate_customer (id),
  constraint fk_corporate_credit_application_customer_no
    foreign key (customer_no_snapshot) references corporate_customer (customer_no),
  constraint fk_corporate_credit_application_limit
    foreign key (related_limit_id) references corporate_credit_limit (id),
  constraint ck_corporate_credit_application_amount
    check (apply_amount >= 0)
);

create table corporate_credit_contract (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  contract_no varchar(32) not null,
  contract_name varchar(200) not null,
  customer_id bigint unsigned not null,
  customer_no_snapshot varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  application_id bigint unsigned,
  application_no_snapshot varchar(32) not null,
  limit_id bigint unsigned,
  limit_no_snapshot varchar(32),
  contract_type varchar(32) not null default 'FRAME',
  product_type varchar(64) not null,
  currency varchar(8) not null default 'CNY',
  contract_amount decimal(18,2) not null default 0.00,
  available_draw_amount decimal(18,2) not null default 0.00,
  used_draw_amount decimal(18,2) not null default 0.00,
  frozen_amount decimal(18,2) not null default 0.00,
  interest_rate decimal(8,4),
  rate_type varchar(32),
  repayment_method varchar(64),
  loan_term_months int,
  purpose varchar(300),
  guarantee_type varchar(64),
  collateral_desc varchar(500),
  guarantor_desc varchar(500),
  party_a_name varchar(200),
  party_b_name varchar(200),
  signatory_a varchar(100),
  signatory_b varchar(100),
  sign_method varchar(32) default 'ONLINE',
  sign_channel varchar(32) default 'SYSTEM',
  sign_date date,
  effective_date date not null,
  expiry_date date not null,
  approval_no varchar(64),
  approval_date date,
  contract_status varchar(32) not null default 'DRAFT',
  manager_id bigint unsigned,
  manager_name varchar(100),
  owner_org_id bigint unsigned,
  owner_org_name varchar(150),
  contract_file_url varchar(500),
  contract_file_hash varchar(128),
  contract_detail_json json,
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  primary key (id),
  unique key uk_corporate_credit_contract_no (contract_no),
  unique key uk_corporate_credit_contract_application_no (application_no_snapshot),
  key idx_corporate_credit_contract_customer (customer_id),
  key idx_corporate_credit_contract_customer_no (customer_no_snapshot),
  key idx_corporate_credit_contract_application (application_id),
  key idx_corporate_credit_contract_limit (limit_id),
  key idx_corporate_credit_contract_status (contract_status),
  key idx_corporate_credit_contract_expiry (expiry_date),
  key idx_corporate_credit_contract_org (owner_org_id),
  key idx_corporate_credit_contract_tenant (tenant_id),
  constraint fk_corporate_credit_contract_customer
    foreign key (customer_id) references corporate_customer (id),
  constraint fk_corporate_credit_contract_customer_no
    foreign key (customer_no_snapshot) references corporate_customer (customer_no),
  constraint fk_corporate_credit_contract_application
    foreign key (application_id) references corporate_credit_application (id),
  constraint fk_corporate_credit_contract_application_no
    foreign key (application_no_snapshot) references corporate_credit_application (application_no),
  constraint fk_corporate_credit_contract_limit
    foreign key (limit_id) references corporate_credit_limit (id),
  constraint ck_corporate_credit_contract_amount
    check (
      contract_amount >= 0
      and available_draw_amount >= 0
      and used_draw_amount >= 0
      and frozen_amount >= 0
    ),
  constraint ck_corporate_credit_contract_date
    check (expiry_date >= effective_date)
);

create table corporate_credit_contract_collateral (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  relation_no varchar(32) not null,
  contract_no varchar(32) not null,
  collateral_no varchar(32) not null,
  customer_no varchar(32) not null,
  secured_amount decimal(18,2) not null default 0.00,
  pledge_rate decimal(8,4),
  relation_type varchar(32) not null default 'MORTGAGE',
  priority_order int not null default 1,
  effective_date date not null,
  expiry_date date,
  release_date date,
  relation_status varchar(32) not null default 'ACTIVE',
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  primary key (id),
  unique key uk_corporate_contract_collateral_relation_no (relation_no),
  unique key uk_corporate_contract_collateral_pair (contract_no, collateral_no),
  key idx_corporate_contract_collateral_contract (contract_no),
  key idx_corporate_contract_collateral_collateral (collateral_no),
  key idx_corporate_contract_collateral_customer (customer_no),
  key idx_corporate_contract_collateral_status (relation_status),
  constraint fk_corporate_contract_collateral_contract_no
    foreign key (contract_no) references corporate_credit_contract (contract_no),
  constraint fk_corporate_contract_collateral_collateral_no
    foreign key (collateral_no) references corporate_credit_collateral (collateral_no),
  constraint fk_corporate_contract_collateral_customer_no
    foreign key (customer_no) references corporate_customer (customer_no),
  constraint ck_corporate_contract_collateral_amount
    check (secured_amount >= 0),
  constraint ck_corporate_contract_collateral_date
    check (expiry_date is null or expiry_date >= effective_date)
);

create table corporate_credit_drawdown (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  drawdown_no varchar(32) not null,
  debt_no varchar(32),
  customer_id bigint unsigned not null,
  customer_no_snapshot varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  contract_id bigint unsigned not null,
  contract_no_snapshot varchar(32) not null,
  application_id bigint unsigned,
  application_no_snapshot varchar(32),
  limit_id bigint unsigned,
  limit_no_snapshot varchar(32),
  product_type varchar(64) not null,
  drawdown_type varchar(32) not null default 'LOAN',
  currency varchar(8) not null default 'CNY',
  apply_draw_amount decimal(18,2) not null default 0.00,
  approved_draw_amount decimal(18,2) not null default 0.00,
  actual_draw_amount decimal(18,2) not null default 0.00,
  repaid_principal_amount decimal(18,2) not null default 0.00,
  outstanding_principal_amount decimal(18,2) not null default 0.00,
  interest_receivable_amount decimal(18,2) not null default 0.00,
  interest_paid_amount decimal(18,2) not null default 0.00,
  overdue_principal_amount decimal(18,2) not null default 0.00,
  overdue_interest_amount decimal(18,2) not null default 0.00,
  interest_rate decimal(8,4),
  rate_type varchar(32),
  repayment_method varchar(64),
  interest_payment_method varchar(64),
  drawdown_date date not null,
  value_date date,
  maturity_date date not null,
  term_days int,
  fund_usage varchar(300),
  payee_name varchar(200),
  payee_bank_name varchar(150),
  payee_account_no_enc varchar(512),
  payee_account_no_hash varchar(128),
  loan_account_no_enc varchar(512),
  loan_account_no_hash varchar(128),
  guarantee_type varchar(64),
  collateral_desc varchar(500),
  guarantor_desc varchar(500),
  five_category varchar(32) default 'NORMAL',
  risk_level varchar(32) default 'LOW',
  overdue_days int not null default 0,
  drawdown_status varchar(32) not null default 'DRAFT',
  approval_no varchar(64),
  approval_date date,
  disbursement_voucher_no varchar(64),
  manager_id bigint unsigned,
  manager_name varchar(100),
  owner_org_id bigint unsigned,
  owner_org_name varchar(150),
  drawdown_detail_json json,
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  primary key (id),
  unique key uk_corporate_credit_drawdown_no (drawdown_no),
  key idx_corporate_credit_drawdown_debt_no (debt_no),
  key idx_corporate_credit_drawdown_customer (customer_id),
  key idx_corporate_credit_drawdown_contract (contract_id),
  key idx_corporate_credit_drawdown_contract_no (contract_no_snapshot),
  key idx_corporate_credit_drawdown_application (application_id),
  key idx_corporate_credit_drawdown_limit (limit_id),
  key idx_corporate_credit_drawdown_status (drawdown_status),
  key idx_corporate_credit_drawdown_maturity (maturity_date),
  key idx_corporate_credit_drawdown_five_category (five_category),
  key idx_corporate_credit_drawdown_risk (risk_level),
  key idx_corporate_credit_drawdown_org (owner_org_id),
  key idx_corporate_credit_drawdown_tenant (tenant_id),
  constraint fk_corporate_credit_drawdown_customer
    foreign key (customer_id) references corporate_customer (id),
  constraint fk_corporate_credit_drawdown_contract
    foreign key (contract_id) references corporate_credit_contract (id),
  constraint fk_corporate_credit_drawdown_contract_no
    foreign key (contract_no_snapshot) references corporate_credit_contract (contract_no),
  constraint fk_corporate_credit_drawdown_application
    foreign key (application_id) references corporate_credit_application (id),
  constraint fk_corporate_credit_drawdown_limit
    foreign key (limit_id) references corporate_credit_limit (id),
  constraint ck_corporate_credit_drawdown_amount
    check (
      apply_draw_amount >= 0
      and approved_draw_amount >= 0
      and actual_draw_amount >= 0
      and repaid_principal_amount >= 0
      and outstanding_principal_amount >= 0
      and interest_receivable_amount >= 0
      and interest_paid_amount >= 0
      and overdue_principal_amount >= 0
      and overdue_interest_amount >= 0
    ),
  constraint ck_corporate_credit_drawdown_date
    check (maturity_date >= drawdown_date),
  constraint ck_corporate_credit_drawdown_overdue_days
    check (overdue_days >= 0)
);

create table corporate_credit_overdue (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  overdue_no varchar(32) not null,
  customer_no varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  contract_no varchar(32) not null,
  debt_no varchar(32) not null,
  drawdown_no varchar(32),
  installment_no varchar(64),
  product_type varchar(64) not null,
  currency varchar(8) not null default 'CNY',
  due_date date not null,
  overdue_date date not null,
  overdue_days int not null default 0,
  grace_days int not null default 0,
  grace_due_date date,
  legal_holiday_flag tinyint not null default 0,
  holiday_calendar_code varchar(64),
  legal_holiday_name varchar(100),
  principal_due_amount decimal(18,2) not null default 0.00,
  interest_due_amount decimal(18,2) not null default 0.00,
  overdue_principal_amount decimal(18,2) not null default 0.00,
  overdue_interest_amount decimal(18,2) not null default 0.00,
  penalty_interest_amount decimal(18,2) not null default 0.00,
  compound_interest_amount decimal(18,2) not null default 0.00,
  fee_overdue_amount decimal(18,2) not null default 0.00,
  total_overdue_amount decimal(18,2) not null default 0.00,
  repaid_overdue_amount decimal(18,2) not null default 0.00,
  remaining_overdue_amount decimal(18,2) not null default 0.00,
  last_repay_date date,
  resolved_date date,
  overdue_stage varchar(32) not null default 'M1',
  collection_status varchar(32) not null default 'PENDING',
  repayment_status varchar(32) not null default 'OVERDUE',
  default_flag tinyint not null default 0,
  extension_flag tinyint not null default 0,
  five_category varchar(32) default 'NORMAL',
  risk_level varchar(32) default 'LOW',
  manager_id bigint unsigned,
  manager_name varchar(100),
  owner_org_id bigint unsigned,
  owner_org_name varchar(150),
  data_date date not null,
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  primary key (id),
  unique key uk_corporate_credit_overdue_no (overdue_no),
  unique key uk_corporate_credit_overdue_debt_due (debt_no, due_date, installment_no),
  key idx_corporate_credit_overdue_customer (customer_no),
  key idx_corporate_credit_overdue_contract (contract_no),
  key idx_corporate_credit_overdue_drawdown (drawdown_no),
  key idx_corporate_credit_overdue_debt (debt_no),
  key idx_corporate_credit_overdue_due_date (due_date),
  key idx_corporate_credit_overdue_overdue_date (overdue_date),
  key idx_corporate_credit_overdue_grace_due_date (grace_due_date),
  key idx_corporate_credit_overdue_status (repayment_status),
  key idx_corporate_credit_overdue_collection (collection_status),
  key idx_corporate_credit_overdue_risk (risk_level),
  key idx_corporate_credit_overdue_data_date (data_date),
  constraint fk_corporate_credit_overdue_customer_no
    foreign key (customer_no) references corporate_customer (customer_no),
  constraint fk_corporate_credit_overdue_contract_no
    foreign key (contract_no) references corporate_credit_contract (contract_no),
  constraint fk_corporate_credit_overdue_drawdown_no
    foreign key (drawdown_no) references corporate_credit_drawdown (drawdown_no),
  constraint ck_corporate_credit_overdue_amount
    check (
      principal_due_amount >= 0
      and interest_due_amount >= 0
      and overdue_principal_amount >= 0
      and overdue_interest_amount >= 0
      and penalty_interest_amount >= 0
      and compound_interest_amount >= 0
      and fee_overdue_amount >= 0
      and total_overdue_amount >= 0
      and repaid_overdue_amount >= 0
      and remaining_overdue_amount >= 0
    ),
  constraint ck_corporate_credit_overdue_days
    check (overdue_days >= 0 and grace_days >= 0),
  constraint ck_corporate_credit_overdue_flag
    check (
      legal_holiday_flag in (0, 1)
      and default_flag in (0, 1)
      and extension_flag in (0, 1)
      and deleted_flag in (0, 1)
    ),
  constraint ck_corporate_credit_overdue_date
    check (
      overdue_date >= due_date
      and (grace_due_date is null or grace_due_date >= overdue_date)
      and (last_repay_date is null or last_repay_date >= due_date)
      and (resolved_date is null or resolved_date >= overdue_date)
    )
);

create table corporate_risk_exposure (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  exposure_no varchar(32) not null,
  customer_no varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  contract_no varchar(32) not null,
  debt_no varchar(32) not null,
  drawdown_no varchar(32),
  product_type varchar(64) not null,
  currency varchar(8) not null default 'CNY',
  exposure_balance decimal(18,2) not null default 0.00,
  ead_amount decimal(18,2) not null default 0.00,
  undrawn_amount decimal(18,2) not null default 0.00,
  collateral_value decimal(18,2) not null default 0.00,
  guaranteed_amount decimal(18,2) not null default 0.00,
  lgd_avg decimal(10,6) not null default 0.000000,
  lgd_downturn decimal(10,6) not null default 0.000000,
  pd decimal(10,6) not null default 0.000000,
  el_avg decimal(18,6) not null default 0.000000,
  el_downturn decimal(18,6) not null default 0.000000,
  rating_result varchar(20),
  product_recovery_rate decimal(10,6) not null default 0.000000,
  recovery_source varchar(64),
  five_category varchar(32) default 'NORMAL',
  risk_level varchar(32) default 'LOW',
  overdue_days int not null default 0,
  default_flag tinyint not null default 0,
  measurement_date date not null,
  data_date date not null,
  model_name varchar(100),
  model_version varchar(64),
  scenario_type varchar(32) default 'BASE',
  exposure_status varchar(32) not null default 'ACTIVE',
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  primary key (id),
  unique key uk_corporate_risk_exposure_no (exposure_no),
  unique key uk_corporate_risk_exposure_debt_date (debt_no, data_date),
  key idx_corporate_risk_exposure_customer (customer_no),
  key idx_corporate_risk_exposure_contract (contract_no),
  key idx_corporate_risk_exposure_drawdown (drawdown_no),
  key idx_corporate_risk_exposure_rating (rating_result),
  key idx_corporate_risk_exposure_level (risk_level),
  key idx_corporate_risk_exposure_status (exposure_status),
  key idx_corporate_risk_exposure_measurement_date (measurement_date),
  constraint fk_corporate_risk_exposure_customer_no
    foreign key (customer_no) references corporate_customer (customer_no),
  constraint fk_corporate_risk_exposure_contract_no
    foreign key (contract_no) references corporate_credit_contract (contract_no),
  constraint fk_corporate_risk_exposure_drawdown_no
    foreign key (drawdown_no) references corporate_credit_drawdown (drawdown_no),
  constraint ck_corporate_risk_exposure_amount
    check (
      exposure_balance >= 0
      and ead_amount >= 0
      and undrawn_amount >= 0
      and collateral_value >= 0
      and guaranteed_amount >= 0
      and el_avg >= 0
      and el_downturn >= 0
    ),
  constraint ck_corporate_risk_exposure_ratio
    check (
      lgd_avg >= 0 and lgd_avg <= 1
      and lgd_downturn >= 0 and lgd_downturn <= 1
      and pd >= 0 and pd <= 1
      and product_recovery_rate >= 0 and product_recovery_rate <= 1
    ),
  constraint ck_corporate_risk_exposure_overdue_days
    check (overdue_days >= 0)
);

create table corporate_debt_default (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  debt_default_no varchar(32) not null,
  customer_no varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  contract_no varchar(32) not null,
  debt_no varchar(32) not null,
  drawdown_no varchar(32),
  exposure_no varchar(32),
  product_type varchar(64) not null,
  currency varchar(8) not null default 'CNY',
  default_level char(1) not null,
  default_date date not null,
  default_recognition_date date not null,
  overdue_days int not null default 0,
  overdue_date date,
  five_category varchar(32),
  rating_result varchar(20),
  overdue_default_flag tinyint not null default 0,
  five_category_default_flag tinyint not null default 0,
  rating_default_flag tinyint not null default 0,
  default_principal_amount decimal(18,2) not null default 0.00,
  default_interest_amount decimal(18,2) not null default 0.00,
  default_exposure_amount decimal(18,2) not null default 0.00,
  recovered_amount decimal(18,2) not null default 0.00,
  loss_amount decimal(18,2) not null default 0.00,
  default_reason varchar(500),
  default_source varchar(64) not null default 'SYSTEM_RULE',
  default_status varchar(32) not null default 'ACTIVE',
  cure_date date,
  resolved_date date,
  manager_id bigint unsigned,
  manager_name varchar(100),
  owner_org_id bigint unsigned,
  owner_org_name varchar(150),
  data_date date not null,
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  active_debt_no varchar(32) generated always as (
    case when default_status = 'ACTIVE' and deleted_flag = 0 then debt_no else null end
  ) stored,
  primary key (id),
  unique key uk_corporate_debt_default_no (debt_default_no),
  unique key uk_corporate_debt_default_active_debt (active_debt_no),
  key idx_corporate_debt_default_customer (customer_no),
  key idx_corporate_debt_default_contract (contract_no),
  key idx_corporate_debt_default_debt (debt_no),
  key idx_corporate_debt_default_drawdown (drawdown_no),
  key idx_corporate_debt_default_exposure (exposure_no),
  key idx_corporate_debt_default_level (default_level),
  key idx_corporate_debt_default_date (default_date),
  key idx_corporate_debt_default_status (default_status),
  key idx_corporate_debt_default_data_date (data_date),
  constraint fk_corporate_debt_default_customer_no
    foreign key (customer_no) references corporate_customer (customer_no),
  constraint fk_corporate_debt_default_contract_no
    foreign key (contract_no) references corporate_credit_contract (contract_no),
  constraint fk_corporate_debt_default_drawdown_no
    foreign key (drawdown_no) references corporate_credit_drawdown (drawdown_no),
  constraint ck_corporate_debt_default_level
    check (default_level in ('A', 'B', 'C')),
  constraint ck_corporate_debt_default_rule
    check (
      (default_level = 'A' and overdue_days > 90)
      or (default_level = 'B' and coalesce(five_category, '') in ('SUBSTANDARD', 'DOUBTFUL', 'LOSS'))
      or (default_level = 'C' and rating_result = '21')
    ),
  constraint ck_corporate_debt_default_reason_flag
    check (
      overdue_default_flag = 1
      or five_category_default_flag = 1
      or rating_default_flag = 1
    ),
  constraint ck_corporate_debt_default_amount
    check (
      default_principal_amount >= 0
      and default_interest_amount >= 0
      and default_exposure_amount >= 0
      and recovered_amount >= 0
      and loss_amount >= 0
    ),
  constraint ck_corporate_debt_default_days
    check (overdue_days >= 0),
  constraint ck_corporate_debt_default_flag
    check (
      overdue_default_flag in (0, 1)
      and five_category_default_flag in (0, 1)
      and rating_default_flag in (0, 1)
      and deleted_flag in (0, 1)
    ),
  constraint ck_corporate_debt_default_date
    check (
      default_recognition_date >= default_date
      and (overdue_date is null or overdue_date <= default_date)
      and (cure_date is null or cure_date >= default_date)
      and (resolved_date is null or resolved_date >= default_date)
    )
);

create table corporate_customer_default (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  customer_default_no varchar(32) not null,
  customer_no varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  default_flag tinyint not null default 1,
  first_default_date date not null,
  latest_default_date date not null,
  highest_default_level char(1) not null,
  active_default_debt_count int not null default 0,
  total_default_debt_count int not null default 0,
  overdue90_debt_count int not null default 0,
  five_category_default_debt_count int not null default 0,
  rating_default_debt_count int not null default 0,
  default_principal_amount decimal(18,2) not null default 0.00,
  default_interest_amount decimal(18,2) not null default 0.00,
  default_exposure_amount decimal(18,2) not null default 0.00,
  recovered_amount decimal(18,2) not null default 0.00,
  loss_amount decimal(18,2) not null default 0.00,
  default_reason_summary varchar(1000),
  default_source varchar(64) not null default 'DEBT_AGGREGATION',
  default_status varchar(32) not null default 'ACTIVE',
  cure_date date,
  resolved_date date,
  aggregation_batch_no varchar(64),
  aggregation_time datetime not null default current_timestamp,
  manager_id bigint unsigned,
  manager_name varchar(100),
  owner_org_id bigint unsigned,
  owner_org_name varchar(150),
  data_date date not null,
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  active_customer_no varchar(32) generated always as (
    case when default_status = 'ACTIVE' and deleted_flag = 0 then customer_no else null end
  ) stored,
  primary key (id),
  unique key uk_corporate_customer_default_no (customer_default_no),
  unique key uk_corporate_customer_default_active_customer (active_customer_no),
  key idx_corporate_customer_default_customer (customer_no),
  key idx_corporate_customer_default_level (highest_default_level),
  key idx_corporate_customer_default_status (default_status),
  key idx_corporate_customer_default_first_date (first_default_date),
  key idx_corporate_customer_default_latest_date (latest_default_date),
  key idx_corporate_customer_default_data_date (data_date),
  constraint fk_corporate_customer_default_customer_no
    foreign key (customer_no) references corporate_customer (customer_no),
  constraint ck_corporate_customer_default_level
    check (highest_default_level in ('A', 'B', 'C')),
  constraint ck_corporate_customer_default_count
    check (
      active_default_debt_count >= 0
      and total_default_debt_count >= 0
      and overdue90_debt_count >= 0
      and five_category_default_debt_count >= 0
      and rating_default_debt_count >= 0
      and total_default_debt_count >= active_default_debt_count
      and (default_status <> 'ACTIVE' or active_default_debt_count > 0)
    ),
  constraint ck_corporate_customer_default_amount
    check (
      default_principal_amount >= 0
      and default_interest_amount >= 0
      and default_exposure_amount >= 0
      and recovered_amount >= 0
      and loss_amount >= 0
    ),
  constraint ck_corporate_customer_default_flag
    check (
      default_flag in (0, 1)
      and deleted_flag in (0, 1)
    ),
  constraint ck_corporate_customer_default_date
    check (
      latest_default_date >= first_default_date
      and (cure_date is null or cure_date >= first_default_date)
      and (resolved_date is null or resolved_date >= first_default_date)
    )
);

create table risk_register (
  id bigint primary key auto_increment,
  risk_code varchar(60) not null unique,
  risk_name varchar(160) not null,
  category varchar(60) not null,
  `level` varchar(20) not null,
  owner_department varchar(100),
  responsible_person varchar(60),
  status varchar(30) not null default '识别中',
  identified_at date,
  due_date date,
  description text,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table risk_assessment (
  id bigint primary key auto_increment,
  risk_code varchar(60) not null,
  risk_name varchar(160) not null,
  likelihood int not null default 1,
  impact int not null default 1,
  inherent_level varchar(20) not null,
  residual_level varchar(20) not null,
  assessor varchar(60),
  assessed_at date,
  conclusion text,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table risk_control_measure (
  id bigint primary key auto_increment,
  control_code varchar(60) not null unique,
  risk_code varchar(60) not null,
  control_name varchar(160) not null,
  control_type varchar(40),
  frequency varchar(40),
  owner varchar(60),
  effectiveness varchar(40),
  status varchar(30) not null default '启用',
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table risk_treatment_plan (
  id bigint primary key auto_increment,
  plan_code varchar(60) not null unique,
  risk_code varchar(60) not null,
  action varchar(255) not null,
  owner varchar(60),
  due_date date,
  progress int not null default 0,
  status varchar(30) not null default '未开始',
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table risk_event (
  id bigint primary key auto_increment,
  event_code varchar(60) not null unique,
  title varchar(160) not null,
  risk_code varchar(60),
  severity varchar(20) not null,
  occurred_at date,
  loss_amount decimal(18,2) default 0,
  owner varchar(60),
  status varchar(30) not null default '登记',
  summary text,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table risk_indicator (
  id bigint primary key auto_increment,
  indicator_code varchar(60) not null unique,
  name varchar(160) not null,
  threshold_value varchar(80) not null,
  current_value varchar(80),
  trend varchar(20),
  owner varchar(100),
  status varchar(30) not null default '正常',
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table risk_scoring_rule (
  id bigint primary key auto_increment,
  rule_code varchar(64) not null unique,
  rule_name varchar(160) not null,
  metric_key varchar(64) not null,
  operator_type varchar(16) not null,
  threshold_value decimal(18,6) not null default 0.000000,
  effect_type varchar(16) not null default 'ADD',
  score_value int not null default 0,
  risk_tag varchar(100),
  reason_template varchar(300),
  enabled tinyint not null default 1,
  sort_order int not null default 0,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  key idx_risk_scoring_rule_enabled_sort (enabled, sort_order)
);

create table risk_alert_subscription (
  id bigint primary key auto_increment,
  subscription_code varchar(64) not null unique,
  subscription_name varchar(160) not null,
  frequency varchar(16) not null default 'DAILY',
  channel varchar(16) not null default 'SYSTEM',
  target_type varchar(16) not null default 'ALL',
  recipients varchar(500),
  enabled tinyint not null default 0,
  last_dispatch_at datetime,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  key idx_risk_alert_subscription_dispatch (enabled, frequency, last_dispatch_at)
);

create table risk_external_data_access_log (
  id bigint primary key auto_increment,
  customer_no varchar(32) not null,
  provider_name varchar(160) not null,
  query_status varchar(32) not null,
  data_available tinyint not null default 0,
  external_risk_score int,
  data_source varchar(160),
  requested_at datetime not null default current_timestamp,
  key idx_risk_external_access_time (requested_at),
  key idx_risk_external_access_status (query_status, requested_at),
  key idx_risk_external_access_customer (customer_no, requested_at)
);

create table risk_ai_chat_log (
  id bigint primary key auto_increment,
  customer_no varchar(32) not null,
  question varchar(500) not null,
  answer_type varchar(64) not null,
  external_status varchar(32),
  created_at datetime not null default current_timestamp,
  key idx_risk_ai_chat_customer (customer_no, created_at),
  key idx_risk_ai_chat_type (answer_type, created_at)
);

create table risk_model_monitor_snapshot (
  id bigint primary key auto_increment,
  snapshot_date date not null,
  model_name varchar(100) not null,
  rule_count int not null default 0,
  customer_total int not null default 0,
  average_risk_score decimal(10,4) not null default 0.0000,
  extreme_risk_count int not null default 0,
  high_risk_count int not null default 0,
  warning_customer_count int not null default 0,
  forecast_upgrade_count int not null default 0,
  external_query_count int not null default 0,
  external_available_count int not null default 0,
  external_unavailable_count int not null default 0,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_risk_model_snapshot (snapshot_date, model_name),
  key idx_risk_model_snapshot_date (snapshot_date)
);

create table risk_data_lineage (
  id bigint primary key auto_increment,
  domain_name varchar(80) not null,
  entity_name varchar(120) not null,
  source_table varchar(120) not null unique,
  business_key varchar(120) not null,
  key_fields varchar(500) not null,
  freshness_sla_hours int not null default 24,
  sensitivity_level varchar(32) not null default 'INTERNAL',
  enabled tinyint not null default 1,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table risk_data_quality_snapshot (
  id bigint primary key auto_increment,
  snapshot_date date not null unique,
  quality_score decimal(6,2) not null default 0.00,
  check_total int not null default 0,
  pass_count int not null default 0,
  warning_count int not null default 0,
  failed_count int not null default 0,
  issue_total int not null default 0,
  detail_json json,
  captured_at datetime not null default current_timestamp
);

create table risk_model_version (
  id bigint primary key auto_increment,
  version_code varchar(64) not null unique,
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
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table risk_model_version_rule (
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
  constraint fk_risk_model_version_rule_version foreign key (version_id) references risk_model_version(id) on delete cascade
);

create table risk_model_approval_log (
  id bigint primary key auto_increment,
  version_id bigint not null,
  action_type varchar(32) not null,
  decision varchar(32) not null,
  operator varchar(100) not null,
  comment varchar(1000),
  created_at datetime not null default current_timestamp,
  constraint fk_risk_model_approval_version foreign key (version_id) references risk_model_version(id) on delete cascade
);

create table risk_alert_case (
  id bigint primary key auto_increment,
  customer_no varchar(32) not null unique,
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
  key idx_risk_alert_case_state_due (alert_state, sla_due_at)
);

create table risk_alert_case_link (
  id bigint primary key auto_increment,
  alert_case_id bigint not null,
  risk_code varchar(80) not null,
  treatment_plan_id bigint,
  risk_event_id bigint,
  risk_indicator_id bigint,
  last_sync_state varchar(32) not null,
  last_sync_at datetime not null,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_risk_alert_case_link_case (alert_case_id),
  key idx_risk_alert_case_link_risk_code (risk_code),
  key idx_risk_alert_case_link_sync (last_sync_state, last_sync_at),
  constraint fk_risk_alert_case_link_case foreign key (alert_case_id) references risk_alert_case(id) on delete cascade,
  constraint fk_risk_alert_case_link_treatment foreign key (treatment_plan_id) references risk_treatment_plan(id) on delete set null,
  constraint fk_risk_alert_case_link_event foreign key (risk_event_id) references risk_event(id) on delete set null,
  constraint fk_risk_alert_case_link_indicator foreign key (risk_indicator_id) references risk_indicator(id) on delete set null
);

create table risk_stress_test_run (
  id bigint primary key auto_increment,
  scenario_code varchar(64) not null,
  scenario_name varchar(160) not null,
  parameter_json json not null,
  summary_json json not null,
  run_by varchar(100) not null,
  created_at datetime not null default current_timestamp
);

create table risk_lgd_model_version (
  id bigint primary key auto_increment,
  version_code varchar(64) not null,
  version_name varchar(160) not null,
  status varchar(32) not null,
  source_description varchar(500),
  created_by varchar(100) not null,
  approved_by varchar(100),
  approved_at datetime,
  effective_date date,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_risk_lgd_model_version_code (version_code),
  key idx_risk_lgd_model_version_status (status, effective_date)
);

create table risk_lgd_segment_parameter (
  id bigint primary key auto_increment,
  version_id bigint not null,
  segment_type varchar(48) not null,
  segment_code varchar(100) not null,
  segment_name varchar(160) not null,
  lgd_avg decimal(10,6) not null,
  lgd_downturn decimal(10,6) not null,
  recovery_rate decimal(10,6) not null,
  collateral_haircut decimal(10,6) not null default 0.000000,
  enabled tinyint not null default 1,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_risk_lgd_segment_parameter (version_id, segment_type, segment_code),
  key idx_risk_lgd_segment_version (version_id, enabled),
  constraint fk_risk_lgd_segment_version foreign key (version_id) references risk_lgd_model_version(id) on delete cascade
);

create table risk_lgd_calculation_run (
  id bigint primary key auto_increment,
  model_version_id bigint,
  run_type varchar(32) not null,
  source_data_date date,
  exposure_count int not null default 0,
  ead_amount decimal(20,2) not null default 0.00,
  weighted_lgd_avg decimal(10,6) not null default 0.000000,
  weighted_lgd_downturn decimal(10,6) not null default 0.000000,
  el_avg_amount decimal(20,2) not null default 0.00,
  el_downturn_amount decimal(20,2) not null default 0.00,
  result_summary json,
  run_by varchar(100) not null,
  created_at datetime not null default current_timestamp,
  key idx_risk_lgd_calculation_run_created (created_at),
  constraint fk_risk_lgd_calculation_model foreign key (model_version_id) references risk_lgd_model_version(id) on delete set null
);

create table risk_lgd_stress_test_run (
  id bigint primary key auto_increment,
  scenario_code varchar(64) not null,
  scenario_name varchar(160) not null,
  parameter_json json not null,
  summary_json json not null,
  run_by varchar(100) not null,
  created_at datetime not null default current_timestamp,
  key idx_risk_lgd_stress_created (created_at)
);

create table risk_portfolio_limit (
  id bigint primary key auto_increment,
  dimension_type varchar(32) not null,
  scope_code varchar(160) not null,
  scope_name varchar(160) not null,
  metric_code varchar(32) not null,
  limit_amount decimal(20,2) not null,
  warning_ratio decimal(10,6) not null default 0.800000,
  owner varchar(100) not null,
  status varchar(32) not null default 'ENABLED',
  effective_date date not null,
  expiry_date date,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_risk_portfolio_limit_scope_metric (dimension_type, scope_code, metric_code),
  key idx_risk_portfolio_limit_status (status, effective_date),
  constraint ck_risk_portfolio_limit_ratio check (warning_ratio > 0 and warning_ratio <= 1)
);

create table risk_portfolio_limit_snapshot (
  id bigint primary key auto_increment,
  limit_id bigint not null,
  data_date date not null,
  actual_amount decimal(20,2) not null,
  portfolio_total_amount decimal(20,2) not null,
  utilization_ratio decimal(10,6) not null,
  concentration_ratio decimal(10,6) not null,
  monitor_status varchar(32) not null,
  evidence_json json,
  captured_by varchar(100) not null,
  captured_at datetime not null default current_timestamp,
  unique key uk_risk_portfolio_limit_snapshot_day (limit_id, data_date),
  key idx_risk_portfolio_limit_snapshot_status (monitor_status, data_date),
  constraint fk_risk_portfolio_limit_snapshot_limit foreign key (limit_id) references risk_portfolio_limit(id) on delete cascade
);

create table risk_model_backtest_run (
  id bigint primary key auto_increment,
  run_code varchar(64) not null,
  as_of_date date not null,
  segment_type varchar(32) not null,
  segment_code varchar(160) not null,
  segment_name varchar(160) not null,
  exposure_count int not null default 0,
  default_count int not null default 0,
  ead_amount decimal(20,2) not null default 0.00,
  expected_pd decimal(10,6) not null default 0.000000,
  observed_default_rate decimal(10,6) not null default 0.000000,
  expected_lgd decimal(10,6) not null default 0.000000,
  observed_lgd decimal(10,6) not null default 0.000000,
  predicted_default_ead decimal(20,2) not null default 0.00,
  observed_default_ead decimal(20,2) not null default 0.00,
  predicted_el decimal(20,2) not null default 0.00,
  observed_loss decimal(20,2) not null default 0.00,
  pd_bias decimal(10,6) not null default 0.000000,
  lgd_bias decimal(10,6) not null default 0.000000,
  ead_bias decimal(10,6) not null default 0.000000,
  calibration_status varchar(32) not null,
  recommendation varchar(1000),
  run_by varchar(100) not null,
  created_at datetime not null default current_timestamp,
  key idx_risk_model_backtest_run_code (run_code),
  key idx_risk_model_backtest_segment (segment_type, segment_code, created_at)
);

create table risk_alert_effectiveness_snapshot (
  id bigint primary key auto_increment,
  alert_case_id bigint not null,
  evaluation_date date not null,
  evaluation_window_days int not null,
  closed_at datetime not null,
  baseline_risk_score int not null,
  current_risk_score int not null,
  score_delta int not null,
  current_pd decimal(10,6) not null default 0.000000,
  current_lgd_downturn decimal(10,6) not null default 0.000000,
  overdue_count int not null default 0,
  debt_default_count int not null default 0,
  effectiveness_status varchar(32) not null,
  conclusion varchar(1000),
  evaluated_by varchar(100) not null,
  created_at datetime not null default current_timestamp,
  unique key uk_risk_alert_effectiveness_day (alert_case_id, evaluation_date, evaluation_window_days),
  key idx_risk_alert_effectiveness_status (effectiveness_status, evaluation_date),
  constraint fk_risk_alert_effectiveness_case foreign key (alert_case_id) references risk_alert_case(id) on delete cascade
);

create table risk_user_workbench_preference (
  id bigint primary key auto_increment,
  username varchar(100) not null,
  workspace_role varchar(32) not null default 'RISK_MANAGER',
  default_tab varchar(64) not null default 'limits',
  density_mode varchar(16) not null default 'COMFORTABLE',
  visible_modules_json json,
  updated_by varchar(100) not null,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_workbench_preference_user (username)
);

create table risk_stress_scenario_definition (
  id bigint primary key auto_increment,
  scenario_code varchar(64) not null,
  scenario_name varchar(100) not null,
  severity varchar(32) not null,
  pd_multiplier decimal(10,6) not null default 1,
  lgd_addon decimal(10,6) not null default 0,
  ead_multiplier decimal(10,6) not null default 1,
  collateral_haircut decimal(10,6) not null default 0,
  description varchar(500),
  status varchar(32) not null default 'ENABLED',
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_stress_scenario_code (scenario_code)
);

create table risk_portfolio_stress_result (
  id bigint primary key auto_increment,
  run_code varchar(64) not null,
  scenario_code varchar(64) not null,
  as_of_date date not null,
  segment_type varchar(32) not null,
  segment_code varchar(160) not null,
  segment_name varchar(160) not null,
  base_ead decimal(20,2) not null,
  stressed_ead decimal(20,2) not null,
  base_el decimal(20,2) not null,
  stressed_el decimal(20,2) not null,
  incremental_loss decimal(20,2) not null,
  stressed_pd decimal(10,6) not null,
  stressed_lgd decimal(10,6) not null,
  stressed_collateral decimal(20,2) not null,
  limit_breach_flag tinyint not null default 0,
  run_by varchar(100) not null,
  created_at datetime not null default current_timestamp,
  key idx_portfolio_stress_run (run_code, segment_type),
  key idx_portfolio_stress_scenario (scenario_code, created_at)
);

create table risk_customer_group (
  id bigint primary key auto_increment,
  group_code varchar(64) not null,
  group_name varchar(200) not null,
  controller_customer_no varchar(32),
  group_type varchar(32) not null default 'CORPORATE_GROUP',
  risk_appetite_amount decimal(20,2) not null default 0,
  owner_org_name varchar(150),
  status varchar(32) not null default 'ACTIVE',
  created_at datetime not null default current_timestamp,
  unique key uk_risk_customer_group_code (group_code)
);

create table risk_customer_group_member (
  id bigint primary key auto_increment,
  group_id bigint not null,
  customer_no varchar(32) not null,
  relationship_type varchar(32) not null default 'AFFILIATE',
  ownership_ratio decimal(10,6) not null default 0,
  guarantee_flag tinyint not null default 0,
  effective_date date not null,
  created_at datetime not null default current_timestamp,
  unique key uk_group_customer_member (group_id, customer_no),
  key idx_group_member_customer (customer_no),
  constraint fk_group_member_group foreign key (group_id) references risk_customer_group(id) on delete cascade
);

create table risk_month_end_batch (
  id bigint primary key auto_increment,
  batch_no varchar(64) not null,
  month_end_date date not null,
  source_data_date date not null,
  version_no int not null default 1,
  batch_type varchar(32) not null default 'MONTH_END',
  source_system varchar(64) not null default 'UPSTREAM_CREDIT',
  source_batch_no varchar(64),
  run_mode varchar(32) not null default 'FORMAL',
  dependency_status varchar(32) not null default 'READY',
  retry_of_batch_id bigint,
  input_checksum varchar(128),
  publish_comment varchar(500),
  status varchar(32) not null default 'RECEIVED',
  published_flag tinyint not null default 0,
  locked_flag tinyint not null default 0,
  record_count int not null default 0,
  customer_count int not null default 0,
  total_ead decimal(20,2) not null default 0.00,
  total_el_avg decimal(20,2) not null default 0.00,
  total_el_downturn decimal(20,2) not null default 0.00,
  quality_score decimal(8,2) not null default 0.00,
  reconciliation_status varchar(32) not null default 'PENDING',
  failure_message varchar(1000),
  created_by varchar(100) not null,
  started_at datetime not null default current_timestamp,
  completed_at datetime,
  published_at datetime,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_risk_month_end_batch_no (batch_no),
  unique key uk_risk_month_end_batch_version (month_end_date, version_no),
  key idx_risk_month_end_batch_status (status, month_end_date),
  constraint ck_risk_month_end_batch_version check (version_no > 0)
);

create table risk_month_end_exposure_snapshot (
  id bigint primary key auto_increment,
  batch_id bigint not null,
  month_end_date date not null,
  exposure_no varchar(32) not null,
  customer_no varchar(32) not null,
  customer_name varchar(200) not null,
  contract_no varchar(32) not null,
  debt_no varchar(32) not null,
  product_type varchar(64) not null,
  industry_name varchar(100),
  owner_org_name varchar(150),
  rating_result varchar(20),
  risk_level varchar(32),
  ead_amount decimal(20,2) not null default 0.00,
  pd decimal(10,6) not null default 0.000000,
  lgd_avg decimal(10,6) not null default 0.000000,
  lgd_downturn decimal(10,6) not null default 0.000000,
  el_avg decimal(20,2) not null default 0.00,
  el_downturn decimal(20,2) not null default 0.00,
  exposure_balance decimal(20,2) not null default 0.00,
  collateral_value decimal(20,2) not null default 0.00,
  product_recovery_rate decimal(10,6) not null default 0.000000,
  overdue_days int not null default 0,
  default_flag tinyint not null default 0,
  five_category varchar(32),
  model_name varchar(100),
  model_version varchar(64),
  created_at datetime not null default current_timestamp,
  unique key uk_risk_month_end_exposure (batch_id, exposure_no),
  key idx_risk_month_end_exposure_customer (batch_id, customer_no),
  key idx_risk_month_end_exposure_debt (batch_id, debt_no),
  key idx_risk_month_end_exposure_dimension (batch_id, industry_name, product_type),
  constraint fk_risk_month_end_exposure_batch foreign key (batch_id) references risk_month_end_batch(id) on delete cascade
);

create table risk_month_end_customer_snapshot (
  id bigint primary key auto_increment,
  batch_id bigint not null,
  month_end_date date not null,
  customer_no varchar(32) not null,
  customer_name varchar(200) not null,
  industry_name varchar(100),
  owner_org_name varchar(150),
  rating_result varchar(20),
  risk_level varchar(32),
  active_debt_count int not null default 0,
  ead_amount decimal(20,2) not null default 0.00,
  weighted_pd decimal(10,6) not null default 0.000000,
  weighted_lgd_avg decimal(10,6) not null default 0.000000,
  weighted_lgd_downturn decimal(10,6) not null default 0.000000,
  el_avg_amount decimal(20,2) not null default 0.00,
  el_downturn_amount decimal(20,2) not null default 0.00,
  collateral_value decimal(20,2) not null default 0.00,
  overdue_count int not null default 0,
  max_overdue_days int not null default 0,
  default_debt_count int not null default 0,
  default_flag tinyint not null default 0,
  high_risk_flag tinyint not null default 0,
  created_at datetime not null default current_timestamp,
  unique key uk_risk_month_end_customer (batch_id, customer_no),
  key idx_risk_month_end_customer_risk (batch_id, risk_level, default_flag),
  constraint fk_risk_month_end_customer_batch foreign key (batch_id) references risk_month_end_batch(id) on delete cascade
);

create table risk_month_end_summary (
  id bigint primary key auto_increment,
  batch_id bigint not null,
  month_end_date date not null,
  dimension_type varchar(32) not null,
  dimension_code varchar(160) not null,
  dimension_name varchar(160) not null,
  customer_count int not null default 0,
  exposure_count int not null default 0,
  high_risk_count int not null default 0,
  overdue_count int not null default 0,
  default_count int not null default 0,
  ead_amount decimal(20,2) not null default 0.00,
  weighted_pd decimal(10,6) not null default 0.000000,
  weighted_lgd_avg decimal(10,6) not null default 0.000000,
  weighted_lgd_downturn decimal(10,6) not null default 0.000000,
  el_avg_amount decimal(20,2) not null default 0.00,
  el_downturn_amount decimal(20,2) not null default 0.00,
  concentration_ratio decimal(10,6) not null default 0.000000,
  created_at datetime not null default current_timestamp,
  unique key uk_risk_month_end_summary_dimension (batch_id, dimension_type, dimension_code),
  key idx_risk_month_end_summary_type (dimension_type, month_end_date),
  constraint fk_risk_month_end_summary_batch foreign key (batch_id) references risk_month_end_batch(id) on delete cascade
);

create table risk_month_end_change_detail (
  id bigint primary key auto_increment,
  current_batch_id bigint not null,
  base_batch_id bigint not null,
  grain_type varchar(32) not null default 'DEBT',
  business_key varchar(64) not null,
  customer_no varchar(32) not null,
  customer_name varchar(200),
  contract_no varchar(32),
  debt_no varchar(32),
  industry_name varchar(100),
  product_type varchar(64),
  previous_rating varchar(20),
  current_rating varchar(20),
  previous_risk_level varchar(32),
  current_risk_level varchar(32),
  previous_ead decimal(20,2) not null default 0.00,
  current_ead decimal(20,2) not null default 0.00,
  ead_delta decimal(20,2) not null default 0.00,
  previous_pd decimal(10,6) not null default 0.000000,
  current_pd decimal(10,6) not null default 0.000000,
  pd_delta decimal(10,6) not null default 0.000000,
  previous_lgd decimal(10,6) not null default 0.000000,
  current_lgd decimal(10,6) not null default 0.000000,
  lgd_delta decimal(10,6) not null default 0.000000,
  previous_el decimal(20,2) not null default 0.00,
  current_el decimal(20,2) not null default 0.00,
  el_delta decimal(20,2) not null default 0.00,
  previous_overdue_days int not null default 0,
  current_overdue_days int not null default 0,
  previous_default_flag tinyint not null default 0,
  current_default_flag tinyint not null default 0,
  change_type varchar(32) not null,
  reason_code varchar(64) not null,
  created_at datetime not null default current_timestamp,
  unique key uk_risk_month_end_change_pair (current_batch_id, base_batch_id, grain_type, business_key),
  key idx_risk_month_end_change_type (current_batch_id, base_batch_id, change_type),
  key idx_risk_month_end_change_customer (current_batch_id, base_batch_id, customer_no),
  constraint fk_risk_month_end_change_current foreign key (current_batch_id) references risk_month_end_batch(id) on delete cascade,
  constraint fk_risk_month_end_change_base foreign key (base_batch_id) references risk_month_end_batch(id) on delete cascade
);

create table risk_month_end_reconciliation (
  id bigint primary key auto_increment,
  batch_id bigint not null,
  check_code varchar(64) not null,
  check_name varchar(200) not null,
  source_value decimal(24,6) not null default 0.000000,
  snapshot_value decimal(24,6) not null default 0.000000,
  difference_value decimal(24,6) not null default 0.000000,
  tolerance_value decimal(24,6) not null default 0.000000,
  check_status varchar(32) not null,
  detail_message varchar(500),
  created_at datetime not null default current_timestamp,
  unique key uk_risk_month_end_reconciliation (batch_id, check_code),
  key idx_risk_month_end_reconciliation_status (check_status, created_at),
  constraint fk_risk_month_end_reconciliation_batch foreign key (batch_id) references risk_month_end_batch(id) on delete cascade
);

create table risk_month_end_source_manifest (
  id bigint primary key auto_increment,
  batch_id bigint not null,
  source_name varchar(100) not null,
  source_batch_no varchar(64),
  data_date date not null,
  expected_count int not null default 0,
  received_count int not null default 0,
  checksum_value varchar(128),
  receive_status varchar(32) not null default 'RECEIVED',
  received_at datetime not null default current_timestamp,
  unique key uk_month_end_manifest_source (batch_id, source_name),
  key idx_month_end_manifest_status (receive_status, data_date),
  constraint fk_month_end_manifest_batch foreign key (batch_id) references risk_month_end_batch(id) on delete cascade
);

create table risk_month_end_quality_issue (
  id bigint primary key auto_increment,
  batch_id bigint not null,
  check_code varchar(64) not null,
  issue_level varchar(16) not null default 'MEDIUM',
  issue_type varchar(64) not null,
  issue_count int not null default 0,
  issue_description varchar(500) not null,
  owner_org varchar(150),
  owner_name varchar(100),
  status varchar(32) not null default 'OPEN',
  resolution_note varchar(1000),
  resolved_by varchar(100),
  resolved_at datetime,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_month_end_quality_issue (batch_id, check_code),
  key idx_month_end_quality_status (status, issue_level, created_at),
  constraint fk_month_end_quality_batch foreign key (batch_id) references risk_month_end_batch(id) on delete cascade
);

insert into sys_tenant(id, name, code, contact_name, contact_phone, status)
values (1, '默认租户', 'default', '管理员', '13800000000', 'ENABLED');

insert into sys_department(id, tenant_id, parent_id, name, code, leader, phone, sort_order, status)
values
(1, 1, 0, '总部', 'HQ', '管理员', '13800000000', 1, 'ENABLED'),
(2, 1, 1, '研发部', 'RD', '技术负责人', '13800000001', 2, 'ENABLED'),
(3, 1, 1, '运营部', 'OPS', '运营负责人', '13800000002', 3, 'ENABLED');

insert into sys_post(id, tenant_id, name, code, sort_order, status)
values
(1, 1, '系统管理员', 'admin', 1, 'ENABLED'),
(2, 1, '产品经理', 'product-manager', 2, 'ENABLED'),
(3, 1, '开发工程师', 'developer', 3, 'ENABLED');

insert into sys_user(id, tenant_id, department_id, post_id, username, password_hash, display_name, email, phone, status)
values
(1, 1, 1, 1, 'admin', 'ACCOUNT_DISABLED_REQUIRES_SECURE_BOOTSTRAP', '系统管理员', 'admin@example.com', '13800000000', 'DISABLED'),
(2, 1, 2, 3, 'demo', 'ACCOUNT_DISABLED_REQUIRES_SECURE_BOOTSTRAP', '演示用户', 'demo@example.com', '13800000003', 'DISABLED');

insert into sys_role(id, tenant_id, name, code, description, status)
values
(1, 1, '超级管理员', 'ADMIN', '拥有全部权限', 'ENABLED'),
(2, 1, '普通用户', 'USER', '基础查看权限', 'ENABLED');

insert into sys_permission(id, name, code, module, description)
values
(1, '用户管理', 'user:manage', 'user', '用户增删改查'),
(2, '角色管理', 'role:manage', 'user', '角色增删改查'),
(3, '权限管理', 'permission:manage', 'user', '权限增删改查'),
(4, '部门管理', 'department:manage', 'organization', '部门维护'),
(5, '岗位管理', 'post:manage', 'organization', '岗位维护'),
(6, '菜单管理', 'menu:manage', 'system', '菜单维护'),
(7, '日志查看', 'log:view', 'audit', '查看登录、操作和异常日志'),
(8, '通知管理', 'notification:manage', 'system', '通知维护'),
(9, '配置管理', 'config:manage', 'system', '系统配置'),
(10, '安全策略', 'security:manage', 'system', '安全策略维护'),
(11, '租户管理', 'tenant:manage', 'tenant', '租户维护'),
(12, '用户导入', 'import:user', 'user', '用户导入'),
(13, '用户导出', 'export:user', 'user', '用户导出'),
(14, '风险台账', 'risk:manage', 'risk', '风险识别与台账维护'),
(15, '风险评估', 'risk:assess', 'risk', '风险评估与评级'),
(16, '控制措施', 'risk:control', 'risk', '控制措施维护'),
(17, '整改任务', 'risk:treat', 'risk', '风险整改跟踪'),
(18, '风险事件', 'risk:event', 'risk', '风险事件登记'),
(19, '风险指标', 'risk:indicator', 'risk', '关键风险指标监控');

insert into sys_user_role(user_id, role_id) values (1, 1), (2, 2);
insert into sys_role_permission(role_id, permission_id)
select 1, id from sys_permission;
insert into sys_role_permission(role_id, permission_id)
select 2, id from sys_permission where code in ('user:manage', 'role:manage', 'department:manage', 'menu:manage', 'log:view');

insert into sys_menu(id, parent_id, title, path, component, icon, permission_code, sort_order, visible)
values
(1, 0, '风险台账', '/risks/registers', 'CrudPage', 'Warning', 'risk:manage', 1, 1),
(2, 0, '风险评估', '/risks/assessments', 'CrudPage', 'DataAnalysis', 'risk:assess', 2, 1),
(3, 0, '控制措施', '/risks/controls', 'CrudPage', 'Lock', 'risk:control', 3, 1),
(4, 0, '整改任务', '/risks/treatments', 'CrudPage', 'Tickets', 'risk:treat', 4, 1),
(5, 0, '风险事件', '/risks/events', 'CrudPage', 'Document', 'risk:event', 5, 1),
(6, 0, '风险指标', '/risks/indicators', 'CrudPage', 'TrendCharts', 'risk:indicator', 6, 1),
(7, 0, '用户与账号', '/users', 'CrudPage', 'User', 'user:manage', 7, 1),
(8, 0, '角色权限', '/roles', 'CrudPage', 'Key', 'role:manage', 8, 1),
(9, 0, '审计日志', '/logs/operation', 'CrudPage', 'FileClock', 'log:view', 9, 1),
(10, 0, '风险通知', '/notifications', 'CrudPage', 'Bell', 'notification:manage', 10, 1),
(11, 0, '风险参数', '/configs', 'CrudPage', 'Settings', 'config:manage', 11, 1);

insert into risk_register(id, risk_code, risk_name, category, `level`, owner_department, responsible_person, status, identified_at, due_date, description)
values
(1, 'R-2026-001', '供应商交付延期', '运营风险', '高', '供应链管理部', '王敏', '整改中', '2026-06-01', '2026-07-15', '关键供应商产能波动，可能影响核心项目交付。'),
(2, 'R-2026-002', '客户数据访问权限过宽', '信息安全风险', '重大', '信息科技部', '李哲', '评估中', '2026-06-10', '2026-06-30', '部分岗位权限未按最小授权原则收敛。'),
(3, 'R-2026-003', '费用报销合规性不足', '合规风险', '中', '财务部', '赵璐', '监控中', '2026-05-20', '2026-07-01', '抽样发现附件完整性和审批链路存在不一致。');

insert into risk_assessment(id, risk_code, risk_name, likelihood, impact, inherent_level, residual_level, assessor, assessed_at, conclusion)
values
(1, 'R-2026-001', '供应商交付延期', 4, 4, '高', '中', '陈然', '2026-06-12', '需建立备选供应商和交付预警机制。'),
(2, 'R-2026-002', '客户数据访问权限过宽', 3, 5, '重大', '高', '刘洋', '2026-06-18', '先冻结高危权限，再分岗位重建授权模型。');

insert into risk_control_measure(id, control_code, risk_code, control_name, control_type, frequency, owner, effectiveness, status)
values
(1, 'C-001', 'R-2026-001', '供应商交付周监控', '发现性控制', '每周', '王敏', '部分有效', '优化中'),
(2, 'C-002', 'R-2026-002', '高权限账号月度复核', '预防性控制', '每月', '李哲', '待验证', '启用');

insert into risk_treatment_plan(id, plan_code, risk_code, action, owner, due_date, progress, status)
values
(1, 'T-001', 'R-2026-001', '引入第二供应商并签订应急交付协议', '王敏', '2026-07-15', 60, '进行中'),
(2, 'T-002', 'R-2026-002', '完成数据权限矩阵重梳理', '李哲', '2026-06-30', 35, '进行中');

insert into risk_event(id, event_code, title, risk_code, severity, occurred_at, loss_amount, owner, status, summary)
values
(1, 'E-001', '供应商 A 延迟发货', 'R-2026-001', '高', '2026-06-16', 120000.00, '王敏', '处理中', '物料到货延期 5 天，已启动替代采购。');

insert into risk_indicator(id, indicator_code, name, threshold_value, current_value, trend, owner, status)
values
(1, 'KRI-001', '关键供应商准时交付率', '95%', '91%', '下降', '供应链管理部', '预警'),
(2, 'KRI-002', '高权限账号复核完成率', '100%', '82%', '上升', '信息科技部', '预警');

insert into risk_scoring_rule(rule_code, rule_name, metric_key, operator_type, threshold_value, effect_type, score_value, risk_tag, reason_template, enabled, sort_order)
values
('BLACKLIST_FLOOR', '黑名单客户', 'blacklist_flag', 'EQ', 1, 'FLOOR', 98, '黑名单客户', '客户存在黑名单标记。', 1, 10),
('DEBT_DEFAULT_FLOOR', '债项违约', 'debt_default_count', 'GT', 0, 'FLOOR', 95, '已违约', '客户存在 {value} 笔有效债项违约。', 1, 20),
('OVERDUE_90_FLOOR', '逾期超过90天', 'max_overdue_days', 'GT', 90, 'FLOOR', 90, '严重逾期', '最大逾期天数为 {value} 天，超过 90 天警戒线。', 1, 30),
('OVERDUE_60_ADD', '逾期超过60天', 'max_overdue_days', 'GT', 60, 'ADD', 35, '高逾期', '最大逾期天数为 {value} 天，处于高逾期区间。', 1, 40),
('OVERDUE_30_ADD', '逾期超过30天', 'max_overdue_days', 'GT', 30, 'ADD', 24, '逾期预警', '最大逾期天数为 {value} 天，触发逾期预警。', 1, 50),
('OVERDUE_ANY_ADD', '存在逾期', 'overdue_count', 'GT', 0, 'ADD', 10, '存在逾期', '客户存在 {value} 笔逾期记录。', 1, 60),
('PD_HIGH_ADD', '高违约概率', 'max_pd', 'GTE', 0.100000, 'ADD', 25, 'PD高', '最大违约概率为 {value}。', 1, 70),
('PD_MEDIUM_ADD', '偏高违约概率', 'max_pd', 'GTE', 0.050000, 'ADD', 16, 'PD偏高', '最大违约概率为 {value}。', 1, 80),
('PD_ATTENTION_ADD', '关注违约概率', 'max_pd', 'GTE', 0.020000, 'ADD', 8, 'PD关注', '最大违约概率为 {value}。', 1, 90),
('RATING_21_ADD', '评级21', 'rating_numeric', 'GTE', 21, 'ADD', 25, '评级21', '当前评级等级为 {value}。', 1, 100),
('RATING_18_ADD', '评级较弱', 'rating_numeric', 'GTE', 18, 'ADD', 18, '评级较弱', '当前评级等级为 {value}。', 1, 110),
('RATING_15_ADD', '评级关注', 'rating_numeric', 'GTE', 15, 'ADD', 10, '评级关注', '当前评级等级为 {value}。', 1, 120),
('UTILIZATION_HIGH_ADD', '额度高占用', 'utilization_rate', 'GTE', 0.900000, 'ADD', 12, '额度高占用', '额度使用率为 {value}。', 1, 130),
('UTILIZATION_ATTENTION_ADD', '额度较高占用', 'utilization_rate', 'GTE', 0.750000, 'ADD', 6, '额度关注', '额度使用率为 {value}。', 1, 140),
('COVERAGE_LOW_ADD', '押品覆盖不足', 'coverage_rate', 'LT', 0.500000, 'ADD', 15, '押品覆盖不足', '押品覆盖率为 {value}。', 1, 150),
('COVERAGE_PARTIAL_ADD', '押品未完全覆盖', 'coverage_rate', 'LT', 1.000000, 'ADD', 6, '押品覆盖关注', '押品覆盖率为 {value}。', 1, 160);

insert into risk_alert_subscription(subscription_code, subscription_name, frequency, channel, target_type, recipients, enabled)
values ('COMPOSITE_RISK_DAILY', '组合风险每日报告', 'DAILY', 'SYSTEM', 'ALL', '', 0);

insert into risk_data_lineage(domain_name, entity_name, source_table, business_key, key_fields, freshness_sla_hours, sensitivity_level, enabled)
values
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
('RISK_EVENT', '客户违约', 'corporate_customer_default', 'customer_default_no', 'customer_default_no,customer_no,default_status,default_date', 24, 'CONFIDENTIAL', 1);

insert into sys_operation_log(username, module, action, method, request_uri, status)
values
('admin', 'system', '初始化项目数据', 'SQL', '/database/init.sql', 'SUCCESS');

insert into sys_error_log(service_name, trace_id, message, stack_trace)
values
('system-service', 'demo-trace', '演示异常日志', 'This is sample data.');

insert into sys_notification(title, content, channel, target_type, status)
values
('系统上线通知', '风险管理系统基础版本已初始化。', 'SYSTEM', 'ALL', 'PUBLISHED');

insert into sys_config(config_key, config_value, description)
values
('password.min.length', '8', '最小密码长度'),
('login.lock.max.failures', '5', '登录失败锁定阈值'),
('default.role', 'USER', '默认角色'),
('risk.level.matrix', 'likelihood*impact', '风险等级矩阵规则'),
('risk.high.threshold', '12', '高风险评分阈值');

insert into sys_security_policy(name, policy_key, policy_value, enabled, description)
values
('密码复杂度', 'password.complexity', 'upper,lower,digit,symbol', 1, '要求密码包含大小写、数字和符号'),
('登录失败锁定', 'login.failure.lock', '5', 1, '连续失败 5 次后锁定'),
('Token 有效期', 'jwt.ttl.seconds', '86400', 1, '默认 24 小时');

insert into risk_stress_scenario_definition
(scenario_code,scenario_name,severity,pd_multiplier,lgd_addon,ead_multiplier,collateral_haircut,description)
values
('BASELINE','基准情景','BASELINE',1.000000,0.000000,1.000000,0.000000,'维持当前组合风险参数'),
('MILD_DOWNTURN','轻度下行情景','MILD',1.250000,0.050000,1.050000,0.100000,'PD 上升 25%，LGD 增加 5 个百分点，押品折价 10%'),
('SEVERE_DOWNTURN','重度下行情景','SEVERE',1.800000,0.120000,1.400000,0.300000,'PD 上升 80%，LGD 增加 12 个百分点，EAD 扩张 40%，押品折价 30%');

insert into risk_user_workbench_preference
(username,workspace_role,default_tab,density_mode,visible_modules_json,updated_by)
values ('admin','RISK_MANAGER','limits','COMFORTABLE',json_array('limits','forecast','stress','groups','lifecycle','backtest','effectiveness'),'SYSTEM');

insert ignore into risk_customer_group
(group_code,group_name,controller_customer_no,risk_appetite_amount,owner_org_name)
select concat('GRP-',lpad(group_seq,3,'0')),concat('企业集团 ',lpad(group_seq,3,'0')),
       max(case when member_seq=1 then customer_no end),30000000,max(owner_org_name)
from (
  select customer_no,owner_org_name,floor((row_number() over(order by customer_no)-1)/5)+1 group_seq,
         mod(row_number() over(order by customer_no)-1,5)+1 member_seq
  from corporate_customer where deleted_flag=0
) x group by group_seq;

insert ignore into risk_customer_group_member
(group_id,customer_no,relationship_type,ownership_ratio,guarantee_flag,effective_date)
select g.id,x.customer_no,case when x.member_seq=1 then 'CONTROLLER' else 'SUBSIDIARY' end,
       case when x.member_seq=1 then 1 else 0.51 end,case when mod(x.member_seq,2)=0 then 1 else 0 end,'2026-01-01'
from (
  select customer_no,floor((row_number() over(order by customer_no)-1)/5)+1 group_seq,
         mod(row_number() over(order by customer_no)-1,5)+1 member_seq
  from corporate_customer where deleted_flag=0
) x join risk_customer_group g on g.group_code=concat('GRP-',lpad(x.group_seq,3,'0'));
