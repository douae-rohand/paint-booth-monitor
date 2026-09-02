# ── Stage 1 : build ──────────────────────────────────────────────────────────
# Maven 3.9 + Eclipse Temurin 21 (distribution OpenJDK maintenue par Adoptium)
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copie pom.xml en premier pour profiter du cache de layer Maven
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2 : runtime ─────────────────────────────────────────────────────────
# JRE seul (pas JDK) — image Alpine légère
FROM eclipse-temurin:21-jre-alpine

# Installer wget pour le healthcheck
RUN apk add --no-cache wget

# Créer un utilisateur non-root dédié
RUN addgroup -S appuser && adduser -S appuser -G appuser

WORKDIR /app

# Copie uniquement le JAR produit — pas les sources, pas les dépendances Maven
COPY --from=build /app/target/*.jar app.jar

# Changer le propriétaire du répertoire de travail
RUN chown -R appuser:appuser /app

# Basculer vers l'utilisateur non-root
USER appuser

EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --retries=5 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
