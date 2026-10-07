# ---------- Etapa 1: compilación ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Wrapper de Maven (fija la versión de Maven del proyecto)
COPY mvnw .
COPY .mvn .mvn

# Dependencias en una capa propia: se reutiliza mientras el pom no cambie
COPY batch-jobs/pom.xml batch-jobs/pom.xml
RUN ./mvnw -B -f batch-jobs/pom.xml dependency:go-offline

# Código fuente y empaquetado (incluye keystore y migraciones de Flyway)
COPY batch-jobs/src batch-jobs/src
RUN ./mvnw -B -f batch-jobs/pom.xml package -DskipTests

# ---------- Etapa 2: ejecución ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sin privilegios
RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=build /workspace/batch-jobs/target/batch-jobs-*.jar app.jar

# Proceso que se ejecuta y termina: el código de salida indica si todos los jobs se completaron
ENTRYPOINT ["java", "-jar", "app.jar"]