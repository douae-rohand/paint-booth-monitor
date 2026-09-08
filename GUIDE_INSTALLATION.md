# Guide d'Installation et de Configuration

## Système de Supervision des Cabines de Peinture (S7-1200)

---

## 1. Introduction

Ce guide décrit l'installation complète du système de supervision des cabines de peinture sur une nouvelle machine. Le système est composé de quatre services interdépendants :

| Service | Technologie | Rôle |
|---|---|---|
| **Backend Java** | Spring Boot 4.1.0 / Java 21 | API REST, WebSocket, sécurité, notifications, rapports |
| **Service Python** | FastAPI / Python 3.13 | Acquisition PLC, détection d'anomalies, module IA, RAG |
| **Frontend** | React 19 / Vite 8 | Interface utilisateur web |
| **Base de données** | PostgreSQL 17 + pgvector | Stockage des données |
| **MinIO** | RELEASE.2025-09-07 | Stockage objet des rapports PDF |

---

## 2. Prérequis

### Mode Docker (recommandé)

| Outil | Version minimale | Vérification |
|---|---|---|
| Docker | 24.x | `docker --version` |
| Docker Compose | 2.x | `docker compose version` |
| Git | 2.x | `git --version` |

### Mode développement local (optionnel)

| Outil | Version utilisée dans le projet | Vérification |
|---|---|---|
| Java (JDK) | 21 (Eclipse Temurin recommandé) | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Python | 3.13 | `python --version` |
| Node.js | 20+ (24 dans le Dockerfile) | `node --version` |
| npm | 10+ | `npm --version` |
| PostgreSQL | 17 | `psql --version` |

---

## 3. Récupération du projet

```bash
git clone <URL_DU_DEPOT>
cd paint-booth-monitor
```

l'URL_DU_DEPOT est: https://github.com/douae-rohand/paint-booth-monitor.git

### Structure principale du projet

```
paint-booth-monitor/
|-- docker-compose.yml        # Orchestration de tous les services
|-- .env.example              # Variables d'environnement Docker (à copier en .env)
|-- java-service/             # Backend Spring Boot
|   |-- .env.example          # Variables d'environnement Java (dev local)
|   |-- src/main/resources/application.yml
|   `-- pom.xml
|-- python-service/           # Service FastAPI
|   |-- requirements.txt
|   `-- app/
|-- frontend/                 # Interface React
|   |-- .env.example          # Variables frontend (à copier en .env)
|   `-- package.json
|-- db/
|   `-- grants.sql            # Grants PostgreSQL (production)
`-- docker/
    `-- postgres/
        `-- init.sh           # Script d'initialisation PostgreSQL
