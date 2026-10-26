# SMP-01 S2 결과 — order-service

- 상태: **Claude 검증 대기**
- 작업일: 2026-10-26 (Asia/Seoul)
- task-id: `s2`
- 작업 디렉터리: `/Users/coolguyhong/.codex/worktrees/smp01-s2/depgov-samples`
- 기준 commit (`git rev-parse HEAD`, exit 0): `82a982158a18834473459cff8aac29c583745112`
- 초기 `git status --short` 출력: 없음, exit 0. 기존 수정 없음.
- 구현 결과: Java 21 app/domain, C01~C11 소스 계약 및 오프라인 빌드·테스트 통과. depgov 분석 판정은 Claude 검증 대상이다.

## 1. 신규 파일 목록

저장소 변경은 아래 22개 파일과 이 결과 문서 1개뿐이다. 파일 경로는 저장소 루트 기준이다. ignored build/.gradle 생성물은 제출 파일 수에서 제외한다.

| 파일 | 변경 내용 |
| --- | --- |
| `v2/order-service/.gitignore` | build/、.gradle/、output/ 생성물 제외. |
| `v2/order-service/README.md` | Java 21 실행 방법, 입출력 시나리오, C01~C11 및 테스트 설명. |
| `v2/order-service/settings.gradle` | order-service 루트 이름과 app/domain 포함. |
| `v2/order-service/build.gradle` | 공통 Java 21·저장소·JUnit Platform 설정; dependencies 선언 없음. |
| `v2/order-service/gradlew` | svc-01의 실행 가능한 wrapper 스크립트 그대로 복사. |
| `v2/order-service/gradlew.bat` | svc-01의 Windows wrapper 스크립트 그대로 복사. |
| `v2/order-service/gradle/wrapper/gradle-wrapper.jar` | svc-01의 wrapper JAR 그대로 복사. |
| `v2/order-service/gradle/wrapper/gradle-wrapper.properties` | svc-01의 Gradle 8.14.5 설정 그대로 복사. |
| `v2/order-service/domain/build.gradle` | domain 자체에 lang3/API 로깅 및 JUnit 의존성 리터럴 선언. |
| `v2/order-service/app/build.gradle` | application 진입점, domain project 및 사례표의 외부 GAV, 실행 경로 설정. |
| `v2/order-service/input/orders.json` | 주문 3건, 품목 2/1/3개와 서로 다른 길이의 고객 ID. |
| `v2/order-service/expected/summary.json` | 손으로 계산한 주문별 금액·세금·총액, 패딩·확장자 기대 JSON. |
| `v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/Order.java` | 주문 입력 record. |
| `v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderLine.java` | 수량·정수 단가 품목 record. |
| `v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java` | C10 정적 검증·합산, C11 domain 로깅, overflow 검출. |
| `v2/order-service/domain/src/test/java/com/depgov/sample/order/domain/OrderCalculatorTest.java` | 계산 및 잘못된 입력·overflow 테스트 6개. |
| `v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderApp.java` | CLI 조합, C01 입력 검사, C07 고객 표시·세금 계산. |
| `v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderReader.java` | C02 ObjectMapper로 입력 JSON 읽기. |
| `v2/order-service/app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java` | C02 결과 쓰기, C03 streaming, C08 codec 왕복. |
| `v2/order-service/app/src/main/java/com/depgov/sample/order/app/PartnerLookup.java` | C05 파라미터 annotation, C09 선택적 파트너 조회. |
| `v2/order-service/app/src/main/java/com/depgov/sample/order/app/AttachmentNames.java` | C06 확장자 method reference만 사용. |
| `v2/order-service/app/src/test/java/com/depgov/sample/order/app/OrderAppTest.java` | main·기대 JSON·streaming·로컬 HTTP·오류 입력 테스트 5개. |
| `v2/S2_RESULT.md` | 기준·실행 명령·사례별 근거·소스 점검·의존성 트리 기록. |

## 2. 실제 실행 명령과 결과

