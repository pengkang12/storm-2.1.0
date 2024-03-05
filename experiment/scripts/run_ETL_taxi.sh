home_path="/home/cc/"
home_source=${home_path}"storm/riot-bench/modules/tasks/src/main/resources/"


${home_path}storm/bin/storm kill ETLTopologyTAXI

# set the input rate
IMG_WIDTH=$1
IMG2_WIDTH=200 # 200
inputRate=$(echo "$IMG_WIDTH $IMG2_WIDTH" | awk '{printf "%.4f \n", $1/$2}')
echo $inputRate
# choose the scheduler methods
option=$2
case "$option" in
   "test") echo "using test scheduler"
	topologyMap="spout1:master,SenMlParseBolt:edge4,RangeFilterBolt:edge4,BloomFilterBolt:edge4,InterpolationBolt:worker2,JoinBolt:worker2,AnnotationBolt:worker2,CsvToSenMLBolt:core,PublishBolt:core,sink:core"
   ;; 
   "default") echo "using default scheduler"
	topologyMap="spout1:master,SenMlParseBolt:edge4,RangeFilterBolt:worker2,BloomFilterBolt:edge4,InterpolationBolt:worker2,JoinBolt:edge4,AnnotationBolt:worker2,CsvToSenMLBolt:core,PublishBolt:core,sink:core"
   ;; 
   "resource") echo "using resource aware scheduler"
	# Resource aware 
      	topologyMap="spout1:master,SenMlParseBolt:edge4,RangeFilterBolt:edge4,BloomFilterBolt:edge4,InterpolationBolt:edge4,JoinBolt:edge4,AnnotationBolt:edge4,CsvToSenMLBolt:edge4,PublishBolt:worker2,sink:core"
   ;; 
   "amnis") echo "using amnis scheduler"
 	# Amnis methods
   	topologyMap="spout1:master,SenMlParseBolt:edge4,RangeFilterBolt:edge4,BloomFilterBolt:edge4,InterpolationBolt:edge4,JoinBolt:edge4,AnnotationBolt:worker2,CsvToSenMLBolt:worker2,PublishBolt:worker2,sink:core"
   ;; 
   "coda") echo "using coda scheduler"
        topologyMap="spout1:master,SenMlParseBolt:edge4,RangeFilterBolt:edge4,BloomFilterBolt:edge4,InterpolationBolt:edge4,JoinBolt:edge4,AnnotationBolt:edge4,CsvToSenMLBolt:worker2,PublishBolt:worker2,sink:core"
   ;;
   "beaver") echo "using beaver scheduler"
 	# beaver 
 	topologyMap="spout1:master,SenMlParseBolt:edge4,RangeFilterBolt:edge4,BloomFilterBolt:edge4,InterpolationBolt:edge4,JoinBolt:edge4,AnnotationBolt:worker2,CsvToSenMLBolt:core,PublishBolt:core,sink:core"
   ;; 
esac

echo "$topologyMap"
 

${home_path}storm/bin/storm jar ${home_path}storm/riot-bench/modules/storm/target/iot-bm-storm-0.1-jar-with-dependencies.jar in.dream_lab.bm.stream_iot.storm.topo.apps.ETLTopology C ETLTopologyTAXI ${home_source}TAXI_sample_data_senml.csv SENML $inputRate   ${home_path}storm/riot-bench/output/    ${home_source}tasks_TAXI.properties  test $topologyMap
