# Usar Oracle Container Registry para evitar rate limits de Docker Hub
FROM container-registry.oracle.com/java/maven:3.9.6-openjdk-17 AS build
WORKDIR /app

# Copiar archivos del proyecto
COPY pom.xml .
COPY src ./src

# Compilar la aplicación
# -DskipTests acelera el build omitiendo tests en el contenedor
RUN mvn clean package -DskipTests

# Imagen final con JRE optimizada
FROM container-registry.oracle.com/java/openjdk:17-jre-slim
WORKDIR /app

# Instalar herramientas útiles para debugging y health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copiar el JAR compilado desde la etapa de build
COPY --from=build /app/target/MyTodoList-0.0.1-SNAPSHOT.jar app.jar

# Copiar wallet Oracle a la ubicación correcta para autenticación de base de datos
COPY --from=build /app/src/main/resources/Wallet_javadev /app/wallet

# Verificar que el wallet se copió correctamente - importante para debugging
RUN ls -la /app/wallet/ && echo "Wallet files:" && ls /app/wallet/

# Variables de entorno para el wallet de Oracle
ENV TNS_ADMIN=/app/wallet
ENV ORACLE_TNS_ADMIN=/app/wallet

# Variables JVM optimizadas para contenedor
# Xmx384m limita memoria máxima, Xms256m establece memoria inicial
ENV JAVA_OPTS="-Xmx384m -Xms256m -Djava.security.egd=file:/dev/./urandom"

# Exponer puerto 8080
EXPOSE 8080

# Health check a nivel de contenedor para Kubernetes
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]