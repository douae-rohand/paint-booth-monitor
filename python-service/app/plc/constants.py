"""
Module: plc
Constantes de configuration pour la communication PLC et l'extraction des données.

IMPORTANT: Les offsets DB (OFFSETS) dépendent de la structure
du Data Block définie dans TIA Portal. Tout changement de structure du DB côté
automatisme doit être coordonné avec le binôme automatisme pour mettre à jour
ces constantes en conséquence.
"""

# ── Configuration Data Block PLC ────────────────────────────────────────────────

DB_NUMBER = 1
START_OFFSET = 0

# Mapping des offsets par point de mesure physique
# Chaque REAL fait 4 octets. La cabine a température + humidité, les zones d'étuve ont uniquement température.
OFFSETS = {
    "Cabine d'après":   {"temperature": 0,  "humidite": 4},
    "Étuve - Zone 1":    {"temperature": 8},
    "Étuve - Zone 2":    {"temperature": 12},
    "Étuve - Zone 3":    {"temperature": 16},
    "Étuve - Zone 4":    {"temperature": 20},
    "Étuve - Zone 5":    {"temperature": 24},
}

# Taille totale à lire : 6 valeurs température + 1 humidité = 7 REALs × 4 octets = 28 octets
READ_SIZE = 28

# ── Plages de plausibilité physique ──────────────────────────────────────────────

# Ces plages sont larges et fixes, distinctes des seuils qualité configurés par l'Admin.
# Elles servent uniquement à filtrer les valeurs physiquement impossibles (ex: capteur
# déconnecté, erreur de lecture, corruption de données) avant toute vérification de seuil.
# La plausibilité détecte uniquement une impossibilité physique du capteur (offset mal aligné,
# câblage, panne), pas une valeur anormale pour un process donné — ce dernier rôle
# appartient exclusivement au SeuilAbsolu, configuré par l'Admin, par point de mesure.

PLAUSIBILITE = {
    "temperature": (-20.0, 250.0),  # large, couvre cabine et étuve
    "humidite": (0.0, 100.0),
}

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
