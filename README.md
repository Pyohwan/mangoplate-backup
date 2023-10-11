# mangoplate-backup
망고플레이트 내 리뷰 로컬 머신에 백업 프로그램
## 빌드
망고플레이트 내 리뷰 백업이 목적이라면 꼭 빌드할 필요는 없습니다.

로컬 머신에서 Mangoplate backup 소스 코드를 빌드 하고 싶다면 다음의 순서로 진행하면 됩니다.
1. JDK 17 설치
2. 소스코드 다운로드
3. gradle wrapper를 이용하여 빌드
```sh
$ ./gradlew build
```
5. 빌드 결과 파일은 mangoplate-backup/build/libs 에 들어있습니다.
## 실행
JDK 17 또는 JRE 17이 설치되어 있어야 합니다.
다음의 명령어로 실행할 수 있습니다. backupFile 아규먼트에 reviews.csv 파일의 경로를 넣으면 됩니다.
```sh
$ java -jar mangoplate-backup-0.0.1-SNAPSHOT.jar --backupFile=[reviews.csv path]
```
## 이슈
제가 실행해 봤을 때는 이미지 파일을 약 10~20개 정도 다운로드 한 다음에 1분 넘게 멈춰지는 현상이 있었습니다. 내부적으로 HttpClient에서 HTTP URL로 접근한 다음 타임아웃이 발생하면 retry를 하고 있습니다. 시간이 제법 오래 걸리는데 그 이유는 망고플레이트의 이미지 서버의 속도가 매우 느린 것으로 보입니다. 이미지 서버가 AWS S3에 위치해 있는 것으로 보이는데 아마도 네트워크 throttling이 있어 처리 속도를 늦추고 있는 것으로 보입니다.

심지어 HttpClient에서 retry를 했음에도 불구하고 이렇게 에러가 뜨며 프로그램이 종료되는 경우도 있습니다. 이럴 경우에는 작업 중이었던 음식점 디렉터리 지우고 다시 시작하면 됩니다.
## 블로그 글 링크
진행 과정을 적어놨습니다.
https://blog.naver.com/saenggeuri/223234094047
