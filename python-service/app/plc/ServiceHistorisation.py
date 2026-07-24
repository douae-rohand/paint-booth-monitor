"""
Module: plc
Service d'orchestration pour la collecte et l'historisation des mesures PLC.

Ce module contient la logique de service principale pour :
- La boucle de polling des mesures depuis le PLC
- L'extraction et la validation des données
- L'écriture en base de données avec calcul des seuils
- Le recalcul périodique des seuils dynamiques
- La réception des notifications PostgreSQL pour reconfiguration dynamique du PLC
"""

import asyncio
import logging
from datetime import datetime
from typing import Optional

import asyncpg
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, func
from sqlalchemy.dialects.postgresql import insert

from app.core.config import settings
from app.plc.models import Mesure, ConfigurationPLC, Metrique, get_active_plc_config
from app.plc.IConnecteurPLC import IConnecteurPLC
from app.plc.connecteurs.snap7_connecteur import ConnecteurSnap7
from app.plc.constants import (
    DB_NUMBER,
    START_OFFSET,
    READ_SIZE,
    FENETRE_MOYENNE_MOBILE_HEURES,
    NB_MESURES_MIN_RECALCUL,
)
from app.plc.extracteur import extraire_mesures
from app.alerting.models import SeuilAbsolu, SeuilDynamique, Alerte, TypeAlerte, Severite

logger = logging.getLogger(__name__)


