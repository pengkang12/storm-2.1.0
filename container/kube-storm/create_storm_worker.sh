# kubectl label node master name=core1
# kubectl label node core name=core
# kubectl label node worker1 name=worker1
# kubectl label node worker2 name=worker2
# kubectl label node edge1 name=edge1
# kubectl label node edge2 name=edge2
# kubectl label node edge3 name=edge3
# kubectl label node edge4 name=edge4


for name in "master" "core" "worker1" "worker2" "edge1" "edge2" "edge3" "edge4" "edge5"
do

export tagName=etl-sys
export nodeName=$name
envsubst < storm-worker-template.json | kubectl apply -f -

done

kubectl get pod
