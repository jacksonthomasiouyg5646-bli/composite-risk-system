use user_management;

create table if not exists corporate_debt_default (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  debt_default_no varchar(32) not null,
  customer_no varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  contract_no varchar(32) not null,
  debt_no varchar(32) not null,
  drawdown_no varchar(32),
  exposure_no varchar(32),
  product_type varchar(64) not null,
  currency varchar(8) not null default 'CNY',
  default_level char(1) not null,
  default_date date not null,
  default_recognition_date date not null,
  overdue_days int not null default 0,
  overdue_date date,
  five_category varchar(32),
  rating_result varchar(20),
  overdue_default_flag tinyint not null default 0,
  five_category_default_flag tinyint not null default 0,
  rating_default_flag tinyint not null default 0,
  default_principal_amount decimal(18,2) not null default 0.00,
  default_interest_amount decimal(18,2) not null default 0.00,
  default_exposure_amount decimal(18,2) not null default 0.00,
  recovered_amount decimal(18,2) not null default 0.00,
  loss_amount decimal(18,2) not null default 0.00,
  default_reason varchar(500),
  default_source varchar(64) not null default 'SYSTEM_RULE',
  default_status varchar(32) not null default 'ACTIVE',
  cure_date date,
  resolved_date date,
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
  active_debt_no varchar(32) generated always as (
    case when default_status = 'ACTIVE' and deleted_flag = 0 then debt_no else null end
  ) stored,
  primary key (id),
  unique key uk_corporate_debt_default_no (debt_default_no),
  unique key uk_corporate_debt_default_active_debt (active_debt_no),
  key idx_corporate_debt_default_customer (customer_no),
  key idx_corporate_debt_default_contract (contract_no),
  key idx_corporate_debt_default_debt (debt_no),
  key idx_corporate_debt_default_drawdown (drawdown_no),
  key idx_corporate_debt_default_exposure (exposure_no),
  key idx_corporate_debt_default_level (default_level),
  key idx_corporate_debt_default_date (default_date),
  key idx_corporate_debt_default_status (default_status),
  key idx_corporate_debt_default_data_date (data_date),
  constraint fk_corporate_debt_default_customer_no
    foreign key (customer_no) references corporate_customer (customer_no),
  constraint fk_corporate_debt_default_contract_no
    foreign key (contract_no) references corporate_credit_contract (contract_no),
  constraint fk_corporate_debt_default_drawdown_no
    foreign key (drawdown_no) references corporate_credit_drawdown (drawdown_no),
  constraint ck_corporate_debt_default_level
    check (default_level in ('A', 'B', 'C')),
  constraint ck_corporate_debt_default_rule
    check (
      (default_level = 'A' and overdue_days > 90)
      or (default_level = 'B' and coalesce(five_category, '') in ('SUBSTANDARD', 'DOUBTFUL', 'LOSS'))
      or (default_level = 'C' and rating_result = '21')
    ),
  constraint ck_corporate_debt_default_reason_flag
    check (
      overdue_default_flag = 1
      or five_category_default_flag = 1
      or rating_default_flag = 1
    ),
  constraint ck_corporate_debt_default_amount
    check (
      default_principal_amount >= 0
      and default_interest_amount >= 0
      and default_exposure_amount >= 0
      and recovered_amount >= 0
      and loss_amount >= 0
    ),
  constraint ck_corporate_debt_default_days
    check (overdue_days >= 0),
  constraint ck_corporate_debt_default_flag
    check (
      overdue_default_flag in (0, 1)
      and five_category_default_flag in (0, 1)
      and rating_default_flag in (0, 1)
      and deleted_flag in (0, 1)
    ),
  constraint ck_corporate_debt_default_date
    check (
      default_recognition_date >= default_date
      and (overdue_date is null or overdue_date <= default_date)
      and (cure_date is null or cure_date >= default_date)
      and (resolved_date is null or resolved_date >= default_date)
    )
);
