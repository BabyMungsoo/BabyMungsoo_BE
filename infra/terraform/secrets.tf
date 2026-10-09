# 지원 계정은 SSM Parameter Store 를 쓸 수 없어서, 서버의 /opt/emj/.env 를 여기서 만들어
# user_data 로 첫 부팅 때 전달한다. API_HOST 는 서버가 부팅할 때마다 자기 공인 IP 로 채운다.
#
# user_data 는 이 계정에서 ec2:DescribeInstanceAttribute 권한이 있는 사람이면 볼 수 있다.

resource "random_password" "db" {
  length  = 32
  special = false
}

resource "random_password" "jwt" {
  length  = 64
  special = false
}

locals {
  env = {
    SPRING_PROFILES_ACTIVE       = "prod"
    SPRING_DATASOURCE_URL        = "jdbc:postgresql://db:5432/${var.db_name}"
    SPRING_DATASOURCE_USERNAME   = var.db_username
    SPRING_DATASOURCE_PASSWORD   = random_password.db.result
    POSTGRES_DB                  = var.db_name
    POSTGRES_USER                = var.db_username
    POSTGRES_PASSWORD            = random_password.db.result
    JWT_SECRET                   = random_password.jwt.result
    CLAUDE_API_MOCK              = var.claude_api_mock
    CLAUDE_API_KEY               = var.claude_api_key
    KAKAO_REST_API_KEY           = var.kakao_rest_api_key
    ADMIN_EMAIL                  = var.admin_email
    ADMIN_PASSWORD               = var.admin_password
    CORS_ALLOWED_ORIGIN_PATTERNS = var.cors_allowed_origin_patterns
  }

  # 작은따옴표로 감싸야 compose 가 값 안의 $ 나 # 를 해석하지 않는다.
  # 빈 값은 빼서 application.yml 의 기본값이 쓰이게 한다.
  env_file = join("", [for k in sort(keys(local.env)) : "${k}='${local.env[k]}'\n" if local.env[k] != ""])
}
