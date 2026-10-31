# S11 notify-service 구현 결과

상태: 구현·오프라인 build 완료, Claude 독립 검증 대기. 계약은 `RPT-01_STAGE2_PLAN.md` 5절과 사용자 S11 지시를 적용했다.

| 항목 | 결과 |
| --- | --- |
| 경로 / 모듈 | `v2/notify-service` / 단일 루트 모듈 `notify-service` |
| 실행 환경 | JDK 21, Gradle wrapper 8.14.5, Spring Boot 3.5.16, dependency-management 1.1.7, Groovy DSL |
| 운영 직접 선언 | 11개: implementation 10개 + runtimeOnly H2 1개 |
| 테스트 직접 선언 | testImplementation starter-test 1개; 전체 직접 선언 12개 |
| 정답표 | `answer-key-v2-s11.csv`: 원본과 같은 23열, C76~C89 총 14행, holdout 전부 N |
| 기대 판정 수 | REFERENCED 6, USED_VIA_CHILD 3, UNUSED_CANDIDATE HIGH 3 + LOW 1, DECLARED_ONLY 1 |
| 플래그 | outOfBom=true 8행, versionMismatch=true 1행(C87 jackson-databind) |
| REST | `POST /notifications` → 201 + UUID + Location, `GET /notifications/{id}` → 200 또는 404 |
| DB / 처리 | JdbcTemplate update/queryForList + H2 in-memory; 입력 정규화·검증; 전송 URL 준비; JSON 날짜 offset 보존 |

`notify-service/gradle/`·`gradlew`·`gradlew.bat`은 order-service에서 복사했고 네 파일 모두 바이트 일치를 확인했다. sourceCompatibility/targetCompatibility는 21이며 java.toolchain은 사용하지 않았다. Boot 관리 좌표는 dependency 선언에서 버전을 생략했다. 사내 ext-client 1.1.0은 기존 Maven 저장소를 사용한다.

실행 명령은 저장소 루트 기준이다. 모든 Gradle 실행은 아래 지정 실행기를 사용했으며, 네트워크·bootRun·사용자 `~/.gradle` 접근·git 쓰기 명령은 수행하지 않았다.

| 순서 | 명령 | exit / 결과 |
| --- | --- | --- |
| 1 | `/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s11 v2/notify-service build` | 1 — 전체 log4j2.version=2.25.4 override 시 log4j-to-slf4j:2.25.4 공급 부재 |
| 2 | `/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s11 v2/notify-service build` | 1 — API만 개별 관리한 뒤 Boot 기본 commons-codec:1.18.0 jar 공급 부재 |
| 3 | `/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s11 v2/notify-service build` | **0 — BUILD SUCCESSFUL, 7 actionable tasks executed, 11 tests PASS** |
| 4 | `/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s11 v2/notify-service dependencies --configuration runtimeClasspath` | 0 — 선택 버전·starter 경로 확인; 출력 `notify-service/build/runtime-dependencies.txt` |

공급 부재에 따른 최종 관리 설정: HttpClient 5.6.1과 호환 HttpCore 5.4 속성, Log4j API만 2.25.4 개별 dependencyManagement, 전이 commons-codec 1.17.1 속성이다. log4j-to-slf4j는 Boot 기본 2.24.3을 유지한다. commons-codec 1.17.1은 계약 5.1 좌표 풀에 있는 공급 버전이다. 이 조정으로 직접 선언을 늘리지 않았으며 공급본·공통 BOM을 수정하지 않았다. 공급에 없는 log4j-to-slf4j 2.25.4와 commons-codec 1.18.0은 최종 graph에서 사용하지 않는다.

