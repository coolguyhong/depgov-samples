# SMP-01 S3 정산 Batch 구현 결과

상태: **Claude 검증 대기**

업무 구현 및 별도 JDK 17 컴파일·JUnit 28건·expected 비교는 통과했다. **지정 Gradle build/run은 HikariCP의 전이 의존성 `org.slf4j:slf4j-api:2.0.17` 미확보로 실패했다.** 요구된 선언은 유지했으며, 공급본의 2.0.18을 constraint로 선택하는 amendment는 요청만 하고 적용하지 않았다. Gradle 성공으로 보고하지 않는다.

## 1. 기준과 수정 파일

- 기준 commit: `1d5e8a383ea122dc2616f8996c532bf8e8a66ec0` (`git rev-parse HEAD`, exit 0)
- 작업 경로: `/Users/coolguyhong/.codex/worktrees/smp01-s3/depgov-samples`
- 사용자 지정 branch/task: `codex/smp01-s3` / `s3`
- 참고: SMP-01_PLAN.md 1~4절, svc-01/, svc-02/, bom/, v2/libs/README.md 및 Money/JsonCodec 공개 API 구현을 읽었다.
- 소스·테스트는 Java 17 문법/API만 사용한다. toolchain/release 선언은 없다. Gradle 실행기는 Temurin 21.0.12.1을 강제하며, 별도 검증은 Homebrew OpenJDK **17.0.11**로 수행했다.

| 파일 | 변경 내용 |
| --- | --- |
| `v2/settlement-batch/.gitignore` | build/, .gradle/, output/ 제외 |
| `v2/settlement-batch/settings.gradle` | 단일 rootProject settlement-batch |
| `v2/settlement-batch/build.gradle` | java/application, 지정 의존 6개와 JUnit; Jackson 직접 선언 없음 |
| `v2/settlement-batch/gradlew` | svc-01에서 원본 그대로 복사한 실행 wrapper |
| `v2/settlement-batch/gradlew.bat` | svc-01에서 원본 그대로 복사한 Windows wrapper |
| `v2/settlement-batch/gradle/wrapper/gradle-wrapper.jar` | svc-01 원본 wrapper JAR |
| `v2/settlement-batch/gradle/wrapper/gradle-wrapper.properties` | svc-01 원본 Gradle 8.14.5 설정 |
| `v2/settlement-batch/input/transactions.csv` | 승인/취소/거절, HALF_UP 경계, 수수료 상한, 인용 필드 입력 |
| `v2/settlement-batch/input/config.json` | 최대 수수료율 3% |
| `v2/settlement-batch/expected/settlement.json` | 정렬된 가맹점별 정산과 전체 합계 기대 결과 |
| `v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java` | CSV → 승인 거래 집계 → JSON + 한 줄 요약; C12~C16 |
| `v2/settlement-batch/src/test/java/com/depgov/sample/settlement/SettlementBatchTest.java` | JUnit 5.11.4, parameterized 포함 28 testcase |
| `v2/settlement-batch/README.md` | 입출력 형식, 계산 규칙, 실행 방법, 사례와 공급 한계 |
| `v2/S3_RESULT.md` | 이 결과 문서, 실행 실패/성공 구분과 사례 근거 |

빌드·검증 산출물은 settlement-batch/build/, settlement-batch/output/와 task 전용 /private/tmp/depgov-smp01/s3/에만 생성했다. wrapper 4개는 각각 cmp exit 0으로 svc-01과 바이트 동일함을 확인했다.

## 2. 실행 명령과 결과

저장소 루트에서 실행했다. Gradle은 지정 실행기만 사용했으며 원문 로그를 task 전용 경로에 보존했다.

```sh
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s3 v2/settlement-batch build > /private/tmp/depgov-smp01/s3/build.log 2>&1
# exit 1: compileClasspath에서 slf4j-api:2.0.17 미확보, compile/test 미실행

/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s3 v2/settlement-batch dependencies --configuration runtimeClasspath > /private/tmp/depgov-smp01/s3/runtimeClasspath-initial.log 2>&1
# exit 0: dependencies task 자체는 성공하나 트리에 FAILED 노드가 있음

/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s3 v2/settlement-batch run --args='input/transactions.csv input/config.json /private/tmp/depgov-smp01/s3/settlement.json' > /private/tmp/depgov-smp01/s3/run-initial.log 2>&1
# exit 1: 동일 compileClasspath 미확보, main 실행 전 실패
```

