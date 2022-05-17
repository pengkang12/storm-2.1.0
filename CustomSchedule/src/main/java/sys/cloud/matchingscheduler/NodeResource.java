package sys.cloud.matchingscheduler;

import java.util.concurrent.atomic.AtomicInteger;

public class NodeResource {
    private static final AtomicInteger idGen = new AtomicInteger(1);
    private final Integer id;

    String nodeName = null;
    // cpu is 100 milicores
    int cpu = 2000;
    int bandwidth = 10000;
    // bandwidth is 10000M.
    // memory is 1000M
    int memory = 8000;
    // latency is 20ms.
    int latency = 20;
    NodeResource(){
        this.id = idGen.getAndIncrement();
    }
    NodeResource(int cpu, int bandwidth){
        this.cpu = cpu;
        this.bandwidth = bandwidth;
        this.id = idGen.getAndIncrement();
    }

    public void setCpu(int cpu){
        this.cpu = cpu;
    }
    public void setBandwidth(int bandwidth){
        this.bandwidth = bandwidth;
    }
    public int getBandwidth(){
        return this.bandwidth;
    }
    public int getCpu(){
        return this.cpu;
    }
    public int getLatency(){
        return this.latency;
    }

    public int getScore(){
        int score = 0;
        score = cpu/ 1000 + bandwidth/10000;
        return score;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return this.nodeName;
    }
}