| 테스트 클래스 | 수 | 검증 |
| --- | --- | --- |
| NotificationApiTest | 6 | SpringBootTest + MockMvc의 시나리오 전체 JSON/UUID/Location, H2 실제 행과 조회, 404, null 필드, 공백·길이 초과, 잘못된 URL·날짜의 400/저장 방지, SQL 특수 문자 보존 |
| DeliveryPlannerTest | 3 | HttpPost URL 구성, PartnerClient 생성자의 scheme/host/query/fragment 검증, user info 거부; 실제 전송 없음 |
| NotificationCodecTest | 2 | JavaTimeModule 명시 등록 후 offset·한글·이스케이프 왕복, 손상 저장 JSON 오류 |
| 합계 | **11** | failures=0, errors=0, skipped=0 |

XML 근거는 `notify-service/build/test-results/test/TEST-com.depgov.sample.notify.{NotificationApiTest,DeliveryPlannerTest,NotificationCodecTest}.xml`, HTML은 `notify-service/build/reports/tests/test/index.html`이다. Boot context·MVC·Hikari·H2 기동과 종료를 테스트로 확인했다. 빌드 산출물은 `notify-service/build/libs/notify-service-1.0.0.jar`와 plain jar다. build/와 .gradle/은 서비스 내부 .gitignore로 제외한다.

README의 입력 `notify-service/input/notification.json` → 기대 필드 `notify-service/expected/notification.json` 시나리오는 API 테스트에서 파일을 직접 읽어 검증했다. 운영 코드는 method reference·람다·Class.forName·ServiceLoader·설정 파일의 클래스 이름 로딩을 새로 사용하지 않는다. 테스트 람다는 프로젝트 클래스 메서드를 호출하며 외부 라이브러리의 람다 전용 사용은 없다.

정답표는 소스 선언·호출과 계약 판정 규칙을 기준으로 작성했고 분석기를 실행하거나 그 출력에서 역생성하지 않았다. CSV 헤더·연번·행 폭·holdout 및 모든 근거 파일/줄의 실재를 확인했다. dependency report는 이미 정의한 선택 버전과 포함 경로를 대조하는 데만 사용했다. `git diff --check`도 exit 0이다.

한계와 후속 대조 사항:

- 기능은 예약 저장·조회와 전송 URL 준비까지다. 외부 전송과 백그라운드 발송은 구현하지 않았고 모든 알림은 QUEUED다. H2는 프로세스 종료 시 데이터를 잃는다. 네트워크 0 조건에 따라 서버 포트 바인딩·bootRun 대신 MockMvc 테스트로 기동을 확인했다.
- commons-compress·commons-text는 한 번도 안 쓴 선언이고, log4j-api는 전송 감사 로그의 Log4j API 호출 기능 삭제 뒤 잔류한 선언이다. README와 build.gradle 주석에 구분했다. 로깅은 Boot 기본 SLF4J/Logback 경로다.
- H2는 실제 DB로 사용하지만 서비스 바이트코드가 드라이버를 직접 참조하지 않으므로 C85의 기대값은 runtime-loading 규칙에 따라 UNUSED_CANDIDATE LOW다. 나머지 미사용 잔류 3개는 HIGH다.
- BOM 플래그는 Boot BOM이 아닌 저장소 `bom/*.gradle` 기준이다. jackson-databind 2.21.4는 저장소 BOM 2.18.2와 달라 C87 mismatch=true다. jsr310은 현재 저장소 BOM에 없어 C78 outOfBom=true다. Claude가 후속 BOM 통합에서 jsr310을 추가하면 그 행의 BOM 필드는 독립 대조 후 조정해야 한다. commons-text는 이미 저장소 BOM에 있으므로 false다.
- macOS FSEvents 시작 경고, JVM CDS 경고 및 Gradle 9 비호환 deprecation 안내가 있었으나 최종 build와 테스트는 성공했다. 분석기 판정·L/G lane 대조는 실행하지 않았으며 Claude 독립 검증 범위다.

변경은 `v2/notify-service/**`, `v2/answer-key-v2-s11.csv`, `v2/S11_RESULT.md`에만 남겼다. 기존 서비스·libs·bom·maven-repo·answer-key-v2.csv는 수정하지 않았다.
