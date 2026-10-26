# SMP-01 공통 라이브러리 1.0.0

SMP-01_PLAN.md 3.3절의 공개 API를 제공하는 단일 Gradle multi-project다. 서비스는 이 소스를 project 의존으로 포함하지 않고 `com.depgov.*` 외부 Maven 좌표로 사용한다. 이번 산출물은 모두 1.0.0이며 1.1.0은 만들지 않는다.

| 좌표 | 목표 release / class major | 의존 선언(테스트 제외) | 공개 API |
| --- | --- | --- | --- |
| `com.depgov.common:common-util:1.0.0` | 8 / 52 | `api 'org.apache.commons:commons-lang3:3.17.0'` | `com.depgov.common.util.StringKit.pad(String, int)`, `StringKit.isBlank(String)`, `com.depgov.common.util.Money.roundHalfUp(long, int)` |
| `com.depgov.common:data-convert:1.0.0` | 17 / 61 | `api 'com.fasterxml.jackson.core:jackson-databind:2.18.2'`, `implementation 'org.apache.commons:commons-lang3:3.17.0'` | `com.depgov.common.convert.JsonCodec.read(String, Class<T>)`, `JsonCodec.write(Object)`, `com.depgov.common.convert.CsvLine.split(String)`, `CsvLine.join(List<String>)` |
| `com.depgov.common:ext-client:1.0.0` | 21 / 65 | `implementation 'org.apache.httpcomponents.client5:httpclient5:5.6.1'`, `implementation 'org.apache.httpcomponents.core5:httpcore5:5.4'` | `com.depgov.common.ext.PartnerClient(String)`, `PartnerClient.get(String)`, `com.depgov.common.ext.PartnerException extends RuntimeException` |
| `com.depgov.plugins:plugin-api:1.0.0` | 21 / 65 | 없음 | `com.depgov.plugins.spi.Plugin.id()`, `Plugin.render(Map<String,String>)` |
| `com.depgov.plugins:report-plugin:1.0.0` | 21 / 65 | `implementation project(':plugin-api')` → POM `com.depgov.plugins:plugin-api:1.0.0` (runtime) | `com.depgov.plugins.report.ReportPlugin implements Plugin`; id `report`, key 순서로 `key=value` 줄 출력 |

각 모듈의 `build.gradle`에 의존성을 직접 선언한다. 공통 루트 설정은 `java-library`, `maven-publish`, group/version, release, JUnit Platform, Java component publication을 제공한다. Sources JAR는 생성하지 않는다. 테스트 의존성은 모듈별 JUnit Jupiter 5.11.4와 JUnit Platform Launcher 1.11.4이며 publication에는 포함하지 않는다.

## API 동작과 경계

- `StringKit.pad`는 오른쪽을 공백으로 채우며 null은 빈 문자열로 취급한다. width가 문자열 길이 이하이면 자르지 않는다. `isBlank`는 `StringUtils.isBlank`에 위임한다.
- `Money.roundHalfUp`은 `amount * ratePercent / 100`을 HALF_UP으로 반올림한다. 음수의 정확한 .5도 0에서 먼 쪽으로 반올림한다. BigDecimal로 중간 곱셈 overflow를 피하고 결과가 long 범위를 벗어나면 `ArithmeticException`을 던진다.
- `JsonCodec`은 변경 없이 공유하는 ObjectMapper 한 개를 사용한다. JSON 처리 실패는 원인을 보존한 `IllegalArgumentException`이다. JSON `null`은 지원하며 Java null JSON 문자열은 거부한다.
- `CsvLine.split`은 쉼표, 큰따옴표 필드, 이중 큰따옴표 escape를 지원하고 각 필드의 양끝을 trim한다. null line은 빈 리스트, 빈 문자열은 빈 필드 하나다. 빈 끝 필드를 유지하며 잘못된 따옴표는 `IllegalArgumentException`이다. `join`은 쉼표·큰따옴표·CR/LF가 있는 필드를 인용하고 큰따옴표를 이중화한다. null 리스트/빈 리스트는 빈 문자열, null 필드는 빈 필드다. split의 trim 때문에 양끝 공백은 왕복 시 정규화된다.
- `PartnerClient`는 base URL의 경로 뒤에 요청 path를 붙이고 경계 `/`를 정리한다. HTTP(S) base URL만 받으며 query/fragment는 path에 둔다. GET의 모든 2xx 본문(UTF-8 기본값, 무본문은 빈 문자열)을 반환한다. 3xx를 따라가지 않으며 비-2xx 및 I/O 실패는 `PartnerException`이다. 호출마다 HTTP client와 응답을 닫는다.
- `ReportPlugin.render`는 입력을 변경하지 않고 key의 자연 순서로 정렬하며 줄 사이에 `\n`을 사용하고 마지막 개행은 붙이지 않는다. 빈 map은 빈 문자열이고 null map은 거부한다. `META-INF/services/com.depgov.plugins.spi.Plugin`에는 구현 클래스 한 줄만 등록한다. 테스트도 ServiceLoader로만 provider를 찾는다.