| 모듈 / 실행 경로 | testcase | 성공 | 실패 | skip | 비고 |
| --- | --- | --- | --- | --- | --- |
| settlement-batch / 지정 Gradle build | 0 | 0 | 0 | 0 | 테스트 시작 전 의존성 해석 실패 |
| settlement-batch / JDK 17 javac + JUnit Platform Launcher | 28 | 28 | 0 | 0 | 같은 main/test 소스, Jupiter 5.11.4 및 Launcher 1.11.4 |

별도 검증은 Maven/Gradle 해석을 우회해 **확보된 JAR의 명시적 classpath**로 javac/java를 실행했다. 사내 라이브러리 2개, 선택된 commons-lang3 3.17.0, commons-io 2.18.0, Jackson 2.18.2 3개, 지정 PostgreSQL/HikariCP JAR, JUnit 및 그 테스트 전이 JAR만 포함했다. SLF4J를 다른 버전으로 대체하거나 HikariCP를 로딩하지 않았다. 이 방식은 업무 코드의 Java 17 호환성을 검증하며 Gradle 의존성 해석 성공을 뜻하지 않는다. 임시 `build/verification/JunitCheck.java`는 JUnit Launcher 실행만 담당한다.

별도 검증의 **실제 javac/java/cmp 명령 전체와 exit**는 아래 원문에 있다(전체 classpath를 생략하지 않음).

