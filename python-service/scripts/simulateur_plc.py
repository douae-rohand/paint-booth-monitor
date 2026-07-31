import random
import time

from snap7.server import Server
from snap7.util import set_real
from snap7.type import SrvArea

# Crée un buffer de 28 octets (7 REALs × 4 octets)
# Correspond à READ_SIZE=28 dans constants.py
db_data = bytearray(28)

# Valeurs initiales au format REAL (4 octets chacune)
# Offsets selon constants.py/OFFSETS
# Valeurs ajustées pour être dans les plages de seuils absolus
set_real(db_data, 0, 72.5)   # Cabine d'après - température (seuil: 50-95)
set_real(db_data, 4, 55.0)   # Cabine d'après - humidité (seuil: 40-70)
set_real(db_data, 8, 140.0)  # Étuve - Zone 1 - température (seuil: 120-160)
set_real(db_data, 12, 142.0) # Étuve - Zone 2 - température (seuil: 120-160)
set_real(db_data, 16, 145.0) # Étuve - Zone 3 - température (seuil: 120-160)
set_real(db_data, 20, 143.0) # Étuve - Zone 4 - température (seuil: 120-160)
set_real(db_data, 24, 141.0) # Étuve - Zone 5 - température (seuil: 120-160)

server = Server()
# C'EST ICI que le "DB" est configuré : on enregistre db_data
# comme étant le Data Block numéro 1 (cohérent avec DB_NUMBER=1)
server.register_area(SrvArea.DB, 1, db_data)

server.start(tcp_port=1102)  # port alternatif pour éviter restrictions admin

print("Simulateur PLC démarré sur 127.0.0.1:1102, DB1")
print("Ctrl+C pour arrêter")

try:
    # Valeurs initiales pour simulation (dans les plages de seuils absolus)
    temp_cabine = 72.5   # Seuil absolu: 50-95
    humid_cabine = 55.0   # Seuil absolu: 40-70
    temp_etuve1 = 140.0   # Seuil absolu: 120-160
    temp_etuve2 = 142.0   # Seuil absolu: 120-160
    temp_etuve3 = 145.0   # Seuil absolu: 120-160
    temp_etuve4 = 143.0   # Seuil absolu: 120-160
    temp_etuve5 = 141.0   # Seuil absolu: 120-160

    while True:
        # Fait dériver les températures lentement pour simuler une vraie tendance
        temp_cabine += random.uniform(-0.3, 0.5)
        humid_cabine += random.uniform(-1.0, 1.0)
        temp_etuve1 += random.uniform(-0.5, 0.8)
        temp_etuve2 += random.uniform(-0.5, 0.8)
        temp_etuve3 += random.uniform(-0.5, 0.8)
        temp_etuve4 += random.uniform(-0.5, 0.8)
        temp_etuve5 += random.uniform(-0.5, 0.8)

        # Occasionnellement (5% de chance), générer une valeur hors seuil pour tester les alertes
        if random.random() < 0.05:
            # Choisir aléatoirement quel capteur dépasse
            which = random.choice(['temp_cabine', 'humid_cabine', 'temp_etuve'])
            if which == 'temp_cabine':
                temp_cabine = random.choice([48.0, 97.0])  # Hors seuil 50-95
            elif which == 'humid_cabine':
                humid_cabine = random.choice([38.0, 72.0])  # Hors seuil 40-70
            else:
                # Une des zones étuve
                zone = random.choice([1, 2, 3, 4, 5])
                if zone == 1:
                    temp_etuve1 = random.choice([118.0, 162.0])  # Hors seuil 120-160
                elif zone == 2:
                    temp_etuve2 = random.choice([118.0, 162.0])
                elif zone == 3:
                    temp_etuve3 = random.choice([118.0, 162.0])
                elif zone == 4:
                    temp_etuve4 = random.choice([118.0, 162.0])
                else:
                    temp_etuve5 = random.choice([118.0, 162.0])

        # Clamp pour rester dans les plages de seuils absolus ET plausibilité
        # Cabine température: seuil 50-95, plausibilité -20 à 250
        temp_cabine = max(50.0, min(95.0, temp_cabine)) if random.random() >= 0.05 else max(-20.0, min(250.0, temp_cabine))
        # Cabine humidité: seuil 40-70, plausibilité 0 à 100
        humid_cabine = max(40.0, min(70.0, humid_cabine)) if random.random() >= 0.05 else max(0.0, min(100.0, humid_cabine))
        # Étuve température: seuil 120-160, plausibilité -20 à 250
        temp_etuve1 = max(120.0, min(160.0, temp_etuve1)) if random.random() >= 0.05 else max(-20.0, min(250.0, temp_etuve1))
        temp_etuve2 = max(120.0, min(160.0, temp_etuve2)) if random.random() >= 0.05 else max(-20.0, min(250.0, temp_etuve2))
        temp_etuve3 = max(120.0, min(160.0, temp_etuve3)) if random.random() >= 0.05 else max(-20.0, min(250.0, temp_etuve3))
        temp_etuve4 = max(120.0, min(160.0, temp_etuve4)) if random.random() >= 0.05 else max(-20.0, min(250.0, temp_etuve4))
        temp_etuve5 = max(120.0, min(160.0, temp_etuve5)) if random.random() >= 0.05 else max(-20.0, min(250.0, temp_etuve5))

        # Mettre à jour le buffer
        set_real(db_data, 0, temp_cabine)
        set_real(db_data, 4, humid_cabine)
        set_real(db_data, 8, temp_etuve1)
        set_real(db_data, 12, temp_etuve2)
        set_real(db_data, 16, temp_etuve3)
        set_real(db_data, 20, temp_etuve4)
        set_real(db_data, 24, temp_etuve5)

        print(f"Cabine: {temp_cabine:.1f}°C, {humid_cabine:.1f}% | "
              f"Étuve: [{temp_etuve1:.1f}, {temp_etuve2:.1f}, {temp_etuve3:.1f}, "
              f"{temp_etuve4:.1f}, {temp_etuve5:.1f}]°C")
        time.sleep(10)
except KeyboardInterrupt:
    server.stop()
    server.destroy()