#!/bin/bash
# 서버(/opt/emj)에서 실행하는 무중단(블루-그린) 배포 스크립트. root 로 실행한다.
#
#   sudo ./deploy.sh <이미지>             이미지로 배포 (로컬에 없으면 pull)
#   sudo ./deploy.sh                      deploy.conf 의 GIT_REF 를 서버에서 빌드해 배포 (최초 부팅·수동용)
#   sudo ./deploy.sh --sync <디렉터리> …  디렉터리의 compose·Caddyfile·deploy.sh 를 먼저 설치하고 이어서 배포
#                                         (GitHub Actions 가 저장소의 infra/terraform/files 로 부른다)
#   sudo ./deploy.sh --rollback           직전에 배포했던 이미지로 다시 배포
#   sudo ./deploy.sh --host-only          공인 IP 로 API_HOST 만 갱신 (부팅 때 systemd 가 호출)
#
# 흐름: 쉬고 있는 칸(blue/green)에 새 버전 기동 → /actuator/health 가 UP 이 될 때까지 대기
#       → Caddy 의 upstream 을 새 칸으로 바꾸고 reload → 이전 칸 graceful 종료.
#       헬스 체크가 실패하면 트래픽은 그대로 이전 칸에 두고 새 칸만 지운 뒤 실패로 끝낸다.
# 비밀값은 /opt/emj/.env 를 직접 고친 뒤 재배포한다.
set -euo pipefail
cd /opt/emj

HEALTH_TIMEOUT_SECONDS=180

if [ "${1:-}" = "--sync" ]; then
  src="${2:?--sync 뒤에 파일 디렉터리를 주세요}"
  shift 2
  install -m 644 "$src/docker-compose.yml" docker-compose.yml
  mkdir -p caddy
  install -m 644 "$src/Caddyfile" caddy/Caddyfile
  # 실행 중인 스크립트 파일을 덮어쓰지 않도록 새 파일로 쓴 뒤 이름만 바꾼다
  install -m 755 "$src/deploy.sh" deploy.sh.new
  mv -f deploy.sh.new deploy.sh
  echo "서버 설정 파일 갱신: docker-compose.yml, caddy/Caddyfile, deploy.sh"
  exec /opt/emj/deploy.sh "$@"
fi

source ./deploy.conf
touch release.env

dc() {
  docker compose --env-file .env --env-file release.env "$@"
}

# release.env 의 한 줄(KEY='value')을 바꾼다
set_release() {
  sed -i "/^$1=/d" release.env
  echo "$1='$2'" >> release.env
}

get_release() {
  (source ./release.env 2>/dev/null; eval "echo \"\${$1:-}\"")
}

