cd ..

kubectl delete -f zookeeper/zookeeper.json

kubectl delete -f zookeeper/zookeeper-service.json

kubectl delete -f storm-nimbus.json

kubectl delete -f storm-nimbus-service.json

kubectl delete -f storm-ui.json

kubectl delete -f storm-ui-service.json
