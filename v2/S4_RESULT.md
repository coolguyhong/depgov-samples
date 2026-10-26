# SMP-01 S4 구현 결과

상태: **Claude 검증 대기**

## 1. 기준과 변경 파일

- 기준 commit: `1d5e8a383ea122dc2616f8996c532bf8e8a66ec0` (`git rev-parse HEAD` 실제 출력).
- 작업 worktree: `/Users/coolguyhong/.codex/worktrees/smp01-s4/depgov-samples`, 지정 branch `codex/smp01-s4`, task-id `s4`.
- 구현 범위: Java 21 단일 모듈 `v2/partner-gateway`, C17~C20.
- 사전 확인: `SMP-01_PLAN.md` 1~4절, `svc-01/`, `svc-02/`, `bom/`, `v2/libs/README.md`, `PartnerClient`/`PartnerException`/`JsonCodec` 소스. 해당 파일은 읽기만 했다.

모든 제출 파일은 신규다. 아래 경로는 저장소 루트 기준이다.

| 파일 | 변경 내용 |
| --- | --- |
| `v2/partner-gateway/settings.gradle` | rootProject.name 고정 |
| `v2/partner-gateway/build.gradle` | Java/application, Java 21 release, 지정 좌표 6개, JUnit Platform |
| `v2/partner-gateway/.gitignore` | build/, .gradle/, 실행 출력 output/ 제외 |
| `v2/partner-gateway/gradlew` | svc-01 원본 복사, 실행 권한 보존 |
| `v2/partner-gateway/gradlew.bat` | svc-01 원본 복사 |
| `v2/partner-gateway/gradle/wrapper/gradle-wrapper.jar` | svc-01 원본 복사 |
| `v2/partner-gateway/gradle/wrapper/gradle-wrapper.properties` | svc-01 원본 복사, Gradle 8.14.5 |
| `v2/partner-gateway/input/requests.json` | 성공→404 실패→성공 순서의 조회 3건 |
| `v2/partner-gateway/input/stub-responses.json` | 한글 이름·활성/비활성·404 HTTP fixture |
| `v2/partner-gateway/expected/responses.json` | 사내 응답 기대값, UTF-8와 마지막 LF |
| `v2/partner-gateway/src/main/java/com/depgov/sample/gateway/GatewayApp.java` | CLI, PartnerClient 호출, 변환·실패 기록, 파일 출력 |
| `v2/partner-gateway/src/main/java/com/depgov/sample/gateway/PartnerStubServer.java` | JDK HttpServer, 127.0.0.1 임의 포트, JSON fixture 응답, 종료 처리 |
| `v2/partner-gateway/src/main/resources/logback.xml` | 콘솔 UTF-8, root INFO |
| `v2/partner-gateway/src/test/java/com/depgov/sample/gateway/GatewayAppTest.java` | loopback 통합·실패·입력 검증 테스트 9건 |
| `v2/partner-gateway/README.md` | 실행법, 입출력 계약, C17~C20 설명 |
| `v2/S4_RESULT.md` | 이 결과 문서 |

`--stub` 단독 실행은 `input/requests.json` → `output/responses.json`이다. 경로를 지정하면 입력 파일과 같은 디렉터리의 `stub-responses.json`을 읽는다. `--base-url` 모드는 별도 loopback 서버를 대상으로 하며 `http://127.0.0.1`만 허용한다. 파트너 응답의 `id/companyName/state`를 사내 `code/displayName/enabled`로 변환한다. 개별 HTTP 오류와 응답 변환 오류는 FAILED로 기록하고 다음 요청을 계속 처리한다. 잘못된 요청 파일과 파일 I/O 오류는 실행 실패로 전달한다.

## 2. 실제 실행 명령과 exit

아래 명령의 cwd는 지정 worktree다. Gradle 실행은 아래 3회뿐이며 모두 지정 스크립트에 위임했다. 로그 보존에는 `set -o pipefail`과 `2>&1 | tee /private/tmp/depgov-smp01/s4/{build,run,runtimeClasspath}.log`를 사용했으며 프로세스 최종 exit를 확인했다.

