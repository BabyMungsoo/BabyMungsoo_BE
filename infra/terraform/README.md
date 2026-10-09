# AWS 배포 (Terraform)

EC2 한 대에 Docker Compose 로 **Spring Boot + PostgreSQL + Caddy** 를 띄웁니다.
HTTPS 는 Caddy 가 `<공인IP>.sslip.io` 도메인으로 Let's Encrypt 인증서를 자동으로 받아 처리합니다. 도메인을 따로 살 필요가 없습니다.

```
브라우저/앱 ─https─→ [EC2 t3.small, 오사카, 기본 VPC]
                      ├─ caddy  :443 → api:8080
                      ├─ api    (BabyMungsoo_BE Dockerfile 빌드)
                      └─ db     postgres:16  (/opt/emj/data/postgres)
비밀값: terraform.tfvars → .env → 첫 부팅 user_data 로 전달
접속:   EC2 Instance Connect (키 페어 없음)
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

## 서버 접속과 재배포

접속은 둘 중 하나로 합니다.
- 콘솔: EC2 → 인스턴스 → `emj-api` → **연결** → EC2 Instance Connect → 연결
- CLI: `$(terraform output -raw connect_command)`

```bash
sudo /opt/emj/deploy.sh              # 최신 코드로 재배포 (deploy.conf 의 브랜치)
sudo /opt/emj/deploy.sh develop      # 브랜치 바꿔서 배포
sudo vi /opt/emj/.env && sudo /opt/emj/deploy.sh   # 비밀값·설정 변경
sudo docker compose -f /opt/emj/docker-compose.yml logs -f api
sudo tail -f /var/log/emj-bootstrap.log            # 최초 부팅 로그
```

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
