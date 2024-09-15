variable access_key {
  type        = string
  description = "aws IAM user access key"
}

variable secret_key {
  type        = string
  description = "aws IAM user secret key"
}

variable region {
  type        = string
  description = "region name"
}

variable vpc_cidr_block {
  type        = string
  description = "vpc cidr block"
}

variable public_subnet_cidr {
  type        = string
  description = "public subnet cidr"
}

variable instance_type {
  type = string
}

variable availability_zone {
  type        = string
}

variable user_data_path {
  type = string
}

variable test {
  type = string
  default = "user_data_test"
}