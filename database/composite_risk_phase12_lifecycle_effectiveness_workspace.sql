/*!40101 SET NAMES utf8mb4 */;
USE user_management;

CREATE TABLE IF NOT EXISTS risk_user_workbench_preference (
  id bigint primary key auto_increment,
  username varchar(100) not null,
  workspace_role varchar(32) not null default 'RISK_MANAGER',
  default_tab varchar(64) not null default 'limits',
  density_mode varchar(16) not null default 'COMFORTABLE',
  visible_modules_json json,
  updated_by varchar(100) not null,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_workbench_preference_user (username)
);

INSERT IGNORE INTO risk_user_workbench_preference
(username,workspace_role,default_tab,density_mode,visible_modules_json,updated_by)
VALUES ('admin','RISK_MANAGER','limits','COMFORTABLE',json_array('limits','forecast','stress','groups','backtest','effectiveness','lifecycle'),'SYSTEM');
