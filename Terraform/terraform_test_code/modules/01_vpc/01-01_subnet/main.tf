resource "aws_subnet" "public_subnet" {
  vpc_id     = var.demo_vpc_id
  cidr_block = var.public_subnet_cidr
  availability_zone = var.availability_zone

  tags = {
    Name = "Main"
  }
}

resource "aws_route_table_association" "myrtassociation1" {
  subnet_id      = aws_subnet.public_subnet.id
  route_table_id = var.demo_route_table_id
}