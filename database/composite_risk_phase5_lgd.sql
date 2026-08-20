SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
USE user_management;

CREATE TABLE IF NOT EXISTS risk_lgd_model_version (
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

CREATE TABLE IF NOT EXISTS risk_lgd_segment_parameter (
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
  constraint fk_risk_lgd_segment_version foreign key (version_id) references risk_lgd_model_version(id) on delete cascade,
  constraint ck_risk_lgd_segment_ratio check (
    lgd_avg >= 0 and lgd_avg <= 1 and lgd_downturn >= 0 and lgd_downturn <= 1
    and recovery_rate >= 0 and recovery_rate <= 1 and collateral_haircut >= 0 and collateral_haircut <= 1
  )
);

CREATE TABLE IF NOT EXISTS risk_lgd_calculation_run (
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

CREATE TABLE IF NOT EXISTS risk_lgd_stress_test_run (
  id bigint primary key auto_increment,
  scenario_code varchar(64) not null,
  scenario_name varchar(160) not null,
  parameter_json json not null,
  summary_json json not null,
  run_by varchar(100) not null,
  created_at datetime not null default current_timestamp,
  key idx_risk_lgd_stress_created (created_at)
);
