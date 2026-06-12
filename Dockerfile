FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=demo
ENV PORT=8080
ENV DOCUMENTS_DIR=/app/uploads/demo-documents

RUN mkdir -p /app/data /app/uploads/demo-documents

COPY --from=build /app/target/lsototalbouw-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]

