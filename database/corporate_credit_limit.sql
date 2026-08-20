use user_management;

create table if not exists corporate_credit_limit (
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
