/*!40101 SET NAMES utf8mb4 */;
USE user_management;

CREATE TABLE IF NOT EXISTS risk_stress_scenario_definition (
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

CREATE TABLE IF NOT EXISTS risk_portfolio_stress_result (
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

CREATE TABLE IF NOT EXISTS risk_customer_group (
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

CREATE TABLE IF NOT EXISTS risk_customer_group_member (
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

INSERT IGNORE INTO risk_stress_scenario_definition
(scenario_code,scenario_name,severity,pd_multiplier,lgd_addon,ead_multiplier,collateral_haircut,description)
VALUES
('BASELINE','基准情景','BASELINE',1.000000,0.000000,1.000000,0.000000,'维持当前组合风险参数'),
('MILD_DOWNTURN','轻度下行情景','MILD',1.250000,0.050000,1.050000,0.100000,'PD 上升 25%，LGD 增加 5 个百分点，押品折价 10%'),
('SEVERE_DOWNTURN','重度下行情景','SEVERE',1.800000,0.120000,1.400000,0.300000,'PD 上升 80%，LGD 增加 12 个百分点，EAD 扩张 40%，押品折价 30%');

UPDATE risk_stress_scenario_definition SET pd_multiplier=1.800000,lgd_addon=0.120000,ead_multiplier=1.400000,
 collateral_haircut=0.300000,description='PD 上升 80%，LGD 增加 12 个百分点，EAD 扩张 40%，押品折价 30%'
WHERE scenario_code='SEVERE_DOWNTURN';

INSERT IGNORE INTO risk_customer_group
(group_code,group_name,controller_customer_no,risk_appetite_amount,owner_org_name)
SELECT concat('GRP-',lpad(group_seq,3,'0')),concat('企业集团 ',lpad(group_seq,3,'0')),
       max(case when member_seq=1 then customer_no end),30000000,max(owner_org_name)
FROM (
  SELECT customer_no,owner_org_name,floor((row_number() over(order by customer_no)-1)/5)+1 group_seq,
         mod(row_number() over(order by customer_no)-1,5)+1 member_seq
  FROM corporate_customer WHERE deleted_flag=0
) x GROUP BY group_seq;

INSERT IGNORE INTO risk_customer_group_member
(group_id,customer_no,relationship_type,ownership_ratio,guarantee_flag,effective_date)
SELECT g.id,x.customer_no,case when x.member_seq=1 then 'CONTROLLER' else 'SUBSIDIARY' end,
       case when x.member_seq=1 then 1 else 0.51 end,case when mod(x.member_seq,2)=0 then 1 else 0 end,'2026-01-01'
FROM (
  SELECT customer_no,floor((row_number() over(order by customer_no)-1)/5)+1 group_seq,
         mod(row_number() over(order by customer_no)-1,5)+1 member_seq
  FROM corporate_customer WHERE deleted_flag=0
) x JOIN risk_customer_group g ON g.group_code=concat('GRP-',lpad(x.group_seq,3,'0'));
