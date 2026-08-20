use user_management;

create table if not exists corporate_credit_contract (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  contract_no varchar(32) not null,
  contract_name varchar(200) not null,
  customer_id bigint unsigned not null,
  customer_no_snapshot varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  application_id bigint unsigned,
  application_no_snapshot varchar(32) not null,
  limit_id bigint unsigned,
  limit_no_snapshot varchar(32),
  contract_type varchar(32) not null default 'FRAME',
  product_type varchar(64) not null,
  currency varchar(8) not null default 'CNY',
  contract_amount decimal(18,2) not null default 0.00,
  available_draw_amount decimal(18,2) not null default 0.00,
  used_draw_amount decimal(18,2) not null default 0.00,
  frozen_amount decimal(18,2) not null default 0.00,
  interest_rate decimal(8,4),
  rate_type varchar(32),
  repayment_method varchar(64),
  loan_term_months int,
  purpose varchar(300),
  guarantee_type varchar(64),
  collateral_desc varchar(500),
  guarantor_desc varchar(500),
  party_a_name varchar(200),
  party_b_name varchar(200),
  signatory_a varchar(100),
  signatory_b varchar(100),
  sign_method varchar(32) default 'ONLINE',
  sign_channel varchar(32) default 'SYSTEM',
  sign_date date,
  effective_date date not null,
  expiry_date date not null,
  approval_no varchar(64),
  approval_date date,
  contract_status varchar(32) not null default 'DRAFT',
  manager_id bigint unsigned,
  manager_name varchar(100),
  owner_org_id bigint unsigned,
  owner_org_name varchar(150),
  contract_file_url varchar(500),
  contract_file_hash varchar(128),
  contract_detail_json json,
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  primary key (id),
  unique key uk_corporate_credit_contract_no (contract_no),
  unique key uk_corporate_credit_contract_application_no (application_no_snapshot),
  key idx_corporate_credit_contract_customer (customer_id),
  key idx_corporate_credit_contract_customer_no (customer_no_snapshot),
  key idx_corporate_credit_contract_application (application_id),
  key idx_corporate_credit_contract_limit (limit_id),
  key idx_corporate_credit_contract_status (contract_status),
  key idx_corporate_credit_contract_expiry (expiry_date),
  key idx_corporate_credit_contract_org (owner_org_id),
  key idx_corporate_credit_contract_tenant (tenant_id),
  constraint fk_corporate_credit_contract_customer
    foreign key (customer_id) references corporate_customer (id),
  constraint fk_corporate_credit_contract_customer_no
    foreign key (customer_no_snapshot) references corporate_customer (customer_no),
  constraint fk_corporate_credit_contract_application
    foreign key (application_id) references corporate_credit_application (id),
  constraint fk_corporate_credit_contract_application_no
    foreign key (application_no_snapshot) references corporate_credit_application (application_no),
  constraint fk_corporate_credit_contract_limit
    foreign key (limit_id) references corporate_credit_limit (id),
  constraint ck_corporate_credit_contract_amount
    check (
      contract_amount >= 0
      and available_draw_amount >= 0
      and used_draw_amount >= 0
      and frozen_amount >= 0
    ),
  constraint ck_corporate_credit_contract_date
    check (expiry_date >= effective_date)
);