```text
$ /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home/bin/javac -encoding UTF-8 -cp /private/tmp/depgov-smp01/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.jar:/private/tmp/depgov-smp01/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.jar:/private/tmp/depgov-smp01/repository/org/apache/commons/commons-lang3/3.17.0/commons-lang3-3.17.0.jar:/private/tmp/depgov-smp01/repository/commons-io/commons-io/2.18.0/commons-io-2.18.0.jar:/private/tmp/depgov-smp01/repository/org/postgresql/postgresql/42.7.11/postgresql-42.7.11.jar:/private/tmp/depgov-smp01/repository/com/zaxxer/HikariCP/7.0.2/HikariCP-7.0.2.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-databind/2.18.2/jackson-databind-2.18.2.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-core/2.18.2/jackson-core-2.18.2.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-annotations/2.18.2/jackson-annotations-2.18.2.jar:/private/tmp/depgov-smp01/repository/org/junit/jupiter/junit-jupiter-api/5.11.4/junit-jupiter-api-5.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/jupiter/junit-jupiter-params/5.11.4/junit-jupiter-params-5.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/jupiter/junit-jupiter-engine/5.11.4/junit-jupiter-engine-5.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/platform/junit-platform-launcher/1.11.4/junit-platform-launcher-1.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/platform/junit-platform-engine/1.11.4/junit-platform-engine-1.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/platform/junit-platform-commons/1.11.4/junit-platform-commons-1.11.4.jar:/private/tmp/depgov-smp01/repository/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar:/private/tmp/depgov-smp01/repository/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar -d /Users/coolguyhong/.codex/worktrees/smp01-s3/depgov-samples/v2/settlement-batch/build/verification/classes /Users/coolguyhong/.codex/worktrees/smp01-s3/depgov-samples/v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java /Users/coolguyhong/.codex/worktrees/smp01-s3/depgov-samples/v2/settlement-batch/src/test/java/com/depgov/sample/settlement/SettlementBatchTest.java
EXIT=0
$ /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home/bin/java -cp /Users/coolguyhong/.codex/worktrees/smp01-s3/depgov-samples/v2/settlement-batch/build/verification/classes:/private/tmp/depgov-smp01/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.jar:/private/tmp/depgov-smp01/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.jar:/private/tmp/depgov-smp01/repository/org/apache/commons/commons-lang3/3.17.0/commons-lang3-3.17.0.jar:/private/tmp/depgov-smp01/repository/commons-io/commons-io/2.18.0/commons-io-2.18.0.jar:/private/tmp/depgov-smp01/repository/org/postgresql/postgresql/42.7.11/postgresql-42.7.11.jar:/private/tmp/depgov-smp01/repository/com/zaxxer/HikariCP/7.0.2/HikariCP-7.0.2.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-databind/2.18.2/jackson-databind-2.18.2.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-core/2.18.2/jackson-core-2.18.2.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-annotations/2.18.2/jackson-annotations-2.18.2.jar:/private/tmp/depgov-smp01/repository/org/junit/jupiter/junit-jupiter-api/5.11.4/junit-jupiter-api-5.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/jupiter/junit-jupiter-params/5.11.4/junit-jupiter-params-5.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/jupiter/junit-jupiter-engine/5.11.4/junit-jupiter-engine-5.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/platform/junit-platform-launcher/1.11.4/junit-platform-launcher-1.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/platform/junit-platform-engine/1.11.4/junit-platform-engine-1.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/platform/junit-platform-commons/1.11.4/junit-platform-commons-1.11.4.jar:/private/tmp/depgov-smp01/repository/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar:/private/tmp/depgov-smp01/repository/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar /Users/coolguyhong/.codex/worktrees/smp01-s3/depgov-samples/v2/settlement-batch/build/verification/JunitCheck.java

Test run finished after 355 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[        28 tests found           ]
[         0 tests skipped         ]
[        28 tests started         ]
[         0 tests aborted         ]
[        28 tests successful      ]
[         0 tests failed          ]

EXIT=0
$ /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home/bin/java -cp /Users/coolguyhong/.codex/worktrees/smp01-s3/depgov-samples/v2/settlement-batch/build/verification/classes:/private/tmp/depgov-smp01/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.jar:/private/tmp/depgov-smp01/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.jar:/private/tmp/depgov-smp01/repository/org/apache/commons/commons-lang3/3.17.0/commons-lang3-3.17.0.jar:/private/tmp/depgov-smp01/repository/commons-io/commons-io/2.18.0/commons-io-2.18.0.jar:/private/tmp/depgov-smp01/repository/org/postgresql/postgresql/42.7.11/postgresql-42.7.11.jar:/private/tmp/depgov-smp01/repository/com/zaxxer/HikariCP/7.0.2/HikariCP-7.0.2.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-databind/2.18.2/jackson-databind-2.18.2.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-core/2.18.2/jackson-core-2.18.2.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-annotations/2.18.2/jackson-annotations-2.18.2.jar:/private/tmp/depgov-smp01/repository/org/junit/jupiter/junit-jupiter-api/5.11.4/junit-jupiter-api-5.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/jupiter/junit-jupiter-params/5.11.4/junit-jupiter-params-5.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/jupiter/junit-jupiter-engine/5.11.4/junit-jupiter-engine-5.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/platform/junit-platform-launcher/1.11.4/junit-platform-launcher-1.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/platform/junit-platform-engine/1.11.4/junit-platform-engine-1.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/platform/junit-platform-commons/1.11.4/junit-platform-commons-1.11.4.jar:/private/tmp/depgov-smp01/repository/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar:/private/tmp/depgov-smp01/repository/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar com.depgov.sample.settlement.SettlementBatch input/transactions.csv input/config.json /private/tmp/depgov-smp01/s3/settlement-jdk17.json
SETTLEMENT  merchants=3 approved=6 gross=5033 fees=105 net=4928
EXIT=0
$ cmp expected/settlement.json /private/tmp/depgov-smp01/s3/settlement-jdk17.json
EXIT=0
$ /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home/bin/java -cp /Users/coolguyhong/.codex/worktrees/smp01-s3/depgov-samples/v2/settlement-batch/build/verification/classes:/private/tmp/depgov-smp01/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.jar:/private/tmp/depgov-smp01/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.jar:/private/tmp/depgov-smp01/repository/org/apache/commons/commons-lang3/3.17.0/commons-lang3-3.17.0.jar:/private/tmp/depgov-smp01/repository/commons-io/commons-io/2.18.0/commons-io-2.18.0.jar:/private/tmp/depgov-smp01/repository/org/postgresql/postgresql/42.7.11/postgresql-42.7.11.jar:/private/tmp/depgov-smp01/repository/com/zaxxer/HikariCP/7.0.2/HikariCP-7.0.2.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-databind/2.18.2/jackson-databind-2.18.2.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-core/2.18.2/jackson-core-2.18.2.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-annotations/2.18.2/jackson-annotations-2.18.2.jar:/private/tmp/depgov-smp01/repository/org/junit/jupiter/junit-jupiter-api/5.11.4/junit-jupiter-api-5.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/jupiter/junit-jupiter-params/5.11.4/junit-jupiter-params-5.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/jupiter/junit-jupiter-engine/5.11.4/junit-jupiter-engine-5.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/platform/junit-platform-launcher/1.11.4/junit-platform-launcher-1.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/platform/junit-platform-engine/1.11.4/junit-platform-engine-1.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/platform/junit-platform-commons/1.11.4/junit-platform-commons-1.11.4.jar:/private/tmp/depgov-smp01/repository/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar:/private/tmp/depgov-smp01/repository/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar com.depgov.sample.settlement.SettlementBatch
SETTLEMENT  merchants=3 approved=6 gross=5033 fees=105 net=4928
EXIT=0
$ cmp expected/settlement.json output/settlement.json
EXIT=0
```

