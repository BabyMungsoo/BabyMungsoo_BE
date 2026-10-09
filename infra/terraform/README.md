# AWS 배포 (Terraform)

EC2 한 대에 Docker Compose 로 **Spring Boot + PostgreSQL + Caddy** 를 띄웁니다.
HTTPS 는 Caddy 가 `<공인IP>.sslip.io` 도메인으로 Let's Encrypt 인증서를 자동으로 받아 처리합니다. 도메인을 따로 살 필요가 없습니다.

```
브라우저/앱 ─https─→ [EC2 t3.small, 오사카, 기본 VPC]
                      ├─ caddy      :443 → api-blue 또는 api-green (upstream.caddy)
                      ├─ api-blue   ┐ 둘 중 하나만 트래픽을 받는다 (블루-그린)
                      ├─ api-green  ┘
                      ├─ db         postgres:16  (/opt/emj/data/postgres)
                      └─ GitHub Actions self-hosted 러너 (라벨 emj-prod)
비밀값: terraform.tfvars → .env → 첫 부팅 user_data 로 전달
접속:   EC2 Instance Connect (키 페어 없음)
배포:   main push → GitHub Actions(.github/workflows/cd.yml) → 무중단 자동 배포
```

## 지원 계정 제약과 대응

권한은 바꿀 수 없으므로 아래처럼 맞췄습니다.

| 정책 / 막힌 권한 | 대응 |
|---|---|
| `RestrictRegionOsaka` | 리전 `ap-northeast-3` 고정 |
| `DenyAllWithoutMFA`, 액세스 키 없음 | `aws login`(콘솔 계정 + MFA)으로 받은 임시 자격 증명 사용 |
| `ControlOnlyOwnResources` | 모든 리소스에 소유자 태그(`owner_tags`)를 붙임. **없으면 보안 그룹 규칙 추가·삭제가 거부됨** |
| `EC2InstanceConnectOnly`, 키 페어 등록 불가 | EC2 Instance Connect 로만 접속 |
| VPC 생성 불가 | 계정의 기본 VPC 사용 |
| 탄력적 IP 불가 | 자동 공인 IP 사용. 부팅 때마다 서버가 자기 IP 로 `API_HOST` 를 갱신 |
| Route 53 / ACM / ELB / Parameter Store 미허용 | sslip.io + Caddy, `.env` 는 user_data 로 전달 |
| Budgets 거부 | 예산 알림 없음. 콘솔에서 비용을 직접 확인 |

예상 비용은 월 $20 안팎입니다(t3.small + 30GB gp3 + 공인 IPv4).

## 처음 한 번

필요한 도구: Terraform ≥ 1.6, AWS CLI v2(≥ 2.32, `aws login` 지원)

```bash
aws login --region ap-northeast-3      # 브라우저에서 콘솔 계정 + MFA 로 로그인
aws sts get-caller-identity             # project13-116-osaka 인지 확인

cd infra/terraform
cp terraform.tfvars.example terraform.tfvars   # 값 채우기 (owner_tags 포함)
terraform init
terraform plan
terraform apply
```

`apply` 자체는 1분이면 끝나지만, 그 뒤 서버가 Docker 설치 → 코드 clone → Gradle 빌드 → 기동하는 데 **10분 정도** 더 걸립니다. 진행 상황은 이렇게 봅니다.

```bash
$(terraform output -raw bootstrap_log_command) | tail -30
curl -i "$(terraform output -raw swagger_url)"
terraform output api_base_url     # 프론트 EXPO_PUBLIC_API_BASE_URL 에 넣을 값
```

**prod 프로필이라 병원 시드가 자동으로 돌지 않습니다.** 첫 배포 후 한 번 호출하세요.

```bash
API=$(terraform output -raw api_base_url)
TOKEN=$(curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"<ADMIN_EMAIL>","password":"<ADMIN_PASSWORD>"}' | jq -r '.accessToken')
curl -X POST "$API/admin/hospitals/seed-nationwide" -H "Authorization: Bearer $TOKEN"
```

