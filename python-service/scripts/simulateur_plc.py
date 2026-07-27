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
set_real(db_data, 0, 22.0)   # Cabine d'après - température
set_real(db_data, 4, 65.0)   # Cabine d'après - humidité
set_real(db_data, 8, 80.0)   # Étuve - Zone 1 - température
set_real(db_data, 12, 82.0)  # Étuve - Zone 2 - température
set_real(db_data, 16, 85.0)  # Étuve - Zone 3 - température
set_real(db_data, 20, 83.0)  # Étuve - Zone 4 - température
set_real(db_data, 24, 81.0)  # Étuve - Zone 5 - température

server = Server()
# C'EST ICI que le "DB" est configuré : on enregistre db_data
# comme étant le Data Block numéro 1 (cohérent avec DB_NUMBER=1)
server.register_area(SrvArea.DB, 1, db_data)

server.start(tcp_port=1102)  # port alternatif pour éviter restrictions admin

print("Simulateur PLC démarré sur 127.0.0.1:1102, DB1")
print("Ctrl+C pour arrêter")

try:
    # Valeurs initiales pour simulation
    temp_cabine = 22.0
    humid_cabine = 65.0
    temp_etuve1 = 80.0
    temp_etuve2 = 82.0
    temp_etuve3 = 85.0
    temp_etuve4 = 83.0
    temp_etuve5 = 81.0

    while True:
        # Fait dériver les températures lentement pour simuler une vraie tendance
        temp_cabine += random.uniform(-0.3, 0.5)
        humid_cabine += random.uniform(-1.0, 1.0)
        temp_etuve1 += random.uniform(-0.5, 0.8)
        temp_etuve2 += random.uniform(-0.5, 0.8)
        temp_etuve3 += random.uniform(-0.5, 0.8)
        temp_etuve4 += random.uniform(-0.5, 0.8)
        temp_etuve5 += random.uniform(-0.5, 0.8)

        # Clamp pour rester dans des plages réalistes
        humid_cabine = max(0.0, min(100.0, humid_cabine))

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