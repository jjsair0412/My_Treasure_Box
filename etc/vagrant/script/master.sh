#!/bin/sh

echo 'init start ..'
sudo kubeadm init --apiserver-advertise-address=192.168.50.10 --pod-network-cidr=192.168.0.0/16

echo 'token create ..'
rm -rf /join/join_command.sh
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
# kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/v3.25.0/manifests/tigera-operator.yaml
curl https://docs.projectcalico.org/archive/v3.8/manifests/calico.yaml -O

kubectl apply -f calico.yaml