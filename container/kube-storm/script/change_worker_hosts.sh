pods=`kubectl get pod | grep storm | grep -v "ui" | awk '{print $1}'`

#kubectl get pod -o wide | grep storm | grep -v "ui" | awk '{print $6, $1}' > host_ip.txt 
cat host_ip.txt

read -p "check the host ip is correct " yes

if [ $yes != 'yes' ]; then 
   echo "please type yes if it is correct"
   exit
fi

for pod in $pods
do
echo $pod
input="host_ip.txt"
while IFS= read -r host_ip
do
  host=($host_ip)
  string=$(kubectl exec $pod -- cat /etc/hosts | grep ${host[1]})
  data=$(echo "${host[1]}" | awk -F "-" '{print $3}')
  app=$(echo "${host[1]}"| awk -F "-" '{print $4}')
  if [[ $data =~ $pod ]] && [[ $app =~ $pod ]] && test -z "string" 
  then
    echo $data $app $pod
    #kubectl exec $pod -- sh -c "echo ${host_ip} >> /etc/hosts"
  else
    echo "existing" 
  fi
  #sleep 1
done < "$input"

done
