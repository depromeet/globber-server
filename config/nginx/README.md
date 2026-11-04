# 운영 호스트 Nginx 전환 준비

이 설정은 현재 운영 프론트 컨테이너와 Spring 연결을 유지하는 전환안이다.
호스트 Nginx를 설치하고 별도 포트 및 실제 운영 포트에서 시험 전환했다.
시험 후에는 기존 Nginx Proxy Manager로 복구했다. 현재 호스트 Nginx 서비스는 masked/inactive 상태다.

## 연결

| 공개 경로 | 호스트 내부 연결 | 동작 |
| --- | --- | --- |
| `/` | `127.0.0.1:3000` | 기존 프론트 컨테이너 |
| `/api/` | `127.0.0.1:8080` | 첫 `/api` 제거 후 Spring 전달 |
| `/login/oauth2/` | `127.0.0.1:8080` | 경로 유지 |
| `/api/places`, `/api/geocode` | `127.0.0.1:3000` | 경로 유지 |

## 전환 전 필요한 작업

1. 기존 NPM 설정(`/home/minug0320/data`)과 인증서(`/home/minug0320/letsencrypt`)를 접근 제한된 위치에 백업한다. 인증서 개인키와 NPM DB는 Git에 넣지 않는다.
2. 프론트 배포가 사용하는 `/home/minug0320/17th-team1-client/docker-compose-prod.yml`의 frontend 서비스에 `127.0.0.1:3000:80` 포트 매핑을 추가한다. 다음 프론트 배포에서도 유지되도록 설정의 원본 관리 위치를 확인한다.
3. 백엔드 `config/docker-compose-prod.yml`의 prod 포트 매핑을 `127.0.0.1:8080:8080`으로 제한한다. DB/Redis 접근 제한은 별도로 수행한다.
4. 컨테이너를 재생성한 뒤 localhost 3000의 프론트 응답과 localhost 8080의 `/actuator/health`를 확인한다. 현재 NPM을 통한 운영 동작도 확인한다.
5. 호스트 Nginx/Certbot을 준비한다. 패키지 설치 시 자동 서비스 시작이 기존 80/443 포트와 충돌하지 않도록 한다.
6. `globber-prod.conf`를 설치하고 호스트에서 `nginx -t`로 검사한다. 현재 설정은 별도로 발급한 `/etc/letsencrypt/live/globber.world` 인증서를 참조한다(2027-02-02 만료).
7. Certbot 갱신 타이머와 성공 시 Nginx reload hook을 구성한다. NPM이 운영 중일 때는 NPM의 기존 challenge webroot를 사용한다. 전환 스크립트는 호스트의 `/var/www/letsencrypt`를 사용한 모의갱신이 성공해야 해당 경로를 영구 저장하고 전환을 확정한다.
8. `scripts/refresh-deployment-proxy.sh`와 개발/운영 CD에서 프록시 종류를 환경별로 구분한다. 운영은 호스트의 `nginx -t` 및 reload를 사용하고, 개발은 실제 구성을 확인해 기존 동작을 유지한다. self-hosted runner에는 필요한 명령만 실행할 권한을 부여한다.

## 전환 및 검증

- 사전 검사와 복구 명령을 준비한 후 기존 NPM 컨테이너를 중지하고 호스트 Nginx를 시작한다. 프론트 컨테이너는 유지한다.
- 두 도메인의 HTTPS/인증서, `/login`, 로그인 시작 및 콜백, API 헬스 체크, places/geocode 경로, 이미지·요청 크기를 기존 동작과 비교한다.
- 실패하면 호스트 Nginx를 중지한 뒤 기존 NPM 컨테이너를 시작하고 공개 경로를 확인한다.
- 성공 후 NPM 자동 재시작을 해제하고 외부 81번 허용을 제거한다. 복구 기간 동안 NPM 데이터와 인증서는 보관한다.

## 현재 준비 범위

`globber-prod.conf`는 기존 요청 경로를 유지하는 설정 초안이다. 호스트 포트 매핑, 인증서 갱신, CI/CD 전환을 완료하기 전에는 운영용으로 계속 활성화하지 않는다.

## 2026-11-05 KST 시험 기록

- 백업: 운영 VM의 `/var/backups/globber-proxy/20261104T145802Z` (root 전용). NPM 설정/인증서, SQLite 온라인 백업, 컨테이너 inspect, 프론트 Compose와 비밀 주입 파일을 포함한다. 이 백업 파일은 Git에 추가하지 않는다.
- SQLite integrity check와 백업 SHA-256 검증을 통과했다.
- Ubuntu Nginx 1.18.0을 설치하되 자동 서비스 시작을 막았다.
- 프론트에 임시 Compose override로 `127.0.0.1:3000:80`을 연결하고, 현재 실행 중이던 이미지로 재생성했다. 백엔드와 데이터 컨테이너는 재생성하지 않았다.
- 127.0.0.1의 18080/18443 포트에서 호스트 Nginx를 시험한 뒤, NPM을 잠시 중지하고 실제 80/443에서도 시험했다. 두 단계 모두 기존 응답과 일치했다.
- `/` (307), `/login` (200), `/api/actuator/health` (200, UP), `/api/places` (400), `/api/geocode` (400), 인증 코드 없는 `/login/oauth2/code/kakao` (302)의 상태 코드와 Content-Type을 비교했다. www 도메인의 로그인 페이지도 확인했다. 파라미터 없는 places/geocode의 400은 시험 전부터 동일했다.
- 실제 사용자 카카오 로그인 성공, 인증된 API, 파일 업로드, 부하/장시간 운영은 이 시험에 포함되지 않는다.
- 복구 과정에서 NPM 시작 직후 내부 Nginx PID가 아직 준비되지 않아 reload가 실패하는 상황을 발견했다. 복구 스크립트에 PID 준비 대기와 HTTP/1.1 로그인 페이지 응답 대기를 추가해 재검증했다. 준비 확인 중 HTTP/2 reload 타이밍의 일시 오류를 피하기 위해 HTTP/1.1을 사용했으며 최종 공개 HTTP/2 응답도 정상 확인했다.
- 최종 상태: NPM/프론트 실행 중, 프론트 호스트 포트 없음, 호스트 Nginx masked/inactive. 5분 자동 복구 타이머는 해제했다. 시험 전후 응답 비교와 백업 체크섬을 다시 확인했다.
- 이 시험용 복구 스크립트: `sudo /var/backups/globber-proxy/20261104T145802Z/rollback.sh`. 당시의 변경되지 않은 Compose 파일을 사용해 원래 실행 구성을 복구한다. 이후 배포/설정 변경이 있다면 실행 전 내용을 다시 확인한다.

