set @ddl = if(
  (select count(*) from information_schema.columns where table_schema = database() and table_name = 'risk_alert_case' and column_name = 'review_comment') = 0,
  'alter table risk_alert_case add column review_comment varchar(1000) null',
  'do 0'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = if(
  (select count(*) from information_schema.columns where table_schema = database() and table_name = 'risk_alert_case' and column_name = 'reviewed_by') = 0,
  'alter table risk_alert_case add column reviewed_by varchar(120) null',
  'do 0'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = if(
  (select count(*) from information_schema.columns where table_schema = database() and table_name = 'risk_alert_case' and column_name = 'reviewed_at') = 0,
  'alter table risk_alert_case add column reviewed_at datetime null',
  'do 0'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

create table if not exists risk_alert_case_timeline (
  id bigint primary key auto_increment,
  customer_no varchar(32) not null,
  action_type varchar(64) not null,
  from_state varchar(32),
  to_state varchar(32),
  operator varchar(120),
  comment varchar(1000),
  created_at datetime not null default current_timestamp,
  key idx_alert_timeline_customer (customer_no, created_at),
  key idx_alert_timeline_action (action_type, created_at)
);

create table if not exists risk_data_ingestion_batch (
  id bigint primary key auto_increment,
  batch_no varchar(80) not null unique,
  source_system varchar(80) not null,
  source_entity varchar(120) not null,
  batch_status varchar(32) not null,
  source_record_count int not null default 0,
  accepted_record_count int not null default 0,
  rejected_record_count int not null default 0,
  quality_score decimal(6,2) not null default 0.00,
  started_at datetime not null,
  completed_at datetime,
  operator varchar(120),
  remark varchar(500),
  created_at datetime not null default current_timestamp,
  key idx_ingestion_source (source_system, source_entity),
  key idx_ingestion_status_time (batch_status, started_at)
);

set @ddl = if(
  (select count(*) from information_schema.columns where table_schema = database() and table_name = 'risk_model_monitor_snapshot' and column_name = 'auc_value') = 0,
  'alter table risk_model_monitor_snapshot add column auc_value decimal(6,4) not null default 0.0000',
  'do 0'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = if(
  (select count(*) from information_schema.columns where table_schema = database() and table_name = 'risk_model_monitor_snapshot' and column_name = 'ks_value') = 0,
  'alter table risk_model_monitor_snapshot add column ks_value decimal(6,4) not null default 0.0000',
  'do 0'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = if(
  (select count(*) from information_schema.columns where table_schema = database() and table_name = 'risk_model_monitor_snapshot' and column_name = 'psi_value') = 0,
  'alter table risk_model_monitor_snapshot add column psi_value decimal(6,4) not null default 0.0000',
  'do 0'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = if(
  (select count(*) from information_schema.columns where table_schema = database() and table_name = 'risk_model_monitor_snapshot' and column_name = 'precision_rate') = 0,
  'alter table risk_model_monitor_snapshot add column precision_rate decimal(6,4) not null default 0.0000',
  'do 0'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = if(
  (select count(*) from information_schema.columns where table_schema = database() and table_name = 'risk_model_monitor_snapshot' and column_name = 'recall_rate') = 0,
  'alter table risk_model_monitor_snapshot add column recall_rate decimal(6,4) not null default 0.0000',
  'do 0'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = if(
  (select count(*) from information_schema.columns where table_schema = database() and table_name = 'risk_model_monitor_snapshot' and column_name = 'false_alarm_rate') = 0,
  'alter table risk_model_monitor_snapshot add column false_alarm_rate decimal(6,4) not null default 0.0000',
  'do 0'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = if(
  (select count(*) from information_schema.columns where table_schema = database() and table_name = 'risk_model_monitor_snapshot' and column_name = 'stability_status') = 0,
  'alter table risk_model_monitor_snapshot add column stability_status varchar(32) not null default ''UNKNOWN''',
  'do 0'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

insert into sys_permission(name, code, module, description)
values
('预警复核', 'risk:alert:review', 'risk', '预警案件复核、退回与关闭'),
('模型效果查看', 'risk:model:effect', 'risk', '模型效果、稳定性和回测指标查看'),
('数据接入治理', 'risk:data:ingest', 'risk', '外部数据接入批次与质量校验'),
('权限审计', 'risk:audit', 'audit', '查看权限矩阵和高敏操作审计')
on duplicate key update description = values(description);

insert into sys_role_permission(role_id, permission_id)
select 1, id from sys_permission
where code in ('risk:alert:review', 'risk:model:effect', 'risk:data:ingest', 'risk:audit')
on duplicate key update role_id = values(role_id);

insert into risk_data_ingestion_batch
  (batch_no, source_system, source_entity, batch_status, source_record_count, accepted_record_count,
   rejected_record_count, quality_score, started_at, completed_at, operator, remark)
values
  ('ING-SEED-CREDIT-2026082101', 'EXT-CREDIT', 'CreditProfile', 'SUCCESS', 200, 196, 4, 98.00, date_sub(now(), interval 360 minute), date_sub(now(), interval 355 minute), 'SYSTEM', '模拟征信评分、逾期摘要、负面记录接入'),
  ('ING-SEED-BIZ-2026082101', 'EXT-BIZ', 'BusinessRegistration', 'SUCCESS', 200, 199, 1, 99.50, date_sub(now(), interval 300 minute), date_sub(now(), interval 296 minute), 'SYSTEM', '模拟工商经营状态、股权关系、经营异常接入'),
  ('ING-SEED-LAWSUIT-2026082101', 'EXT-LAWSUIT', 'JudicialCase', 'WARNING', 120, 112, 8, 93.33, date_sub(now(), interval 240 minute), date_sub(now(), interval 234 minute), 'SYSTEM', '模拟司法涉诉和被执行记录接入，部分客户号待匹配')
on duplicate key update completed_at = values(completed_at), quality_score = values(quality_score);

insert into risk_model_monitor_snapshot
  (snapshot_date, model_name, rule_count, customer_total, average_risk_score, extreme_risk_count,
   high_risk_count, warning_customer_count, forecast_upgrade_count, external_query_count,
   external_available_count, external_unavailable_count, auc_value, ks_value, psi_value,
   precision_rate, recall_rate, false_alarm_rate, stability_status)
values
  (date_sub(current_date, interval 2 day), 'COMPOSITE_SCORING_FORECAST_RULE_V2', 8, 200, 51.3200, 28, 0, 50, 28, 36, 34, 2, 0.8120, 0.4310, 0.0620, 0.7420, 0.8060, 0.1180, 'STABLE'),
  (date_sub(current_date, interval 1 day), 'COMPOSITE_SCORING_FORECAST_RULE_V2', 8, 200, 52.0800, 29, 0, 52, 29, 42, 39, 3, 0.8190, 0.4460, 0.0710, 0.7510, 0.8120, 0.1090, 'STABLE'),
  (current_date, 'COMPOSITE_SCORING_FORECAST_RULE_V2', 8, 200, 52.6400, 30, 0, 54, 30, 48, 44, 4, 0.8260, 0.4580, 0.0830, 0.7630, 0.8210, 0.1020, 'WATCH')
on duplicate key update
  auc_value = values(auc_value), ks_value = values(ks_value), psi_value = values(psi_value),
  precision_rate = values(precision_rate), recall_rate = values(recall_rate),
  false_alarm_rate = values(false_alarm_rate), stability_status = values(stability_status);
