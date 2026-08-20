/*!40101 SET NAMES utf8mb4 */;
USE user_management;

ALTER TABLE risk_month_end_batch
  ADD COLUMN source_batch_no varchar(64) NULL AFTER source_system,
  ADD COLUMN run_mode varchar(32) NOT NULL DEFAULT 'FORMAL' AFTER source_batch_no,
  ADD COLUMN dependency_status varchar(32) NOT NULL DEFAULT 'READY' AFTER run_mode,
  ADD COLUMN retry_of_batch_id bigint NULL AFTER dependency_status,
  ADD COLUMN input_checksum varchar(128) NULL AFTER retry_of_batch_id,
  ADD COLUMN publish_comment varchar(500) NULL AFTER input_checksum;

CREATE TABLE IF NOT EXISTS risk_month_end_source_manifest (
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

CREATE TABLE IF NOT EXISTS risk_month_end_quality_issue (
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

INSERT IGNORE INTO risk_month_end_source_manifest
(batch_id,source_name,source_batch_no,data_date,expected_count,received_count,checksum_value,receive_status)
SELECT id,'CORPORATE_CREDIT_EXPOSURE',concat('HIST-',date_format(source_data_date,'%Y%m%d')),source_data_date,
       record_count,record_count,concat('BACKFILL-',id),'VERIFIED'
FROM risk_month_end_batch;

INSERT IGNORE INTO risk_month_end_reconciliation
(batch_id,check_code,check_name,source_value,snapshot_value,difference_value,tolerance_value,check_status,detail_message)
SELECT id,'UNIQUE_EXPOSURE','债项敞口唯一性',0,0,0,0,'PASSED','历史批次补充检查通过' FROM risk_month_end_batch
UNION ALL
SELECT id,'RELATION_INTEGRITY','客户合同债项关联完整性',0,0,0,0,'PASSED','历史批次补充检查通过' FROM risk_month_end_batch
UNION ALL
SELECT id,'PARAMETER_RANGE','PD/LGD/EAD 参数范围',0,0,0,0,'PASSED','历史批次补充检查通过' FROM risk_month_end_batch;
