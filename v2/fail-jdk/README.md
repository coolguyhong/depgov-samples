# fail-jdk — C30 의도적 JDK 불일치

이 서비스는 depgov에 `targetPath=v2/fail-jdk`, **`buildJdkMajor=11`**로 등록한다.
소스는 Java 17에서 사용할 수 있는 `record`, text block (`"""`),
`sealed interface ... permits`, `instanceof` 패턴 매칭을 모두 사용한다.
따라서 JDK 11 javac에서는 컴파일이 실패해야 한다. 이는 실패 기록 검증용이며
정상/성공 표본 수에 포함하지 않는다.

`build.gradle`에는 toolchain, `options.release`, source/target 호환성 고정을
선언하지 않아 depgov가 선택한 JDK의 기본 javac가 그대로 소스를 처리한다.
동일 소스는 JDK 17 또는 21에서 정상 컴파일할 수 있다.

## 로컬 검증

저장소 루트에서 지정된 오프라인 실행기를 사용한다. 실행기는 JDK 21을 사용하므로
다음 명령의 기대 결과는 exit 0이다.

```sh
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s7 v2/fail-jdk classes
```

`ModernReport.main`은 text block의 JSON을 Jackson으로 읽고 제목을 정리한 뒤
`Report` record와 패턴 매칭을 거쳐 `Monthly report: 3`을 출력하도록 작성했다.
로컬 검증 범위는 compile이며 main 실행은 필수가 아니다.

## depgov 실패 관측과 복구

로컬에는 JDK 11이 없으므로 실제 JDK 11 컴파일 실패는 depgov G lane에서 관측한다.
기대 결과는 C30 `FAILED(build)`이며 로컬 JDK 21 성공과 구분하여 기록한다.
복구 시 소스를 바꾸지 않고 depgov 서비스의 `buildJdkMajor`를 17 또는 21로 변경한다.
실제 로컬 exit와 문법의 소스 위치는 [S7_RESULT.md](../S7_RESULT.md)에 기록한다.
