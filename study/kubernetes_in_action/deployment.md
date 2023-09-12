# deployment
## revisionHistoryLimit
deployment가 rollout 될 때 , kubernetes는 새로운 옵션의 파드를 생성하고 , Old파드는 새로운 파드가 Running이 된다면 이전 파드가 제거됩니다.

**그러나 K8S는 기본적으로 , Old Pod가 제거되더라도 ```.spec.revisionHistoryLimit``` 옵션이 default로 10개로 잡히기 때문에 , 예전에 사용중이었던 Replicaset의 개수가 ```.spec.revisionHistoryLimit``` 개수만큼 유지됩니다!!**

그렇게 하는 이유는 , Rollout된 Deployment가 RollBack 될 때 , 저장하고있는 예전 Replicaset을 통해서 RollBack되기 때문입니다.
>따라서 default 옵션(10개) 라면 , -10 Revision까지만 롤백이 가능하다는 이야기가 됩니다.

- [revisionHistoryLimit 관련 공식문서설명](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#revision-history-limit)