```sh
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s4 v2/partner-gateway build
# exit 0 — BUILD SUCCESSFUL in 4s, 8 actionable tasks: 8 executed

/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s4 v2/partner-gateway run --args='--stub input/requests.json /private/tmp/depgov-smp01/s4/responses.json'
# exit 0 — BUILD SUCCESSFUL in 2s

/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s4 v2/partner-gateway dependencies --configuration runtimeClasspath
# exit 0 — BUILD SUCCESSFUL in 2s

cmp v2/partner-gateway/expected/responses.json /private/tmp/depgov-smp01/s4/responses.json
# exit 0 — 출력 없음, 바이트 단위 일치

shasum -a 256 v2/partner-gateway/expected/responses.json /private/tmp/depgov-smp01/s4/responses.json
# exit 0 — 두 파일 모두 3bd6c4adc484bf16436a0b6c858ec32e939284e8f6b415316d147afb8c51cb16

cat /private/tmp/depgov-smp01/s4/responses.json
# exit 0 — 아래 실행 결과 절에 원문 수록

rg -n -F 'org.apache.hc' v2/partner-gateway/src
# exit 1 — 일치 0건

rg -n -F 'ch.qos.logback' v2/partner-gateway/src -g '*.java'
# exit 1 — 일치 0건

rg -n -F 'ch.qos.logback' v2/partner-gateway/src/main/resources/logback.xml
# exit 0 — 2행 ConsoleAppender 자원 설정 1건

/usr/bin/javap -verbose -c v2/partner-gateway/build/classes/java/main/com/depgov/sample/gateway/GatewayApp.class > /private/tmp/depgov-smp01/s4/GatewayApp.javap.txt
rg -n 'major version|PartnerClient|PartnerException|JsonCodec|org/slf4j' /private/tmp/depgov-smp01/s4/GatewayApp.javap.txt
# 각 exit 0 — major 65, 생성·메서드 호출·예외·SLF4J 참조 확인

git diff --check -- v2/partner-gateway v2/S4_RESULT.md
# exit 0 (신규 untracked 텍스트의 공백은 별도 Python 검사도 수행)

for file in gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties; do cmp "svc-01/$file" "v2/partner-gateway/$file" || exit; done
# exit 0 — 네 wrapper 파일 원본 동일

git status --short --untracked-files=all
# exit 0 — 변경 경로는 허용 범위의 신규 파일뿐
```

구현 파일은 `apply_patch`로 생성했다. wrapper 복사 명령은 다음과 같고 exit 0이었다.

```sh
mkdir -p v2/partner-gateway/gradle/wrapper
cp -p svc-01/gradlew svc-01/gradlew.bat v2/partner-gateway/
cp -p svc-01/gradle/wrapper/gradle-wrapper.jar svc-01/gradle/wrapper/gradle-wrapper.properties v2/partner-gateway/gradle/wrapper/
```

읽기 전용 조사에는 `pwd`, `git rev-parse HEAD`, `git status --short`, `rg --files`, `cat`, `sed -n`, `nl -ba`, `ls -ld`를 사용했다. 대상은 위 사전 확인 파일, 조상 경로의 AGENTS.md, `/private/tmp/depgov-smp01/repository-gavs.txt`, 사내 artifact 두 개의 로컬 배포 경로, 지정 실행기와 `gradlehome.sh`, 생성한 소스·리소스다. AGENTS.md 검색은 일치 없음(exit 1), 구현 전 아직 없는 `v2/partner-gateway`를 포함한 `ls -ld`는 exit 1이었으며 사내 artifact 두 개는 모두 존재했다. 나머지 파일 읽기는 성공했다. 원격 URL 설정이나 `.env`는 읽지 않았다.

JUnit XML 집계와 최종 감사에는 `/opt/homebrew/bin/python3 -I -B - <<'PY'` 형태의 inline 코드를 사용했고 각 exit 0이었다. 감사 결과는 다음과 같다. 좌표는 build.gradle의 6개 문자열 선언을 inventory 및 허용된 사내 artifact 목록과 대조했다. 바이트코드는 main 클래스 전체를 확인했다.

```text
Java source org.apache.hc: 0 matches
Java source ch.qos.logback: 0 matches
Main bytecode org/apache/hc: 0 matches
Main bytecode ch/qos/logback: 0 matches
Java 21 class major 65: 8 main classes
Wrapper identical: gradlew
Wrapper identical: gradlew.bat
Wrapper identical: gradle/wrapper/gradle-wrapper.jar
Wrapper identical: gradle/wrapper/gradle-wrapper.properties
Declared coordinates available: 6/6
New project text trailing whitespace: 0
JUnit: tests=9 failures=0 errors=0 skipped=0
Changed paths within allowed scope: PASS
```

