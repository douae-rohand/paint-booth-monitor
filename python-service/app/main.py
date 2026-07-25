import asyncio
import logging
import contextlib

from fastapi import FastAPI

from app.core.database import SessionLocal
from app.plc.models import get_active_plc_config
from app.plc.ServiceHistorisation import ServiceHistorisation
from app.plc.connecteurs.snap7_connecteur import ConnecteurSnap7

logger = logging.getLogger(__name__)


@contextlib.asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Gestion du cycle de vie du service Python :
    - Startup: lecture configuration PLC, instanciation connecteur, démarrage polling
    - Shutdown: arrêt propre des tâches asyncio
    """
    service_historisation = None
    
    try:
        # Startup
        logger.info("Démarrage du service Python...")
        
        # Lecture de la configuration PLC active
        async with SessionLocal() as session:
            config_plc = await get_active_plc_config(session)
            
            if config_plc is None:
                logger.warning(
                    "Aucune configuration PLC active trouvée. "
                    "Le service de polling ne démarrera pas. "
                    "Veuillez configurer une ConfigurationPLC via l'interface Admin."
                )
                yield
                return
            
            logger.info(
                f"Configuration PLC trouvée: IP={config_plc.plc_ip}, "
                f"port={config_plc.plc_port}, rack={config_plc.plc_rack}, slot={config_plc.plc_slot}, "
                f"intervalle={config_plc.plc_polling_interval_ms}ms"
            )
        
        # Instanciation du connecteur Snap7
        connecteur = ConnecteurSnap7(
            ip=config_plc.plc_ip,
            rack=config_plc.plc_rack,
            slot=config_plc.plc_slot,
            port=config_plc.plc_port,
        )
        
        # Instanciation du service d'historisation
        service_historisation = ServiceHistorisation(
            connecteur=connecteur,
            session_factory=SessionLocal,
            polling_interval_ms=config_plc.plc_polling_interval_ms,
        )
        
        # Démarrage des tâches de polling et recalcul
        await service_historisation.demarrer()
        
        # Stocker le service dans l'état de l'app pour accès éventuel
        app.state.service_historisation = service_historisation
        
        logger.info("Service Python démarré avec succès")
        
        yield
        
    except Exception as e:
        logger.error(f"Erreur lors du démarrage du service: {e}")
        raise
    finally:
        # Shutdown
        logger.info("Arrêt du service Python...")
        
        if service_historisation:
            await service_historisation.arreter()
        
        logger.info("Service Python arrêté proprement")


app = FastAPI(
    title="Python Data & Intelligence Service",
    description="Service for PLC communication, AI predictions, alerts and RAG chatbot features",
    lifespan=lifespan,
)


@app.get("/")
def read_root():
    return {"status": "ok", "service": "python-service"}


@app.get("/health")
def health_check():
    """
    Endpoint de health check simple.
    """
    return {"status": "healthy", "service": "python-service"}
