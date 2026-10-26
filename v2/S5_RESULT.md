# SMP-01 S5 구현 결과

상태: **Claude 검증 대기**

## 1. 기준과 수정 파일

- 작업일: 2026-10-26 (Asia/Seoul), task-id `s5`.
- 작업 worktree: `/Users/coolguyhong/.codex/worktrees/smp01-s5/depgov-samples`, 지정 branch `codex/smp01-s5`, 통합 기준 `claude/smp01-int`.
- `git rev-parse HEAD` (exit 0): `c86f2b4ee1ad1a6411f7fc0f1d22208940d49041`.
- 시작 시 `git status --short` (exit 0): 출력 없음.
- 사전 확인: `/Users/coolguyhong/workspace/depgov/docs/implementation/SMP-01_PLAN.md` 1~4절과 후속 절, `svc-01/`, `svc-02/`, `bom/`, `v2/libs/README.md`, Plugin API와 provider 소스·테스트, 지정 실행기와 오프라인 mirror 설정. 모두 읽기 전용.

추가 파일은 아래 16개다. 기존 파일 수정은 없다.

| 파일 | 내용 |
| --- | --- |
| `v2/plugin-host/settings.gradle` | root 이름 `plugin-host`, `host` 모듈 하나 포함 |
| `v2/plugin-host/build.gradle` | subprojects java/group/version 및 지정 Maven 저장소 설정 |
| `v2/plugin-host/host/build.gradle` | application, C21~C24 리터럴 의존 선언, JUnit, Java release 21, 루트 workingDir |
| `v2/plugin-host/.gitignore` | `build/`, `.gradle/`, 생성 출력 `output/` 제외 |
| `v2/plugin-host/gradlew` | svc-01 wrapper 그대로 복사, 실행 권한 유지 |
| `v2/plugin-host/gradlew.bat` | svc-01 wrapper 그대로 복사 |
| `v2/plugin-host/gradle/wrapper/gradle-wrapper.jar` | svc-01 wrapper 그대로 복사 |
| `v2/plugin-host/gradle/wrapper/gradle-wrapper.properties` | svc-01 Gradle 8.14.5 설정 그대로 복사 |
| `v2/plugin-host/input/plugins.yml` | 활성 id, handler 클래스·메서드, 고객·금액 문자열 입력 |
| `v2/plugin-host/expected/report.txt` | 독립 계산한 플러그인 출력과 SHA-256 정답 |
| `v2/plugin-host/host/src/main/java/com/depgov/sample/plugin/PluginConfig.java` | YAML 직접 로딩, 설정 검증, 정렬한 digest 입력 |
| `v2/plugin-host/host/src/main/java/com/depgov/sample/plugin/ReflectiveHandler.java` | 설정 클래스 이름으로 reflection 호출 |
| `v2/plugin-host/host/src/main/java/com/depgov/sample/plugin/HostApp.java` | SPI 발견·활성 플러그인 실행, 파일 출력, CLI |
| `v2/plugin-host/host/src/test/java/com/depgov/sample/plugin/HostAppTest.java` | SPI·reflection·파일 시나리오와 실패 동작 테스트 10개 |
| `v2/plugin-host/README.md` | 사용 방식·정규화·실행·분석기 기대 판정 설명 |
| `v2/S5_RESULT.md` | 이 결과 문서 |

## 2. 실제 실행 명령과 결과

아래 명령은 모두 저장소 루트에서 실행했다. Gradle 명령은 각각 `set -o pipefail` 후 `| tee <로그 경로>`로 기록했으며, 표의 exit은 전체 pipeline exit이다.

| 실제 Gradle 명령 | exit | 결과 / 로그 |
| --- | --- | --- |
| `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s5 v2/plugin-host build` | 0 | BUILD SUCCESSFUL (4s), `/private/tmp/depgov-smp01/s5/build.log` |
| `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s5 v2/plugin-host :host:run --args='input/plugins.yml /private/tmp/depgov-smp01/s5/report.txt'` | 0 | BUILD SUCCESSFUL (2s), `/private/tmp/depgov-smp01/s5/run.log` |
| `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s5 v2/plugin-host :host:dependencies --configuration runtimeClasspath` | 0 | BUILD SUCCESSFUL (2s), `/private/tmp/depgov-smp01/s5/runtimeClasspath.log` |
| `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s5 v2/plugin-host :host:run` | 0 | 기본 `output/report.txt` 생성, BUILD SUCCESSFUL (2s), `/private/tmp/depgov-smp01/s5/run-default.log` |

