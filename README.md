# Intelligent Credit Granting Platform

A local intelligent credit-granting platform built using Java 21, Spring Boot, JavaFX, PostgreSQL, and Ollama. It evaluates credit applications via a multi-agent AI pipeline using `deepseek-r1:8b` and `deepseek-r1:14b`.

## Prerequisites

- JDK 21
- Maven
- PostgreSQL running on localhost:5432
- Ollama running locally at `http://localhost:11434`

### Ensure Models are Downloaded
Run the following commands before starting the backend:
```bash
ollama pull deepseek-r1:8b
ollama pull deepseek-r1:14b
```

## Setup Instructions

1. **Database Setup**
   Create a database named `credit_platform` in PostgreSQL.
   ```sql
   CREATE DATABASE credit_platform;
   ```
   The backend relies on `spring.jpa.hibernate.ddl-auto=update` to automatically create tables.
   Default credentials will be populated automatically:
   - Admin: `admin` / `adminpass`
   - Banker: `banker` / `bankerpass`

2. **Backend Execution**
   ```bash
   mvn clean package
   mvn spring-boot:run
   ```
   Or run `CreditPlatformApplication.java` from Eclipse/IntelliJ.

3. **Desktop Client Execution**
   Run the `DesktopApplication.java` main class. This provides the login UI and interacts via HTTP with the backend.

## Structure

- `com.talan.creditplatform.domain`: JPA Entities
- `com.talan.creditplatform.repository`: Data access
- `com.talan.creditplatform.security`: JWT and Security Configuration
- `com.talan.creditplatform.service`: Ollama integration and AI orchestrators
- `com.talan.creditplatform.controller`: REST APIs
- `com.talan.creditplatform.desktop`: JavaFX Application and HTTP Clients

## API Documentation

- `POST /api/auth/login` - Returns JWT token
- `POST /api/dossiers` - Create a dossier
- `POST /api/credit-requests/{siren}/evaluate?mode=FAST|FULL` - Triggers AI pipeline
- `GET /api/credit-requests/{siren}/history` - History for a dossier
- `GET /api/admin/users` - List all users (ROLE_ADMIN only)
