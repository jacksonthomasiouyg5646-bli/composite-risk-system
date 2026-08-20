use user_management;

create table if not exists corporate_risk_exposure (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  exposure_no varchar(32) not null,
  customer_no varchar(32) not null,
  customer_name_snapshot varchar(200) not null,
  contract_no varchar(32) not null,
  debt_no varchar(32) not null,
  drawdown_no varchar(32),
  product_type varchar(64) not null,
  currency varchar(8) not null default 'CNY',
  exposure_balance decimal(18,2) not null default 0.00,
  ead_amount decimal(18,2) not null default 0.00,
  undrawn_amount decimal(18,2) not null default 0.00,
  collateral_value decimal(18,2) not null default 0.00,
  guaranteed_amount decimal(18,2) not null default 0.00,
  lgd_avg decimal(10,6) not null default 0.000000,
  lgd_downturn decimal(10,6) not null default 0.000000,
  pd decimal(10,6) not null default 0.000000,
  el_avg decimal(18,6) not null default 0.000000,
  el_downturn decimal(18,6) not null default 0.000000,
  rating_result varchar(20),
  product_recovery_rate decimal(10,6) not null default 0.000000,
  recovery_source varchar(64),
  five_category varchar(32) default 'NORMAL',
  risk_level varchar(32) default 'LOW',
  overdue_days int not null default 0,
  default_flag tinyint not null default 0,
  measurement_date date not null,
  data_date date not null,
  model_name varchar(100),
  model_version varchar(64),
  scenario_type varchar(32) default 'BASE',
  exposure_status varchar(32) not null default 'ACTIVE',
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  primary key (id),
  unique key uk_corporate_risk_exposure_no (exposure_no),
  unique key uk_corporate_risk_exposure_debt_date (debt_no, data_date),
  key idx_corporate_risk_exposure_customer (customer_no),
  key idx_corporate_risk_exposure_contract (contract_no),
  key idx_corporate_risk_exposure_drawdown (drawdown_no),
  key idx_corporate_risk_exposure_rating (rating_result),
  key idx_corporate_risk_exposure_level (risk_level),
  key idx_corporate_risk_exposure_status (exposure_status),
  key idx_corporate_risk_exposure_measurement_date (measurement_date),
  constraint fk_corporate_risk_exposure_customer_no
    foreign key (customer_no) references corporate_customer (customer_no),
  constraint fk_corporate_risk_exposure_contract_no
    foreign key (contract_no) references corporate_credit_contract (contract_no),
  constraint fk_corporate_risk_exposure_drawdown_no
    foreign key (drawdown_no) references corporate_credit_drawdown (drawdown_no),
  constraint ck_corporate_risk_exposure_amount
    check (
      exposure_balance >= 0
      and ead_amount >= 0
      and undrawn_amount >= 0
      and collateral_value >= 0
      and guaranteed_amount >= 0
      and el_avg >= 0
      and el_downturn >= 0
    ),
  constraint ck_corporate_risk_exposure_ratio
    check (
      lgd_avg >= 0 and lgd_avg <= 1
      and lgd_downturn >= 0 and lgd_downturn <= 1
      and pd >= 0 and pd <= 1
      and product_recovery_rate >= 0 and product_recovery_rate <= 1
    ),
  constraint ck_corporate_risk_exposure_overdue_days
    check (overdue_days >= 0)
);
