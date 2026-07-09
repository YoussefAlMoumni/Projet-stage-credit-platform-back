-- ============================================================
-- Credit Platform - Sample Data
-- ============================================================
-- Run this AFTER starting CreditPlatformApplication once
-- so that Hibernate has already created the table schema.
-- ============================================================

-- Clear existing data (order matters due to FK constraints)
DELETE FROM stage_results;
DELETE FROM evaluations;
DELETE FROM dossiers;
-- NOTE: Do NOT delete users here. User seeding (admin/banker/analyst)
-- is handled automatically by DatabaseInitializer.java on every startup
-- with correct BCrypt-encoded passwords.

-- ============================================================
-- DOSSIERS (Credit Application Files)
-- ============================================================
INSERT INTO dossiers (siren, name, type_client, montant_demande, raw_data) VALUES
(
    '123456789',
    'Dupont Industries',
    'Personne Morale',
    '250000',
    '{"chiffre_affaires": "1200000", "resultat_net": "85000", "effectif": 42, "secteur": "Industrie Manufacturière", "anciennete": 12}'
),
(
    '987654321',
    'Marie Lefebvre',
    'Personne Physique',
    '75000',
    '{"revenus_mensuels": "4500", "charges_mensuelles": "1200", "apport_personnel": "15000", "type_projet": "Achat Immobilier", "situation_professionnelle": "CDI"}'
),
(
    '456789123',
    'Tech Solutions SARL',
    'Personne Morale',
    '500000',
    '{"chiffre_affaires": "3500000", "resultat_net": "210000", "effectif": 78, "secteur": "Technologies de l Information", "anciennete": 7}'
),
(
    '321654987',
    'Jean-Pierre Martin',
    'Personne Physique',
    '30000',
    '{"revenus_mensuels": "2800", "charges_mensuelles": "950", "apport_personnel": "5000", "type_projet": "Travaux Renovation", "situation_professionnelle": "CDI"}'
),
(
    '654321789',
    'Boulangerie Artisanale Morin',
    'Personne Morale',
    '120000',
    '{"chiffre_affaires": "480000", "resultat_net": "32000", "effectif": 8, "secteur": "Agroalimentaire", "anciennete": 25}'
);

-- ============================================================
-- EVALUATIONS (Sample completed evaluations)
-- ============================================================
INSERT INTO evaluations (dossier_siren, mode, final_report, created_at) VALUES
(
    '123456789',
    'FAST',
    '# Rapport de Décision - Dupont Industries

## Décision : APPROUVÉ

Après consolidation des analyses des quatre spécialistes, le dossier Dupont Industries (SIREN: 123456789) présente un profil de risque acceptable.

### Points Positifs
- Ratio d''endettement maîtrisé (< 35%)
- Historique bancaire sans incident sur 5 ans
- Garanties réelles couvrant 120% du montant demandé
- Conformité KYC/AML validée

### Conditions d''Octroi
- Montant accordé : 250 000 €
- Durée : 84 mois
- Taux : 3.75% fixe

**Décision finale : ACCORD**',
    '2026-07-01 10:30:00'
),
(
    '987654321',
    'FAST',
    '# Rapport de Décision - Marie Lefebvre

## Décision : APPROUVÉ

Le dossier de Mme Marie Lefebvre présente des indicateurs financiers solides pour un financement immobilier.

### Points Positifs
- Revenus stables en CDI depuis 8 ans
- Taux d''endettement post-prêt : 37% (acceptable)
- Apport personnel de 20%
- Aucun incident de paiement

### Conditions d''Octroi
- Montant accordé : 75 000 €
- Durée : 180 mois
- Taux : 3.20% fixe

**Décision finale : ACCORD**',
    '2026-07-02 14:15:00'
),
(
    '321654987',
    'FAST',
    '# Rapport de Décision - Jean-Pierre Martin

## Décision : REFUSÉ

Le dossier présente un niveau de risque trop élevé au regard des critères d''octroi actuels.

### Points Négatifs
- Taux d''endettement post-prêt : 67% (trop élevé)
- Un incident de paiement enregistré en 2024
- Apport personnel insuffisant (< 10%)

**Décision finale : REFUS**',
    '2026-07-03 09:45:00'
);

-- ============================================================
-- STAGE RESULTS (AI pipeline outputs for evaluation 1)
-- ============================================================
INSERT INTO stage_results (evaluation_id, stage_name, output, duration_ms)
SELECT id, 'solvabilite',
'Analyse Solvabilité - Dupont Industries:
Ratio d''endettement calculé : 28.5%. La capacité de remboursement mensuelle est estimée à 4 200€ pour une échéance prévisionnelle de 3 500€.
RECOMMANDATION : Solvabilité satisfaisante, financement envisageable.',
3240
FROM evaluations WHERE dossier_siren = '123456789' LIMIT 1;

INSERT INTO stage_results (evaluation_id, stage_name, output, duration_ms)
SELECT id, 'historique',
'Analyse Historique - Dupont Industries:
Aucun incident de paiement enregistré sur les 5 dernières années. Encours actuels : 2 crédits en cours, tous à jour.
Score de risque historique : FAIBLE.',
2890
FROM evaluations WHERE dossier_siren = '123456789' LIMIT 1;

INSERT INTO stage_results (evaluation_id, stage_name, output, duration_ms)
SELECT id, 'garanties',
'Évaluation des Garanties - Dupont Industries:
Collatéral proposé : Hypothèque sur entrepôt industriel estimé à 300 000€. Ratio de couverture : 120%.
AVIS : Couverture du risque SATISFAISANTE.',
2150
FROM evaluations WHERE dossier_siren = '123456789' LIMIT 1;

INSERT INTO stage_results (evaluation_id, stage_name, output, duration_ms)
SELECT id, 'conformite',
'Contrôle Conformité - Dupont Industries:
Vérifications KYC/AML effectuées. Aucune alerte détectée. Bénéficiaires effectifs identifiés et vérifiés.
STATUT: APPROUVE - Conformité réglementaire validée.',
1980
FROM evaluations WHERE dossier_siren = '123456789' LIMIT 1;
