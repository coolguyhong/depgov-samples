# RPT-01 C — S12 구현 결과

상태: 구현·오프라인 build 완료, Claude 독립 검증 대기. 변경은 `v2/audit-service/**`, `v2/answer-key-v2-s12.csv`, 이 문서에만 있다. 기존 서비스·libs·bom·maven-repo·정답표 본체는 수정하지 않았다. 네트워크 및 사용자 `~/.gradle` 접근, bootRun, git add/commit/checkout/reset/clean은 수행하지 않았다.

## 구성과 수량

| 항목 | 결과 |
| --- | --- |
| 서비스 | `audit-service` / S12 |
| 모듈 | `app`, `domain`; app → project(':domain') |
| 빌드 | Spring Boot 3.5.16, dependency-management 1.1.7, Gradle wrapper 8.14.5, Groovy DSL |
| Java | 실행기 JDK 21에서 source/target 17 및 javac release 17; main class 7개 모두 major 61 확인; toolchain 없음 |
| 업무용 외부 직접 선언 | app 9개 + domain 1개 = 10개 |
| 추가 선언 | project 의존 1개, app testImplementation 1개; compileOnly/testRuntimeOnly 없음 |
| REST | POST /audit-events, GET /audit-events/{eventId} |
| DB·유틸 | H2 메모리 DB, JdbcTemplate 파라미터 바인딩, ObjectMapper JSON CLOB 저장·복원, Validator 직접 호출, StringUtils/Validate 정규화, LoggerFactory 로깅 |
| 정답표 | C90~C102 총 13사례: 직접 10 + starter 하위 2 + 테스트 1 |
| 기대 판정 수 | REFERENCED 6, USED_VIA_CHILD 3, UNUSED_CANDIDATE 3(HIGH 1·LOW 2), DECLARED_ONLY 1 |
| 플래그 | outOfBom true 7개, versionMismatch true 3개; holdout 전부 N |

코드와 계약에서 정답을 작성했다. 분석기는 실행하지 않았으며 분석기 결과로 정답표를 만들지 않았다. starter 하위 명시 사례는 `jakarta.validation:jakarta.validation-api`와 `org.springframework:spring-jdbc`다. 모든 CSV 근거 경로·줄 범위 및 직접 선언 11개(테스트 포함)의 행 대응을 확인했다. 원본 CSV와 헤더 23열이 같고 C90부터 순차적이다.

## 실행 명령과 exit

모든 Gradle 명령은 저장소 루트에서 다음 형태로 실행했다.

```sh
/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s12 v2/audit-service build
```

| 실행 | args | exit | 결과 |
| --- | --- | --- | --- |
| 1 | `build` | 1 | Boot 기본 commons-codec 1.18.0 JAR 미공급으로 compileClasspath 해석 실패 |
| 2 | `build` | 1 | BOM import 내부 속성 조정만으로는 Boot 자동 import에 반영되지 않아 동일 실패 |
| 3 | `build` | 0 | subproject 확장 속성으로 공급 버전 1.17.1 선택; 11 tests PASS |
| 4 | `:app:dependencies --configuration runtimeClasspath :domain:dependencies --configuration runtimeClasspath` | 0 | 소스에서 정한 선택 버전·starter 하위 경로 확인; 테스트 의존은 운영 classpath에 없음 |
| 5 | `build` | 0 | 공백 정규화 시 빈 값이 되는 NBSP actor의 400 처리 보완 후 최종 build; 11 tests PASS |

선택 버전은 starter 3.5.16, commons-lang3 3.17.0, jackson-databind 2.21.4, slf4j-api 2.0.18, commons-codec 1.17.1, H2 2.3.232, PostgreSQL 42.7.11, jakarta.validation-api 3.0.2, spring-jdbc 6.2.19다. wrapper의 gradlew·gradlew.bat·JAR·properties는 기존 order-service와 바이트 동일함을 확인했다.