```

---

## 4. Configuration des variables d'environnement

### 4.1 Fichier `.env` racine (Docker)

Copier le fichier exemple :

```bash
cp .env.example .env
```

Éditer `.env` et renseigner toutes les valeurs marquées `change_me` :

| Variable | Service | Description | Exemple |
|---|---|---|---|
| `POSTGRES_DB` | PostgreSQL | Nom de la base de données | `supervision_db` |
| `POSTGRES_USER` | PostgreSQL | Utilisateur superadmin PostgreSQL | `postgres` |
| `POSTGRES_PASSWORD` | PostgreSQL | Mot de passe superadmin | `<YOUR_PASSWORD>` |
| `POSTGRES_HOST` | PostgreSQL | Hôte PostgreSQL (Docker) | `postgres` |
| `POSTGRES_PORT` | PostgreSQL | Port PostgreSQL | `5432` |
| `JAVA_SERVICE_DB_USER` | Java | Utilisateur applicatif Java | `java_service` |
| `JAVA_SERVICE_DB_PASSWORD` | Java | Mot de passe utilisateur Java | `<YOUR_PASSWORD>` |
| `PYTHON_SERVICE_DB_USER` | Python | Utilisateur applicatif Python | `python_service` |
| `PYTHON_SERVICE_DB_PASSWORD` | Python | Mot de passe utilisateur Python | `<YOUR_PASSWORD>` |
| `JWT_SECRET` | Java | Clé secrète JWT (min. 32 caractères) | `<YOUR_LONG_SECRET>` |
| `JWT_EXPIRATION` | Java | Durée de vie du token en ms | `86400000` |
| `FRONTEND_URL` | Java | URL du frontend (liens emails) | `http://localhost` |
| `ALLOWED_ORIGINS` | Java | Origines CORS autorisées | `http://localhost,http://localhost:80` |
| `SECURE_COOKIE` | Java | Cookie HTTPS uniquement | `false` |
| `ADMIN_INITIAL_EMAIL` | Java | Email du compte Admin initial | `admin@renault.com` |
| `ADMIN_INITIAL_PASSWORD` | Java | Mot de passe Admin initial | `<YOUR_PASSWORD>` |
| `RESET_TOKEN_EXPIRATION_MINUTES` | Java | Expiration token réinitialisation | `15` |
| `ACTIVATION_TOKEN_EXPIRATION_HOURS` | Java | Expiration token activation | `24` |
| `PYTHON_SERVICE_URL` | Java | URL interne service Python | `http://host.docker.internal:8000` |
| `DATABASE_ECHO` | Python | Afficher les requêtes SQL | `false` |
| `DATABASE_POOL_SIZE` | Python | Taille du pool de connexions | `5` |
| `PLC_IP` | Python | Adresse IP de l'automate S7-1200 | `192.168.0.x` |
| `PLC_RACK` | Python | Numéro de rack de l'automate | `0` |
| `PLC_SLOT` | Python | Numéro de slot de l'automate | `1` |
| `SENDGRID_API_KEY` | Java | Clé API SendGrid (emails) | `<YOUR_SENDGRID_KEY>` |
| `SENDGRID_SENDER_EMAIL` | Java | Email expéditeur vérifié SendGrid | `<YOUR_EMAIL>` |
| `SENDGRID_SENDER_NAME` | Java | Nom affiché dans les emails | `Supervision Cabine de Peinture` |
| `EMAIL_WORKER_BATCH_SIZE` | Java | Lots de traitement email | `20` |
| `EMAIL_WORKER_MAX_TENTATIVES` | Java | Nombre maximum de tentatives | `5` |
| `EMAIL_WORKER_CRON` | Java | Cron de traitement email | `*/5 * * * * *` |
| `VITE_API_URL` | Frontend | URL de l'API Java (build Docker) | `http://localhost:8081` |
| `VITE_WS_URL` | Frontend | URL WebSocket (build Docker) | `http://localhost:8081/ws` |
| `MINIO_ROOT_USER` | MinIO | Utilisateur root MinIO | `minioadmin` |
| `MINIO_ROOT_PASSWORD` | MinIO | Mot de passe root MinIO | `<YOUR_PASSWORD>` |
| `MINIO_ENDPOINT` | Java | Endpoint MinIO (interne Docker) | `http://minio:9000` |
| `MINIO_ACCESS_KEY` | Java | Clé d'accès MinIO | `minioadmin` |
| `MINIO_SECRET_KEY` | Java | Clé secrète MinIO | `<YOUR_PASSWORD>` |
| `MINIO_BUCKET` | Java | Nom du bucket rapports | `rapports` |
| `VAPID_PUBLIC_KEY` | Java | Clé publique VAPID (Web Push) | _Voir section 4.3_ |
| `VAPID_PRIVATE_KEY` | Java | Clé privée VAPID (Web Push) | _Voir section 4.3_ |
| `VAPID_SUBJECT` | Java | Identifiant VAPID (mailto:) | `mailto:contact@domaine.com` |

### 4.2 Fichier `java-service/.env` (développement local uniquement)

```bash
cp java-service/.env.example java-service/.env
```

Renseigner les mêmes valeurs que le `.env` racine, en utilisant `localhost` comme hôte pour PostgreSQL et MinIO.

### 4.3 Génération des clés VAPID (Web Push)

Les clés VAPID sont nécessaires pour les notifications push navigateur. Elles doivent être générées **une seule fois** :

```bash
cd java-service
./mvnw compile "exec:java" "-Dexec.mainClass=com.projet.notifications.util.VapidKeyGenerator"
```

Copier les valeurs `VAPID_PUBLIC_KEY` et `VAPID_PRIVATE_KEY` affichées dans le `.env` (racine et `java-service/.env`).

> ⚠️ Les clés VAPID ne doivent jamais être partagées ni commitées dans Git.

