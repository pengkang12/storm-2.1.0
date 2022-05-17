package sys.cloud.matchingscheduler;

import org.apache.storm.scheduler.WorkerSlot;

import javax.xml.soap.Node;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;



public class WorkerSlotExtern {

    private static final AtomicInteger idGen = new AtomicInteger(1);
    private final Integer id;

    WorkerSlot workerSlot = null;
    NodeResource node = null;
    int score = 0;
    HashMap<String, HashMap<String, Integer>>  nodeLatencyMap = new HashMap<String, HashMap<String, Integer>>(){{
        put("worker1",
                new HashMap<String, Integer>(){{
                    put("worker2",20);
                }}
            );
        put("worker2",
                new HashMap<String, Integer>(){{
                    put("worker1",40);
                }}
        );
        }};

    WorkerSlotExtern(){
        this.id = idGen.getAndIncrement();
    }

    public Integer getId() {
        return this.id;
    }

    public WorkerSlot getWorkerSlot() {
        return workerSlot;
    }
    public void setWorkerSlot(WorkerSlot workerSlot){
        this.workerSlot = workerSlot;
    }

    public void setNode(NodeResource node) {
        this.node = node;
    }
    public NodeResource getNode(){
        return this.node;
    }
    public int getScore(){
        return this.score;
    }
    public void setScore(int score) {
        this.score = score;
    }
    public int getTwoNodeLatency(NodeResource n1, Collection<Container> prevContainerList){
        int latency = -1;
        if (prevContainerList == null){
            return new Random().nextInt(100);
        }
        for (Container container : prevContainerList){
            String name1 = container.getWorkerSlotExtern().getNode().getNodeName();
            String name2 = container.getWorkerSlotExtern().getNode().getNodeName();
            if (latency > nodeLatencyMap.get(name1).get(name2)){
                latency = nodeLatencyMap.get(name1).get(name2);
            }
        }
        return latency;
    }
    public void calculateScore(Container container){
        int scores = container.getBandWidth()/this.node.getBandwidth() + container.getCpu()/this.node.getCpu() + getTwoNodeLatency(this.node, container.getPredecessors());
        setScore(scores);
    }
}