모든 Gradle 실행은 지정 실행기의 JDK 21, `--offline --no-daemon --console=plain`, task 전용 `GRADLE_USER_HOME=/private/tmp/depgov-smp01/s5/gradle-home`을 사용했다. 실행기에서 저장소를 로컬로 치환했다. wrapper 배포판도 공급본을 사용했다.

Gradle이 `--no-daemon` 실행에도 JVM 옵션 적용을 위해 single-use 프로세스를 fork하고 종료한다는 메시지를 출력했다(`Daemon will be stopped at the end of the build`). 지속 Gradle daemon을 별도 실행하지 않았다. 빌드·run의 macOS FSEvents stream 경고와 Gradle 9 비호환 deprecated feature 요약도 출력됐으며 명령은 모두 exit 0이었다.

### 테스트 수와 근거

| 모듈 | suite | tests | failures | errors | skipped |
| --- | --- | --- | --- | --- | --- |
| host | `com.depgov.sample.plugin.HostAppTest` | 10 | 0 | 0 | 0 |

XML: `v2/plugin-host/host/build/test-results/test/TEST-com.depgov.sample.plugin.HostAppTest.xml`.

| 테스트 메서드 | 확인 내용 |
| --- | --- |
| `discoversAndExecutesReportThroughServiceLoader` | 실제 SPI provider의 id와 render 결과 |
| `reflectiveHandlerComputesRealDigestFromSortedInput` | 정렬된 실제 입력의 reflection digest와 JDK MessageDigest 일치 |
| `writesExactExpectedReportAndCreatesOutputDirectories` | main 실행·디렉터리 생성·expected와 바이트 비교 |
| `executesOnlyActivePlugins` | active 빈 목록일 때 provider render 제외 |
| `changedInputChangesReflectionDigest` | 변경된 UTF-8 입력의 digest 정확성 및 기본 입력과 차이 |
| `missingActivePluginFails` | 존재하지 않는 활성 id 오류 |
| `missingHandlerClassFailsInsteadOfProducingFallbackDigest` | 설정 클래스 로딩 실패 전파 |
| `rejectsUnsupportedHandlerMethod` | sha256Hex 외 메서드 설정 거부 |
| `rejectsNonStringInputValues` | YAML 숫자값 등 문자열 외 입력 거부 |
| `rejectsDuplicateActiveIds` | 활성 id 중복 거부 |

### 비교·복사·정적 검증 명령

| 실행 명령 | exit / 결과 |
| --- | --- |
| `mkdir -p v2/plugin-host/gradle/wrapper v2/plugin-host/expected` | 0 |
| `cp -p svc-01/gradlew svc-01/gradlew.bat v2/plugin-host/` | 0 |
| `cp -p svc-01/gradle/wrapper/gradle-wrapper.jar svc-01/gradle/wrapper/gradle-wrapper.properties v2/plugin-host/gradle/wrapper/` | 0 |
| `for candidate in gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties; do cmp "svc-01/$candidate" "v2/plugin-host/$candidate" || exit; done` | 0, wrapper 4개 모두 동일 |
| `cmp v2/plugin-host/expected/report.txt /private/tmp/depgov-smp01/s5/report.txt` | 0, 출력 없음 |
| `cmp v2/plugin-host/expected/report.txt v2/plugin-host/output/report.txt` | 0, 출력 없음 |
| `shasum -a 256 v2/plugin-host/expected/report.txt /private/tmp/depgov-smp01/s5/report.txt` | 0, 두 파일 모두 아래 SHA-256 |
| `cat /private/tmp/depgov-smp01/s5/report.txt` | 0, 아래 실행 출력 |
| `rg -n 'SMP-C2[1-4]\|ServiceLoader\|plugin.render\|new Yaml\|Class.forName\|getMethod' v2/plugin-host/host` | 0, 사례 주석·선언·호출 위치 확인 |
| `rg -n -F 'com.depgov.plugins.report' v2/plugin-host/host/src` | 1, 출력 없음: main/test 소스 0건 |
| `rg -n -F 'org.apache.commons.codec' v2/plugin-host/host/src` | 1, 출력 없음: main/test 소스 0건 |
| `rg -n -F 'org.apache.commons.codec' v2/plugin-host/input` | 0, plugins.yml:2 한 건 |
| `rg -n -F 'org.apache.commons.codec' v2/plugin-host -g '!gradlew*' -g '!**/build/**' -g '!**/.gradle/**'` | 0, plugins.yml:2 한 건 |
| `git diff --check -- v2/plugin-host v2/S5_RESULT.md` | 0 |
| `git status --short --untracked-files=all` | 0, 허용 경로의 신규 파일만 존재 |
| `git status --short --branch --untracked-files=all` | 0, `codex/smp01-s5` 및 허용 경로의 신규 파일 16개 확인 |

