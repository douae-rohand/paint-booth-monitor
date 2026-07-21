# Architecture Polyglotte — Hiérarchie et Configuration Priorisée

**Projet PFA — Système de Supervision et Historisation Cabine de Peinture (S7-1200)**

---

## 1. Hiérarchie du projet

```
project-root/
│
├── frontend/                          # React
│   ├── src/
│   │   ├── api/
│   │   │   ├── client.js              # instance axios/fetch config (baseURL Java Gateway)
│   │   │   ├── auth.js
│   │   │   ├── measures.js
│   │   │   ├── alerts.js
│   │   │   ├── kpis.js
│   │   │   ├── rag.js
│   │   │   └── ws.js                  # client WebSocket
│   │   ├── components/
│   │   │   ├── dashboard/
│   │   │   ├── charts/
│   │   │   ├── alerts/
│   │   │   ├── chatbot/
│   │   │   └── admin/
│   │   ├── contexts/
│   │   │   └── AuthContext.jsx        # gestion JWT côté client
│   │   ├── hooks/
│   │   │   └── useWebSocket.js
│   │   ├── pages/
│   │   └── App.jsx
│   ├── .env                           # VITE_API_URL, VITE_WS_URL
│   ├── package.json
│   └── Dockerfile
│
├── java-service/                      # Business & Access — API Gateway
│   ├── src/main/java/com/projet/
│   │   ├── auth/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── dto/
│   │   │   └── model/
│   │   ├── notifications/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── dto/
│   │   │   └── model/
│   │   ├── kpis/
│   │   │   ├── controller/
│   │   │   └── service/
│   │   ├── reports/
│   │   │   ├── controller/            # export CSV/Excel, PDF
│   │   │   └── service/
│   │   ├── gateway/
│   │   │   ├── PythonProxyController.java   # relais REST vers Python (IA/RAG)
│   │   │   └── WebSocketConfig.java
│   │   ├── listener/
│   │   │   └── PostgresNotifyListener.java  # LISTEN des alertes créées par Python
│   │   └── config/
│   │       ├── SecurityConfig.java    # Spring Security + JWT
│   │       ├── CorsConfig.java
│   │       └── application.yml
│   ├── pom.xml
│   └── Dockerfile
│
├── python-service/                    # Data & Intelligence
│   ├── app/
│   │   ├── main.py
│   │   ├── plc/
│   │   │   ├── service.py             # Snap7 / OPC UA
│   │   │   └── schemas.py
│   │   ├── ai/
│   │   │   ├── service.py             # Isolation Forest, régression
│   │   │   └── schemas.py
│   │   ├── rag/
│   │   │   ├── service.py             # LangChain + pgvector
│   │   │   └── schemas.py
│   │   ├── alerting/
│   │   │   ├── service.py             # seuils absolus/dynamiques
│   │   │   └── schemas.py
│   │   ├── internal_api/
│   │   │   └── router.py              # endpoints appelés uniquement par Java
│   │   └── core/
│   │       ├── config.py
│   │       └── database.py
│   ├── requirements.txt
│   └── Dockerfile
│
├── docker-compose.yml                 # orchestre frontend, java, python, postgres
├── .env
└── .github/workflows/                 # CI GitHub Actions
    ├── frontend-ci.yml
    ├── java-ci.yml
    └── python-ci.yml
```

---

## 2. Ordre de priorité — logique

L'idée : faire tourner une chaîne complète et minimale (frontend → Java → base de données) le plus vite possible, **avant** de raffiner l'infrastructure ou de brancher Python. Python/l'IA/le RAG viennent après, car ils dépendent d'un socle Frontend↔Java déjà fonctionnel.

| Priorité | Bloc | Pourquoi en premier / plus tard |
|---|---|---|
| **P1** | Frontend ↔ Java (auth, routes, erreurs) | C'est le squelette utilisateur — sans lui, rien n'est testable de bout en bout |
| **P2** | Configuration transverse minimale (juste ce qu'il faut pour lancer Java + Postgres + Frontend) | Nécessaire pour que P1 tourne concrètement, mais réduite au strict minimum (pas encore CI, pas encore logs uniformisés) |
| **P3** | Java ↔ Python (communication interne) | Vient une fois que l'auth et le flux utilisateur de base sont stables ; c'est la partie la plus complexe à sécuriser/déboguer |
| **P4** | Configuration transverse avancée (CI/CD complet, versionnage API, logs structurés) | Optimisations qui n'empêchent pas de développer, à faire une fois le système stable |

---

## 3. PRIORITÉ 1 — Frontend ↔ Java (Gateway)

- [x] **URL de base de l'API** : variable d'environnement (`VITE_API_URL`) pointant vers le service Java, jamais vers Python directement
- [x] **CORS côté Java** : autoriser l'origine du frontend (dev d'abord, prod plus tard)
- [x] **Authentification JWT** :
  - [x] Endpoint de login qui retourne le token
  - [x] Stockage du token côté client (cookie httpOnly recommandé plutôt que localStorage)
  - [x] Intercepteur HTTP côté React qui attache le token à chaque requête (`Authorization: Bearer ...`)
  - [x] Gestion du refresh token / expiration et redirection vers login si 401
