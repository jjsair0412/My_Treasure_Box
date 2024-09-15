# terraform test code
## 배포방안
### 1. terraform.tfvars 정의
```bash
cat <<EOF> terraform.tfvars
    # access key
    access_key = "{AWS_ACCESS_KEY}"
    secret_key = "{AWS_ACCESS_SECRET_KEY}"
    region = "{REGION}"

    # vpc
    vpc_cidr_block = "{vpc_cidr}"

    # subnet
    public_subnet_cidr = "{public_subnet_cidr}"

    # instance
    instance_type = "t2.micro" # instnace type
    availability_zone = "{availability_zone_name}"
    user_data_path = "./scripts/nginx_init_sc.sh" # init script path
EOF
```


### 2. 배포
```bash
terraform plan

terraform apply
```