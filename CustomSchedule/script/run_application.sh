
STORM_HOME="/home/cc/storm"
APP_NAME="wordcountMatchingAware"
$STORM_HOME/bin/storm  kill $APP_NAME 

sleep 5

#Running testing application
$STORM_HOME/bin/storm jar $STORM_HOME/examples/storm-starter/target/storm-starter-2.1.0.jar org.apache.storm.starter.WordCountTopologyTagAware $APP_NAME 


sleep 5

$STORM_HOME/bin/storm jar $STORM_HOME/examples/storm-starter/target/storm-starter-2.1.0.jar org.apache.storm.starter.WordCountTopologyTagAware ${APP_NAME}2

# $STORM_HOME/bin/storm  kill $APP_NAME 


