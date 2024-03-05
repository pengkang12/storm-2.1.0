core1="192.168.122.204"
worker1="192.168.122.130" 
worker2="192.168.122.131" 
edge1="192.168.122.148"
edge2="192.168.122.149"
edge3="192.168.122.132"
edge4="192.168.122.22" 
edge5="192.168.122.235" 



for i in $core1 $worker1 $worker2 $edge1 $edge2 $edge3 $edge4 $edge5
do
device="ens2"
echo "syscloud" | ssh -tt $i sudo tc qdisc delete dev $device parent 1:10 handle 10: netem
echo "syscloud" | ssh -tt $i  sudo tc qdisc delete dev $device root

done

for i in $core1 $worker1 $worker2 $edge1 $edge2 $edge3 $edge4 $edge5
do
echo ""
done
exit


