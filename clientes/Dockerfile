# ---------- Etapa 1: compilación ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Wrapper de Maven (fija la versión de Maven del proyecto)
COPY mvnw .
COPY .mvn .mvn

# Dependencias en una capa propia: se reutiliza mientras el pom no cambie
COPY clientes/pom.xml clientes/pom.xml
RUN ./mvnw -B -f clientes/pom.xml dependency:go-offline

# Código fuente y empaquetado (incluye keystore y migraciones de Flyway)
COPY clientes/src clientes/src
RUN ./mvnw -B -f clientes/pom.xml package -DskipTests

# ---------- Etapa 2: ejecución ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sin privilegios
RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=build /workspace/clientes/target/clientes-*.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]