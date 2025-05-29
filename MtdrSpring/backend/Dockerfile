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

# Copiar el JAR compilado
COPY --from=build /app/target/MyTodoList-0.0.1-SNAPSHOT.jar app.jar

# Copiar wallet Oracle si es necesario
COPY src/main/resources/Wallet_javadev /app/Wallet_javadev

# Exponer puerto
EXPOSE 8080

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]