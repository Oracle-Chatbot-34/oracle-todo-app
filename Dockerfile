FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/MyTodoList-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/Wallet_javadev /app/wallet

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
