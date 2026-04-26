FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mkdir -p /app/file-storage /app/logs
RUN chmod -R 755 /app/file-storage /app/logs
RUN mvn clean package -DskipTests

# Этап запуска
FROM openjdk:17.0.1-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8070
ENTRYPOINT ["java","-jar","app.jar"]