Gradle은 아래 5회 모두 지정 실행기로만 실행했다. 작업 경로는 저장소 루트이며 실행기가 `--offline --no-daemon --console=plain`을 붙이고 저장소를 로컬로 치환한다. 다운로드 0. `--scan`은 실행하지 않았다.

| 순서 | 실제 명령 | exit | 결과 / 로그 |
| --- | --- | --- | --- |
| 1 | `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s2 v2/order-service build` | 0 | BUILD SUCCESSFUL, 11 tasks executed; `build.log` |
| 2 | `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s2 v2/order-service :app:run '--args=input/orders.json /private/tmp/depgov-smp01/s2/summary.json'` | 0 | BUILD SUCCESSFUL, streaming 한 줄 및 결과 JSON 생성; `run.log` |
| 3 | `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s2 v2/order-service :app:dependencies --configuration runtimeClasspath` | 0 | runtime 트리 확보; `app-runtime.log` |
| 4 | `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s2 v2/order-service :domain:dependencies --configuration runtimeClasspath` | 0 | runtime 트리 확보; `domain-runtime.log` |
| 5 | `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s2 v2/order-service build` | 0 | 최종 소스 상태 BUILD SUCCESSFUL, 11 tasks UP-TO-DATE; `build-final.log` |

로그 디렉터리: `/private/tmp/depgov-smp01/s2/evidence/`. 실행 2~4의 명령·exit 원본은 `commands.json`에도 남겼다. 첫 build 후 C06 파일의 첫 줄 주석에서 타입명을 지워 grep가 import 1 + method reference 1만 찾도록 했으며, 시나리오 실행에서 최종 소스를 재컴파일했다. 마지막 build는 같은 산출물과 테스트를 UP-TO-DATE로 확인했다.

| module | suite | tests | failures | errors | skipped |
| --- | --- | ---: | ---: | ---: | ---: |
| app | `com.depgov.sample.order.app.OrderAppTest` | 5 | 0 | 0 | 0 |
| domain | `com.depgov.sample.order.domain.OrderCalculatorTest` | 6 | 0 | 0 | 0 |
| 합계 | | 11 | 0 | 0 | 0 |

테스트 수는 각 module의 `build/test-results/test/TEST-*.xml`에서 읽었다. app은 임시 디렉터리로 main을 실행하여 expected JSON 전체 동등성 및 stdout 한 줄을 검증한다. 파트너 테스트는 127.0.0.1 임의 포트의 JDK HttpServer에서 GET `/partners/C1` 호출 수 1, 메서드, 경로, 응답 저장을 검증하고 서버를 종료한다. 금액 5의 세금 0.5 → 1도 확인한다. domain은 합산·무료 품목·빈 ID/품목·0/음수 수량·음수 단가·곱셈/합산 overflow를 검증한다.

추가 검증 명령:

| 명령 | exit | 결과 |
| --- | ---: | --- |
| `/opt/homebrew/bin/python3 -I -B /private/tmp/depgov-smp01/s2/evidence/audit.py` (초회) | 1 | C04에서 ignored 빌드 바이너리까지 grep하여 중단. 구현 실패가 아닌 검사 범위 문제; `audit-initial.log` 보존 |
| `grep -rn 'org.apache.commons.compress' v2/order-service/` | 0 | 생성된 previous-compilation-data.bin 2개와 배포 tar 1개만 일치, 아래 설명 참조 |
| `/opt/homebrew/bin/python3 -I -B /private/tmp/depgov-smp01/s2/evidence/audit.py` (범위 수정 후) | 0 | 전체 Java 10개 첫 줄 사례 주석, 참조 제약, 선언 GAV 검증 PASS; `audit.log`, `audit.json` |
| `/opt/homebrew/bin/python3 -I -B - <<'PY' ... PY` (JSON·wrapper·class major·JUnit XML 점검) | 0 | JSON EQUAL, wrapper 4개 byte 동일, app/domain major 65, 11 tests 성공 |
| `git diff --name-only -- svc-01 svc-02 svc-03 bom fixtures v2/libs v2/maven-repo` | 0 | 출력 0, 보호 경로 수정 없음 |
| `git diff --check -- v2/order-service v2/S2_RESULT.md` | 0 | 출력 0. 신규 파일 공백 검사는 verify.py에서도 별도로 수행 |
| `/opt/homebrew/bin/python3 -I -B /private/tmp/depgov-smp01/s2/evidence/verify.py` | 0 | 최종 JSON·wrapper·class major·JUnit XML·파일 범위·공백 점검, 아래 출력 |

