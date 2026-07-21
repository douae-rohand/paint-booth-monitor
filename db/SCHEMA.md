# SCHEMA.md — Répartition des accès DB par entité (Java ↔ Python)

Basé sur le MCD (v1) et la répartition de responsabilités du CDC §13.3.
Sert de référence pour la définition des rôles PostgreSQL (`app_java`, `app_python`) en P3.

---

## Légende

- **Écrit par** : service qui fait `INSERT`/`UPDATE` sur l'entité
- **Lit par** : service qui fait `SELECT` (au-delà de l'écrivain lui-même)
- ⚠️ = entité à cheval, GRANT colonne par colonne nécessaire
- ✅ = un seul écrivain, partage simple (`GRANT SELECT` pour le lecteur)

---

## Domaine Auth & Access (propriété Java)

| Entité | Écrit par | Lit par | Statut |
|---|---|---|---|
| Superviseur | Java | Java | ✅ pas d'accès Python nécessaire |
| Admin | Java | Java | ✅ pas d'accès Python nécessaire |
| RefreshToken | Java | Java | ✅ interne à Java |
| TokenReinitialisation | Java | Java | ✅ interne à Java |
| LogAudit | Java | Java | ✅ actions métier/accès uniquement |

## Domaine Data & Intelligence (propriété Python)

| Entité | Écrit par | Lit par | Statut |
|---|---|---|---|
| Mesure | Python | Java (KPIs, historique, export) | ✅ Java lecture seule |
| PredictionIA | Python | Java (affichage frontend) | ✅ Java lecture seule |
| DocumentEmbedding | Python | Python uniquement | ✅ aucun accès Java requis |
| ConversationChatbot | Python (produit la paire Q/R) | Java (historique affiché au frontend) | ✅ Java lecture seule |

## Domaine partagé / configuration (propriété Java, appliqué par Python)

| Entité | Écrit par | Lit par | Statut |
|---|---|---|---|
| SeuilAbsolu | Java (Admin configure) | Python (applique le seuil à l'ingestion) | ✅ Java écrit, Python lit — pas de conflit colonne |
| ConfigurationDestinataire | Java | Java | ✅ interne à Java |
| Notification | Java | Java | ✅ interne à Java — le message (titre/contenu), une ligne par événement |
| EnvoiNotification | Java | Java | ✅ interne à Java — un envoi par (notification, superviseur, canal) |
| RapportPDF | Java | Java | ✅ interne à Java |

---

## ⚠️ Entités à cheval — GRANT colonne par colonne requis

### `Alerte`

| Colonnes | Écrites par | Détail |
|---|---|---|
| `id_alerte`, `metrique`, `type_alerte`, `severite`, `createdAt` | **Python** | créées à l'ingestion, quand un seuil (absolu/dynamique) ou l'IA détecte une anomalie |
| `statut` (ACTIVE → RESOLUE) | **Java** | mis à jour quand un Admin/Superviseur traite l'alerte (action `VALIDATION_ANOMALIE` du LogAudit) |
| `updatedAt`, `deletedAt` | **Java** (au moment de la mise à jour du statut) | |

**GRANT nécessaire :**
```sql
-- Python : peut créer l'alerte, ne doit jamais toucher au statut
GRANT SELECT, INSERT ON alerte TO app_python;

-- Java : peut lire toute l'alerte, mais ne modifie que le statut/updatedAt
GRANT SELECT, UPDATE (statut, updatedAt, deletedAt) ON alerte TO app_java;
```

### `SeuilDynamique`

| Colonnes | Écrites par | Détail |
|---|---|---|
| `id_seuil_dynamique`, `metrique`, `marge_configuree` | **Java** | créées par l'Admin (config initiale) — `marge_configuree` = le facteur `k` du calcul `moyenne ± k × écart-type`, réglable par l'Admin (plus `k` est grand, plus le seuil est tolérant) |
| `valeur_min_calculee`, `valeur_max_calculee`, `date_calcul` | **Python** | recalculées en continu (moyenne/écart-type glissants, en appliquant `marge_configuree`) |
| `createdAt`, `updatedAt`, `deletedAt` | **Java** (création/désactivation) | |

**GRANT nécessaire :**
```sql
-- Java : crée et administre le seuil, mais ne touche jamais aux valeurs calculées
GRANT SELECT, INSERT, UPDATE (metrique, marge_configuree, deletedAt) ON seuil_dynamique TO app_java;

-- Python : lit la config (dont marge_configuree, pour l'appliquer), n'écrit QUE les colonnes calculées
GRANT SELECT, UPDATE (valeur_min_calculee, valeur_max_calculee, date_calcul) ON seuil_dynamique TO app_python;
```

---

## Point de vigilance pour l'implémentation (P3)

1. **Migration unique (Flyway, côté Java)** doit créer les colonnes de `Alerte` et `SeuilDynamique` en une seule fois, avec les deux "zones" de colonnes (Java vs Python) documentées en commentaire SQL dans la migration elle-même.
2. **Idempotence** : pour `Alerte`, si Python retente un `INSERT` suite à une reconnexion PLC ou un NOTIFY manqué, il faut une contrainte d'unicité (ex : sur `mesure_id` + `type_alerte` + fenêtre de temps) pour éviter les doublons — déjà noté dans ton doc de priorités P3 ("gestion de l'idempotence").
3. Toutes les autres entités (✅) peuvent recevoir un `GRANT SELECT` simple pour le service lecteur — pas de complexité colonne par colonne à prévoir.
4. **`Notification` a été scindée en deux tables** (voir migration V15/V16) : `notification` porte le contenu du message (titre, contenu, une fois par événement), `envoi_notification` porte la diffusion par destinataire/canal (évite la duplication de titre/contenu sur chaque canal envoyé). Les deux restent 100% propriété Java, aucun changement de GRANT pour Python.

---

## Point ouvert à trancher

`ConversationChatbot` : le MCD ne précise pas explicitement quel service écrit la ligne (question + réponse). Deux options possibles :
- **Python écrit directement** (le service RAG a déjà la question et vient de générer la réponse) → le plus simple, retenu ci-dessus par défaut
- **Java écrit après avoir relayé la réponse de Python** → cohérent avec "Java = point d'entrée unique", mais duplique un aller-retour inutile pour une simple écriture

À confirmer selon ce que tu préfères implémenter en P3.
