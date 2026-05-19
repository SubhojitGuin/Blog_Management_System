# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom.xml first so Maven dependency layer is cached
# (only re-downloads dependencies when pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Non-root user for security — never run Java apps as root in containers
RUN groupadd -r blogapp && useradd -r -g blogapp blogapp
USER blogapp

# Copy the fat JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Spring Boot app port
EXPOSE 8080

# JVM flags:
#   -XX:+UseContainerSupport    → respect Docker CPU/memory limits (critical in containers)
#   -XX:MaxRAMPercentage=75.0   → use 75% of container RAM for heap
#   -Dspring.profiles.active    → activate JSON logging (logback-spring.xml !dev profile)
#   -Dapp.env                   → picked up by logback-spring.xml ${APP_ENV} property
ENTRYPOINT ["java", "-jar", "app.jar"]
