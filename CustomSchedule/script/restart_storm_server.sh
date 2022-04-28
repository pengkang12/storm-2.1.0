~/zookeeper/bin/zkServer.sh stop

rm -rf ~/zookeeper/data

~/zookeeper/bin/zkServer.sh start

ps aux | grep nimbus | awk '{print $2}' | xargs kill -9

rm -rf ~/storm/data

sleep 1

~/storm/bin/storm nimbus &

sleep 5

ssh worker1 ps aux | grep supervisor | awk '{print $2}' | xargs kill -9
ssh worker1 ~/storm/bin/storm supervisor &

ssh worker2 ps aux | grep supervisor | awk '{print $2}' | xargs kill -9
ssh worker2 ~/storm/bin/storm supervisor &

sleep 5
