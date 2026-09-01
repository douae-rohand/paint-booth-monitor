FROM python:3.13-slim-bookworm

# Créer un utilisateur non-root dédié
RUN groupadd --system appuser && useradd --system --gid appuser --no-create-home appuser

WORKDIR /app

# Copier requirements et installer les dépendances en tant que root (nécessaire pour pip)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copier le code de l'application avec le bon propriétaire
COPY --chown=appuser:appuser . .

# Basculer vers l'utilisateur non-root pour l'exécution
USER appuser

EXPOSE 8000
HEALTHCHECK --interval=30s --timeout=3s --retries=3 --start-period=10s \
    CMD python -c "from urllib.request import urlopen; urlopen('http://localhost:8000/health', timeout=2).read()" || exit 1
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
