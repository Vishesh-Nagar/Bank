CREATE DATABASE IF NOT EXISTS bank_users;
CREATE DATABASE IF NOT EXISTS bank_accounts;
CREATE DATABASE IF NOT EXISTS bank_payments;

-- Note: Audit logs are stored in the 'bank_users' database
-- (Table 'audit_logs' is auto-created by Hibernate).
