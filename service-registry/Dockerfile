# ---------- Etapa 1: compilación ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Wrapper de Maven (fija la versión de Maven del proyecto)
COPY mvnw .
COPY .mvn .mvn

# Dependencias en una capa propia: se reutiliza mientras el pom no cambie
COPY service-registry/pom.xml service-registry/pom.xml
RUN ./mvnw -B -f service-registry/pom.xml dependency:go-offline

# Código fuente y empaquetado
COPY service-registry/src service-registry/src
RUN ./mvnw -B -f service-registry/pom.xml package -DskipTests

# ---------- Etapa 2: ejecución ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sin privilegios
RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=build /workspace/service-registry/target/service-registry-*.jar app.jar

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]