## 자동 배포 (CI/CD, 무중단)

`main` 에 push 되면(= `develop` → `main` PR merge) `.github/workflows/cd.yml` 이 돕니다.

```
[GitHub 호스팅 러너]  테스트(./gradlew build) → Docker 이미지 빌드 → GHCR push
                        ghcr.io/babymungsoo/babymungsoo_be:<커밋 SHA>
[EC2 self-hosted 러너] 이미지 pull → sudo /opt/emj/deploy.sh --sync <저장소 infra 파일> <이미지>
   1. 쉬고 있는 칸(blue/green)에 새 버전 기동
   2. http://127.0.0.1:1808x/actuator/health 가 UP 이 될 때까지 대기 (최대 180초)
   3. caddy/upstream.caddy 를 새 칸으로 바꾸고 caddy reload  ← 연결이 끊기지 않음
   4. 이전 칸을 graceful 종료 (처리 중인 요청은 끝까지 처리)
   헬스 체크 실패 시: 트래픽은 이전 칸에 그대로, 새 칸만 지우고 배포 실패로 끝남
```

- 빌드는 GitHub 서버에서 하므로 2GB 서버가 느려지지 않습니다. 서버는 이미지를 받아 바꾸기만 합니다.
- 러너는 GitHub 에 **바깥으로** 접속해 일을 받아 오므로 22번 포트·SSH 키·AWS 키가 필요 없고, IP 가 바뀌어도 상관없습니다.
- 전환하는 동안 JVM 이 두 개 뜹니다. 컨테이너마다 `mem_limit: 768m` 을 걸어 두었습니다.
- 서버 설정 파일(`docker-compose.yml`, `Caddyfile`, `deploy.sh`)도 배포 때마다 저장소의 `infra/terraform/files` 로 덮어씁니다. 서버에서 직접 고친 내용은 다음 배포 때 사라지니 저장소에서 고치세요(`.env` 는 건드리지 않음).
- 배포 이력은 Actions 탭에서 보고, 같은 커밋을 다시 배포하려면 **Backend CD → Run workflow** 를 누릅니다.

### 처음 한 번: 서버에 러너 설치

EC2 Instance Connect 로 서버에 접속해 아래를 실행합니다. `<TOKEN>` 은 GitHub 저장소 **Settings → Actions → Runners → New self-hosted runner**(Linux, x64) 화면의 `./config.sh` 줄에 있는 값입니다(1시간 안에 써야 함). 러너 버전·다운로드 주소도 그 화면에 나온 것을 씁니다.

```bash
# 1) 러너 전용 계정. docker 를 쓰고, deploy.sh 하나만 root 로 실행할 수 있다
sudo dnf install -y libicu
sudo useradd -m github-runner
sudo usermod -aG docker github-runner
echo 'github-runner ALL=(root) NOPASSWD: /opt/emj/deploy.sh' | sudo tee /etc/sudoers.d/github-runner
sudo chmod 440 /etc/sudoers.d/github-runner

# 2) 러너 설치·등록 (GitHub 화면의 Download 단계 명령으로 받은 뒤)
sudo -iu github-runner
mkdir actions-runner && cd actions-runner
curl -o runner.tar.gz -L <GitHub 화면의 다운로드 주소>
tar xzf runner.tar.gz
./config.sh --url https://github.com/BabyMungsoo/BabyMungsoo_BE --token <TOKEN> \
  --name emj-api --labels emj-prod --unattended
exit

# 3) 서비스로 등록해 재부팅해도 자동 실행
cd /home/github-runner/actions-runner
sudo ./svc.sh install github-runner
sudo ./svc.sh start
```

GitHub 의 Runners 목록에 `emj-api` 가 **Idle** 로 보이면 준비 끝입니다.

