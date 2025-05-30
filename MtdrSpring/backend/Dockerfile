# Usar imagen base oficial de Maven con JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar archivos del proyecto
COPY pom.xml .
COPY src ./src

# Compilar la aplicación
RUN mvn clean package -DskipTests

# Imagen final con JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Instalar herramientas útiles
RUN apk add --no-cache curl

# Copiar el JAR compilado
COPY --from=build /app/target/MyTodoList-0.0.1-SNAPSHOT.jar app.jar

# Copiar wallet Oracle a la ubicación correcta
COPY --from=build /app/src/main/resources/Wallet_javadev /app/wallet

# Verificar que el wallet se copió correctamente
RUN ls -la /app/wallet/ && echo "Wallet files:" && ls /app/wallet/

# Variables de entorno para el wallet
ENV TNS_ADMIN=/app/wallet
ENV ORACLE_TNS_ADMIN=/app/wallet

# Variables JVM optimizadas para contenedor
ENV JAVA_OPTS="-Xmx384m -Xms256m -Djava.security.egd=file:/dev/./urandom"

# Exponer puerto
EXPOSE 8080

# Health check a nivel de contenedor
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]