# Estrategia: usar eclipse-temurin que es la distribución oficial de OpenJDK
# Esta imagen es mantenida por la Eclipse Foundation y tiene excelente disponibilidad
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Instalar Maven desde el repositorio oficial de Alpine - esto evita dependencias externas
# Alpine tiene Maven en sus repositorios oficiales, por lo que no hay problemas de rate limiting
RUN apk add --no-cache maven

# Copiar archivos del proyecto para la construcción
COPY pom.xml .
COPY src ./src

# Compilar la aplicación - el flag -DskipTests acelera el proceso para CI/CD
# En un entorno de producción, los tests se ejecutan en etapas separadas del pipeline
RUN mvn clean package -DskipTests

# Etapa de producción: usar la misma imagen base para consistencia
# eclipse-temurin es más liviano y optimizado para contenedores que las imágenes de Oracle
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Instalar herramientas esenciales para operaciones y debugging
# curl es necesario para health checks de Kubernetes
RUN apk add --no-cache curl

# Copiar el JAR construido desde la etapa anterior
COPY --from=build /app/target/MyTodoList-0.0.1-SNAPSHOT.jar app.jar

# Copiar el wallet de Oracle para autenticación con la base de datos
# Este wallet contiene los certificados necesarios para conectarse a Oracle Autonomous Database
COPY --from=build /app/src/main/resources/Wallet_javadev /app/wallet

# Verificar que el wallet se copió correctamente - esto es crucial para debugging
# Si el wallet no está presente, la aplicación fallará al intentar conectarse a la base de datos
RUN ls -la /app/wallet/ && echo "Wallet files:" && ls /app/wallet/

# Variables de entorno para que Oracle JDBC encuentre el wallet
ENV TNS_ADMIN=/app/wallet
ENV ORACLE_TNS_ADMIN=/app/wallet

# Configuración JVM optimizada para contenedores
# Xmx384m: máximo de memoria heap (suficiente para una aplicación Spring Boot mediana)
# Xms256m: memoria heap inicial (evita overhead de crecimiento dinámico)
# La configuración de entropy mejora el rendimiento de operaciones criptográficas
ENV JAVA_OPTS="-Xmx384m -Xms256m -Djava.security.egd=file:/dev/./urandom"

# Exponer el puerto donde la aplicación escucha
EXPOSE 8080

# Health check nativo de Docker - Kubernetes puede usar esto para verificar salud del contenedor
# El endpoint /api/actuator/health es proporcionado por Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Comando de inicio - usa el script wrapper para mejor manejo de señales en contenedores
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]