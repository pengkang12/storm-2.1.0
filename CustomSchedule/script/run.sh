
STORM_HOME="/home/cc/storm"

$STORM_HOME/bin/storm  kill wordcountTagAware

sleep 5

#Running testing application
$STORM_HOME/bin/storm jar $STORM_HOME/examples/storm-starter/target/storm-starter-2.1.0.jar org.apache.storm.starter.WordCountTopologyTagAware wordcountTagAware


sleep 30

$STORM_HOME/bin/storm  kill wordcountTagAware


