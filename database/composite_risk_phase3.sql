SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
USE user_management;

CREATE TABLE IF NOT EXISTS risk_external_data_access_log (
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
  key idx_risk_external_access_customer (customer_no, requested_at),
  constraint ck_risk_external_access_available check (data_available in (0, 1))
);

CREATE TABLE IF NOT EXISTS risk_ai_chat_log (
  id bigint primary key auto_increment,
  customer_no varchar(32) not null,
  question varchar(500) not null,
  answer_type varchar(64) not null,
  external_status varchar(32),
  created_at datetime not null default current_timestamp,
  key idx_risk_ai_chat_customer (customer_no, created_at),
  key idx_risk_ai_chat_type (answer_type, created_at)
);

CREATE TABLE IF NOT EXISTS risk_model_monitor_snapshot (
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