추가 읽기/검증 명령:

```sh
/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home/bin/java -version
# exit 0: OpenJDK 17.0.11
/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home/bin/javap -classpath v2/settlement-batch/build/verification/classes -c -l -p com.depgov.sample.settlement.SettlementBatch > /private/tmp/depgov-smp01/s3/bytecode-jdk17.log
# exit 0
cmp svc-01/gradlew v2/settlement-batch/gradlew
cmp svc-01/gradlew.bat v2/settlement-batch/gradlew.bat
cmp svc-01/gradle/wrapper/gradle-wrapper.jar v2/settlement-batch/gradle/wrapper/gradle-wrapper.jar
cmp svc-01/gradle/wrapper/gradle-wrapper.properties v2/settlement-batch/gradle/wrapper/gradle-wrapper.properties
# 각 exit 0

git status --short --untracked-files=all
git diff --check -- v2/settlement-batch v2/S3_RESULT.md
# 각 exit 0
```

Python은 `/opt/homebrew/bin/python3 -I -B`로만 실행했다. 검증용 subprocess javac/java/cmp exit는 위 원문에 있다. 같은 Python으로 wrapper 동일성, 허용 경로, 금지 소스 참조 0, expected 합계를 검사했다(exit 0). 클래스 헤더를 읽어 main 5개 및 test 1개 모두 class major **61**(Java 17)임을 확인했다(exit 0).

테스트 범위: golden JSON 바이트 및 main 요약 한 줄, 거래별 HALF_UP·상한, 승인만 포함, 인용·한글·정렬, 0/100%·금액 0, 빈 결과, 헤더 오류 2건, 잘못된 설정 8건, CSV 오류 9건, 중복 ID, 합계 overflow, 파일/JSON 오류, CLI 인자 오류.

## 3. 사례 구현 위치와 다른 참조 없음

아래 주 소스 경로는 `v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java`다. 파일 상단 1~5행에 사례 ID와 사용 방식을 기재했다.

| 사례 | 선언 위치 | 구현 파일·줄 / 사용 방식 | 기대 분석 결과(실제 분석은 미실행) |
| --- | --- | --- | --- |
| C12 | Jackson 좌표 직접 선언 없음 | `v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:96` ObjectMapper 정적 타입 생성, `:97` `mapper.readTree(config.toFile())` 직접 호출. JsonNode 타입 참조도 `:98~103` | data-convert → jackson-databind 2.18.2, REFERENCED |
| C13 | `v2/settlement-batch/build.gradle:23`, **3.16.0** | `v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:58`, `:114` `StringUtils.isBlank` 정적 호출 | REFERENCED, 트리의 **3.16.0 -> 3.17.0**, versionMismatch |
| C14 | `v2/settlement-batch/build.gradle:24` | 주 소스 `:3`에 declaration-only 의도 기재. main/test 전체 소스에 드라이버/DriverManager 참조 0 | UNUSED_CANDIDATE / LOW |
| C15 | `v2/settlement-batch/build.gradle:25` | 주 소스 `:3`에 declaration-only 의도 기재. main/test 전체 소스에 해당 패키지 문자열 0, reflection/SPI 로딩도 없음 | UNUSED_CANDIDATE / HIGH |
| C16 | `v2/settlement-batch/build.gradle:26` | `v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:48` `FileUtils.readLines`, `:82` `FileUtils.writeStringToFile` 정적 호출 | REFERENCED, outOfBom |

