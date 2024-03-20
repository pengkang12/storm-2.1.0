#!/bin/bash 
cd $HOME/storm/container/kube-storm/

kube_pod(){
    # $2 is pod's file
    # $1 is node name
    # create a pod to specified node
    export nodeName=$2
    envsubst < $1 | kubectl create  -f -
}

# label master'name as master
nodeName="master"
kubectl label master name=$nodeName


# build mqtt, specify which node to host mosquitto
kube_pod mosquitto/mosquitto-bridge-pods.json $nodeName
kube_pod mosquitto/mosquitto-bridge-pods.json 

kube_pod zookeeper/zookeeper.json $nodeName
sleep 1m
#echo ruok | nc `kubectl get service | grep zookeeper | awk '{print $3}'` 2181; echo
kube_pod zookeeper/zookeeper-service.json
kube_pod storm-nimbus.json $nodeName
sleep 1m

kube_pod storm-nimbus-service.json
kube_pod storm-ui.json $nodeName
sleep 30
kube_pod storm-ui-service.json
sleep 30

#echo stat | nc `kubectl get service | grep zookeeper | awk '{print $3}'` 2181; echo
kubectl get pods,services,rc
sleep 10
exit

bash script/create_storm_worker.sh

sleep 200
bash script/change_worker_hosts.sh

# setup nginx gateway if necessary
echo "YourServerPassword" | sudo -S bash /home/cc/storm/container/kube-storm/script/nginx-proxy.sh