`rg` exit 1은 위 두 0건 검사에서 기대한 결과다. `nl -ba`로 main 세 파일·테스트·host/build.gradle의 줄 번호를 확인했다. Python은 네 번 모두 `/opt/homebrew/bin/python3 -I -B -`의 stdin 스크립트로 실행했고 exit 0이었다: (1) `hashlib.sha256`으로 expected 생성, (2) 소스 금지 문자열 0건·JUnit XML·class major 출력, (3) wrapper 원본 byte 동일성과 main class major 65·금지 패키지 constant 0건·JUnit count를 assert, (4) 읽기 전용 `git status --porcelain --untracked-files=all` 결과를 읽어 신규 파일 16개·허용 경로만 존재함을 assert하고 작성 텍스트의 끝 개행·후행 공백 및 두 실행 출력과 expected 바이트 일치를 검사. 추가 네트워크나 Gradle 호출은 없다.

최종 Python 검증 출력:

```text
wrapper identical: gradlew
wrapper identical: gradlew.bat
wrapper identical: gradle/wrapper/gradle-wrapper.jar
wrapper identical: gradle/wrapper/gradle-wrapper.properties
HostApp.class: major=65, forbidden package constants=0
PluginConfig.class: major=65, forbidden package constants=0
ReflectiveHandler.class: major=65, forbidden package constants=0
com.depgov.sample.plugin.HostAppTest: {'tests': 10, 'failures': 0, 'errors': 0, 'skipped': 0}
```

최종 범위 검사 출력 (exit 0):

```text
PASS: 16 new files, allowed paths only, authored text newline/whitespace checks
PASS: expected == explicit output == default output (byte-for-byte)
```

## 3. C21~C24 구현 위치와 사용 방식

아래 Java 경로의 공통 prefix는 `v2/plugin-host/host/src/main/java/com/depgov/sample/plugin/`이다. 모든 사례 소스의 1행에 SMP 사례 ID와 사용 방식 주석이 있다.

| 사례 | 선언 위치 | 구현 위치 | 실제 사용 및 기대 판정 |
| --- | --- | --- | --- |
| SMP-C21 | `v2/plugin-host/host/build.gradle:11` | `HostApp.java:29` (발견), `HostApp.java:37` (활성 id 선택), `HostApp.java:42` (실행) | runtimeOnly report-plugin을 `ServiceLoader.load(Plugin.class)`로만 발견. 구현 클래스 import·생성·이름 문자열 없음. SPI 전용이므로 기대 UNUSED_CANDIDATE / MEDIUM, LIM-SPI 오탐, hold-out |
| SMP-C22 | `v2/plugin-host/host/build.gradle:12` | `PluginConfig.java:36`, `ReflectiveHandler.java:9`, `ReflectiveHandler.java:10`, `v2/plugin-host/input/plugins.yml:2` | YAML handler 클래스 이름 → Class.forName → getMethod("sha256Hex", String.class).invoke(null, ...). handlerMethod는 YAML에서 읽고 sha256Hex만 허용. codec 정적 타입·import·클래스 이름 상수 없음. 기대 UNUSED_CANDIDATE / HIGH, LIM-REFLECTION 오탐 |
| SMP-C23 | `v2/plugin-host/host/build.gradle:13` | `PluginConfig.java:14` (import), `PluginConfig.java:20` (생성·load) | `new Yaml().load(stream)` 직접 호출. 기대 REFERENCED, 선택 2.6 vs BOM 2.3 versionMismatch |
| SMP-C24 | `v2/plugin-host/host/build.gradle:14` | `HostApp.java:4`, `HostApp.java:29`, `HostApp.java:32`, `HostApp.java:42` | Plugin 인터페이스 타입, id·render 메서드 직접 호출. 기대 REFERENCED, 사내 좌표 |

테스트 소스 prefix: `v2/plugin-host/host/src/test/java/com/depgov/sample/plugin/`. `HostAppTest.java:22`는 SPI 발견, `:26`은 인터페이스 render 호출, `:30`은 reflection digest 검증, `:37`은 expected 파일 비교, `:50`은 다른 UTF-8 입력 검증이다. 테스트도 provider 구현이나 codec 클래스 이름을 참조하지 않는다.

정확한 grep 결과:

```text
$ rg -n -F 'com.depgov.plugins.report' v2/plugin-host/host/src
(출력 없음, exit 1, 0건)
$ rg -n -F 'org.apache.commons.codec' v2/plugin-host/host/src
(출력 없음, exit 1, 0건)
$ rg -n -F 'org.apache.commons.codec' v2/plugin-host -g '!gradlew*' -g '!**/build/**' -g '!**/.gradle/**'
v2/plugin-host/input/plugins.yml:2:handler: org.apache.commons.codec.digest.DigestUtils
(exit 0, plugins.yml 한 건만 존재)
```