# 탄력적 IP 를 쓸 수 없어 IP 가 바뀔 수 있다. 매번 메타데이터에서 읽어 sslip.io 이름을 만든다.
refresh_host() {
  local token ip
  token=$(curl -fsS -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60')
  ip=$(curl -fsS -H "X-aws-ec2-metadata-token: $token" http://169.254.169.254/latest/meta-data/public-ipv4)
  sed -i '/^API_HOST=/d' .env
  echo "API_HOST='${ip//./-}.sslip.io'" >> .env
  echo "API_HOST=${ip//./-}.sslip.io"
}

port_of() {
  if [ "$1" = blue ]; then echo 18081; else echo 18082; fi
}

wait_healthy() {
  local port deadline
  port=$(port_of "$1")
  deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
  until curl -fsS "http://127.0.0.1:$port/actuator/health" 2>/dev/null | grep -q '"UP"'; do
    if [ $SECONDS -ge $deadline ]; then
      return 1
    fi
    sleep 3
  done
}

# 블루-그린 이전의 단일 api 컨테이너(서비스 이름 api). 처음 전환할 때 한 번만 지운다.
remove_legacy_api() {
  local ids
  ids=$(docker ps -aq --filter label=com.docker.compose.project=emj --filter label=com.docker.compose.service=api)
  if [ -n "$ids" ]; then
    echo "이전 구조의 api 컨테이너 정리"
    docker stop -t 40 $ids >/dev/null
    docker rm $ids >/dev/null
  fi
}

# 새 upstream 을 Caddy 에 적용한다. 떠 있으면 reload(연결 유지), 아니면 새로 띄운다.
switch_caddy() {
  local color="$1"
  echo "reverse_proxy api-$color:8080" > caddy/upstream.caddy.new
  mv -f caddy/upstream.caddy.new caddy/upstream.caddy

  local caddy_id mounts
  caddy_id=$(dc ps -q caddy 2>/dev/null || true)
  mounts=""
  if [ -n "$caddy_id" ]; then
    mounts=$(docker inspect -f '{{range .Mounts}}{{.Destination}} {{end}}' "$caddy_id")
  fi
  # 처음 전환할 때는 기존 Caddy 가 예전 Caddyfile 을 파일로 물고 있어 다시 만들어야 한다(1~2초 끊김)
  if [ -n "$caddy_id" ] && [[ " $mounts " == *" /etc/caddy "* ]] \
    && dc exec -T caddy caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile; then
    echo "Caddy reload 완료 → api-$color"
  else
    dc up -d --force-recreate caddy
    echo "Caddy 재생성 완료 → api-$color"
  fi
}

deploy_image() {
  local image="$1" active new old_image
  if [ ! -f caddy/Caddyfile ]; then
    echo "caddy/Caddyfile 이 없습니다. --sync 로 서버 설정 파일을 먼저 설치하세요." >&2
    exit 1
  fi
  if ! docker image inspect "$image" >/dev/null 2>&1; then
    docker pull "$image"
  fi

  active=$(get_release ACTIVE_COLOR)
  if [ "$active" = blue ]; then new=green; else new=blue; fi
  old_image=""
  if [ -n "$active" ]; then
    old_image=$(get_release "API_${active^^}_IMAGE")
  fi

  echo "배포 이미지: $image"
  echo "현재 활성: ${active:-없음} → 새 칸: $new"

  set_release "API_${new^^}_IMAGE" "$image"
  dc up -d db
  dc up -d --no-deps --force-recreate "api-$new"

  echo "api-$new 헬스 체크 대기 (최대 ${HEALTH_TIMEOUT_SECONDS}초)"
  if ! wait_healthy "$new"; then
    echo "헬스 체크 실패. 트래픽은 그대로 두고 api-$new 를 지웁니다." >&2
    dc logs --tail 80 "api-$new" >&2 || true
    dc rm -sf "api-$new" >/dev/null || true
    exit 1
  fi

  switch_caddy "$new"
  set_release ACTIVE_COLOR "$new"
  if [ -n "$old_image" ]; then
    set_release PREVIOUS_IMAGE "$old_image"
  fi

  if [ -n "$active" ]; then
    echo "이전 칸 api-$active 종료 (처리 중인 요청은 마치고 내려갑니다)"
    dc stop "api-$active"
    dc rm -f "api-$active" >/dev/null
  fi
  remove_legacy_api

  # 태그 없는 이미지만 지운다. 직전 이미지는 롤백용으로 남는다
  docker image prune -f >/dev/null

  # Caddy(HTTPS)를 거쳐서도 응답하는지 확인한다. 이미 전환은 끝났으므로 실패해도 경고만 남긴다
  local host
  host=$(sed -n "s/^API_HOST='\{0,1\}\([^']*\)'\{0,1\}$/\1/p" .env | tail -1)
  if curl -fsS --retry 5 --retry-delay 2 --retry-all-errors "https://$host/actuator/health" >/dev/null; then
    echo "외부 확인 완료: https://$host/actuator/health"
  else
    echo "경고: https://$host/actuator/health 에 닿지 않습니다. Caddy 로그를 확인하세요." >&2
  fi
  echo "배포 완료: api-$new ($image)"
}

if [ "${1:-}" = "--host-only" ]; then
  refresh_host
  if [ -f caddy/Caddyfile ]; then
    dc up -d caddy
  else
    docker compose up -d caddy
  fi
  exit 0
fi

if [ "${1:-}" = "--rollback" ]; then
  previous=$(get_release PREVIOUS_IMAGE)
  if [ -z "$previous" ]; then
    echo "되돌릴 이전 이미지가 없습니다." >&2
    exit 1
  fi
  refresh_host
  deploy_image "$previous"
  exit 0
fi

if [ $# -ge 1 ]; then
  refresh_host
  deploy_image "$1"
  exit 0
fi

# 이미지를 주지 않으면 서버에서 직접 빌드한다(최초 부팅, 레지스트리 없이 수동 배포할 때)
if [ -d src/.git ]; then
  git -C src fetch --depth 1 origin "$GIT_REF"
  git -C src checkout -f FETCH_HEAD
else
  git clone --depth 1 --branch "$GIT_REF" "$GIT_REPO" src
fi
commit=$(git -C src rev-parse --short HEAD)
echo "빌드 커밋: $(git -C src log -1 --format='%h %s')"
docker build -q -t "emj-api:$commit" src
refresh_host
deploy_image "emj-api:$commit"