### 4.5 Fichier `python-service/.env` (développement local uniquement)

```bash
cp python-service/.env.example python-service/.env
```

Renseigner les mêmes valeurs que le `.env` racine. Les champs différents par rapport au `.env` racine sont :

| Variable | Différence | Valeur locale |
|---|---|---|
| `DATABASE_URL` | Format asyncpg complet au lieu de variables séparées | `postgresql+asyncpg://python_service:<PASSWORD>@localhost:5432/supervision_db` |
| `PLC_IP` | `localhost` au lieu de l'IP réelle (mode simulateur) | `127.0.0.1` |

### 4.4 Fichier `frontend/.env` (développement local uniquement)

```bash
cp frontend/.env.example frontend/.env
```

| Variable | Description | Valeur par défaut |
|---|---|---|
| `VITE_API_URL` | URL de l'API Java | `http://localhost:8081` |
| `VITE_WS_URL` | URL WebSocket | `http://localhost:8081/ws` |

---

## 5. Configuration de PostgreSQL

### En mode Docker

PostgreSQL est démarré automatiquement via Docker Compose. Le script `docker/postgres/init.sh` crée automatiquement les deux utilisateurs applicatifs (`java_service`, `python_service`) avec leurs droits respectifs au premier démarrage.

Extensions requises (activées automatiquement par la migration V1) :
- `pgcrypto` — génération d'UUID
- `vector` — embeddings pgvector pour le module RAG

### Migrations Flyway

Les migrations sont appliquées automatiquement au démarrage du service Java. Elles sont numérotées V1 à V47 et se trouvent dans :

```
java-service/src/main/resources/db/migration/
```

Vérification manuelle des migrations :

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

### En mode développement local

Créer la base de données et les utilisateurs manuellement :

```sql
CREATE DATABASE supervision_db;

CREATE USER java_service WITH PASSWORD '<YOUR_PASSWORD>';
CREATE USER python_service WITH PASSWORD '<YOUR_PASSWORD>';

GRANT CONNECT ON DATABASE supervision_db TO java_service, python_service;
```

Activer les extensions :

```sql
\c supervision_db
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;
```

---

## 6. Configuration de MinIO

### En mode Docker

MinIO est démarré automatiquement. Le service `minio-init` crée le bucket `rapports` au premier démarrage.

| Paramètre | Valeur |
|---|---|
| API S3 | `http://localhost:9000` |
| Console Web | `http://localhost:9001` |
| Utilisateur | valeur de `MINIO_ROOT_USER` |
| Mot de passe | valeur de `MINIO_ROOT_PASSWORD` |
| Bucket | `rapports` |

### En mode développement local

Télécharger et lancer MinIO manuellement :

```bash
# Windows PowerShell
Invoke-WebRequest -Uri "https://dl.min.io/server/minio/release/windows-amd64/minio.exe" -OutFile "minio.exe"
$env:MINIO_ROOT_USER="minioadmin"; $env:MINIO_ROOT_PASSWORD="<YOUR_PASSWORD>"
.\minio.exe server ./data --console-address ":9001"
```

Créer le bucket via la console `http://localhost:9001` ou via la CLI `mc`.

---

## 7. Configuration du backend Java

### Installation des dépendances

```bash
cd java-service
./mvnw dependency:resolve
```

### Compilation

```bash
./mvnw clean package -DskipTests
```

### Lancement

```bash
./mvnw spring-boot:run
```

Le service démarre sur le port **8081**.

### Vérification

```bash
curl http://localhost:8081/actuator/health
```

Réponse attendue : `{"status":"UP"}`

### Dépendances externes requises au démarrage

- PostgreSQL accessible sur le port configuré dans `SPRING_DATASOURCE_URL`
- MinIO accessible sur `MINIO_ENDPOINT`
- Les variables d'environnement `JWT_SECRET`, `SENDGRID_API_KEY`, `SENDGRID_SENDER_EMAIL` doivent être définies

---

## 8. Configuration du service Python / FastAPI

### Création de l'environnement virtuel

```bash
cd python-service
python -m venv .venv

# Windows
.venv\Scripts\activate

# Linux/macOS
source .venv/bin/activate
```

### Installation des dépendances

```bash
pip install -r requirements.txt
```

