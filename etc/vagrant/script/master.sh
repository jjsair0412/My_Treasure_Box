#!/bin/sh

echo 'init start ..'
sudo kubeadm init --apiserver-advertise-address=192.168.50.10 --pod-network-cidr=192.168.0.0/16

echo 'token create ..'
rm -rf /home/vagrant/join/join_command.sh
sudo kubeadm token create --print-join-command > /home/vagrant/join/join_command.sh

chmod +x /home/vagrant/join/join_command.sh

echo 'print join command file location ..'
ls

su vagrant # user 변경
mkdir -p $HOME/.kube
sudo cp /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

kubectl get nodes

# Install Calico Network Plugin
curl https://docs.projectcalico.org/archive/v3.25/manifests/calico.yaml -O

kubectl apply -f calico.yaml

echo 'check install result'
kubectl get pods -n kube-system

# Install helm chart
echo 'install helm version3'
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh