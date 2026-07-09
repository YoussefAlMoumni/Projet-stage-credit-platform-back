# DECISIONS.md

This document logs key design, architectural, and implementation decisions made for the Intelligent Credit Granting Platform.

## 1. Project Module Structure
- **Decision:** A single-module Maven project is used instead of a multi-module Maven structure.
- **Rationale:** The prompt requests compatibility with Eclipse (import via "Maven -> Import Existing Maven Project"). A single-module project containing both the backend and client is significantly easier to import, compile, and run in Eclipse without workspace configuration issues. 
- **Separation:** Backend services (`com.talan.creditplatform.*`) and Desktop classes (`com.talan.creditplatform.desktop.*`) are kept strictly separated by packaging. The desktop app does not load Spring context and interacts with the backend exclusively via REST.

## 2. Database Schema and Initialization
- **Decision:** We rely on Hibernate's `ddl-auto=update` to generate the tables, but we implement an automatic database initializer bean (`DatabaseInitializer.java`) in Spring to populate the database on startup.
- **Rationale:** This ensures that default users (`admin`/`adminpass` and `banker`/`bankerpass`) are seeded dynamically and BCrypt-hashed correctly when the database is empty, preventing authentication failure on first start.

## 3. Dossier Blob Representation
- **Decision:** The `Dossier` entity uses a `VARCHAR` / `TEXT` field named `rawData` to store financial/identity JSON.
- **Rationale:** This provides flexibility to store structured financial information (like income, active loans, debt ratios, compliance checks) without needing complex entity mapping, while remaining easy to deserialize and feed directly into LLM prompts.

## 4. REST Client for Ollama
- **Decision:** We use Spring Boot's built-in `RestClient` to perform raw HTTP POST requests to `http://localhost:11434/api/generate`.
- **Rationale:** Avoids unnecessary external dependencies (like LangChain4j) and allows absolute control over raw HTTP request payloads (e.g. settings parameters `num_ctx`, `num_predict`, and `keep_alive` directly).

## 5. Security & Stateless Session Management
- **Decision:** Spring Security is configured to use stateless session management (`SessionCreationPolicy.STATELESS`) and uses custom JWT filtering.
- **Rationale:** This is standard for API/Client architecture, ensuring the JavaFX client can perform stateless authentication and attach the bearer token for subsequent calls.
