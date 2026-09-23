# Système de supervision et d’historisation en temps réel de la température et de l’humidité d’une cabine de peinture et d’une étuve industrielles

## 1. Contexte et problématique

La cabine de peinture et l'étuve nécessitent un suivi rigoureux de la température **et de l'humidité** afin de garantir la qualité du processus de production. Actuellement, ces données sont visualisées localement sur l'IHM, sans historisation exploitable ni possibilité d'analyse a posteriori.

L'objectif du projet est de concevoir une application capable de se connecter à l'automate Siemens S7-1200, de récupérer en temps réel les valeurs de température et d'humidité de la cabine de peinture et de l'étuve, de les historiser, de les représenter graphiquement, de produire automatiquement un rapport journalier, et d'intégrer un système robuste de détection d'anomalies (seuils absolus et dynamiques) et d'assistance via un chatbot intelligent.

---

## 2. Objectifs du projet

* Établir une communication fiable entre un PC et l'automate S7-1200 pour la lecture des variables de température et d'humidité
* Historiser les données mesurées dans une base de données horodatée (PostgreSQL)
* Développer une interface de supervision avec courbes et tendances (temps réel et historique)
* Générer automatiquement un rapport journalier (PDF)
* Mettre en place un système d'alerting à seuils multiples (seuils absolus critiques et seuils dynamiques par moyenne mobile)
* Fournir un chatbot à appel d'outils (tool calling) permettant l'interrogation en langage naturel de l'historique des données
* Calculer des indicateurs de performance (KPIs) adaptés au contexte qualité peinture
* Gérer l'authentification et les droits d'accès selon 2 rôles (Superviseur, Admin)

---

## 3. Répartition des tâches

### 3.1 Informatique (étudiante)

* Conception et développement de l'application complète (backend, frontend)
* Conception de la base de données (MCD/MLD, PostgreSQL)
* Interface graphique de supervision (dashboard temps réel, historique)
* Génération des graphes et courbes de tendance (température et humidité)
* Génération automatique du rapport journalier (PDF)
* Module de détection d'anomalies à seuils (absolu et dynamique)
* Chatbot à appel d'outils pour l'interrogation en langage naturel de l'historique
* Calcul des KPIs adaptés (taux de conformité, temps moyen entre incidents, temps moyen de retour à la normale)
* Système d'authentification et gestion des rôles (Superviseur / Admin)
* Système de notification multicanal (email, Web Push natif navigateur/VAPID, notifications in-app temps réel)
* Export des données (CSV/Excel)
* Recherche manuelle dans l'historique par identifiant de caisse (en remplacement de la traçabilité formelle des lots)

### 3.2 Automatisme / Électronique

