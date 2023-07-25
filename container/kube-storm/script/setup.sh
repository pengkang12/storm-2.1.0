#!/bin/bash 

kubectl create -f zookeeper.json


sleep 1m
 check zookeeper
#echo ruok | nc `kubectl get service | grep zookeeper | awk '{print $3}'` 2181; echo

kubectl create -f zookeeper-service.json
kubectl create -f storm-nimbus.json
sleep 1m

kubectl create -f storm-nimbus-service.json

kubectl create -f storm-ui.json

sleep 30

kubectl create -f storm-ui-service.json
kubectl create -f storm-worker-controller.json

sleep 30

#echo stat | nc `kubectl get service | grep zookeeper | awk '{print $3}'` 2181; echo


kubectl create -f storm-worker-service.json

kubectl get pods,services,rc
sleep 10

echo "syscloud" | sudo -S bash nginx-proxy.sh

sleep 10
#bash change_worker_hosts.sh
