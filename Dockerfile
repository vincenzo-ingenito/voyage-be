# ============================================
# STAGE 1: Build con cache delle dipendenze
# ============================================
FROM maven:3.9.4-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copia solo pom.xml per cachare le dipendenze
COPY pom.xml .

# Download dipendenze (layer cachato se pom.xml non cambia)
RUN mvn dependency:go-offline -B

# Copia il resto del codice
COPY src ./src

# Build dell'applicazione con ottimizzazioni
RUN mvn clean package -DskipTests -Dspring-boot.run.jvmArguments="-Xmx256m" && \
    # Rimuovi file non necessari per ridurre dimensione
    rm -rf /root/.m2/repository

# ============================================
# STAGE 2: Runtime ultra-ottimizzato
# ============================================
FROM eclipse-temurin:21-jre-alpine

# Metadata immagine
LABEL maintainer="Vincenzo Ingenito <vincenzo.ingenit@hotmail.it>"
LABEL description="Voyage Backend - Ottimizzato per 512MB RAM (Bootstrap-Safe)"
LABEL version="2.0.1"

# Crea user non-root per sicurezza
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

WORKDIR /app

# Copia solo il JAR dall'immagine di build
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

# Ottimizzazioni JVM ESTREME per 512MB - Bootstrap-Safe
# Breakdown memoria (ottimizzato al limite per Spring Boot 3.x):
# - Heap: 96-320MB (massimo possibile per bootstrap)
# - Metaspace: 140MB (ridotto ma sufficiente)
# - Code Cache: 32MB (ridotto per risparmiare)
# - Direct memory: 16MB (minimo necessario)
# - Thread stacks: ~30MB (ridotto)
# - Overhead JVM: ~40MB
# TOTALE: ~478MB (margine critico ~34MB)
#
# STRATEGIA:
# 1. Heap 320MB: appena sufficiente per bootstrap pesante
# 2. Metaspace 140MB: limite minimo per Spring Boot 3.x
# 3. GC aggressivo: libera memoria rapidamente durante bootstrap
# 4. Disabilitare features opzionali via application.properties
ENV JAVA_TOOL_OPTIONS="\
-Xms96m \
-Xmx320m \
-XX:MaxMetaspaceSize=140m \
-XX:MetaspaceSize=96m \
-XX:ReservedCodeCacheSize=32m \
-XX:MaxDirectMemorySize=16m \
-XX:+UseG1GC \
-XX:G1HeapRegionSize=1m \
-XX:MaxGCPauseMillis=50 \
-XX:GCTimeRatio=4 \
-XX:InitiatingHeapOccupancyPercent=30 \
-XX:+UseStringDeduplication \
-XX:+OptimizeStringConcat \
-XX:+UseCompressedOops \
-XX:+UseCompressedClassPointers \
-XX:+ExitOnOutOfMemoryError \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=/tmp/heapdump.hprof \
-Djava.security.egd=file:/dev/./urandom \
-Dspring.jmx.enabled=false \
-Dspring.main.lazy-initialization=true \
-Dfile.encoding=UTF-8 \
-Djava.awt.headless=true \
-verbose:gc"

# Switch a user non-root
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9080/health/ready || exit 1

EXPOSE 9080

# Usa exec form per migliore gestione segnali
ENTRYPOINT ["java", "-jar", "app.jar"]
