# Système de Supervision et Historisation des Températures et de l'Humidité – Cabine de Peinture (S7-1200)

**Version 3 — Document mis à jour suite aux échanges de conception sur l'architecture logicielle**
**Projet : PFA (Projet de Fin d'Année)**

---

## Historique des modifications par rapport aux versions précédentes

| Élément | Version 1 (initiale) | Version 2 | Version 3 (actuelle) |
|---|---|---|---|
| Nature du projet | Stage | PFA | PFA |
| Métriques supervisées | Température uniquement | Température et Humidité | Température et Humidité |
| Volet IA prédictif | Optionnel/avancé | Confirmé | Confirmé |
| Rôles utilisateurs | Non détaillé | 2 rôles confirmés | 2 rôles confirmés |
| Traçabilité des lots | Axe validé initialement | Exclue, remplacée par recherche manuelle | Exclue, remplacée par recherche manuelle |
| Chatbot RAG | Non mentionné | Ajouté | Ajouté |
| KPIs industriels | Non mentionnés | Ajoutés (adaptés) | Ajoutés (adaptés) |
| Architecture logicielle | Non définie | Backend "à déterminer" | **Architecture polyglotte détaillée (Python + Java), avec stratégie de mitigation du risque** |
| Stratégie de développement | Non définie | Non définie | **Approche en 2 temps : socle fonctionnel d'abord, extraction polyglotte ensuite (Option B)** |

---

## 1. Contexte et problématique

La cabine de peinture nécessite un suivi rigoureux de la température **et de l'humidité** afin de garantir la qualité du processus de production. Actuellement, ces données sont visualisées localement sur l'IHM, sans historisation exploitable ni possibilité d'analyse a posteriori.

L'objectif du projet est de concevoir une application capable de se connecter à l'automate Siemens S7-1200, de récupérer en temps réel les valeurs de température et d'humidité de la cabine de peinture, de les historiser, de les représenter graphiquement, de produire automatiquement un rapport journalier, et d'intégrer un volet d'analyse intelligente (IA) pour la détection d'anomalies et la prédiction de dérives.

---

## 2. Objectifs du projet

* Établir une communication fiable entre un PC et l'automate S7-1200 pour la lecture des variables de température et d'humidité
* Historiser les données mesurées dans une base de données horodatée (PostgreSQL)
* Développer une interface de supervision avec courbes et tendances (temps réel et historique)
* Générer automatiquement un rapport journalier (PDF)
* Intégrer un module d'intelligence artificielle pour la détection d'anomalies et la prédiction de dérive thermique/hygrométrique
* Mettre en place un système d'alerting à seuils multiples (absolus et dynamiques)
* Fournir un chatbot RAG permettant l'interrogation en langage naturel de l'historique des données
* Calculer des indicateurs de performance (KPIs) adaptés au contexte qualité peinture
* Gérer l'authentification et les droits d'accès selon 2 rôles (Utilisateur, Admin)

---

## 3. Répartition des tâches

### 3.1 Informatique (étudiante)

* Conception et développement de l'application complète (backend, frontend)
* Conception de la base de données (MCD/MLD, PostgreSQL)
* Interface graphique de supervision (dashboard temps réel, historique)
* Génération des graphes et courbes de tendance (température et humidité)
* Génération automatique du rapport journalier (PDF)
* Module de détection d'anomalies à seuils (absolu et dynamique)
* Module IA (Isolation Forest et/ou modèle de régression) pour la dérive thermique/hygrométrique
* Chatbot RAG pour l'interrogation en langage naturel de l'historique
* Calcul des KPIs adaptés (taux de conformité, temps moyen entre incidents, temps moyen de retour à la normale)
* Système d'authentification et gestion des rôles (Utilisateur / Admin)
* Système de notification multicanal (email, WhatsApp, push)
* Export des données (CSV/Excel)
* Recherche manuelle dans l'historique par identifiant de caisse (en remplacement de la traçabilité formelle des lots)

### 3.2 Automatisme / Électronique (binôme)

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

### Architecture de la donnée (clarification importante)

Il est essentiel de distinguer deux niveaux de données :

1. **Le PLC (DB du S7-1200)** : contient uniquement les valeurs **instantanées**, écrasées à chaque cycle de scan de l'automate. Ce n'est pas une source d'historique.
2. **WinCC** : historise en théorie les données mesurées dans une base **PostgreSQL** de production, gérée et sécurisée par l'équipe IT/OT de Renault. L'étudiante n'y a pas accès dans le cadre du PFA, ce qui exclut ce scénario d'architecture.

**Scénario retenu — Connexion directe au PLC** : lecture périodique via Snap7 (protocole S7 natif) ou OPC UA, avec historisation gérée entièrement côté application (pas de WinCC intermédiaire), puis stockage dans une base de données dédiée au projet.

Le choix du protocole de communication (Snap7 vs OPC UA) relève de la responsabilité du binôme automatisme, en lien avec la configuration réelle du PLC et les DB exposées.

---

## 5. Rôles utilisateurs et gestion des accès

Le système comporte **2 rôles**, confirmés par l'encadrant :

### 5.1 Utilisateur

* Se connecter / se déconnecter
* Consulter le dashboard temps réel (courbes et valeurs actuelles de température et d'humidité)
* Consulter l'historique et les courbes de tendance sur une période donnée, avec recherche possible par identifiant de caisse
* Consulter les KPIs
* Consulter les alertes et anomalies actives et passées
* Consulter les prédictions de dérive issues du module IA (lecture seule)
* Interroger le chatbot RAG sur l'historique des données
* Télécharger le rapport journalier PDF
* Exporter des données en CSV/Excel
* Recevoir des notifications d'alerte selon ses préférences de canal

### 5.2 Admin

Hérite de tous les droits de l'Utilisateur, plus :

* Gérer les comptes utilisateurs (création, modification, désactivation)
* Configurer les seuils d'alerte (absolus et dynamiques)
* Configurer les destinataires et canaux de notification (email, WhatsApp, push)
* Consulter et piloter les résultats du module IA
* Consulter les logs d'accès et d'audit

L'authentification s'applique à l'ensemble des utilisateurs, sans exception — aucun accès anonyme ou public au système.

---

## 6. Système de détection d'anomalies — architecture à 3 mécanismes indépendants

La détection d'anomalies repose sur **trois mécanismes distincts et complémentaires** :

### 6.1 Seuils absolus

Limites physiques/qualité fixes, définies une fois par l'Admin sur la base des spécifications qualité réelles du processus peinture (ex : température jamais en dehors de [50°C, 95°C]). Ne changent jamais automatiquement. Dépassement → alerte de sévérité **critique**.

### 6.2 Seuils dynamiques

Bornes recalculées automatiquement à intervalle régulier (ex : toutes les heures), basées sur une moyenne mobile des mesures récentes ± une marge configurée par l'Admin. Objectif : détecter une dérive progressive de comportement **avant** qu'elle n'atteigne le seuil absolu. Dépassement → alerte de sévérité **moyenne**.

### 6.3 Module IA (détection par apprentissage automatique)

Réservé aux véritables algorithmes de machine learning : **Isolation Forest** (détection d'anomalies non supervisée) et/ou **modèle de régression** (prédiction de la valeur à court terme, avec comparaison à la valeur réelle mesurée). Ce module est structuré via une interface commune, permettant de changer d'algorithme ou d'en ajouter un nouveau sans modifier le reste du système.

> Clarification terminologique : les seuils dynamiques et la moyenne mobile relèvent de méthodes statistiques classiques et non de l'intelligence artificielle au sens strict. Cette distinction a été clarifiée dans la conception pour éviter toute ambiguïté technique.

Les trois mécanismes fonctionnent en parallèle, en continu, et génèrent chacun des alertes indépendamment.

---

## 7. Système d'alertes et de notifications

* Canaux de notification pris en charge : **email, WhatsApp, push**
* Chaque utilisateur configure ses préférences de canal (un ou plusieurs canaux actifs simultanément)
* Une alerte est caractérisée par : la métrique concernée, le type (seuil absolu, seuil dynamique, dérive IA), la sévérité (faible, moyenne, critique), un statut (active/résolue)
* Les notifications ne sont pas limitées aux alertes : le système notifie également d'autres événements (rapport généré, compte créé, seuil modifié)

---

## 8. Indicateurs de performance (KPIs)

Les indicateurs industriels classiques (OEE, MTBF, MTTR) ne sont pas directement applicables : ce système ne supervise pas des pannes d'équipement mais une grandeur physique de process liée à la qualité du produit fini. Les indicateurs ont donc été adaptés et déclinés **par métrique** (température et humidité séparément) :

* **Taux de conformité thermique / hygrométrique** — pourcentage du temps où la métrique est restée dans la plage acceptable sur une période donnée
* **Temps moyen entre incidents (thermiques / hygrométriques)** — durée moyenne entre deux dépassements de seuil ou dérives détectées
* **Temps moyen de retour à la normale** — durée moyenne entre le déclenchement d'une alerte et le retour dans la plage normale

---

## 9. Chatbot RAG

Module permettant d'interroger en langage naturel l'historique des mesures (température, humidité) et des événements du système (alertes, incidents).

---

## 10. Traçabilité — clarification du périmètre

La traçabilité formelle des lots de production (association automatique entre un lot identifié et ses conditions de température/humidité) est **exclue** du périmètre, en l'absence d'un retour qualité véhicule exploitable côté Renault.

En remplacement, chaque mesure peut être associée à un **identifiant de caisse** (champ optionnel), permettant à l'Utilisateur d'effectuer une **recherche manuelle** dans l'historique (par identifiant de caisse ou par plage horaire) pour retrouver les conditions climatiques associées au passage d'une caisse donnée dans la cabine.

---

## 11. Architecture technique retenue (vue d'ensemble)

* **Frontend** : React, prototypage via Lovable, style neumorphisme avec accent orange/gold
* **Backend** : Architecture **polyglotte** — voir section 13 pour le détail complet
* **Base de données** : PostgreSQL (extension `pgvector` pour les embeddings du RAG)
* **IA / RAG** : Python (scikit-learn, LangChain)
* **Conteneurisation** : Docker / docker-compose
* **Intégration continue** : GitHub Actions
* **Connexion PLC** : Snap7 ou OPC UA (choix du protocole porté par le binôme automatisme, implémentation côté service Python)
* **Gestion de projet** : à déterminer

---

## 12. Livrables attendus

* Application fonctionnelle connectée à l'automate S7-1200
* Base de données d'historique des températures et de l'humidité
* Tableau de bord avec graphes temps réel et historiques (double métrique)
* Système d'alerting à seuils absolus et dynamiques
* Module IA de détection de dérive (Isolation Forest et/ou régression)
* Chatbot RAG pour l'interrogation de l'historique
* Calcul et affichage des KPIs adaptés
* Génération automatique du rapport journalier (PDF)
* Export des données en CSV/Excel
* Système d'authentification et de gestion des rôles (Utilisateur / Admin)
* Documentation de conception complète (diagrammes UML, MCD/MLD, diagrammes de séquence, diagramme de composants et de déploiement)

---

## 13. Architecture logicielle détaillée — approche polyglotte

### 13.1 Principe et justification

Le backend est réparti sur **deux services** répartis par affinité technique plutôt que par découpage arbitraire, chacun exploitant l'écosystème le plus mature pour sa responsabilité :

* **Service "Data & Intelligence" (Python / FastAPI)** — tout ce qui touche au matériel et à l'intelligence artificielle
* **Service "Business & Access" (Java / Spring Boot)** — tout ce qui touche à l'utilisateur et à la logique métier/accès

Ce découpage reflète des patterns réels observés dans l'industrie (agent de collecte/IA en Python côté edge, couche applicative en Java/C# côté business), notamment dans des contextes industriels comparables au périmètre Renault de ce projet.

### 13.2 Schéma d'architecture

Le service Java est le **point d'entrée unique** du système : le frontend ne communique jamais directement avec le service Python. Toute demande impliquant l'IA ou le RAG transite par Java, qui relaie en interne vers Python puis retransmet la réponse.

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (React)                          │
└───────────────────────┬───────────────────────────────────┘
                         │ REST + WebSocket
                         │ (SEUL point d'entrée du système)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│    Service Java / Spring Boot — "Business & Access"            │
│    (API Gateway du système)                                     │
│  • Authentification & rôles (Spring Security + JWT)             │
│  • Configuration des seuils par l'Admin (écrit en base)         │
│  • Dispatch des notifications (email / WhatsApp / push)          │
│  • Calcul des KPIs (requêtes agrégées sur mesures/alertes)       │
│  • CRUD utilisateurs, export CSV/Excel, génération rapport PDF   │
│  • API REST + WebSocket vers le frontend                        │
│  • Relaie en interne vers Python (prédiction IA, requête RAG)    │
└──────┬────────────────────────────────────────────┬───────────┘
       │ appel REST interne                          │ lit / écrit
       │ (synchrone, à la demande)                    ▼
       │                                  ┌────────────────────┐
       │                                  │   PostgreSQL          │
       │                                  │   (+ extension          │
       │                                  │    pgvector)             │
       │                                  └────────────────────┘
       │                                            ▲       ▲
       │                                            │       │ NOTIFY (alerte créée)
       │                                            │ lit/écrit  │
       ▼                                            │       │
┌─────────────────────────────────────────────────────────────┐
│    Service Python / FastAPI — "Data & Intelligence"           │
│    (jamais exposé directement au frontend)                     │
│  • Lecture PLC (Snap7 ou OPC UA) — polling périodique           │
│  • Écriture des mesures en base                                 │
│  • Calcul des seuils absolus et dynamiques (au fil de l'eau)     │
│  • Isolation Forest / régression (module IA)                     │
│  • Chatbot RAG (LangChain + pgvector)                             │
│  • NOTIFY PostgreSQL lors de la création d'une alerte             │
│  • Répond aux appels REST internes venant de Java                 │
└─────────────────────────────────────────────────────────────┘
```

**Les deux flux de communication à bien distinguer :**

* **Flux "action utilisateur"** (synchrone, initié par le frontend) : `Frontend → Java (vérifie l'auth/les droits) → Python (calcule/répond) → Java → Frontend`. Exemple : une question posée au chatbot RAG, ou la consultation d'une prédiction IA.
* **Flux "collecte de données"** (asynchrone, initié en continu par Python, indépendant du frontend) : `PLC → Python (lit, écrit en base, calcule les seuils) → NOTIFY PostgreSQL → Java (LISTEN, déclenche les notifications)`. Python ne parle jamais directement au frontend ni à Java dans ce flux — tout passe par la base de données.

### 13.3 Répartition détaillée des responsabilités

| Responsabilité | Service | Justification |
|---|---|---|
| Lecture PLC (Snap7/OPC UA) | Python | `python-snap7` et `asyncua` sont les librairies les plus matures pour ce cas d'usage |
| Historisation des mesures | Python | Écrit directement à la source de la donnée collectée |
| Calcul seuils absolus/dynamiques | Python | Calculé au fil de l'eau, au moment de l'ingestion de la mesure |
| Module IA (Isolation Forest, régression) | Python | Écosystème scikit-learn, aucun équivalent aussi riche en Java |
| Chatbot RAG | Python | Écosystème LangChain/embeddings pensé Python-first |
| Authentification & rôles | Java | Spring Security offre une gestion de rôles plus fine et mature |
| Configuration des seuils (côté Admin) | Java | Fonction d'administration, cohérente avec le reste des CRUD |
| Notifications multicanal | Java | Logique métier de dispatch, indépendante de la donnée brute |
| KPIs | Java | Requêtes agrégées orientées reporting/présentation |
| Export CSV/Excel, rapport PDF | Java | Fonctions orientées utilisateur final |
| API Gateway vers le frontend | Java | Point d'entrée unique pour l'authentification et les WebSockets |

### 13.4 Communication inter-services

* **PostgreSQL LISTEN/NOTIFY** est utilisé comme mécanisme événementiel léger : le service Python exécute un `NOTIFY` après écriture d'une nouvelle alerte, le service Java reste en écoute (`LISTEN`) pour déclencher le dispatch des notifications.
* Ce choix évite l'introduction d'un message broker dédié (RabbitMQ/Kafka), jugé disproportionné au regard du volume de données et de la criticité temporelle du projet.
* Les échanges ponctuels (ex : Java interrogeant Python pour une prédiction IA à la demande, ou pour une requête au chatbot RAG) passent par une API REST interne simple entre les deux services.

### 13.5 Stratégie de mitigation du risque (approche en 2 temps)

Compte tenu d'une durée de développement contrainte (moins de 2 mois) et d'un développement en solo, l'architecture polyglotte est mise en œuvre selon une **approche en 2 temps** visant à sécuriser la livraison d'un système complet avant d'introduire la complexité du second langage :

**Temps 1 — Socle fonctionnel modulaire (Python)**
L'ensemble des fonctionnalités est d'abord développé en Python (FastAPI), organisé en modules strictement cloisonnés (`plc/`, `ai/`, `rag/`, `alerting/`, `auth/`, `notifications/`, `kpis/`), communiquant entre eux via des interfaces explicites (pas d'accès direct aux données d'un autre module). Les schémas d'entrée/sortie de chaque module sont définis avec Pydantic comme s'il s'agissait déjà de contrats d'API réseau, afin de préparer une extraction future à moindre coût.

**Temps 2 — Extraction polyglotte (conditionnelle)**
Une fois le socle fonctionnel validé (jalon fixé à environ 5 semaines sur les 8 disponibles), les modules les plus indépendants (typiquement `auth/` et `notifications/`) sont **réimplémentés** en Java/Spring Boot, avec le même contrat d'API, puis le frontend et les modules Python restants sont rebranchés vers ce nouveau service. Le module Python équivalent est conservé comme filet de sécurité jusqu'à validation complète de la bascule.

Ce séquencement garantit que :
* Un système complet et fonctionnel est disponible même si le temps ne permet pas d'aller au bout de l'extraction polyglotte
* La décision d'engager l'extraction est prise avec une visibilité réelle sur l'avancement du projet, plutôt qu'anticipée dès le départ sans garantie de temps disponible
* La modularisation initiale, si elle est bien conçue, rend l'extraction largement mécanique plutôt qu'une réécriture complète

### 13.6 Limites assumées de cette approche

* L'extraction d'un module vers Java représente un effort de réimplémentation réel (nouveau code, nouveaux tests), non un simple changement de configuration
* En cas de non-extraction, le projet reste présenté comme un **modular monolith** — une architecture reconnue et légitime dans l'industrie, conçue pour être extraite en microservices si le contexte le justifie
* Le choix du protocole PLC (Snap7 vs OPC UA) reste de la responsabilité du binôme automatisme et conditionne certains détails d'implémentation du module `plc/`
