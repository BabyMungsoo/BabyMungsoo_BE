#!/bin/bash
# 서버(/opt/emj)에서 실행: 코드 갱신 → 이미지 빌드 → 재기동
#   sudo ./deploy.sh              deploy.conf 의 GIT_REF 브랜치로 배포
#   sudo ./deploy.sh <브랜치>      다른 브랜치로 배포 (deploy.conf 도 그 브랜치로 바뀜)
#   sudo ./deploy.sh --host-only  공인 IP 로 API_HOST 만 갱신 (부팅 때 systemd 가 호출)
# 비밀값은 /opt/emj/.env 를 직접 고친 뒤 재배포한다.
set -euo pipefail
cd /opt/emj
source ./deploy.conf

# 탄력적 IP 를 쓸 수 없어 IP 가 바뀔 수 있다. 매번 메타데이터에서 읽어 sslip.io 이름을 만든다.
refresh_host() {
  local token ip
  token=$(curl -fsS -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60')
  ip=$(curl -fsS -H "X-aws-ec2-metadata-token: $token" http://169.254.169.254/latest/meta-data/public-ipv4)
  sed -i '/^API_HOST=/d' .env
  echo "API_HOST='${ip//./-}.sslip.io'" >> .env
  echo "API_HOST=${ip//./-}.sslip.io"
}

if [ "${1:-}" = "--host-only" ]; then
  refresh_host
  docker compose up -d caddy
  exit 0
fi

if [ $# -ge 1 ]; then
  GIT_REF="$1"
  sed -i "s|^GIT_REF=.*|GIT_REF='$GIT_REF'|" deploy.conf
fi

if [ -d src/.git ]; then
  git -C src fetch --depth 1 origin "$GIT_REF"
  git -C src checkout -f FETCH_HEAD
else
  git clone --depth 1 --branch "$GIT_REF" "$GIT_REPO" src
fi
echo "배포 커밋: $(git -C src log -1 --format='%h %s')"

refresh_host
docker build -q -t emj-api:latest src
docker compose up -d --remove-orphans
docker image prune -f

echo "완료. 로그: sudo docker compose -f /opt/emj/docker-compose.yml logs -f api"
