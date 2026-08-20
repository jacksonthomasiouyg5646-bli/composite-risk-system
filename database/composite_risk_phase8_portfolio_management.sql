SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
USE user_management;

CREATE TABLE IF NOT EXISTS risk_portfolio_limit (
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

CREATE TABLE IF NOT EXISTS risk_portfolio_limit_snapshot (
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

CREATE TABLE IF NOT EXISTS risk_model_backtest_run (
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

CREATE TABLE IF NOT EXISTS risk_alert_effectiveness_snapshot (
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