C04의 소스 계약은 소스·테스트·자원 미참조이다. 전체 디렉터리에 생성된 Gradle 증분 컴파일 데이터와 dependency를 포함하는 배포 tar가 있으므로 바이너리 일치를 소스 참조로 세지 않았다. 최종 grep는 `--exclude-dir=build --exclude-dir=.gradle`로 생성물만 제외하며 README·Gradle·input·expected·모든 소스/테스트를 검사한다.

사전 읽기 조사: 계약 전체 및 1~4절, `svc-01/`, `svc-02/`, `bom/`, `v2/libs/README.md`와 공통 API 소스, 실행기, GAV 목록을 cat/sed/rg로 읽었다. AGENTS.md 검색의 미발견 exit 1, 존재하지 않는 `svc-02/src` 조회의 exit 2는 실제 모듈 경로 app/core/common을 읽어 해결했다. 외부 검색은 하지 않았다. 생성 작업은 apply_patch, mkdir, svc-01 wrapper의 `cp -p`, Python 결과 문서 작성만 사용했다.

## 3. C01~C11 구현 위치

다음 표의 파일 경로는 `v2/order-service/` 기준이며 줄은 최종 소스 기준이다. 모든 Java 파일 첫 줄에 SMP 사례 ID와 용도를 적었다.

| 사례 | 선언 위치 | 실행 소스 위치 | 사용 방식 / 다른 참조 점검 |
| --- | --- | --- | --- |
| SMP-C01 | `app/build.gradle:12` | `app/src/main/java/com/depgov/sample/order/app/OrderApp.java:21`, `:22` | StringUtils.isBlank 정적 호출. 아래 전역 app/src grep의 단일 파일에만 위치 |
| SMP-C02 | `app/build.gradle:13` | `app/src/main/java/com/depgov/sample/order/app/OrderReader.java:11`, `:14`; `app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:15`, `:23` | new ObjectMapper + readValue/writeValue. 테스트의 mapper는 기대 JSON 동등성 확인용. 그 외 ObjectMapper 사용 없음 |
| SMP-C03 | 직접 선언 없음 | `app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:28`, `:29` | new JsonFactory, createGenerator로 summary 한 줄 streaming. app에 jackson-core 선언 0 |
| SMP-C04 | `app/build.gradle:15` | 소스·테스트·자원 사용 위치 없음 | commons-compress는 진짜 미사용. 패키지 문자열 grep 0건(생성물 제외) |
| SMP-C05 | `app/build.gradle:16` | `app/src/main/java/com/depgov/sample/order/app/PartnerLookup.java:5`, `:8` | import + 메서드 파라미터 @Nullable 1회만. 클래스 리터럴·다른 annotation/type 없음 |
| SMP-C06 | `app/build.gradle:17` | `app/src/main/java/com/depgov/sample/order/app/AttachmentNames.java:5`, `:12` | import 1 + stream().map(FilenameUtils::getExtension) 1만. 직접 호출·필드 타입 등 다른 참조 없음 |
| SMP-C07 | `app/build.gradle:18` | `app/src/main/java/com/depgov/sample/order/app/OrderApp.java:36`, `:38` | Money.roundHalfUp(subtotal, 10), StringKit.pad(customerId, 8) 정적 호출만 |
| SMP-C08 | `app/build.gradle:19` | `app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:19`, `:20` | JsonCodec.write/read 정적 호출로 요약 DTO JSON 왕복 후 파일 기록. 옵션 없는 기본 실행에서도 모두 실행 |
| SMP-C09 | `app/build.gradle:20` | `app/src/main/java/com/depgov/sample/order/app/PartnerLookup.java:8`, `:12`; `app/src/main/java/com/depgov/sample/order/app/OrderApp.java:26`, `:40` | 옵션 없음 → null 반환; 옵션 있음 → new PartnerClient(baseUrl).get("/partners/" + id). 다른 PartnerClient 참조 없음 |
| SMP-C10 | `domain/build.gradle:2` | `domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:13`~`:22` | domain에서 StringUtils/Validate 정적 호출. 다른 domain 소스의 lang3 참조 없음 |
| SMP-C11 | `domain/build.gradle:3`; app 선언 없음 | `domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:6`, `:7`, `:10`, `:24` | domain만 LoggerFactory.getLogger 및 Logger 사용. app/src의 org.slf4j 문자열 0, app/build.gradle 직접 선언 0 |

