# Implementation Plan - Intelligent Credit Granting Platform

This plan outlines the design and step-by-step implementation for migrating the LangGraph/LangChain credit evaluation Python prototype into a Java 21 Spring Boot backend + JavaFX desktop client.

## User Review Required

> [!IMPORTANT]
> **Database Initialization:**
> We will configure the application to automatically bootstrap a schema via `spring.jpa.hibernate.ddl-auto=update` and load initial reference data (like default admin and banker accounts) using a `data.sql` file or a Spring `@PostConstruct` bootstrapper. This avoids manual database setup other than creating the target database `credit_platform`.
> 
> **Default Test Credentials:**
> We propose creating the following default accounts for evaluation and testing:
> *   Admin: `admin` / `adminpass` (has `ROLE_ADMIN`)
> *   Banker: `banker` / `bankerpass` (has `ROLE_BANQUIER`)

> [!WARNING]
> **Ollama Connection & Models:**
> The backend expects a locally running Ollama instance at `http://localhost:11434`. It requires two models to be already pulled locally:
> *   `deepseek-r1:8b` (used for the four analyst stages)
> *   `deepseek-r1:14b` (used for the supervisor/decision stage)
> 
> *Ensure both models are downloaded (`ollama pull deepseek-r1:8b` and `ollama pull deepseek-r1:14b`) before starting the backend evaluation pipeline.*

---

## Open Questions

> [!NOTE]
> **JavaFX Executable Packaging:**
> To keep the client and backend as separate deployable units within a single Maven module, we will implement two separate main classes:
> 1. `com.talan.creditplatform.CreditPlatformApplication` (starts the Spring Boot backend server).
> 2. `com.talan.creditplatform.desktop.DesktopApplication` (launches the JavaFX desktop GUI application).
> 
> Do you prefer this setup, or would you like a multi-module Maven structure with `backend/` and `frontend-desktop/` directories? *We recommend the single module setup as it simplifies Eclipse imports, builds, and running out-of-the-box.*

---

## Proposed Changes

### Configuration and Infrastructure

#### [MODIFY] [application.properties](file:///c:/Users/youss/Desktop/projet_stage/src/main/resources/application.properties)
*   Define Spring Security endpoints configuration, secret key for signing JWTs, and the Ollama endpoint URL (`ollama.api.url=http://localhost:11434`).
*   Keep database setup pointing to `jdbc:postgresql://localhost:5432/credit_platform`.

#### [NEW] [DECISIONS.md](file:///c:/Users/youss/Desktop/projet_stage/DECISIONS.md)
*   Document structural, security, database, and UI choices.

---

### Database Entities & Repositories

#### [NEW] Entities
1.  **[User.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/domain/User.java):** ID, username, BCrypt-hashed password, and role (`ROLE_ADMIN` or `ROLE_BANQUIER`).
2.  **[Dossier.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/domain/Dossier.java):** SIREN/ID (primary key), client name, type of client (`Personne Physique` or `Personne Morale`), loan amount requested, and a raw data text blob.
3.  **[Evaluation.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/domain/Evaluation.java):** ID, reference to Dossier, mode (`FAST`/`FULL`), final supervisor report (Markdown), and timestamp.
4.  **[StageResult.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/domain/StageResult.java):** ID, reference to Evaluation, stage name (`solvabilite`, `historique`, etc.), output response, and execution duration.

#### [NEW] Repositories
*   `UserRepository`, `DossierRepository`, `EvaluationRepository`, `StageResultRepository` extending `JpaRepository`.

---

### Security & JWT Configuration

#### [NEW] Security Components
1.  **[JwtService.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/security/JwtService.java):** Utility to generate, parse, and validate HS256-signed JWTs.
2.  **[JwtAuthFilter.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/security/JwtAuthFilter.java):** Extends `OncePerRequestFilter` to validate `Authorization: Bearer <token>` headers on incoming requests and establish authentication contexts.
3.  **[SecurityConfig.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/config/SecurityConfig.java):** Configures security filters, stateless session management, cors, permit-all routes (like `/api/auth/login`), and role-based path access.

