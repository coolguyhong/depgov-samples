# SMP-01 S6 — legacy-report 구현 결과

상태: **Claude 검증 대기**

## 1. 기준과 범위

- 기준 commit: `c86f2b4ee1ad1a6411f7fc0f1d22208940d49041` (`git rev-parse HEAD`, exit 0).
- worktree: `/Users/coolguyhong/.codex/worktrees/smp01-s6/depgov-samples`, branch `codex/smp01-s6`, task `s6`. 시작 작업 트리는 깨끗했다.
- 계약: `/Users/coolguyhong/workspace/depgov/docs/implementation/SMP-01_PLAN.md` 1~4절, svc-01/svc-02/bom, `v2/libs/README.md`를 읽었다.
- 사용자 작업 지시가 계획의 `--release 8` Gradle 설정보다 우선한다. build.gradle에는 `sourceCompatibility = '1.8'`, `targetCompatibility = '1.8'`만 두었고 toolchain/`options.release`는 없다. 추가 API 검사는 별도 임시 디렉터리로 standalone javac를 실행했다.
- depgov 등록 계약은 `targetPath=v2/legacy-report`, `buildJdkMajor=8`. 이 세션은 서비스 등록·G lane 실행을 하지 않았다. 로컬 빌드/실행 JVM은 지정 스크립트의 Temurin JDK 21이다.

신규 파일 15개이며 기존 파일 수정은 없다. 아래 경로는 저장소 루트 기준이다.

| 파일 | 내용 |
| --- | --- |
| `v2/legacy-report/settings.gradle` | 단일 root 프로젝트명 legacy-report |
| `v2/legacy-report/build.gradle` | 문자열 리터럴 의존 선언, application, source/target 1.8, JUnit Platform |
| `v2/legacy-report/.gitignore` | build/, .gradle/, 기본 출력 output/ 제외 |
| `v2/legacy-report/gradlew` | svc-01에서 바이트 동일 복사, 실행 권한 유지 |
| `v2/legacy-report/gradlew.bat` | svc-01에서 바이트 동일 복사 |
| `v2/legacy-report/gradle/wrapper/gradle-wrapper.jar` | svc-01에서 바이트 동일 복사 |
| `v2/legacy-report/gradle/wrapper/gradle-wrapper.properties` | svc-01의 Gradle 8.14.5 설정 그대로 복사 |
| `v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java` | C25~C27 정적 호출, 월별 집계·반올림·출력, C28 참조 없음 |
| `v2/legacy-report/src/main/java/com/depgov/sample/legacy/SalesRecord.java` | 26자 고정폭 및 실제 달력 날짜 검증 |
| `v2/legacy-report/src/main/resources/log4j2.simplelog.properties` | log4j-api SimpleLogger의 감사 INFO 출력 활성화 |
| `v2/legacy-report/src/test/java/com/depgov/sample/legacy/LegacyReportTest.java` | Java 8 문법으로 16 testcase 검증 |
| `v2/legacy-report/input/sales.txt` | 월·지점 순서를 섞은 고정폭 6건 |
| `v2/legacy-report/expected/report.txt` | 수작업 계산값 기준 고정 기대 보고서, UTF-8/LF 302 bytes |
| `v2/legacy-report/README.md` | 입출력 계약, 실행법, 사례 및 실제 JDK 8 검증 경계 |
| `v2/S6_RESULT.md` | 이번 구현·실행·호환성 근거 및 검증 대기 상태 |

## 2. 실제 실행 명령과 exit

아래 세 가지가 이번 세션에서 실행한 Gradle 명령 전부이며 각각 한 번 실행했다. stdout/stderr는 `/private/tmp/depgov-smp01/s6/{build,run,dependencies}.log`에 보관했다. Gradle은 지정 실행기만 사용했다.