C09 실제 HTTP 검증 위치는 `app/src/test/java/com/depgov/sample/order/app/OrderAppTest.java:52`(서버), `:74`(옵션 실행), `:77`~`:81`(호출 1회·GET·경로·응답 검증)이다.

### 실제 grep 명령과 결과 전체

각 명령은 저장소 루트에서 audit.py가 subprocess로 그대로 실행했다. exit 1은 grep의 정상적인 0건 의미다. 주석에 포함된 타입명도 출력했으며, 아래 목록과 명시된 import·실행식·테스트 외 소스 참조는 없다. Python으로 모든 Java 파일을 추가 점검하여 FilenameUtils 토큰 총 2개, method reference 1개, @Nullable 1개, javax.annotation import 종류는 Nullable 하나임을 확인했다.

**C01** — exit 0, 4건

```sh
grep -rnE StringUtils v2/order-service/app/src
```

```text
v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderApp.java:1:// SMP-C01: StringUtils.isBlank 정적 호출; SMP-C07: StringKit.pad/Money.roundHalfUp 정적 호출.
v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderApp.java:13:import org.apache.commons.lang3.StringUtils;
v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderApp.java:21:                || StringUtils.isBlank(args[0]) || StringUtils.isBlank(args[1])
v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderApp.java:22:                || (args.length == 4 && (!"--partner-url".equals(args[2]) || StringUtils.isBlank(args[3])))) {
```

**C02** — exit 0, 11건

```sh
grep -rnE 'ObjectMapper|readValue|writeValue' v2/order-service/app/src
```

```text
v2/order-service/app/src/test/java/com/depgov/sample/order/app/OrderAppTest.java:10:import com.fasterxml.jackson.databind.ObjectMapper;
v2/order-service/app/src/test/java/com/depgov/sample/order/app/OrderAppTest.java:24:    private final ObjectMapper mapper = new ObjectMapper();
v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderReader.java:1:// SMP-C02: new ObjectMapper() 및 readValue 메서드로 입력 JSON을 주문 타입으로 읽기.
v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderReader.java:5:import com.fasterxml.jackson.databind.ObjectMapper;
v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderReader.java:11:    private final ObjectMapper mapper = new ObjectMapper();
v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderReader.java:14:        OrderInput document = mapper.readValue(input.toFile(), OrderInput.class);
v2/order-service/app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:1:// SMP-C02: ObjectMapper 출력; SMP-C03: JsonFactory streaming; SMP-C08: JsonCodec.write/read 정적 호출.
v2/order-service/app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:7:import com.fasterxml.jackson.databind.ObjectMapper;
v2/order-service/app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:15:    private final ObjectMapper mapper = new ObjectMapper();
v2/order-service/app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:18:        // 공통 codec의 DTO 왕복을 거친 결과를 ObjectMapper로 읽기 좋게 저장한다.
v2/order-service/app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:23:        mapper.writerWithDefaultPrettyPrinter().writeValue(absolute.toFile(), decoded);
```

