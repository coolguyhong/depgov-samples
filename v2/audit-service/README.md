# Audit service — RPT-01 S12

Spring Boot 3.5.16, Gradle 8.14.5, Java 17의 감사 기록 REST 표본이다. `app`은 `project(':domain')`으로 domain의 정규화 기능을 호출한다. 실제 빌드는 지정 실행기의 JDK 21에서 `javac --release 17`로 컴파일하며 toolchain을 사용하지 않는다.

`app`은 REST·명시적 Validator 검증·JdbcTemplate·JSON 저장을 담당하고, `domain`은 commons-lang3로 공백을 정리하고 action을 대문자로 바꾼다. H2 메모리 DB의 `audit_event` 테이블에 정규화한 기록을 JSON으로 저장한다. SQL은 바인딩 파라미터를 사용하고 eventId 기본 키로 중복을 방지한다. 저장 JSON은 CLOB이므로 JSON escape로 길어진 본문도 보존한다.

일반 빌드 인터페이스는 서비스 루트에서 `./gradlew build`다. 이 작업 환경에서는 저장소 루트에서 다음 실행기만 사용한다. 격리 Gradle home·로컬 Maven mirror·offline 모드를 적용하며 네트워크 다운로드는 없다.

```sh
/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s12 v2/audit-service build
```

`bootRun`은 실행하지 않는다. `@SpringBootTest` + MockMvc가 Boot application context, MVC 요청 처리, 직접 호출하는 Validator와 H2 저장·조회를 함께 확인한다. 테스트는 외부 연결이나 HTTP 소켓을 사용하지 않는다. domain 단위 테스트도 app의 테스트 소스에 두어 testImplementation 선언은 한 개만 둔다.

| 요청 | 동작 | 성공 상태 |
| --- | --- | --- |
| `POST /audit-events` | 검증·정규화한 감사 기록 저장 | 201 + 정규화된 JSON |
| `GET /audit-events/{eventId}` | 저장한 기록 조회 | 200 + 같은 JSON |

eventId는 영문·숫자·`_`·`-` 1~40자, actor는 공백이 아닌 문자열 최대 80자다. action은 create/update/delete/read 중 하나이며 대소문자와 앞뒤 공백을 허용한다. detail은 선택 항목으로 최대 500자이며 생략하면 빈 문자열이다. 공백 정규화는 domain에서 수행한다. 잘못된 필드·빈 본문·잘못된 JSON은 400, 중복 eventId는 409, 존재하지 않는 eventId는 404다. 중복 요청은 기존 기록을 바꾸지 않는다.

## 입력 → 기대 출력 시나리오

새 DB 상태에서 `POST /audit-events`의 body에 `input/audit-event.json`을 넣는다.

```json
{"eventId":"EVT-001","actor":"  ops   user  ","action":" update ","detail":"  changed   retention  "}
```

201 응답은 `expected/audit-event.json`과 JSON 동등하다.

```json
{"eventId":"EVT-001","actor":"ops user","action":"UPDATE","detail":"changed retention"}
```

이후 `GET /audit-events/EVT-001`은 200과 같은 JSON을 반환한다. `AuditApplicationTest.scenarioCreatesAndReadsNormalizedPersistedJson`은 두 응답뿐 아니라 H2에서 직접 읽은 저장 JSON도 기대 파일과 비교한다. 같은 ID로 다시 생성하면 409다.

## 선언과 분석 사례

업무용 외부 직접 선언은 app 9개 + domain 1개다. 이외에 app의 project 의존 1개와 `testImplementation spring-boot-starter-test` 1개가 있다. 모든 직접 라이브러리 버전은 Boot BOM 관리 좌표이므로 생략했다. Boot BOM과 depgov의 `bom/*.gradle`은 별도의 기준이다.

오프라인 공급에는 Boot 기본 commons-codec 1.18.0의 POM만 있고 JAR가 없다. 계약 좌표 풀에 있는 공급 버전 1.17.1로 `commons-codec.version`을 재정의했다. 그 외에는 Boot BOM 기본값을 사용하며 jackson-databind는 2.21.4, slf4j-api는 2.0.18이다. depgov BOM의 jackson 2.18.2·slf4j 2.0.16·postgresql 42.7.4와 선택 버전이 달라 정답표의 versionMismatch는 true다.

| 좌표 / 모듈 | 직접 사용 또는 잔류 구분 |
| --- | --- |
| starter-web / app | 하위 spring-web의 ResponseStatusException·HttpStatus 호출 등 → USED_VIA_CHILD |
| starter-jdbc / app | 하위 spring-jdbc의 JdbcTemplate.update/queryForList 호출 → USED_VIA_CHILD |
| starter-validation / app | 하위 jakarta.validation의 Validation.buildDefaultValidatorFactory/getValidator/validate 호출 → USED_VIA_CHILD |
| h2 / app | Boot가 JDBC URL에서 선택하는 실제 런타임 드라이버. H2 클래스 직접 참조는 없으므로 UNUSED_CANDIDATE LOW |
| commons-lang3 / app·domain | StringUtils 및 Validate 직접 호출 → REFERENCED |
| jackson-databind / app | new ObjectMapper, writeValueAsString/readValue → REFERENCED |
| slf4j-api / app | LoggerFactory.getLogger 및 Logger.info 직접 호출 → REFERENCED |
| commons-codec / app | NEVER_USED: 처음부터 사용하지 않은 잔류 선언 → UNUSED_CANDIDATE HIGH |
| postgresql / app | REMOVED_FEATURE: PostgreSQL 외부 감사 보관 기능을 삭제한 뒤 드라이버 선언만 남김. 현재 저장은 H2 전용 → UNUSED_CANDIDATE LOW |
| starter-test / app | 테스트 전용 선언 → DECLARED_ONLY |

`../answer-key-v2-s12.csv`는 C90~C102 13사례다. 업무용 직접 선언 10개, starter 경유 하위 사용 2개(jakarta.validation-api, spring-jdbc), testImplementation 1개를 소스·실제 줄 번호로 기록했다. PostgreSQL을 포함해 holdout은 모두 N이며 최종 지정은 통합 담당이 한다. project 의존에는 외부 G:A가 없으므로 별도 행을 만들지 않는다.

라이브러리 사용은 일반 생성자·메서드 호출로 드러난다. 새 reflection, ServiceLoader, method reference, 설정 파일의 드라이버 클래스 지정은 없다. Validator의 표준 부트스트랩과 Boot의 JDBC 자동 구성은 계약에서 요구한 경로다. 단위 테스트의 assertThrows 람다는 project 메서드만 호출한다.

## 범위와 한계

H2 데이터는 프로세스 종료 시 사라진다. 인증·영구 보관·수정·삭제 기능은 없는 단일 인스턴스 감사 표본이다. H2의 LOW 후보는 정적 직접 참조가 없는 런타임 사용을 구분하기 위한 사례다. `@SpringBootTest`는 mock 웹 환경으로 실행하므로 실제 포트 바인딩은 검증하지 않는다. 정답표는 계약과 소스에서 작성했으며 분석기 결과는 사용하지 않았다.