## 빌드와 publish

저장소 루트에서 JDK 21과 사전 공급된 오프라인 저장소를 사용하는 지정 실행기로만 실행한다. Gradle wrapper 8.14.5의 네 파일은 `svc-01/`에서 그대로 복사했다.

```sh
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s1 v2/libs build
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s1 v2/libs publish
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s1 v2/libs :ext-client:dependencies --configuration runtimeClasspath
```

publish 대상은 `uri("${rootDir}/../maven-repo")`, 즉 이 저장소의 `v2/maven-repo/`다. 각 artifact의 `1.0.0/`에 JAR/POM/Gradle module metadata와 checksum이, 상위 디렉터리에 `maven-metadata.xml`과 checksum이 생성된다. 이 파일을 유지한다. 오프라인 공용 mirror로의 반영은 Claude 검증 세션 소관이다. 실제 테스트 수, SHA-256, class major, 선택 의존 트리는 [S1_RESULT.md](../S1_RESULT.md)에 기록한다. ext-client의 실제 runtime 선택은 httpclient5 5.6.1, httpcore5/httpcore5-h2 5.4, slf4j-api 1.7.36이며 httpcore5 버전 충돌은 없다. Java 8/17 목표는 JDK 21의 `--release`로 검증하며 실제 JDK 8 실행 검증은 이번 범위에 포함하지 않는다.

## 1.1.0 예정 변경 — 계약 3.3절 그대로

- `common-util` 1.0.0: `com.depgov.common.util.StringKit { static String pad(String, int); static boolean isBlank(String); }`, `com.depgov.common.util.Money { static long roundHalfUp(long amount, int rate) }`. 1.1.0: `StringKit.pad` **제거**, `StringKit.padLeft/padRight` 추가, `Money` 유지. 의존: commons-lang3 3.17.0(`api`).
- `data-convert` 1.0.0: `com.depgov.common.convert.JsonCodec { static <T> T read(String, Class<T>); static String write(Object) }`, `com.depgov.common.convert.CsvLine { static List<String> split(String); static String join(List<String>) }`. 의존: jackson-databind 2.18.2(`api`), commons-lang3 3.17.0(`implementation`). 1.1.0: jackson-databind 2.21.4로만 교체.
- `ext-client` 1.0.0: `com.depgov.common.ext.PartnerClient { PartnerClient(String baseUrl); String get(String path); }` (httpclient5 5.6.1 사용, httpcore5 5.4 명시 `implementation`). 1.1.0: httpcore5 명시 선언 제거(전이 버전만) 또는 버전 변경 — Claude가 S1 결과를 보고 확정해 변경 이력에 적는다.
- `report-plugin` 1.0.0: `com.depgov.plugins.report.ReportPlugin implements com.depgov.plugins.spi.Plugin`(SPI 인터페이스는 `com.depgov.plugins:plugin-api` 1.0.0 별도 artifact, host가 직접 참조). `META-INF/services/com.depgov.plugins.spi.Plugin` 포함.