- [x] **Gestion des rôles côté frontend** : routes protégées (Utilisateur vs Admin), affichage conditionnel des menus admin
- [x] **Gestion des erreurs uniformisée** : format d'erreur standard renvoyé par Java (code, message)
- [ ] **Pagination / filtrage** : convention commune pour les requêtes d'historique (dates, identifiant de caisse)
- [ ] **WebSocket** (peut arriver en fin de P1, une fois l'auth HTTP stable) :
  - [ ] Endpoint WebSocket exposé par Java (`/ws`)
  - [ ] Authentification du WebSocket (JWT à la connexion)
  - [ ] Reconnexion automatique côté client
  - [ ] Format des messages poussés (schéma JSON commun)

---

## 4. PRIORITÉ 2 — Configuration transverse minimale

*(juste ce qu'il faut pour que P1 tourne réellement, pas plus)*

- [ ] `docker-compose.yml` basique : frontend + java + postgres uniquement (Python pas encore branché)
- [x] `.env` par service avec les secrets essentiels (JWT secret, credentials DB)
- [ ] Healthcheck simple sur PostgreSQL avant démarrage de Java (`depends_on`)

---

## 5. PRIORITÉ 3 — Java ↔ Python (communication interne)

- [ ] **Contrat d'API REST interne** :
  - [ ] Définir les endpoints Python appelés par Java (ex : `POST /internal/rag/query`, `GET /internal/ai/prediction`)
  - [ ] Schémas de requête/réponse identiques des deux côtés (Pydantic ↔ DTO Java)
  - [ ] Documentation du contrat (OpenAPI/Swagger côté Python)
- [ ] **Sécurisation de l'appel interne** : réseau Docker interne uniquement (pas de port publié) ou clé d'API interne partagée
- [ ] **Timeout et gestion des erreurs** : comportement de Java si Python ne répond pas
- [ ] **PostgreSQL LISTEN/NOTIFY** :
  - [ ] Nom du canal NOTIFY (ex : `alert_created`)
  - [ ] Format du payload NOTIFY (JSON : id d'alerte, sévérité, métrique)
  - [ ] Client Java en écoute permanente (LISTEN), avec reconnexion automatique
  - [ ] Gestion de l'idempotence (double traitement du même NOTIFY)
- [ ] **Accès base de données partagée** :
  - [ ] Définir qui écrit sur quelles tables (Python : mesures/alertes ; Java : utilisateurs/config seuils)
  - [ ] Outil de migration unique (Flyway ou Alembic) pour éviter les conflits de schéma
  - [ ] Pool de connexion et droits PostgreSQL différenciés par service

---

## 6. PRIORITÉ 4 — Configuration transverse avancée

- [ ] `docker-compose.yml` complet (Python inclus, réseau Docker finalisé)
- [ ] Healthchecks `/health` sur Java et Python
- [ ] Logs uniformisés (format JSON structuré) pour corréler les deux services
- [ ] CI/CD complet (pipelines séparés frontend/java/python + tests end-to-end)
- [ ] Versionnage de l'API interne (`/internal/v1/...`)
- [ ] Script d'init SQL pour l'extension `pgvector` (nécessaire uniquement une fois le RAG branché)

---

## 7. Rappel du point de vigilance

Même en démarrant directement en polyglotte, l'objectif de la Priorité 1 + 2 est de valider **une chaîne complète minimale** (un utilisateur se connecte, obtient un JWT, appelle une route protégée, reçoit une réponse de Java) avant d'investir du temps dans la communication Java↔Python. Ça permet de détecter tôt les problèmes de base (CORS, JWT, Docker) sans les mélanger avec la complexité du LISTEN/NOTIFY ou du contrat REST interne.