> **처음 자동 배포 때는 1~2초 끊깁니다.** 예전 구조(단일 `api` 컨테이너)에서 블루-그린으로 바뀌면서 Caddy 를 한 번 다시 만들어야 하기 때문입니다. 그 다음 배포부터는 끊기지 않습니다.

### 공개 저장소 보안 설정 (필수)

이 저장소는 공개 저장소라, 외부인이 fork 해서 올린 PR 의 워크플로가 운영 서버 러너에서 돌 위험이 있습니다.
- `cd.yml` 의 self-hosted 잡은 `main` push 와 수동 실행에서만 돕니다. **`pull_request` 트리거 워크플로에서 `runs-on: self-hosted` 를 쓰지 마세요.**
- 저장소 **Settings → Actions → General → Approval for running fork pull request workflows** 를 **Require approval for all external contributors** 로 바꿔 두세요. 외부 PR 의 워크플로는 관리자가 승인해야만 돕니다.
- 필요하면 **Settings → Environments → production** 에 승인자를 지정해 배포 전에 한 번 더 확인할 수 있습니다.

## 서버 접속과 수동 배포

접속은 둘 중 하나로 합니다.
- 콘솔: EC2 → 인스턴스 → `emj-api` → **연결** → EC2 Instance Connect → 연결
- CLI: `$(terraform output -raw connect_command)`

```bash
sudo /opt/emj/deploy.sh --rollback   # 직전에 배포했던 이미지로 되돌리기 (무중단)
sudo /opt/emj/deploy.sh <이미지>     # 특정 이미지로 배포 (예: ghcr.io/babymungsoo/babymungsoo_be:<SHA>)
sudo /opt/emj/deploy.sh              # 레지스트리 없이 deploy.conf 의 브랜치를 서버에서 빌드해 배포
cat /opt/emj/release.env             # 지금 트래픽을 받는 칸(ACTIVE_COLOR)과 칸별 이미지
sudo docker compose -f /opt/emj/docker-compose.yml --env-file /opt/emj/.env --env-file /opt/emj/release.env logs -f api-blue api-green
sudo tail -f /var/log/emj-bootstrap.log            # 최초 부팅 로그
```

`.env`(비밀값)만 바꿨다면, 같은 이미지를 다시 배포해야 새 컨테이너가 바뀐 값을 읽습니다. Actions 탭에서 **Backend CD → Run workflow** 를 누르는 게 가장 간단합니다.

`terraform.tfvars` 를 고치고 `apply` 해도 **이미 떠 있는 서버의 `.env` 는 바뀌지 않습니다**(데이터 보호를 위해 `user_data` 변경은 무시). 서버에서 직접 고치세요.

## 주의

- **인스턴스를 "중지"했다가 "시작"하면 공인 IP 가 바뀝니다.** 그러면 API 주소도 바뀌어 프론트의 `EXPO_PUBLIC_API_BASE_URL` 을 다시 넣어야 합니다. "재부팅"은 IP 가 유지됩니다.
- **`terraform.tfstate` 에 DB 비밀번호·API 키가 평문으로 들어 있습니다.** 커밋하지 말고 배포 담당자 PC 에만 두세요(`.gitignore` 대상).
- `.env` 내용이 user_data 에 들어갑니다. 이 계정에서 인스턴스 속성 조회 권한이 있는 사람은 볼 수 있습니다. 컨테이너에서는 메타데이터 접근을 막아 두었습니다(hop limit 1).
- **DB 와 업로드 사진이 EC2 루트 볼륨에 있습니다.** 인스턴스가 교체되거나 `terraform destroy` 를 하면 함께 사라집니다.
- 백업: `sudo docker compose -f /opt/emj/docker-compose.yml exec db pg_dump -U babymungsoo babymungsoo > backup.sql`
- `CLAUDE_API_MOCK` 의 기본값은 `false`(실호출·과금)입니다.
- Swagger 가 공개돼 있습니다(`SecurityConfig` permitAll).
- 다 쓰면 `terraform destroy` 로 정리합니다.
