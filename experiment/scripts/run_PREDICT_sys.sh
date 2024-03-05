home_path="/home/cc/"
home_source=${home_path}"storm/riot-bench/modules/tasks/src/main/resources/"
app_path="${home_path}storm/riot-bench/modules/storm/target/iot-bm-storm-0.1-jar-with-dependencies.jar"
output_path="${home_path}storm/riot-bench/output/"
data_type="SYS"
app_name="IoTPredictionTopology"$data_type
input_name="${home_source}${data_type}_sample_data_senml.csv"

# set the input rate
IMG_WIDTH=$1
IMG2_WIDTH=0.9 #0.8, 0.6
inputRate=$(echo "$IMG_WIDTH $IMG2_WIDTH" | awk '{printf "%.4f \n", $1/$2}')

# choose the scheduler methods
option=$2
case "$option" in
   "test") echo "using test scheduler"
  	topologyMap="spout1:master,SenMLParseBoltPRED:master,DecisionTreeClassifyBolt:master,LinearRegressionPredictorBolt:master,BlockWindowAverageBolt:master,ErrorEstimationBolt:master,MQTTPublishBolt:master,sink:master"
   ;;
 
   "default") echo "using default scheduler"
	topologyMap='{"spout1":"master","SenMLParseBoltPRED":"edge1","DecisionTreeClassifyBolt":"worker1","LinearRegressionPredictorBolt":"edge3","BlockWindowAverageBolt":"edge3","ErrorEstimationBolt":"edge1","MQTTPublishBolt":"worker1","sink":"edge3"}'
   ;;
   "resource") echo "using resource aware scheduler"
	# Resource aware 
        topologyMap="spout1:master,SenMLParseBoltPRED:edge1,DecisionTreeClassifyBolt:edge1,LinearRegressionPredictorBolt:edge1,BlockWindowAverageBolt:edge1,ErrorEstimationBolt:edge1,MQTTPublishBolt:worker1,sink:edge3"
   ;;
   "amnis") echo "using amnis scheduler"
 	# Amnis methods
	topologyMap='{"spout1":"master","SenMLParseBoltPRED":"edge1","DecisionTreeClassifyBolt":"edge1","LinearRegressionPredictorBolt":"edge1","BlockWindowAverageBolt":"edge1","ErrorEstimationBolt":"worker1","MQTTPublishBolt":"worker1","sink":"edge3"}'
   ;;
   "coda") echo "using  coda scheduler"
 	# method coda
	topologyMap='{"spout1":"master","SenMLParseBoltPRED":"edge1","DecisionTreeClassifyBolt":"edge1","LinearRegressionPredictorBolt":"edge1","BlockWindowAverageBolt":"edge1","ErrorEstimationBolt":"worker1","MQTTPublishBolt":"edge1","sink":"edge3"}'
   ;;
 
   "beaver") echo "using beaver scheduler"
 	# beaver plus 
   	topologyMap="spout1:master,SenMLParseBoltPRED:edge1,DecisionTreeClassifyBolt:worker1,LinearRegressionPredictorBolt:edge1,BlockWindowAverageBolt:edge1,ErrorEstimationBolt:worker1,MQTTPublishBolt:worker1,sink:edge3"
   ;;
esac
echo "$inputRate $topologyMap"


${home_path}storm/bin/storm kill ${app_name}

cd ~/storm/riot-bench/

~/maven/bin/mvn clean compile package -DskipTests

cd -

#sleep 60

${home_path}storm/bin/storm jar $app_path in.dream_lab.bm.stream_iot.storm.topo.apps.${app_name} C ${app_name} $input_name SENML-210  $inputRate $output_path ${home_source}tasks.properties test $topologyMap

#  Command Meaning: topology-fully-qualified-name <local-or-cluster> <Topo-name> <input-dataset-path-name> <Experi-Run-id> <scaling-factor> 
#<output dir name> <tasks properites filename> <tasks name>

# <task name> only uses in micro. 
#    Example command: SampleTopology L NA /var/tmp/bangalore.csv E01-01 0.001
