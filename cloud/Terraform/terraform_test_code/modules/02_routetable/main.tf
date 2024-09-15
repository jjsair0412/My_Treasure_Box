resource "aws_route_table" "demo_route_table" {
  vpc_id = var.demo_vpc_id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }

  tags = {
    Name = "demo route table"
  }
}

resource "aws_internet_gateway" "igw" {
  vpc_id = var.demo_vpc_id

  tags = {
    Name = "demo igw"
  }
}
