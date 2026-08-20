use user_management;

insert into corporate_customer_default (
  tenant_id,
  customer_default_no,
  customer_no,
  customer_name_snapshot,
  default_flag,
  first_default_date,
  latest_default_date,
  highest_default_level,
  active_default_debt_count,
  total_default_debt_count,
  overdue90_debt_count,
  five_category_default_debt_count,
  rating_default_debt_count,
  default_principal_amount,
  default_interest_amount,
  default_exposure_amount,
  recovered_amount,
  loss_amount,
  default_reason_summary,
  default_source,
  default_status,
  aggregation_batch_no,
  aggregation_time,
  data_date,
  created_by,
  created_by_name
)
select
  min(d.tenant_id) as tenant_id,
  concat('CDEF', date_format(now(), '%Y%m%d%H%i%s'), upper(substr(md5(min(d.customer_no)), 1, 8))) as customer_default_no,
  d.customer_no,
  max(d.customer_name_snapshot) as customer_name_snapshot,
  1 as default_flag,
  min(d.default_date) as first_default_date,
  max(d.default_date) as latest_default_date,
  case min(case d.default_level when 'A' then 1 when 'B' then 2 when 'C' then 3 else 9 end)
    when 1 then 'A'
    when 2 then 'B'
    else 'C'
  end as highest_default_level,
  count(*) as active_default_debt_count,
  count(*) as total_default_debt_count,
  sum(case when d.overdue_default_flag = 1 then 1 else 0 end) as overdue90_debt_count,
  sum(case when d.five_category_default_flag = 1 then 1 else 0 end) as five_category_default_debt_count,
  sum(case when d.rating_default_flag = 1 then 1 else 0 end) as rating_default_debt_count,
  sum(d.default_principal_amount) as default_principal_amount,
  sum(d.default_interest_amount) as default_interest_amount,
  sum(d.default_exposure_amount) as default_exposure_amount,
  sum(d.recovered_amount) as recovered_amount,
  sum(d.loss_amount) as loss_amount,
  concat(
    'active_default_debt_count=', count(*),
    '; overdue90_debt_count=', sum(case when d.overdue_default_flag = 1 then 1 else 0 end),
    '; five_category_default_debt_count=', sum(case when d.five_category_default_flag = 1 then 1 else 0 end),
    '; rating_default_debt_count=', sum(case when d.rating_default_flag = 1 then 1 else 0 end)
  ) as default_reason_summary,
  'DEBT_AGGREGATION' as default_source,
  'ACTIVE' as default_status,
  concat('BATCH', date_format(now(), '%Y%m%d%H%i%s')) as aggregation_batch_no,
  now() as aggregation_time,
  curdate() as data_date,
  0 as created_by,
  'system' as created_by_name
from corporate_debt_default d
where d.default_status = 'ACTIVE'
  and d.deleted_flag = 0
group by d.customer_no
on duplicate key update
  customer_name_snapshot = values(customer_name_snapshot),
  default_flag = 1,
  first_default_date = values(first_default_date),
  latest_default_date = values(latest_default_date),
  highest_default_level = values(highest_default_level),
  active_default_debt_count = values(active_default_debt_count),
  total_default_debt_count = values(total_default_debt_count),
  overdue90_debt_count = values(overdue90_debt_count),
  five_category_default_debt_count = values(five_category_default_debt_count),
  rating_default_debt_count = values(rating_default_debt_count),
  default_principal_amount = values(default_principal_amount),
  default_interest_amount = values(default_interest_amount),
  default_exposure_amount = values(default_exposure_amount),
  recovered_amount = values(recovered_amount),
  loss_amount = values(loss_amount),
  default_reason_summary = values(default_reason_summary),
  default_status = 'ACTIVE',
  resolved_date = null,
  aggregation_batch_no = values(aggregation_batch_no),
  aggregation_time = values(aggregation_time),
  data_date = values(data_date),
  updated_by = 0,
  updated_by_name = 'system';

update corporate_customer_default c
left join (
  select customer_no
  from corporate_debt_default
  where default_status = 'ACTIVE'
    and deleted_flag = 0
  group by customer_no
) d on d.customer_no = c.customer_no
set
  c.default_flag = 0,
  c.default_status = 'RESOLVED',
  c.active_default_debt_count = 0,
  c.resolved_date = curdate(),
  c.aggregation_time = now(),
  c.updated_by = 0,
  c.updated_by_name = 'system'
where c.default_status = 'ACTIVE'
  and c.deleted_flag = 0
  and d.customer_no is null;
