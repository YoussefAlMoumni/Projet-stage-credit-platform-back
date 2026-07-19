package com.talan.creditplatform.config;

import com.talan.creditplatform.model.entity.AiModel;
import com.talan.creditplatform.model.entity.AiPrompt;
import com.talan.creditplatform.model.entity.User;
import com.talan.creditplatform.repository.AiModelRepository;
import com.talan.creditplatform.repository.AiPromptRepository;
import com.talan.creditplatform.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DatabaseInitializer {

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                          AiModelRepository aiModelRepository, AiPromptRepository aiPromptRepository) {
        return args -> {
            // Upsert each default user so they always exist with the correct password,
            // even if the sample_data.sql was run and wiped the users table.
            upsertUser(userRepository, passwordEncoder, "admin",   "AdminPass2024!",  "admin");
            upsertUser(userRepository, passwordEncoder, "manager", "ManagerPass2025!", "manager");
            upsertUser(userRepository, passwordEncoder, "banker",  "BankerPass2025!",  "manager");
            upsertUser(userRepository, passwordEncoder, "analyst", "AnalystPass2025!", "analyst");

            // Seed default AI models and prompts if tables are empty
            if (aiModelRepository.count() == 0) {
                seedAiModelsAndPrompts(aiModelRepository, aiPromptRepository);
            }
        };
    }

    private void upsertUser(UserRepository repo, PasswordEncoder encoder,
                            String username, String rawPassword, String role) {
        repo.findByUsername(username).ifPresentOrElse(
            existing -> {
                // Re-encode and update password in case it was corrupted by manual SQL
                existing.setPassword(encoder.encode(rawPassword));
                existing.setRole(role);
                repo.save(existing);
            },
            () -> repo.save(new User(username, encoder.encode(rawPassword), role))
        );
    }

    private void seedAiModelsAndPrompts(AiModelRepository modelRepo, AiPromptRepository promptRepo) {
        LocalDateTime now = LocalDateTime.now();

        // Solvency stage
        AiModel solvency = modelRepo.save(new AiModel(
                "solvency", "deepseek-r1:8b", 2048, 0.3, "0s", true));
        promptRepo.save(createPrompt(solvency, "Analyste Solvabilité:\n" +
                "Évaluer la capacité brute de remboursement pour le dossier {siren} " +
                "concernant un client de type '{clientType}'. Extraire et analyser le ratio d'endettement.\n" +
                "Générer une recommandation structurée.", "v1.0.0", now));

        // History stage
        AiModel history = modelRepo.save(new AiModel(
                "history", "deepseek-r1:8b", 2048, 0.3, "0s", true));
        promptRepo.save(createPrompt(history, "Analyste Historique:\n" +
                "Vérifier les incidents de paiement historiques et l'état des engagements en cours " +
                "pour le dossier {siren}.\n" +
                "Fournir un score de risque sur les antécédents.", "v1.0.0", now));

        // Guarantees stage
        AiModel guarantees = modelRepo.save(new AiModel(
                "guarantees", "deepseek-r1:8b", 2048, 0.3, "0s", true));
        promptRepo.save(createPrompt(guarantees, "Évaluateur de Garanties:\n" +
                "Évaluer la liquidité, la valeur estimée et le ratio de couverture du collatéral proposé " +
                "par rapport au montant demandé de {montantDemande}.\n" +
                "Rédiger un avis sur la couverture du risque.", "v1.0.0", now));

        // Compliance stage
        AiModel compliance = modelRepo.save(new AiModel(
                "compliance", "deepseek-r1:8b", 2048, 0.3, "0s", true));
        promptRepo.save(createPrompt(compliance, "Officier de Conformité:\n" +
                "Exécuter les vérifications réglementaires d'usage (KYC/AML) sur le dossier {siren}.\n" +
                "CONTRAINTE DE FORMAT STRICTE: Votre réponse DOIT commencer explicitement par l'un des drapeaux suivants:\n" +
                "soit 'STATUT: APPROUVE' soit 'STATUT: REFUS'. Justifier ensuite votre choix.", "v1.0.0", now));

        // Supervisor stage
        AiModel supervisor = modelRepo.save(new AiModel(
                "supervisor", "deepseek-r1:14b", 4096, 0.4, "0s", true));
        promptRepo.save(createPrompt(supervisor, "Directeur d'Engagement (Superviseur):\n" +
                "Consolider les rapports des 4 spécialistes ci-dessous pour formuler la décision d'octroi finale.\n\n" +
                "### Données de base:\n Dossier: {siren} | Montant: {montantDemande}\n\n" +
                "### Analyse Solvabilité:\n {solvabilite}\n\n" +
                "### Analyse Historique:\n {historique}\n\n" +
                "### Analyse Garanties:\n {garanties}\n\n" +
                "### Contrôle Conformité:\n {conformite}\n\n" +
                "Rédiger un rapport de décision complet, clair et formalisé au format Markdown.", "v1.0.0", now));
    }

    private AiPrompt createPrompt(AiModel model, String text, String version, LocalDateTime updatedAt) {
        AiPrompt prompt = new AiPrompt();
        prompt.setAiModel(model);
        prompt.setPromptText(text);
        prompt.setVersionTag(version);
        prompt.setUpdatedAt(updatedAt);
        return prompt;
    }
}
