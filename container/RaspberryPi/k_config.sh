#!/bin/bash

#reference doc:
#  https://forum.linuxfoundation.org/discussion/864693/the-repository-http-apt-kubernetes-io-kubernetes-xenial-release-does-not-have-a-release-file
#  https://manjit28.medium.com/installing-kubernetes-cluster-on-ubuntu-20-04-or-raspberry-pi-ubuntu-a7eec0856217
 

# specify kubernetes' version you want to install.
version=1.24

curl -fsSL https://pkgs.k8s.io/core:/stable:/v${version}/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg

echo "deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v${version}/deb/ /" | sudo tee /etc/apt/sources.list.d/kubernetes.list

sudo apt update

echo "apt install -y kubeadm=${version}.1-1.1 kubelet=${version}.1-1.1 kubectl=${version}.1-1.1 "

apt install -y kubeadm=${version}.1-1.1 kubelet=${version}.1-1.1 kubectl=${version}.1-1.1 --allow-downgrades 
