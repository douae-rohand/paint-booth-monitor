import random
import time

from snap7.server import Server
from snap7.util import set_real
from snap7.type import SrvArea

# Crée un buffer de 8 octets, comme votre READ_SIZE réel
db_data = bytearray(8)

# Valeurs initiales au format REAL (4 octets chacune)
set_real(db_data, 0, 75.0)   # OFFSET_TEMP = 0
set_real(db_data, 4, 60.0)   # OFFSET_HUMID = 4

server = Server()
# C'EST ICI que le "DB" est configuré : on enregistre db_data
# comme étant le Data Block numéro 1 (cohérent avec DB_NUMBER=1)
server.register_area(SrvArea.DB, 1, db_data)

server.start(tcp_port=102)  # port standard S7

print("Simulateur PLC démarré sur 127.0.0.1:102, DB1")
print("Ctrl+C pour arrêter")

try:
    temp = 75.0
    while True:
        # Fait dériver la température lentement, pour simuler une vraie tendance
        temp += random.uniform(-0.5, 1.5)
        set_real(db_data, 0, temp)
        print(f"Température simulée : {temp:.1f}°C")
        time.sleep(10)
except KeyboardInterrupt:
    server.stop()
    server.destroy()