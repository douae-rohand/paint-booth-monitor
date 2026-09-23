# Système de supervision et d’historisation en temps réel de la température et de l’humidité d’une cabine de peinture et d’une étuve industrielles

Application de supervision industrielle permettant la collecte, l'historisation, l'analyse et la restitution des mesures de **température** et d'**humidité** d'une cabine de peinture, connectée à un automate **Siemens S7-1200**.

**Projet de Fin d'Année (PFA)** - développé dans un contexte industriel automobile, en lien avec une cabine de peinture réelle.

---

## Table des matières

1. [Contexte](#contexte)
2. [Problématique](#problématique)
3. [Objectifs](#objectifs)
4. [Périmètre fonctionnel](#périmètre-fonctionnel)
5. [Architecture](#architecture)
6. [Choix techniques](#choix-techniques)
7. [Structure du dépôt](#structure-du-dépôt)
8. [Prérequis](#prérequis)
9. [Installation et démarrage](#installation-et-démarrage)
10. [Configuration](#configuration)
11. [Rôles et sécurité](#rôles-et-sécurité)
12. [Détection d'anomalies](#détection-danomalies)
13. [Indicateurs de performance (KPIs)](#indicateurs-de-performance-kpis)
14. [Livrables attendus](#livrables-attendus)
15. [Stratégie de développement](#stratégie-de-développement)
16. [Documentation complémentaire](#documentation-complémentaire)

---

## Contexte

La cabine de peinture est un équipement critique du processus de production automobile. La qualité du revêtement dépend directement du respect des conditions thermiques et hygrométriques pendant l'application de la peinture.

L'automate de terrain est un **SIMATIC S7-1200** (CPU 1215C, réf. 6ES7 215-1HG40-0XB0), programmé via **TIA Portal V17**. Les mesures de température et d'humidité y sont disponibles en temps réel, mais leur consultation se limite aujourd'hui à l'IHM locale, sans historisation centralisée ni outils d'analyse avancés.

Ce projet s'inscrit dans le cadre d'un **PFA** réalisé en binôme :

- **Informatique** : conception et développement de l'application complète (backend, frontend, base de données, modules IA et chatbot).
- **Automatisme / électronique** : configuration PLC, protocole de communication (Snap7 ou OPC UA), validation terrain des capteurs et des valeurs remontées.

---

## Problématique

Les données de température et d'humidité de la cabine de peinture et des zones de l'étuve sont visualisées localement sur l'IHM, sans :

- historisation exploitable sur le long terme ;
- analyse a posteriori des dérives ou des incidents ;
- alerting structuré en cas de dépassement de seuils ;
- restitution consolidée (rapports, exports, KPIs) pour les équipes qualité et production.

Le scénario retenu est une **connexion directe au PLC** (lecture périodique via le protocole Snap7), avec historisation entièrement gérée par l'application développée.

---

## Objectifs

| Objectif | Description |
|---|---|
| Connexion PLC | Établir une communication fiable entre le poste de supervision et l'automate S7-1200 |
| Historisation | Stocker les mesures horodatées dans PostgreSQL |
| Supervision | Proposer un dashboard temps réel et un historique graphique (température et humidité) |
| Alerting | Détecter les anomalies via seuils absolus (critiques) et seuils dynamiques par moyenne mobile |
| Chatbot | Permettre l'interrogation en langage naturel de l'historique — Chatbot à appel d'outils (tool calling) via Spring AI |
| Reporting | Générer automatiquement un rapport journalier (PDF) et exporter les données (CSV/Excel) |
| KPIs | Calculer des indicateurs adaptés au contexte qualité peinture |
| Sécurité | Authentifier les utilisateurs et distinguer les rôles Superviseur et Admin |

---

## Périmètre fonctionnel

### Inclus

- Lecture périodique des mesures PLC (température, humidité).
- Historisation, visualisation temps réel et consultation de l'historique.
- Recherche manuelle par identifiant de caisse ou par plage horaire.
- Système d'alertes à 2 niveaux (seuils absolus, seuils dynamiques) et notifications multicanal (email, push natif navigateur, in-app WebSocket).
- Chatbot à appel d'outils (tool calling), KPIs, exports et rapports PDF.
- Authentification JWT et gestion des rôles.

### Exclus

- Traçabilité formelle automatique des lots de production (absence de retour qualité véhicule exploitable).

En remplacement de la traçabilité des lots, chaque mesure peut porter un **identifiant de caisse** optionnel, permettant une recherche manuelle dans l'historique.

---

## Architecture

Le système repose sur une **architecture polyglotte** à trois couches applicatives, orchestrée par Docker Compose.

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (React)                        │
│     Dashboard, historique, alertes, chatbot (tool calling)  │
└───────────────────────────┬─────────────────────────────────┘
                            │ REST + WebSocket (JWT)
                            │ Point d'entrée unique
                            ▼
┌───────────────────────────────────────────────────────────────┐
│         Service Java - Business & Access (Spring Boot)        │
│  Auth JWT · KPIs · Rapports · Notifications · API Gateway     │
│  Chatbot (Spring AI, tool calling) · WebSocket · LISTEN/NOTIFY│
└───────────────────────────────────────────────────────────────┘
                                        │ JDBC (R/W)
                                        ▼
┌──────────────────────────────┐   ┌──────────────────────────┐
│  Service Python - Data &     │   │      PostgreSQL          │
│  Ingestion                   │◄──┤                          │
│  Snap7 · Ingestion · Alerting│──►│                          │
└──────────────┬───────────────┘   └──────────────────────────┘
               │ Snap7
               ▼
        ┌─────────────┐
        │  S7-1200    │
        │  (PLC)      │
        └─────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      MinIO (Stockage)                       │
│              Rapports PDF · Fichiers · Assets               │
└─────────────────────────────────────────────────────────────┘
```

### Flux principaux

**Flux utilisateur (synchrone)**

`Frontend → Java (auth, droits, chatbot tool calling) → Frontend`

Le frontend ne communique **jamais** directement avec le service Python.

**Flux de collecte (asynchrone, continu)**

`PLC → Python (lecture, seuils, écriture en base) → NOTIFY PostgreSQL → Java (LISTEN, notifications)`

### Répartition des responsabilités

| Responsabilité | Service | Justification |
|---|---|---|
| Lecture PLC (Snap7 / OPC UA) | Python | Bibliothèques `python-snap7` et `asyncua` matures pour l'industrie |
| Historisation et alerting temps réel | Python | Traitement au plus près de la source de données (seuils absolus et dynamiques) |
| Chatbot (tool calling, Spring AI) | Java | Données déjà exposées par les services Java existants ; Java déjà point d'entrée unique |
| Authentification et rôles | Java | Spring Security, gestion fine des droits |
| KPIs, exports, rapports PDF | Java | Logique orientée reporting et utilisateur final |
| Notifications multicanal | Java | Dispatch métier indépendant de la collecte (Email, Push VAPID, WebSocket In-App) |
| API Gateway et WebSocket | Java | Point d'entrée unique, sécurisation centralisée |
| Stockage fichiers (rapports PDF) | MinIO | Stockage objet S3-compatible pour les documents générés |

---

## Choix techniques

| Composant | Technologie | Version / remarque |
|---|---|---|
| Frontend | React, TanStack Start/Router, Tailwind CSS, shadcn/ui, Recharts | Interface neumorphique, dashboard industriel |
| Gateway & Chatbot | Java 21, Spring Boot, Spring Security, JWT, Spring AI | Auth, API REST, WebSocket, Flyway, chatbot tool calling |
| Data & Ingestion | Python 3.13+, FastAPI, SQLAlchemy (async) | Collecte PLC, ingestion, alerting (seuils absolus et dynamiques) |
| Base de données | PostgreSQL 17 | Données horodatées et tables applicatives |
| Stockage objet | MinIO | Stockage des rapports PDF et fichiers |
| PLC | Siemens S7-1200, protocole Snap7 (ou OPC UA) | Connexion Ethernet, IP statique |
| Conteneurisation | Docker, Docker Compose | Orchestration multi-services |
| CI | GitHub Actions | Pipelines séparés frontend / Java / Python |
| Migrations SQL | Flyway (Java) | Schéma partagé, `ddl-auto: validate` côté Hibernate |

### Justification de l'architecture polyglotte

Le découpage Python / Java s'appuie sur l'**affinité technique** de chaque écosystème plutôt que sur un découpage arbitraire :

- Python pour tout ce qui touche au matériel (PLC), à l'ingestion de données et au calcul des seuils au fil de l'eau.
- Java pour tout ce qui touche à l'utilisateur, à la sécurité, au reporting et à la logique métier transverse.

La communication événementielle entre les deux services passe par **PostgreSQL LISTEN/NOTIFY**, évitant l'introduction d'un broker de messages (RabbitMQ, Kafka) disproportionné pour le volume et la criticité temporelle du projet.

---

## Structure du dépôt

```
paint-booth-monitor/
├── frontend/                 # Interface React (dashboard, historique, auth)
├── java-service/             # Gateway Spring Boot (auth, KPIs, rapports, WebSocket)
├── python-service/           # Collecte PLC, IA, alerting
├── docker/                   # Scripts d'initialisation (PostgreSQL)
├── docker-compose.yml        # Orchestration des 5 services (postgres, java, python, frontend, minio)
├── .env.example              # Variables d'environnement Docker (modèle) 
└── Cahier_des_charges.md     # Spécifications fonctionnelles et techniques
```

---

## Prérequis

- **Docker** et **Docker Compose** (déploiement recommandé)
- **Java 21** et **Maven 3.8+** (développement local du service Java)
- **Python 3.13+** et **pip** (développement local du service Python)
- **Node.js 24+** (développement local du frontend)
- Accès réseau à l'automate S7-1200 (IP configurée, même sous-réseau)
- Clé API LLM pour le module chatbot (Spring AI) — [À COMPLÉTER : provider LLM choisi]

---

## Installation et démarrage

### Via Docker Compose (recommandé)

```bash
# 1. Copier et renseigner les variables d'environnement
cp .env.example .env

# 2. Lancer l'ensemble des services
docker compose up --build

# 3. Accès aux services
# Frontend  : http://localhost:80
# Java API  : http://localhost:8081
# Python API: http://localhost:8000 (interne, non exposé au frontend)
# PostgreSQL: localhost:5432
# MinIO     : http://localhost:9000 (API), http://localhost:9001 (Console)
```

Si la base a déjà été initialisée avec une ancienne configuration, recréer le volume :

```bash
docker compose down -v
docker compose up --build
```

### Développement local (service par service)

**Java**

```bash
cd java-service
cp .env.example .env
mvn spring-boot:run
```

**Python**

```bash
cd python-service
cp .env.example .env
pip install -r requirements.txt
uvicorn app.main:app --reload
```

**Frontend**

```bash
cd frontend
npm install
npm run dev
```

---

## Configuration

Les secrets et paramètres sensibles sont externalisés dans des fichiers `.env`, jamais versionnés.

| Fichier | Usage |
|---|---|
| `.env` (racine) | Variables Docker Compose (PostgreSQL, JWT, PLC, frontend) |
| `java-service/.env` | Connexion JDBC, JWT, URL du service Python (dev local) |
| `python-service/.env` | Connexion SQLAlchemy async, paramètres PLC (dev local) |
| `frontend/.env` | `VITE_API_URL`, `VITE_WS_URL` |

Copier les fichiers `.env.example` correspondants avant le premier lancement.

**Base de données** : `supervision_db`, avec des comptes applicatifs distincts (`java_service`, `python_service`) créés automatiquement au premier démarrage Docker via `docker/postgres/init.sh`.

---

## Rôles et sécurité

Le système distingue **deux rôles**. Aucun accès anonyme n'est autorisé.

### Superviseur

- Consultation du dashboard temps réel et de l'historique.
- Recherche par identifiant de caisse ou plage horaire.
- Consultation des KPIs et des alertes (lecture seule).
- Interrogation du chatbot (tool calling).
- Téléchargement du rapport PDF et export CSV/Excel.
- Réception des notifications selon ses préférences.

### Admin

- Tous les droits superviseur.
- Gestion des comptes superviseur.
- Configuration des seuils d'alerte (absolus et dynamiques).
- Consultation des logs d'accès et d'audit.

L'authentification repose sur **JWT** (Spring Security côté Java). Le frontend attache le token à chaque requête via l'en-tête `Authorization: Bearer`.

---

## Détection d'anomalies

La détection d'anomalies repose sur **deux mécanismes de seuils complémentaires** fonctionnant en parallèle :

| Mécanisme | Principe | Sévérité typique |
|---|---|---|
| Seuils absolus | Limites fixes définies par l'Admin (specs qualité peinture) | Critique |
| Seuils dynamiques | Bornes recalculées périodiquement (moyenne mobile ± marge) | Moyenne |

Les seuils dynamiques calculés sur la moyenne mobile glissante permettent de détecter les dérives de comportement du process avant que les limites critiques ne soient franchies.

---

## Livrables attendus

- Application connectée à l'automate S7-1200.
- Base de données d'historique des températures et de l'humidité.
- Tableau de bord temps réel et historique (double métrique).
- Système d'alerting à 2 niveaux (seuils absolus, dynamiques).
- Chatbot à appel d'outils pour l'interrogation de l'historique.
- KPIs adaptés au contexte qualité peinture.
- Rapport journalier PDF et exports CSV/Excel.
- Authentification et gestion des rôles (Superviseur / Admin).
- Documentation de conception (UML, MCD/MLD, diagrammes de séquence, composants, déploiement).

---

## Stratégie de développement

Le projet a été engagé **directement en architecture polyglotte** (Frontend React, gateway Java, service Python, PostgreSQL), sans passer par une phase monolithique Python préalable.

Compte tenu d'une durée contrainte (environ 8 semaines) et d'un développement majoritairement solo, l'implémentation suit néanmoins une **montée en charge priorisée** :

1. **Frontend ↔ Java** - authentification JWT, routes protégées, API gateway, gestion des erreurs.
2. **Configuration transverse** - Docker Compose, variables d'environnement, base PostgreSQL opérationnelle.
3. **Java ↔ Python** - contrat REST interne, proxy gateway, LISTEN/NOTIFY pour les alertes temps réel.
4. **Fonctionnalités avancées** - collecte PLC, IA, KPIs, rapports, CI/CD.

Cette approche permet de valider rapidement une chaîne bout en bout (connexion, JWT, route protégée) tout en conservant la séparation des responsabilités dès le départ.

---

## Documentation complémentaire

| Document | Contenu |
|---|---|
| [`Cahier_des_charges.md`](Cahier_des_charges.md) | Spécifications fonctionnelles complètes |
| [`GUIDE_INSTALLATION.md`](GUIDE_INSTALLATION.md) | Guide d'installation et de déploiement |

---

## Licence

Ce projet est distribué sous la licence [MIT](LICENSE). Voir le fichier [LICENSE](LICENSE) pour plus de détails.

