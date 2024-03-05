home_path="/home/cc/"
home_source=${home_path}"storm/riot-bench/modules/tasks/src/main/resources/"

# set the input rate
IMG_WIDTH=$1
IMG2_WIDTH=1.5 # 1
inputRate=$(echo "$IMG_WIDTH $IMG2_WIDTH" | awk '{printf "%.4f \n", $1/$2}')
echo $inputRate
#

inputrate=$inputRate
# choose the scheduler methods
option=$2
case "$option" in
   "test") echo "using test scheduler"
	topologyMap="spout1:master,SenMlParseBolt:master,RangeFilterBolt:master,BloomFilterBolt:master,InterpolationBolt:master,JoinBolt:master,AnnotationBolt:master,CsvToSenMLBolt:master,PublishBolt:master,sink:master"
   ;;
   "default") echo "using default scheduler"
	topologyMap="spout1:master,SenMlParseBolt:edge5,RangeFilterBolt:worker2,BloomFilterBolt:edge5,InterpolationBolt:worker2,JoinBolt:edge5,AnnotationBolt:worker2,CsvToSenMLBolt:core,PublishBolt:core,sink:core"
   ;;
   "resource") echo "using resource aware scheduler"
	# Resource aware 
   	topologyMap="spout1:master,SenMlParseBolt:edge5,RangeFilterBolt:edge5,BloomFilterBolt:edge5,InterpolationBolt:edge5,JoinBolt:edge5,AnnotationBolt:edge5,CsvToSenMLBolt:edge5,PublishBolt:worker2,sink:core"
   ;;

   "amnis") echo "using amnis scheduler"
 	# Amnis methods
   	topologyMap="spout1:master,SenMlParseBolt:edge5,RangeFilterBolt:edge5,BloomFilterBolt:edge5,InterpolationBolt:edge5,JoinBolt:edge5,AnnotationBolt:worker2,CsvToSenMLBolt:worker2,PublishBolt:worker2,sink:core"
   ;;
   "coda") echo "using coda scheduler"
 	# coda methods
   	topologyMap="spout1:master,SenMlParseBolt:edge5,RangeFilterBolt:edge5,BloomFilterBolt:edge5,InterpolationBolt:edge5,JoinBolt:edge5,AnnotationBolt:edge5,CsvToSenMLBolt:worker2,PublishBolt:worker2,sink:core"
   ;;
   "beaver") echo "using beaver scheduler"
 	# beaver 
	topologyMap="spout1:master,SenMlParseBolt:edge5,RangeFilterBolt:edge5,BloomFilterBolt:edge5,InterpolationBolt:edge5,JoinBolt:edge5,AnnotationBolt:worker2,CsvToSenMLBolt:core,PublishBolt:core,sink:core"
   ;;
#"spout1:edge1,SenMlParseBolt:edge1,RangeFilterBolt:edge1,BloomFilterBolt:edge1,InterpolationBolt:edge1,JoinBolt:edge1,AnnotationBolt:edge1,CsvToSenMLBolt:master,PublishBolt:master,sink:master,spout2:edge2,SenMlParseBolt1:edge2,RangeFilterBolt1:edge2,BloomFilterBolt1:edge2,InterpolationBolt1:edge2,JoinBolt1:edge2, AnnotationBolt1:edge2,CsvToSenMLBolt1:edge2,PublishBolt1:edge2"
esac

${home_path}storm/bin/storm kill ETLTopologySYS

cd ${home_path}/storm/riot-bench/

${home_path}/maven/bin/mvn clean compile package -DskipTests
cd -

#sleep 60

${home_path}storm/bin/storm jar ${home_path}storm/riot-bench/modules/storm/target/iot-bm-storm-0.1-jar-with-dependencies.jar in.dream_lab.bm.stream_iot.storm.topo.apps.ETLTopology C ETLTopologySYS ${home_source}SYS_sample_data_senml.csv SENML $inputrate   ${home_path}storm/riot-bench/output/    ${home_source}tasks.properties  test $topologyMap

#  Command Meaning: topology-fully-qualified-name <local-or-cluster> <Topo-name> <input-dataset-path-name> <Experi-Run-id> <scaling-factor> 
#<output dir name> <tasks properites filename> <tasks name>

# <task name> only uses in micro. 
#    Example command: SampleTopology L NA /var/tmp/bangalore.csv E01-01 0.001
