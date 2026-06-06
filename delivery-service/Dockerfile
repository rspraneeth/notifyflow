FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/delivery-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-Xms64m", "-Xmx192m", "-jar", "app.jar"]