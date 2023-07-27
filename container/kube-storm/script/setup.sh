#!/bin/bash 
cd ..
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

echo "syscloud" | sudo -S bash scripts/nginx-proxy.sh

sleep 10
#bash change_worker_hosts.sh
