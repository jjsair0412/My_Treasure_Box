# Using range option to multi deploy deployment-resource 
values.yaml의 loopCount 값에 따라 deployment 개수가 변화하는 helm-chart 입니다.
### 1. deploy result
```
helm upgrade --install test . -f values.yaml
```

만약 loopCount의 값이 3이라면 결과는 아래와 같습니다.
```
NAME                                     READY   STATUS    RESTARTS   AGE
pod/test-loop-chart-0-686c8b48db-cflxx   1/1     Running   0          4s
pod/test-loop-chart-1-6c6697df78-clxmw   1/1     Running   0          4s
pod/test-loop-chart-2-6666bf7cfd-gh954   1/1     Running   0          4s

NAME                      TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)   AGE
service/kubernetes        ClusterIP   10.43.0.1      <none>        443/TCP   25h
service/test-loop-chart   ClusterIP   10.43.45.200   <none>        80/TCP    4s

NAME                                READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/test-loop-chart-0   1/1     1            1           4s
deployment.apps/test-loop-chart-1   1/1     1            1           4s
deployment.apps/test-loop-chart-2   1/1     1            1           4s

NAME                                           DESIRED   CURRENT   READY   AGE
replicaset.apps/test-loop-chart-0-686c8b48db   1         1         1       4s
replicaset.apps/test-loop-chart-1-6c6697df78   1         1         1       4s
replicaset.apps/test-loop-chart-2-6666bf7cfd   1         1         1       4s
```

### 2. 구현 방안
range옵션을 이용하여 구현하였습니다.
#### 2.1 응용시 주의 사항
1. range option 내부 변수 사용
helm chart에서 range option 내부에는 global values에 접근하는 방법이 상이합니다.
기존에 방식은 아래와 같이 ' . ' dot을 사용합니다. 

그러나 해당 예제는 deployment.yaml 전체를 반복해야 하기에 , ' . ' 을 사용하지 못합니다.
```
{{- include "loop-chart.selectorLabels" . | nindent 6 }}

# values.yaml에 접근하는 경우 맨 앞에 .을 추가  .Values.pods 
{{- toYaml .Values.podSecurityContext | nindent 8 }}

# Chart.yaml에 접근하는 경우 맨 앞에 .을 추가   .Chart.Name
name: {{ .Chart.Name }}
```

따라서 range 외부에 . 값을 가지는 변수를 정의한 뒤 , 해당 변수를 기존에 ' . ' 이 사용되었던 곳과 변경합니다.
```
{{- $dot := .  -}}
{{-  $loopCount := (.Values.loopCount) | int -}} 
{{- range $count, $v := until $loopCount }}

# . 대신 $dot 사용
{{- include "loop-chart.selectorLabels" $dot | nindent 6 }}-{{ $count }}

{{- toYaml $dot.Values.podSecurityContext | nindent 8 }}

name: {{ $dot.Chart.Name }}-{{ $count }}

{{- end }}
```