### Lancement

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Le service démarre sur le port **8000**.

### Vérification

```bash
curl http://localhost:8000/health
```

### Mode simulation PLC (sans automate réel)

Un simulateur PLC est fourni dans `python-service/scripts/simulateur_plc.py`. Il démarre un serveur S7 sur `127.0.0.1:1102` :

```bash
python scripts/simulateur_plc.py
```

Configurer ensuite `PLC_IP=127.0.0.1` dans le `.env`.

---

## 9. Configuration du frontend React

### Installation des dépendances

```bash
cd frontend
npm install
```

### Configuration

Créer le fichier `frontend/.env` :

```bash
cp .env.example .env
```

| Variable | Description | Valeur locale |
|---|---|---|
| `VITE_API_URL` | URL de l'API Java | `http://localhost:8081` |
| `VITE_WS_URL` | URL WebSocket | `http://localhost:8081/ws` |

### Lancement en développement

```bash
npm run dev
```

Le frontend démarre sur le port **5173**.

### Build de production

```bash
npm run build
```

---

## 10. Configuration du PLC

Le service Python se connecte à l'automate Siemens S7-1200 via le protocole **Snap7** (bibliothèque `python-snap7`).

### Paramètres de connexion

| Paramètre | Variable | Description |
|---|---|---|
| Adresse IP | `PLC_IP` | IP statique de l'automate sur le LAN |
| Rack | `PLC_RACK` | Numéro de rack (généralement `0`) |
| Slot | `PLC_SLOT` | Numéro de slot CPU (généralement `1` pour S7-1200) |
| Port TCP | Configuré via l'Admin | Port de communication (défaut : `102`) |
| Intervalle de polling | Configuré via l'Admin | Intervalle en ms (min. 100 ms) |

### Configuration en base

La configuration PLC active est gérée dynamiquement via l'interface d'administration (`/plc`). L'Admin peut créer, activer et désactiver des configurations sans redémarrer le service.

### Sans automate réel

Utiliser le simulateur fourni :

```bash
cd python-service
python scripts/simulateur_plc.py
```

Le simulateur écoute sur `127.0.0.1:1102` et génère des valeurs de température et d'humidité dans les plages configurées.

---

## 11. Lancement avec Docker Compose

### Étape 1 — Préparer la configuration

```bash
cp .env.example .env
# Éditer .env et renseigner toutes les valeurs
```

### Étape 2 — Générer les clés VAPID (première fois uniquement)

```bash
cd java-service
./mvnw compile "exec:java" "-Dexec.mainClass=com.projet.notifications.util.VapidKeyGenerator"
# Copier les clés générées dans .env et java-service/.env
cd ..
```

### Étape 3 — Construction et démarrage

```bash
# Construire et démarrer tous les services
docker compose up --build -d

# Ou démarrer sans reconstruire (si déjà buildé)
docker compose up -d
```

### Étape 4 — Vérification des conteneurs

```bash
docker compose ps
```

Résultat attendu (tous les services `healthy` ou `running`) :

```
NAME                 STATUS
paint-booth-db       Up (healthy)
minio-storage        Up (healthy)
minio-bucket-init    Exited (0)
java-gateway         Up (healthy)
python-ai-rag        Up
react-frontend       Up (healthy)
```

### Étape 5 — Accès aux services

| Service | URL | Description |
|---|---|---|
| Frontend | `http://localhost` | Interface utilisateur |
| Backend Java | `http://localhost:8081` | API REST |
| Backend Java Health | `http://localhost:8081/actuator/health` | Healthcheck |
| Service Python | `http://localhost:8000` | API FastAPI |
| MinIO Console | `http://localhost:9001` | Administration MinIO |
| MinIO API S3 | `http://localhost:9000` | API S3 |
| PostgreSQL | `localhost:5432` | Base de données |

### Tableau des services Docker

