FROM eclipse-temurin:21-jre

WORKDIR /app

# Copia il JAR buildato
COPY target/*.jar app.jar

# Opzioni JVM (puoi passarle anche da Render come ENV)
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]