| 명령 | exit | 결과 |
| --- | --- | --- |
| `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s6 v2/legacy-report build` | 0 | BUILD SUCCESSFUL, 8 actionable tasks executed |
| `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s6 v2/legacy-report run --args='input/sales.txt /private/tmp/depgov-smp01/s6/report.txt'` | 0 | BUILD SUCCESSFUL, 감사 이벤트 1줄 |
| `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s6 v2/legacy-report dependencies --configuration runtimeClasspath` | 0 | 아래 runtimeClasspath 원문 |
| `cmp v2/legacy-report/expected/report.txt /private/tmp/depgov-smp01/s6/report.txt` | 0 | 바이트 동일 |
| `rg -c 'REPORT_CREATED' /private/tmp/depgov-smp01/s6/run.log` | 0 | `1` |
| `rg -n 'com\.fasterxml' v2/legacy-report/src` | 1 | main/test/resource 전체 0건; 의도한 미참조 |
| `rg -n 'toolchain\|options\.release\|junit:junit\|log4j-core\|slf4j-api:2\.' v2/legacy-report/build.gradle` | 1 | 금지 선언 0건 |
| `rg -n 'com/fasterxml\|com\.fasterxml' /private/tmp/depgov-smp01/s6/bytecode.log` | 1 | LegacyReport 역어셈블 결과 0건 |
| `git diff --check -- v2/legacy-report v2/S6_RESULT.md` | 0 | tracked diff 오류 없음; 신규 텍스트는 별도 Python whitespace 검사도 통과 |
| `git status --short --branch` 및 `git status --short --untracked-files=all -- v2/legacy-report v2/S6_RESULT.md` | 0 | 지정 branch, 허용 경로 신규 파일만 확인 |

추가로 실행한 검사 명령:

- `cmp svc-01/<p> v2/legacy-report/<p>`를 wrapper 네 파일 각각에 실행: 모두 exit 0. Python 바이트 비교/SHA-256 검사도 exit 0.
- `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/javap -v -classpath <jar> <대표 클래스>`를 아래 7개 jar에 각각 실행: 모두 exit 0. C27 사전 검사로 같은 LogManager 명령을 한 번 더 실행했다.
- `/usr/bin/unzip -p /private/tmp/depgov-smp01/repository/org/apache/logging/log4j/log4j-api/2.25.4/log4j-api-2.25.4.jar META-INF/MANIFEST.MF` 및 manifest 필드 검색: exit 0.
- `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/javap -v <class 경로>`를 아래 Gradle main 클래스 4개에 각각 실행: 모두 exit 0.
- `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/javap -c -p -classpath v2/legacy-report/build/classes/java/main com.depgov.sample.legacy.LegacyReport > /private/tmp/depgov-smp01/s6/bytecode.log`: exit 0. 정적 호출 검색 exit 0.
- 같은 javap의 `-c -p -classpath <log4j-api jar>`로 `org.apache.logging.log4j.simple.SimpleLoggerContext`와 `org.apache.logging.log4j.simple.internal.SimpleProvider$Config`를 각각 읽고 내장 로거의 설정 파일명/키를 확인: exit 0.
- `/opt/homebrew/bin/python3 -I -B -`의 inline 검사: 입력 26자 확인, JUnit XML 집계, jar manifest/모든 class header 검사, main/test source API 검사, wrapper/텍스트 whitespace 검사 각각 exit 0. API 검사에서 실행한 실제 javac 명령은 5절에 그대로 기재했다.
- 소스 정적 호출 검색(`rg -n 'StringKit\.pad|Money\.roundHalfUp|LoggerFactory\.getLogger|LogManager\.getLogger|AUDIT\.info|StringUtils\.(leftPad|rightPad)' v2/legacy-report/src/main`) exit 0. `nl -ba`로 소스/build.gradle 줄 번호 확인 exit 0.
- 읽기 전용 준비 명령 `pwd`, `git rev-parse HEAD`, `git status --short`, `cat`, `sed -n`, `rg --files`, `ls`로 계약/표본/API/로컬 좌표/실행기를 확인: exit 0. AGENTS.md 검색에서 파일 없음인 `rg --files -g AGENTS.md ... .`는 exit 1(적용되는 프로젝트 지침 없음); 상위 빈 `~/.codex/AGENTS.md`도 확인했다.
- 파일 생성 준비는 허용 경로 `mkdir -p`, wrapper `cp`만 사용: exit 0. 나머지 텍스트는 apply_patch 및 지정 Python으로 생성했다.
- 결과 문서 생성 후 첫 whitespace assertion은 원문 로그의 줄 끝 공백 때문에 exit 1이었다. 문서의 줄 끝 공백을 정리하고 재검사했다. 애플리케이션/테스트/expected는 변경하지 않았다.
- 첫 미참조 grep 집계 shell은 `status`가 zsh 읽기 전용 변수여서 exit 1이었다. 파일/빌드 영향 없이 `source_grep_exit`로 바꿔 재검사 shell exit 0, 실제 grep exit 1(0건)을 확인했다. 광범위 Java 문법 grep의 `record` 일치는 일반 변수명/문자열이며 별도 Java 8 컴파일은 통과했다.