class ServiceHistorisation:
    """
    Service d'orchestration pour la collecte et l'historisation des mesures PLC.
    
    Ce service gère :
    - La connexion au PLC via un IConnecteurPLC injecté
    - La boucle de polling asyncio avec gestion des erreurs
    - L'écriture des mesures en base avec calcul des seuils
    - Le recalcul périodique des seuils dynamiques
    """

    def __init__(
        self,
        connecteur: IConnecteurPLC,
        session_factory,
        polling_interval_ms: int = 5000,
    ):
        """
        Initialise le service avec un connecteur PLC et une factory de session.
        
        Args:
            connecteur: Instance d'un connecteur implémentant IConnecteurPLC
            session_factory: Factory pour créer les sessions SQLAlchemy async
            polling_interval_ms: Intervalle de polling en millisecondes
        """
        self.connecteur = connecteur
        self.session_factory = session_factory
        self.polling_interval_ms = polling_interval_ms
        self._running = False
        self._polling_task: Optional[asyncio.Task] = None
        self._recalc_task: Optional[asyncio.Task] = None
        self._consecutive_errors = 0
        self._max_reconnect_delay = 60  # secondes
        
        # Lock pour protéger le remplacement du connecteur pendant le polling
        self._connecteur_lock = asyncio.Lock()
        
        # Connexion asyncpg dédiée pour LISTEN config_plc_change
        self._listen_conn: Optional[asyncpg.Connection] = None
        self._listen_task: Optional[asyncio.Task] = None
        self._listen_consecutive_errors = 0
        self._max_listen_reconnect_delay = 60  # secondes

    async def demarrer(self) -> None:
        """
        Démarre les tâches de polling, recalcul des seuils dynamiques et écoute des notifications.
        """
        if self._running:
            logger.warning("Service déjà en cours d'exécution")
            return

        logger.info("Démarrage du service d'historisation PLC")
        self._running = True

        # Tâche de polling des mesures
        self._polling_task = asyncio.create_task(self._boucle_polling())

        # Tâche de recalcul des seuils dynamiques (toutes les heures)
        self._recalc_task = asyncio.create_task(self._boucle_recalcul_seuils_dynamiques())

        # Tâche d'écoute des notifications PostgreSQL config_plc_change
        self._listen_task = asyncio.create_task(self._boucle_listen_config_change())

        logger.info("Tâches de polling, recalcul et écoute démarrées")

    async def arreter(self) -> None:
        """
        Arrête proprement toutes les tâches en cours.
        """
        if not self._running:
            return

        logger.info("Arrêt du service d'historisation PLC")
        self._running = False

        if self._polling_task:
            self._polling_task.cancel()
            try:
                await self._polling_task
            except asyncio.CancelledError:
                pass

        if self._recalc_task:
            self._recalc_task.cancel()
            try:
                await self._recalc_task
            except asyncio.CancelledError:
                pass

        if self._listen_task:
            self._listen_task.cancel()
            try:
                await self._listen_task
            except asyncio.CancelledError:
                pass

        # Fermer la connexion d'écoute
        await self._close_listen_connection()

        logger.info("Service arrêté proprement")

    async def _boucle_polling(self) -> None:
        """
        Boucle principale de polling des mesures depuis le PLC.
        """
        loop = asyncio.get_running_loop()

        while self._running:
            try:
                # Lecture synchrone via executor (Snap7 est bloquant)
                buffer = await loop.run_in_executor(
                    None,
                    self._lire_buffer_plc
                )

                # Extraction des mesures
                donnees = extraire_mesures(buffer)

                # Écriture en base avec calcul des seuils
                async with self.session_factory() as session:
                    await self._ecrire_mesure_avec_seuils(
                        session,
                        donnees["temperature"],
                        donnees["humidite"],
                        donnees["plausible"]
                    )
                    await session.commit()

                # Réinitialiser le compteur d'erreurs après succès
                if self._consecutive_errors > 0:
                    logger.info(f"Connexion rétablie après {self._consecutive_errors} échecs")
                    self._consecutive_errors = 0

            except Exception as e:
                self._consecutive_errors += 1
                logger.error(
                    f"Erreur lors du polling (tentative {self._consecutive_errors}): {e}"
                )

                # Gestion du backoff exponentiel
                delay = min(5 * (2 ** (self._consecutive_errors - 1)), self._max_reconnect_delay)
                
                if self._consecutive_errors >= 5:
                    logger.critical(
                        f"{self._consecutive_errors} échecs consécutifs - "
                        f"prochaine tentative dans {delay}s"
                    )

                await asyncio.sleep(delay)
                continue

            # Attendre l'intervalle de polling après traitement réussi
            await asyncio.sleep(self.polling_interval_ms / 1000)

    def _lire_buffer_plc(self) -> bytes:
        """
        Lit le buffer depuis le PLC via l'interface IConnecteurPLC (appel synchrone, exécuté dans executor).

        Returns:
            Buffer brut lu depuis le Data Block PLC
        """
        if not self.connecteur.is_connected():
            self.connecteur.connect()

        return self.connecteur.read_db(DB_NUMBER, START_OFFSET, READ_SIZE)

    async def _ecrire_mesure_avec_seuils(
        self,
        session: AsyncSession,
        temperature: float,
        humidite: float,
        plausible: bool,
    ) -> None:
        """
        Écrit la mesure en base et crée les alertes si nécessaire (transaction unique).
        
        Args:
            session: Session SQLAlchemy async
            temperature: Valeur de température mesurée
            humidite: Valeur d'humidité mesurée
            plausible: Flag indiquant si les valeurs sont physiquement plausibles
        """
        timestamp = datetime.now()

        # Insertion des mesures (une par métrique)
        mesure_temp = Mesure(
            metrique=Metrique.TEMPERATURE,
            valeur=temperature,
            plausible=plausible,
            created_at=timestamp,
        )
        mesure_humid = Mesure(
            metrique=Metrique.HUMIDITE,
            valeur=humidite,
            plausible=plausible,
            created_at=timestamp,
        )

        session.add(mesure_temp)
        session.add(mesure_humid)
        await session.flush()  # Pour obtenir les UUID

        # Si non plausible, pas de vérification de seuils
        if not plausible:
            logger.warning(f"Mesure non plausible ignorée pour seuils: T={temperature}°C, H={humidite}%")
            return

        # Vérification et création d'alertes pour chaque métrique
        await self._verifier_et_creer_alerte(
            session,
            mesure_temp.id_mesure,
            Metrique.TEMPERATURE,
            temperature,
            TypeAlerte.SEUIL_ABSOLU,
            Severite.CRITIQUE,
        )
        await self._verifier_et_creer_alerte(
            session,
            mesure_temp.id_mesure,
            Metrique.TEMPERATURE,
            temperature,
            TypeAlerte.SEUIL_DYNAMIQUE,
            Severite.MOYENNE,
        )
        await self._verifier_et_creer_alerte(
            session,
            mesure_humid.id_mesure,
            Metrique.HUMIDITE,
            humidite,
            TypeAlerte.SEUIL_ABSOLU,
            Severite.CRITIQUE,
        )
        await self._verifier_et_creer_alerte(
            session,
            mesure_humid.id_mesure,
            Metrique.HUMIDITE,
            humidite,
            TypeAlerte.SEUIL_DYNAMIQUE,
            Severite.MOYENNE,
        )

    async def _verifier_et_creer_alerte(
        self,
        session: AsyncSession,
        id_mesure,
        metrique: Metrique,
        valeur: float,
        type_alerte: TypeAlerte,
        severite: Severite,
    ) -> None:
        """
        Vérifie si la valeur dépasse le seuil spécifié et crée une alerte si nécessaire.
        
        Args:
            session: Session SQLAlchemy async
            id_mesure: UUID de la mesure associée
            metrique: Type de métrique (TEMPERATURE ou HUMIDITE)
            valeur: Valeur mesurée
            type_alerte: Type d'alerte (SEUIL_ABSOLU ou SEUIL_DYNAMIQUE)
            severite: Sévérité de l'alerte
        """
        if type_alerte == TypeAlerte.SEUIL_ABSOLU:
            # Récupérer le seuil absolu actif
            result = await session.execute(
                select(SeuilAbsolu).where(
                    and_(
                        SeuilAbsolu.metrique == metrique,
                        SeuilAbsolu.deleted_at.is_(None),
                    )
                )
            )
            seuil = result.scalar_one_or_none()

            if seuil and (valeur < seuil.valeur_min or valeur > seuil.valeur_max):
                alerte = Alerte(
                    id_mesure=id_mesure,
                    metrique=metrique,
                    type_alerte=type_alerte,
                    severite=severite,
                )
                session.add(alerte)
                await self._notifier_nouvelle_alerte(alerte.id_alerte)

        elif type_alerte == TypeAlerte.SEUIL_DYNAMIQUE:
            # Récupérer le dernier seuil dynamique calculé
            result = await session.execute(
                select(SeuilDynamique).where(
                    and_(
                        SeuilDynamique.metrique == metrique,
                        SeuilDynamique.deleted_at.is_(None),
                        SeuilDynamique.valeur_min_calculee.isnot(None),
                        SeuilDynamique.valeur_max_calculee.isnot(None),
                    )
                ).order_by(SeuilDynamique.date_calcul.desc())
            )
            seuil = result.scalar_one_or_none()

            if seuil and (
                valeur < seuil.valeur_min_calculee or valeur > seuil.valeur_max_calculee
            ):
                alerte = Alerte(
                    id_mesure=id_mesure,
                    metrique=metrique,
                    type_alerte=type_alerte,
                    severite=severite,
                )
                session.add(alerte)
                await self._notifier_nouvelle_alerte(alerte.id_alerte)

    async def _boucle_recalcul_seuils_dynamiques(self) -> None:
        """
        Boucle de recalcul périodique des seuils dynamiques (toutes les heures).
        """
        while self._running:
            try:
                await self._recalculer_seuils_dynamiques()
            except Exception as e:
                logger.error(f"Erreur lors du recalcul des seuils dynamiques: {e}")

            # Attendre 1 heure avant le prochain recalcul
            await asyncio.sleep(3600)

    async def _recalculer_seuils_dynamiques(self) -> None:
        """
        Recalcule les seuils dynamiques pour chaque métrique.
        """
        async with self.session_factory() as session:
            for metrique in [Metrique.TEMPERATURE, Metrique.HUMIDITE]:
                await self._recalculer_seuil_dynamique(session, metrique)

    async def _recalculer_seuil_dynamique(
        self,
        session: AsyncSession,
        metrique: Metrique,
    ) -> None:
        """
        Recalcule le seuil dynamique pour une métrique donnée.
        
        Args:
            session: Session SQLAlchemy async
            metrique: Métrique à recalculer
        """
        # Récupérer la configuration du seuil dynamique
        result = await session.execute(
            select(SeuilDynamique).where(
                and_(
                    SeuilDynamique.metrique == metrique,
                    SeuilDynamique.deleted_at.is_(None),
                )
            )
        )
        config = result.scalar_one_or_none()

        if not config:
            logger.warning(f"Aucune configuration de seuil dynamique pour {metrique}")
            return

        # Compter les mesures plausibles dans la fenêtre
        result_count = await session.execute(
            select(func.count())
            .select_from(Mesure)
            .where(
                and_(
                    Mesure.metrique == metrique,
                    Mesure.plausible == True,
                    Mesure.created_at >= datetime.now() - 
                    func.text(f"INTERVAL '{FENETRE_MOYENNE_MOBILE_HEURES} hours'"),
                )
            )
        )
        nb_mesures = result_count.scalar()

        if nb_mesures < NB_MESURES_MIN_RECALCUL:
            logger.info(
                f"Recalcul ignoré pour {metrique}: "
                f"{nb_mesures}/{NB_MESURES_MIN_RECALCUL} mesures plausibles"
            )
            return

        # Calculer la moyenne sur la fenêtre
        result_avg = await session.execute(
            select(func.avg(Mesure.valeur))
            .select_from(Mesure)
            .where(
                and_(
                    Mesure.metrique == metrique,
                    Mesure.plausible == True,
                    Mesure.created_at >= datetime.now() - 
                    func.text(f"INTERVAL '{FENETRE_MOYENNE_MOBILE_HEURES} hours'"),
                )
            )
        )
        moyenne = result_avg.scalar()

        if moyenne is None:
            logger.warning(f"Moyenne None pour {metrique}")
            return

        # Appliquer la marge configurée
        marge = config.marge_configuree
        valeur_min_calculee = float(moyenne) - float(marge)
        valeur_max_calculee = float(moyenne) + float(marge)

        # Insérer une nouvelle ligne (historisation)
        nouveau_seuil = SeuilDynamique(
            id_admin=config.id_admin,
            metrique=metrique,
            marge_configuree=marge,
            valeur_min_calculee=valeur_min_calculee,
            valeur_max_calculee=valeur_max_calculee,
            date_calcul=datetime.now(),
        )
        session.add(nouveau_seuil)

    async def _boucle_listen_config_change(self) -> None:
        """
        Boucle d'écoute des notifications PostgreSQL config_plc_change.
        """
        while self._running:
            try:
                await self._ensure_listen_connection()
                # La connexion asyncpg gère l'écoute en arrière-plan via add_listener
                # On attend simplement pour permettre la reconnexion en cas d'erreur
                await asyncio.sleep(60)
            except Exception as e:
                self._listen_consecutive_errors += 1
                logger.error(
                    f"Erreur lors de l'écoute config_plc_change (tentative {self._listen_consecutive_errors}): {e}"
                )
                
                if self._listen_consecutive_errors >= 5:
                    logger.critical(
                        f"{self._listen_consecutive_errors} échecs consécutifs - "
                        f"prochaine tentative dans {self._calculate_listen_reconnect_delay()}s"
                    )
                
                await self._close_listen_connection()
                
                # Backoff exponentiel
                delay = self._calculate_listen_reconnect_delay()
                await asyncio.sleep(delay)

    async def _ensure_listen_connection(self) -> None:
        """
        Établit ou maintient la connexion asyncpg dédiée pour LISTEN.
        """
        if self._listen_conn is None or self._listen_conn.is_closed():
            logger.info("Tentative de connexion asyncpg dédiée pour LISTEN config_plc_change")
            
            # Extraire l'URL de connexion depuis settings.DATABASE_URL
            # Convertir postgresql+asyncpg:// en postgresql:// pour asyncpg direct
            db_url = settings.DATABASE_URL.replace("postgresql+asyncpg://", "postgresql://")
            
            self._listen_conn = await asyncpg.connect(db_url)
            
            # Ajouter le listener pour le canal config_plc_change
            await self._listen_conn.add_listener(
                "config_plc_change",
                self._config_change_callback
            )
            
            await self._listen_conn.execute("LISTEN config_plc_change")
            
            logger.info("Connexion asyncpg dédiée établie et LISTEN config_plc_change activé")
            
            # Réinitialiser le compteur d'erreurs après succès
            if self._listen_consecutive_errors > 0:
                logger.info(f"Connexion LISTEN rétablie après {self._listen_consecutive_errors} échecs")
                self._listen_consecutive_errors = 0

    async def _config_change_callback(
        self,
        connection: asyncpg.Connection,
        pid: int,
        channel: str,
        payload: str
    ) -> None:
        """
        Callback appelé lors de la réception d'une notification config_plc_change.
        """
        logger.info(f"Notification reçue sur canal '{channel}' avec payload: '{payload}'")
        
        if channel == "config_plc_change":
            # Ignorer le payload et relire la config active (source de vérité unique)
            await self._reconfigurer_plc()

    async def _reconfigurer_plc(self) -> None:
        """
        Reconfigure le connecteur PLC avec la nouvelle configuration active.
        """
        logger.info("Réception notification config_plc_change, reconfiguration du PLC...")
        
        async with self.session_factory() as session:
            config_plc = await get_active_plc_config(session)
            
            if config_plc is None:
                logger.warning("Aucune configuration PLC active trouvée après notification")
                return
        
        # Protéger le remplacement du connecteur avec un lock
        async with self._connecteur_lock:
            old_config = f"{self.connecteur.ip}:{self.connecteur.port}" if hasattr(self.connecteur, 'port') else self.connecteur.ip
            
            logger.info(
                f"Ancienne config: {old_config}, "
                f"Nouvelle config: {config_plc.plc_ip}:{config_plc.plc_port}"
            )
            
            try:
                # Déconnecter l'ancien connecteur
                self.connecteur.disconnect()
                logger.info("Ancien connecteur déconnecté")
                
                # Créer et connecter le nouveau connecteur
                nouveau_connecteur = ConnecteurSnap7(
                    ip=config_plc.plc_ip,
                    rack=config_plc.plc_rack,
                    slot=config_plc.plc_slot,
                    port=config_plc.plc_port,
                )
                nouveau_connecteur.connect()
                
                # Remplacer le connecteur
                self.connecteur = nouveau_connecteur
                
                logger.info(
                    f"Reconfiguration PLC réussie: "
                    f"{config_plc.plc_ip}:{config_plc.plc_port}, "
                    f"rack={config_plc.plc_rack}, slot={config_plc.plc_slot}"
                )
                
            except Exception as e:
                logger.error(f"Échec de la reconfiguration PLC: {e}")
                # En cas d'échec, la boucle de polling principale tentera de se reconnecter
                # avec la logique de retry existante

    async def _close_listen_connection(self) -> None:
        """
        Ferme proprement la connexion asyncpg dédiée.
        """
        if self._listen_conn is not None:
            try:
                if not self._listen_conn.is_closed():
                    await self._listen_conn.close()
                logger.info("Connexion asyncpg dédiée fermée")
            except Exception as e:
                logger.warn(f"Erreur lors de la fermeture de la connexion asyncpg: {e}")
            finally:
                self._listen_conn = None

    def _calculate_listen_reconnect_delay(self) -> int:
        """
        Calcule le délai de reconnexion avec backoff exponentiel.
        """
        if self._listen_consecutive_errors == 1:
            return 0  # Tentative immédiate
        delay = 5 * (2 ** (self._listen_consecutive_errors - 2))
        return min(delay, self._max_listen_reconnect_delay)

    async def _notifier_nouvelle_alerte(self, id_alerte) -> None:
        """
        Envoie une notification PostgreSQL sur le canal nouvelle_alerte
        pour informer le service Java de dispatcher l'alerte.

        Args:
            id_alerte: UUID de l'alerte créée
        """
        try:
            # Utiliser une connexion temporaire pour le NOTIFY
            db_url = settings.DATABASE_URL.replace("postgresql+asyncpg://", "postgresql://")
            async with asyncpg.connect(db_url) as conn:
                await conn.execute(f"NOTIFY nouvelle_alerte, '{id_alerte}'")
            logger.info(f"NOTIFY envoyé pour alerte {id_alerte}")
        except Exception as e:
            # On ne veut pas échouer la transaction si le NOTIFY échoue
            logger.error(f"Erreur lors du NOTIFY nouvelle_alerte: {e}")
