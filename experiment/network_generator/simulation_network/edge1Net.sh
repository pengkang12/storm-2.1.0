edge1="192.168.122.148"
edge2="192.168.122.149"
edge3="192.168.122.133"
edge4="192.168.122.22" 
edge5="192.168.122.235" 
router="192.168.122.1"
worker1="192.168.122.130"
worker2="192.168.122.131"
core="192.168.122.132"

#first layer network delay
delay1=$1
# second layer network delay
delay2=$2
# first layer bandwidth
band1=$3
# second layer bandwidth
band2=$4

device="ens2"
sudo tc qdisc delete dev $device parent 1:10 handle 10: netem
sudo tc qdisc delete dev $device root

sudo tc class delete dev $device classid 1:10
sudo tc class delete dev $device classid 1:20
sleep 5
# Define the traffic control classes to classify and control network traffic. For example, let's create two classes: class1 and class2 with different bandwidth rates:
sudo tc qdisc add dev $device root handle 1: htb default 1
sudo tc class add dev $device parent 1: classid 1:1 htb rate 150mbit
sudo tc class add dev $device parent 1:1 classid 1:10 htb rate ${band1}mbit 
sudo tc class add dev $device parent 1:1 classid 1:20 htb rate ${band1}mbit 
sudo tc class add dev $device parent 1:1 classid 1:30 htb rate ${band1}mbit 



# Add a 10ms delay to the traffic assigned to classid 1:10
# Add a 5ms delay to the traffic assigned to classid 1:20
#need to consider other node for worker1
sudo tc qdisc add dev $device parent 1:10 handle 10: netem delay ${delay1}ms
val1=`expr $delay1 + $delay2`
sudo tc qdisc add dev $device parent 1:20 handle 20: netem delay ${val1}ms
val2=`expr $delay1 + $delay1`
sudo tc qdisc add dev $device parent 1:30 handle 30: netem delay ${val2}ms 

# Create traffic control filters to match specific IP addresses and assign them to the desired classes
sudo tc filter add dev $device parent 1: protocol ip prio 1 u32 match ip dst $worker1 flowid 1:10
sudo tc filter add dev $device parent 1: protocol ip prio 1 u32 match ip dst $core flowid 1:20
sudo tc filter add dev $device parent 1: protocol ip prio 1 u32 match ip dst $edge3 flowid 1:30




# verify configuration
sudo tc -s -d qdisc show dev $device

# delete configuration 
device="ens2"
#sudo tc qdisc delete dev $device parent 1:10 handle 10: netem
#sudo tc class delete dev $device classid 1:10
#sudo tc class delete dev $device classid 1:20
#sudo tc qdisc delete dev $device root


