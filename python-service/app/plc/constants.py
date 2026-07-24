"""
Module: plc
Constantes de configuration pour la communication PLC et l'extraction des données.

IMPORTANT: Les offsets DB (OFFSET_TEMP, OFFSET_HUMID) dépendent de la structure
du Data Block définie dans TIA Portal. Tout changement de structure du DB côté
automatisme doit être coordonné avec le binôme automatisme pour mettre à jour
ces constantes en conséquence.
"""

# ── Configuration Data Block PLC ────────────────────────────────────────────────

DB_NUMBER = 1
START_OFFSET = 0
READ_SIZE = 8
OFFSET_TEMP = 0
OFFSET_HUMID = 4

# ── Plages de plausibilité physique ──────────────────────────────────────────────

# Ces plages sont larges et fixes, distinctes des seuils qualité configurés par l'Admin.
# Elles servent uniquement à filtrer les valeurs physiquement impossibles (ex: capteur
# déconnecté, erreur de lecture, corruption de données) avant toute vérification de seuil.

TEMP_PLAUSIBLE_MIN = -20.0
TEMP_PLAUSIBLE_MAX = 200.0

HUMID_PLAUSIBLE_MIN = 0.0
HUMID_PLAUSIBLE_MAX = 100.0

# ── Configuration du recalcul des seuils dynamiques ────────────────────────────────

# Fenêtre en DURÉE (heures) pour le calcul de la moyenne mobile.
# Choix justifié : une fenêtre en durée reste stable indépendamment de l'intervalle
# de polling configuré par l'Admin, et absorbe naturellement les trous causés par
# une coupure réseau.
FENETRE_MOYENNE_MOBILE_HEURES = 2

# Nombre minimum de mesures plausibles requises dans la fenêtre avant recalcul.
# Si insuffisant, le recalcul est ignoré et la dernière valeur valide est conservée.
# Cas typiques : démarrage du service, sortie d'une longue coupure réseau.
NB_MESURES_MIN_RECALCUL = 10
