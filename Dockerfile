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
LABEL description="Voyage Backend - Ottimizzato per 512MB RAM"
LABEL version="1.0.0"

# Crea user non-root per sicurezza
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

WORKDIR /app

# Copia solo il JAR dall'immagine di build
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

# Ottimizzazioni JVM BILANCIATE per 512MB
# Breakdown memoria (aggiustato dopo OOM Metaspace):
# - Heap: 128-280MB (bilanciato per lasciare spazio a Metaspace)
# - Metaspace: 160MB (aumentato da 96MB - Spring Boot 3.x + dipendenze pesanti)
# - Code Cache: 64MB (aumentato da 32MB - necessario per JIT)
# - Thread stacks: ~40MB
# - Direct memory: 32MB (ridotto da default 128MB)
# - Overhead JVM: ~40MB
# TOTALE: ~440-480MB (margine sicurezza ~32-72MB)
ENV JAVA_TOOL_OPTIONS="\
-Xms128m \
-Xmx280m \
-XX:MaxMetaspaceSize=160m \
-XX:MetaspaceSize=128m \
-XX:ReservedCodeCacheSize=64m \
-XX:MaxDirectMemorySize=32m \
-XX:+UseG1GC \
-XX:G1HeapRegionSize=1m \
-XX:MaxGCPauseMillis=100 \
-XX:+UseStringDeduplication \
-XX:+OptimizeStringConcat \
-XX:+UseCompressedOops \
-XX:+UseCompressedClassPointers \
-XX:+ExitOnOutOfMemoryError \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=/tmp/heapdump.hprof \
-Djava.security.egd=file:/dev/./urandom \
-Dspring.jmx.enabled=false \
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