| 모듈 / suite | testcase | failures | errors | skipped |
| --- | ---: | ---: | ---: | ---: |
| root legacy-report / `com.depgov.sample.legacy.LegacyReportTest` | 16 | 0 | 0 | 0 |

근거: `v2/legacy-report/build/test-results/test/TEST-com.depgov.sample.legacy.LegacyReportTest.xml`. 테스트는 기준 보고서 바이트 일치·상위 폴더 생성, 연도 경계 정렬·월 단위 반올림, 빈 입력, 윤일/0원, 잘못된 입력 8종, 기존 보고서 보존/오류 줄 번호, 누락 입력, CLI 경로, 잘못된 인수 수를 검증했다. 모든 테스트 소스도 Java 8이다.

빌드 로그의 source/target 8 obsolete 경고는 JDK 21 javac 안내다. FSEvents stream 시작 실패 메시지는 빌드 성공에 영향이 없었다. 지정 실행기는 `--offline --no-daemon`을 사용하며 공급된 JVM 설정으로 Gradle이 일회성 JVM을 띄웠다가 종료한다. 지속 daemon 실행 옵션을 추가하지 않았다.

## 3. C25~C28 구현 위치와 grep/바이트코드

경로의 줄 번호는 이번 최종 소스 기준이다. 모든 사례 주석은 `LegacyReport.java:1`~`:4`에 있다.

| 사례 | 선언 위치 | 구현 위치와 사용 방식 | 기대 판정 |
| --- | --- | --- | --- |
| C25 | `v2/legacy-report/build.gradle:22` | `src/main/java/com/depgov/sample/legacy/LegacyReport.java:60`, `:86` StringKit.pad 정적 호출, `:70` Money.roundHalfUp 정적 호출 | REFERENCED, common-util 1.0.0 / Java 8 |
| C26 | `v2/legacy-report/build.gradle:23` | 같은 파일 `:25` LoggerFactory.getLogger 정적 호출, `:43`, `:82` 운영 로그 | REFERENCED, 선택 1.7.36, BOM 2.0.16과 versionMismatch |
| C27 | `v2/legacy-report/build.gradle:24` | 같은 파일 `:26` LogManager.getLogger 정적 호출, `:80` 별도 감사 INFO 1회; `src/main/resources/log4j2.simplelog.properties:2` INFO 활성화 | REFERENCED, outOfBom; log4j-core 없음 |
| C28 | `v2/legacy-report/build.gradle:25` | 선언만, `src/` 전체 `com.fasterxml` 0건 | UNUSED_CANDIDATE / MEDIUM(spi), 참조 0 |
| 보조 | `v2/legacy-report/build.gradle:26` | 같은 파일 `:61` StringUtils.rightPad, `:62`, `:87` 등 leftPad 정적 호출 | commons-lang3 3.17.0 REFERENCED |

소스 grep 결과:

```text
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:1:// SMP-C25: StringKit.pad / Money.roundHalfUp static calls (Java 8 common-util).
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:2:// SMP-C26: LoggerFactory.getLogger static call; SLF4J 1.7.36 stays selected.
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:3:// SMP-C27: LogManager.getLogger static call; one separate audit event per report.
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:25:    private static final Logger LOG = LoggerFactory.getLogger(LegacyReport.class);
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:26:    private static final org.apache.logging.log4j.Logger AUDIT = LogManager.getLogger("legacy.audit");
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:60:        report.append(StringKit.pad("MONTH", 7)).append(' ')
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:61:                .append(StringUtils.rightPad("RECORDS", 7)).append(' ')
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:62:                .append(StringUtils.leftPad("SALES", 12)).append(' ')
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:63:                .append(StringUtils.leftPad("TAX", 12)).append(' ')
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:64:                .append(StringUtils.leftPad("TOTAL", 12)).append('\n');
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:70:            long monthlyTax = Money.roundHalfUp(sales.amount, 10);
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:80:        AUDIT.info("REPORT_CREATED records={} months={} sales={} output={}",
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:86:        report.append(StringKit.pad(month, 7)).append(' ')
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:87:                .append(StringUtils.leftPad(Long.toString(count), 7)).append(' ')
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:88:                .append(StringUtils.leftPad(Long.toString(amount), 12)).append(' ')
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:89:                .append(StringUtils.leftPad(Long.toString(tax), 12)).append(' ')
v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java:90:                .append(StringUtils.leftPad(Long.toString(Math.addExact(amount, tax)), 12)).append('\n');
```

정적 호출 바이트코드 확인(`invokestatic`):

```text
     185: invokestatic  #153                // Method com/depgov/common/util/StringKit.pad:(Ljava/lang/String;I)Ljava/lang/String;
     200: invokestatic  #164                // Method org/apache/commons/lang3/StringUtils.rightPad:(Ljava/lang/String;I)Ljava/lang/String;
     215: invokestatic  #171                // Method org/apache/commons/lang3/StringUtils.leftPad:(Ljava/lang/String;I)Ljava/lang/String;
     230: invokestatic  #171                // Method org/apache/commons/lang3/StringUtils.leftPad:(Ljava/lang/String;I)Ljava/lang/String;
     245: invokestatic  #171                // Method org/apache/commons/lang3/StringUtils.leftPad:(Ljava/lang/String;I)Ljava/lang/String;
     320: invokestatic  #203                // Method com/depgov/common/util/Money.roundHalfUp:(JI)J
       4: invokestatic  #153                // Method com/depgov/common/util/StringKit.pad:(Ljava/lang/String;I)Ljava/lang/String;
      21: invokestatic  #171                // Method org/apache/commons/lang3/StringUtils.leftPad:(Ljava/lang/String;I)Ljava/lang/String;
      39: invokestatic  #171                // Method org/apache/commons/lang3/StringUtils.leftPad:(Ljava/lang/String;I)Ljava/lang/String;
      57: invokestatic  #171                // Method org/apache/commons/lang3/StringUtils.leftPad:(Ljava/lang/String;I)Ljava/lang/String;
      80: invokestatic  #171                // Method org/apache/commons/lang3/StringUtils.leftPad:(Ljava/lang/String;I)Ljava/lang/String;
       2: invokestatic  #275                // Method org/slf4j/LoggerFactory.getLogger:(Ljava/lang/Class;)Lorg/slf4j/Logger;
      11: invokestatic  #283                // Method org/apache/logging/log4j/LogManager.getLogger:(Ljava/lang/String;)Lorg/apache/logging/log4j/Logger;
```


C28 확인 명령은 `rg -n 'com\.fasterxml' v2/legacy-report/src`: stdout 없음, exit 1. 메인 클래스 역어셈블 결과에도 `com/fasterxml` 및 `com.fasterxml` 일치 없음. Jackson은 로깅/입력 처리/테스트 어디에도 사용하지 않는다. SLF4J에 다른 버전을 유입하는 의존성도 없다.

## 4. 실행 출력과 expected 비교

입력 6건은 날짜 8자+지점 6자+금액 12자로 모두 26자다. 월별 세액은 각 월 합산 매출의 10% HALF_UP이며 마지막 행은 월별 세액을 합산한다. expected는 고정된 계산값으로 작성했고 실행 산출물을 복사하지 않았다.

