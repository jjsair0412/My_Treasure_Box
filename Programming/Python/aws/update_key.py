import json
import boto3


def lambda_handler(event, context):


    secret_client = boto3.client('secretsmanager')
    # secret manager 파싱
    get_secret_value_response = secret_client.get_secret_value(SecretId="secret_key_name")
    secret = get_secret_value_response['SecretString']
    secret_dict=json.loads(secret)
    
    user_name = event['user_name']
    setting_value = event['setting_value']
    
    iam = boto3.resource('iam')
    access_key = iam.AccessKey(user_name,secret_dict["access_key"])
    
    match setting_value:
        case 'deactivate':
            response = access_key.deactivate()
            return {
                'statusCode': 200,
                'body': json.dumps(response)
            }
        case 'activate':
            response = access_key.activate()
            return {
                'statusCode': 200,
                'body': json.dumps(response)
            }
        case _:
            return {
                'statusCode': 200,
                'body': json.dumps('wrong setting_value')
            }
    