# 🗂️ SayUp-Back

> 🎓 Graduation Project - SayUp

Spring Boot 기반의 SayUp 백엔드 서버입니다. 사용자 인증, 친구 시스템, 오디오 업로드, 음성 변환, OpenAI 연동 및 카카오 소셜 로그인을 지원합니다.

<br>

### 🚀 주요 기능

- JWT 기반 인증 / 회원가입 / 로그인
- 오디오 업로드 및 사용자 음성 등록
- Python 서버와 연동한 음성 처리
- ChatGPT 기반 OpenAI 튜터 챗봇 API
- 카카오 OAuth 로그인 통합
- 친구 추가 / 요청 수락 기능

<br>

### 📁 프로젝트 구조 요약

```
src/main/java/com/sayup/SayUp/
├── aop                # 로깅 AOP
├── config             # 전역 설정 (보안, Swagger 등)
├── controller         # REST API 진입점
├── dto                # 요청/응답 DTO
├── entity             # JPA 도메인 모델
├── kakao              # 카카오 로그인 전용 모듈
├── repository         # JPA Repository
├── security           # JWT 인증 구성
└── service            # 비즈니스 로직
```

> 📚 **자세한 구조와 설명은 [Wiki 페이지](https://github.com/GraduationProject-SayUp/SayUp-Back/wiki) 를 참고해주세요.**

<br>

### ⚙️ 기술 스택

| 계층        | 기술                                 |
|-------------|--------------------------------------|
| 언어        | Java 17                              |
| 프레임워크  | Spring Boot 3.4.0                    |
| 빌드 도구   | Gradle                               |
| 보안        | Spring Security + JWT                |
| DB          | MySQL, JPA                           |
| 문서화      | Swagger                              |
| 외부 API    | Kakao OAuth, OpenAI (ChatGPT)        |

<br>

### 🛠️ 개발 환경 세팅

1. `.env` 또는 `application.yml` 설정 (예: 카카오 키, OpenAI 키, DB 정보)
2. `./gradlew build`
3. `java -jar build/libs/sayup-backend.jar`

<br>

### 📌 기타

🌐 프론트엔드(Flutter)와 통합되어 동작합니다.