* Étude de la configuration existante du S7-1200 (adressage, DB, mnémoniques des variables de température et d'humidité)
* Choix et mise en œuvre du protocole de communication (Snap7 ou OPC UA)
* Vérification et câblage des capteurs de température **et d'humidité** de la cabine (type, étendue de mesure, position)
* Adaptation du programme TIA Portal si nécessaire (création/exposition des DB pour la lecture)
* Tests de robustesse de la liaison (coupures réseau, redémarrage automate, etc.)
* Validation terrain des valeurs remontées par rapport à la réalité

---

## 4. Informations détaillées sur l'automate

**Modèle** : SIMATIC S7-1200, CPU 1215C DC/DC/RLY
**Référence** : 6ES7 215-1HG40-0XB0
**Année de fabrication** : 2021

C'est un CPU moderne, nativement supporté par TIA Portal, sans problématique de compatibilité héritée d'anciennes générations d'automates (contrairement à un S7-300 par exemple).

**Outil de développement** : TIA Portal V17 (licence d'essai 21 jours, installée sur poste personnel de l'étudiante pour les besoins de prise en main et de configuration initiale).

**Connexion physique** : liaison Ethernet entre le PC et l'automate, avec configuration d'une IP statique sur le même sous-réseau, puis scan via la fonctionnalité "Accessible devices" de TIA Portal.

### Architecture de la donnée

Le système collecte et historise les données de **deux équipements majeurs** :
1. **La cabine de peinture** : supervision de la température et de l'humidité relative.
2. **L'étuve industrielle** : supervision de la température sur 5 zones distinctes.

**Scénario retenu - Connexion directe au PLC** : lecture périodique via Snap7 (protocole S7 natif) directement sur l'automate S7-1200, avec historisation gérée entièrement côté application puis stockage dans la base de données PostgreSQL dédiée au projet.

Le choix du protocole de communication (Snap7) a été validé par l'encadrant d'entreprise.

---

## 5. Rôles utilisateurs et gestion des accès

Le système comporte **2 rôles**, confirmés par l'encadrant :

### 5.1 Superviseur

* Se connecter / se déconnecter
* Consulter le dashboard temps réel (courbes et valeurs actuelles de température et d'humidité)
* Consulter l'historique et les courbes de tendance sur une période donnée, avec recherche possible par identifiant de caisse
* Consulter les KPIs
* Consulter les alertes et anomalies actives et passées
* Interroger le chatbot sur l'historique des données
* Télécharger le rapport PDF
* Exporter des données en CSV/Excel
* Recevoir des notifications d'alerte 

### 5.2 Admin

Hérite de tous les droits de l'Utilisateur, plus :

* Gérer les comptes utilisateurs (création, modification, désactivation)
* Configurer les seuils d'alerte (absolus et dynamiques)
* Consulter les logs d'accès et d'audit (journal des 13 actions sensibles : connexions, gestion des comptes, modifications de configuration, exports, rapports)

L'authentification s'applique à l'ensemble des utilisateurs, sans exception - aucun accès anonyme ou public au système.

---

## 6. Système de détection d'anomalies - architecture à 2 mécanismes de seuils

La détection d'anomalies repose sur **deux mécanismes distincts et complémentaires** :

### 6.1 Seuils absolus

Limites physiques/qualité fixes, définies une fois par l'Admin sur la base des spécifications qualité réelles du processus peinture (ex : température jamais en dehors de [50°C, 95°C]). Ne changent jamais automatiquement. Dépassement → alerte de sévérité **critique**.

### 6.2 Seuils dynamiques

Bornes recalculées automatiquement à intervalle régulier (ex : toutes les heures), basées sur une moyenne mobile des mesures récentes ± une marge configurée par l'Admin. Objectif : détecter une dérive progressive de comportement **avant** qu'elle n'atteigne le seuil absolu. Dépassement → alerte de sévérité **moyenne**.

> Clarification terminologique : les seuils dynamiques et la moyenne mobile relèvent de méthodes statistiques et permettent de détecter les dérives de processus. Les modèles d'IA complexes (dérive IA) ont été écartés pour privilégier cette approche robuste et explicable.

Les deux mécanismes fonctionnent en parallèle, en continu, et génèrent chacun des alertes indépendamment.

---

## 7. Système d'alertes et de notifications

* Canaux de notification pris en charge : **email, Web Push natif navigateur (protocole VAPID, sans dépendance à un service tiers), notifications in-app (WebSocket STOMP)**
* Le canal WhatsApp initialement prévu a été abandonné - retiré de l'enum Canal et des contraintes de base de données (migration V43)
* Les préférences de canal sont gérées en code (mapping en dur par type d'événement) - la table `configuration_destinataire` initialement prévue a été supprimée (migration V38)
* Une alerte est caractérisée par : la métrique concernée, le type (seuil absolu, seuil dynamique), la sévérité (faible, moyenne, critique), un statut (active/résolue)
* Les notifications couvrent plusieurs types d'événements : alertes créées/résolues, activation de compte superviseur, modification de configuration des seuils
* Les abonnements Web Push (endpoint + clés de chiffrement du navigateur) sont stockés en base dans la table `abonnement_push_navigateur` - un superviseur peut avoir plusieurs abonnements actifs (plusieurs navigateurs/appareils)
* Les notifications in-app sont poussées en temps réel via WebSocket STOMP sur le topic `/user/queue/notifications` (personnel par utilisateur)

---

## 8. Indicateurs de performance (KPIs)

Les indicateurs industriels classiques (OEE, MTBF, MTTR) ne sont pas directement applicables : ce système ne supervise pas des pannes d'équipement mais une grandeur physique de process liée à la qualité du produit fini. Les indicateurs ont donc été adaptés et déclinés **par métrique** (température et humidité séparément) :

* **Taux de conformité thermique / hygrométrique** - pourcentage du temps où la métrique est restée dans la plage acceptable sur une période donnée
* **Temps moyen entre incidents (thermiques / hygrométriques)** - durée moyenne entre deux dépassements de seuil ou dérives détectées
* **Temps moyen de retour à la normale** - durée moyenne entre le déclenchement d'une alerte et le retour dans la plage normale

---

## 9. Chatbot à appel d'outils (Tool Calling)

> **Écart assumé par rapport aux versions antérieures de ce document** : la version initiale décrivait un chatbot RAG vectoriel (embeddings pgvector + LangChain, côté service Python). Cette approche a été abandonnée lors de la conception détaillée - la justification principale de placer le chatbot côté Python était l'écosystème LangChain/embeddings, devenue caduque une fois la recherche par similarité vectorielle écartée. Le chatbot est désormais un module Java (Spring AI), exploitant les services métier existants via le mécanisme de tool calling natif des LLMs.

Le chatbot permet d'interroger en langage naturel l'historique des mesures (température, humidité) et les événements du système (alertes, incidents).

**Principe du tool calling :**

1. Le superviseur pose une question en langage naturel (ex : "Quelle était la température moyenne hier dans la cabine ?").
2. Le LLM (via Spring AI) identifie l'intention et sélectionne l'outil approprié parmi un ensemble défini (ex : `getMesuresHistorique`, `getAlertesActives`, `getKpis`).
3. Le backend Java exécute l'outil en appelant le service métier correspondant (déjà existant dans l'application).
4. Le résultat structuré est retourné au LLM, qui formule une réponse en langage naturel.

**Avantages par rapport au RAG vectoriel :**
- Les données sont déjà exposées par les services Java existants - aucune duplication ni indexation d'embeddings.
- Le comportement est déterministe et traçable (appel d'outil explicite, pas de recherche par similarité approximative).
- Aucune dépendance externe supplémentaire côté Python (LangChain, pgvector).

**[À COMPLÉTER : provider LLM choisi et clé API correspondante]**

---

## 10. Traçabilité - clarification du périmètre

La traçabilité formelle des lots de production (association automatique entre un lot identifié et ses conditions de température/humidité) est **exclue** du périmètre, en l'absence d'un retour qualité véhicule exploitable côté Renault.

En remplacement, chaque mesure peut être associée à un **identifiant de caisse** (champ optionnel), permettant à l'Utilisateur d'effectuer une **recherche manuelle** dans l'historique (par identifiant de caisse ou par plage horaire) pour retrouver les conditions climatiques associées au passage d'une caisse donnée dans la cabine.

---

## 11. Architecture technique retenue (vue d'ensemble)

* **Frontend** : React (TanStack Router, TanStack Query), style neumorphisme avec accent orange/gold
* **Backend** : Architecture **polyglotte** - voir section 13 pour le détail complet
* **Base de données** : PostgreSQL
* **Chatbot** : Java (Spring AI, tool calling) 
* **Conteneurisation** : Docker / docker-compose
* **Intégration continue** : GitHub Actions
* **Connexion PLC** : Snap7
* **Stockage fichiers** : MinIO (stockage objet S3-compatible pour les rapports PDF générés)
* **Authentification** : JWT (access token HttpOnly cookie 15 min + refresh token 7 jours avec rotation)
* **Notifications push** : Web Push natif (protocole VAPID, clés ECDSA P-256) - sans dépendance Firebase/FCM
* **Migrations base de données** : Flyway

---

## 12. Livrables attendus

* Application fonctionnelle connectée à l'automate S7-1200
* Base de données d'historique des températures et de l'humidité
* Tableau de bord avec graphes temps réel et historiques (double métrique)
* Système d'alerting à seuils absolus et dynamiques
* Chatbot à appel d'outils pour l'interrogation de l'historique
* Calcul et affichage des KPIs adaptés
* Génération du rapport (PDF)
* Export des données en CSV/Excel
* Système d'authentification et de gestion des rôles (Superviseur / Admin)
* Module d'audit des actions sensibles (journal des événements consultable par l'Admin)
* Documentation de conception complète (diagrammes UML, MCD/MLD, diagrammes de séquence, diagramme de composants et de déploiement)

---

## 13. Architecture logicielle détaillée - approche polyglotte

### 13.1 Principe et justification

Le backend est réparti sur **deux services** répartis par affinité technique plutôt que par découpage arbitraire, chacun exploitant l'écosystème le plus mature pour sa responsabilité :

* **Service "Data & Ingestion" (Python / FastAPI)** - tout ce qui touche au matériel, à l'ingestion des données PLC et au calcul des seuils au fil de l'eau
* **Service "Business & Access" (Java / Spring Boot)** - tout ce qui touche à l'utilisateur, à la sécurité, au reporting et au chatbot

Ce découpage reflète des patterns réels observés dans l'industrie (agent de collecte en Python côté edge, couche applicative en Java/C# côté business), notamment dans des contextes industriels comparables au périmètre Renault de ce projet.

### 13.2 Schéma d'architecture

Le service Java est le **point d'entrée unique** du système : le frontend ne communique jamais directement avec le service Python.

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (React)                          │
└───────────────────────┬───────────────────────────────────┘
                         │ REST + WebSocket
                         │ (SEUL point d'entrée du système)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│    Service Java / Spring Boot - "Business & Access"           │
│    (API Gateway du système)                                     │
│  • Authentification & rôles (Spring Security + JWT)             │
│  • Configuration des seuils par l'Admin (écrit en base)         │
│  • Dispatch des notifications (email / push natif / in-app)     │
│  • Calcul des KPIs (requêtes agrégées sur mesures/alertes)       │
│  • CRUD utilisateurs, export CSV/Excel, génération rapport PDF   │
│  • Chatbot (Spring AI, tool calling)                             │
│  • API REST + WebSocket vers le frontend                         │
└────────────────────────────────────────────┬────────────────┘
                                             │ lit / écrit
                                             ▼
                                  ┌────────────────────┐
                                  │   PostgreSQL          │
                                  └────────────────────┘
                                            ▲       ▲
                                            │       │ NOTIFY (alerte créée)
                                            │ lit/écrit  │
                                            │       │
┌─────────────────────────────────────────────────────────────┐
│    Service Python / FastAPI - "Data & Ingestion"              │
│    (jamais exposé directement au frontend)                     │
│  • Lecture PLC (Snap7) - polling périodique                     │
│  • Écriture des mesures en base                                 │
│  • Calcul des seuils absolus et dynamiques (au fil de l'eau)     │
│  • NOTIFY PostgreSQL lors de la création d'une alerte             │
└─────────────────────────────────────────────────────────────┘
```

**Les deux flux de communication à bien distinguer :**

* **Flux "action utilisateur"** (synchrone, initié par le frontend) : `Frontend → Java (vérifie l'auth/les droits) → Frontend`. Le chatbot est traité entièrement dans Java (tool calling).
* **Flux "collecte de données"** (asynchrone, initié en continu par Python, indépendant du frontend) : `PLC → Python (lit, écrit en base, calcule les seuils) → NOTIFY PostgreSQL → Java (LISTEN, déclenche les notifications)`. Python ne parle jamais directement au frontend ni à Java dans ce flux - tout passe par la base de données.

### 13.3 Répartition détaillée des responsabilités

| Responsabilité | Service | Justification |
|---|---|---|
| Lecture PLC (Snap7/OPC UA) | Python | `python-snap7` et `asyncua` sont les librairies les plus matures pour ce cas d'usage |
| Historisation des mesures | Python | Écrit directement à la source de la donnée collectée |
| Calcul seuils absolus/dynamiques | Python | Calculé au fil de l'eau, au moment de l'ingestion de la mesure |
| Chatbot (tool calling, Spring AI) | **Java** | Données déjà exposées par les services Java existants ; Java déjà point d'entrée unique |
| Authentification & rôles | Java | Spring Security offre une gestion de rôles plus fine et mature |
| Configuration des seuils (côté Admin) | Java | Fonction d'administration, cohérente avec le reste des CRUD |
| Notifications multicanal | Java | Logique métier de dispatch, indépendante de la donnée brute - canaux : EMAIL, Web Push (VAPID), IN_APP (WebSocket) |
| KPIs | Java | Requêtes agrégées orientées reporting/présentation - filtrées par point de mesure, métrique et période |
| Export CSV/Excel, rapport PDF | Java | Fonctions orientées utilisateur final - stockage des PDF sur MinIO |
| Audit des actions sensibles | Java | 13 actions traçables (connexions, gestion comptes, exports, rapports, configurations) |
| API Gateway vers le frontend | Java | Point d'entrée unique pour l'authentification et les WebSockets |

### 13.4 Communication inter-services

* **PostgreSQL LISTEN/NOTIFY** est utilisé comme mécanisme événementiel léger : le service Python exécute un `NOTIFY` après écriture d'une nouvelle alerte, le service Java reste en écoute (`LISTEN`) pour déclencher le dispatch des notifications.
* Ce choix évite l'introduction d'un message broker dédié (RabbitMQ/Kafka), jugé disproportionné au regard du volume de données et de la criticité temporelle du projet.


