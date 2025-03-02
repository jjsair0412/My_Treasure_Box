import json
import boto3
import os
from botocore.vendored import requests
from botocore.exceptions import ClientError

def lambda_handler(event, context):
    
    formation_client = boto3.client('cloudformation')
    secret_client = boto3.client('secretsmanager')
    
    # secret manager 파싱
    get_secret_value_response = secret_client.get_secret_value(SecretId="secert_key_name")
    secret = get_secret_value_response['SecretString']
    
    # JSON 문자열 파싱
    secret_dict = json.loads(secret)
  
    key_name = secret_dict["key_name"]
    AvailabilityZone = event['AvailabilityZone']
    image_ami = event['image_ami']
    subnet_id = event['subnet_id']
    my_ip = event['my_ip']
    instance_type = event['instance_type']
    
    
    client = boto3.client('ec2')

    check_response = check_security_group(client)
    
    
    if (check_response == "false") :
        security_group_id = create_security_group(my_ip, client)
    else:
        security_group_id = check_response
        
    ec2 = boto3.resource('ec2')
    instances_create_response = ec2.create_instances(
        ImageId=image_ami,
        MinCount=1,
        MaxCount=1,
        InstanceType=instance_type,
        KeyName=key_name,
        Placement = {
            'AvailabilityZone' : AvailabilityZone
        },
        SecurityGroupIds =[
            security_group_id
        ],
        BlockDeviceMappings=[
            {
                'DeviceName': '/dev/xvdf',
                'Ebs':{
                    'DeleteOnTermination': True,
                    'VolumeSize': 50
                }
            }    
        ],
        SubnetId=subnet_id
    )
    
    new_instance = instances_create_response[0]
    
    # 인스턴스의 ID와 프라이빗 IP 주소를 가져옴
    new_instance_id = new_instance.instance_id
    new_instance_private_ip = new_instance.private_ip_address
    new_instance_public_ip = new_instance.public_ip_address
    
    instance_status = f"my test vm create status: Instance ID - {new_instance_id}, Private IP - {new_instance_private_ip}, SecurityGroupIds - {security_group_id}, Public IP - {new_instance_public_ip}"

    # TODO implement
    return {
        'statusCode': 200,
        'body': json.dumps(instance_status)
    }

def check_security_group(client) -> str:
    security_group_name = 'test_vm_security_group'
    filters = [{'Name': 'group-name', 'Values': [security_group_name]}]
    response = client.describe_security_groups(Filters=filters)
    
    security_groups = response['SecurityGroups']
    
    if security_groups:
        # 보안 그룹이 존재하는 경우
        print(f"Security group '{security_group_name}' exists.")
        return security_groups[0]['GroupId']
    else:
        # 보안 그룹이 존재하지 않는 경우
        print(f"Security group '{security_group_name}' does not exist.")
        return "false"


def create_security_group(my_ip, client) -> str:
    response = client.create_security_group(
        VpcId='vpc-053761ccc2a2922ed',
        Description='test_vm_security_group',
        GroupName='test_vm_security_group',
        TagSpecifications=[
            {
                'ResourceType': 'security-group',
                'Tags': [
                    {
                        'Key': 'Name',
                        'Value': 'test_vm_security_group'
                    },
                ]
            },
        ]
    )
    
    security_group_id = response['GroupId']
    
    client.authorize_security_group_ingress(
        GroupId = security_group_id,
        IpPermissions = [
            {
                'IpProtocol': 'tcp',
                'FromPort': 22,
                'ToPort':22,
                'IpRanges': [
                    {
                        'CidrIp': my_ip, 'Description': 'my-ip-allow'
                    }
                ],
            },
            {
                'IpProtocol': 'tcp',
                'FromPort': 80,
                'ToPort':80,
                'IpRanges': [
                    {
                        'CidrIp': my_ip, 'Description': 'my-ip-allow'
                    }
                ],
            },
            {
                'IpProtocol': 'tcp',
                'FromPort': 443,
                'ToPort':443,
                'IpRanges': [
                    {
                        'CidrIp': my_ip, 'Description': 'my-ip-allow'
                    }
                ],
            }
        ]
    )
    
    return security_group_id