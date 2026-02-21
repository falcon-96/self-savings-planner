# docker build -t blk-hacking-ind-chetan-lastname .

# Eclipse Temurin on Alpine — lightweight (~200MB), stable LTS,
# official OpenJDK distribution optimized for containers.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 5477

ENTRYPOINT ["java", "-jar", "app.jar"]
