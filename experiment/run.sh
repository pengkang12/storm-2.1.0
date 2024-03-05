#!/bin/bash

# configure phyiscal network
bash $HOME/storm/network_generator/simulation_network/setup.sh 15 50 50 50

# clean environment
redis-cli flushall
#cat container_name.txt | while read line; do kubeclean $line; done;

app0="ETLTopologySYS"
app1="IoTPredictionTopologySYS"
app2="IoTPredictionTopologyTAXI"
app3="ETLTopologyTAXI"
app4="IoTTrainTopologySYS"
app5="IoTTrainTopologyTAXI"

for app in $app0 $app1 $app2 $app3 $app4 $app5
do
echo $app
# remove application
kubectl exec nimbus -- $HOME/storm/bin/storm kill $app
done

#sleep 120
sleep $[ 60 - $(date +%s) % 60  ]

# create application
scale="10"
baseline="beaver"
# remove application
kubectl exec nimbus -- /bin/bash $HOME/storm/experiment/scripts/run_ETL_sys.sh 1.2 $baseline
sleep 10
kubectl exec nimbus -- /bin/bash $HOME/storm/experiment/scripts/run_PREDICT_sys.sh 0.6 $baseline 
sleep 10
kubectl exec nimbus -- /bin/bash $HOME/storm/experiment/scripts/run_ETL_taxi.sh 1.2 $baseline 
sleep 10
kubectl exec nimbus -- /bin/bash $HOME/storm/experiment/scripts/run_PREDICT_taxi.sh 0.6 $baseline 
sleep 10

sleep 300
START_TIME="`date`"
# start to control CPU resource

# clear application data
LOG="results/"
rm ${LOG}perf.log

# data collection
LOG_FILE="results/perf.log"
sleep $[ 60 - $(date +%s) % 60  ]
for i in {1..125}
do
sleep 56
python3.7 $HOME/storm/experiment/scripts/perf.py >> ${LOG_FILE} &

sleep $[ 60 - $(date +%s) % 60  ]
done
END_TIME= "`date`"

# extract data
METHOD="beaver"
python3 $HOME/storm/experiment/read_container_metrics.py $START_TIME $END_TIME > ${METHOD}.log
sed  "s/::\[/\, /g; s/\]//g; /^121/d; "  ${METHOD}.log  > Util_${METHOD}.txt

# read latency
echo $(grep latency perf.log | awk '{print $4}' ) >> performance.txt
# read throughput
echo $(grep latency perf.log | awk '{print $6}' ) >> performance.txt
# read success tuple
echo $(grep latency perf.log | awk '{print $8}' ) >> performance.txt
