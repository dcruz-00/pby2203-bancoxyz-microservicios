# ---------- Etapa 1: compilación ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Wrapper de Maven (fija la versión de Maven del proyecto)
COPY mvnw .
COPY .mvn .mvn

# Dependencias en una capa propia: se reutiliza mientras el pom no cambie
COPY pagos/pom.xml pagos/pom.xml
RUN ./mvnw -B -f pagos/pom.xml dependency:go-offline

# Código fuente y empaquetado (incluye keystore y migraciones de Flyway)
COPY pagos/src pagos/src
RUN ./mvnw -B -f pagos/pom.xml package -DskipTests

# ---------- Etapa 2: ejecución ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sin privilegios
RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=build /workspace/pagos/target/pagos-*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]