공통 라이브러리 사용: `v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:49`, `:108` CsvLine.split; `:68` Money.roundHalfUp; `:82` JsonCodec.write; `:87` StringKit.pad. 모두 직접 정적 호출이다.

실제 rg(요구한 grep 확인에 사용) 명령과 결과:

```text
$ rg -n 'org\.postgresql|java\.sql\.DriverManager|com\.zaxxer' v2/settlement-batch/src
EXIT=1 (일치 없음; 참조 0)

$ rg -n 'jackson|toolchain|release' v2/settlement-batch/build.gradle
EXIT=1 (일치 없음; 참조 0)

$ rg -n 'com\.fasterxml|ObjectMapper|readTree|StringUtils\.|FileUtils\.|CsvLine\.|JsonCodec\.|Money\.|StringKit\.' v2/settlement-batch/src
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:1:// SMP-C12: transitive ObjectMapper.readTree direct typed call; no direct dependency declaration.
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:4:// SMP-C16: FileUtils.readLines / writeStringToFile static calls.
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:5:// Common APIs: CsvLine.split, JsonCodec.write, Money.roundHalfUp, StringKit.pad static calls.
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:12:import com.fasterxml.jackson.databind.JsonNode;
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:13:import com.fasterxml.jackson.databind.ObjectMapper;
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:48:        List<String> lines = FileUtils.readLines(input.toFile(), StandardCharsets.UTF_8);
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:49:        if (lines.isEmpty() || !HEADER.equals(CsvLine.split(lines.get(0)))) {
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:58:            if (StringUtils.isBlank(line)) {
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:68:            long fee = Money.roundHalfUp(transaction.amount(), Math.min(transaction.feeRate(), maxFeeRate));
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:82:        FileUtils.writeStringToFile(output.toFile(), JsonCodec.write(result) + "\n", StandardCharsets.UTF_8);
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:87:        return StringKit.pad("SETTLEMENT", 12)
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:96:        ObjectMapper mapper = new ObjectMapper();
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:97:        JsonNode root = mapper.readTree(config.toFile());
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:108:            List<String> fields = CsvLine.split(line);
v2/settlement-batch/src/main/java/com/depgov/sample/settlement/SettlementBatch.java:114:            if (StringUtils.isBlank(merchant) || StringUtils.isBlank(id)) {
EXIT=0

$ rg -n 'Class\.forName|ServiceLoader|java\.sql|javax\.sql' v2/settlement-batch/src
EXIT=1 (일치 없음; 참조 0)

$ rg -n 'org/postgresql|java/sql/DriverManager|com/zaxxer' /private/tmp/depgov-smp01/s3/bytecode-jdk17.log
EXIT=1 (일치 없음; 참조 0)

$ rg -n 'invoke.*(ObjectMapper|readTree|StringUtils|FileUtils|CsvLine|JsonCodec|Money|StringKit)' /private/tmp/depgov-smp01/s3/bytecode-jdk17.log
91:      14: invokestatic  #63                 // Method org/apache/commons/io/FileUtils.readLines:(Ljava/io/File;Ljava/nio/charset/Charset;)Ljava/util/List;
101:      43: invokestatic  #83                 // Method com/depgov/common/convert/CsvLine.split:(Ljava/lang/String;)Ljava/util/List;
136:     133: invokestatic  #116                // Method org/apache/commons/lang3/StringUtils.isBlank:(Ljava/lang/CharSequence;)Z
170:     215: invokestatic  #159                // Method com/depgov/common/util/Money.roundHalfUp:(JI)J
249:     427: invokestatic  #234                // Method com/depgov/common/convert/JsonCodec.write:(Ljava/lang/Object;)Ljava/lang/String;
252:     438: invokestatic  #241                // Method org/apache/commons/io/FileUtils.writeStringToFile:(Ljava/io/File;Ljava/lang/String;Ljava/nio/charset/Charset;)V
290:       4: invokestatic  #247                // Method com/depgov/common/util/StringKit.pad:(Ljava/lang/String;I)Ljava/lang/String;
317:       4: invokespecial #274                // Method com/fasterxml/jackson/databind/ObjectMapper."<init>":()V
322:      15: invokevirtual #275                // Method com/fasterxml/jackson/databind/ObjectMapper.readTree:(Ljava/io/File;)Lcom/fasterxml/jackson/databind/JsonNode;
367:       1: invokestatic  #83                 // Method com/depgov/common/convert/CsvLine.split:(Ljava/lang/String;)Ljava/util/List;
390:      57: invokestatic  #116                // Method org/apache/commons/lang3/StringUtils.isBlank:(Ljava/lang/CharSequence;)Z
393:      65: invokestatic  #116                // Method org/apache/commons/lang3/StringUtils.isBlank:(Ljava/lang/CharSequence;)Z
EXIT=0

```

