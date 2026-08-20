SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
USE user_management;

CREATE TABLE IF NOT EXISTS risk_scoring_rule (
  id bigint primary key auto_increment,
  rule_code varchar(64) not null,
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
  unique key uk_risk_scoring_rule_code (rule_code),
  key idx_risk_scoring_rule_enabled_sort (enabled, sort_order),
  constraint ck_risk_scoring_rule_operator check (operator_type in ('GT', 'GTE', 'LT', 'LTE', 'EQ')),
  constraint ck_risk_scoring_rule_effect check (effect_type in ('ADD', 'FLOOR')),
  constraint ck_risk_scoring_rule_enabled check (enabled in (0, 1))
);

CREATE TABLE IF NOT EXISTS risk_alert_subscription (
  id bigint primary key auto_increment,
  subscription_code varchar(64) not null,
  subscription_name varchar(160) not null,
  frequency varchar(16) not null default 'DAILY',
  channel varchar(16) not null default 'SYSTEM',
  target_type varchar(16) not null default 'ALL',
  recipients varchar(500),
  enabled tinyint not null default 0,
  last_dispatch_at datetime,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_risk_alert_subscription_code (subscription_code),
  key idx_risk_alert_subscription_dispatch (enabled, frequency, last_dispatch_at),
  constraint ck_risk_alert_subscription_frequency check (frequency in ('DAILY')),
  constraint ck_risk_alert_subscription_channel check (channel in ('SYSTEM', 'EMAIL')),
  constraint ck_risk_alert_subscription_target check (target_type in ('ALL', 'USER')),
  constraint ck_risk_alert_subscription_enabled check (enabled in (0, 1))
);

INSERT IGNORE INTO risk_scoring_rule
  (rule_code, rule_name, metric_key, operator_type, threshold_value, effect_type, score_value, risk_tag, reason_template, enabled, sort_order)
VALUES
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

INSERT IGNORE INTO risk_alert_subscription
  (subscription_code, subscription_name, frequency, channel, target_type, recipients, enabled)
VALUES
  ('COMPOSITE_RISK_DAILY', '组合风险每日报告', 'DAILY', 'SYSTEM', 'ALL', '', 0);