## 2026-11-05 KST 영구 전환 준비 상태

- 준비 브랜치: `chore/host-nginx-migration`. 아직 main 반영 전이다.
- 호스트 설정 `/etc/nginx/sites-available/globber` 및 enabled 링크를 배치했다. 기본 Ubuntu 사이트 링크는 제거했다. 서비스는 masked/inactive이므로 현재 공개 트래픽은 NPM이 처리한다.
- 새 호스트 인증서 발급 완료. `certbot.timer` 활성화 및 deploy hook 배치 완료. NPM이 운영 중인 동안 호스트 인증서의 갱신 webroot는 `/home/minug0320/data/letsencrypt-acme-challenge`를 유지한다.
- 갱신 모의시험은 미통과: 첫 시도는 인증기관의 도메인 A/AAAA 조회 시간 초과, 두 번째는 `rateLimited: Service busy; retry later`. 로컬에서 두 도메인의 challenge 파일에 접근되는 것은 확인했다. 인증기관에 추가 반복 요청을 보내지 않았다. 이후 재시도 및 영구 전환 후 최종 webroot 검증이 필요하다.
- 프론트 서버 Compose에 `127.0.0.1:3000:80`을 저장했다. 현재 실행 중인 프론트 컨테이너는 재생성하지 않았으므로 이 포트는 아직 열리지 않았다. 프론트 배포 워크플로가 이 서버 파일을 사용하므로 이후 프론트 재배포에서도 매핑이 유지된다.
- root 전용 추가 백업: `/var/backups/globber-proxy/prepared-20261104T151314Z`. 호스트 Nginx 이전 설정, 프론트 Compose 이전 설정, 이전 인증서 갱신 설정을 포함한다.
- minug0320 runner 사용자에게 호스트 Nginx 문법 검사와 nginx 서비스 reload 두 명령만 비대화형 sudo 허용을 추가했다. 실제 runner 사용자로 문법 검사 통과.
- 배포 helper는 활성화된 호스트 Nginx를 우선 선택하고, 그렇지 않으면 기존 NPM을 사용한다. `DEPLOYMENT_PROXY=host|npm`으로 명시할 수도 있다. 개발 NPM 동작은 보존한다.
- 운영 Compose에서 Spring을 localhost에만 공개하고 Promtail 서비스를 제거했다. 운영 CD는 prod만 재생성하도록 변경해 DB/Redis가 이번 배포에서 함께 재생성되지 않도록 했다. 개발 Promtail은 이 작업 범위에서 변경하지 않았다.
- 공개 저장소의 main에 배포 helper 변경이 반영되기 전에는 전환 스크립트가 종료된다. 실제로 이 차단 동작을 확인했다.

### 전환 시 실행

1. 준비 브랜치를 검토하고 main에 반영한다. main의 CI/CD 실행을 확인하고 다른 배포와 동시에 전환하지 않는다.
2. 인증기관 오류가 해소됐는지 현재 NPM 경로에서 `sudo certbot renew --cert-name globber.world --dry-run --non-interactive --no-random-sleep-on-renew`로 확인한다. 실패하면 반복 요청하지 말고 원인을 확인한다.
3. 준비된 전환 명령: `sudo /etc/globber-nginx/cutover.sh`.
4. 이 명령은 프론트 재생성, NPM 중지, 호스트 Nginx 시작, 주요 공개 경로 확인, 최종 호스트 webroot 모의갱신을 수행한다. systemd transient 서비스가 전환 전체를 240초로 제한한다. 시간 초과/실패 시 해당 cgroup의 모든 전환 프로세스가 종료된 뒤 ExecStopPost가 복구를 수행한다. 별도의 복구 타이머가 전환과 경쟁하지 않는다. 복구 callback에는 최대 300초를 허용한다.
5. 성공 후 호스트 Nginx를 부팅 시 시작하도록 등록하고 NPM 자동 재시작을 끈다. 실패하면 기존 NPM과 NPM용 갱신 webroot를 복구한다. 프론트의 localhost 전용 포트는 복구 후에도 유지된다.
6. 필요할 때 수동 복구: `sudo /etc/globber-nginx/rollback.sh`.
7. 전환 성공 후 실제 로그인·업로드를 확인하고, GCP의 81번 외부 허용을 제거한다. NPM 데이터와 인증서는 복구용으로 보관한다.

코드 리뷰에서 발견한 두 문제(타이머와 진행 중 전환의 경쟁, 인증서 설정 실패로 프록시 복구가 건너뛰어지는 상황)는 위 systemd 관리 방식과 트래픽 우선 복구 순서로 수정했다. 배포 helper 및 복구 callback/부가 작업 실패에 대한 Python 테스트 12개가 통과했다.