C12/C13/C16 및 공통 API의 모든 소스 호출은 위 주 소스에만 있다. test에는 해당 외부 API의 추가 직접 참조가 없으며, C14/C15는 main/test 소스 및 주 클래스 javap 모두 참조가 없다. 바이트코드에서도 readTree는 invokevirtual, C13/C16/공통 API는 invokestatic으로 확인된다. method reference/reflection/SPI로 사용 방식을 바꾸지 않았다.

## 4. 실행 시나리오와 expected 비교

CSV 입력 8건(승인 6, 취소 1, 거절 1), config 상한 3%로 계산한다. 승인 없는 M004는 제외하고 가맹점을 정렬한다. 수수료는 **거래별**로 반올림한 뒤 더한다.

| 가맹점 | 승인 | 원금 | 수수료 | 정산액 |
| --- | ---: | ---: | ---: | ---: |
| M,003 | 1 | 333 | 10 | 323 |
| M001 | 2 | 2049 | 41 | 2008 |
| M002 | 3 | 2651 | 54 | 2597 |
| 전체 | 6 | 5033 | 105 | 4928 |

별도 **JDK 17 실행** stdout:

```text
SETTLEMENT  merchants=3 approved=6 gross=5033 fees=105 net=4928
```

출력 JSON(`settlement-jdk17.json`)과 expected는 cmp exit 0으로 바이트 동일하다. 같은 JDK 17 main을 인자 없이 실행한 `output/settlement.json`도 cmp exit 0으로 바이트 동일하다. JUnit에서도 main을 실제 호출해 JSON 바이트와 stdout의 한 줄 전체를 검증했다. 지정 Gradle run은 main 이전 실패했으므로 요청된 `/private/tmp/depgov-smp01/s3/settlement.json`의 Gradle 생성·비교는 미실행이다.

```json
{"merchants":[{"merchantId":"M,003","approvedTransactions":1,"grossAmount":333,"feeAmount":10,"netAmount":323},{"merchantId":"M001","approvedTransactions":2,"grossAmount":2049,"feeAmount":41,"netAmount":2008},{"merchantId":"M002","approvedTransactions":3,"grossAmount":2651,"feeAmount":54,"netAmount":2597}],"approvedTransactions":6,"grossAmount":5033,"feeAmount":105,"netAmount":4928}
```

## 5. runtimeClasspath 실제 출력

2절 dependencies 명령의 실제 stdout이다. task exit 0만으로 해석 성공을 판단하지 않았다. commons-lang3의 선언≠선택은 확인되며 **HikariCP 아래 SLF4J FAILED**는 남아 있다.

```text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.5/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build

> Task :dependencies

------------------------------------------------------------
Root project 'settlement-batch'
------------------------------------------------------------

runtimeClasspath - Runtime classpath of source set 'main'.
+--- com.depgov.common:data-convert:1.0.0
|    +--- org.apache.commons:commons-lang3:3.17.0
|    \--- com.fasterxml.jackson.core:jackson-databind:2.18.2
|         +--- com.fasterxml.jackson.core:jackson-annotations:2.18.2
|         |    \--- com.fasterxml.jackson:jackson-bom:2.18.2
|         |         +--- com.fasterxml.jackson.core:jackson-annotations:2.18.2 (c)
|         |         +--- com.fasterxml.jackson.core:jackson-core:2.18.2 (c)
|         |         \--- com.fasterxml.jackson.core:jackson-databind:2.18.2 (c)
|         +--- com.fasterxml.jackson.core:jackson-core:2.18.2
|         |    \--- com.fasterxml.jackson:jackson-bom:2.18.2 (*)
|         \--- com.fasterxml.jackson:jackson-bom:2.18.2 (*)
+--- com.depgov.common:common-util:1.0.0
|    \--- org.apache.commons:commons-lang3:3.17.0
+--- org.apache.commons:commons-lang3:3.16.0 -> 3.17.0
+--- org.postgresql:postgresql:42.7.11
+--- com.zaxxer:HikariCP:7.0.2
|    \--- org.slf4j:slf4j-api:2.0.17 FAILED
\--- commons-io:commons-io:2.18.0

(c) - A dependency constraint, not a dependency. The dependency affected by the constraint occurs elsewhere in the tree.
(*) - Indicates repeated occurrences of a transitive dependency subtree. Gradle expands transitive dependency subtrees only once per project; repeat occurrences only display the root of the subtree, followed by this annotation.

A web-based, searchable dependency report is available by adding the --scan option.

[Incubating] Problems report is available at: file:///Users/coolguyhong/.codex/worktrees/smp01-s3/depgov-samples/v2/settlement-batch/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14.5/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed
```

