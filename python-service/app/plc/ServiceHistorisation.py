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
from sqlalchemy import select, and_, func, update
from sqlalchemy.dialects.postgresql import insert

from app.core.config import settings
from app.plc.models import Mesure, ConfigurationPLC, PointMesure, Metrique, get_active_plc_config
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

        # Cache pour id_point_mesure (nom -> id) chargé au démarrage
        self._point_mesure_cache: dict[str, int] = {}
        self._seuils_manquants_logges: set[str] = set()  # Pour éviter les warnings répétés

    async def demarrer(self) -> None:
        """
        Démarre les tâches de polling, recalcul des seuils dynamiques et écoute des notifications.
        """
        if self._running:
            logger.warning("Service déjà en cours d'exécution")
            return

        logger.info("Démarrage du service d'historisation PLC")
        self._running = True

        # Charger le cache des points de mesure
        await self._charger_point_mesure_cache()

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

    async def _charger_point_mesure_cache(self) -> None:
        """
        Charge le cache des points de mesure actifs depuis la base de données.
        """
        async with self.session_factory() as session:
            result = await session.execute(
                select(PointMesure).where(
                    and_(
                        PointMesure.actif == True,
                        PointMesure.deleted_at.is_(None),
                    )
                )
            )
            points = result.scalars().all()

            self._point_mesure_cache = {p.nom: p.id for p in points}
            logger.info(f"Cache chargé avec {len(self._point_mesure_cache)} points de mesure actifs")

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

                # Extraction des mesures (retourne une liste)
                mesures = extraire_mesures(buffer)

                # Écriture en base avec calcul des seuils pour chaque mesure
                async with self.session_factory() as session:
                    for mesure_data in mesures:
                        try:
                            await self._traiter_mesure(session, mesure_data)
                        except Exception as e:
                            logger.error(
                                f"Erreur lors du traitement de la mesure "
                                f"{mesure_data['nom_point_mesure']} - {mesure_data['metrique']}: {e}"
                            )
                            # Continuer avec les autres mesures
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

    async def _traiter_mesure(
        self,
        session: AsyncSession,
        mesure_data: dict,
    ) -> None:
        """
        Traite une mesure individuelle : insertion en base et vérification des seuils.

        Args:
            session: Session SQLAlchemy async
            mesure_data: Dictionnaire contenant nom_point_mesure, metrique, valeur, plausible
        """
        nom_point_mesure = mesure_data["nom_point_mesure"]
        metrique_str = mesure_data["metrique"]
        valeur = mesure_data["valeur"]
        plausible = mesure_data["plausible"]

        # Résoudre l'id_point_mesure depuis le cache
        id_point_mesure = self._point_mesure_cache.get(nom_point_mesure)
        if id_point_mesure is None:
            logger.error(f"Point de mesure introuvable dans le cache: {nom_point_mesure}")
            return

        # Convertir la métrique string en enum
        metrique = Metrique(metrique_str)

        # Insertion de la mesure
        timestamp = datetime.now()
        mesure = Mesure(
            id_point_mesure=id_point_mesure,
            metrique=metrique,
            valeur=valeur,
            plausible=plausible,
            created_at=timestamp,
        )
        session.add(mesure)
        await session.flush()  # Pour obtenir l'UUID

        # Si non plausible, pas de vérification de seuils
        if not plausible:
            logger.warning(
                f"Mesure non plausible ignorée pour seuils: "
                f"{nom_point_mesure} - {metrique} = {valeur}"
            )
            return

        # Vérification des seuils absolu et dynamique
        await self._verifier_seuil_absolu(
            session,
            mesure.id_mesure,
            id_point_mesure,
            metrique,
            valeur,
        )
        await self._verifier_seuil_dynamique(
            session,
            mesure.id_mesure,
            id_point_mesure,
            metrique,
            valeur,
        )

    async def _verifier_seuil_absolu(
        self,
        session: AsyncSession,
        id_mesure,
        id_point_mesure: int,
        metrique: Metrique,
        valeur: float,
    ) -> None:
        """
        Vérifie si la valeur dépasse le seuil absolu pour ce point de mesure.

        Args:
            session: Session SQLAlchemy async
            id_mesure: UUID de la mesure associée
            id_point_mesure: ID du point de mesure
            metrique: Type de métrique
            valeur: Valeur mesurée
        """
        # Clé unique pour éviter les warnings répétés
        seuil_key = f"absolu_{id_point_mesure}_{metrique.value}"

        result = await session.execute(
            select(SeuilAbsolu).where(
                and_(
                    SeuilAbsolu.id_point_mesure == id_point_mesure,
                    SeuilAbsolu.metrique == metrique,
                    SeuilAbsolu.actif == True,
                )
            )
        )
        seuil = result.scalar_one_or_none()

        if seuil is None:
            if seuil_key not in self._seuils_manquants_logges:
                logger.warning(
                    f"Aucun seuil absolu configuré pour {metrique.value} "
                    f"au point de mesure {id_point_mesure}"
                )
                self._seuils_manquants_logges.add(seuil_key)
            return

        if valeur < seuil.valeur_min or valeur > seuil.valeur_max:
            alerte = Alerte(
                id_mesure=id_mesure,
                metrique=metrique,
                type_alerte=TypeAlerte.SEUIL_ABSOLU,
                severite=Severite.CRITIQUE,
            )
            session.add(alerte)
            await session.flush()  # Génère l'UUID avant le NOTIFY
            await self._notifier_nouvelle_alerte(alerte.id_alerte)

    async def _verifier_seuil_dynamique(
        self,
        session: AsyncSession,
        id_mesure,
        id_point_mesure: int,
        metrique: Metrique,
        valeur: float,
    ) -> None:
        """
        Vérifie si la valeur dépasse le seuil dynamique pour ce point de mesure.

        Args:
            session: Session SQLAlchemy async
            id_mesure: UUID de la mesure associée
            id_point_mesure: ID du point de mesure
            metrique: Type de métrique
            valeur: Valeur mesurée
        """
        # Clé unique pour éviter les warnings répétés
        seuil_key = f"dynamique_{id_point_mesure}_{metrique.value}"

        result = await session.execute(
            select(SeuilDynamique).where(
                and_(
                    SeuilDynamique.id_point_mesure == id_point_mesure,
                    SeuilDynamique.metrique == metrique,
                    SeuilDynamique.deleted_at.is_(None),
                    SeuilDynamique.valeur_min_calculee.isnot(None),
                    SeuilDynamique.valeur_max_calculee.isnot(None),
                )
            )
        )
        seuil = result.scalar_one_or_none()

        if seuil is None:
            if seuil_key not in self._seuils_manquants_logges:
                logger.warning(
                    f"Aucun seuil dynamique configuré pour {metrique.value} "
                    f"au point de mesure {id_point_mesure}"
                )
                self._seuils_manquants_logges.add(seuil_key)
            return

        if valeur < seuil.valeur_min_calculee or valeur > seuil.valeur_max_calculee:
            alerte = Alerte(
                id_mesure=id_mesure,
                metrique=metrique,
                type_alerte=TypeAlerte.SEUIL_DYNAMIQUE,
                severite=Severite.MOYENNE,
            )
            session.add(alerte)
            await session.flush()  # Génère l'UUID avant le NOTIFY
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
        Recalcule les seuils dynamiques pour chaque point de mesure actif et chaque métrique applicable.
        """
        async with self.session_factory() as session:
            # Récupérer tous les points de mesure actifs
            result = await session.execute(
                select(PointMesure).where(
                    and_(
                        PointMesure.actif == True,
                        PointMesure.deleted_at.is_(None),
                    )
                )
            )
            points = result.scalars().all()

            for point in points:
                # Déterminer les métriques applicables à ce point de mesure
                # La cabine a température + humidité, les zones d'étuve ont uniquement température
                if point.type_emplacement == "CABINE":
                    metriques = [Metrique.TEMPERATURE, Metrique.HUMIDITE]
                else:  # ETUVE
                    metriques = [Metrique.TEMPERATURE]

                for metrique in metriques:
                    try:
                        await self._recalculer_seuil_dynamique(session, point.id, metrique)
                    except Exception as e:
                        logger.error(
                            f"Erreur lors du recalcul du seuil dynamique pour "
                            f"{point.nom} - {metrique}: {e}"
                        )
                        # Continuer avec les autres points/métriques

    async def _recalculer_seuil_dynamique(
        self,
        session: AsyncSession,
        id_point_mesure: int,
        metrique: Metrique,
    ) -> None:
        """
        Recalcule le seuil dynamique pour un point de mesure et une métrique donnés.

        Args:
            session: Session SQLAlchemy async
            id_point_mesure: ID du point de mesure
            metrique: Métrique à recalculer
        """
        # Clé unique pour éviter les warnings répétés
        seuil_key = f"dynamique_{id_point_mesure}_{metrique.value}"

        # Récupérer la configuration du seuil dynamique pour ce point de mesure
        result = await session.execute(
            select(SeuilDynamique).where(
                and_(
                    SeuilDynamique.id_point_mesure == id_point_mesure,
                    SeuilDynamique.metrique == metrique,
                    SeuilDynamique.deleted_at.is_(None),
                )
            )
        )
        config = result.scalar_one_or_none()

        if not config:
            if seuil_key not in self._seuils_manquants_logges:
                logger.warning(
                    f"Aucune configuration de seuil dynamique pour {metrique.value} "
                    f"au point de mesure {id_point_mesure}"
                )
                self._seuils_manquants_logges.add(seuil_key)
            return

        # Compter les mesures plausibles dans la fenêtre pour CE point de mesure
        result_count = await session.execute(
            select(func.count())
            .select_from(Mesure)
            .where(
                and_(
                    Mesure.id_point_mesure == id_point_mesure,
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
                f"Recalcul ignoré pour {metrique.value} au point {id_point_mesure}: "
                f"{nb_mesures}/{NB_MESURES_MIN_RECALCUL} mesures plausibles"
            )
            return

        # Calculer la moyenne sur la fenêtre pour CE point de mesure
        result_avg = await session.execute(
            select(func.avg(Mesure.valeur))
            .select_from(Mesure)
            .where(
                and_(
                    Mesure.id_point_mesure == id_point_mesure,
                    Mesure.metrique == metrique,
                    Mesure.plausible == True,
                    Mesure.created_at >= datetime.now() -
                    func.text(f"INTERVAL '{FENETRE_MOYENNE_MOBILE_HEURES} hours'"),
                )
            )
        )
        moyenne = result_avg.scalar()

        if moyenne is None:
            logger.warning(f"Moyenne None pour {metrique.value} au point {id_point_mesure}")
            return

        # Appliquer la marge configurée
        marge = config.marge_configuree
        valeur_min_calculee = float(moyenne) - float(marge)
        valeur_max_calculee = float(moyenne) + float(marge)

        # UPDATE ciblé sur les 3 colonnes calculées par Python uniquement.
        # Ne jamais faire session.add() ici : la contrainte UNIQUE(id_point_mesure, metrique)
        # ajoutée en V31 interdit toute nouvelle ligne — seul un UPDATE est autorisé.
        await session.execute(
            update(SeuilDynamique)
            .where(SeuilDynamique.id_seuil_dynamique == config.id_seuil_dynamique)
            .values(
                valeur_min_calculee=valeur_min_calculee,
                valeur_max_calculee=valeur_max_calculee,
                date_calcul=datetime.now(),
            )
        )

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
            conn = await asyncpg.connect(db_url)
            try:
                await conn.execute(f"NOTIFY nouvelle_alerte, '{id_alerte}'")
                logger.info(f"NOTIFY envoyé pour alerte {id_alerte}")
            finally:
                await conn.close()
        except Exception as e:
            # On ne veut pas échouer la transaction si le NOTIFY échoue
            logger.error(f"Erreur lors du NOTIFY nouvelle_alerte: {e}")
