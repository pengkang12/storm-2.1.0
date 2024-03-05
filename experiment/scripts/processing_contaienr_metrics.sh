for name in "Beaver" #"Amnis" "Coda" "RStorm" "Storm"
do
python3 ../read_container_metrics.py $name > sys_${name}.log
echo ""
done
rm sys_latency.log
for name in "Beaver" "Amnis" "Coda" "RStorm" "Storm"
do
echo $(grep latency perf_${name}.log | awk '{print $4}' ) >> sys_latency.log
echo $(grep latency perf_${name}.log | awk '{print $6}' ) >> sys_latency.log
echo $(grep latency perf_${name}.log | awk '{print $8}' ) >> sys_latency.log
done
