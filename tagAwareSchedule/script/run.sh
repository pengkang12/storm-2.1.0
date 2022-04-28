
STORM_HOME="/home/cc/storm"
#Running testing application
$STORM_HOME/bin/storm jar $STORM_HOME/examples/storm-starter/target/storm-starter-2.1.0.jar org.apache.storm.starter.WordCountTopologyTagAware wordcountTagAware