**C03** — exit 0, 4건

```sh
grep -rnE 'JsonFactory|createGenerator' v2/order-service/app/src
```

```text
v2/order-service/app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:1:// SMP-C02: ObjectMapper 출력; SMP-C03: JsonFactory streaming; SMP-C08: JsonCodec.write/read 정적 호출.
v2/order-service/app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:5:import com.fasterxml.jackson.core.JsonFactory;
v2/order-service/app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:28:        JsonFactory factory = new JsonFactory();
v2/order-service/app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:29:        try (JsonGenerator generator = factory.createGenerator(output)) {
```

**C03-no-declaration** — exit 1, 0건

```sh
grep -n jackson-core v2/order-service/app/build.gradle
```

```text
(0 matches)
```

**C04** — exit 1, 0건

```sh
grep -rn --exclude-dir=build --exclude-dir=.gradle org.apache.commons.compress v2/order-service
```

```text
(0 matches)
```

**C05** — exit 0, 3건

```sh
grep -rnE 'javax\.annotation|Nullable|CheckForNull' v2/order-service/app/src
```

```text
v2/order-service/app/src/main/java/com/depgov/sample/order/app/PartnerLookup.java:1:// SMP-C05: Nullable은 파라미터 annotation 전용; SMP-C09: 옵션이 있을 때만 PartnerClient 생성·get 호출.
v2/order-service/app/src/main/java/com/depgov/sample/order/app/PartnerLookup.java:5:import javax.annotation.Nullable;
v2/order-service/app/src/main/java/com/depgov/sample/order/app/PartnerLookup.java:8:    public String lookup(@Nullable String baseUrl, String id) {
```

**C05-no-other-usage** — exit 1, 0건

```sh
grep -rnE 'Nullable\.class|CheckForNull|javax\.annotation\.[A-MO-Za-z]' v2/order-service/app/src
```

```text
(0 matches)
```

**C06** — exit 0, 2건

```sh
grep -rn FilenameUtils v2/order-service/app/src
```

```text
v2/order-service/app/src/main/java/com/depgov/sample/order/app/AttachmentNames.java:5:import org.apache.commons.io.FilenameUtils;
v2/order-service/app/src/main/java/com/depgov/sample/order/app/AttachmentNames.java:12:        return names == null ? List.of() : names.stream().map(FilenameUtils::getExtension).toList();
```

**C06-no-direct-call** — exit 1, 0건

```sh
grep -rnE 'FilenameUtils\.' v2/order-service/app/src
```

```text
(0 matches)
```

**C07** — exit 0, 5건

```sh
grep -rnE 'com\.depgov\.common\.util|StringKit|Money' v2/order-service/app/src
```

```text
v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderApp.java:1:// SMP-C01: StringUtils.isBlank 정적 호출; SMP-C07: StringKit.pad/Money.roundHalfUp 정적 호출.
v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderApp.java:4:import com.depgov.common.util.Money;
v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderApp.java:5:import com.depgov.common.util.StringKit;
v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderApp.java:36:            long orderTax = Money.roundHalfUp(orderSubtotal, 10);
v2/order-service/app/src/main/java/com/depgov/sample/order/app/OrderApp.java:38:            results.add(new OrderSummary(order.orderId(), StringKit.pad(order.customerId(), 8),
```

**C08** — exit 0, 4건

```sh
grep -rn JsonCodec v2/order-service/app/src
```

```text
v2/order-service/app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:1:// SMP-C02: ObjectMapper 출력; SMP-C03: JsonFactory streaming; SMP-C08: JsonCodec.write/read 정적 호출.
v2/order-service/app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:4:import com.depgov.common.convert.JsonCodec;
v2/order-service/app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:19:        String encoded = JsonCodec.write(summary);
v2/order-service/app/src/main/java/com/depgov/sample/order/app/SummaryWriter.java:20:        Summary decoded = JsonCodec.read(encoded, Summary.class);
```

**C09** — exit 0, 5건