## 6. 미확보·미실행·amendment 요청

- **미확보**: `org.slf4j:slf4j-api:2.0.17`. 로컬 HikariCP 7.0.2 POM 51행의 `slf4j.version=2.0.17`, 106~107행의 전이 선언으로 확인했다. `/private/tmp/depgov-smp01/repository-gavs.txt`의 확보 버전은 1.7.36 / 2.0.13 / 2.0.18이다. 외부 다운로드나 합성 POM으로 보완하지 않았다.
- **미실행**: 의존성 해석을 통과한 지정 Gradle compile/test/run, Gradle 경로의 기본 output/settlement.json 생성, depgov 분석기 전체 파이프라인. 별도 JDK 17 소스 컴파일·JUnit 28건·명시 경로 및 기본 경로 시나리오가 성공했다.
- **amendment 요청(미적용, 응답 대기)**: HikariCP 선언과 전이 경로를 유지하면서 dependencies 내부 `constraints { implementation 'org.slf4j:slf4j-api:2.0.18' }`를 추가하도록 허용하거나, Claude 소유 공급 절차에서 2.0.17을 확보할 것. 보완 후 지정 build/run/expected 비교/dependencies 및 JDK 17 Gradle 검증을 재실행해야 한다. 이 세션에서 계약 밖 좌표를 직접 선언하거나 전이를 exclude하지 않았다.
- 참고: common-util은 1.0.0 API인 StringKit.pad를 사용한다. 공통 라이브러리 1.1.0 변경이나 다른 표본/정답표 수정은 이 작업 범위에 포함하지 않았다.

## 7. 보호 규칙 준수

- 외부 host 접속 **0**, 다운로드 **0**. web/curl/네트워크 도구를 쓰지 않았고 Gradle은 지정 실행기의 offline 로컬 mirror만 사용했다. 설정 파일에 요구된 저장소 URL이 존재하는 것과 실제 접속은 구분했다.
- Gradle 직접 실행 **0**. 지정 samplegradle.sh만 호출했으며 task s3 전용 GRADLE_USER_HOME을 사용했다. 모든 호출에 실행기가 `--offline --no-daemon`을 강제했다. 로그의 single-use 프로세스는 실행기가 JVM 옵션을 맞추기 위해 생성·종료한 Gradle 프로세스이며 상주 daemon은 요청하지 않았다. 사용자 ~/.gradle / ~/.depgov-gradle, 다른 task 캐시는 사용하지 않았다.
- git 쓰기 **0**. rev-parse/status/diff 읽기만 수행했고 add/commit/checkout/reset/clean/stash는 실행하지 않았다.
- 허용 범위 밖 저장소 파일 수정 **0**. 변경 파일은 위 14개이며 v2/libs/**, v2/maven-repo/**, svc-01/, svc-02/, bom/은 읽기만 했다.
- docker/kubectl/gcloud, DB/MQ, host 5432 접속 **0**. .env 및 git 원격 URL을 읽지 않았으며 비밀값·토큰을 출력/저장하지 않았다.
- 기존 assertion 완화·삭제·skip **0**. 테스트 소스는 신규이며 별도 JDK 17 검증의 skip은 0이다.

상태: **Claude 검증 대기** (Gradle 의존 공급 보완 필요)
