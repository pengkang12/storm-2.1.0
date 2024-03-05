cd $HOME/storm/container/kube-storm/

# application name
declare -a applicationList=("etl-sys" "etl-taxi" "predict-taxi" "predict-sys")
# change this to match your cluster
declare -a nodeList=("master" "core" "worker1" "worker2" "edge1" "edge2" "edge3" "edge4" "edge5")

# label each node
for name in ${nodeList[@]}
do
kubectl label node $name name=$name
done
kubectl label node master name=core1

# create container for each node
for tag in ${applicationList[@]}
do
for name in ${nodeList[@]}
do
export tagName=$tag
export nodeName=$name
export slotNum=2
envsubst < storm-worker-template.json | kubectl create  -f -
done
done

# create master node with more slot configuration
kubectl get pod
export tagName=core1
export nodeName=master
export slotNum=8
envsubst < storm-master.json | kubectl create -f -

sleep 10

kubectl get pod






