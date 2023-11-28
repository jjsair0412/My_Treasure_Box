terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
  access_key = var.access_key
  secret_key = var.secret_key
}

module vpc {
  source = "./modules/01_vpc"

  ## vpc variable ##
  vpc_cidr_block = var.vpc_cidr_block
}

module routetable {
  source = "./modules/02_routetable"
  
  ## vpc id ##
  demo_vpc_id = module.vpc.demo_vpc_id
}

module subnet {
  source = "./modules/01_vpc/01-01_subnet"

  ## subnet variable ##
  public_subnet_cidr = var.public_subnet_cidr

  ## vpc id ##
  demo_vpc_id = module.vpc.demo_vpc_id
  
  ## route table id ##
  demo_route_table_id = module.routetable.demo_route_table_id

  availability_zone = var.availability_zone
}

module sg {
  source = "./modules/03_securitygroup"

  ## vpc id ##
  demo_vpc_id = module.vpc.demo_vpc_id
}

module ec2_instance {
  source = "./modules/04_ec2"

  availability_zone = var.availability_zone
  instance_type = var.instance_type
  public_subent_id = module.subnet.public_subent_id
  ubuntu_ami_id = data.aws_ami.ubuntu.id
  allow_http_sg_id = module.sg.allow_http_sg_id
  allow_ssh_sg_id = module.sg.allow_ssh_sg_id
  user_data_path = var.user_data_path
  test = var.test
}
