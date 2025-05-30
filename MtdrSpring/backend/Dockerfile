# Estrategia ultra-simplificada usando Amazon Corretto (menos popular que eclipse-temurin)
# Corretto es la distribución OpenJDK de Amazon, muy estable y confiable
FROM amazoncorretto:17-alpine-jdk AS build
WORKDIR /app

# Instalar Maven desde Alpine packages - rápido y confiable
RUN apk add --no-cache maven

# Copiar y compilar - proceso directo sin complicaciones
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime simple con Corretto JRE
FROM amazoncorretto:17-alpine
WORKDIR /app

# Solo instalar lo esencial
RUN apk add --no-cache curl

# Copiar aplicación y wallet
COPY --from=build /app/target/MyTodoList-0.0.1-SNAPSHOT.jar app.jar
COPY --from=build /app/src/main/resources/Wallet_javadev /app/wallet

# Variables esenciales
ENV TNS_ADMIN=/app/wallet
ENV ORACLE_TNS_ADMIN=/app/wallet
ENV JAVA_OPTS="-Xmx384m -Xms256m"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]