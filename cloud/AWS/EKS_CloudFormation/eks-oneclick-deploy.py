import json
import boto3
import os
from botocore.vendored import requests
from botocore.exceptions import ClientError

def lambda_handler(event, context):
    
    formation_client = boto3.client('cloudformation')
    secret_client = boto3.client('secretsmanager')
    
    # secret manager 파싱
    get_secret_value_response = secret_client.get_secret_value(SecretId='jinseong-key')
    secret = get_secret_value_response['SecretString']
    
    # JSON 문자열 파싱
    secret_dict = json.loads(secret)
  
    key_name = secret_dict["key_name"]
    access_key = secret_dict["access_key"]
    secret_key = secret_dict["secret_key"]
    
    NumWorkerNodes = event['NumWorkerNodes']
    WorkerNodesInstanceType = event['WorkerNodesInstanceType']
    KubernetesVersion = event['KubernetesVersion']

    
    formation_client.create_stack(
        StackName = 'myeks',
        TemplateURL = '{myEKS.yaml_S3_location}',
        Parameters = [
                {
                    'ParameterKey': 'NumWorkerNodes',
                    'ParameterValue': NumWorkerNodes
                },
                {
                    'ParameterKey': 'WorkerNodesInstanceType',
                    'ParameterValue': resourcestype
                },
                {
                    'ParameterKey': 'KeyPairName',
                    'ParameterValue': key_name
                    
                },
                {
                    'ParameterKey': 'KubernetesVersion',
                    'ParameterValue': KubernetesVersion
                }
            ]
    )
    
    # TODO implement
    return {
        'statusCode': 200,
        'body': json.dumps('EKS Create Success!')
    }

