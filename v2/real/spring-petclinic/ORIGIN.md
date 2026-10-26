# 출처 고정 (SMP-01 R-REAL, 2026-10-26)

- 원본: https://github.com/spring-projects/spring-petclinic
- commit: 500158f732419217507c7656904b8e6aa1bcc0d6 (main, GitHub API 조회 2026-10-26)
- 취득: GitHub archive zip (codeload.github.com 으로 redirect 1회), sha256 160c4b3eec1cdda42e6821a8e1a3e8d9b446c52423bd1d8ef14d19a8ee5e770f, 542140 bytes
- 변경: **없음**(원본 파일 그대로 vendoring). 이 ORIGIN.md 만 추가. 라이선스는 원본 LICENSE.txt(Apache-2.0).
- 분석 등록: targetPath `v2/real/spring-petclinic`, Gradle wrapper 9.5.1, toolchain Java 17, Spring Boot 4.1.0 plugin → depgov G lane(GKE, Maven Central·plugins.gradle.org 접근)에서만 빌드·분석. L lane(오프라인)에서는 빌드하지 않는다.
- 분석기 한계 예상: 버전 없는 선언(`'org.springframework.boot:spring-boot-starter-cache'`)은 선언으로 인식(선언 버전 null), `"${var}"` 보간 선언 4개와 `developmentOnly`/`checkstyle` 구성은 선언으로 세지 않음(finding) → 해당 좌표는 TRANSITIVE_ONLY 또는 선언 없는 REFERENCED 로 나타난다.
