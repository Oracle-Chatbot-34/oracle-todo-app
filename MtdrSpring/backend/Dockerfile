FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Instalar dependencias primero para aprovechar la caché de Docker
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline

# Copiar código fuente
COPY src ./src

# Construir aplicación
RUN ./mvnw package -DskipTests

# Segunda etapa: imagen de ejecución
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copiar solo el JAR de la etapa de compilación
COPY --from=build /app/target/MyTodoList-0.0.1-SNAPSHOT.jar app.jar

# Copiar wallet Oracle para conectividad a base de datos
COPY src/main/resources/Wallet_javadev /app/src/main/resources/Wallet_javadev

# Variables de entorno para Java
ENV JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8080

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]