git pull

STORM_HOME="/home/cc/storm"

~/maven/bin/mvn clean compile package -DskipTest

#copy lib file to storm/lib/
cp target/CustomScheduler-1.0.jar  $STORM_HOME/lib/

#Running testing application
$STORM_HOME/bin/storm jar $STORM_HOME/examples/storm-starter/target/storm-starter-2.1.0.jar org.apache.storm.starter.WordCountTopologyTagAware wordcountTagAware
