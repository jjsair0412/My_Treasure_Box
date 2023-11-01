# TroubleShooting
## 1. VPC IP 할당 에러
```bash
Failed to create pod sandbox: rpc error: code = Unknown desc = failed to setup network for sandbox "92712bb7ddf47e95718e22e12efbbf022dcf5e759b189cd4c56291eccdf28b72": plugin type="aws-cni" name="aws-cni" failed (add): add cmd: failed to assign an IP address to container
```

EKS에서 해당 pod에 할당할 Subnet IP 개수가 모자랄때 나는 에러..