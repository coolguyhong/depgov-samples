# Notify service — RPT-01 S11

JDK 21, Gradle 8.14.5 wrapper, Spring Boot 3.5.16의 단일 모듈 `notify-service`다. 알림의 수신자·본문을 정규화하고 전송 예정 시각과 URL을 H2 in-memory outbox에 저장한다. 예약 생성과 ID 조회 REST endpoint 두 개를 제공한다.

저장소 루트에서 지정된 오프라인 실행기로 빌드한다. 실행기는 별도 Gradle home, 공급된 Gradle/JDK, 로컬 Maven mirror만 사용한다. 네트워크 다운로드나 사용자 `~/.gradle` 접근은 하지 않는다.

```sh
/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s11 v2/notify-service build
```

프로젝트의 표준 빌드 진입점은 `./gradlew build`이며, 이 작업에서는 위 실행기를 통해서만 호출한다. `@SpringBootTest` + MockMvc가 실제 Boot application context, MVC, Hikari/JdbcTemplate, H2 스키마를 기동한다. 테스트는 HTTP 포트를 열지 않고 요청을 MVC로 전달한다.

| 요청 | 결과 |
| --- | --- |
| `POST /notifications` | 유효한 예약을 저장하고 201, 생성한 UUID `id`, `Location: /notifications/{id}` 반환 |
| `GET /notifications/{id}` | H2에서 JSON을 읽어 복원한 알림 반환; 없는 ID는 404 |

POST 본문의 `recipient`, `message`, `scheduledAt`, `partnerBaseUrl`은 필수다. 수신자는 양쪽 공백을 제거하고 최대 120자, 본문은 연속 공백을 하나로 정규화한 뒤 최대 1,000자다. 빈 값, 잘못된 날짜, HTTP(S) 외 URL, host 없는 URL, query·fragment·user info가 있는 URL은 400이다. 날짜는 offset을 포함한 ISO 8601 형식이다.

`PartnerClient` 생성자를 실제 호출하여 사내 클라이언트의 URL 검증을 재사용한다. `new HttpPost(...)`로 `/notifications` 전송 URL을 구성해 저장한다. 표본의 기능 범위는 예약 저장과 요청 준비까지이며, 외부 전송·스케줄러 실행은 구현하지 않았다. 모든 예약은 `QUEUED` 상태로 남는다. H2 데이터는 프로세스 종료 시 사라진다. 로깅은 Boot 기본 SLF4J/Logback 경로를 따른다.

입력→기대 출력 시나리오는 `input/notification.json`과 `expected/notification.json`이다. 아래 입력을 POST하면:

```json
{
  "recipient": "  ops-team  ",
  "message": "  Release   ready  ",
  "scheduledAt": "2030-11-01T09:00:00+09:00",
  "partnerBaseUrl": "https://notify.example.invalid/"
}
```

201 응답은 다음 필드와 매번 생성되는 UUID `id`를 포함한다. GET으로 같은 `id`를 조회하면 POST 응답과 같은 JSON을 반환한다.

```json
{
  "recipient": "ops-team",
  "message": "Release ready",
  "scheduledAt": "2030-11-01T09:00:00+09:00",
  "deliveryUrl": "https://notify.example.invalid/notifications",
  "status": "QUEUED"
}
```

`NotificationCodec`은 `new ObjectMapper()`와 `registerModule(new JavaTimeModule())`을 호출하고 날짜를 문자열로 저장하며 입력 offset을 보존한다. `NotificationRepository`는 JdbcTemplate의 `update`/`queryForList`를 직접 호출하며 SQL 파라미터 바인딩을 사용한다. Commons Lang의 `StringUtils` 호출은 필드 정규화·검증에 사용한다. method reference, ServiceLoader, Class.forName 또는 설정 파일의 클래스 이름으로 새로 로딩하는 코드는 없다.

| 사례 | 선언·사용 의도 |
| --- | --- |
| starter-web | MVC 하위 라이브러리 사용 → USED_VIA_CHILD |
| starter-json | web 경유와 중복 직접 선언; 하위 ObjectMapper 사용 → USED_VIA_CHILD |
| jackson-datatype-jsr310 | JavaTimeModule 명시 생성·등록 → REFERENCED |
| httpclient5 / commons-lang3 / ext-client | 요청 준비 / 정규화 / URL 검증에 직접 호출 → REFERENCED |
| commons-compress / commons-text | 한 번도 안 쓴 선언; 압축·템플릿 치환 미구현 → UNUSED_CANDIDATE HIGH |
| log4j-api | 기능 삭제 후 잔류: 예전 전송 감사 로그의 Log4j API 호출 기능을 제거했다 → UNUSED_CANDIDATE HIGH |
| h2 (runtimeOnly) | DB는 실제 사용하지만 서비스 바이트코드의 드라이버 참조 없음 → UNUSED_CANDIDATE LOW |
| starter-jdbc | 하위 JdbcTemplate 사용 → USED_VIA_CHILD |
| jackson-databind / spring-jdbc | 직접 선언 없는 starter 하위 클래스 직접 호출 → REFERENCED |
| starter-test (testImplementation 1개) | 운영 runtimeClasspath 밖 → DECLARED_ONLY |

운영 직접 선언 11개에 testImplementation 1개를 더해 총 12개다. 정답표 조각 `../answer-key-v2-s11.csv`에는 직접 선언 12개와 하위 사용 2개, C76~C89 총 14행을 소스 근거로 기록했다. H2 LOW는 실제 기능 미사용이라는 뜻이 아니라 정적 코드 참조만으로 드라이버 사용을 확정하지 못하는 규칙의 기대값이다.

Boot 관리 좌표는 dependency 선언에서 버전을 생략한다. S11 고정 버전에 맞게 HttpClient 5.6.1과 호환 HttpCore 5.4 관리 속성을 지정하고, Log4j API만 2.25.4로 개별 관리한다. 전체 Log4j BOM을 2.25.4로 바꾸면 공급본에 없는 log4j-to-slf4j 2.25.4가 필요하므로 브리지는 Boot 기본 버전을 유지한다. Boot 기본 commons-codec 1.18.0 jar도 없어 전이 버전을 계약 5.1 풀의 1.17.1로 관리한다. 이 두 전이 좌표는 직접 선언을 추가하지 않았다.

정답표의 BOM은 Boot BOM이 아니라 저장소 `bom/*.gradle` 기준이다. jackson-databind 선택 버전 2.21.4는 기준 2.18.2와 달라 versionMismatch=true다. jackson-datatype-jsr310은 현재 저장소 BOM에 없으므로 outOfBom=true로 작성했다. commons-text는 기존 BOM에 1.12.0이 있어 false다. Claude의 후속 BOM 통합에서 jsr310을 추가하면 해당 행은 독립 대조 후 조정해야 한다.

테스트는 정상 시나리오의 전체 응답·DB 행·왕복 조회, 없는 ID, null·공백·길이 초과·날짜·URL 오류의 저장 방지, SQL 특수 문자의 데이터 보존, 요청 URL 준비, JSON offset·한글·이스케이프 왕복과 손상 JSON 실패를 검증한다. API 테스트 6개와 단위 테스트 5개다.
