#build stage - builds with maven wrapper, needed for docker deployment
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src/ src/
RUN ./mvnw package -DskipTests -q
#skip running tests when building

#copies .jar output to java runtime env and runs on port 6767
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 6767
ENTRYPOINT ["java", "-jar", "app.jar"]
