variable "region" {
  description = "지원 계정은 RestrictRegionOsaka 정책으로 오사카만 허용"
  type        = string
  default     = "ap-northeast-3"
}

variable "project" {
  description = "리소스 이름·태그 접두어"
  type        = string
  default     = "emj"
}

variable "owner_tags" {
  description = "모든 리소스에 붙일 소유자 태그. 지원 계정의 ControlOnlyOwnResources 정책은 소유 태그가 있는 리소스만 수정·삭제를 허용한다"
  type        = map(string)
  default     = {}
}

variable "instance_type" {
  description = "t3.micro(1GB)는 Gradle 빌드 + JVM + Postgres 에 메모리가 모자란다"
  type        = string
  default     = "t3.small"
}

variable "root_volume_size" {
  description = "GB. DB 데이터와 업로드 파일도 이 볼륨에 저장된다"
  type        = number
  default     = 30
}

variable "git_repo" {
  type    = string
  default = "https://github.com/BabyMungsoo/BabyMungsoo_BE.git"
}

variable "git_ref" {
  description = "최초 부팅 때 서버에서 빌드할 브랜치. 이후 배포는 main push 시 GitHub Actions(cd.yml)가 한다"
  type        = string
  default     = "main"
}

variable "ssh_allowed_cidrs" {
  description = "Instance Connect(SSH) 를 허용할 CIDR. 비우면 apply 하는 PC 의 공인 IP 하나만 허용. 콘솔 브라우저 접속용 AWS 대역은 항상 허용"
  type        = list(string)
  default     = []
}

variable "cors_allowed_origin_patterns" {
  description = "쉼표 구분. 예: https://babymungsoo*.vercel.app,http://localhost:*"
  type        = string
}

variable "db_name" {
  type    = string
  default = "babymungsoo"
}

variable "db_username" {
  type    = string
  default = "babymungsoo"
}

variable "claude_api_mock" {
  description = "\"true\" 면 AI 분석이 고정 응답. 리허설 중 과금을 막을 때만 true"
  type        = string
  default     = "false"
}

variable "claude_api_key" {
  type      = string
  sensitive = true
  default   = ""
}

variable "kakao_rest_api_key" {
  type      = string
  sensitive = true
  default   = ""
}

variable "admin_email" {
  type    = string
  default = ""
}

variable "admin_password" {
  type      = string
  sensitive = true
  default   = ""
}
