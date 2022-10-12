~/zookeeper/bin/zkServer.sh stop

rm -rf ~/zookeeper/data

~/zookeeper/bin/zkServer.sh start

ps aux | grep nimbus | awk '{print $2}' | xargs kill -9

rm -rf ~/storm/data

sleep 1

~/storm/bin/storm nimbus &

sleep 5

ssh worker1 bash kill_storm.sh
sleep 1
ssh worker1 ~/storm/bin/storm supervisor &

ssh worker2 bash kill_storm.sh
sleep 1
ssh worker2 ~/storm/bin/storm supervisor &

ssh raspberry3B bash kill_storm.sh
sleep 1
ssh raspberry3B ~/storm/bin/storm supervisor &

ssh raspberry3B1 bash kill_storm.sh
sleep 1
ssh raspberry3B1 ~/storm/bin/storm supervisor &


sleep 5
