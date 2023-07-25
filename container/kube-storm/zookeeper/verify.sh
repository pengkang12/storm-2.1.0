echo ruok | nc `kubectl get service | grep zookeeper | awk '{print $3}'` 2181; echo
echo stat | nc `kubectl get service | grep zookeeper | awk '{print $3}'` 2181; echo