## 3. 테스트와 실행 결과

| 모듈 | 테스트 클래스 | tests | failures | errors | skipped |
| --- | --- | ---: | ---: | ---: | ---: |
| partner-gateway (root) | com.depgov.sample.gateway.GatewayAppTest | 9 | 0 | 0 | 0 |

근거: `partner-gateway/build/test-results/test/TEST-com.depgov.sample.gateway.GatewayAppTest.xml`. `stubCliMatchesExpectedBytesAndContinuesAfter404`는 expected 바이트 일치, 실패 상태·메시지, 실패 후 후속 성공, UTF-8 한글, 활성/비활성 변환을 확인한다. 나머지 8건은 별도 loopback 서버의 query 전달, HTTP 500, 미등록 경로 404, JSON null/잘못된 타입/필수 필드·상태 오류, 빈 요청 목록, 외부 URL의 연결 전 거부, 인자·요청 파일 오류, 중복 stub 경로 거부를 확인한다. skip 또는 기존 assertion 변경은 없다.

실행 출력의 애플리케이션 부분:

```text
INFO c.depgov.sample.gateway.GatewayApp - request=REQ-001 status=SUCCESS
WARN c.depgov.sample.gateway.GatewayApp - request=REQ-002 status=FAILED reason=Partner GET failed: HTTP 404
INFO c.depgov.sample.gateway.GatewayApp - request=REQ-003 status=SUCCESS
INFO c.depgov.sample.gateway.GatewayApp - Processed 3 requests: success=2, failed=1; output=/private/tmp/depgov-smp01/s4/responses.json
```

실제 출력 JSON (`expected/responses.json`과 동일, 마지막 LF 포함):

```json
[{"requestId":"REQ-001","status":"SUCCESS","partner":{"code":"P100","displayName":"한빛상사","enabled":true},"error":null},{"requestId":"REQ-002","status":"FAILED","partner":null,"error":"Partner GET failed: HTTP 404"},{"requestId":"REQ-003","status":"SUCCESS","partner":{"code":"P200","displayName":"새봄물류","enabled":false},"error":null}]
```

## 4. 사례별 구현 위치와 사용 방식

다음 판정은 계약상 기대값이며 분석기 실행 결과를 대신하지 않는다. 경로는 이 문서 기준이다.