```sh
grep -rnE 'PartnerClient|partners/' v2/order-service/app/src
```

```text
v2/order-service/app/src/test/java/com/depgov/sample/order/app/OrderAppTest.java:53:        server.createContext("/partners/", exchange -> {
v2/order-service/app/src/test/java/com/depgov/sample/order/app/OrderAppTest.java:79:            assertEquals("/partners/C1", path.get());
v2/order-service/app/src/main/java/com/depgov/sample/order/app/PartnerLookup.java:1:// SMP-C05: Nullable은 파라미터 annotation 전용; SMP-C09: 옵션이 있을 때만 PartnerClient 생성·get 호출.
v2/order-service/app/src/main/java/com/depgov/sample/order/app/PartnerLookup.java:4:import com.depgov.common.ext.PartnerClient;
v2/order-service/app/src/main/java/com/depgov/sample/order/app/PartnerLookup.java:12:        return new PartnerClient(baseUrl).get("/partners/" + id);
```

**C10** — exit 0, 11건

```sh
grep -rnE 'org\.apache\.commons\.lang3|StringUtils|Validate' v2/order-service/domain/src
```

```text
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:1:// SMP-C10: StringUtils/Validate 정적 호출; SMP-C11: domain에서만 LoggerFactory 생성·로깅.
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:4:import org.apache.commons.lang3.StringUtils;
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:5:import org.apache.commons.lang3.Validate;
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:13:        Validate.notNull(order, "order is required");
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:14:        Validate.isTrue(!StringUtils.isBlank(order.orderId()), "orderId is required");
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:15:        Validate.isTrue(!StringUtils.isBlank(order.customerId()), "customerId is required");
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:16:        Validate.notEmpty(order.lines(), "at least one order line is required");
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:19:            Validate.notNull(line, "order line is required");
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:20:            Validate.isTrue(!StringUtils.isBlank(line.productId()), "productId is required");
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:21:            Validate.isTrue(line.qty() > 0, "qty must be positive");
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:22:            Validate.isTrue(line.unitPrice() >= 0, "unitPrice must be nonnegative");
```

**C11-domain** — exit 0, 4건

```sh
grep -rnE 'org\.slf4j|LoggerFactory' v2/order-service/domain/src
```

```text
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:1:// SMP-C10: StringUtils/Validate 정적 호출; SMP-C11: domain에서만 LoggerFactory 생성·로깅.
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:6:import org.slf4j.Logger;
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:7:import org.slf4j.LoggerFactory;
v2/order-service/domain/src/main/java/com/depgov/sample/order/domain/OrderCalculator.java:10:    private static final Logger LOG = LoggerFactory.getLogger(OrderCalculator.class);
```

**C11-app-no-reference** — exit 1, 0건

```sh
grep -rn org.slf4j v2/order-service/app/src
```

```text
(0 matches)
```

**C11-app-no-declaration** — exit 1, 0건

```sh
grep -n slf4j v2/order-service/app/build.gradle
```

```text
(0 matches)
```

## 4. 실행 시나리오 및 expected 비교

실제 stdout (JsonFactory streaming):

```json
{"orderCount":3,"subtotal":16801,"tax":1681,"total":18482}
```

생성 파일: `/private/tmp/depgov-smp01/s2/summary.json`. 아래는 실제 출력 내용이다.

```json
{
  "orderCount" : 3,
  "subtotal" : 16801,
  "tax" : 1681,
  "total" : 18482,
  "orders" : [ {
    "orderId" : "ORD-001",
    "customerId" : "A1      ",
    "subtotal" : 2999,
    "tax" : 300,
    "total" : 3299,
    "attachmentExtensions" : [ "pdf", "csv" ],
    "partnerResponse" : null
  }, {
    "orderId" : "ORD-002",
    "customerId" : "CUST1234",
    "subtotal" : 1005,
    "tax" : 101,
    "total" : 1106,
    "attachmentExtensions" : [ "txt" ],
    "partnerResponse" : null
  }, {
    "orderId" : "ORD-003",
    "customerId" : "CUSTOMER-12345",
    "subtotal" : 12797,
    "tax" : 1280,
    "total" : 14077,
    "attachmentExtensions" : [ "png", "", "gz" ],
    "partnerResponse" : null
  } ]
}
```

