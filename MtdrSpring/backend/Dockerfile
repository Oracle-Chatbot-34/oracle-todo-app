# Usar imagen oficial más específica y menos popular para evitar rate limits
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Instalar Maven manualmente para tener más control
RUN apk add --no-cache maven

# Copiar archivos del proyecto
COPY pom.xml .
COPY src ./src

# Compilar la aplicación
RUN mvn clean package -DskipTests

# Imagen final optimizada - usar la misma base para consistencia
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Instalar curl para health checks
RUN apk add --no-cache curl

# Copiar el JAR compilado
COPY --from=build /app/target/MyTodoList-0.0.1-SNAPSHOT.jar app.jar

# Copiar wallet Oracle
COPY --from=build /app/src/main/resources/Wallet_javadev /app/wallet

# Verificar wallet
RUN ls -la /app/wallet/ && echo "Wallet files:" && ls /app/wallet/

# Variables de entorno para Oracle
ENV TNS_ADMIN=/app/wallet
ENV ORACLE_TNS_ADMIN=/app/wallet

# Variables JVM optimizadas
ENV JAVA_OPTS="-Xmx384m -Xms256m -Djava.security.egd=file:/dev/./urandom"

# Exponer puerto
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Ejecutar aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]