use user_management;

create table if not exists corporate_customer_default (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  customer_default_no varchar(32) not null,
  customer_no varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  default_flag tinyint not null default 1,
  first_default_date date not null,
  latest_default_date date not null,
  highest_default_level char(1) not null,
  active_default_debt_count int not null default 0,
  total_default_debt_count int not null default 0,
  overdue90_debt_count int not null default 0,
  five_category_default_debt_count int not null default 0,
  rating_default_debt_count int not null default 0,
  default_principal_amount decimal(18,2) not null default 0.00,
  default_interest_amount decimal(18,2) not null default 0.00,
  default_exposure_amount decimal(18,2) not null default 0.00,
  recovered_amount decimal(18,2) not null default 0.00,
  loss_amount decimal(18,2) not null default 0.00,
  default_reason_summary varchar(1000),
  default_source varchar(64) not null default 'DEBT_AGGREGATION',
  default_status varchar(32) not null default 'ACTIVE',
  cure_date date,
  resolved_date date,
  aggregation_batch_no varchar(64),
  aggregation_time datetime not null default current_timestamp,
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
  active_customer_no varchar(32) generated always as (
    case when default_status = 'ACTIVE' and deleted_flag = 0 then customer_no else null end
  ) stored,
  primary key (id),
  unique key uk_corporate_customer_default_no (customer_default_no),
  unique key uk_corporate_customer_default_active_customer (active_customer_no),
  key idx_corporate_customer_default_customer (customer_no),
  key idx_corporate_customer_default_level (highest_default_level),
  key idx_corporate_customer_default_status (default_status),
  key idx_corporate_customer_default_first_date (first_default_date),
  key idx_corporate_customer_default_latest_date (latest_default_date),
  key idx_corporate_customer_default_data_date (data_date),
  constraint fk_corporate_customer_default_customer_no
    foreign key (customer_no) references corporate_customer (customer_no),
  constraint ck_corporate_customer_default_level
    check (highest_default_level in ('A', 'B', 'C')),
  constraint ck_corporate_customer_default_count
    check (
      active_default_debt_count >= 0
      and total_default_debt_count >= 0
      and overdue90_debt_count >= 0
      and five_category_default_debt_count >= 0
      and rating_default_debt_count >= 0
      and total_default_debt_count >= active_default_debt_count
      and (default_status <> 'ACTIVE' or active_default_debt_count > 0)
    ),
  constraint ck_corporate_customer_default_amount
    check (
      default_principal_amount >= 0
      and default_interest_amount >= 0
      and default_exposure_amount >= 0
      and recovered_amount >= 0
      and loss_amount >= 0
    ),
  constraint ck_corporate_customer_default_flag
    check (
      default_flag in (0, 1)
      and deleted_flag in (0, 1)
    ),
  constraint ck_corporate_customer_default_date
    check (
      latest_default_date >= first_default_date
      and (cure_date is null or cure_date >= first_default_date)
      and (resolved_date is null or resolved_date >= first_default_date)
    )
);
