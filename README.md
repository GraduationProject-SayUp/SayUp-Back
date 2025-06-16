<div align="center">
  <img src="https://github.com/user-attachments/assets/023d5d2a-23e7-44b6-850b-64c579b69b42" alt="SayUp Logo" width="80" />
  
  <h1>SayUp - BE</h1>
  <p><strong>🎓 Graduation Project</strong></p>
  <p><em>내 목소리로 배우는 한국어</em></p>

  <p align="center">
    <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=Spring&logoColor=white" alt="Spring Boot"/>
    <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=MySQL&logoColor=white" alt="MySQL"/>
    <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=Redis&logoColor=white" alt="Redis"/>
    <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=Docker&logoColor=white" alt="Docker"/>
  </p>
  <p align="center">
    <img src="https://img.shields.io/badge/개발기간-2024.09~2025.06-7E57C2?style=for-the-badge" alt="개발기간"/>
</p>
</div>

<br>

### 주요 기능

- JWT 기반 사용자 인증 및 회원가입
- 카카오 소셜 로그인 연동
- 오디오 업로드 및 사용자 음성 데이터 저장
- FastAPI 서버와 연동한 발음 분석 처리
- ChatGPT API 기반 한국어 학습 챗봇 기능
- 친구 요청 및 수락 처리 기능

<br>

### 🚀 Quick Start

**1. 실행 전 설정**

- `.env` 또는 `application.yml`에 다음 항목 입력
  
  - DB 정보
  - Kakao REST API Key
  - OpenAI API Key

<br>

**2. Gradle로 직접 실행**

```bash
./gradlew build
java -jar build/libs/sayup-backend.jar
```

<br>

**3. Docker Compose로 실행**
```
docker-compose up --build
```
<br>

 ### 문서
 전체 아키텍처 및 상세 기능 설명은 [Wiki 페이지](https://github.com/GraduationProject-SayUp/SayUp-Back/wiki)를 참고하세요!