| Service | Image | Port | Rôle |
|---|---|---|---|
| `postgres` | pgvector/pgvector:pg17 | 5432 | Base de données PostgreSQL + pgvector |
| `java-service` | Build local | 8081 | Backend Spring Boot |
| `python-service` | Build local | 8000 (host) | Service FastAPI (mode réseau hôte) |
| `frontend` | Build local | 80 | Frontend React servi par Nginx |
| `minio` | minio/minio:RELEASE.2025-09-07T16-13-09Z | 9000, 9001 | Stockage objet MinIO |
| `minio-init` | minio/mc:RELEASE.2025-08-13T08-35-41Z | — | Init bucket (s'arrête après exécution) |

---

## 12. Lancement sans Docker

Le projet supporte également un lancement en mode développement local, service par service.

### Ordre de démarrage recommandé

```
1. PostgreSQL
2. MinIO
3. Backend Java (Spring Boot)
4. Service Python (FastAPI)
5. Frontend (Vite)
```

### 1. PostgreSQL

Démarrer PostgreSQL localement et créer la base (voir section 5).

### 2. MinIO

```bash
.\minio.exe server ./data --console-address ":9001"
```

### 3. Backend Java

```bash
cd java-service
# Vérifier que java-service/.env est configuré
./mvnw spring-boot:run
# Port : 8081
```

### 4. Service Python

```bash
cd python-service
source .venv/bin/activate  # ou .venv\Scripts\activate sur Windows
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
# Port : 8000
```

### 5. Frontend

```bash
cd frontend
# Vérifier que frontend/.env est configuré
npm run dev
# Port : 5173
```

---

## 13. Vérification de l'installation

### Checklist complète

```bash
# 1. Conteneurs Docker actifs
docker compose ps

# 2. PostgreSQL accessible
docker exec paint-booth-db pg_isready -U postgres -d supervision_db

# 3. Migrations Flyway appliquées (47 migrations)
docker exec paint-booth-db psql -U postgres -d supervision_db \
  -c "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true;"
# Résultat attendu : 47

# 4. Extensions PostgreSQL actives
docker exec paint-booth-db psql -U postgres -d supervision_db \
  -c "SELECT extname FROM pg_extension WHERE extname IN ('pgcrypto','vector');"

# 5. Backend Java opérationnel
curl http://localhost:8081/actuator/health
# Attendu : {"status":"UP"}

# 6. Service Python opérationnel
curl http://localhost:8000/health

# 7. Frontend accessible
curl -I http://localhost
# Attendu : HTTP/1.1 200 OK

# 8. MinIO opérationnel
curl http://localhost:9000/minio/health/live
# Attendu : 200 OK

# 9. Bucket 'rapports' créé
docker exec minio-storage mc ls local/
# Attendu : rapports/

# 10. Compte Admin initial créé
docker exec paint-booth-db psql -U postgres -d supervision_db \
  -c "SELECT email, actif FROM superviseur s JOIN admin a ON s.id_superviseur = a.id_admin;"
```

### Vérification WebSocket

Se connecter au frontend (`http://localhost`), se connecter avec le compte Admin, et vérifier dans la console du navigateur (F12) que le WebSocket STOMP est bien connecté :

```
WebSocket STOMP connecté
Dashboard WebSocket connecté
```

---

## 14. Dépannage

### Port déjà utilisé

**Problème** : `bind: address already in use` sur le port 8081, 5432, 9000, etc.

**Cause** : Un processus utilise déjà ce port.

**Solution** :
```bash
# Identifier le processus (Windows)
netstat -ano | findstr :8081
# Tuer le processus
taskkill /PID <PID> /F

# Linux/macOS
lsof -i :8081
kill -9 <PID>
```

---

### Flyway — erreur de migration

**Problème** : `Found non-empty schema(s) [...] without schema history table` ou migration échouée.

**Cause** : La base contient déjà des tables sans historique Flyway, ou une migration précédente a échoué partiellement.

**Solution** :
```bash
# Vérifier l'état des migrations
docker exec paint-booth-db psql -U postgres -d supervision_db \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

# En cas de migration failed, supprimer la ligne et relancer
docker exec paint-booth-db psql -U postgres -d supervision_db \
  -c "DELETE FROM flyway_schema_history WHERE success = false;"
docker compose restart java-service
```

---

### Backend Java ne démarre pas

**Problème** : `Could not resolve placeholder 'JWT_SECRET'` ou variables manquantes.

**Cause** : Le fichier `.env` racine ou `java-service/.env` n'est pas configuré.

**Solution** : Vérifier que toutes les variables obligatoires sont définies dans `.env`. En mode Docker, seul le `.env` racine est utilisé.

---

### Erreur CSRF — `Requête bloquée par protection CSRF`

**Problème** : Les requêtes retournent `403 — Origin invalide ou manquant`.

**Cause** : Le header `Origin` est absent ou non autorisé.

**Solution** :
- En développement local : s'assurer que `ALLOWED_ORIGINS` contient bien `http://localhost:5173`.
- Dans Postman : ajouter le header `Origin: http://localhost:5173` au niveau de la collection.

---

### MinIO inaccessible — `Failed to connect to localhost:9000`

**Problème** : Le service Java ne peut pas uploader les rapports PDF.

**Cause** : MinIO n'est pas démarré.

**Solution** :
```bash
docker compose up -d minio minio-init
```

---

### Service Python — connexion PLC échouée

**Problème** : `S7Connection error` ou timeout lors de la connexion à l'automate.

**Cause** : L'adresse IP `PLC_IP` est incorrecte, l'automate est éteint, ou le réseau n'est pas configuré.

**Solution** :
- Vérifier que l'automate est accessible en réseau : `ping <PLC_IP>`
- Utiliser le simulateur PLC pour les tests sans automate réel :
  ```bash
  cd python-service
  python scripts/simulateur_plc.py
  ```
- Configurer `PLC_IP=127.0.0.1` et le port `1102` dans l'Admin.

---

### Frontend — erreur CORS au login

**Problème** : `Access to fetch blocked by CORS policy`.

**Cause** : `ALLOWED_ORIGINS` ne contient pas l'URL du frontend.

**Solution** :
- Vérifier la valeur de `ALLOWED_ORIGINS` dans `.env`.
- En local : `ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000`
- En Docker : `ALLOWED_ORIGINS=http://localhost,http://localhost:80`

---

### WebSocket — reconnexions fréquentes

**Problème** : Le WebSocket se déconnecte et reconnecte en boucle.

**Cause** : Le token JWT a expiré et le refresh échoue.

**Solution** : Se reconnecter. Vérifier que `JWT_EXPIRATION` est une valeur raisonnable (ex: `86400000` = 24h).

---

### `pg_isready` échoue

**Problème** : PostgreSQL ne répond pas.

**Cause** : Le conteneur PostgreSQL est encore en cours de démarrage.

**Solution** :
```bash
# Attendre que le healthcheck soit vert
docker compose ps postgres
# Si le statut est "starting", attendre quelques secondes
docker compose logs postgres
```

---

### Clés VAPID invalides — `InvalidAccessError`

**Problème** : Le navigateur refuse de s'abonner aux notifications push.

**Cause** : Les clés VAPID ont été générées dans l'ancien format DER (commençant par `MFkw`).

**Solution** : Regénérer les clés avec la commande corrigée :
```bash
cd java-service
./mvnw compile "exec:java" "-Dexec.mainClass=com.projet.notifications.util.VapidKeyGenerator"
```
Les nouvelles clés publiques commencent par `BL` (format raw 65 bytes). Mettre à jour `.env` et `java-service/.env`, puis redémarrer le service Java.

---

## 15. Résumé du démarrage rapide

Après une première installation et configuration du fichier `.env` :

```bash
# Démarrer tous les services
docker compose up -d

# Vérifier l'état
docker compose ps

# Accéder à l'application
# http://localhost  (frontend)
# http://localhost:8081/actuator/health  (backend Java)
# http://localhost:9001  (console MinIO)

# Arrêter tous les services
docker compose down

# Arrêter et supprimer les volumes (⚠️ supprime les données)
docker compose down -v
```

### Redémarrage après modification du code

```bash
# Reconstruire un seul service
docker compose up -d --build java-service
docker compose up -d --build python-service
docker compose up -d --build frontend

# Reconstruire tout
docker compose up -d --build
```

### Commandes utiles

```bash
# Voir les logs en temps réel
docker compose logs -f java-service
docker compose logs -f python-service

# Accéder au shell PostgreSQL
docker exec -it paint-booth-db psql -U postgres -d supervision_db

# Vérifier les migrations Flyway
docker exec paint-booth-db psql -U postgres -d supervision_db \
  -c "SELECT version, description FROM flyway_schema_history ORDER BY installed_rank;"
```
