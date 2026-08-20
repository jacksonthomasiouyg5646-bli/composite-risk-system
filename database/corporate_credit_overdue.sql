use user_management;

create table if not exists corporate_credit_overdue (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  overdue_no varchar(32) not null,
  customer_no varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  contract_no varchar(32) not null,
  debt_no varchar(32) not null,
  drawdown_no varchar(32),
  installment_no varchar(64),
  product_type varchar(64) not null,
  currency varchar(8) not null default 'CNY',
  due_date date not null,
  overdue_date date not null,
  overdue_days int not null default 0,
  grace_days int not null default 0,
  grace_due_date date,
  legal_holiday_flag tinyint not null default 0,
  holiday_calendar_code varchar(64),
  legal_holiday_name varchar(100),
  principal_due_amount decimal(18,2) not null default 0.00,
  interest_due_amount decimal(18,2) not null default 0.00,
  overdue_principal_amount decimal(18,2) not null default 0.00,
  overdue_interest_amount decimal(18,2) not null default 0.00,
  penalty_interest_amount decimal(18,2) not null default 0.00,
  compound_interest_amount decimal(18,2) not null default 0.00,
  fee_overdue_amount decimal(18,2) not null default 0.00,
  total_overdue_amount decimal(18,2) not null default 0.00,
  repaid_overdue_amount decimal(18,2) not null default 0.00,
  remaining_overdue_amount decimal(18,2) not null default 0.00,
  last_repay_date date,
  resolved_date date,
  overdue_stage varchar(32) not null default 'M1',
  collection_status varchar(32) not null default 'PENDING',
  repayment_status varchar(32) not null default 'OVERDUE',
  default_flag tinyint not null default 0,
  extension_flag tinyint not null default 0,
  five_category varchar(32) default 'NORMAL',
  risk_level varchar(32) default 'LOW',
  manager_id bigint unsigned,
  manager_name varchar(100),
  owner_org_id bigint unsigned,
  owner_org_name varchar(150),
  data_date date not null,
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  primary key (id),
  unique key uk_corporate_credit_overdue_no (overdue_no),
  unique key uk_corporate_credit_overdue_debt_due (debt_no, due_date, installment_no),
  key idx_corporate_credit_overdue_customer (customer_no),
  key idx_corporate_credit_overdue_contract (contract_no),
  key idx_corporate_credit_overdue_drawdown (drawdown_no),
  key idx_corporate_credit_overdue_debt (debt_no),
  key idx_corporate_credit_overdue_due_date (due_date),
  key idx_corporate_credit_overdue_overdue_date (overdue_date),
  key idx_corporate_credit_overdue_grace_due_date (grace_due_date),
  key idx_corporate_credit_overdue_status (repayment_status),
  key idx_corporate_credit_overdue_collection (collection_status),
  key idx_corporate_credit_overdue_risk (risk_level),
  key idx_corporate_credit_overdue_data_date (data_date),
  constraint fk_corporate_credit_overdue_customer_no
    foreign key (customer_no) references corporate_customer (customer_no),
  constraint fk_corporate_credit_overdue_contract_no
    foreign key (contract_no) references corporate_credit_contract (contract_no),
  constraint fk_corporate_credit_overdue_drawdown_no
    foreign key (drawdown_no) references corporate_credit_drawdown (drawdown_no),
  constraint ck_corporate_credit_overdue_amount
    check (
      principal_due_amount >= 0
      and interest_due_amount >= 0
      and overdue_principal_amount >= 0
      and overdue_interest_amount >= 0
      and penalty_interest_amount >= 0
      and compound_interest_amount >= 0
      and fee_overdue_amount >= 0
      and total_overdue_amount >= 0
      and repaid_overdue_amount >= 0
      and remaining_overdue_amount >= 0
    ),
  constraint ck_corporate_credit_overdue_days
    check (overdue_days >= 0 and grace_days >= 0),
  constraint ck_corporate_credit_overdue_flag
    check (
      legal_holiday_flag in (0, 1)
      and default_flag in (0, 1)
      and extension_flag in (0, 1)
      and deleted_flag in (0, 1)
    ),
  constraint ck_corporate_credit_overdue_date
    check (
      overdue_date >= due_date
      and (grace_due_date is null or grace_due_date >= overdue_date)
      and (last_repay_date is null or last_repay_date >= due_date)
      and (resolved_date is null or resolved_date >= overdue_date)
    )
);
