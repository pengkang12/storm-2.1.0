#!/bin/bash 
cd $HOME/storm/container/kube-storm/


# build mqtt
kubectl create -f mosquitto/mosquitto-bridge-pods.json
kubectl create -f mosquitto/mosquitto-bridge-svc.json

kubectl create -f zookeeper/zookeeper.json

sleep 1m
#echo ruok | nc `kubectl get service | grep zookeeper | awk '{print $3}'` 2181; echo

kubectl create -f zookeeper/zookeeper-service.json
kubectl create -f storm-nimbus.json
sleep 1m

kubectl create -f storm-nimbus-service.json

kubectl create -f storm-ui.json

sleep 30

kubectl create -f storm-ui-service.json

sleep 30

#echo stat | nc `kubectl get service | grep zookeeper | awk '{print $3}'` 2181; echo
kubectl get pods,services,rc
sleep 10

sleep 10

bash script/create_storm_worker.sh

sleep 200
bash script/change_worker_hosts.sh

# setup nginx gateway if necessary
echo "YourServerPassword" | sudo -S bash /home/cc/storm/container/kube-storm/script/nginx-proxy.sh

