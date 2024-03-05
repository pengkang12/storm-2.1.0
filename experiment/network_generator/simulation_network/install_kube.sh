apt-get update
apt-get install \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg-agent \
    software-properties-common -y

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
   $(lsb_release -cs) \
   stable"
apt-get install docker-ce docker-ce-cli containerd.io -y

cat <<EOF | tee /etc/docker/daemon.json
{
  "exec-opts": ["native.cgroupdriver=systemd"],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m"
  },
  "storage-driver": "overlay2"
}
EOF
systemctl start docker && systemctl enable docker


swapoff -a
apt-get update
curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -
#apt-add-repository "deb http://apt.kubernetes.io/ kubernetes-xenial main"

sleep 5

apt-get update
apt-get install -y kubeadm=1.23.0-00 kubelet=1.23.0-00 kubectl=1.23.0-00
systemctl daemon-reload
systemctl restart kubelet

kubeadm reset
kubeadm join 192.168.122.204:6443 --token gu3n6s.iqhudhkwra0aonfy --discovery-token-ca-cert-hash sha256:37195d40da8906ae587c639fb51af96636269641b1ecd36a71388b0a1ad0b59d
