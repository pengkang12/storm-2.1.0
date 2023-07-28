pods=`kubectl get pod | grep storm | grep -v "ui" | awk '{print $1}'`

#kubectl get pod -o wide | grep storm | grep -v "ui" | awk '{print $8, $1}' > host_ip.txt 


for pod in $pods
do
echo $pod
input="host_ip.txt"
while IFS= read -r host_ip
do
  host=($host_ip)
  string=$(kubectl exec $pod -- cat /etc/hosts | grep ${host[1]})
  if test -z "$string" 
  then
    kubectl exec $pod -- sh -c "echo ${host_ip} >> /etc/hosts"
  else
    echo "existing" 
  fi
done < "$input"

done
