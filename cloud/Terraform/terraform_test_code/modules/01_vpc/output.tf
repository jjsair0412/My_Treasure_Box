output demo_vpc_id {
  value       = aws_vpc.demo_vpc.id
  description = "description"
}

output demo_vpc_cidr_block {
  value = aws_vpc.demo_vpc.cidr_block
}