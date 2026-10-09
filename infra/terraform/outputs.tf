locals {
  api_host = "${replace(aws_instance.api.public_ip, ".", "-")}.sslip.io"
}

output "api_base_url" {
  description = "프론트 EXPO_PUBLIC_API_BASE_URL 에 넣을 값. 인스턴스를 중지했다 켜면 IP 와 함께 바뀐다"
  value       = "https://${local.api_host}/api/v1"
}

output "swagger_url" {
  value = "https://${local.api_host}/swagger-ui.html"
}

output "public_ip" {
  value = aws_instance.api.public_ip
}

output "instance_id" {
  value = aws_instance.api.id
}

output "connect_command" {
  description = "EC2 Instance Connect 로 서버 접속. 콘솔 EC2 > 인스턴스 > 연결 버튼으로도 가능"
  value       = "aws ec2-instance-connect ssh --region ${var.region} --instance-id ${aws_instance.api.id} --os-user ec2-user --connection-type direct"
}

output "bootstrap_log_command" {
  description = "첫 부팅 진행 상황 (접속 없이 콘솔 출력으로 확인)"
  value       = "aws ec2 get-console-output --region ${var.region} --instance-id ${aws_instance.api.id} --latest --output text"
}
