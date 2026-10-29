FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml ./pom.xml
RUN mvn -B dependency:go-offline
COPY backend/src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system inbox && useradd --system --gid inbox --home-dir /app inbox
WORKDIR /app
COPY --from=build --chown=inbox:inbox /build/target/inbox-0.1.0-exec.jar /app/inbox.jar
USER inbox
ENV SPRING_PROFILES_ACTIVE=cloud
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+ExitOnOutOfMemoryError"
EXPOSE 10000
ENTRYPOINT ["java","-jar","/app/inbox.jar"]
