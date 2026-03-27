FROM maven:3.9.13-eclipse-temurin-25-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY package.json package-lock.json ./
COPY .npmrc ./
COPY svelte.config.js vite.config.ts tsconfig.json ./
COPY src ./src

RUN mvn -DskipTests package

FROM eclipse-temurin:25-jre-alpine-3.23
WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
