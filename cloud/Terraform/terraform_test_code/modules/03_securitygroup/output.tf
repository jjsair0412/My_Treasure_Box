output allow_http_sg_id {
  value       = aws_security_group.allow_http.id
}

output allow_ssh_sg_id {
  value = aws_security_group.allow_ssh.id
}