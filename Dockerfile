FROM openjdk:17-jdk-slim

WORKDIR /app

# JAR 파일 복사 (빌드된 JAR 파일 이름에 맞춰 수정 필요)
COPY build/libs/SayUp-0.0.1-SNAPSHOT.jar app.jar

ARG DB_PASSWORD
ARG API_KEY

# 환경변수 설정
ENV DB_CONNECTION=mysql
ENV DB_NAME=sayup_db
ENV DB_USERNAME=root
ENV DB_PASSWORD=$DB_PASSWORD
ENV API_KEY=$API_KEY
ENV FILE_UPLOAD_DIR=/file/temp

ENTRYPOINT ["java", "-jar", "app.jar"]