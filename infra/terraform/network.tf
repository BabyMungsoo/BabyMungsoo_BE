# 지원 계정은 VPC 를 만들 수 없어서 계정의 기본 VPC 를 쓴다.
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }

  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

# apply 하는 PC 의 공인 IP. 장소가 바뀌어 IP 가 달라지면 apply 를 다시 하면 된다.
data "http" "my_ip" {
  url = "https://checkip.amazonaws.com"
}

# 콘솔의 "연결 > EC2 Instance Connect" (브라우저 터미널) 이 들어오는 AWS 대역
data "http" "aws_ip_ranges" {
  url = "https://ip-ranges.amazonaws.com/ip-ranges.json"
}

locals {
  my_cidrs = length(var.ssh_allowed_cidrs) > 0 ? var.ssh_allowed_cidrs : ["${chomp(data.http.my_ip.response_body)}/32"]

  instance_connect_cidrs = [
    for p in jsondecode(data.http.aws_ip_ranges.response_body).prefixes : p.ip_prefix
    if p.service == "EC2_INSTANCE_CONNECT" && p.region == var.region
  ]
}

resource "aws_security_group" "api" {
  name        = "${var.project}-api"
  description = "HTTP/HTTPS public, SSH via EC2 Instance Connect only. Caddy terminates TLS."
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "HTTP (ACME challenge, redirect to HTTPS)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "SSH (Instance Connect from deployer PC and AWS console)"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = concat(local.my_cidrs, local.instance_connect_cidrs)
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project}-api" }
}
