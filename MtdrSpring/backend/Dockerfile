# Multi-stage build para optimizar el tamaño de la imagen
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Instalar dependencias primero para aprovechar la caché de Docker
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw

# Descargar dependencias (esto se cachea si no cambia el pom.xml)
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Construir aplicación
RUN ./mvnw package -DskipTests -B

# Segunda etapa: imagen de ejecución
FROM eclipse-temurin:17-jre-alpine

# Crear usuario no-root para mayor seguridad
RUN addgroup -g 1001 -S dashmaster && \
    adduser -S -D -H -u 1001 -h /app -s /sbin/nologin -G dashmaster -g dashmaster dashmaster

WORKDIR /app

# Copiar solo el JAR de la etapa de compilación
COPY --from=build /app/target/MyTodoList-0.0.1-SNAPSHOT.jar app.jar

# Copiar wallet Oracle para conectividad a base de datos
COPY --chown=dashmaster:dashmaster src/main/resources/Wallet_javadev /app/src/main/resources/Wallet_javadev

# Cambiar ownership de los archivos
RUN chown -R dashmaster:dashmaster /app

# Cambiar al usuario no-root
USER dashmaster

# Variables de entorno para Java
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Habilitar actuator endpoints para health checks
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info
ENV MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=always

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Ejecutar la aplicación con configuración optimizada para contenedores
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]