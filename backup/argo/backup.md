# ArgoCD Backup & Restore Cronjob 설정
- CronJob controller를 이용한 backup
- 참고 링크
  - [ArgoCD backup cronjob](https://www.jacobbaek.com/1244)

## 1. CronJob 파일 생성
-   KST 기준 새벽 2시 ArgoCD Backup이 수행 되는 Cronjob 생성
-   1일 이상 지난 ArgoCD Backup 본은 삭제
-   longhorn과 같은 외부 스토리지에는 저장할 수 없습니다. 
  - backup 파일을 생성하는 기준이 argo가 실제로 배포되어있는 환경에서 데이터를 긁어오기 때문입니다.
 
```
apiVersion: batch/v1
kind: CronJob
metadata:
  name: argo-backup-cronjob
  namespace: argo
spec:
  schedule: "0 2 * * *"
  successfulJobsHistoryLimit: 2
  failedJobsHistoryLimit: 2 
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: argo-backup
            image: argocd:2.0.0
            imagePullPolicy: IfNotPresent
            command:
            - "/bin/sh"
            - "-c"
            args: ["/usr/local/bin/argocd-util -n argo export > /backup/argocd-backup-$(date +%F).yaml"]
            volumeMounts:
            - name: argo-backup-volume
              mountPath: "/backup/argocd-backup"
          - name: clean-backup-file
            image: busybox
            args:
            - /bin/sh
            - -c
            - find /backup -type f -mtime +1 -exec rm {} \;
            volumeMounts:
            - name: argo-backup-volume
              mountPath: "/backup/argocd-backup"
          serviceAccount: argocd-server
          serviceAccountName: argocd-server
          restartPolicy: OnFailure
          securityContext:
            runAsNonRoot: true
            runAsUser: 1000
          volumes:
          - name: argo-backup-volume
            hostPath:
              path: /backup/argo-backup
              type: Directory

```
## 2. Backup 파일 저장소 권한 부여
- backup 파일이 저장될 폴더의 권한을 1000으로 변경합니다.
```
$ chmod 777 {dir_name}
```
## 3. ArgoCD restore
- backup yaml 파일을 argocd가 배포된 namespace 지정해준 후 apply 합니다.
```
$ kubectl apply -f {backup_file_name} -n argocd
```