지정 run 명령 전체 출력:

```text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.5/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build

> Task :compileJava UP-TO-DATE
Caught exception: Could not start the FSEvents stream: /Users/coolguyhong/.codex/worktrees/smp01-s6/depgov-samples/v2/legacy-report

> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE

> Task :run
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
2026-10-26T01:23:42.871115Z main ERROR Log4j API could not find a logging provider.
INFO legacy.audit REPORT_CREATED records=6 months=3 sales=4230 output=/private/tmp/depgov-smp01/s6/report.txt
Report written: /private/tmp/depgov-smp01/s6/report.txt

[Incubating] Problems report is available at: file:///Users/coolguyhong/.codex/worktrees/smp01-s6/depgov-samples/v2/legacy-report/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14.5/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 2s
3 actionable tasks: 1 executed, 2 up-to-date
```

산출 보고서:

```text
MONTHLY SALES REPORT (KRW)
MONTH   RECORDS        SALES          TAX        TOTAL
2026-01       3         1215          122         1337
2026-02       2         3000          300         3300
2026-03       1           15            2           17
TOTAL         6         4230          424         4654
```


`cmp` exit 0, 예상 파일/실제 파일 모두 302 bytes이며 SHA-256은 `5066b23d132e315f82c786e1d0a78b843ae8d4f64dcf213a226cd4fbf09c0c14`다. `rg -c 'REPORT_CREATED' .../run.log` 출력은 `1`이다. SLF4J NOP/Log4j provider 부재 안내는 바인딩/core를 추가하지 않는 계약에 따른 시작 메시지이며 보고서와 감사 INFO 출력은 정상이다. 로그에 출력된 URL을 방문하지 않았다.

## 5. class major와 Java 8 호환성 근거

javap는 전부 `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/javap`를 사용했다.

Gradle의 `build/classes/java/main` 아래 파일 전부를 `javap -v`로 확인했다:

```text
v2/legacy-report/build/classes/java/main/com/depgov/sample/legacy/LegacyReport$1.class: major version: 52
v2/legacy-report/build/classes/java/main/com/depgov/sample/legacy/LegacyReport$MonthlySales.class: major version: 52
v2/legacy-report/build/classes/java/main/com/depgov/sample/legacy/LegacyReport.class: major version: 52
v2/legacy-report/build/classes/java/main/com/depgov/sample/legacy/SalesRecord.class: major version: 52
```


Java 8 private nested class 접근용으로 javac가 생성한 `$1.class`도 포함한 총 4개 모두 major **52**다. `build/classes/java/test/.../LegacyReportTest.class`도 header major 52다.

| 런타임 jar | 대표 클래스 | 대표 major | 모든 기본 실행 class 수 / 최대 major | 추가 근거 |
| --- | --- | ---: | --- | --- |
| common-util 1.0.0 | `com.depgov.common.util.StringKit` | 52 | 2 / 52 | Money도 기본 class 52 |
| slf4j-api 1.7.36 | `org.slf4j.LoggerFactory` | 49 | 34 / 49 | Build-Jdk-Spec 1.8, Java 8 이하 bytecode |
| log4j-api 2.25.4 | `org.apache.logging.log4j.LogManager` | 52 | 209 / 52 | manifest OSGi JavaSE 1.8, Multi-Release true |
| commons-lang3 3.17.0 | `org.apache.commons.lang3.StringUtils` | 52 | 395 / 52 | manifest OSGi JavaSE 1.8 |
| jackson-databind 2.18.2 | `com.fasterxml.jackson.databind.ObjectMapper` | 52 | 789 / 52 | manifest OSGi JavaSE 1.8, Build-Jdk-Spec 1.8 |
| jackson-core 2.18.2（전이） | `com.fasterxml.jackson.core.JsonFactory` | 52 | 211 / 52 | manifest OSGi JavaSE 1.8, Build-Jdk-Spec 1.8 |
| jackson-annotations 2.18.2（전이） | `com.fasterxml.jackson.annotation.JsonProperty` | 50 | 73 / 50 | manifest OSGi JavaSE 1.6, Build-Jdk-Spec 1.8 |