부가 검증(Python, exit 0): 테스트 XML 집계, class major 확인, CSV 헤더/ID/근거 줄/직접 선언 대응, wrapper 바이트 대조, 변경 경로 범위 확인. 소스에서 method reference·Class.forName·ServiceLoader 사용은 없고 설정 파일의 드라이버 클래스 이름 지정도 없다. 단위 테스트의 assertThrows 람다는 project 코드만 호출한다.

## 테스트와 시나리오

최종 실행: **11 tests, failures 0, errors 0, skipped 0**.

| 테스트 클래스 | 수 | 검증 |
| --- | --- | --- |
| AuditApplicationTest | 7 | SpringBootTest + MockMvc의 Boot context/MVC/H2 통합; 시나리오 생성·조회·DB JSON 동등성, 필드 검증·NBSP actor·초과 길이, 중복 시 409와 기존 값 보존, 조회 404, 빈/null/잘못된 JSON 400, 선택 detail 기본값, 최대 길이 및 JSON escape 확장 저장 |
| AuditNormalizerTest | 4 | 공백·action 정규화, 한글 보존·null detail 기본값, null 입력, 필수 필드 거부 |

domain 단위 테스트는 app 테스트 소스에서 실제 domain 클래스를 사용한다. 따라서 domain에 추가 테스트 의존성을 선언하지 않았다. 결과 XML은 다음과 같다.

- `audit-service/app/build/test-results/test/TEST-com.depgov.sample.audit.app.AuditApplicationTest.xml`
- `audit-service/app/build/test-results/test/TEST-com.depgov.sample.audit.domain.AuditNormalizerTest.xml`
- HTML: `audit-service/app/build/reports/tests/test/index.html`

README에 `input/audit-event.json` → 201 `expected/audit-event.json` → GET 200 동일 JSON 시나리오를 적었다. 테스트가 실제 두 fixture를 읽으며 H2 저장값까지 대조한다. app Boot JAR는 `audit-service/app/build/libs/app-1.0.0.jar`다.

## 한계와 공급 조정

- Boot BOM 기본 commons-codec 1.18.0은 로컬에 POM만 있고 JAR가 없어 사용하지 않았다. 계약 좌표 풀에 포함되고 공급된 1.17.1을 root build.gradle의 `ext['commons-codec.version']`으로 지정했다. 직접 선언의 버전은 생략했다. 추가 좌표·다운로드는 없다. 다른 직접 선언은 Boot BOM 기본 버전을 그대로 따른다.
- depgov BOM과 Boot BOM은 서로 다른 기준이다. depgov BOM의 jackson-databind 2.18.2·slf4j-api 2.0.16·postgresql 42.7.4는 선택 버전과 달라 versionMismatch=true다. 버전 생략 자체를 불일치로 보지 않았다.
- 의도적 잔류는 commons-codec(NEVER_USED: 한 번도 안 씀), PostgreSQL(REMOVED_FEATURE: PostgreSQL 외부 감사 보관 삭제 후 잔류)이다. build.gradle 주석과 README에 구분했다. 현재 저장 기능은 H2 전용이다.
- H2는 실제 사용하지만 H2 클래스에 대한 직접 정적 참조는 없다. 계약 1절의 runtime-loading 목록 규칙에 따라 H2도 UNUSED_CANDIDATE LOW로 기록했다. 따라서 LOW 2건 중 의도적 미사용은 PostgreSQL 1건이다. hold-out 지정은 통합 담당이 한다.
- 테스트는 mock 웹 환경이므로 Boot context와 MVC·DB를 검증하며 실제 포트 바인딩은 검증하지 않았다. 외부 HTTP·DB 연결은 없다. DB는 메모리 전용이며 인증·영구 보관·수정·삭제는 범위 밖이다.
- 빌드에 FSEvents 및 JVM CDS 경고가 있었으나 최종 build와 11개 테스트는 exit 0이다. 통합 분석·L/G lane·정답표 병합·BOM 수정은 수행하지 않았다.
