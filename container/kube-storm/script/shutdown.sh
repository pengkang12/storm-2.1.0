cd $HOME/storm/container/kube-storm/

kubectl delete -f zookeeper/zookeeper.json

kubectl delete -f zookeeper/zookeeper-service.json

kubectl delete -f storm-nimbus.json

kubectl delete -f storm-nimbus-service.json

kubectl delete -f storm-ui.json

kubectl delete -f storm-ui-service.json

bash script/delete_storm_worker.sh
