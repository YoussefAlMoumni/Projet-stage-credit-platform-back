# Plateforme Intelligente d'Octroi de Crédit - Backend

Il s'agit du backend de la Plateforme Intelligente d'Octroi de Crédit. Il est construit avec **Spring Boot 3** et **Java 21**.

## Technologies Clés

- **Java 21**
- **Spring Boot 3.5.x** (Web, Data JPA, Security)
- **PostgreSQL** (Base de données)
- **JJWT** (pour l'Authentification)
- **Ollama** (pour l'inférence du modèle d'IA local)

## Prérequis

- Java 21+ installé et configuré dans votre `PATH`.
- Maven (`mvn`) installé.
- PostgreSQL en cours d'exécution localement avec une base de données nommée `credit_platform`.
- Ollama en cours d'exécution localement pour les fonctionnalités d'IA.

## Variables d'Environnement

Pour des raisons de sécurité, les valeurs de configuration sensibles ne sont pas codées en dur. Vous **devez** fournir les variables d'environnement suivantes avant de démarrer l'application :

- `DB_USERNAME` : Le nom d'utilisateur de la base de données PostgreSQL (ex. : `postgres`).
- `DB_PASSWORD` : Le mot de passe de la base de données PostgreSQL.
- `JWT_SECRET` : Une chaîne sécurisée, encodée en Base64 (au moins 32 octets) pour signer les tokens JWT.

## Lancer l'Application

1. Ouvrez un terminal et accédez à ce répertoire (`projet_stage_back`).
2. Définissez les variables d'environnement requises.
3. Démarrez l'application avec Maven :

### Windows (PowerShell)
```powershell
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="votre_mot_de_passe_securise"
$env:JWT_SECRET="votre_cle_secrete_tres_longue_encodee_en_base64"
mvn spring-boot:run
```

### Linux/macOS
```bash
export DB_USERNAME="postgres"
export DB_PASSWORD="votre_mot_de_passe_securise"
export JWT_SECRET="votre_cle_secrete_tres_longue_encodee_en_base64"
./mvnw spring-boot:run
```

Le serveur démarrera sur `http://localhost:8081`.

## Intégration de l'IA

Ce backend s'appuie sur **Ollama** pour exécuter de grands modèles linguistiques localement. Assurez-vous d'avoir téléchargé les modèles requis dans Ollama (ex. : `deepseek-r1:8b`). Si Ollama ne fonctionne pas, le backend tentera de le démarrer automatiquement ou de gérer l'erreur de manière élégante.
