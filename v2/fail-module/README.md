# fail-module — C29 의도적 module 컴파일 실패

이 프로젝트는 실패 기록을 검증하는 표본이며 정상/성공 표본 수에 포함하지 않는다.
JDK 21에서 `ok`는 정상 컴파일되지만 `broken`에는 존재하지 않는
`FilenameUtils.getExtensionX("a.txt")` 호출이 정확히 한 줄 있다.
정상 `FilenameUtils.getExtension("a.txt")` 호출과 `ok`의
`StringUtils.trimToEmpty` 정적 호출도 각각 한 번 유지한다.

## 실행과 기대 결과

저장소 루트에서 다음 오프라인 실행기를 사용한다.

```sh
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s7 v2/fail-module :ok:classes
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s7 v2/fail-module classes
```

첫 명령은 exit 0, 둘째 명령은 `:broken:compileJava`에서 `cannot find symbol`
(`method getExtensionX(String)`, `class FilenameUtils`) 오류로 exit nonzero가 기대된다.
즉 depgov의 `gradlew classes` 빌드가 실패하는 C29 사례다.
서비스 등록 시 `targetPath=v2/fail-module`, `buildJdkMajor=21`을 사용한다.
depgov job의 기대 결과는 `DEGRADED`, 분석 모드는 `REDUCED`,
`fail_stage=build`이며 실패 module의 확인 불가 범위를 표시해야 한다.
이 depgov 결과는 로컬 compile 결과와 구분하여 후속 pipeline에서 검증한다.

## 복구

`broken/src/main/java/com/depgov/sample/v2/failmodule/broken/BrokenReport.java`에서
`FilenameUtils.getExtensionX("a.txt");`가 있는 한 줄을 삭제하고 둘째 명령을 다시 실행한다.
정상 호출과 나머지 소스/의존성은 그대로 둔다.
실제 exit와 오류 원문은 [S7_RESULT.md](../S7_RESULT.md)에 기록한다.
