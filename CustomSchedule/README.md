# this is tag-based scheduler for apache storm.

### tag supervisor node, edit their configuration file: $STORM_HOME/conf/storm.yaml

```
supervisor.scheduler.meta:
    tags: edge1
```

### tag java topology, example file: $STORM_HOME/
	TopologyBuilder builder = new TopologyBuilder();
	builder.setSpout("spout", new ExampleSpout(), 1); builder.setBolt("bolt1", new ExampleBolt1(), 1).shuffleGrouping("spout");
	builder.setBolt("bolt2", new ExampleBolt2(), 1).shuffleGrouping("bolt1");
	builder.setBolt("bolt3", new ExampleBolt3(), 1).shuffleGrouping("bolt2").addConfiguration("tags", "edge1");

### Compile application:

	~/maven/bin/mvn clean compile package -DskipTest


### copy lib file to storm/lib/

	cp target/CustomScheduler-1.0.jar $STORM_HOME/lib/

### Add the following content to configuration file $STORM_HOME/conf/storm.yaml

	storm.scheduler: "sys.cloud.tagawarescheduler.TagAwareScheduler"

### Restart Apache Storm

### Running testing application

	bin/storm jar examples/storm-starter/target/storm-starter-2.1.0.jar org.apache.storm.starter.WordCountTopologyTagAware wordcountTagAware 

