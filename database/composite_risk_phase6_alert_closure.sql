SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
USE user_management;

CREATE TABLE IF NOT EXISTS risk_alert_case_link (
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
