# kubectl label node master name=core1
# kubectl label node core name=core
# kubectl label node worker1 name=worker1
# kubectl label node worker2 name=worker2
# kubectl label node edge1 name=edge1
# kubectl label node edge2 name=edge2
# kubectl label node edge3 name=edge3
# kubectl label node edge4 name=edge4



export tagName=etl-sys
export nodeName=core1
envsubst < storm-worker-template.json | kubectl delete -f -
