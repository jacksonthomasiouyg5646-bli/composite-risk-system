SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
USE user_management;

UPDATE corporate_rating
SET rating_date = '2025-09-15', valid_from = '2025-09-15', valid_to = '2026-09-14'
WHERE rating_no LIKE 'RATE20260721%' AND created_by_name = 'system';

UPDATE corporate_credit_limit
SET approval_date = '2025-10-01', effective_date = '2025-10-01', expiry_date = '2026-10-01'
WHERE limit_no LIKE 'LIM20260721%' AND created_by_name = 'system';

UPDATE corporate_credit_application
SET submitted_at = DATE_ADD('2025-10-15 09:00:00', INTERVAL CAST(SUBSTRING(application_no, 12, 4) AS UNSIGNED) DAY),
    approved_at = DATE_ADD('2025-10-15 10:00:00', INTERVAL CAST(SUBSTRING(application_no, 12, 4) AS UNSIGNED) DAY)
WHERE application_no LIKE 'APP20260721%' AND created_by_name = 'system';

UPDATE corporate_credit_contract ct
JOIN corporate_credit_application app ON app.application_no = ct.application_no_snapshot
SET ct.sign_date = DATE_ADD(DATE(app.submitted_at), INTERVAL 1 DAY),
    ct.effective_date = DATE_ADD(DATE(app.submitted_at), INTERVAL 1 DAY),
    ct.approval_date = DATE(app.approved_at),
    ct.expiry_date = DATE_ADD(DATE_ADD(DATE(app.submitted_at), INTERVAL 1 DAY), INTERVAL ct.loan_term_months MONTH)
WHERE ct.contract_no LIKE 'CON20260721%' AND ct.created_by_name = 'system';

UPDATE corporate_credit_drawdown d
JOIN corporate_credit_contract ct ON ct.contract_no = d.contract_no_snapshot
SET d.drawdown_date = DATE_ADD(ct.effective_date, INTERVAL CAST(RIGHT(d.drawdown_no, 2) AS UNSIGNED) DAY),
    d.value_date = DATE_ADD(ct.effective_date, INTERVAL CAST(RIGHT(d.drawdown_no, 2) AS UNSIGNED) DAY),
    d.approval_date = DATE_ADD(ct.effective_date, INTERVAL CAST(RIGHT(d.drawdown_no, 2) AS UNSIGNED) DAY),
    d.maturity_date = DATE_ADD(DATE_ADD(ct.effective_date, INTERVAL CAST(RIGHT(d.drawdown_no, 2) AS UNSIGNED) DAY), INTERVAL d.term_days DAY)
WHERE d.drawdown_no LIKE 'DRAW20260721%' AND d.created_by_name = 'system';

UPDATE corporate_credit_overdue o
JOIN corporate_credit_drawdown d ON d.debt_no = o.debt_no AND d.deleted_flag = 0
SET o.due_date = DATE_SUB(DATE_SUB(o.data_date, INTERVAL o.overdue_days DAY), INTERVAL 1 DAY),
    o.overdue_date = DATE_SUB(o.data_date, INTERVAL o.overdue_days DAY),
    o.grace_due_date = DATE_ADD(DATE_SUB(o.data_date, INTERVAL o.overdue_days DAY), INTERVAL o.grace_days DAY)
WHERE o.overdue_no LIKE 'OVD20260721%' AND o.created_by_name = 'system';

UPDATE corporate_debt_default dd
JOIN corporate_credit_overdue o ON o.debt_no = dd.debt_no AND o.deleted_flag = 0
SET dd.overdue_date = o.overdue_date,
    dd.default_date = DATE_ADD(o.overdue_date, INTERVAL 1 DAY),
    dd.default_recognition_date = DATE_ADD(o.overdue_date, INTERVAL 1 DAY)
WHERE dd.debt_default_no LIKE 'DDEF20260721%' AND dd.created_by_name = 'system';

UPDATE corporate_customer_default cd
JOIN (
  SELECT customer_no, MIN(default_date) AS first_default_date, MAX(default_date) AS latest_default_date
  FROM corporate_debt_default
  WHERE deleted_flag = 0 AND default_status = 'ACTIVE'
  GROUP BY customer_no
) defaults_by_customer ON defaults_by_customer.customer_no = cd.customer_no
SET cd.first_default_date = defaults_by_customer.first_default_date,
    cd.latest_default_date = defaults_by_customer.latest_default_date
WHERE cd.customer_default_no LIKE 'CDEF20260721%' AND cd.created_by_name = 'system';