| 사례 | 구현 위치(파일·줄) | 실제 사용 방식과 확인 |
| --- | --- | --- |
| SMP-C17 | [GatewayApp.java:1](partner-gateway/src/main/java/com/depgov/sample/gateway/GatewayApp.java#L1), [build.gradle:30](partner-gateway/build.gradle#L30) | httpclient5 5.6.1을 직접 선언·참조하지 않는다. gateway → ext-client → httpclient5. 소스 grep 0, main 바이트코드 참조 0. 기대 TRANSITIVE_ONLY |
| SMP-C18 | [GatewayApp.java:1](partner-gateway/src/main/java/com/depgov/sample/gateway/GatewayApp.java#L1), [build.gradle:30](partner-gateway/build.gradle#L30) | httpcore5 5.4 직접 선언·참조 0. gateway → ext-client → httpcore5(depth 2), httpclient5 경유 경로도 존재. 기대 TRANSITIVE_ONLY, BOM 5.3.1 대비 versionMismatch |
| SMP-C19 | [GatewayApp.java:50](partner-gateway/src/main/java/com/depgov/sample/gateway/GatewayApp.java#L50), [56행](partner-gateway/src/main/java/com/depgov/sample/gateway/GatewayApp.java#L56), [67행](partner-gateway/src/main/java/com/depgov/sample/gateway/GatewayApp.java#L67), [build.gradle:30](partner-gateway/build.gradle#L30) | `new PartnerClient(baseUrl)`, `client.get(path)`, 정적 타입 `PartnerException` catch 및 getMessage 호출. javap에 new/invokespecial/invokevirtual/예외 테이블 확인. 기대 REFERENCED(사내) |
| SMP-C20 | [build.gradle:34](partner-gateway/build.gradle#L34), [GatewayApp.java:15](partner-gateway/src/main/java/com/depgov/sample/gateway/GatewayApp.java#L15), [19행](partner-gateway/src/main/java/com/depgov/sample/gateway/GatewayApp.java#L19), [66행](partner-gateway/src/main/java/com/depgov/sample/gateway/GatewayApp.java#L66), [logback.xml:2](partner-gateway/src/main/resources/logback.xml#L2) | logback-classic 1.5.34는 runtimeOnly 바인딩 전용. Java 소스·main 바이트코드 참조 0. Logger/LoggerFactory는 선언된 slf4j-api 2.0.18만 참조. 콘솔 INFO/WARN 실제 출력 확인. 기대 UNUSED_CANDIDATE/LOW, outOfBom |
| 공통 JSON 변환 | [GatewayApp.java:38](partner-gateway/src/main/java/com/depgov/sample/gateway/GatewayApp.java#L38), [57행](partner-gateway/src/main/java/com/depgov/sample/gateway/GatewayApp.java#L57), [80행](partner-gateway/src/main/java/com/depgov/sample/gateway/GatewayApp.java#L80), [PartnerStubServer.java:24](partner-gateway/src/main/java/com/depgov/sample/gateway/PartnerStubServer.java#L24), [62행](partner-gateway/src/main/java/com/depgov/sample/gateway/PartnerStubServer.java#L62) | data-convert 1.0.0의 JsonCodec.read/write 정적 호출로 입력·파트너 응답·출력·stub JSON 처리 |

Stub bind는 [PartnerStubServer.java:40](partner-gateway/src/main/java/com/depgov/sample/gateway/PartnerStubServer.java#L40)의 `127.0.0.1:0`, 종료는 73~74행, try-with-resources는 GatewayApp 28행이다. Java 소스의 사례 ID·사용 방식은 각 파일 상단에 적었다. logback 구현 클래스 문자열은 허용된 XML 자원에만 있다.

## 5. runtimeClasspath 실제 출력

선택 버전은 httpclient5 **5.6.1**, httpcore5/httpcore5-h2 **5.4**, SLF4J **2.0.18**, logback-classic/core **1.5.34**다. HTTP client 전이의 `slf4j-api:1.7.36 -> 2.0.18`과 logback 전이의 `2.0.17 -> 2.0.18`을 모두 확인했다. 직접 선언이 최종 버전과 일치하며 미확보된 SLF4J 2.0.17을 다운로드하지 않았다.

사내 ext-client가 core를 직접 runtime 의존으로 갖기 때문에 core의 최소 깊이는 2이고 client를 통한 깊이 3 경로도 보인다. data-convert 경로의 commons-lang3는 공급 저장소에서 POM 없는 jar metadata로 해석하는 알려진 L lane 조건이다. 이번 트리에서 추가 전이는 없으며 임의 POM은 만들지 않았다.

```text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.5/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build

> Task :dependencies

------------------------------------------------------------
Root project 'partner-gateway'
------------------------------------------------------------

runtimeClasspath - Runtime classpath of source set 'main'.
+--- com.depgov.common:ext-client:1.0.0
|    +--- org.apache.httpcomponents.client5:httpclient5:5.6.1
|    |    +--- org.apache.httpcomponents.core5:httpcore5:5.4
|    |    +--- org.apache.httpcomponents.core5:httpcore5-h2:5.4
|    |    |    \--- org.apache.httpcomponents.core5:httpcore5:5.4
|    |    \--- org.slf4j:slf4j-api:1.7.36 -> 2.0.18
|    \--- org.apache.httpcomponents.core5:httpcore5:5.4
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
+--- org.slf4j:slf4j-api:2.0.18
\--- ch.qos.logback:logback-classic:1.5.34
     +--- ch.qos.logback:logback-core:1.5.34
     \--- org.slf4j:slf4j-api:2.0.17 -> 2.0.18

(c) - A dependency constraint, not a dependency. The dependency affected by the constraint occurs elsewhere in the tree.
(*) - Indicates repeated occurrences of a transitive dependency subtree. Gradle expands transitive dependency subtrees only once per project; repeat occurrences only display the root of the subtree, followed by this annotation.

A web-based, searchable dependency report is available by adding the --scan option.

[Incubating] Problems report is available at: file:///Users/coolguyhong/.codex/worktrees/smp01-s4/depgov-samples/v2/partner-gateway/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14.5/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed
```

## 6. 미확보·미실행·요청·보호 규칙

- 미확보 좌표: 없음. 지정 직접 선언 6개와 필요한 선택 전이 의존성 모두 로컬 공급본으로 해결했다.
- 미실행: depgov 분석기·DB/MQ·GKE·실 OSV 조회, 공통 라이브러리 1.1.0 교체 전후 분석은 S4 구현 범위 밖이며 수행하지 않았다. C17~C20 최종 분석 판정은 Claude 검증 대상이다.
- Amendment 요청: 없음.
- 외부 host 접속·다운로드: 0. 네트워크 도구, curl, 브라우저, 외부 artifact 조회를 사용하지 않았다. 실행·테스트 HTTP는 127.0.0.1 임의 포트뿐이다.
- Gradle: 지정 samplegradle.sh 3회만 실행. 스크립트의 offline mirror와 task 전용 GRADLE_USER_HOME(`/private/tmp/depgov-smp01/s4/gradle-home`)를 사용했다. 직접 gradle/gradlew 실행과 사용자 Gradle home 사용은 없다. 스크립트의 `--no-daemon`을 유지했으며 Gradle은 로그에 표시한 일회성 JVM을 빌드 종료 시 중지했다. 지속 daemon 실행 지시는 하지 않았다.
- Git 쓰기: 0. add/commit/checkout/reset/clean/stash, 원격 URL 조회 모두 하지 않았다.
- 보호 파일: `v2/libs/**`, `v2/maven-repo/**`, 기존 svc/BOM 및 그 밖의 저장소 파일 수정 0. 변경 경로는 제출 파일 목록과 일치한다. `/private/tmp/depgov-smp01/s4`에는 요청된 출력, 검증 로그와 지정 실행기의 격리 빌드 파일만 생성됐다.
- Docker/kubectl/gcloud/DB/MQ 접속, 전역 프로세스 종료, `.env`·비밀값 읽기·기록: 0.
- Gradle 로그에는 macOS FSEvents 시작 불가 및 Gradle 9 비호환 deprecated feature 안내가 있었다. 빌드·테스트·실행은 모두 exit 0이며 기능 실패는 관찰되지 않았다. 경고를 이유로 공용 도구를 변경하지 않았다.

최종 문서 감사: `git status --short --branch --untracked-files=all` exit 0으로 지정 branch와 신규 16개 경로를 확인했다. `/opt/homebrew/bin/python3 -I -B` 문서 감사 초회는 복사한 Gradle 로그의 행 끝 공백 때문에 exit 1이었다. 해당 공백을 정리하고 재검사하여 문서 링크·로그 내용·실행 권한·출력 일치·허용 경로 검사 exit 0을 확인했다. 코드와 테스트 결과에는 변경이 없다.

최종 상태: **Claude 검증 대기**.

## 부록: build/run 원문 로그 (행 끝 공백 정리)

### build

```text
Welcome to Gradle 8.14.5!

Here are the highlights of this release:
 - Java 24 support
 - GraalVM Native Image toolchain selection
 - Enhancements to test reporting
 - Build Authoring improvements

For more details see https://docs.gradle.org/8.14.5/release-notes.html

To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.5/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build

> Task :compileJava
Caught exception: Could not start the FSEvents stream: /Users/coolguyhong/.codex/worktrees/smp01-s4/depgov-samples/v2/partner-gateway

> Task :processResources
> Task :classes
> Task :jar
> Task :startScripts
> Task :distTar
> Task :distZip
> Task :assemble
> Task :compileTestJava
> Task :processTestResources NO-SOURCE
> Task :testClasses
> Task :test
> Task :check
> Task :build

[Incubating] Problems report is available at: file:///Users/coolguyhong/.codex/worktrees/smp01-s4/depgov-samples/v2/partner-gateway/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14.5/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 4s
8 actionable tasks: 8 executed
```

### run

```text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.5/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build

> Task :compileJava UP-TO-DATE
Caught exception: Could not start the FSEvents stream: /Users/coolguyhong/.codex/worktrees/smp01-s4/depgov-samples/v2/partner-gateway

> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE

> Task :run
INFO c.depgov.sample.gateway.GatewayApp - request=REQ-001 status=SUCCESS
WARN c.depgov.sample.gateway.GatewayApp - request=REQ-002 status=FAILED reason=Partner GET failed: HTTP 404
INFO c.depgov.sample.gateway.GatewayApp - request=REQ-003 status=SUCCESS
INFO c.depgov.sample.gateway.GatewayApp - Processed 3 requests: success=2, failed=1; output=/private/tmp/depgov-smp01/s4/responses.json

[Incubating] Problems report is available at: file:///Users/coolguyhong/.codex/worktrees/smp01-s4/depgov-samples/v2/partner-gateway/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14.5/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 2s
3 actionable tasks: 1 executed, 2 up-to-date
```
