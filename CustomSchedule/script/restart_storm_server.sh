~/zookeeper/bin/zkServer.sh stop

rm -rf ~/zookeeper/data

~/zookeeper/bin/zkServer.sh start

ps aux | grep nimbus | awk '{print $2}' | xargs kill -9

rm -rf ~/storm/data

sleep 1

~/storm/bin/storm nimbus &

sleep 5


for edge in "worker1" "worker2" "edge1" "edge2" "edge3" "edge4" "edge5"
do
ssh $edge bash kill_storm.sh
sleep 1
ssh $edge  ~/storm/bin/storm supervisor &

done
sleep 5