---

### Ollama Client & Pipeline Orchestration

#### [NEW] Ollama Client Service
*   **[OllamaClient.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/service/OllamaClient.java):** Direct wrapper using JDK HTTP Client or Spring RestClient to send JSON POST requests to `http://localhost:11434/api/generate` with stream disabled. It will parse responses using Jackson.

#### [NEW] Agent Implementations
*   **[AnalystAgent.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/service/AnalystAgent.java):** Prepares prompts and runs LLM completions for individual analyst domains:
    *   *Solvabilité*
    *   *Historique*
    *   *Garanties*
    *   *Conformité* (with strict `STATUT: APPROUVE` or `STATUT: REFUS` formatting constraints).
*   **[SupervisorAgent.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/service/SupervisorAgent.java):** Consolidates responses, calls `deepseek-r1:14b` with higher token limit and context size, and outputs the final evaluation decision.

#### [NEW] Pipeline Orchestrator Service
*   **[PipelineOrchestrator.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/service/PipelineOrchestrator.java):** Coordinates the execution, measures stage durations, handles error cases gracefully, and commits results to PostgreSQL.

---

### REST API Controllers

#### [NEW] Controllers
1.  **[AuthController.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/controller/AuthController.java):** Provides `/api/auth/login` endpoint for users to obtain JWTs.
2.  **[DossierController.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/controller/DossierController.java):** Standard REST operations to fetch and create dossiers.
3.  **[CreditRequestController.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/controller/CreditRequestController.java):** Handles evaluation execution (`POST /api/credit-requests/{siren}/evaluate`) and history retrieval.
4.  **[AdminUserController.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/controller/AdminUserController.java):** Endpoint for admin to register new users or manage roles.

---

### JavaFX Desktop Client

#### [NEW] Layout FXML & Controllers
1.  **[DesktopApplication.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/desktop/DesktopApplication.java):** GUI runner.
2.  **[LoginController.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/desktop/controller/LoginController.java) + [login.fxml](file:///c:/Users/youss/Desktop/projet_stage/src/main/resources/fxml/login.fxml):** Login form.
3.  **[MainViewController.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/desktop/controller/MainViewController.java) + [main_view.fxml](file:///c:/Users/youss/Desktop/projet_stage/src/main/resources/fxml/main_view.fxml):** Layout container with side navigation panel (Dossier Lookup, Evaluation Trigger, History, Admin User management).
4.  **[DossierController.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/desktop/controller/DossierController.java) + [dossier.fxml](file:///c:/Users/youss/Desktop/projet_stage/src/main/resources/fxml/dossier.fxml):** Dossier view and registry creation.
5.  **[EvaluationController.java](file:///c:/Users/youss/Desktop/projet_stage/src/main/java/com/talan/creditplatform/desktop/controller/EvaluationController.java) + [evaluation.fxml](file:///c:/Users/youss/Desktop/projet_stage/src/main/resources/fxml/evaluation.fxml):** Running state and pipeline execution views.

---

## Verification Plan

### Automated Tests
-   **Unit Tests:** Create `OllamaClientTest`, `PipelineOrchestratorTest` (using Mockito to stub Ollama API responses), and security layer filter checks using JUnit 5.
-   Run tests via:
    ```bash
    mvn clean test
    ```

### Manual Verification
1.  **Database Migration Check:** Verify schemas create automatically on start.
2.  **Ollama REST Endpoint Connectivity:** Execute a simple curl check or verify using log statements that `OllamaClient` successfully connects.
3.  **API Verification:** Perform HTTP token generation and dossier submission validation using manual CLI requests (such as Invoke-RestMethod).
4.  **End-to-End GUI Walkthrough:** Launch `DesktopApplication`, sign in, enter/create a dossier, trigger a FAST evaluation, monitor logs for stage progression, and view the final decision report and agent outputs.