비교는 `json.loads(actual) == json.loads(expected)`로 PASS, exit 0. app JUnit도 ObjectMapper.readTree로 전체 JSON 동등성을 검증했다. pretty-print 공백과 마지막 개행은 비교 조건에서 제외하며 고객 ID 안의 패딩 공백은 그대로 검증한다.

| 주문 | 품목 합계 | 10% HALF_UP 세금 | 총액 |
| --- | ---: | ---: | ---: |
| ORD-001 | 2×1250 + 1×499 = 2,999 | 300 | 3,299 |
| ORD-002 | 3×335 = 1,005 | 101 | 1,106 |
| ORD-003 | 1×12000 + 2×250 + 3×99 = 12,797 | 1,280 | 14,077 |
| 전체(주문별 결과 합산) | 16,801 | 1,681 | 18,482 |

## 5. 실제 runtimeClasspath 트리

### app

```text
runtimeClasspath - Runtime classpath of source set 'main'.
+--- project :domain
|    +--- org.apache.commons:commons-lang3:3.17.0
|    \--- org.slf4j:slf4j-api:2.0.13
+--- org.apache.commons:commons-lang3:3.17.0
+--- com.fasterxml.jackson.core:jackson-databind:2.18.2
|    +--- com.fasterxml.jackson.core:jackson-annotations:2.18.2
|    |    \--- com.fasterxml.jackson:jackson-bom:2.18.2
|    |         +--- com.fasterxml.jackson.core:jackson-annotations:2.18.2 (c)
|    |         +--- com.fasterxml.jackson.core:jackson-core:2.18.2 (c)
|    |         \--- com.fasterxml.jackson.core:jackson-databind:2.18.2 (c)
|    +--- com.fasterxml.jackson.core:jackson-core:2.18.2
|    |    \--- com.fasterxml.jackson:jackson-bom:2.18.2 (*)
|    \--- com.fasterxml.jackson:jackson-bom:2.18.2 (*)
+--- org.apache.commons:commons-compress:1.27.1
|    +--- commons-codec:commons-codec:1.17.1
|    +--- commons-io:commons-io:2.16.1 -> 2.18.0
|    \--- org.apache.commons:commons-lang3:3.16.0 -> 3.17.0
+--- com.google.code.findbugs:jsr305:3.0.2
+--- commons-io:commons-io:2.18.0
+--- com.depgov.common:common-util:1.0.0
|    \--- org.apache.commons:commons-lang3:3.17.0
+--- com.depgov.common:data-convert:1.0.0
|    +--- org.apache.commons:commons-lang3:3.17.0
|    \--- com.fasterxml.jackson.core:jackson-databind:2.18.2 (*)
\--- com.depgov.common:ext-client:1.0.0
     +--- org.apache.httpcomponents.client5:httpclient5:5.6.1
     |    +--- org.apache.httpcomponents.core5:httpcore5:5.4
     |    +--- org.apache.httpcomponents.core5:httpcore5-h2:5.4
     |    |    \--- org.apache.httpcomponents.core5:httpcore5:5.4
     |    \--- org.slf4j:slf4j-api:1.7.36 -> 2.0.13
     \--- org.apache.httpcomponents.core5:httpcore5:5.4

(c) - A dependency constraint, not a dependency. The dependency affected by the constraint occurs elsewhere in the tree.
(*) - Indicates repeated occurrences of a transitive dependency subtree. Gradle expands transitive dependency subtrees only once per project; repeat occurrences only display the root of the subtree, followed by this annotation.
```

### domain

```text
runtimeClasspath - Runtime classpath of source set 'main'.
+--- org.apache.commons:commons-lang3:3.17.0
\--- org.slf4j:slf4j-api:2.0.13
```

