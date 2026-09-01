import os
from pathlib import Path

from dotenv import load_dotenv

# Charger le .env racine s'il existe (variables partagées)
_ROOT_ENV_PATH = Path(__file__).resolve().parents[3] / ".env"
load_dotenv(_ROOT_ENV_PATH)

# Surcharger avec le .env du python-service s'il existe (overrides locaux)
_ENV_PATH = Path(__file__).resolve().parents[2] / ".env"
load_dotenv(_ENV_PATH, override=True)


def _require_env(name: str) -> str:
    """
    Lit une variable d'environnement obligatoire.
    Lève une erreur explicite au démarrage si absente (fail-fast).
    """
    value = os.getenv(name)
    if not value:
        raise EnvironmentError(
            f"[CONFIG] Variable d'environnement obligatoire manquante : {name!r}. "
            f"Vérifiez votre fichier .env ou les variables d'environnement du conteneur."
        )
    return value


def _build_database_url() -> str:
    """
    Construit DATABASE_URL depuis ses composants séparés.

    En Docker : composants injectés par docker-compose.yml depuis le .env racine.
    En dev local : composants lus depuis python-service/.env.

    Une seule source de vérité : reconstruction depuis composants séparés.
    """
    user     = _require_env("PYTHON_SERVICE_DB_USER")
    password = _require_env("PYTHON_SERVICE_DB_PASSWORD")
    host     = _require_env("POSTGRES_HOST")
    port     = _require_env("POSTGRES_PORT")
    db       = _require_env("POSTGRES_DB")

    return f"postgresql+asyncpg://{user}:{password}@{host}:{port}/{db}"


class Settings:
    DATABASE_URL: str = _build_database_url()

    DATABASE_ECHO: bool = os.getenv("DATABASE_ECHO", "false").lower() == "true"
    DATABASE_POOL_SIZE: int = int(os.getenv("DATABASE_POOL_SIZE", "5"))

    PLC_IP: str = os.getenv("PLC_IP", "127.0.0.1")
    PLC_RACK: int = int(os.getenv("PLC_RACK", "0"))
    PLC_SLOT: int = int(os.getenv("PLC_SLOT", "0"))


settings = Settings()
