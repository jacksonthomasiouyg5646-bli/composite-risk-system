use user_management;

create table if not exists corporate_credit_drawdown (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  drawdown_no varchar(32) not null,
  debt_no varchar(32),
  customer_id bigint unsigned not null,
  customer_no_snapshot varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  contract_id bigint unsigned not null,
  contract_no_snapshot varchar(32) not null,
  application_id bigint unsigned,
  application_no_snapshot varchar(32),
  limit_id bigint unsigned,
  limit_no_snapshot varchar(32),
  product_type varchar(64) not null,
  drawdown_type varchar(32) not null default 'LOAN',
  currency varchar(8) not null default 'CNY',
  apply_draw_amount decimal(18,2) not null default 0.00,
  approved_draw_amount decimal(18,2) not null default 0.00,
  actual_draw_amount decimal(18,2) not null default 0.00,
  repaid_principal_amount decimal(18,2) not null default 0.00,
  outstanding_principal_amount decimal(18,2) not null default 0.00,
  interest_receivable_amount decimal(18,2) not null default 0.00,
  interest_paid_amount decimal(18,2) not null default 0.00,
  overdue_principal_amount decimal(18,2) not null default 0.00,
  overdue_interest_amount decimal(18,2) not null default 0.00,
  interest_rate decimal(8,4),
  rate_type varchar(32),
  repayment_method varchar(64),
  interest_payment_method varchar(64),
  drawdown_date date not null,
  value_date date,
  maturity_date date not null,
  term_days int,
  fund_usage varchar(300),
  payee_name varchar(200),
  payee_bank_name varchar(150),
  payee_account_no_enc varchar(512),
  payee_account_no_hash varchar(128),
  loan_account_no_enc varchar(512),
  loan_account_no_hash varchar(128),
  guarantee_type varchar(64),
  collateral_desc varchar(500),
  guarantor_desc varchar(500),
  five_category varchar(32) default 'NORMAL',
  risk_level varchar(32) default 'LOW',
  overdue_days int not null default 0,
  drawdown_status varchar(32) not null default 'DRAFT',
  approval_no varchar(64),
  approval_date date,
  disbursement_voucher_no varchar(64),
  manager_id bigint unsigned,
  manager_name varchar(100),
  owner_org_id bigint unsigned,
  owner_org_name varchar(150),
  drawdown_detail_json json,
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  primary key (id),
  unique key uk_corporate_credit_drawdown_no (drawdown_no),
  key idx_corporate_credit_drawdown_debt_no (debt_no),
  key idx_corporate_credit_drawdown_customer (customer_id),
  key idx_corporate_credit_drawdown_contract (contract_id),
  key idx_corporate_credit_drawdown_contract_no (contract_no_snapshot),
  key idx_corporate_credit_drawdown_application (application_id),
  key idx_corporate_credit_drawdown_limit (limit_id),
  key idx_corporate_credit_drawdown_status (drawdown_status),
  key idx_corporate_credit_drawdown_maturity (maturity_date),
  key idx_corporate_credit_drawdown_five_category (five_category),
  key idx_corporate_credit_drawdown_risk (risk_level),
  key idx_corporate_credit_drawdown_org (owner_org_id),
  key idx_corporate_credit_drawdown_tenant (tenant_id),
  constraint fk_corporate_credit_drawdown_customer
    foreign key (customer_id) references corporate_customer (id),
  constraint fk_corporate_credit_drawdown_contract
    foreign key (contract_id) references corporate_credit_contract (id),
  constraint fk_corporate_credit_drawdown_contract_no
    foreign key (contract_no_snapshot) references corporate_credit_contract (contract_no),
  constraint fk_corporate_credit_drawdown_application
    foreign key (application_id) references corporate_credit_application (id),
  constraint fk_corporate_credit_drawdown_limit
    foreign key (limit_id) references corporate_credit_limit (id),
  constraint ck_corporate_credit_drawdown_amount
    check (
      apply_draw_amount >= 0
      and approved_draw_amount >= 0
      and actual_draw_amount >= 0
      and repaid_principal_amount >= 0
      and outstanding_principal_amount >= 0
      and interest_receivable_amount >= 0
      and interest_paid_amount >= 0
      and overdue_principal_amount >= 0
      and overdue_interest_amount >= 0
    ),
  constraint ck_corporate_credit_drawdown_date
    check (maturity_date >= drawdown_date),
  constraint ck_corporate_credit_drawdown_overdue_days
    check (overdue_days >= 0)
);
