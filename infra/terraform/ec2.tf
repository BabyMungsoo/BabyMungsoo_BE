# SSM 공개 파라미터로 찾는 게 보통이지만 지원 계정은 Parameter Store 가 막혀 있다
data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-kernel-*-x86_64"]
  }
}

# 키 페어는 쓰지 않는다. 접속은 EC2 Instance Connect 로만 한다(AL2023 에 기본 설치).
resource "aws_instance" "api" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.instance_type
  subnet_id                   = sort(data.aws_subnets.default.ids)[0]
  vpc_security_group_ids      = [aws_security_group.api.id]
  associate_public_ip_address = true

  metadata_options {
    http_tokens = "required"
    # 컨테이너에서는 메타데이터(=user_data 의 비밀값)에 닿지 못하게
    http_put_response_hop_limit = 1
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = var.root_volume_size
    encrypted   = true
  }

  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    git_repo      = var.git_repo
    git_ref       = var.git_ref
    env_b64       = base64encode(local.env_file)
    compose_b64   = filebase64("${path.module}/files/docker-compose.yml")
    caddyfile_b64 = filebase64("${path.module}/files/Caddyfile")
    deploy_b64    = filebase64("${path.module}/files/deploy.sh")
  })

  lifecycle {
    # DB 와 업로드 파일이 루트 볼륨에 있다. 새 AMI 가 나오거나 user_data(비밀값 포함)를
    # 고쳤다고 인스턴스가 교체되면 데이터가 전부 사라지므로 변경을 무시한다.
    # 비밀값·스크립트를 바꾸려면 서버에 접속해 /opt/emj 아래 파일을 직접 고칠 것.
    ignore_changes = [ami, user_data]
  }

  tags = { Name = "${var.project}-api" }

  volume_tags = merge({ Name = "${var.project}-api", Project = var.project, ManagedBy = "terraform" }, var.owner_tags)
}