jackson-core 2.18.2는 databind 전이이며 app 직접 선언이 없다. SLF4J는 domain의 2.0.13이 ext-client → httpclient5의 1.7.36보다 우선하여 2.0.13으로 선택되었다. commons-compress의 전이 commons-io/lang3는 app 직접 버전 2.18.0/3.17.0으로 선택된다.

## 6. 미확보·미실행·amendment 및 보호 규칙

- 미확보 좌표: 없음. module별 리터럴 외부 GAV 선언은 총 14개(중복 제외 11개)이며 제공 GAV 목록 및 사내 좌표 집합에 모두 포함된다.
- 좌표 수 해석: 요청의 “좌표 7개” 문구와 달리 사례표에는 app 직접 외부 GAV가 8개 있으므로 표의 8개를 모두 선언했다. jackson-core와 app SLF4J는 선언하지 않았다. amendment 요청 없음.
- 미실행: depgov clone·분석·API/UI 및 G lane은 이번 S2 구현 범위 밖으로 Claude 검증 대기. 외부 파트너/외부 HTTP 호출 없음. 테스트의 로컬 파트너 호출 1회만 수행.
- 알려진 한계는 유지: C05 LIM-ANNOTATION, C06 LIM-INVOKEDYNAMIC, C11 LIM-PROJECT-PATH. 기대 판정이나 분석기를 수정하지 않았다.
- 경고: SLF4J provider 부재(NOP)는 API만 의도적으로 선언한 결과다. Gradle의 FSEvents stream 시작 실패 및 deprecation 경고가 있었으나 모든 빌드 exit 0. 공급 wrapper는 --no-daemon 실행에도 “single-use Daemon” JVM을 fork하고 종료한다고 출력했다. 별도 상주 daemon을 시작하거나 전역 프로세스 종료를 수행하지 않았다.
- 외부 host 접속·다운로드 0. HTTP는 127.0.0.1 HttpServer만 사용. DB/MQ·docker·kubectl·gcloud 사용 0.
- Gradle 직접 실행 0. 지정 samplegradle.sh와 s2 전용 Gradle home만 사용. 사용자 ~/.gradle 및 ~/.depgov-gradle 접근 0. toolchain·외부 plugin·catalog·platform 선언 0.
- git 쓰기(add/commit/checkout/reset/clean/stash) 0. 작업 트리를 그대로 남겼다. git 읽기 명령만 사용했다.
- v2/libs/**, v2/maven-repo/**, svc-0*/**, bom/**, fixtures/** 및 depgov 저장소 수정 0. 도구 스크립트도 읽기만 했다. .env/비밀값을 읽거나 출력하지 않았다.
- Python은 `/opt/homebrew/bin/python3 -I -B`만 사용. 검사 스크립트·로그는 `/private/tmp/depgov-smp01/s2/evidence/`에 보관했다.

최종 verify.py 출력:

```text
SCENARIO JSON EQUAL: PASS
WRAPPER IDENTICAL gradlew b187b4c52e749f5760afdd6fadc31b2a98ad35fb249bf0dff03b72650f320409
WRAPPER IDENTICAL gradlew.bat 1d297e00bd21de3ace22b4d7f2de1f9dfa858883d66bbf7c1ccbecccec8f4f3b
WRAPPER IDENTICAL gradle/wrapper/gradle-wrapper.jar 7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172
WRAPPER IDENTICAL gradle/wrapper/gradle-wrapper.properties cc9996e1b641176fded450c88cecacbb1c06bb7c4e111d2f11f0164af143cfbe
JAVA CLASS MAJOR app [65] classes=8
TESTS app {'tests': '5', 'failures': '0', 'errors': '0', 'skipped': '0'}
JAVA CLASS MAJOR domain [65] classes=3
TESTS domain {'tests': '6', 'failures': '0', 'errors': '0', 'skipped': '0'}
OWNERSHIP/WHITESPACE: PASS; new files=23
```

상태: **Claude 검증 대기**
