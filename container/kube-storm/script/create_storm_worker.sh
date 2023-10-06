cd ..
# kubectl label node master name=core1
# kubectl label node core name=core
# kubectl label node worker1 name=worker1
# kubectl label node worker2 name=worker2
# kubectl label node edge1 name=edge1
# kubectl label node edge2 name=edge2
# kubectl label node edge3 name=edge3
# kubectl label node edge4 name=edge4

for tag in "etl-sys" "etl-taxi" "predict-taxi" "predict-sys"
do

for name in "master" "core" 
do
export tagName=$tag
export nodeName=$name
export slotNum=2
#envsubst < storm-worker-template.json | kubectl create  -f -
envsubst < storm-worker-deploy-template.yaml | kubectl create -f -
#envsubst < storm-worker-service-template.json | kubectl apply -f -
done
done

for tag in "predict-sys" "predict-taxi" 
do
for name in "worker1"  "edge1" "edge2" "edge3"
do
export tagName=$tag
export nodeName=$name
export slotNum=2
echo ""
#envsubst < storm-worker-template.json | kubectl create  -f -
envsubst < storm-worker-deploy-template.yaml | kubectl create -f -
#envsubst < storm-worker-service-template.json | kubectl apply  -f -
done
done

for tag in  "etl-sys" "etl-taxi" 
do
for name in  "worker2" "edge4" "edge5"
do
export tagName=$tag
export nodeName=$name
export slotNum=2
#envsubst < storm-worker-template.json | kubectl create  -f -
envsubst < storm-worker-deploy-template.yaml | kubectl create  -f -
#envsubst < storm-worker-service-template.json | kubectl apply  -f -
done
done


kubectl get pod

export tagName=core1
export nodeName=master
export slotNum=8
#envsubst < storm-master.json | kubectl create -f -
envsubst < storm-master-deploy.yaml | kubectl create -f -
#envsubst < storm-master-service.json | kubectl apply -f -



