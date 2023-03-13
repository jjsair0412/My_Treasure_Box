#!/bin/sh

$OUPUT_FILE=/home/vagrant/join/join_command.sh
rm -rf /vagrant/join/join.sh
echo 'init start ..'
sudo kubeadm init --apiserver-advertise-address=192.168.50.10

echo 'token create ..'
mkdir join
sudo kubeadm token create --print-join-command > /home/vagrant/join/join_command.sh

chmod +x /home/vagrant/join/join_command.sh

echo 'print join command file location ..'
ls
chmod +x $OUTPUT_FILE

mkdir -p $HOME/.kube
sudo cp /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

kubectl get nodes

# Install Calico Network Plugin
kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/v3.25.0/manifests/tigera-operator.yaml