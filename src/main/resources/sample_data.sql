BEGIN;

INSERT INTO employee (id, email, username, password, last_name, first_name, national_id, gender, phone_number, hire_date, salary, role) VALUES
(1, 'admin@talan.com', 'admin', '$2a$10$vK3M25Ld99W08wB0B1mGpeVpI0L/ZofWjTq4gTid06Q47H10U3ia.', 'System', 'Administrator', 'NID-0001', 'M', '+21671000001', '2024-01-15', 5500.00, 'admin'),
(2, 'banker@talan.com', 'banker', '$2a$10$9X2DbeGZf537.oVpP19MueNfA88P9z8Cgq5U2G.SBeR/7A1YvA02G', 'Dupont', 'Jean', 'NID-0002', 'M', '+21671000002', '2025-03-01', 3800.00, 'manager'),
(3, 'analyst@talan.com', 'analyst', '$2a$10$rDpGshq65XfA752A1w0KLeUvB0A7BkWzYIcl09XlGvC5hV9wS8Biu', 'Martin', 'Claire', 'NID-0003', 'F', '+21671000003', '2025-06-01', 3200.00, 'analyst')
ON CONFLICT (id) DO NOTHING;

ALTER TABLE employee ALTER COLUMN id RESTART WITH 4;

INSERT INTO employee_admin (employee_id) VALUES (1) ON CONFLICT DO NOTHING;
INSERT INTO employee_manager (employee_id) VALUES (2) ON CONFLICT DO NOTHING;
INSERT INTO employee_analyst (employee_id) VALUES (3) ON CONFLICT DO NOTHING;

INSERT INTO ai_model (id, stage_name, model_name, context_window_size, temperature, keep_alive_setting, is_active) VALUES
(1, 'solvency', 'deepseek-r1:8b', 8192, 0.2, '5m', true),
(2, 'history', 'deepseek-r1:8b', 8192, 0.1, '5m', true),
(3, 'guarantees', 'deepseek-r1:8b', 8192, 0.3, '5m', true),
(4, 'compliance', 'deepseek-r1:8b', 4096, 0.0, '5m', true),
(5, 'supervisor', 'deepseek-r1:14b', 16384, 0.4, '15m', true)
ON CONFLICT (id) DO NOTHING;

ALTER TABLE ai_model ALTER COLUMN id RESTART WITH 6;

INSERT INTO ai_prompt (id, ai_model_id, prompt_text, version_tag, updated_at) VALUES
(1, 1, 'Analyze client solvency, monthly revenue ratios, and balance parameters.', 'v1.0.0', '2026-07-12 10:00:00'),
(2, 2, 'Examine credit records historical entries for late settlements or active defaults.', 'v1.0.0', '2026-07-12 10:00:00'),
(3, 3, 'Evaluate provided assets value against total liability exposure margins.', 'v1.0.0', '2026-07-12 10:00:00'),
(4, 4, 'Verify client entity legal structure parameters against risk guidelines.', 'v1.0.0', '2026-07-12 10:00:00'),
(5, 5, 'Consolidate individual analytical tracks into a final markdown scoring verdict.', 'v1.0.0', '2026-07-12 10:00:00')
ON CONFLICT (id) DO NOTHING;

ALTER TABLE ai_prompt ALTER COLUMN id RESTART WITH 6;

INSERT INTO dossier (id, siren, client_type, status, creation_date, assigned_analyst_id, approved_by_id) VALUES
(1, NULL, 'individual', 'approved', '2026-07-01 09:15:00', 3, 2),
(2, '123456789', 'corporate', 'in_progress', '2026-07-13 11:00:00', 3, NULL)
ON CONFLICT (id) DO NOTHING;

ALTER TABLE dossier ALTER COLUMN id RESTART WITH 3;

INSERT INTO dossier_individual (dossier_id, last_name, first_name, national_id, date_of_birth, profession, monthly_income) VALUES
(1, 'Ben Ali', 'Sami', 'CIN-09912345', '1992-05-14', 'Senior Software Engineer', 4200.00)
ON CONFLICT (dossier_id) DO NOTHING;

INSERT INTO dossier_corporate (dossier_id, company_name, tax_registration_number, legal_form, registered_office_address, fiscal_year, turnover, net_income, total_debt, equity, liquidity_ratio) VALUES
(2, 'Talan Tunisie SARL', '1234567MAM000', 'SARL', 'Rue des Entrepreneurs, Charguia II, Tunis', 2025, 1250000.00, 180000.00, 300000.00, 450000.00, 1.65)
ON CONFLICT (dossier_id) DO NOTHING;

INSERT INTO credit_history (id, dossier_id, institution_name, incident_type, amount_in_delinquency, resolution_status, reported_date) VALUES
(1, 1, 'Banque Centrale', 'none', 0.00, true, '2026-01-10'),
(2, 2, 'BIAT', 'late_payment', 12000.00, true, '2025-11-04')
ON CONFLICT (id) DO NOTHING;

ALTER TABLE credit_history ALTER COLUMN id RESTART WITH 3;

INSERT INTO loan (id, dossier_id, amount, interest_rate, term_months, payment_frequency, start_date, status) VALUES
(1, 1, 85000.00, 4.25, 60, 'monthly', '2026-07-05', 'active'),
(2, 2, 250000.00, 6.50, 36, 'quarterly', NULL, 'active')
ON CONFLICT (id) DO NOTHING;

ALTER TABLE loan ALTER COLUMN id RESTART WITH 3;

INSERT INTO collateral (id, loan_id, type, description, estimated_value, valuation_date, status) VALUES
(1, 1, 'real_estate', 'Appartement Residence El Ons, Tunis', 110000.00, '2026-06-20', 'held'),
(2, 2, 'personal_guarantee', 'Corporate Partner Fondateur Caution Solidaire', 200000.00, '2026-07-10', 'held')
ON CONFLICT (id) DO NOTHING;

ALTER TABLE collateral ALTER COLUMN id RESTART WITH 3;

INSERT INTO evaluation (id, dossier_id, ai_model_id, execution_mode, solvency_stage_output, solvency_duration_ms, history_stage_output, history_duration_ms, guarantees_stage_output, guarantees_duration_ms, compliance_stage_output, compliance_duration_ms, supervisor_stage_output, supervisor_duration_ms, created_at) VALUES
(1, 1, 5, 'FAST',
 'Solvency verified. DTI ratio is at 24%, safely below maximum thresholds.', 1240,
 'No baseline active default anomalies encountered in local banking historical registries.', 980,
 'Collateral value covers loan principal amount at 129% scaling efficiency ratio.', 1150,
 'All mandatory regulatory documentation targets completed, checked, and validated.', 620,
 '# Final Evaluation Report Summary\n\n**Verdict**: APPROVED\n\nClient displays robust indicators with optimal liability metrics.', 3450,
 '2026-07-01 10:30:00')
ON CONFLICT (id) DO NOTHING;

ALTER TABLE evaluation ALTER COLUMN id RESTART WITH 2;

COMMIT;