host main class 세 개에서도 금지 패키지를 dot·slash 표기 모두 검사해 bytecode constant 0건을 확인했다. C21/C22가 실제 사용된다는 근거는 실행·테스트이고, 분석기 오탐은 계약의 기대값이다. 분석기의 실제 결과는 이 작업에서 실행하지 않았다.

## 4. 실행 출력과 expected 비교

digest 입력 문자열은 `amount=12500\ncustomer=C-1001`이다. 입력 key를 자연 순서로 정렬해 `key=value`를 LF로 연결하며 끝 LF는 없다. 기본 YAML은 customer를 먼저 적었으므로 정렬 동작도 확인한다. `expected/report.txt`의 digest는 Python 표준 라이브러리로 독립 계산했고 테스트에서는 JDK MessageDigest로도 비교했다.

지정 인수 실행과 무인수 기본 실행의 실제 출력은 동일하다:

```text
amount=12500
customer=C-1001
digest=91741088cc254461b90e7bdb61351c150f1602980bb453921feb5f9a7c1e1689
```

UTF-8/LF, 마지막 줄에도 LF 포함. `cmp` 두 번 모두 exit 0, `Files.mismatch` 테스트도 -1이다.

```text
be975346416ea14ffe8b075ccdb64590f6122d2e411c79b63c8e7ae71913c3f5  v2/plugin-host/expected/report.txt
be975346416ea14ffe8b075ccdb64590f6122d2e411c79b63c8e7ae71913c3f5  /private/tmp/depgov-smp01/s5/report.txt
```

## 5. runtimeClasspath 실제 출력

명령: `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s5 v2/plugin-host :host:dependencies --configuration runtimeClasspath`, exit 0.

```text
> Task :host:dependencies

------------------------------------------------------------
Project ':host'
------------------------------------------------------------

runtimeClasspath - Runtime classpath of source set 'main'.
+--- commons-codec:commons-codec:1.17.1
+--- org.yaml:snakeyaml:2.6
+--- com.depgov.plugins:plugin-api:1.0.0
\--- com.depgov.plugins:report-plugin:1.0.0
     \--- com.depgov.plugins:plugin-api:1.0.0

(*) - Indicates repeated occurrences of a transitive dependency subtree. Gradle expands transitive dependency subtrees only once per project; repeat occurrences only display the root of the subtree, followed by this annotation.

BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed
```

요구한 네 runtime 좌표가 모두 해석됐다. report-plugin → plugin-api의 runtime 전이도 존재한다. 충돌·버전 대체·미해결 좌표는 없다. 전체 Gradle 메시지는 위 runtimeClasspath.log에 보존했다.

## 6. 미확보·미실행·보호 규칙

- 미확보 좌표 없음. 네 운영 좌표와 지정 JUnit 두 좌표를 오프라인에서 해석했다.
- amendment 요청 없음. sha256Hex 전용 handlerMethod 검증과 digest 정규화 규약은 README에 명시했다.
- depgov 분석 pipeline·실제 오탐 판정·G lane·OSV 조회는 이 구현 작업에서 미실행이며 Claude 검증 범위다.
- 외부 host 접속 0, 다운로드 0. Maven/Gradle 외부 URL은 요구된 선언과 원본 wrapper 설정으로만 남겨두고 실제 실행은 로컬 공급본·mirror를 사용했다.
- git 쓰기 0 (`add/commit/checkout/reset/clean/stash` 미사용). git 읽기와 파일 생성만 수행했다.
- `v2/libs/**`, `v2/maven-repo/**`, 기존 svc/bom 및 나머지 저장소 파일 수정 0. 신규 산출물은 `v2/plugin-host/**`, `v2/S5_RESULT.md`로 한정했다. 실행 로그·검증 출력 파일은 지정 s5 임시 lane에 두었다.
- `.env` 읽기 0, 비밀값·토큰 기록 0. docker/kubectl/gcloud·DB/MQ 접속·전역 process 종료 명령 미사용.
- Gradle 직접 호출·사용자 Gradle cache·toolchain 선언·외부 plugin·catalog/platform 의존 선언 미사용. 각 module의 dependencies 블록에 문자열 리터럴로 선언했다.
- 기존 assertion 완화·삭제·skip 없음. Java 21 main class major 65 확인, JUnit 10개 전부 통과.

상태: **Claude 검증 대기**
