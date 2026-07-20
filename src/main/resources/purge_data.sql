BEGIN;

-- Delete data in reverse order of dependencies to avoid foreign key violations
DELETE FROM evaluation;
DELETE FROM collateral;
DELETE FROM loan;
DELETE FROM credit_history;
DELETE FROM dossier_corporate;
DELETE FROM dossier_individual;
DELETE FROM dossier;
DELETE FROM ai_prompt;
DELETE FROM ai_model;
DELETE FROM employee;

-- Reset sequences
ALTER TABLE employee ALTER COLUMN id RESTART WITH 1;
ALTER TABLE ai_model ALTER COLUMN id RESTART WITH 1;
ALTER TABLE ai_prompt ALTER COLUMN id RESTART WITH 1;
ALTER TABLE dossier ALTER COLUMN id RESTART WITH 1;
ALTER TABLE credit_history ALTER COLUMN id RESTART WITH 1;
ALTER TABLE loan ALTER COLUMN id RESTART WITH 1;
ALTER TABLE collateral ALTER COLUMN id RESTART WITH 1;
ALTER TABLE evaluation ALTER COLUMN id RESTART WITH 1;

COMMIT;
