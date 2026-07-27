BEGIN;

DROP TABLE IF EXISTS
  evaluation,
  collateral,
  loan,
  credit_history,
  dossier_individual,
  dossier_corporate,
  dossier,
  ai_prompt,
  ai_model,
  employee_admin,
  employee_manager,
  employee_analyst,
  employee
CASCADE;

COMMIT;
