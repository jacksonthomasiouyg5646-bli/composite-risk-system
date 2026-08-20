use user_management;

create table if not exists corporate_credit_contract_collateral (
  id bigint unsigned not null auto_increment,
  tenant_id bigint not null default 1,
  relation_no varchar(32) not null,
  contract_no varchar(32) not null,
  collateral_no varchar(32) not null,
  customer_no varchar(32) not null,
  secured_amount decimal(18,2) not null default 0.00,
  pledge_rate decimal(8,4),
  relation_type varchar(32) not null default 'MORTGAGE',
  priority_order int not null default 1,
  effective_date date not null,
  expiry_date date,
  release_date date,
  relation_status varchar(32) not null default 'ACTIVE',
  remark varchar(500),
  created_by bigint unsigned,
  created_by_name varchar(100),
  created_at datetime not null default current_timestamp,
  updated_by bigint unsigned,
  updated_by_name varchar(100),
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_flag tinyint not null default 0,
  primary key (id),
  unique key uk_corporate_contract_collateral_relation_no (relation_no),
  unique key uk_corporate_contract_collateral_pair (contract_no, collateral_no),
  key idx_corporate_contract_collateral_contract (contract_no),
  key idx_corporate_contract_collateral_collateral (collateral_no),
  key idx_corporate_contract_collateral_customer (customer_no),
  key idx_corporate_contract_collateral_status (relation_status),
  constraint fk_corporate_contract_collateral_contract_no
    foreign key (contract_no) references corporate_credit_contract (contract_no),
  constraint fk_corporate_contract_collateral_collateral_no
    foreign key (collateral_no) references corporate_credit_collateral (collateral_no),
  constraint fk_corporate_contract_collateral_customer_no
    foreign key (customer_no) references corporate_customer (customer_no),
  constraint ck_corporate_contract_collateral_amount
    check (secured_amount >= 0),
  constraint ck_corporate_contract_collateral_date
    check (expiry_date is null or expiry_date >= effective_date)
);
