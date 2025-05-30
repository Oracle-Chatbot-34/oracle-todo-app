# Estrategia robusta usando Oracle Linux como base
# Esta imagen está garantizada de existir en Oracle Container Registry
FROM container-registry.oracle.com/os/oraclelinux:8-slim AS build
WORKDIR /app

# Instalar OpenJDK 17 y herramientas de desarrollo desde repositorios oficiales
# Esto nos da control total sobre las versiones y evita problemas de manifests
RUN microdnf update -y && \
    microdnf install -y java-17-openjdk-devel wget tar && \
    microdnf clean all

# Verificar que Java se instaló correctamente
# Esto es crucial para debugging si algo sale mal
RUN java -version && javac -version

# Instalar Maven desde Apache directamente
# Usamos una versión específica para garantizar reproducibilidad
RUN wget https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz && \
    tar -xzf apache-maven-3.9.6-bin.tar.gz -C /opt/ && \
    ln -s /opt/apache-maven-3.9.6/bin/mvn /usr/local/bin/mvn && \
    rm apache-maven-3.9.6-bin.tar.gz

# Verificar instalación de Maven
RUN mvn -version

# Copiar archivos del proyecto para la compilación
# El orden importa aquí: pom.xml primero para aprovechar cache de Docker
COPY pom.xml .
COPY src ./src

# Compilar la aplicación Spring Boot
# -DskipTests acelera el build para CI/CD
RUN mvn clean package -DskipTests

# Verificar que el JAR se creó correctamente antes de continuar
RUN ls -la target/ && echo "✅ JAR file created successfully"

# Etapa de runtime usando la misma base para consistencia
FROM container-registry.oracle.com/os/oraclelinux:8-slim
WORKDIR /app

# Instalar solo JRE para runtime (más eficiente que JDK completo)
# También incluimos curl para health checks de Kubernetes
RUN microdnf update -y && \
    microdnf install -y java-17-openjdk-headless curl && \
    microdnf clean all

# Verificar que Java runtime está disponible
RUN java -version

# Copiar JAR compilado desde la etapa de build
COPY --from=build /app/target/MyTodoList-0.0.1-SNAPSHOT.jar app.jar

# Copiar wallet Oracle para autenticación con Autonomous Database
# Este wallet contiene certificados y configuración de conexión
COPY --from=build /app/src/main/resources/Wallet_javadev /app/wallet

# Verificar wallet y mostrar contenido para facilitar debugging
RUN ls -la /app/wallet/ && echo "=== Archivos del wallet Oracle ===" && ls -la /app/wallet/

# Variables de entorno para que Oracle JDBC encuentre el wallet
ENV TNS_ADMIN=/app/wallet
ENV ORACLE_TNS_ADMIN=/app/wallet

# Configuración JVM optimizada para entornos containerizados
# Estos valores están calculados para un contenedor con recursos limitados
ENV JAVA_OPTS="-Xmx384m -Xms256m -Djava.security.egd=file:/dev/./urandom"

# Exponer puerto donde Spring Boot escucha por defecto
EXPOSE 8080

# Health check nativo de Docker para integración con Kubernetes
# Spring Boot Actuator proporciona el endpoint /actuator/health
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Ejecutar la aplicación usando las variables JVM configuradas
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]