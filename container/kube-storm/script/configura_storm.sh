 sudo docker run -it -w /opt/apache-storm mattf/storm-base sh -c "/configure.sh `kubectl get service | grep zookeeper | awk '{print $3}'` `kubectl get service| grep nimbus| awk '{print $3}'`; ./bin/storm list"

