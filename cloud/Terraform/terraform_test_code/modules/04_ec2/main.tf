resource "aws_instance" "demo" {
  ami           = var.ubuntu_ami_id
  instance_type     = var.instance_type
  availability_zone = var.availability_zone
  user_data = templatefile(var.user_data_path,{TEST=var.test})

  network_interface {
    network_interface_id = aws_network_interface.demo_network_interface.id
    device_index         = 0
  }
  
  tags = {
    Name = "demo ec2 instance"
  }
}

resource "aws_network_interface" "demo_network_interface" {
  subnet_id   = var.public_subent_id
  security_groups = [var.allow_http_sg_id, var.allow_ssh_sg_id]

  tags = {
    Name = "public_network_interface"
  }
}

resource "aws_eip" "demo_eip" {
  instance = aws_instance.demo.id
  network_interface = aws_network_interface.demo_network_interface.id
  domain   = "vpc"
}