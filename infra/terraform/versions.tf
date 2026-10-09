terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
    http = {
      source  = "hashicorp/http"
      version = "~> 3.4"
    }
  }

  # 상태 파일은 지금은 로컬(terraform.tfstate)에 둔다. 비밀값이 평문으로 들어 있으므로
  # 절대 커밋하지 말 것.
}

# 자격 증명은 `aws login` (콘솔 계정 + MFA) 으로 받은 것을 그대로 쓴다.
provider "aws" {
  region = var.region

  default_tags {
    tags = merge(
      {
        Project   = var.project
        ManagedBy = "terraform"
      },
      var.owner_tags,
    )
  }
}
