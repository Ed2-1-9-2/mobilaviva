FROM node:20-alpine AS front
WORKDIR /app/frontend
COPY frontend/package.json .
RUN npm install --no-audit --no-fund
COPY frontend .
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY src src
COPY --from=front /app/src/main/resources/static src/main/resources/static
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
COPY --from=build /src/target/mobila-shop.jar /app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-jar", "/app.jar"]
