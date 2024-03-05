#!/bin/bash

core1="192.168.122.132"
worker1="192.168.122.130" 
worker2="192.168.122.131" 
edge1="192.168.122.148"
edge2="192.168.122.149"
edge3="192.168.122.133"
edge4="192.168.122.22" 
edge5="192.168.122.235" 

DIR=$HOME"/storm/experiment/schedule_policy/network_generator/simulation_network/"

for host in  "worker1" "edge1" "edge2" "worker2" "edge4" "edge5"
do

scp ${DIR}${host}Net.sh $host:~/.
echo "syscloud" | ssh -tt $host bash ~/${host}Net.sh $1 $2 $3 $4 

done

