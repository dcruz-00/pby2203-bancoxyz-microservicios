# ---------- Etapa 1: compilación ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Wrapper de Maven (fija la versión de Maven del proyecto)
COPY mvnw .
COPY .mvn .mvn

# Dependencias en una capa propia: se reutiliza mientras el pom no cambie
COPY auth-server/pom.xml auth-server/pom.xml
RUN ./mvnw -B -f auth-server/pom.xml dependency:go-offline

# Código fuente y empaquetado
COPY auth-server/src auth-server/src
RUN ./mvnw -B -f auth-server/pom.xml package -DskipTests

# ---------- Etapa 2: ejecución ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sin privilegios
RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=build /workspace/auth-server/target/auth-server-*.jar app.jar

EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]