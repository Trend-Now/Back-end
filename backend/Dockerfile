FROM openjdk:21
ARG JAR_FILE=build/libs/backend-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS=""

# JVM 메모리 설정 (1GB 인스턴스 최적화)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]