FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home aria \
    && mkdir -p /app/data \
    && chown aria:aria /app/data
COPY --from=build /workspace/target/aria-backend-0.0.1-SNAPSHOT.jar /app/aria.jar
USER 10001
EXPOSE 8080
# Keep heap below the 512 MB instance limit, leaving room for JVM native memory.
ENV LOGGING_LEVEL_ROOT=INFO TZ=Asia/Qyzylorda
ENTRYPOINT ["java", "-Duser.timezone=Asia/Qyzylorda", "-Xms64m", "-Xmx256m", "-XX:+UseSerialGC", "-XX:ActiveProcessorCount=2", "-XX:TieredStopAtLevel=1", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/aria.jar"]