`META-INF/versions/**`는 Java 8에서 선택하지 않는 Multi-Release 계층이므로 기본 실행 클래스와 분리했다. 루트 `module-info.class`도 Java 8 실행 클래스가 아닌 Java 9 모듈 기술자다. log4j-api의 기본 클래스 전부 52 및 manifest JavaSE 1.8 요구를 확인했으므로 C27을 유지한다. Build-Jdk-Spec 17은 제작 JDK이며 최소 실행 JDK를 뜻하지 않는다. Jackson core의 Java 11~22 계층도 Java 8에서 선택되지 않는다.

다음은 실제 검사 원문이다. class header의 6~7 byte를 big-endian major로 읽어 모든 기본 실행 클래스가 `<=52`인지 assert했고, 대표 클래스는 별도로 javap를 호출했다. 저장소 위치도 함께 기록한다.

```text
JAR: /private/tmp/depgov-smp01/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.jar
REPRESENTATIVE: com.depgov.common.util.StringKit
major version: 52
BASE_CLASSES: 2 MIN_MAJOR: 52 MAX_MAJOR: 52
MR_CLASSES: 0 MAJORS: [] BASE_MODULE_DESCRIPTORS: []

JAR: /private/tmp/depgov-smp01/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar
REPRESENTATIVE: org.slf4j.LoggerFactory
major version: 49
MANIFEST: Build-Jdk-Spec: 1.8
BASE_CLASSES: 34 MIN_MAJOR: 49 MAX_MAJOR: 49
MR_CLASSES: 0 MAJORS: [] BASE_MODULE_DESCRIPTORS: []

JAR: /private/tmp/depgov-smp01/repository/org/apache/logging/log4j/log4j-api/2.25.4/log4j-api-2.25.4.jar
REPRESENTATIVE: org.apache.logging.log4j.LogManager
major version: 52
MANIFEST: Build-Jdk-Spec: 17
MANIFEST: Multi-Release: true
MANIFEST: Require-Capability: osgi.extender;filter:="(&(osgi.extender=osgi.serviceloader.processor)(version>=1.0.0)(!(version>=2.0.0)))";resolution:=optional,osgi.extender;filter:="(&(osgi.extender=osgi.serviceloader.registrar)(version>=1.0.0)(!(version>=2.0.0)))";resolution:=optional,osgi.serviceloader;filter:="(osgi.serviceloader=org.apache.logging.log4j.message.ThreadDumpMessage$ThreadInfoFactory)";osgi.serviceloader="org.apache.logging.log4j.message.ThreadDumpMessage$ThreadInfoFactory";cardinality:=single;resolution:=optional,osgi.serviceloader;filter:="(osgi.serviceloader=org.apache.logging.log4j.spi.Provider)";osgi.serviceloader="org.apache.logging.log4j.spi.Provider";cardinality:=multiple;resolution:=optional,osgi.serviceloader;filter:="(osgi.serviceloader=org.apache.logging.log4j.util.PropertySource)";osgi.serviceloader="org.apache.logging.log4j.util.PropertySource";cardinality:=multiple;resolution:=optional,osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"
BASE_CLASSES: 209 MIN_MAJOR: 52 MAX_MAJOR: 52
MR_CLASSES: 4 MAJORS: [53] BASE_MODULE_DESCRIPTORS: [53]

JAR: /private/tmp/depgov-smp01/repository/org/apache/commons/commons-lang3/3.17.0/commons-lang3-3.17.0.jar
REPRESENTATIVE: org.apache.commons.lang3.StringUtils
major version: 52
MANIFEST: Build-Jdk-Spec: 17
MANIFEST: Require-Capability: osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"
MANIFEST: Multi-Release: true
BASE_CLASSES: 395 MIN_MAJOR: 52 MAX_MAJOR: 52
MR_CLASSES: 1 MAJORS: [53] BASE_MODULE_DESCRIPTORS: []

JAR: /private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-databind/2.18.2/jackson-databind-2.18.2.jar
REPRESENTATIVE: com.fasterxml.jackson.databind.ObjectMapper
major version: 52
MANIFEST: Require-Capability: osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"
MANIFEST: Multi-Release: true
MANIFEST: Build-Jdk-Spec: 1.8
BASE_CLASSES: 789 MIN_MAJOR: 52 MAX_MAJOR: 52
MR_CLASSES: 1 MAJORS: [53] BASE_MODULE_DESCRIPTORS: []

JAR: /private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-core/2.18.2/jackson-core-2.18.2.jar
REPRESENTATIVE: com.fasterxml.jackson.core.JsonFactory
major version: 52
MANIFEST: Require-Capability: osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"
MANIFEST: Multi-Release: true
MANIFEST: Build-Jdk-Spec: 1.8
BASE_CLASSES: 211 MIN_MAJOR: 52 MAX_MAJOR: 52
MR_CLASSES: 10 MAJORS: [53, 55, 61, 65, 66] BASE_MODULE_DESCRIPTORS: []

JAR: /private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-annotations/2.18.2/jackson-annotations-2.18.2.jar
REPRESENTATIVE: com.fasterxml.jackson.annotation.JsonProperty
major version: 50
MANIFEST: Require-Capability: osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.6))"
MANIFEST: Build-Jdk-Spec: 1.8
BASE_CLASSES: 73 MIN_MAJOR: 50 MAX_MAJOR: 50
MR_CLASSES: 0 MAJORS: [] BASE_MODULE_DESCRIPTORS: [53]
```


