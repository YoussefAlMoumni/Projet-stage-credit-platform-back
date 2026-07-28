# Intelligent Credit Granting Platform - Backend

This is the backend for the Intelligent Credit Granting Platform. It is built using **Spring Boot 3** and **Java 21**.

## Key Technologies

- **Java 21**
- **Spring Boot 3.5.x** (Web, Data JPA, Security)
- **PostgreSQL** (Database)
- **JJWT** (for Authentication)
- **Ollama** (for local AI model inference)

## Prerequisites

- Java 21+ installed and configured in your `PATH`.
- Maven (`mvn`) installed.
- PostgreSQL running locally with a database named `credit_platform`.
- Ollama running locally for AI features.

## Environment Variables

For security reasons, sensitive configuration values are not hardcoded. You **must** provide the following environment variables before starting the application:

- `DB_USERNAME`: The PostgreSQL database username (e.g., `postgres`).
- `DB_PASSWORD`: The PostgreSQL database password.
- `JWT_SECRET`: A secure, Base64-encoded string (at least 32 bytes) for signing JWT tokens.

## Running the Application

1. Open a terminal and navigate to this directory (`projet_stage_back`).
2. Set the required environment variables.
3. Start the application using Maven:

### Windows (PowerShell)
```powershell
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_secure_password"
$env:JWT_SECRET="your_very_long_base64_encoded_secret_key"
mvn spring-boot:run
```

### Linux/macOS
```bash
export DB_USERNAME="postgres"
export DB_PASSWORD="your_secure_password"
export JWT_SECRET="your_very_long_base64_encoded_secret_key"
./mvnw spring-boot:run
```

The server will start on `http://localhost:8081`.

## AI Integration

This backend relies on **Ollama** to run large language models locally. Ensure that you have the required models downloaded in Ollama (e.g., `deepseek-r1:8b`). If Ollama is not running, the backend will attempt to start it automatically or gracefully handle the error.
