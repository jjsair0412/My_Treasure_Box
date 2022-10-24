# kubernetes_info
## known issues
### 1. kubeconfig file's location is not set in right direction.
This site is governed by the CNCF Code of Conduct. Check out our quick tutorial video on how to effectively subscribe to topics.
The connection to the server localhost:8080 was refused - did you specify the right host or port?

- cp kube.config file into $HOME/.kube/config
- kubeconfig file is for each different which k8s provider systems.
    - exampe : location of rancher's kubeconfig file is /etc/rancher/rke2/rke2.yaml

```
  mkdir -p $HOME/.kube
  sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
  sudo chown $(id -u):$(id -g) $HOME/.kube/config
```
### 2. helm install
```
$ curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
$ chmod 700 get_helm.sh
$ ./get_helm.sh
```