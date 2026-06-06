FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/payload-producer-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Xms64m", "-Xmx192m", "-jar", "app.jar"]