Java 9+ JDK API 유입은 source/target만으로 차단되지 않으므로 main/test 소스 전체를 다음 standalone javac 명령으로 추가 컴파일했다(exit 0). Gradle을 별도로 직접 실행한 것이 아니며 Gradle DSL에 release 설정을 추가하지 않았다. 출력은 임시 디렉터리이고 Gradle 산출물의 major 검사를 대체하지 않는다.

```sh
/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/javac --release 8 -encoding UTF-8 -classpath /private/tmp/depgov-smp01/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.jar:/private/tmp/depgov-smp01/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar:/private/tmp/depgov-smp01/repository/org/apache/logging/log4j/log4j-api/2.25.4/log4j-api-2.25.4.jar:/private/tmp/depgov-smp01/repository/org/apache/commons/commons-lang3/3.17.0/commons-lang3-3.17.0.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-databind/2.18.2/jackson-databind-2.18.2.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-core/2.18.2/jackson-core-2.18.2.jar:/private/tmp/depgov-smp01/repository/com/fasterxml/jackson/core/jackson-annotations/2.18.2/jackson-annotations-2.18.2.jar:/private/tmp/depgov-smp01/repository/org/junit/jupiter/junit-jupiter-api/5.11.4/junit-jupiter-api-5.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/jupiter/junit-jupiter-params/5.11.4/junit-jupiter-params-5.11.4.jar:/private/tmp/depgov-smp01/repository/org/junit/platform/junit-platform-commons/1.11.4/junit-platform-commons-1.11.4.jar:/private/tmp/depgov-smp01/repository/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar:/private/tmp/depgov-smp01/repository/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar -d /private/tmp/depgov-smp01/s6/java8-api-check v2/legacy-report/src/main/java/com/depgov/sample/legacy/LegacyReport.java v2/legacy-report/src/main/java/com/depgov/sample/legacy/SalesRecord.java v2/legacy-report/src/test/java/com/depgov/sample/legacy/LegacyReportTest.java
```


## 6. runtimeClasspath 원문

지정 `dependencies --configuration runtimeClasspath` 명령 출력 전체:

