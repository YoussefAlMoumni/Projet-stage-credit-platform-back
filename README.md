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

## API Documentation & Manual Testing (`curl`)

First, obtain a JWT token using the default admin or banker accounts:
```bash
# 1. Login to get JWT Token
curl -X POST http://localhost:8081/api/auth/login \
     -H "Content-Type: application/json" \
     -d "{\"username\":\"admin\",\"password\":\"adminpass\"}"

# (For Windows PowerShell, replace single quotes/escaping as needed, or use Postman)
# Export the token for subsequent requests
export TOKEN="<your_jwt_token_here>"
```

```bash
# 2. Create a Dossier
curl -X POST http://localhost:8081/api/dossiers \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN" \
     -d "{\"siren\":\"123456789\", \"name\":\"Tech Corp\", \"typeClient\":\"Personne Morale\", \"montantDemande\":\"500000.0\", \"rawData\":\"{}\"}"

# 3. Trigger Evaluation Pipeline (FAST mode)
curl -X POST "http://localhost:8081/api/credit-requests/123456789/evaluate?mode=FAST" \
     -H "Authorization: Bearer $TOKEN"

# 4. View Evaluation History for a Dossier
curl -X GET "http://localhost:8081/api/credit-requests/123456789/history" \
     -H "Authorization: Bearer $TOKEN"

# 5. List all users (Admin only)
curl -X GET "http://localhost:8081/api/admin/users" \
     -H "Authorization: Bearer $TOKEN"
```
