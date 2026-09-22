# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# O Maven Wrapper mantém a versão de Maven usada pelo projeto consistente no build.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/dafabi-backend-*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
