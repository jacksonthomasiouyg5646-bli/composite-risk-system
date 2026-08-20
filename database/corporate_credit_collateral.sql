use user_management;

create table if not exists corporate_credit_collateral (
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
