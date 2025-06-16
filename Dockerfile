FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build -x test

FROM openjdk:17-jdk-slim

# 보안을 위한 비루트 사용자 생성
RUN groupadd -r sayup && useradd -r -g sayup sayup

# 필수 패키지 설치
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/* \

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# 필요한 디렉토리 생성 및 권한 설정
RUN mkdir -p /app/logs /app/tmp/file/userVoice && \
    chown -R sayup:sayup /app

USER sayup

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
