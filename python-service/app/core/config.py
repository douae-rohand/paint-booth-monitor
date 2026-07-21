import os
from pathlib import Path

from dotenv import load_dotenv

_ENV_PATH = Path(__file__).resolve().parents[2] / ".env"
load_dotenv(_ENV_PATH)


class Settings:
    DATABASE_URL: str = os.getenv(
        "DATABASE_URL",
        "postgresql+asyncpg://python_service:python12345@localhost:5432/supervision_db",
    )
    DATABASE_ECHO: bool = os.getenv("DATABASE_ECHO", "false").lower() == "true"
    DATABASE_POOL_SIZE: int = int(os.getenv("DATABASE_POOL_SIZE", "5"))

    PLC_IP: str = os.getenv("PLC_IP", "192.168.0.1")
    PLC_RACK: int = int(os.getenv("PLC_RACK", "0"))
    PLC_SLOT: int = int(os.getenv("PLC_SLOT", "1"))


settings = Settings()