```text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.5/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build

> Task :dependencies

------------------------------------------------------------
Root project 'legacy-report'
------------------------------------------------------------

runtimeClasspath - Runtime classpath of source set 'main'.
+--- com.depgov.common:common-util:1.0.0
|    \--- org.apache.commons:commons-lang3:3.17.0
+--- org.slf4j:slf4j-api:1.7.36
+--- org.apache.logging.log4j:log4j-api:2.25.4
+--- com.fasterxml.jackson.core:jackson-databind:2.18.2
|    +--- com.fasterxml.jackson.core:jackson-annotations:2.18.2
|    |    \--- com.fasterxml.jackson:jackson-bom:2.18.2
|    |         +--- com.fasterxml.jackson.core:jackson-annotations:2.18.2 (c)
|    |         +--- com.fasterxml.jackson.core:jackson-core:2.18.2 (c)
|    |         \--- com.fasterxml.jackson.core:jackson-databind:2.18.2 (c)
|    +--- com.fasterxml.jackson.core:jackson-core:2.18.2
|    |    \--- com.fasterxml.jackson:jackson-bom:2.18.2 (*)
|    \--- com.fasterxml.jackson:jackson-bom:2.18.2 (*)
\--- org.apache.commons:commons-lang3:3.17.0

(c) - A dependency constraint, not a dependency. The dependency affected by the constraint occurs elsewhere in the tree.
(*) - Indicates repeated occurrences of a transitive dependency subtree. Gradle expands transitive dependency subtrees only once per project; repeat occurrences only display the root of the subtree, followed by this annotation.

A web-based, searchable dependency report is available by adding the --scan option.

[Incubating] Problems report is available at: file:///Users/coolguyhong/.codex/worktrees/smp01-s6/depgov-samples/v2/legacy-report/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14.5/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed
```


SLF4J는 1.7.36 한 버전만 선택되었다. log4j-core 및 다른 SLF4J 버전은 없다. commons-lang3은 직접 선언과 common-util 경유로 같은 3.17.0이다. Jackson BOM은 해당 라이브러리 metadata에서 전이로 공급된 것이며 이 프로젝트에 platform 선언은 없다. 오프라인 commons-lang3 3.17.0은 jar-only metadata로 해석되는 leaf artifact이며 이번 runtime 트리에서 누락되는 자체 전이는 없다.

## 7. 미확보·미실행·amendment 및 보호 규칙

- 필요한 좌표 미확보: 없음. C25~C28 전부 구현했다. JUnit 4.13.2는 공급되지 않은 좌표이므로 요청대로 선언·사용하지 않았다.
- 실제 JDK 8 javac 빌드/실행: 미실행(로컬 JDK 8 없음). 이 결과는 JDK 21 로컬 빌드, class 52, Java 8 API 추가 컴파일, 의존 jar 기본 class/manifest 확인까지다. 실제 JDK 8 성공 판정은 Claude의 G lane 후속 검증 대상이다.
- depgov 등록, 전체 분석 pipeline, UI/OSV/GKE 실행: 미실행(이번 구현 범위 밖). 정답 판정은 계약상의 기대값이며 분석기 실측값으로 주장하지 않는다.
- amendment 요청: 없음. 사용자의 source/target 설정 지시를 그대로 적용했다.
- 허용 경로 외 저장소 파일 변경 0. `v2/libs/**`, `v2/maven-repo/**`, svc-01/svc-02/bom은 읽기만 했다. wrapper만 svc-01에서 허용 경로로 복사했고 4/4 바이트 동일하다.
- 외부 host 접속 **0**, 다운로드 **0**. 로컬 artifact/배포판과 오프라인 전용 home `/private/tmp/depgov-smp01/s6/gradle-home`만 사용했다. `~/.gradle`, `~/.depgov-gradle` 사용 없음.
- git add/commit/checkout/reset/clean/stash 등 **git 쓰기 0**. 읽기 전용 git 상태/HEAD/diff 확인만 수행했다.
- docker/kubectl/gcloud, DB/MQ, 포트 5432 접속 **0**. `.env` 읽기 0, 비밀값/토큰 출력·저장 0. Python은 지정한 `/opt/homebrew/bin/python3 -I -B`만 사용했다.

최종 상태: **Claude 검증 대기**.
